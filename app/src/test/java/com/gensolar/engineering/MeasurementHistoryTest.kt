package com.gensolar.engineering

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementHistoryTest {
    private fun record(
        id: String = "record-1",
        category: String = "Room",
        title: String = "Room dimensions",
        result: String = "20.00 m² floor",
        details: String = "Length: 5 m; width: 4 m",
        createdAt: Long = 1_763_400_000_000L
    ) = MeasurementRecord(id, category, title, result, details, createdAt)

    @Test
    fun acceptsCompleteMeasurementRecord() {
        assertNull(MeasurementHistoryCodec.validationError(record()))
    }

    @Test
    fun rejectsBlankOrOversizedFieldsAndInvalidTimestamp() {
        assertFalse(MeasurementHistoryCodec.validationError(record(title = " ")) == null)
        assertFalse(MeasurementHistoryCodec.validationError(record(category = "x".repeat(41))) == null)
        assertFalse(MeasurementHistoryCodec.validationError(record(details = "x".repeat(2001))) == null)
        assertFalse(MeasurementHistoryCodec.validationError(record(createdAt = 0L)) == null)
    }

    @Test
    fun removesOnlyTheSelectedRecordAndPreservesOrder() {
        val first = record(id = "first")
        val second = record(id = "second")
        val third = record(id = "third")
        assertEquals(listOf(first, third), MeasurementHistoryCodec.withoutId(listOf(first, second, third), "second"))
        assertEquals(listOf(first, second, third), MeasurementHistoryCodec.withoutId(listOf(first, second, third), "missing"))
    }

    @Test
    fun filtersByCategoryAndCaseInsensitiveSearchWithoutReordering() {
        val room = record(id = "room", category = "Room", title = "Main hall", result = "20 m²")
        val roof = record(id = "roof", category = "Solar roof", title = "East roof", result = "12 panels")
        val gps = record(id = "gps", category = "GPS", title = "Site boundary", result = "125 m")
        val records = listOf(room, roof, gps)
        assertEquals(listOf(gps), MeasurementHistoryCodec.filter(records, "All", "SITE"))
        assertEquals(listOf(roof), MeasurementHistoryCodec.filter(records, "Solar roof", "EAST"))
        assertEquals(emptyList<MeasurementRecord>(), MeasurementHistoryCodec.filter(records, "Room", "inverter"))
        assertEquals(records, MeasurementHistoryCodec.filter(records, "All", ""))
    }

    @Test
    fun csvEscapesCommasQuotesAndLineBreaks() {
        val csv = MeasurementHistoryCodec.toCsv(
            listOf(record(title = "Roof, east", details = "Survey note: \"checked\"\nsecond line"))
        )
        assertTrue(csv.startsWith("id,created_at_epoch_ms,category,title,result,details\n"))
        assertTrue(csv.contains("\"Roof, east\""))
        assertTrue(csv.contains("\"Survey note: \"\"checked\"\"\nsecond line\""))
    }

    @Test
    fun csvNeutralizesSpreadsheetFormulaCells() {
        val csv = MeasurementHistoryCodec.toCsv(listOf(record(title = "=IMPORTXML(\"url\")")))
        assertTrue(csv.contains("\"'=IMPORTXML(\"\"url\"\")\""))
    }

    @Test
    fun csvPreservesInputOrderAndIncludesEveryRecord() {
        val first = record(id = "first", createdAt = 100L)
        val second = record(id = "second", createdAt = 200L)
        val csv = MeasurementHistoryCodec.toCsv(listOf(first, second))
        assertTrue(csv.indexOf("\"first\"") < csv.indexOf("\"second\""))
        assertEquals(3, csv.trimEnd().lines().size)
    }
}
