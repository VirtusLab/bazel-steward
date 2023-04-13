# solution from https://github.com/bazelbuild/bazel/issues/6377#issuecomment-1237791008
def _sort_by_kind_then_name(x):
    return x["kind"] + x["name"]

def map(f, list):
    return [f(x) for x in list]

def to_struct(x):
    return struct(
        name = x.get("name"),
        kind = x.get("kind"),
        url = x.get("url"),
        urls = x.get("urls"),
        generator_function = x.get("generator_function"),
        sha256 = x.get("sha256"),
        strip_prefix = x.get("strip_prefix"),
    )

def repositories_as_json():
    sorted_list = map(to_struct, sorted(native.existing_rules().values(), key = _sort_by_kind_then_name))
    return json.encode_indent(sorted_list, indent = " ")

def _dump_all_repositories_impl(repository_ctx):
    repository_ctx.file("BUILD", executable = False, content = "exports_files(['result.json'])")
    repository_ctx.file("result.json", executable = False, content = repository_ctx.attr.repositories_json)

dump_all_repositories = repository_rule(
    implementation = _dump_all_repositories_impl,
    attrs = {
        "repositories_json": attr.string(mandatory = True),
    },
)
