package com.fitness.repositories

import com.fitness.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

object PlanRepository {

    // Training Plans
    fun findPlanById(id: UUID): TrainingPlanDTO? = transaction {
        TrainingPlans.select(TrainingPlans.id eq id)
            .map { it.toPlanDTO() }
            .singleOrNull()
    }

    fun findAllPlans(): List<TrainingPlanDTO> = transaction {
        TrainingPlans.selectAll()
            .orderBy(TrainingPlans.createdAt, SortOrder.DESC)
            .map { it.toPlanDTO() }
    }

    fun findActivePlans(): List<TrainingPlanDTO> = transaction {
        TrainingPlans.select(TrainingPlans.isActive eq true)
            .orderBy(TrainingPlans.createdAt, SortOrder.DESC)
            .map { it.toPlanDTO() }
    }

    fun createPlan(request: CreateTrainingPlanRequest): TrainingPlanDTO = transaction {
        val id = TrainingPlans.insertAndGetId {
            it[nameCs] = request.nameCs
            it[nameEn] = request.nameEn
            it[descriptionCs] = request.descriptionCs
            it[descriptionEn] = request.descriptionEn
            it[credits] = request.credits
            it[isActive] = request.isActive
            it[createdAt] = Instant.now()
        }

        findPlanById(id.value)!!
    }

    fun updatePlan(id: UUID, request: UpdateTrainingPlanRequest): TrainingPlanDTO? = transaction {
        TrainingPlans.update({ TrainingPlans.id eq id }) {
            request.nameCs?.let { value -> it[nameCs] = value }
            request.nameEn?.let { value -> it[nameEn] = value }
            request.descriptionCs?.let { value -> it[descriptionCs] = value }
            request.descriptionEn?.let { value -> it[descriptionEn] = value }
            request.credits?.let { value -> it[credits] = value }
            request.isActive?.let { value -> it[isActive] = value }
        }

        findPlanById(id)
    }

    fun updatePlanFile(id: UUID, filePath: String): Boolean = transaction {
        TrainingPlans.update({ TrainingPlans.id eq id }) {
            it[TrainingPlans.filePath] = filePath
        } > 0
    }

    fun updatePlanPreview(id: UUID, previewImage: String): Boolean = transaction {
        TrainingPlans.update({ TrainingPlans.id eq id }) {
            it[TrainingPlans.previewImage] = previewImage
        } > 0
    }

    fun deletePlan(id: UUID): Boolean = transaction {
        TrainingPlans.deleteWhere { TrainingPlans.id eq id } > 0
    }

    fun getPlanFilePath(id: UUID): String? = transaction {
        TrainingPlans.select(TrainingPlans.id eq id)
            .map { it[TrainingPlans.filePath] }
            .singleOrNull()
    }

    fun getPlanCredits(id: UUID): Int? = transaction {
        TrainingPlans.select(TrainingPlans.id eq id)
            .map { it[TrainingPlans.credits] }
            .singleOrNull()
    }

    // Purchased Plans
    fun findPurchasedPlanById(id: UUID): PurchasedPlanDTO? = transaction {
        (PurchasedPlans innerJoin TrainingPlans)
            .select(PurchasedPlans.id eq id)
            .map { it.toPurchasedPlanDTO() }
            .singleOrNull()
    }

    fun findPurchasedPlansByUser(userId: UUID): List<PurchasedPlanDTO> = transaction {
        (PurchasedPlans innerJoin TrainingPlans)
            .select(PurchasedPlans.userId eq userId)
            .orderBy(PurchasedPlans.purchasedAt, SortOrder.DESC)
            .map { it.toPurchasedPlanDTO() }
    }

    fun hasUserPurchasedPlan(userId: UUID, planId: UUID): Boolean = transaction {
        PurchasedPlans.select {
            (PurchasedPlans.userId eq userId) and (PurchasedPlans.planId eq planId)
        }.count() > 0
    }

