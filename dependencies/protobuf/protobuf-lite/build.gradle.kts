import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.2.0"
    `java-base`
    `maven-publish`
    id("com.gradleup.shadow") version "8.3.0" apply false
}

repositories {
    mavenCentral()
}

val relocatedProtobuf by configurations.creating
val relocatedProtobufSources by configurations.creating

val protobufVersion: String by rootProject.extra

val renamedSources = "${layout.buildDirectory.get()}/renamedSrc/"
val outputJarsPath = "${layout.buildDirectory.get()}/libs"

dependencies {
    relocatedProtobuf("com.google.protobuf:protobuf-javalite:$protobufVersion") { isTransitive = false }
    relocatedProtobufSources("com.google.protobuf:protobuf-javalite:$protobufVersion:sources") { isTransitive = false }
}

val prepare = tasks.register<ShadowJar>("prepare") {
    destinationDirectory.set(File(outputJarsPath))
    archiveVersion.set(protobufVersion)
    archiveClassifier.set("")
    // from(sourceSets.main.get().output)
    from(relocatedProtobuf) {
        include("**/*")
    }

    relocate("com.google.protobuf", "org.jetbrains.kotlin.protobuf" ) {
        exclude("META-INF/maven/com.google.protobuf/protobuf-javalite/pom.properties")
    }
    relocate("sun.misc", "org.jetbrains.kotlin.protobuf.sun.misc")
}

@Suppress("DEPRECATION")
val relocateSources = task<Copy>("relocateSources") {
    from(
        provider {
            zipTree(relocatedProtobufSources.files.single())
        }
    )

    into(renamedSources)

    filter { it.replace("com.google.protobuf", "org.jetbrains.kotlin.protobuf") }
}

@Suppress("DEPRECATION")
val prepareSources = task<Jar>("prepareSources") {
    destinationDirectory.set(File(outputJarsPath))
    archiveVersion.set(protobufVersion)
    archiveClassifier.set("sources")
    from(relocateSources)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifact(prepare)
            artifact(prepareSources)
        }
    }

    repositories {
        maven {
            @Suppress("DEPRECATION")
            url = uri("${rootProject.buildDir}/internal/repo")
        }

        maven {
            name = "kotlinSpace"
            url = uri("https://redirector.kotlinlang.org/maven/kotlin-dependencies")
            credentials(org.gradle.api.artifacts.repositories.PasswordCredentials::class)
        }
    }
}
