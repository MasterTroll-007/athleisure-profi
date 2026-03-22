package com.fitness.repository

import com.fitness.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, UUID> {
    fun findByToken(token: String): RefreshToken?

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.token = :token")
    fun deleteByToken(token: String)

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.userId = :userId")
    fun deleteByUserId(userId: UUID)
}
