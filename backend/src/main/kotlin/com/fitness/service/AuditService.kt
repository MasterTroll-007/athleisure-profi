package com.fitness.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Service for audit logging of sensitive admin operations.
 * 
 * Logs important security events including:
 * - Credit adjustments (who, for whom, amount, reason)
 * - Reservation cancellations (who, which reservation, refund status)
 * - User profile modifications (who, what changed)
 * 
 * All audit logs include: timestamp, actor (admin), target, action, and before/after values.
 */
@Service
class AuditService {
    
    private val auditLogger = LoggerFactory.getLogger("AUDIT")
    
    /**
     * Audit log entry for structured logging.
     */
    data class AuditEntry(
        val timestamp: Instant = Instant.now(),
        val actor: String,           // Admin user ID performing the action
        val actorEmail: String?,     // Admin email for easier identification
        val action: AuditAction,     // Type of action
        val targetType: String,      // Type of target entity (User, Reservation, etc.)
        val targetId: String,        // ID of the target entity
        val details: Map<String, Any?> = emptyMap()  // Action-specific details
    )
    
    enum class AuditAction {
        CREDIT_ADJUSTMENT,
        RESERVATION_CANCELLED,
        RESERVATION_CREATED,
        USER_UPDATED,
        USER_TRAINER_ASSIGNED,
        CLIENT_NOTE_CREATED,
        CLIENT_NOTE_DELETED
    }
    
    /**
     * Log a credit adjustment operation.
     * 
     * @param adminId The admin performing the adjustment
     * @param adminEmail Admin's email for identification
     * @param targetUserId User whose credits are being adjusted
     * @param previousBalance Credits before adjustment
     * @param adjustment Amount being added/subtracted
     * @param newBalance Credits after adjustment
     * @param reason Optional reason for the adjustment
     */
    fun logCreditAdjustment(
        adminId: String,
        adminEmail: String?,
        targetUserId: String,
        previousBalance: Int,
        adjustment: Int,
        newBalance: Int,
        reason: String?
    ) {
        val entry = AuditEntry(
            actor = adminId,
            actorEmail = adminEmail,
            action = AuditAction.CREDIT_ADJUSTMENT,
            targetType = "User",
            targetId = targetUserId,
            details = mapOf(
                "previousBalance" to previousBalance,
                "adjustment" to adjustment,
                "newBalance" to newBalance,
                "reason" to reason
            )
        )
        log(entry)
    }
    
    /**
     * Log a reservation cancellation by admin.
     * 
     * @param adminId The admin cancelling the reservation
     * @param adminEmail Admin's email for identification
     * @param reservationId ID of the cancelled reservation
     * @param userId User who owned the reservation
     * @param refundCredits Whether credits were refunded
     * @param creditsRefunded Amount of credits refunded (if any)
     */
    fun logReservationCancellation(
        adminId: String,
        adminEmail: String?,
        reservationId: String,
        userId: String,
        refundCredits: Boolean,
        creditsRefunded: Int
    ) {
        val entry = AuditEntry(
            actor = adminId,
            actorEmail = adminEmail,
            action = AuditAction.RESERVATION_CANCELLED,
            targetType = "Reservation",
            targetId = reservationId,
            details = mapOf(
                "userId" to userId,
                "refundCredits" to refundCredits,
                "creditsRefunded" to creditsRefunded
            )
        )
        log(entry)
    }
    
    /**
     * Log an admin-created reservation.
     * 
     * @param adminId The admin creating the reservation
     * @param adminEmail Admin's email for identification
     * @param reservationId ID of the created reservation
     * @param userId User the reservation is for
     * @param deductCredits Whether credits were deducted
     * @param creditsDeducted Amount of credits deducted (if any)
     */
    fun logReservationCreated(
        adminId: String,
        adminEmail: String?,
        reservationId: String,
        userId: String,
        deductCredits: Boolean,
        creditsDeducted: Int
    ) {
        val entry = AuditEntry(
            actor = adminId,
            actorEmail = adminEmail,
            action = AuditAction.RESERVATION_CREATED,
            targetType = "Reservation",
            targetId = reservationId,
            details = mapOf(
                "userId" to userId,
                "deductCredits" to deductCredits,
                "creditsDeducted" to creditsDeducted
            )
        )
        log(entry)
    }
    
