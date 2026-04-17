package com.fitness.app.data.repository

import com.fitness.app.data.api.ApiService
import com.fitness.app.data.dto.AuthResponse
import com.fitness.app.data.dto.LoginRequest
import com.fitness.app.data.dto.MessageResponse
import com.fitness.app.data.dto.RefreshTokenRequest
import com.fitness.app.data.dto.RefreshTokenResponse
import com.fitness.app.data.dto.UserDTO
import com.fitness.app.data.local.TokenManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/**
 * Unit tests for AuthRepository. Uses MockK for API + TokenManager.
 */
class AuthRepositoryTest {

    private fun user(id: String = "u1", email: String = "x@y.com", role: String = "client") =
        UserDTO(id = id, email = email, role = role)

    private fun tokenManagerStub(): TokenManager = mockk(relaxed = true) {
        every { isLoggedIn } returns MutableStateFlow(false)
        every { logoutEvent } returns MutableSharedFlow()
    }

    @Test
    fun `login success saves tokens and publishes current user`() = runTest {
        val api = mockk<ApiService>()
        val tm = tokenManagerStub()
        val body = AuthResponse("access", "refresh", user())
        coEvery { api.login(LoginRequest("a", "b", false)) } returns Response.success(body)

        val repo = AuthRepository(api, tm)
        val result = repo.login("a", "b")

        assertTrue(result is Result.Success)
        assertEquals("u1", (result as Result.Success).data.id)
        verify { tm.saveTokens("access", "refresh") }
        assertEquals("u1", repo.currentUser.value?.id)
    }

    @Test
    fun `login failure returns error and does not touch token manager`() = runTest {
        val api = mockk<ApiService>()
        val tm = tokenManagerStub()
        val errBody = """{"message":"Invalid credentials"}""".toResponseBody("application/json".toMediaType())
        coEvery { api.login(any()) } returns Response.error(401, errBody)

        val repo = AuthRepository(api, tm)
        val result = repo.login("a", "bad")

        assertTrue(result is Result.Error)
        assertNull(repo.currentUser.value)
        verify(exactly = 0) { tm.saveTokens(any(), any()) }
    }

    @Test
    fun `refreshToken rotates both access and refresh tokens`() = runTest {
        val api = mockk<ApiService>()
        val tm = tokenManagerStub().apply {
            every { getRefreshTokenSync() } returns "old-refresh"
        }
        coEvery { api.refreshToken(RefreshTokenRequest("old-refresh")) } returns
            Response.success(RefreshTokenResponse("new-access", "new-refresh"))

        val repo = AuthRepository(api, tm)
        val result = repo.refreshToken()

        assertTrue(result is Result.Success)
        assertEquals("new-access", (result as Result.Success).data)
        verify { tm.saveTokens("new-access", "new-refresh") }
    }

    @Test
    fun `refreshToken with missing refresh token returns error without calling api`() = runTest {
        val api = mockk<ApiService>(relaxed = true)
        val tm = tokenManagerStub().apply {
            every { getRefreshTokenSync() } returns null
        }

        val repo = AuthRepository(api, tm)
        val result = repo.refreshToken()

        assertTrue(result is Result.Error)
        coVerify(exactly = 0) { api.refreshToken(any()) }
    }

    @Test
    fun `refreshToken server rejection triggers logout path`() = runTest {
        val api = mockk<ApiService>()
        val tm = tokenManagerStub().apply {
            every { getRefreshTokenSync() } returns "stale"
        }
        val errBody = "".toResponseBody(null)
        coEvery { api.refreshToken(any()) } returns Response.error(401, errBody)
        coEvery { api.logout() } returns Response.success(MessageResponse("ok"))

        val repo = AuthRepository(api, tm)
        val result = repo.refreshToken()

        assertTrue(result is Result.Error)
        verify { tm.clearTokens() }
    }

    @Test
    fun `logout clears tokens even if api call throws`() = runTest {
        val api = mockk<ApiService>()
        val tm = tokenManagerStub()
        coEvery { api.logout() } throws RuntimeException("network down")

        val repo = AuthRepository(api, tm)
        repo.logout()

        assertNull(repo.currentUser.value)
        verify { tm.clearTokens() }
    }
}
