package com.fitness.repositories

import com.fitness.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

object UserRepository {

    fun findById(id: UUID): UserDTO? = transaction {
        Users.select(Users.id eq id)
            .map { it.toUserDTO() }
            .singleOrNull()
    }

    fun findByEmail(email: String): UserDTO? = transaction {
        Users.select(Users.email eq email)
            .map { it.toUserDTO() }
            .singleOrNull()
    }

    fun getPasswordHash(email: String): String? = transaction {
        Users.select(Users.email eq email)
            .map { it[Users.passwordHash] }
            .singleOrNull()
    }

    fun create(
        email: String,
        passwordHash: String,
        firstName: String?,
        lastName: String?,
        phone: String?,
        role: String = "client"
    ): UserDTO = transaction {
        val id = Users.insertAndGetId {
            it[Users.email] = email
            it[Users.passwordHash] = passwordHash
            it[Users.firstName] = firstName
            it[Users.lastName] = lastName
            it[Users.phone] = phone
            it[Users.role] = role
            it[credits] = 0
            it[locale] = "cs"
            it[theme] = "system"
            it[createdAt] = Instant.now()
            it[updatedAt] = Instant.now()
        }

        findById(id.value)!!
    }

    fun update(id: UUID, request: UpdateProfileRequest): UserDTO? = transaction {
        Users.update({ Users.id eq id }) {
            request.firstName?.let { value -> it[firstName] = value }
            request.lastName?.let { value -> it[lastName] = value }
            request.phone?.let { value -> it[phone] = value }
            request.locale?.let { value -> it[locale] = value }
            request.theme?.let { value -> it[theme] = value }
            it[updatedAt] = Instant.now()
        }

        findById(id)
    }

    fun updatePassword(id: UUID, newPasswordHash: String): Boolean = transaction {
        Users.update({ Users.id eq id }) {
            it[passwordHash] = newPasswordHash
            it[updatedAt] = Instant.now()
        } > 0
    }

    fun updateCredits(id: UUID, amount: Int): Boolean = transaction {
        val currentCredits = Users.select(Users.id eq id)
            .map { it[Users.credits] }
            .singleOrNull() ?: 0

        Users.update({ Users.id eq id }) {
            it[credits] = currentCredits + amount
            it[updatedAt] = Instant.now()
        } > 0
    }

    fun setCredits(id: UUID, amount: Int): Boolean = transaction {
        Users.update({ Users.id eq id }) {
            it[credits] = amount
            it[updatedAt] = Instant.now()
        } > 0
    }

    fun getCredits(id: UUID): Int = transaction {
        Users.select(Users.id eq id)
            .map { it[Users.credits] }
            .singleOrNull() ?: 0
    }

    fun findAll(): List<UserDTO> = transaction {
        Users.selectAll()
            .orderBy(Users.createdAt, SortOrder.DESC)
            .map { it.toUserDTO() }
    }

    fun findAllClients(): List<UserDTO> = transaction {
        Users.select(Users.role eq "client")
            .orderBy(Users.createdAt, SortOrder.DESC)
            .map { it.toUserDTO() }
    }

    fun searchClients(query: String): List<UserDTO> = transaction {
        val searchPattern = "%${query.lowercase()}%"
        Users.select {
            (Users.role eq "client") and (
                (Users.email.lowerCase() like searchPattern) or
                (Users.firstName.lowerCase() like searchPattern) or
                (Users.lastName.lowerCase() like searchPattern)
            )
        }
            .orderBy(Users.lastName, SortOrder.ASC)
            .map { it.toUserDTO() }
    }

    fun emailExists(email: String): Boolean = transaction {
        Users.select(Users.email eq email).count() > 0
    }

    private fun ResultRow.toUserDTO() = UserDTO(
        id = this[Users.id].value.toString(),
        email = this[Users.email],
        firstName = this[Users.firstName],
        lastName = this[Users.lastName],
        phone = this[Users.phone],
        role = this[Users.role],
        credits = this[Users.credits],
        locale = this[Users.locale],
        theme = this[Users.theme],
        createdAt = this[Users.createdAt].toString()
    )
}
