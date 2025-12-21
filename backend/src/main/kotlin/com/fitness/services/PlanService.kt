package com.fitness.services

import com.fitness.models.*
import com.fitness.repositories.CreditRepository
import com.fitness.repositories.PlanRepository
import com.fitness.repositories.UserRepository
import java.io.File
import java.util.*

object PlanService {

    private val uploadPath = System.getenv("UPLOAD_PATH") ?: "./uploads"

    fun getPlans(): List<TrainingPlanDTO> {
        return PlanRepository.findActivePlans()
    }

    fun getAllPlans(): List<TrainingPlanDTO> {
        return PlanRepository.findAllPlans()
    }

    fun getPlanById(id: String): TrainingPlanDTO {
        return PlanRepository.findPlanById(UUID.fromString(id))
            ?: throw NoSuchElementException("Plan not found")
    }

    fun purchasePlan(userId: String, planId: String): PurchasedPlanDTO {
        val userUUID = UUID.fromString(userId)
        val planUUID = UUID.fromString(planId)

        // Check if already purchased
        if (PlanRepository.hasUserPurchasedPlan(userUUID, planUUID)) {
            throw IllegalStateException("You have already purchased this plan")
        }

        // Get plan and check credits
        val plan = PlanRepository.findPlanById(planUUID)
            ?: throw NoSuchElementException("Plan not found")

        if (!plan.isActive) {
            throw IllegalStateException("This plan is not available for purchase")
        }

        val userCredits = UserRepository.getCredits(userUUID)
        if (userCredits < plan.credits) {
            throw IllegalStateException("Not enough credits. Required: ${plan.credits}, Available: $userCredits")
        }

        // Deduct credits
        UserRepository.updateCredits(userUUID, -plan.credits)

        // Record transaction
        CreditRepository.createTransaction(
            userId = userUUID,
            amount = -plan.credits,
            type = TransactionType.PLAN_PURCHASE.value,
            referenceId = planUUID,
            note = "Purchased: ${plan.nameCs}"
        )

        // Create purchased plan record
        return PlanRepository.createPurchasedPlan(userUUID, planUUID, plan.credits)
    }

    fun getMyPlans(userId: String): List<PurchasedPlanDTO> {
        return PlanRepository.findPurchasedPlansByUser(UUID.fromString(userId))
    }

    fun canDownloadPlan(userId: String, planId: String): Boolean {
        return PlanRepository.hasUserPurchasedPlan(
            UUID.fromString(userId),
            UUID.fromString(planId)
        )
    }

    fun getPlanFilePath(planId: String): String? {
        return PlanRepository.getPlanFilePath(UUID.fromString(planId))
    }

    fun getPlanFile(userId: String, planId: String): File {
        val userUUID = UUID.fromString(userId)
        val planUUID = UUID.fromString(planId)

        // Check if user has purchased the plan
        if (!PlanRepository.hasUserPurchasedPlan(userUUID, planUUID)) {
            throw SecurityException("You have not purchased this plan")
        }

        // Get file path
        val filePath = PlanRepository.getPlanFilePath(planUUID)
            ?: throw NoSuchElementException("Plan file not found")

        val file = File("$uploadPath/$filePath")
        if (!file.exists()) {
            throw NoSuchElementException("Plan file not found on disk")
        }

        return file
    }

    // Admin functions
    fun createPlan(request: CreateTrainingPlanRequest): TrainingPlanDTO {
        return PlanRepository.createPlan(request)
    }

    fun updatePlan(id: String, request: UpdateTrainingPlanRequest): TrainingPlanDTO {
        return PlanRepository.updatePlan(UUID.fromString(id), request)
            ?: throw NoSuchElementException("Plan not found")
    }

    fun deletePlan(id: String): Boolean {
        val planUUID = UUID.fromString(id)

        // Delete associated files
        val filePath = PlanRepository.getPlanFilePath(planUUID)
        if (filePath != null) {
            File("$uploadPath/$filePath").delete()
        }

        val plan = PlanRepository.findPlanById(planUUID)
        if (plan?.previewImage != null) {
            File("$uploadPath/${plan.previewImage}").delete()
        }

        return PlanRepository.deletePlan(planUUID)
    }

    fun uploadPlanFile(planId: String, fileName: String, fileBytes: ByteArray): Boolean {
        val planUUID = UUID.fromString(planId)

        // Ensure plan exists
        PlanRepository.findPlanById(planUUID)
            ?: throw NoSuchElementException("Plan not found")

        // Create directory if needed
        val plansDir = File("$uploadPath/plans")
        if (!plansDir.exists()) {
            plansDir.mkdirs()
        }

        // Save file
        val safeFileName = "${planId}_${fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")}"
        val filePath = "plans/$safeFileName"
        File("$uploadPath/$filePath").writeBytes(fileBytes)

        // Update database
        return PlanRepository.updatePlanFile(planUUID, filePath)
    }

    fun uploadPlanPreview(planId: String, fileName: String, fileBytes: ByteArray): Boolean {
        val planUUID = UUID.fromString(planId)

        // Ensure plan exists
        PlanRepository.findPlanById(planUUID)
            ?: throw NoSuchElementException("Plan not found")

        // Create directory if needed
        val imagesDir = File("$uploadPath/images")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }

        // Save file
        val safeFileName = "${planId}_preview_${fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")}"
        val filePath = "images/$safeFileName"
        File("$uploadPath/$filePath").writeBytes(fileBytes)

        // Update database
        return PlanRepository.updatePlanPreview(planUUID, filePath)
    }

    // Client notes
    fun getClientNotes(userId: String): List<ClientNoteDTO> {
        return PlanRepository.findNotesByClient(UUID.fromString(userId))
    }

    fun createClientNote(adminId: String, request: CreateClientNoteRequest): ClientNoteDTO {
        return PlanRepository.createNote(
            userId = UUID.fromString(request.userId),
            adminId = UUID.fromString(adminId),
            note = request.note
        )
    }

    fun deleteClientNote(noteId: String): Boolean {
        return PlanRepository.deleteNote(UUID.fromString(noteId))
    }
}
