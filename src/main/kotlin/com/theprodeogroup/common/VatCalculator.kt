package com.theprodeogroup.common

import java.math.BigDecimal
import java.math.MathContext

/**
 * The one VAT rule every FiSH service shares (GL-T-29; Femi 2026-10-22, F22-18 and CBB-FR-EN-8: VAT belongs to the product or
 * service, so the caller works it out with `Money` and sends GL the finished amount). SOP, POP and CBB must not drift, so the
 * arithmetic lives here, beside [Money].
 *
 * The rate is a **percent**, exactly as `GET /companies/{id}/vat-categories` answers it (`"23"`, `"13.5"`, `"0"`), and a `null`
 * rate means EXEMPT (outside the rate arithmetic: no VAT). Nothing here looks a rate up; the caller reads it for the transaction
 * date. A rate is never a float.
 *
 * - [vatOnNet]: the amount is exclusive of VAT. VAT = net x rate, rounded by [Money] to the currency's minor unit, HALF_EVEN. It is
 *   the rule GL has always applied to a sale or purchase line, kept exactly so that moving the arithmetic changes no posted
 *   number. Rounding is **per line**; never round a document total.
 * - [vatInGross]: the amount already contains VAT (a cash or bank book entry). VAT is taken first, by the VAT fraction
 *   rate / (100 + rate), rounded HALF_EVEN; the net is the gross less the VAT, so net plus VAT is always exactly the gross that
 *   was typed (UK VAT Notice 700, 7.3.1 and 16.6.2, via the Tax Jurisdiction session, PR-VT-11). The tie rule (half even) is a
 *   FiSH rule the UK text is silent on; Ireland and Nigeria are not yet researched (Tax item V-13).
 */
object VatCalculator {

    /** A gross amount split into its [net] and the [vat] inside it; they always add back to the gross. */
    data class Split(val net: Money, val vat: Money)

    fun vatOnNet(net: Money, ratePercent: String?): Money =
        if (ratePercent == null) Money(BigDecimal.ZERO, net.currency) else vatOnNet(net, parse(ratePercent))

    fun vatOnNet(net: Money, ratePercent: BigDecimal): Money {
        requireValid(ratePercent)
        return Money(net.amount * ratePercent.movePointLeft(2), net.currency)
    }

    fun vatInGross(gross: Money, ratePercent: String?): Split =
        if (ratePercent == null) Split(gross, Money(BigDecimal.ZERO, gross.currency)) else vatInGross(gross, parse(ratePercent))

    fun vatInGross(gross: Money, ratePercent: BigDecimal): Split {
        requireValid(ratePercent)
        val vat = Money(gross.amount.multiply(ratePercent).divide(BigDecimal(100).add(ratePercent), MathContext(34)), gross.currency)
        return Split(gross - vat, vat)
    }

    private fun parse(ratePercent: String): BigDecimal =
        try {
            BigDecimal(ratePercent.trim())
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("A VAT rate must be a plain decimal percent, such as 23 or 13.5")
        }

    private fun requireValid(ratePercent: BigDecimal) =
        require(ratePercent.signum() >= 0) { "A VAT rate cannot be negative" }
}
