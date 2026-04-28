package com.fitness.repository

import com.fitness.entity.Announcement
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Repository
interface AnnouncementRepository : JpaRepository<Announcement, UUID> {
    fun findByTrainerIdOrderByCreatedAtDesc(trainerId: UUID): List<Announcement>
    fun findByTrainerIdOrderByCreatedAtDesc(trainerId: UUID, pageable: Pageable): Page<Announcement>
    fun countByTrainerIdAndCreatedAtAfter(trainerId: UUID, after: Instant): Long
}
