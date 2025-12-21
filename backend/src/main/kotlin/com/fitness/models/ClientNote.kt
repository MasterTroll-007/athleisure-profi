package com.fitness.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object ClientNotes : UUIDTable("client_notes") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val adminId = reference("admin_id", Users, onDelete = ReferenceOption.SET_NULL).nullable()
    val note = text("note")
    val createdAt = timestamp("created_at").default(Instant.now())
}

@Serializable
data class ClientNoteDTO(
    val id: String,
    val userId: String,
    val adminId: String?,
    val adminName: String?,
    val note: String,
    val createdAt: String
)

@Serializable
data class CreateClientNoteRequest(
    val userId: String,
    val note: String
)
