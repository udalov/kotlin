#!/usr/bin/env bash

set -e

rm -rf build/repo
./gradlew -PkotlinxMetadataDeployVersion=0.0.123 :kotlin-stdlib-common:publish :kotlin-stdlib:publish :kotlinx-metadata-jvm:publish

rm -rf ../kotlin-obfuscation-test-app/kotlin-build-repo
mv build/repo ../kotlin-obfuscation-test-app/kotlin-build-repo
