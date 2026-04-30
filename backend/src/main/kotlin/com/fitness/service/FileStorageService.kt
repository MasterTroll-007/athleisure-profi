package com.fitness.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

@Service
class FileStorageService(
    @Value("\${file.upload-dir:uploads}") private val uploadDir: String
) {
    private val baseDir: Path = Paths.get(uploadDir).toAbsolutePath().normalize()

    companion object {
        private const val MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024 // 5MB

        // Image magic bytes for validation
        private val JPEG_MAGIC = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        private val PNG_MAGIC = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
        private val WEBP_MAGIC = "RIFF".toByteArray() // + WEBP at offset 8
        private val ALLOWED_IMAGE_EXTENSIONS = setOf("jpeg", "jpg", "png", "webp")
    }

    init {
        try {
            Files.createDirectories(baseDir)
        } catch (e: IOException) {
            throw RuntimeException("Could not create upload directory", e)
        }
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
