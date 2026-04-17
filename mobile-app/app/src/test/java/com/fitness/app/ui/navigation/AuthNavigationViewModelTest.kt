package com.fitness.app.ui.navigation

import app.cash.turbine.test
import com.fitness.app.data.local.TokenManager
import com.fitness.app.data.repository.AuthRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test for [AuthNavigationViewModel]. Proves the initial `isLoggedIn`
 * value is seeded from the synchronous token manager (so the nav host
 * doesn't briefly flash the login screen to an already-authenticated user),
 * and that the flow tracks repository updates.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthNavigationViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val tokenManager = mockk<TokenManager>(relaxed = true)
    private val isLoggedInFlow = MutableStateFlow(false)
    private val authRepository = mockk<AuthRepository>(relaxed = true) {
        every { isLoggedIn } returns isLoggedInFlow
        every { currentUser } returns MutableStateFlow(null)
        every { logoutEvent } returns MutableSharedFlow()
    }

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial isLoggedIn is true when access token exists`() = runTest(dispatcher) {
        every { tokenManager.getAccessTokenSync() } returns "existing-token"
        val vm = AuthNavigationViewModel(authRepository, tokenManager)

        assertTrue(vm.isLoggedIn.value)
    }

    @Test
    fun `initial isLoggedIn is false when no token`() = runTest(dispatcher) {
        every { tokenManager.getAccessTokenSync() } returns null
        val vm = AuthNavigationViewModel(authRepository, tokenManager)

        assertEquals(false, vm.isLoggedIn.value)
    }

    @Test
    fun `isLoggedIn emits repository updates`() = runTest(dispatcher) {
        every { tokenManager.getAccessTokenSync() } returns null
        val vm = AuthNavigationViewModel(authRepository, tokenManager)

        vm.isLoggedIn.test {
            assertEquals(false, awaitItem())
            isLoggedInFlow.value = true
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(true, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
