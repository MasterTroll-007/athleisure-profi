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
    private val baseDir: Path = Paths.get(uploadDir).toAbsolutePath().normalize()
    private val plansDir: Path = baseDir.resolve("plans")

    companion object {
        // PDF magic bytes: %PDF (0x25 0x50 0x44 0x46)
        private val PDF_MAGIC_BYTES = byteArrayOf(0x25, 0x50, 0x44, 0x46)
        private const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024 // 10MB
        private const val MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024 // 5MB

        // Image magic bytes for validation
        private val JPEG_MAGIC = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        private val PNG_MAGIC = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
        private val WEBP_MAGIC = "RIFF".toByteArray() // + WEBP at offset 8
        private val ALLOWED_IMAGE_EXTENSIONS = setOf("jpeg", "jpg", "png", "webp")
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
        return resolveAndValidatePath(filePath)
    }

    /**
     * Validates that the file is a valid image by checking magic bytes.
     * Prevents uploading malicious files (e.g. SVG with XSS) with spoofed content-type headers.
     */
    fun validateImageFile(file: MultipartFile) {
        if (file.size > MAX_IMAGE_SIZE_BYTES) {
            throw IllegalArgumentException("Image file size exceeds maximum allowed size of 5MB")
        }

        val bytes = file.bytes
        if (bytes.size < 4) {
            throw IllegalArgumentException("Invalid file: too small to be a valid image")
        }

        val isJpeg = bytes.size >= 3 && bytes.sliceArray(0..2).contentEquals(JPEG_MAGIC)
        val isPng = bytes.size >= 4 && bytes.sliceArray(0..3).contentEquals(PNG_MAGIC)
        val isWebp = bytes.size >= 12 &&
                bytes.sliceArray(0..3).contentEquals(WEBP_MAGIC) &&
                String(bytes.sliceArray(8..11)) == "WEBP"

        if (!isJpeg && !isPng && !isWebp) {
            throw IllegalArgumentException("Invalid image file. Only JPEG, PNG and WebP are allowed")
        }
    }

    /**
     * Determines the real extension from magic bytes (not from content-type header).
     */
    fun getImageExtension(file: MultipartFile): String {
        val bytes = file.bytes
        return when {
            bytes.size >= 3 && bytes.sliceArray(0..2).contentEquals(JPEG_MAGIC) -> "jpg"
            bytes.size >= 4 && bytes.sliceArray(0..3).contentEquals(PNG_MAGIC) -> "png"
            bytes.size >= 12 && bytes.sliceArray(0..3).contentEquals(WEBP_MAGIC) -> "webp"
            else -> throw IllegalArgumentException("Unsupported image format")
        }
    }

    /**
     * Stores an image file in the specified subdirectory with path traversal protection.
     */
    fun storeImageFile(file: MultipartFile, subdir: String, filename: String): String {
        validateImageFile(file)
        val ext = getImageExtension(file)
        val safeFilename = "${filename}.$ext"
        val targetDir = baseDir.resolve(subdir).normalize()
        if (!targetDir.startsWith(baseDir)) {
            throw IllegalArgumentException("Invalid storage path")
        }
        Files.createDirectories(targetDir)
        val targetPath = targetDir.resolve(safeFilename).normalize()
        if (!targetPath.startsWith(baseDir)) {
            throw IllegalArgumentException("Invalid file path")
        }
        Files.copy(file.inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)
        return "$subdir/$safeFilename"
    }

    /**
     * Resolves a relative path within the upload directory with path traversal protection.
     */
    fun resolveAndValidatePath(relativePath: String): Path {
        val resolved = baseDir.resolve(relativePath).normalize()
        if (!resolved.startsWith(baseDir)) {
            throw IllegalArgumentException("Invalid file path")
        }
        return resolved
    }
}
