plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("com.squareup.wire") version "5.3.10"
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_8)

dependencies {
    api(project(":core:metadata"))
    implementation("com.squareup.wire:wire-compiler:5.3.10")
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

wire {
    protoPath {
        srcDir(rootProject.projectDir.path + "/core/metadata/src")
        include("ext_options.proto")
        include("google/protobuf/descriptor.proto")
        include("metadata.proto")
    }
    sourcePath {
        srcDir("src")
        include("jvm_metadata.proto")
    }
    java {}
}
