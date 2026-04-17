package com.fitness.app.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class TrainingLocationDTO(
    val id: String,
    val nameCs: String,
    val nameEn: String? = null,
    val addressCs: String? = null,
    val addressEn: String? = null,
    val color: String,
    val isActive: Boolean = true,
    val createdAt: String? = null
) {
    val name: String get() = nameCs
    val address: String? get() = addressCs
}
