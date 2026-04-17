package com.fitness.app.data.api

import com.fitness.app.data.local.TokenManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit test for [AuthInterceptor] focused on the public-path matcher.
 * Ensures admin routes like `/api/admin/plans` are NOT mistakenly
 * classified as public just because they contain `/plans`.
 */
class AuthInterceptorTest {

    private val tokenManager: TokenManager = mockk(relaxed = true) {
        every { getAccessTokenSync() } returns "fake-token"
    }

    private val interceptor = AuthInterceptor(tokenManager)

    private fun runIntercept(url: String, responseCode: Int = 200): Request {
        val request = Request.Builder().url(url).build()
        var captured: Request? = null

        val chain = mockk<Interceptor.Chain>(relaxed = true) {
            every { this@mockk.request() } returns request
            every { proceed(any()) } answers {
                captured = firstArg()
                Response.Builder()
                    .request(firstArg())
                    .protocol(Protocol.HTTP_1_1)
                    .code(responseCode)
                    .message("")
                    .build()
            }
        }

        interceptor.intercept(chain)
        return captured!!
    }

    @Test
    fun `public login path is not authenticated`() {
        val forwarded = runIntercept("http://10.0.2.2:8080/api/auth/login")
        assertNull(forwarded.header("Authorization"))
    }

    @Test
    fun `admin plans path is authenticated despite 'plans' substring`() {
        val forwarded = runIntercept("http://10.0.2.2:8080/api/admin/plans")
        assertEquals("Bearer fake-token", forwarded.header("Authorization"))
    }

    @Test
    fun `plain plans path stays public`() {
        val forwarded = runIntercept("http://10.0.2.2:8080/api/plans")
        assertNull(forwarded.header("Authorization"))
    }

    @Test
    fun `403 response triggers token clear`() {
        runIntercept("http://10.0.2.2:8080/api/reservations/my", responseCode = 403)
        verify { tokenManager.clearTokens() }
    }
}
