package com.fitness.mapper

import com.fitness.dto.AdminTrainingPlanDTO
import com.fitness.dto.TrainingPlanDTO
import com.fitness.entity.TrainingPlan
import org.springframework.stereotype.Component

@Component
class TrainingPlanMapper {
    /**
     * Convert TrainingPlan entity to public TrainingPlanDTO.
     */
    fun toDTO(plan: TrainingPlan, locale: String = "cs"): TrainingPlanDTO {
        val name = if (locale == "en" && plan.nameEn != null) plan.nameEn else plan.nameCs
        val description = if (locale == "en" && plan.descriptionEn != null) plan.descriptionEn else plan.descriptionCs

        return TrainingPlanDTO(
            id = plan.id.toString(),
            name = name,
            description = description,
            credits = plan.credits,
            price = plan.price,
            currency = plan.currency,
            validityDays = plan.validityDays,
            sessionsCount = plan.sessionsCount,
            isActive = plan.isActive
        )
    }

    /**
     * Convert TrainingPlan entity to AdminTrainingPlanDTO.
     */
    fun toAdminDTO(plan: TrainingPlan): AdminTrainingPlanDTO {
        return AdminTrainingPlanDTO(
            id = plan.id.toString(),
            nameCs = plan.nameCs,
            nameEn = plan.nameEn,
            descriptionCs = plan.descriptionCs,
            descriptionEn = plan.descriptionEn,
            credits = plan.credits,
            isActive = plan.isActive,
            filePath = plan.filePath,
            previewImage = plan.previewImage,
            createdAt = plan.createdAt.toString()
        )
    }

    /**
     * Batch convert TrainingPlan entities to AdminTrainingPlanDTO.
     */
    fun toAdminDTOBatch(plans: List<TrainingPlan>): List<AdminTrainingPlanDTO> {
        return plans.map { toAdminDTO(it) }
    }

    /**
     * Batch convert TrainingPlan entities to public TrainingPlanDTO.
     */
    fun toDTOBatch(plans: List<TrainingPlan>, locale: String = "cs"): List<TrainingPlanDTO> {
        return plans.map { toDTO(it, locale) }
    }
}
