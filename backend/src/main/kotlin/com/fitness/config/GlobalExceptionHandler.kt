package com.fitness.config

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<Map<String, Any>> {
        val errors = e.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid value") }
        val firstError = e.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "Validation failed"
        logger.warn("Validation failed: $errors")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(mapOf("error" to firstError, "fields" to errors))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): ResponseEntity<Map<String, String>> {
        logger.warn("IllegalArgumentException: ${e.message}")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(mapOf("error" to (e.message ?: "Bad request")))
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(e: NoSuchElementException): ResponseEntity<Map<String, String>> {
        logger.warn("NoSuchElementException: ${e.message}")
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(mapOf("error" to (e.message ?: "Not found")))
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(e: AccessDeniedException): ResponseEntity<Map<String, String>> {
        logger.warn("AccessDeniedException: ${e.message}")
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(mapOf("error" to (e.message ?: "Access denied")))
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(e: Exception): ResponseEntity<Map<String, String>> {
        // Log full details for debugging but don't expose to client
        logger.error("Unhandled exception: ${e.javaClass.simpleName}: ${e.message}", e)
        // Return generic message without internal details to prevent information leakage
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(mapOf("error" to "Internal server error"))
    }
}
