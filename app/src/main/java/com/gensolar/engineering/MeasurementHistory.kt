package com.gensolar.engineering

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class MeasurementRecord(
    val id: String,
    val category: String,
    val title: String,
    val result: String,
    val details: String,
    val createdAtEpochMs: Long
)

object MeasurementHistoryCodec {
    fun validationError(record: MeasurementRecord): String? {
        if (record.id.isBlank() || record.id.length > 80) return "Invalid record identifier"
        if (record.category.isBlank() || record.category.length > 40) return "Invalid category"
        if (record.title.isBlank() || record.title.length > 120) return "Invalid title"
        if (record.result.isBlank() || record.result.length > 300) return "Invalid result"
        if (record.details.length > 2000) return "Measurement details are too long"
        if (record.createdAtEpochMs <= 0L) return "Invalid measurement timestamp"
        return null
    }

    fun withoutId(records: List<MeasurementRecord>, id: String): List<MeasurementRecord> =
        records.filterNot { it.id == id }

    fun toCsv(records: List<MeasurementRecord>): String {
        val rows = records.map { record ->
            listOf(
                record.id,
                record.createdAtEpochMs.toString(),
                record.category,
                record.title,
                record.result,
                record.details
            ).joinToString(",") { csvCell(it) }
        }
        return (listOf("id,created_at_epoch_ms,category,title,result,details") + rows).joinToString("\n", postfix = "\n")
    }

    private fun csvCell(value: String): String {
        val first = value.trimStart().firstOrNull()
        val safe = if (first == '=' || first == '+' || first == '-' || first == '@') "'$value" else value
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }
}

class MeasurementHistoryStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    @Synchronized
    fun load(): List<MeasurementRecord> = readRecords() ?: emptyList()

    @Synchronized
    fun append(record: MeasurementRecord): Boolean {
        if (MeasurementHistoryCodec.validationError(record) != null) return false
        val existing = readRecords() ?: return false
        val updated = (existing.filterNot { it.id == record.id } + record).takeLast(MAX_RECORDS)
        return writeRecords(updated)
    }

    @Synchronized
    fun remove(recordId: String): Boolean {
        if (recordId.isBlank()) return false
        val existing = readRecords() ?: return false
        if (existing.none { it.id == recordId }) return false
        return writeRecords(MeasurementHistoryCodec.withoutId(existing, recordId))
    }

    @Synchronized
    fun clearAll(): Boolean {
        val existing = readRecords() ?: return false
        if (existing.isEmpty()) return true
        return preferences.edit().remove(KEY).commit()
    }

    private fun writeRecords(records: List<MeasurementRecord>): Boolean {
        val json = JSONArray()
        records.forEach { item ->
            json.put(
                JSONObject()
                    .put("id", item.id)
                    .put("category", item.category)
                    .put("title", item.title)
                    .put("result", item.result)
                    .put("details", item.details)
                    .put("createdAtEpochMs", item.createdAtEpochMs)
            )
        }
        return preferences.edit().putString(KEY, json.toString()).commit()
    }

    private fun readRecords(): List<MeasurementRecord>? {
        val raw = preferences.getString(KEY, null) ?: return emptyList()
        return try {
            val json = JSONArray(raw)
            val records = mutableListOf<MeasurementRecord>()
            for (index in 0 until json.length()) {
                val item = json.optJSONObject(index) ?: return null
                val record = MeasurementRecord(
                    id = item.optString("id", ""),
                    category = item.optString("category", ""),
                    title = item.optString("title", ""),
                    result = item.optString("result", ""),
                    details = item.optString("details", ""),
                    createdAtEpochMs = item.optLong("createdAtEpochMs", 0L)
                )
                if (MeasurementHistoryCodec.validationError(record) != null) return null
                records.add(record)
            }
            records.takeLast(MAX_RECORDS)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val PREFERENCES = "gensolar_measurement_history"
        private const val KEY = "records_v1"
        private const val MAX_RECORDS = 200
    }
}
