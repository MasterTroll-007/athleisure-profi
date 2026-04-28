package com.fitness.config

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class MonitorSecurityIT {

    @Autowired private lateinit var mockMvc: MockMvc

    @Test
    fun `actuator internals are not public`() {
        mockMvc.perform(get("/api/monitor/env"))
            .andExpect(status().isUnauthorized())

        mockMvc.perform(get("/api/monitor/loggers"))
            .andExpect(status().isUnauthorized())
    }

    @Test
    fun `public monitor surface is limited to health`() {
        mockMvc.perform(get("/api/monitor/health"))
            .andExpect(status().isOk())

        mockMvc.perform(get("/api/monitor/stats"))
            .andExpect(status().isUnauthorized())
    }
}
