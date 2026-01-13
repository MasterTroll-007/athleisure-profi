package com.fitness.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.springframework.mock.web.MockMultipartFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.nio.file.Path
import java.util.UUID

class FileStorageServiceTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var fileStorageService: FileStorageService

    @BeforeEach
    fun setup() {
        fileStorageService = FileStorageService(tempDir.toString())
    }

    @Test
    fun `validatePdfFile accepts valid PDF`() {
        // %PDF magic bytes
        val pdfContent = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34)
        val file = MockMultipartFile("file", "test.pdf", "application/pdf", pdfContent)

        // Should not throw
        fileStorageService.validatePdfFile(file)
    }

    @Test
    fun `validatePdfFile rejects file without PDF magic bytes`() {
        // Not a PDF - just random bytes
        val notPdfContent = byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05)
        val file = MockMultipartFile("file", "fake.pdf", "application/pdf", notPdfContent)

        val exception = assertThrows<IllegalArgumentException> {
            fileStorageService.validatePdfFile(file)
        }
        assertEquals("Invalid file: not a valid PDF document", exception.message)
    }

    @Test
    fun `validatePdfFile rejects file that is too small`() {
        // Only 2 bytes - too small
        val tinyContent = byteArrayOf(0x25, 0x50)
        val file = MockMultipartFile("file", "tiny.pdf", "application/pdf", tinyContent)

        val exception = assertThrows<IllegalArgumentException> {
            fileStorageService.validatePdfFile(file)
        }
        assertEquals("Invalid file: too small to be a valid PDF", exception.message)
    }

    @Test
    fun `validatePdfFile rejects file exceeding size limit`() {
        // Create file larger than 10MB
        val largeContent = ByteArray(11 * 1024 * 1024) { 0x00 }
        // Set PDF magic bytes at start
        largeContent[0] = 0x25
        largeContent[1] = 0x50
        largeContent[2] = 0x44
        largeContent[3] = 0x46
        val file = MockMultipartFile("file", "large.pdf", "application/pdf", largeContent)

        val exception = assertThrows<IllegalArgumentException> {
            fileStorageService.validatePdfFile(file)
        }
        assertEquals("File size exceeds maximum allowed size of 10MB", exception.message)
    }

    @Test
    fun `storePlanFile stores valid PDF and returns path`() {
        // Valid PDF content
        val pdfContent = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34)
        val file = MockMultipartFile("file", "test.pdf", "application/pdf", pdfContent)
        val planId = UUID.randomUUID()

        val resultPath = fileStorageService.storePlanFile(file, planId)

        assertEquals("plans/$planId.pdf", resultPath)
        assertTrue(tempDir.resolve("plans/$planId.pdf").toFile().exists())
    }

    @Test
    fun `deletePlanFile removes existing file`() {
        // First store a file
        val pdfContent = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34)
        val file = MockMultipartFile("file", "test.pdf", "application/pdf", pdfContent)
        val planId = UUID.randomUUID()
        val filePath = fileStorageService.storePlanFile(file, planId)

        // Verify file exists
        assertTrue(tempDir.resolve(filePath).toFile().exists())

        // Delete the file
        fileStorageService.deletePlanFile(filePath)

        // Verify file is deleted
        assertTrue(!tempDir.resolve(filePath).toFile().exists())
    }

    @Test
    fun `deletePlanFile handles null path gracefully`() {
        // Should not throw
        fileStorageService.deletePlanFile(null)
    }

    @Test
    fun `deletePlanFile handles blank path gracefully`() {
        // Should not throw
        fileStorageService.deletePlanFile("")
        fileStorageService.deletePlanFile("   ")
    }
}
