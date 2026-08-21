plugins {
    kotlin("jvm") version "2.0.21"
}

group = "com.theprodeogroup.common"
version = "0.1.0"

repositories {
    mavenCentral()
}

// Deliberately a plain Kotlin library, no publishing plugin configured -
// this repo is consumed as a git submodule (its Kotlin source directory
// added directly to each consumer's own sourceSets), not published as a
// versioned Maven artifact. That's the whole point: single source of
// truth (DRY) for Money/ValidationResult without reintroducing a runtime
// dependency, a package registry, or coordinated-release overhead across
// repos (GL/SOP/POP/IM/HR) that are otherwise meant to stay independently
// buildable and independently deployable. See README.md.
dependencies {
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
