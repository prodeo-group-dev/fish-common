package com.theprodeogroup.common

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ValidationResultTest {

    @Test
    fun `success() produces a valid result with no errors or warnings`() {
        val result = ValidationResult.success()

        result.isValid shouldBe true
        result.errors shouldBe emptyList()
        result.warnings shouldBe emptyList()
    }

    @Test
    fun `failure() produces an invalid result carrying the given errors`() {
        val result = ValidationResult.failure("first error", "second error")

        result.isValid shouldBe false
        result.errors shouldBe listOf("first error", "second error")
        result.hasErrors() shouldBe true
    }

    @Test
    fun `withWarnings() produces a valid result carrying the given warnings`() {
        val result = ValidationResult.withWarnings("a warning")

        result.isValid shouldBe true
        result.warnings shouldBe listOf("a warning")
        result.hasWarnings() shouldBe true
    }

    @Test
    fun `combine() is valid only if both inputs are valid`() {
        val bothValid = ValidationResult.success().combine(ValidationResult.success())
        val oneInvalid = ValidationResult.success().combine(ValidationResult.failure("bad"))

        bothValid.isValid shouldBe true
        oneInvalid.isValid shouldBe false
    }

    @Test
    fun `combine() merges errors and warnings from both sides`() {
        val a = ValidationResult(isValid = false, errors = listOf("error A"), warnings = listOf("warning A"))
        val b = ValidationResult(isValid = false, errors = listOf("error B"), warnings = listOf("warning B"))

        val combined = a.combine(b)

        combined.errors shouldBe listOf("error A", "error B")
        combined.warnings shouldBe listOf("warning A", "warning B")
    }

    @Test
    fun `hasErrors() and hasWarnings() are false when both lists are empty`() {
        val result = ValidationResult.success()

        result.hasErrors() shouldBe false
        result.hasWarnings() shouldBe false
    }
}