    /**
     * Log a user profile update by admin.
     * 
     * @param adminId The admin making the change
     * @param adminEmail Admin's email for identification
     * @param targetUserId User being updated
     * @param changes Map of field names to (oldValue, newValue) pairs
     */
    fun logUserUpdate(
        adminId: String,
        adminEmail: String?,
        targetUserId: String,
        changes: Map<String, Pair<Any?, Any?>>
    ) {
        val entry = AuditEntry(
            actor = adminId,
            actorEmail = adminEmail,
            action = AuditAction.USER_UPDATED,
            targetType = "User",
            targetId = targetUserId,
            details = mapOf(
                "changes" to changes.mapValues { (_, v) -> 
                    mapOf("before" to v.first, "after" to v.second) 
                }
            )
        )
        log(entry)
    }
    
    /**
     * Log trainer assignment to a client.
     * 
     * @param adminId The admin making the assignment
     * @param adminEmail Admin's email for identification
     * @param clientId Client being assigned
     * @param previousTrainerId Previous trainer (null if none)
     * @param newTrainerId New trainer (null if unassigning)
     */
    fun logTrainerAssignment(
        adminId: String,
        adminEmail: String?,
        clientId: String,
        previousTrainerId: String?,
        newTrainerId: String?
    ) {
        val entry = AuditEntry(
            actor = adminId,
            actorEmail = adminEmail,
            action = AuditAction.USER_TRAINER_ASSIGNED,
            targetType = "User",
            targetId = clientId,
            details = mapOf(
                "previousTrainerId" to previousTrainerId,
                "newTrainerId" to newTrainerId
            )
        )
        log(entry)
    }
    
    /**
     * Log client note creation.
     */
    fun logClientNoteCreated(
        adminId: String,
        adminEmail: String?,
        noteId: String,
        clientId: String
    ) {
        val entry = AuditEntry(
            actor = adminId,
            actorEmail = adminEmail,
            action = AuditAction.CLIENT_NOTE_CREATED,
            targetType = "ClientNote",
            targetId = noteId,
            details = mapOf("clientId" to clientId)
        )
        log(entry)
    }
    
    /**
     * Log client note deletion.
     */
    fun logClientNoteDeleted(
        adminId: String,
        adminEmail: String?,
        noteId: String,
        clientId: String?
    ) {
        val entry = AuditEntry(
            actor = adminId,
            actorEmail = adminEmail,
            action = AuditAction.CLIENT_NOTE_DELETED,
            targetType = "ClientNote",
            targetId = noteId,
            details = mapOf("clientId" to clientId)
        )
        log(entry)
    }
    
    /**
     * Write audit entry to the audit log.
     * Uses structured JSON-like format for easy parsing.
     */
    private fun log(entry: AuditEntry) {
        val detailsJson = entry.details.entries.joinToString(", ") { (k, v) -> 
            "\"$k\": ${formatValue(v)}" 
        }
        
        auditLogger.info(
            "AUDIT | action={} | actor={} ({}) | target={}:{} | details={{ {} }}",
            entry.action,
            entry.actor,
            entry.actorEmail ?: "unknown",
            entry.targetType,
            entry.targetId,
            detailsJson
        )
    }
    
    /**
     * Format a value for JSON-like output.
     */
    private fun formatValue(value: Any?): String {
        return when (value) {
            null -> "null"
            is String -> "\"$value\""
            is Number, is Boolean -> value.toString()
            is Map<*, *> -> "{ ${value.entries.joinToString(", ") { (k, v) -> "\"$k\": ${formatValue(v)}" }} }"
            else -> "\"$value\""
        }
    }
}
