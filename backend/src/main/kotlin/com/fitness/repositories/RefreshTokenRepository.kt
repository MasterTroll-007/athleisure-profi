package com.fitness.repositories

import com.fitness.models.RefreshTokens
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

object RefreshTokenRepository {

    fun create(userId: UUID, token: String, expiresAt: Instant): Unit = transaction {
        RefreshTokens.insert {
            it[RefreshTokens.userId] = userId
            it[RefreshTokens.token] = token
            it[RefreshTokens.expiresAt] = expiresAt
            it[createdAt] = Instant.now()
        }
    }

    fun findByToken(token: String): RefreshTokenData? = transaction {
        RefreshTokens.select(RefreshTokens.token eq token)
            .map {
                RefreshTokenData(
                    id = it[RefreshTokens.id].value,
                    userId = it[RefreshTokens.userId].value,
                    token = it[RefreshTokens.token],
                    expiresAt = it[RefreshTokens.expiresAt]
                )
            }
            .singleOrNull()
    }

    fun delete(token: String): Boolean = transaction {
        RefreshTokens.deleteWhere { RefreshTokens.token eq token } > 0
    }

    fun deleteAllForUser(userId: UUID): Int = transaction {
        RefreshTokens.deleteWhere { RefreshTokens.userId eq userId }
    }

    fun deleteExpired(): Int = transaction {
        RefreshTokens.deleteWhere { expiresAt less Instant.now() }
    }
}

data class RefreshTokenData(
    val id: UUID,
    val userId: UUID,
    val token: String,
    val expiresAt: Instant
)
