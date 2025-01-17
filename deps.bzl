# Copyright (c) 2019 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: Apache-2.0

#
# The dependencies of the daml workspace.
# This allows using the daml workspace externally
# from another bazel workspace.
#
# For example, another Bazel project can depend on
# targets in the daml repository by doing:
# ---
# local_repository(
#   name = "com_github_digital_asset_daml",
#   path = "/path/to/daml"
# )
# load("@com_github_digital_asset_daml//:deps.bzl", "daml_deps")
# daml_deps()
# ---
#
# A 3rd-party consumer would also need to register relevant
# toolchains and repositories in order to build targets.
# That is, copy some setup calls from WORKSPACE into the
# other WORKSPACE.
#
# Make sure to reference repository local files with the full
# prefix: @com_github_digital_asset_daml//..., as these won't
# be resolvable from external workspaces otherwise.

rules_scala_version = "78104d8014d4e4fc8f905cd34b91dfabd9a268c8"
rules_haskell_version = "1abeb655147459dfc60eddf556914699d1630d86"
rules_haskell_sha256 = "d8d4f5247e59182b1c5ce3c19d2f48f0b6fbbbc9b16b2e37dd53533c4f258ba3"
rules_nixpkgs_version = "5ffb8a4ee9a52bc6bc12f95cd64ecbd82a79bc82"

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")

