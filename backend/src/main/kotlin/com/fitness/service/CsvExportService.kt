package com.fitness.service

import org.springframework.stereotype.Service

@Service
class CsvExportService {
    fun buildCsv(rows: List<List<String>>): ByteArray {
        val csv = rows.joinToString("\n") { row ->
            row.joinToString(",") { escapeCell(it) }
        } + "\n"
        return csv.toByteArray(Charsets.UTF_8)
    }

    fun escapeCell(value: String): String {
        val normalized = value.replace("\r\n", "\n").replace("\r", "\n")
        val safeValue = if (startsLikeSpreadsheetFormula(normalized)) "'$normalized" else normalized
        return if (safeValue.needsCsvQuoting()) {
            "\"" + safeValue.replace("\"", "\"\"") + "\""
        } else {
            safeValue
        }
    }

    private fun startsLikeSpreadsheetFormula(value: String): Boolean {
        val firstSignificantChar = value.dropWhile { it == ' ' }.firstOrNull() ?: return false
        return firstSignificantChar == '=' ||
            firstSignificantChar == '+' ||
            firstSignificantChar == '-' ||
            firstSignificantChar == '@' ||
            firstSignificantChar == '\t'
    }

    private fun String.needsCsvQuoting(): Boolean =
        any { it == ',' || it == '"' || it == '\n' || it == '\t' } ||
            firstOrNull()?.isWhitespace() == true ||
            lastOrNull()?.isWhitespace() == true
}
