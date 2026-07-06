package com.fitness.repository

import com.fitness.IntegrationTestBase
import com.fitness.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class UserRepositoryIT : IntegrationTestBase() {

    @Autowired private lateinit var userRepository: UserRepository

    @Test
    fun `findDebtorsByTrainerId returns only trainer clients with negative credits ordered by debt`() {
        val trainer = userRepository.save(TestFixtures.adminUser())
        val otherTrainer = userRepository.save(TestFixtures.adminUser())
        val deepestDebt = userRepository.save(
            TestFixtures.user(
                email = "deep-debt@test.com",
                credits = -5,
                trainerId = trainer.id
            )
        )
        val smallerDebt = userRepository.save(
            TestFixtures.user(
                email = "small-debt@test.com",
                credits = -1,
                trainerId = trainer.id
            )
        )
        userRepository.save(TestFixtures.user(email = "zero@test.com", credits = 0, trainerId = trainer.id))
        userRepository.save(TestFixtures.user(email = "positive@test.com", credits = 3, trainerId = trainer.id))
        userRepository.save(TestFixtures.user(email = "other@test.com", credits = -10, trainerId = otherTrainer.id))

        val debtors = userRepository.findDebtorsByTrainerId(trainer.id!!)

        assertThat(debtors.map { it.id }).containsExactly(deepestDebt.id, smallerDebt.id)
    }
}
