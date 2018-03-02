plugins {
    kotlin("jvm")
    id("jps-compatible")
}

sourceSets {
    "main" { }
    "proto" { java.srcDirs("proto") }
    "protoCompare" { java.srcDirs("protoCompare") }
    "test" { projectDefault() }
}

dependencies {
    val protoCompile by configurations
    val protoRuntime by configurations
    val protoCompareCompile by configurations

    compile(projectTests(":compiler:cli"))
    compile(projectTests(":idea:idea-maven"))
    compile(projectTests(":j2k"))
    compile(projectTests(":idea:idea-android"))
    compile(projectTests(":jps-plugin"))
    compile(projectTests(":plugins:android-extensions-compiler"))
    compile(projectTests(":plugins:android-extensions-ide"))
    compile(projectTests(":plugins:android-extensions-jps"))
    compile(projectTests(":kotlin-annotation-processing"))
    compile(projectTests(":kotlin-allopen-compiler-plugin"))
    compile(projectTests(":kotlin-noarg-compiler-plugin"))
    compile(projectTests(":kotlin-sam-with-receiver-compiler-plugin"))
    compile(projectTests(":generators:test-generator"))
    testCompileOnly(intellijDep("jps-build-test"))
    testCompileOnly(project(":kotlin-reflect-api"))
    testCompile(intellijDep("jps-build-test"))
    testRuntime(intellijDep()) { includeJars("idea_rt") }
    testRuntime(projectDist(":kotlin-reflect"))

    protoCompile(project(":kotlin-stdlib"))
    protoCompile(intellijDep()) { includeJars("openapi", "util") }
    protoRuntime(intellijDep()) { includeJars("trove4j") }

    protoCompareCompile(projectTests(":kotlin-build-common"))
    protoCompareCompile(projectTests(":generators:test-generator"))
}

projectTest {
    workingDir = rootDir
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateTestsKt")

val generateProtoBuf by generator("org.jetbrains.kotlin.generators.protobuf.GenerateProtoBufKt", sourceSet = "proto")
val generateProtoBufCompare by generator("org.jetbrains.kotlin.generators.protobuf.GenerateProtoBufCompare", sourceSet = "protoCompare")

val generateGradleOptions by generator("org.jetbrains.kotlin.generators.arguments.GenerateGradleOptionsKt")

testsJar()
