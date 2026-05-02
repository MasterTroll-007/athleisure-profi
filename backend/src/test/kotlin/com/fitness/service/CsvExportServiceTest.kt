package com.fitness.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CsvExportServiceTest {
    private val service = CsvExportService()

    @Test
    fun `csv cells starting like spreadsheet formulas are neutralized`() {
        assertThat(service.escapeCell("=cmd")).isEqualTo("'=cmd")
        assertThat(service.escapeCell("+cmd")).isEqualTo("'+cmd")
        assertThat(service.escapeCell("-cmd")).isEqualTo("'-cmd")
        assertThat(service.escapeCell("@cmd")).isEqualTo("'@cmd")
        assertThat(service.escapeCell(" \t=cmd")).isEqualTo("\"' \t=cmd\"")
    }

    @Test
    fun `csv escaping preserves commas quotes and line breaks`() {
        assertThat(service.escapeCell("name, \"quoted\"\nnext"))
            .isEqualTo("\"name, \"\"quoted\"\"\nnext\"")
    }
}
