package com.fitness.mapper

import com.fitness.dto.AdminCreditPackageDTO
import com.fitness.dto.CreditPackageDTO
import com.fitness.entity.CreditPackage
import com.fitness.repository.CreditPackageRepository
import org.springframework.stereotype.Component
import java.util.*

@Component
class CreditPackageMapper(
    private val creditPackageRepository: CreditPackageRepository
) {
    /**
     * Convert CreditPackage entity to public CreditPackageDTO.
     * Calculates discount percent compared to basic package.
     */
    fun toDTO(pkg: CreditPackage, locale: String = "cs"): CreditPackageDTO {
        val basicPackage = pkg.trainerId?.let { trainerId ->
            creditPackageRepository.findByTrainerIdOrderBySortOrder(trainerId).find { it.isBasic }
        }
        val discountPercent = calculateDiscountPercent(pkg, basicPackage)
        val name = if (locale == "en" && pkg.nameEn != null) pkg.nameEn else pkg.nameCs

        return CreditPackageDTO(
            id = pkg.id.toString(),
            name = name,
            description = pkg.description,
            credits = pkg.credits,
            priceCzk = pkg.priceCzk,
            currency = pkg.currency ?: "CZK",
            isActive = pkg.isActive,
            highlightType = pkg.highlightType.name,
            isBasic = pkg.isBasic,
            discountPercent = discountPercent
        )
    }

    /**
     * Convert CreditPackage entity to AdminCreditPackageDTO.
     * Requires trainerId to look up basic package for discount calculation.
     */
    fun toAdminDTO(pkg: CreditPackage, trainerId: UUID): AdminCreditPackageDTO {
        val basicPackage = creditPackageRepository.findByTrainerIdOrderBySortOrder(trainerId).find { it.isBasic }
        val discountPercent = calculateDiscountPercent(pkg, basicPackage)

        return AdminCreditPackageDTO(
            id = pkg.id.toString(),
            nameCs = pkg.nameCs,
            nameEn = pkg.nameEn,
            description = pkg.description,
            credits = pkg.credits,
            priceCzk = pkg.priceCzk,
            currency = pkg.currency,
            isActive = pkg.isActive,
            sortOrder = pkg.sortOrder,
            highlightType = pkg.highlightType.name,
            isBasic = pkg.isBasic,
            discountPercent = discountPercent,
            createdAt = pkg.createdAt.toString()
        )
    }

    /**
     * Batch convert CreditPackage entities to AdminCreditPackageDTO.
     * More efficient when converting multiple packages for same trainer.
     */
    fun toAdminDTOBatch(packages: List<CreditPackage>, trainerId: UUID): List<AdminCreditPackageDTO> {
        val basicPackage = packages.find { it.isBasic }
            ?: creditPackageRepository.findByTrainerIdOrderBySortOrder(trainerId).find { it.isBasic }

        return packages.map { pkg ->
            val discountPercent = calculateDiscountPercent(pkg, basicPackage)
            AdminCreditPackageDTO(
                id = pkg.id.toString(),
                nameCs = pkg.nameCs,
                nameEn = pkg.nameEn,
                description = pkg.description,
                credits = pkg.credits,
                priceCzk = pkg.priceCzk,
                currency = pkg.currency,
                isActive = pkg.isActive,
                sortOrder = pkg.sortOrder,
                highlightType = pkg.highlightType.name,
                isBasic = pkg.isBasic,
                discountPercent = discountPercent,
                createdAt = pkg.createdAt.toString()
            )
        }
    }

    /**
     * Calculate discount percent compared to basic package price per credit.
     * Returns null if this is the basic package or no basic package exists.
     */
    private fun calculateDiscountPercent(pkg: CreditPackage, basicPackage: CreditPackage?): Int? {
        if (pkg.isBasic) return null

        val basicPricePerCredit = basicPackage?.let {
            if (it.credits > 0) it.priceCzk.toDouble() / it.credits else null
        } ?: return null

        if (basicPricePerCredit <= 0) return null

        val pricePerCredit = if (pkg.credits > 0) pkg.priceCzk.toDouble() / pkg.credits else 0.0
        return ((basicPricePerCredit - pricePerCredit) / basicPricePerCredit * 100).toInt()
    }
}
