load("@rules_jvm_external//:defs.bzl", "artifact")
load("@io_bazel_rules_kotlin//kotlin:android.bzl", "kt_android_library")
load("@io_bazel_rules_kotlin//kotlin:core.bzl", "kt_compiler_plugin")
load("@rules_java//java:defs.bzl", "java_lite_proto_library")
load("@rules_proto//proto:defs.bzl", "proto_library")
load("@io_bazel_rules_kotlin//kotlin:android.bzl", "kt_android_local_test")

package(
    default_visibility = ["//visibility:public"],
)

kt_compiler_plugin(
    name = "open_for_testing_plugin",
    id = "org.jetbrains.kotlin.allopen",
    options = {
        "annotation": "plugin.allopen.OpenForTesting",
    },
    deps = [
        "@com_github_jetbrains_kotlin//:allopen-compiler-plugin",
    ],
)

proto_library(
    name = "uwbinfo_proto",
    srcs = ["main/java/com/google/location/nearby/apps/uwbranging/impl/uwbinfo.proto"],
)

java_lite_proto_library(
    name = "uwbinfo_java_proto_lite",
    deps = [":uwbinfo_proto"],
)

kt_android_library(
    name = "uwb_ranging_lib",
    srcs = glob([
        "main/java/com/google/location/nearby/apps/uwbranging/*.kt",
        "main/java/com/google/location/nearby/apps/uwbranging/impl/*.kt",
    ]),
    plugins = [
        ":open_for_testing_plugin",
    ],
    deps = [
        ":uwbinfo_java_proto_lite",
        artifact("androidx.core:core"),
        artifact("androidx.core.uwb:uwb"),
        artifact("com.google.guava:guava"),
        artifact("com.google.android.gms:play-services-nearby"),
        artifact("org.jetbrains.kotlinx:kotlinx-coroutines-core"),
        artifact("org.jetbrains.kotlinx:kotlinx-coroutines-android"),
        artifact("org.jetbrains.kotlinx:kotlinx-coroutines-play-services"),
        artifact("com.google.protobuf:protobuf-java"),
    ],
)

kt_android_local_test(
    name="NearbyConnectionsTest",
    test_class = "com.google.location.nearby.apps.uwbranging.impl.NearbyConnectionsTest",
    srcs=glob([
        "androidTest/java/com/google/location/nearby/apps/uwbranging/impl/NearbyConnectionsTest.kt",
        "main/java/com/google/location/nearby/apps/uwbranging/*.kt",
        "main/java/com/google/location/nearby/apps/uwbranging/impl/*.kt",
    ]),
    manifest = "AndroidManifest.xml",
    deps=[
        "@maven//:org_robolectric_robolectric",
        "@robolectric//bazel:android-all",
        artifact("junit:junit"),
        artifact("androidx.test:core"),
        ":uwbinfo_java_proto_lite",
        artifact("androidx.core:core"),
        artifact("androidx.core.uwb:uwb"),
        artifact("com.google.guava:guava"),
        artifact("com.google.android.gms:play-services-nearby"),
        artifact("com.google.android.gms:play-services-tasks"),
        artifact("com.google.truth:truth"),
        artifact("org.jetbrains.kotlinx:kotlinx-coroutines-core"),
        artifact("org.jetbrains.kotlinx:kotlinx-coroutines-android"),
        artifact("org.jetbrains.kotlinx:kotlinx-coroutines-test"),
        artifact("org.jetbrains.kotlinx:kotlinx-coroutines-play-services"),
        artifact("com.google.protobuf:protobuf-java"),
        artifact("org.mockito.kotlin:mockito-kotlin"),
        artifact("org.mockito:mockito-inline"),
    ],
)

