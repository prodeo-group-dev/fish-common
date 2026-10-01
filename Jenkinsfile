// common's Jenkins pipeline (2026-10-01) - replaces .github/workflows/ci.yml
// for this repo, given GitHub Actions is confirmed disabled account-wide
// (see every sibling Jenkinsfile's own header for the full story). ci.yml
// is left in place, dormant, in case Actions ever comes back - same
// precedent GL's pipeline.yml already set.
//
// Test-only, no deploy stage: common is a plain Kotlin library with no
// database, no HTTP layer, and no Docker image - it's consumed as a git
// submodule by GL/SOP/POP/IM/HR (its Kotlin source directory added
// directly to each consumer's own sourceSets), never published or
// deployed on its own (see README.md). The real risk this closes: with
// no CI at all, a broken change here could silently propagate into five
// services at once the next time any of them pulls this submodule
// forward, caught only whenever that consumer's own build happens to
// run - this gives it its own test gate on every PR and master push,
// the same discipline every other repo already has.

pipeline {
    agent any

    options {
        timeout(time: 10, unit: 'MINUTES')
        disableConcurrentBuilds()
        throttleJobProperty(
            categories: ['fish-family'],
            throttleEnabled: true,
            throttleOption: 'category',
            limitOneJobWithMatchingParams: false
        )
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'chmod +x gradlew'
            }
        }

        stage('Unit tests') {
            steps {
                sh './gradlew test --console=plain'
            }
            post {
                always {
                    junit testResults: 'build/test-results/test/*.xml', allowEmptyResults: true
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
    }
}
