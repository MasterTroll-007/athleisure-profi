package com.fitness.dto

import org.springframework.data.domain.Page

data class PageDTO<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
    val size: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

fun <T, R> Page<T>.toPageDTO(mapper: (T) -> R): PageDTO<R> = PageDTO(
    content = content.map(mapper),
    totalElements = totalElements,
    totalPages = totalPages,
    page = number,
    size = size,
    hasNext = hasNext(),
    hasPrevious = hasPrevious()
)

fun <T> Page<T>.toPageDTO(): PageDTO<T> = toPageDTO { it }
