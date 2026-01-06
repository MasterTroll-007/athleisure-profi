package com.fitness.repository

import com.fitness.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, UUID> {
    fun findByEmail(email: String): User?
    fun existsByEmail(email: String): Boolean
    fun findByInviteCode(inviteCode: String): User?
    fun findByRole(role: String): List<User>
    fun findByRole(role: String, pageable: Pageable): Page<User>
    fun countByRole(role: String): Long
    fun findByTrainerId(trainerId: UUID, pageable: Pageable): Page<User>
    fun countByTrainerId(trainerId: UUID): Long
    
    @Query("""
        SELECT * FROM users u WHERE u.role = 'client' AND u.trainer_id = :trainerId AND (
            LOWER(unaccent(u.email)) LIKE LOWER(unaccent(CONCAT('%', :query, '%')))
            OR LOWER(unaccent(COALESCE(u.first_name, ''))) LIKE LOWER(unaccent(CONCAT('%', :query, '%')))
            OR LOWER(unaccent(COALESCE(u.last_name, ''))) LIKE LOWER(unaccent(CONCAT('%', :query, '%')))
            OR LOWER(unaccent(CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, '')))) LIKE LOWER(unaccent(CONCAT('%', :query, '%')))
            OR LOWER(unaccent(CONCAT(COALESCE(u.last_name, ''), ' ', COALESCE(u.first_name, '')))) LIKE LOWER(unaccent(CONCAT('%', :query, '%')))
        )
    """, nativeQuery = true)
    fun searchClientsByTrainer(query: String, trainerId: UUID): List<User>
    
    @Modifying
    @Query("UPDATE User u SET u.credits = u.credits + :amount WHERE u.id = :id")
    fun updateCredits(id: UUID, amount: Int): Int
    
    @Modifying
    @Query("UPDATE User u SET u.credits = :amount WHERE u.id = :id")
    fun setCredits(id: UUID, amount: Int): Int
}
