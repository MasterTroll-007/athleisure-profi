package com.fitness.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID

@Service
class FileStorageService(
    @Value("\${file.upload-dir:uploads}") private val uploadDir: String
) {
    private val plansDir: Path = Paths.get(uploadDir, "plans")

    companion object {
        // PDF magic bytes: %PDF (0x25 0x50 0x44 0x46)
        private val PDF_MAGIC_BYTES = byteArrayOf(0x25, 0x50, 0x44, 0x46)
        private const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024 // 10MB
    }

    init {
        try {
            Files.createDirectories(plansDir)
        } catch (e: IOException) {
            throw RuntimeException("Could not create upload directory", e)
        }
    }

    /**
     * Validates that the file is a valid PDF by checking magic bytes.
     * This prevents uploading malicious files with spoofed content-type headers.
     */
    fun validatePdfFile(file: MultipartFile) {
        // Check file size
        if (file.size > MAX_FILE_SIZE_BYTES) {
            throw IllegalArgumentException("File size exceeds maximum allowed size of 10MB")
        }

        // Check magic bytes
        val bytes = file.bytes
        if (bytes.size < PDF_MAGIC_BYTES.size) {
            throw IllegalArgumentException("Invalid file: too small to be a valid PDF")
        }

        val header = bytes.sliceArray(0 until PDF_MAGIC_BYTES.size)
        if (!header.contentEquals(PDF_MAGIC_BYTES)) {
            throw IllegalArgumentException("Invalid file: not a valid PDF document")
        }
    }

    fun storePlanFile(file: MultipartFile, planId: UUID): String {
        // Validate file content before storing
        validatePdfFile(file)

        val filename = "${planId}.pdf"
        val targetPath = plansDir.resolve(filename)

        try {
            Files.copy(file.inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)
            return "plans/$filename"
        } catch (e: IOException) {
            throw RuntimeException("Failed to store file", e)
        }
    }

    fun deletePlanFile(filePath: String?) {
        if (filePath.isNullOrBlank()) return

        try {
            val path = Paths.get(uploadDir, filePath)
            Files.deleteIfExists(path)
        } catch (e: IOException) {
            // Log error but don't throw - file deletion is not critical
        }
    }

    fun getPlanFilePath(filePath: String): Path {
        return Paths.get(uploadDir, filePath)
    }
}
