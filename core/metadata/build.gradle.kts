plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("gradle-plugin-compiler-dependency-configuration")
    id("com.squareup.wire") version "5.3.10"
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_8)

dependencies {
    api(protobufLite())
    api(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

wire {
    protoPath {
        srcDir("src")
        include("google/protobuf/descriptor.proto")
    }
    sourcePath {
        srcDir("src")
        include("ext_options.proto")
        include("metadata.proto")
    }
    java {}
}
