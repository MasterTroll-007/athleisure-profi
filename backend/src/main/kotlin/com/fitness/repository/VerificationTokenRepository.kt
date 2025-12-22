package com.fitness.repository

import com.fitness.entity.VerificationToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VerificationTokenRepository : JpaRepository<VerificationToken, UUID> {
    fun findByToken(token: String): VerificationToken?
    fun deleteByToken(token: String)
    fun deleteByUserId(userId: UUID)
}
