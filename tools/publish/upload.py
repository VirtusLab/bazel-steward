"""Publish Maven artifacts to Sonatype Central Publisher API.

Reads a JSON manifest (produced by the maven_central_artifacts Bazel rule)
to locate versioned artifacts in the runfiles tree, then signs, bundles,
and uploads them to Maven Central.

Checksums are computed with hashlib -- no coreutils dependency.
"""

import base64
import hashlib
import json
import os
import shutil
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request
import zipfile

from python.runfiles import runfiles as runfiles_lib

CENTRAL_API = "https://central.sonatype.com/api/v1/publisher"
POLL_INTERVAL_SECONDS = 10
POLL_MAX_ATTEMPTS = 60


def _require_env(name):
    v = os.environ.get(name)
    if not v:
        sys.exit(name + " environment variable must be set")
    return v


def _file_digest(path, algorithm):
    h = hashlib.new(algorithm)
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(8192), b""):
            h.update(chunk)
    return h.hexdigest()


def _write_checksum(artifact_path, algorithm, ext):
    digest = _file_digest(artifact_path, algorithm)
    with open(artifact_path + ext, "w") as f:
        f.write(digest)


def _gpg_sign(path, passphrase):
    subprocess.run(
        [
            "gpg", "--batch", "--yes",
            "--passphrase-fd", "0",
            "--pinentry-mode", "loopback",
            "-ab", path,
        ],
        input=passphrase.encode(),
        check=True,
    )


def _http_post(url, headers, data=None, content_type=None):
    hdrs = dict(headers)
    if content_type:
        hdrs["Content-Type"] = content_type
    req = urllib.request.Request(url, data=data, headers=hdrs, method="POST")
    try:
        resp = urllib.request.urlopen(req)
        return resp.status, resp.read()
    except urllib.error.HTTPError as e:
        return e.code, e.read()


def _upload_bundle(auth_header, bundle_path):
    boundary = "----BazelStewardPublish"
    with open(bundle_path, "rb") as f:
        payload = f.read()
    body = (
        "--" + boundary + "\r\n"
        "Content-Disposition: form-data;"
        ' name="bundle"; filename="central-bundle.zip"\r\n'
        "Content-Type: application/octet-stream\r\n\r\n"
    ).encode() + payload + ("\r\n--" + boundary + "--\r\n").encode()

    code, data = _http_post(
        CENTRAL_API + "/upload?publishingType=AUTOMATIC",
        {"Authorization": auth_header},
        data=body,
        content_type="multipart/form-data; boundary=" + boundary,
    )
    if code < 200 or code >= 300:
        sys.exit("Upload failed (HTTP " + str(code) + "): " + data.decode())
    return data.decode().strip()


def _poll_status(auth_header, deployment_id):
    state = "UNKNOWN"
    for _ in range(POLL_MAX_ATTEMPTS):
        time.sleep(POLL_INTERVAL_SECONDS)
        code, data = _http_post(
            CENTRAL_API + "/status?id=" + deployment_id,
            {"Authorization": auth_header},
        )
        if code < 200 or code >= 300:
            sys.exit(
                "Status check failed (HTTP " + str(code) + "): " + data.decode()
            )

        status = json.loads(data)
        state = status["deploymentState"]
        print("Deployment state: " + state)

        if state == "PUBLISHED":
            print("Successfully published to Maven Central!")
            return
        if state == "FAILED":
            print("Deployment failed!", file=sys.stderr)
            print(json.dumps(status, indent=2), file=sys.stderr)
            sys.exit(1)
        if state not in ("PENDING", "VALIDATING", "VALIDATED", "PUBLISHING"):
            print("Unknown deployment state: " + state, file=sys.stderr)

    sys.exit("Timed out waiting for deployment (last state: " + state + ")")


def main():
    if len(sys.argv) < 2:
        sys.exit("Usage: upload.py <manifest-rlocationpath>")

    rf = runfiles_lib.Create()
    if rf is None:
        sys.exit("Cannot initialize Bazel runfiles")

    manifest_path = rf.Rlocation(sys.argv[1])
    if not manifest_path or not os.path.isfile(manifest_path):
        sys.exit("Manifest not found at rlocation: " + sys.argv[1])

    with open(manifest_path) as f:
        manifest = json.load(f)

    version = manifest["version"]
    group_id = manifest["group_id"]
    artifact_id = manifest["artifact_id"]
    workspace = manifest["workspace"]

    if version == "0.0.0":
        sys.exit(
            "Version 0.0.0 is a placeholder. "
            "Set --define MAVEN_VERSION=<version> when invoking bazel run."
        )

    passphrase = _require_env("PGP_PASSPHRASE")
    username = _require_env("SONATYPE_USERNAME")
    password = _require_env("SONATYPE_PASSWORD")

    artifact_keys = {
        ".jar": "jar",
        "-sources.jar": "sources",
        "-javadoc.jar": "javadoc",
        ".pom": "pom",
    }
    artifact_paths = {}
    for suffix, key in artifact_keys.items():
        rloc = workspace + "/" + manifest[key]
        path = rf.Rlocation(rloc)
        if not path or not os.path.isfile(path):
            sys.exit("Artifact not found at rlocation: " + rloc)
        artifact_paths[suffix] = path

    pom_path = artifact_paths[".pom"]
    with open(pom_path) as pom_f:
        pom_content = pom_f.read()
    expected_version_tag = "<version>" + version + "</version>"
    if expected_version_tag not in pom_content:
        sys.exit(
            "POM version mismatch: '{}' not found in {}.\n"
            "The version substitution in publish.bzl may have failed "
            "(check that the POM placeholder is '<version>0.0.0</version>').".format(
                expected_version_tag, pom_path
            )
        )

    with tempfile.TemporaryDirectory() as tmpdir:
        group_path = group_id.replace(".", "/")
        adir = os.path.join(tmpdir, group_path, artifact_id, version)
        os.makedirs(adir)

        for suffix, src in artifact_paths.items():
            dst = os.path.join(adir, artifact_id + "-" + version + suffix)
            shutil.copy2(src, dst)
            _write_checksum(dst, "md5", ".md5")
            _write_checksum(dst, "sha1", ".sha1")
            _gpg_sign(dst, passphrase)

        bundle = os.path.join(tmpdir, "central-bundle.zip")
        group_root = group_id.split(".")[0]
        with zipfile.ZipFile(bundle, "w", zipfile.ZIP_DEFLATED) as zf:
            base = os.path.join(tmpdir, group_root)
            for dirpath, _, filenames in os.walk(base):
                for fn in sorted(filenames):
                    full = os.path.join(dirpath, fn)
                    zf.write(full, os.path.relpath(full, tmpdir))

        auth_token = base64.b64encode(
            (username + ":" + password).encode()
        ).decode()
        auth_header = "Bearer " + auth_token

        coords = group_id + ":" + artifact_id + ":" + version
        print("Uploading " + coords + " to Maven Central...")
        deployment_id = _upload_bundle(auth_header, bundle)
        print("Deployment ID: " + deployment_id)
        _poll_status(auth_header, deployment_id)


if __name__ == "__main__":
    main()
