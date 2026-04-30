package com.fitness.repository

import com.fitness.entity.AuditLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AuditLogRepository : JpaRepository<AuditLog, UUID> {
    @Query(
        value = """
            SELECT a FROM AuditLog a
            WHERE a.adminId = :adminId
            AND (:clientId IS NULL OR a.clientId = :clientId)
            AND (:action IS NULL OR a.action = :action)
            ORDER BY a.createdAt DESC
        """,
        countQuery = """
            SELECT COUNT(a) FROM AuditLog a
            WHERE a.adminId = :adminId
            AND (:clientId IS NULL OR a.clientId = :clientId)
            AND (:action IS NULL OR a.action = :action)
        """
    )
    fun findForAdmin(adminId: UUID, clientId: UUID?, action: String?, pageable: Pageable): Page<AuditLog>
}
