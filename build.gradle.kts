plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
}

group = "com.theprodeogroup.common"
version = "0.1.0"

repositories {
    mavenCentral()
}

// Same ktorVersion as every consumer repo's own build.gradle.kts
// (GL/SOP/POP/IM/HR), kept in sync by hand for consistency - see the
// comment below on why pinning it here doesn't actually couple their
// builds together.
val ktorVersion = "2.3.12"

// Deliberately a plain Kotlin library, no publishing plugin configured -
// this repo is consumed as a git submodule (its Kotlin source directory
// added directly to each consumer's own sourceSets), not published as a
// versioned Maven artifact. That's the whole point: single source of
// truth (DRY) for Money/ValidationResult without reintroducing a runtime
// dependency, a package registry, or coordinated-release overhead across
// repos (GL/SOP/POP/IM/HR) that are otherwise meant to stay independently
// buildable and independently deployable. See README.md.
//
// Because of that (source dir borrowed, not this build file), these
// main dependencies only matter for compiling/testing this repo on its
// own - a consumer never reads this dependencies block, so pinning
// ktor-client-core/coroutines/serialization here has zero effect on
// GL/SOP/POP/IM/HR, which already declare their own copies of these to
// support the rest of their own codebases.
dependencies {
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}
