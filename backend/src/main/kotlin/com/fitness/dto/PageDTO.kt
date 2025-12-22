package com.fitness.dto

data class PageDTO<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
    val size: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)
