package com.nikkras.dosering.domain

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.floor

/** Formatting and parsing of dose amounts like "400 mg" or "2 sprays". */
object Doses {

    val UNITS = listOf("mg", "ml", "spray", "tablet", "capsule", "lozenge", "sachet", "drop")

    private val MEASURES = setOf("mg", "mcg", "g", "ml")

    fun format(amount: Double, unit: String): String = "${number(amount)} ${unitLabel(amount, unit)}"

    fun unitLabel(amount: Double, unit: String): String =
        if (unit in MEASURES || amount == 1.0) unit else unit + "s"

    fun number(value: Double): String =
        if (value == floor(value) && value < 1e12) {
            value.toLong().toString()
        } else {
            BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        }

    /** Accepts "2", "2.5" and "2,5". Returns null for anything that is not a positive number. */
    fun parse(text: String): Double? =
        text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 && it.isFinite() }
}
