package com.theprodeogroup.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Currency

/**
 * The golden vectors for the one VAT rule every FiSH service shares (GL-T-29, GL-FR-TAX-7/8; Femi 2026-10-22: VAT belongs
 * to the product or service and the caller works it out with `Money`). The rule is GL's live rule, kept so that moving the
 * arithmetic changes no posted number: per line, rate as a percent, rounded HALF_EVEN to the currency's minor unit, EXEMPT
 * (a null rate) is zero, and nothing is rounded on a total. `vatInGross` takes the VAT first, by the VAT fraction
 * (rate / (100 + rate)), then the net is the gross less the VAT (Tax Jurisdiction, PR-VT-11, UK VAT Notice 700 7.3.1 and 16.6.2).
 * A change to any vector here is a change to what every service posts: it needs Femi's word.
 */
class VatCalculatorTest {

    private val gbp: Currency = Currency.getInstance("GBP")
    private val jpy: Currency = Currency.getInstance("JPY")

    private fun gbp(amount: String) = Money(BigDecimal(amount), gbp)
    private fun jpy(amount: String) = Money(BigDecimal(amount), jpy)

    // ---- vatOnNet: the net is exclusive of VAT ----

    private fun assertVatOnNet(net: Money, ratePercent: String?, expected: Money) =
        assertEquals(expected, VatCalculator.vatOnNet(net, ratePercent), "VAT on $net at ${ratePercent ?: "exempt"}%")

    @Test
    fun `VAT on a net amount at whole, half and zero percent rates`() {
        assertVatOnNet(gbp("100.00"), "20", gbp("20.00"))
        assertVatOnNet(gbp("100.00"), "13.5", gbp("13.50"))
        assertVatOnNet(gbp("100.00"), "9", gbp("9.00"))
        assertVatOnNet(gbp("100.00"), "0", gbp("0.00"))
        assertVatOnNet(gbp("10.25"), "23", gbp("2.36")) // 2.3575
    }

    @Test
    fun `an exempt line, with no rate, has no VAT`() {
        assertVatOnNet(gbp("100.00"), null, gbp("0.00"))
    }

    @Test
    fun `ties round half even, to the even penny`() {
        assertVatOnNet(gbp("0.25"), "10", gbp("0.02")) // 0.025 -> 0.02
        assertVatOnNet(gbp("0.35"), "10", gbp("0.04")) // 0.035 -> 0.04
        assertVatOnNet(gbp("0.45"), "10", gbp("0.04")) // 0.045 -> 0.04
        assertVatOnNet(gbp("0.55"), "10", gbp("0.06")) // 0.055 -> 0.06
    }

    @Test
    fun `rounding is per line and never on a total`() {
        val perLine = VatCalculator.vatOnNet(gbp("0.25"), "10") + VatCalculator.vatOnNet(gbp("0.25"), "10")
        assertEquals(gbp("0.04"), perLine) // not 0.05, which is the VAT on the 0.50 total
        assertEquals(gbp("0.05"), VatCalculator.vatOnNet(gbp("0.50"), "10"))
    }

    @Test
    fun `a currency with no minor unit rounds to the whole unit, half even`() {
        assertVatOnNet(jpy("1050"), "10", jpy("105"))
        assertVatOnNet(jpy("1005"), "10", jpy("100")) // 100.5 -> 100
        assertVatOnNet(jpy("1015"), "10", jpy("102")) // 101.5 -> 102
    }

    @Test
    fun `a BigDecimal percent gives the same VAT as the string percent`() {
        assertEquals(VatCalculator.vatOnNet(gbp("10.25"), "23"), VatCalculator.vatOnNet(gbp("10.25"), BigDecimal("23")))
    }

    @Test
    fun `a negative or non-numeric rate is refused`() {
        assertThrows(IllegalArgumentException::class.java) { VatCalculator.vatOnNet(gbp("1.00"), "-1") }
        assertThrows(IllegalArgumentException::class.java) { VatCalculator.vatOnNet(gbp("1.00"), "twenty") }
        assertThrows(IllegalArgumentException::class.java) { VatCalculator.vatOnNet(gbp("1.00"), "") }
    }

    // ---- vatInGross: the gross already contains the VAT ----

    private fun assertSplit(gross: Money, ratePercent: String, expectedNet: Money, expectedVat: Money) {
        val split = VatCalculator.vatInGross(gross, ratePercent)
        assertEquals(expectedVat, split.vat, "VAT in $gross at $ratePercent%")
        assertEquals(expectedNet, split.net, "net of $gross at $ratePercent%")
    }

    @Test
    fun `VAT in a gross amount is the VAT fraction of it, and the net is the rest`() {
        assertSplit(gbp("120.00"), "20", gbp("100.00"), gbp("20.00"))
        assertSplit(gbp("125.00"), "25", gbp("100.00"), gbp("25.00"))
        assertSplit(gbp("100.00"), "23", gbp("81.30"), gbp("18.70")) // 18.699...
        assertSplit(gbp("10.00"), "13.5", gbp("8.81"), gbp("1.19"))  // 1.1894...
        assertSplit(gbp("100.00"), "0", gbp("100.00"), gbp("0.00"))
    }

    @Test
    fun `on an exact tie the VAT is taken first and rounded half even, so 3 pence at 20 percent has no VAT`() {
        assertSplit(gbp("0.03"), "20", gbp("0.03"), gbp("0.00")) // VAT 0.005 -> 0.00; net-first would give net 0.02, VAT 0.01
        assertSplit(gbp("0.09"), "20", gbp("0.07"), gbp("0.02")) // VAT 0.015 -> 0.02
    }

    @Test
    fun `net plus VAT is always exactly the gross that was typed`() {
        for (rate in listOf("0", "4.8", "5", "9", "13.5", "15", "20", "23")) {
            for (cents in 1..5000) {
                val gross = Money(BigDecimal(cents).movePointLeft(2), gbp)
                val split = VatCalculator.vatInGross(gross, rate)
                assertEquals(gross, split.net + split.vat, "gross $gross at $rate%")
            }
        }
    }

    @Test
    fun `a gross split in a zero-decimal currency adds back to the gross`() {
        for (rate in listOf("5", "7.5", "18")) {
            for (units in 1..2000) {
                val gross = Money(BigDecimal(units), jpy)
                val split = VatCalculator.vatInGross(gross, rate)
                assertEquals(gross, split.net + split.vat, "gross $gross at $rate%")
            }
        }
    }

    @Test
    fun `a negative rate is refused for a gross split too`() {
        assertThrows(IllegalArgumentException::class.java) { VatCalculator.vatInGross(gbp("1.00"), "-5") }
    }
}