kt_android_local_test(
    name="NearbyControleeConnectorTest",
    test_class = "com.google.location.nearby.apps.uwbranging.impl.NearbyControleeConnectorTest",
    srcs=glob([
        "androidTest/java/com/google/location/nearby/apps/uwbranging/impl/NearbyControleeConnectorTest.kt",
        "main/java/com/google/location/nearby/apps/uwbranging/*.kt",
        "main/java/com/google/location/nearby/apps/uwbranging/impl/*.kt",
    ]),
    manifest = "AndroidManifest.xml",
    deps=[
        "@maven//:org_robolectric_robolectric",
        "@robolectric//bazel:android-all",
        artifact("junit:junit"),
        artifact("androidx.test:core"),
        ":uwbinfo_java_proto_lite",
        artifact("androidx.core:core"),
        artifact("androidx.core.uwb:uwb"),
        artifact("com.google.guava:guava"),
        artifact("com.google.android.gms:play-services-nearby"),
        artifact("com.google.android.gms:play-services-tasks"),
        artifact("com.google.truth:truth"),
        artifact("org.jetbrains.kotlinx:kotlinx-coroutines-core"),
        artifact("org.jetbrains.kotlinx:kotlinx-coroutines-android"),
        artifact("org.jetbrains.kotlinx:kotlinx-coroutines-test"),
        artifact("org.jetbrains.kotlinx:kotlinx-coroutines-play-services"),
        artifact("com.google.protobuf:protobuf-java"),
        artifact("org.mockito.kotlin:mockito-kotlin"),
        artifact("org.mockito:mockito-inline"),
    ],
)

kt_android_local_test(
    name="NearbyControllerConnectorTest",
    test_class = "com.google.location.nearby.apps.uwbranging.impl.NearbyControllerConnectorTest",
    srcs=glob([
        "androidTest/java/com/google/location/nearby/apps/uwbranging/impl/NearbyControllerConnectorTest.kt",
        "main/java/com/google/location/nearby/apps/uwbranging/*.kt",
        "main/java/com/google/location/nearby/apps/uwbranging/impl/*.kt",
    ]),
    manifest = "AndroidManifest.xml",
    deps=[
        "@maven//:org_robolectric_robolectric",
        "@robolectric//bazel:android-all",
        artifact("junit:junit"),
        artifact("androidx.test:core"),
        ":uwbinfo_java_proto_lite",
        artifact("androidx.core:core"),
        artifact("androidx.core.uwb:uwb"),
        artifact("com.google.guava:guava"),
        artifact("com.google.android.gms:play-services-nearby"),
        artifact("com.google.android.gms:play-services-tasks"),
        artifact("com.google.truth:truth"),
        artifact("org.jetbrains.kotlinx:kotlinx-coroutines-core"),
        artifact("org.jetbrains.kotlinx:kotlinx-coroutines-android"),
        artifact("org.jetbrains.kotlinx:kotlinx-coroutines-test"),
        artifact("org.jetbrains.kotlinx:kotlinx-coroutines-play-services"),
        artifact("com.google.protobuf:protobuf-java"),
        artifact("org.mockito.kotlin:mockito-kotlin"),
        artifact("org.mockito:mockito-inline"),
    ],
)

kt_android_local_test(
    name="UwbSessionScopeImplTest",
    test_class = "com.google.location.nearby.apps.uwbranging.impl.UwbSessionScopeImplTest",
    srcs=glob([
        "androidTest/java/com/google/location/nearby/apps/uwbranging/impl/UwbSessionScopeImplTest.kt",
        "main/java/com/google/location/nearby/apps/uwbranging/*.kt",
        "main/java/com/google/location/nearby/apps/uwbranging/impl/*.kt",
    ]),
    manifest = "AndroidManifest.xml",
    deps=[
        "@maven//:org_robolectric_robolectric",
        "@robolectric//bazel:android-all",
        artifact("junit:junit"),
        artifact("androidx.test:core"),
        ":uwbinfo_java_proto_lite",
        artifact("androidx.core:core"),
        artifact("androidx.core.uwb:uwb"),
        artifact("com.google.guava:guava"),
        artifact("com.google.android.gms:play-services-nearby"),
        artifact("com.google.android.gms:play-services-tasks"),
        artifact("com.google.truth:truth"),
        artifact("org.jetbrains.kotlinx:kotlinx-coroutines-core"),
        artifact("org.jetbrains.kotlinx:kotlinx-coroutines-android"),
        artifact("org.jetbrains.kotlinx:kotlinx-coroutines-test"),
        artifact("org.jetbrains.kotlinx:kotlinx-coroutines-play-services"),
        artifact("com.google.protobuf:protobuf-java"),
        artifact("org.mockito.kotlin:mockito-kotlin"),
        artifact("org.mockito:mockito-inline"),
    ],
)