    fun createPurchasedPlan(userId: UUID, planId: UUID, creditsUsed: Int): PurchasedPlanDTO = transaction {
        val id = PurchasedPlans.insertAndGetId {
            it[PurchasedPlans.userId] = userId
            it[PurchasedPlans.planId] = planId
            it[PurchasedPlans.creditsUsed] = creditsUsed
            it[purchasedAt] = Instant.now()
        }

        findPurchasedPlanById(id.value)!!
    }

    // Client Notes
    fun findNoteById(id: UUID): ClientNoteDTO? = transaction {
        (ClientNotes innerJoin Users)
            .select(ClientNotes.id eq id)
            .map { it.toNoteDTO() }
            .singleOrNull()
    }

    fun findNotesByClient(userId: UUID): List<ClientNoteDTO> = transaction {
        ClientNotes
            .leftJoin(Users, { adminId }, { Users.id })
            .select(ClientNotes.userId eq userId)
            .orderBy(ClientNotes.createdAt, SortOrder.DESC)
            .map {
                ClientNoteDTO(
                    id = it[ClientNotes.id].value.toString(),
                    userId = it[ClientNotes.userId].value.toString(),
                    adminId = it[ClientNotes.adminId]?.value?.toString(),
                    adminName = it.getOrNull(Users.firstName)?.let { fn ->
                        "$fn ${it.getOrNull(Users.lastName) ?: ""}".trim()
                    },
                    note = it[ClientNotes.note],
                    createdAt = it[ClientNotes.createdAt].toString()
                )
            }
    }

    fun createNote(userId: UUID, adminId: UUID, note: String): ClientNoteDTO = transaction {
        val id = ClientNotes.insertAndGetId {
            it[ClientNotes.userId] = userId
            it[ClientNotes.adminId] = adminId
            it[ClientNotes.note] = note
            it[createdAt] = Instant.now()
        }

        ClientNoteDTO(
            id = id.value.toString(),
            userId = userId.toString(),
            adminId = adminId.toString(),
            adminName = null,
            note = note,
            createdAt = Instant.now().toString()
        )
    }

    fun deleteNote(id: UUID): Boolean = transaction {
        ClientNotes.deleteWhere { ClientNotes.id eq id } > 0
    }

    private fun ResultRow.toPlanDTO() = TrainingPlanDTO(
        id = this[TrainingPlans.id].value.toString(),
        nameCs = this[TrainingPlans.nameCs],
        nameEn = this[TrainingPlans.nameEn],
        descriptionCs = this[TrainingPlans.descriptionCs],
        descriptionEn = this[TrainingPlans.descriptionEn],
        credits = this[TrainingPlans.credits],
        previewImage = this[TrainingPlans.previewImage],
        hasFile = this[TrainingPlans.filePath] != null,
        isActive = this[TrainingPlans.isActive],
        createdAt = this[TrainingPlans.createdAt].toString()
    )

    private fun ResultRow.toPurchasedPlanDTO() = PurchasedPlanDTO(
        id = this[PurchasedPlans.id].value.toString(),
        userId = this[PurchasedPlans.userId].value.toString(),
        planId = this[PurchasedPlans.planId].value.toString(),
        planName = this[TrainingPlans.nameCs],
        creditsUsed = this[PurchasedPlans.creditsUsed],
        purchasedAt = this[PurchasedPlans.purchasedAt].toString()
    )

    private fun ResultRow.toNoteDTO() = ClientNoteDTO(
        id = this[ClientNotes.id].value.toString(),
        userId = this[ClientNotes.userId].value.toString(),
        adminId = this[ClientNotes.adminId]?.value?.toString(),
        adminName = this[Users.firstName]?.let { fn ->
            "$fn ${this[Users.lastName] ?: ""}".trim()
        },
        note = this[ClientNotes.note],
        createdAt = this[ClientNotes.createdAt].toString()
    )
}
