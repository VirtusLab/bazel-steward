import unittest

from action.release_tag_resolver import ReleaseTagResolver, RepositoryTag


class FakeMetadataProvider:
    def __init__(self, releases, tags, refs):
        self._releases = releases
        self._tags = tags
        self._refs = refs

    def list_releases(self, repository):
        return self._releases

    def list_tags(self, repository):
        return self._tags

    def resolve_ref_to_commit_sha(self, repository, ref):
        return self._refs.get(ref)


class ReleaseTagResolverTest(unittest.TestCase):
    tagged_commit_sha = "15ba5fa2b7eb9d9f2e67edb8cb355130b96d7a4d"
    other_commit_sha = "cccccccccccccccccccccccccccccccccccccccc"
    fake_releases = [
        "v1.7.2-rc9",
        "v1.7.2",
        "v1.7.2.1",
        "v1.7.3",
    ]
    fake_tags = [
        RepositoryTag("v1.7.2", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"),
        RepositoryTag("v1.7.2.1", tagged_commit_sha),
        RepositoryTag("v1.7.3", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"),
        RepositoryTag("v1.7.2-rc9", tagged_commit_sha),
    ]

    def make_provider(self):
        return FakeMetadataProvider(
            releases=self.fake_releases,
            tags=self.fake_tags,
            refs={"main": self.tagged_commit_sha, "develop": self.other_commit_sha},
        )

    def test_v_prefix_resolves_to_latest_matching_stable_release(self):
        self.assertEqual(
            ReleaseTagResolver.resolve_latest_matching_release("v1.7", self.fake_releases),
            "v1.7.3",
        )

    def test_exact_v_tag_ref_resolves_to_latest_matching_patch_release(self):
        self.assertEqual(
            ReleaseTagResolver.resolve_latest_matching_release("v1.7.2", self.fake_releases),
            "v1.7.2.1",
        )

    def test_commit_sha_resolves_to_release_tag_pointing_to_the_same_commit(self):
        self.assertEqual(
            ReleaseTagResolver.resolve(self.tagged_commit_sha, "VirtusLab/bazel-steward", self.make_provider()),
            "v1.7.2.1",
        )

    def test_branch_ref_resolves_to_release_tag_via_branch_head_commit(self):
        self.assertEqual(
            ReleaseTagResolver.resolve("main", "VirtusLab/bazel-steward", self.make_provider()),
            "v1.7.2.1",
        )

    def test_unknown_branch_ref_fails(self):
        with self.assertRaisesRegex(
            RuntimeError,
            "Could not resolve ref non-existing-branch in VirtusLab/bazel-steward to a commit SHA",
        ):
            ReleaseTagResolver.resolve("non-existing-branch", "VirtusLab/bazel-steward", self.make_provider())

    def test_commit_sha_without_matching_release_tag_fails(self):
        with self.assertRaisesRegex(
            RuntimeError,
            (
                "No GitHub release tag in VirtusLab/bazel-steward points to commit "
                "cccccccccccccccccccccccccccccccccccccccc \\(resolved from "
                "cccccccccccccccccccccccccccccccccccccccc\\)"
            ),
        ):
            ReleaseTagResolver.resolve(self.other_commit_sha, "VirtusLab/bazel-steward", self.make_provider())

    def test_matches_tag_pattern_accepts_optional_numeric_suffix_segments(self):
        self.assertTrue(ReleaseTagResolver.matches_tag_pattern("v1.7.2.1", "v1.7.2"))
        self.assertFalse(ReleaseTagResolver.matches_tag_pattern("v1.7.2-rc9", "v1.7.2"))
        self.assertTrue(ReleaseTagResolver.matches_tag_pattern("v1.7.3", "v1.7"))

    def test_human_readable_gh_release_table_line_is_not_treated_as_release_tag(self):
        self.assertIsNone(
            ReleaseTagResolver.extract_release_tag("Release 1.7.2\tLatest\tv1.7.2\tabout 1 day ago"),
        )

    def test_short_commit_sha_resolves_to_release_tag(self):
        short_sha = self.tagged_commit_sha[:12]
        self.assertEqual(
            ReleaseTagResolver.resolve(short_sha, "VirtusLab/bazel-steward", self.make_provider()),
            "v1.7.2.1",
        )

    def test_ref_resolving_to_commit_without_release_tag_fails(self):
        with self.assertRaisesRegex(
            RuntimeError,
            (
                "No GitHub release tag in VirtusLab/bazel-steward points to commit "
                "cccccccccccccccccccccccccccccccccccccccc \\(resolved from develop\\)"
            ),
        ):
            ReleaseTagResolver.resolve("develop", "VirtusLab/bazel-steward", self.make_provider())

    def test_commit_sha_can_resolve_to_rc_release_tag(self):
        rc_only_provider = FakeMetadataProvider(
            releases=["v1.7.2-rc9"],
            tags=[RepositoryTag("v1.7.2-rc9", self.tagged_commit_sha)],
            refs={"main": self.tagged_commit_sha},
        )
        self.assertEqual(
            ReleaseTagResolver.resolve(self.tagged_commit_sha, "VirtusLab/bazel-steward", rc_only_provider),
            "v1.7.2-rc9",
        )


if __name__ == "__main__":
    unittest.main()
