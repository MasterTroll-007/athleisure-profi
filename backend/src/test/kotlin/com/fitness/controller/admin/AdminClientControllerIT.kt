package com.fitness.controller.admin

import com.fitness.IntegrationTestBase
import com.fitness.TestFixtures
import com.fitness.entity.User
import com.fitness.repository.UserRepository
import com.fitness.security.JwtService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
class AdminClientControllerIT : IntegrationTestBase() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var jwtService: JwtService

    @Test
    fun `admin can activate and deactivate own client email verification`() {
        val admin = userRepository.save(TestFixtures.adminUser(email = "verify-admin@test.com"))
        val client = userRepository.save(
            TestFixtures.user(
                email = "verify-client@test.com",
                emailVerified = false,
                trainerId = admin.id
            )
        )

        mockMvc.perform(
            patch("/api/admin/clients/${client.id}/email-verification")
                .header("Authorization", authHeader(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"emailVerified":true}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.emailVerified").value(true))

        assertThat(userRepository.findById(client.id!!).orElseThrow().emailVerified).isTrue()

        mockMvc.perform(
            patch("/api/admin/clients/${client.id}/email-verification")
                .header("Authorization", authHeader(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"emailVerified":false}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.emailVerified").value(false))

        assertThat(userRepository.findById(client.id!!).orElseThrow().emailVerified).isFalse()
    }

    @Test
    fun `admin cannot change another trainers client email verification`() {
        val admin = userRepository.save(TestFixtures.adminUser(email = "verify-admin-a@test.com"))
        val otherAdmin = userRepository.save(TestFixtures.adminUser(email = "verify-admin-b@test.com"))
        val client = userRepository.save(
            TestFixtures.user(
                email = "verify-other-client@test.com",
                emailVerified = false,
                trainerId = otherAdmin.id
            )
        )

        mockMvc.perform(
            patch("/api/admin/clients/${client.id}/email-verification")
                .header("Authorization", authHeader(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"emailVerified":true}""")
        )
            .andExpect(status().isForbidden)

        assertThat(userRepository.findById(client.id!!).orElseThrow().emailVerified).isFalse()
    }

    private fun authHeader(user: User): String =
        "Bearer ${jwtService.generateAccessToken(user.id.toString(), user.email, user.role)}"
}
