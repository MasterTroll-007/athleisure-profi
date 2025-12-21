package com.fitness.config

import com.fitness.models.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseConfig {
    fun init() {
        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/fitness"
            username = System.getenv("DATABASE_USER") ?: "fitness"
            password = System.getenv("DATABASE_PASSWORD") ?: "fitness123"
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Users,
                PricingItems,
                AvailabilityBlocks,
                Reservations,
                CreditPackages,
                CreditTransactions,
                TrainingPlans,
                PurchasedPlans,
                ClientNotes,
                GopayPayments,
                RefreshTokens
            )
        }
    }
}
