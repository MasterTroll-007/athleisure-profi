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

    init {
        try {
            Files.createDirectories(plansDir)
        } catch (e: IOException) {
            throw RuntimeException("Could not create upload directory", e)
        }
    }

    fun storePlanFile(file: MultipartFile, planId: UUID): String {
        val originalFilename = file.originalFilename ?: "file"
        val extension = originalFilename.substringAfterLast('.', "pdf")
        val filename = "${planId}.${extension}"

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
