plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("gradle-plugin-compiler-dependency-configuration")
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_8)

dependencies {
    api(protobufLite())
    api(kotlinStdlib())
    api("us.hebi.quickbuf:quickbuf-runtime:1.4")
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