def daml_deps():
    if "io_tweag_rules_haskell" not in native.existing_rules():
        http_archive(
            name = "io_tweag_rules_haskell",
            strip_prefix = "rules_haskell-%s" % rules_haskell_version,
            urls = ["https://github.com/tweag/rules_haskell/archive/%s.tar.gz" % rules_haskell_version],
            patches = [
                "@com_github_digital_asset_daml//bazel_tools:haskell-static-linking.patch",
                "@com_github_digital_asset_daml//bazel_tools:haskell-package-env.patch",
                "@com_github_digital_asset_daml//bazel_tools:haskell-drop-fake-static.patch",
                "@com_github_digital_asset_daml//bazel_tools:haskell-keep-hs-extra-libs.patch",
            ],
            patch_args = ["-p1"],
            sha256 = rules_haskell_sha256,
        )

    if "io_tweag_rules_nixpkgs" not in native.existing_rules():
        http_archive(
            name = "io_tweag_rules_nixpkgs",
            strip_prefix = "rules_nixpkgs-%s" % rules_nixpkgs_version,
            urls = ["https://github.com/tweag/rules_nixpkgs/archive/%s.tar.gz" % rules_nixpkgs_version],
            sha256 = "085d480232c0bada20c0d0b8b1b4ba8c62fcc006d9dc826aa0e4205e4dca6cb3",
        )

    if "ai_formation_hazel" not in native.existing_rules():
        http_archive(
            name = "ai_formation_hazel",
            strip_prefix = "rules_haskell-{}/hazel".format(rules_haskell_version),
            urls = ["https://github.com/tweag/rules_haskell/archive/%s.tar.gz" % rules_haskell_version],
            sha256 = rules_haskell_sha256,
        )

    if "com_github_madler_zlib" not in native.existing_rules():
        http_archive(
            name = "com_github_madler_zlib",
            build_file = "@com_github_digital_asset_daml//3rdparty/c:zlib.BUILD",
            strip_prefix = "zlib-cacf7f1d4e3d44d871b605da3b647f07d718623f",
            urls = ["https://github.com/madler/zlib/archive/cacf7f1d4e3d44d871b605da3b647f07d718623f.tar.gz"],
            sha256 = "6d4d6640ca3121620995ee255945161821218752b551a1a180f4215f7d124d45",
        )

    if "io_bazel_rules_go" not in native.existing_rules():
        http_archive(
            name = "io_bazel_rules_go",
            urls = ["https://github.com/bazelbuild/rules_go/releases/download/0.17.0/rules_go-0.17.0.tar.gz"],
            sha256 = "492c3ac68ed9dcf527a07e6a1b2dcbf199c6bf8b35517951467ac32e421c06c1",
        )

    if "io_bazel_rules_scala" not in native.existing_rules():
        http_archive(
            name = "io_bazel_rules_scala",
            url = "https://github.com/bazelbuild/rules_scala/archive/%s.zip" % rules_scala_version,
            type = "zip",
            strip_prefix = "rules_scala-%s" % rules_scala_version,
            sha256 = "2b39ea3eba5ce86126980fa2bf20db9e0896b75aec23f0c639d9bb47dd9914b9",
            patches = [
                "@com_github_digital_asset_daml//bazel_tools:scala-escape-jvmflags.patch",
            ],
            patch_args = ["-p1"],
        )

    if "com_google_protobuf" not in native.existing_rules():
        http_archive(
            name = "com_google_protobuf",
            sha256 = "9510dd2afc29e7245e9e884336f848c8a6600a14ae726adb6befdb4f786f0be2",
            strip_prefix = "protobuf-3.6.1.3",
            urls = ["https://github.com/google/protobuf/archive/v3.6.1.3.zip"],
        )

    if "com_google_protobuf_javalite" not in native.existing_rules():
        http_archive(
            name = "com_google_protobuf_javalite",
            sha256 = "79d102c61e2a479a0b7e5fc167bcfaa4832a0c6aad4a75fa7da0480564931bcc",
            strip_prefix = "protobuf-384989534b2246d413dbcd750744faab2607b516",
            urls = ["https://github.com/google/protobuf/archive/384989534b2246d413dbcd750744faab2607b516.zip"],
        )

    if "io_bazel_skydoc" not in native.existing_rules():
        http_archive(
            name = "io_bazel_skydoc",
            sha256 = "19eb6c162075707df5703c274d3348127625873dbfa5ff83b1ef4b8f5dbaa449",
            strip_prefix = "skydoc-0.2.0",
            urls = ["https://github.com/bazelbuild/skydoc/archive/0.2.0.tar.gz"],
        )

    if "bazel_gazelle" not in native.existing_rules():
        http_archive(
            name = "bazel_gazelle",
            urls = ["https://github.com/bazelbuild/bazel-gazelle/releases/download/0.16.0/bazel-gazelle-0.16.0.tar.gz"],
            sha256 = "7949fc6cc17b5b191103e97481cf8889217263acf52e00b560683413af204fcb",
        )

    if "io_bazel_rules_sass" not in native.existing_rules():
        http_archive(
            name = "io_bazel_rules_sass",
            sha256 = "1e135452dc627f52eab39a50f4d5b8d13e8ed66cba2e6da56ac4cbdbd776536c",
            strip_prefix = "rules_sass-1.15.2",
            urls = ["https://github.com/bazelbuild/rules_sass/archive/1.15.2.tar.gz"],
        )

    # Fetch rules_nodejs so we can install our npm dependencies
    if "build_bazel_rules_nodejs" not in native.existing_rules():
        http_archive(
            name = "build_bazel_rules_nodejs",
            urls = ["https://github.com/bazelbuild/rules_nodejs/releases/download/0.29.2/rules_nodejs-0.29.2.tar.gz"],
            sha256 = "395b7568f20822c13fc5abc65b1eced637446389181fda3a108fdd6ff2cac1e9",
            patches = ["@com_github_digital_asset_daml//bazel_tools:rules_nodejs_default_shell_env.patch"],
            patch_args = ["-p1"],
        )

    if "com_github_grpc_grpc" not in native.existing_rules():
        http_archive(
            name = "com_github_grpc_grpc",
            strip_prefix = "grpc-1.19.0",
            urls = ["https://github.com/grpc/grpc/archive/v1.19.0.tar.gz"],
            sha256 = "1d54cd95ed276c42c276e0a3df8ab33ee41968b73af14023c03a19db48f82e73",
            patches = [
                "@com_github_digital_asset_daml//bazel_tools:grpc-bazel-mingw.patch",
            ],
            patch_args = ["-p1"],
        )

    if "io_grpc_grpc_java" not in native.existing_rules():
        http_archive(
            name = "io_grpc_grpc_java",
            strip_prefix = "grpc-java-1.19.0",
            urls = ["https://github.com/grpc/grpc-java/archive/v1.19.0.tar.gz"],
            sha256 = "81d1e12bf0f8bd1560eed7c75f24d8bb8e7368dcf07802586e439c85cf89b005",
        )

    if "com_github_johnynek_bazel_jar_jar" not in native.existing_rules():
        http_archive(
            name = "com_github_johnynek_bazel_jar_jar",
            sha256 = "ee227e7f304e9b7f26d033af677f31066f68b1c94ee8f8d04fbecfb371c3caef",
            strip_prefix = "bazel_jar_jar-16e48f319048e090a2fe7fd39a794312d191fc6f",
            urls = ["https://github.com/johnynek/bazel_jar_jar/archive/16e48f319048e090a2fe7fd39a794312d191fc6f.zip"],  # Latest commit SHA as at 2019/02/13
        )

    if "com_github_scalapb_scalapb" not in native.existing_rules():
        http_archive(
            name = "com_github_scalapb_scalapb",
            url = "https://github.com/scalapb/ScalaPB/releases/download/v0.8.0/scalapbc-0.8.0.zip",
            sha256 = "bda0b44b50f0a816342a52c34e6a341b1a792f2a6d26f4f060852f8f10f5d854",
            strip_prefix = "scalapbc-0.8.0/lib",
            build_file_content = """
java_import(
    name = "compilerplugin",
    jars = ["com.thesamet.scalapb.compilerplugin-0.8.0.jar"],
    visibility = ["//visibility:public"],
)
java_import(
    name = "scala-library",
    jars = ["org.scala-lang.scala-library-2.11.12.jar"],
    visibility = ["//visibility:public"],
)
            """,
        )

        if "com_github_googleapis_googleapis" not in native.existing_rules():
            http_archive(
                name = "com_github_googleapis_googleapis",
                strip_prefix = "googleapis-6c48ab5aef47dc14e02e2dc718d232a28067129d",
                urls = ["https://github.com/googleapis/googleapis/archive/6c48ab5aef47dc14e02e2dc718d232a28067129d.tar.gz"],
                sha256 = "70d7be6ad49b4424313aad118c8622aab1c5fdd5a529d4215d3884ff89264a71",
            )

    # Buildifier.
    # It is written in Go and hence needs rules_go to be available.
    if "com_github_bazelbuild_buildtools" not in native.existing_rules():
        http_archive(
            name = "com_github_bazelbuild_buildtools",
            sha256 = "7525deb4d74e3aa4cb2b960da7d1c400257a324be4e497f75d265f2f508c518f",
            strip_prefix = "buildtools-0.22.0",
            url = "https://github.com/bazelbuild/buildtools/archive/0.22.0.tar.gz",
        )

    c2hs_version = "0.28.3"
    c2hs_hash = "80cc6db945ee7c0328043b4e69213b2a1cb0806fb35c8362f9dea4a2c312f1cc"
    c2hs_package_id = "c2hs-{0}".format(c2hs_version)
    c2hs_url = "https://hackage.haskell.org/package/{0}/{1}.tar.gz".format(
        c2hs_package_id,
        c2hs_package_id,
    )
    c2hs_build_file = "//3rdparty/haskell:BUILD.c2hs"
    if "haskell_c2hs" not in native.existing_rules():
        http_archive(
            name = "haskell_c2hs",
            build_file = c2hs_build_file,
            patch_args = ["-p1"],
            patches = ["@com_github_digital_asset_daml//bazel_tools:haskell-c2hs.patch"],
            sha256 = c2hs_hash,
            strip_prefix = c2hs_package_id,
            urls = [c2hs_url],
        )

    native.bind(
        name = "guava",
        actual = "@com_google_guava_guava//jar",
    )
    native.bind(
        name = "gson",
        actual = "@com_google_code_gson_gson//jar",
    )

    if "com_github_google_bazel_common" not in native.existing_rules():
        http_archive(
            name = "com_github_google_bazel_common",
            sha256 = "ccdd09559b49c7efd9e4b0b617b18e2a4bbdb2142fc30dfd3501eb5fa1294dcc",
            strip_prefix = "bazel-common-f3dc1a775d21f74fc6f4bbcf076b8af2f6261a69",
            urls = ["https://github.com/google/bazel-common/archive/f3dc1a775d21f74fc6f4bbcf076b8af2f6261a69.zip"],
        )
