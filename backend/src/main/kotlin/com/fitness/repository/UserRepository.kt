package com.fitness.repository

import com.fitness.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, UUID> {
    fun findByEmail(email: String): User?
    fun existsByEmail(email: String): Boolean
    fun findByRole(role: String): List<User>
    
    @Query("SELECT u FROM User u WHERE u.role = 'client' AND (LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')))")
    fun searchClients(query: String): List<User>
    
    @Modifying
    @Query("UPDATE User u SET u.credits = u.credits + :amount WHERE u.id = :id")
    fun updateCredits(id: UUID, amount: Int): Int
    
    @Modifying
    @Query("UPDATE User u SET u.credits = :amount WHERE u.id = :id")
    fun setCredits(id: UUID, amount: Int): Int
}
