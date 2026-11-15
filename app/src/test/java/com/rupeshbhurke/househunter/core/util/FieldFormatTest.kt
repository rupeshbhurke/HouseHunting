package com.rupeshbhurke.househunter.core.util

import com.rupeshbhurke.househunter.core.data.model.FieldType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

class FieldFormatTest {
    @Test
    fun money_usesRupeeSymbolAndIndianGrouping() {
        assertEquals("₹ 25,000", FieldFormat.display(FieldType.MONEY, null, 25000.0))
        assertEquals("₹ 15,00,000", FieldFormat.display(FieldType.MONEY, null, 1_500_000.0))
    }

    @Test
    fun number_groupsSmallAndLargeValues() {
        assertEquals("0", FieldFormat.number(0.0))
        assertEquals("999", FieldFormat.number(999.0))
        assertEquals("1,000", FieldFormat.number(1000.0))
        assertEquals("1,00,000", FieldFormat.number(100000.0))
        assertEquals("12,34,56,789", FieldFormat.number(123456789.0))
    }

    @Test
    fun number_keepsDecimalsButDropsTrailingZeros() {
        assertEquals("1,250.5", FieldFormat.display(FieldType.NUMBER, null, 1250.5))
        assertEquals("900", FieldFormat.display(FieldType.NUMBER, null, 900.0))
    }

    @Test
    fun rating_showsOutOfFive() {
        assertEquals("★ 4/5", FieldFormat.display(FieldType.RATING, null, 4.0))
    }

    @Test
    fun display_isNullWhenNothingIsStored() {
        assertNull(FieldFormat.display(FieldType.TEXT, "  ", null))
        assertNull(FieldFormat.display(FieldType.MONEY, null, null))
    }

    @Test
    fun date_formatsUtcMidnightMillisWithoutShiftingTheDay() {
        val millis = LocalDate.of(2026, 11, 15).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val text = FieldFormat.date(millis, Locale.ENGLISH)
        assertTrue(text, text.contains("15") && text.contains("2026"))
    }

    @Test
    fun editable_dropsTrailingZeroForWholeNumbers() {
        assertEquals("25000", FieldFormat.editable(FieldType.MONEY, null, 25000.0))
        assertEquals("12.5", FieldFormat.editable(FieldType.NUMBER, null, 12.5))
        assertEquals("", FieldFormat.editable(FieldType.NUMBER, null, null))
        assertEquals("Sharma", FieldFormat.editable(FieldType.TEXT, "Sharma", null))
    }

    @Test
    fun duration_showsMinutesAndSecondsAndHoursOnlyWhenNeeded() {
        assertEquals("0:00", FieldFormat.duration(0))
        assertEquals("0:12", FieldFormat.duration(12_400))
        assertEquals("1:05", FieldFormat.duration(65_000))
        assertEquals("1:02:03", FieldFormat.duration(3_723_000))
    }

    @Test
    fun whatsAppNumber_assumesIndiaForTenDigitNumbers() {
        assertEquals("919876543210", ContactActions.whatsAppNumber("98765 43210"))
    }

    @Test
    fun whatsAppNumber_keepsExistingCountryCode() {
        assertEquals("919876543210", ContactActions.whatsAppNumber("+91 98765-43210"))
        assertEquals("447911123456", ContactActions.whatsAppNumber("+44 7911 123456"))
    }
}
