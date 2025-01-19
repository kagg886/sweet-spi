/*
 * Copyright (c) 2024-2025 Oleg Yukhnevich. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("UnstableApiUsage", "HasPlatformType")

import sweetbuild.*

plugins {
    id("sweetbuild.kotlin-base")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.powerAssert)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        optIn.add("kotlin.io.path.ExperimentalPathApi")
    }
}

dependencies {
    testImplementation(gradleTestKit())
    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.junit.params)
}

// Dev artifacts for tests resolution - inspired by https://github.com/adamko-dev/dev-publish-plugin

val devArtifacts by configurations.dependencyScope("devArtifacts")

val devArtifactsResolver by configurations.resolvable("devArtifactsResolver") {
    devArtifactAttributes(objects)
    extendsFrom(devArtifacts)
}

dependencies {
    devArtifacts(projects.sweetspiBom)
    devArtifacts(projects.sweetspiRuntime)
    devArtifacts(projects.sweetspiProcessor)
    devArtifacts(projects.sweetspiGradlePlugin)
}

tasks.test {
    useJUnitPlatform()
    jvmArgumentProviders.add(
        TestsArgumentProvider(
            devArtifactsDirectories = devArtifactsResolver.incoming.files,
            testKitDirectory = layout.buildDirectory.dir("test-kit"),
            projectVersion = provider { project.version.toString() }
        )
    )
}

class TestsArgumentProvider(
    private val devArtifactsDirectories: FileCollection,
    private val testKitDirectory: Provider<Directory>,
    @get:Input
    val projectVersion: Provider<String>,
) : CommandLineArgumentProvider {

    // we don't need to track ALL content because of flexible timestamps
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    val devArtifactsDirectoriesInput
        get() = devArtifactsDirectories.asFileTree
            // exclude metadata with timestamp which changes with every publication
            .matching { exclude("**/maven-metadata.*") }
            // convert to provider
            .elements
            // fileTrees have an unstable order
            .map { files -> files.map { it.asFile }.sorted() }

    // no need to track content, just path
    @get:Input
    val testKitDirectoryPath get() = testKitDirectory.map { it.asFile.canonicalPath }

    private val devArtifactsDirectoriesList: Provider<String>
        get() = devArtifactsDirectories.elements.map { directories ->
            directories.joinToString(",") { it.asFile.canonicalFile.invariantSeparatorsPath }
        }

    override fun asArguments(): Iterable<String> = listOf(
        "-Dorg.gradle.testkit.dir=${testKitDirectoryPath.get()}",
        "-Dsweettests.dev-artifacts-directories=${devArtifactsDirectoriesList.get()}",
        "-Dsweettests.dev-artifacts-version=${projectVersion.get()}",
    )
}
