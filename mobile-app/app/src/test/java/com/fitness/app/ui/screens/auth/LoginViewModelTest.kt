package com.fitness.app.ui.screens.auth

import app.cash.turbine.test
import com.fitness.app.data.dto.UserDTO
import com.fitness.app.data.local.BiometricAuthManager
import com.fitness.app.data.repository.AuthRepository
import com.fitness.app.data.repository.Result
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for LoginViewModel. Proves the state-machine transitions
 * (loading → success / loading → error) and the biometric setup branch
 * triggered by rememberMe=true.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val authRepository = mockk<AuthRepository>()
    private val biometric = mockk<BiometricAuthManager>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        // Defaults so init{} doesn't explode.
        every { biometric.isBiometricAvailable() } returns false
        coEvery { biometric.isBiometricLoginEnabled() } returns false
        coEvery { biometric.getBiometricCredentials() } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun user() = UserDTO(id = "u1", email = "a@b.com", role = "client")

    @Test
    fun `login with blank fields sets error and skips repository call`() = runTest(dispatcher) {
        val vm = LoginViewModel(authRepository, biometric)

        vm.login("", "")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Please fill in all fields", vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `successful login without rememberMe ends in isSuccess true`() = runTest(dispatcher) {
        coEvery { authRepository.login("a@b.com", "pwd", false) } returns Result.Success(user())
        val vm = LoginViewModel(authRepository, biometric)

        vm.login("a@b.com", "pwd")
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.isSuccess)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `failed login surfaces error message and resets loading`() = runTest(dispatcher) {
        coEvery { authRepository.login(any(), any(), any()) } returns Result.Error("bad creds")
        val vm = LoginViewModel(authRepository, biometric)

        vm.login("a@b.com", "pwd")
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("bad creds", state.error)
        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
    }

    @Test
    fun `rememberMe + biometric available opens biometric setup dialog instead of success`() = runTest(dispatcher) {
        coEvery { authRepository.login(any(), any(), true) } returns Result.Success(user())
        every { biometric.isBiometricAvailable() } returns true
        val vm = LoginViewModel(authRepository, biometric)

        vm.setRememberMe(true)
        vm.login("a@b.com", "pwd")
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.showBiometricSetupDialog)
        assertFalse(state.isSuccess)
        assertNotNull(state.pendingBiometricCredentials)
    }

    @Test
    fun `skipBiometricSetup closes dialog and marks success`() = runTest(dispatcher) {
        val vm = LoginViewModel(authRepository, biometric)

        vm.uiState.test {
            awaitItem() // initial
            vm.skipBiometricSetup()
            val finalState = expectMostRecentItem()
            assertTrue(finalState.isSuccess)
            assertFalse(finalState.showBiometricSetupDialog)
            assertNull(finalState.pendingBiometricCredentials)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearError removes error from state`() = runTest(dispatcher) {
        coEvery { authRepository.login(any(), any(), any()) } returns Result.Error("oops")
        val vm = LoginViewModel(authRepository, biometric)
        vm.login("a@b.com", "pwd")
        dispatcher.scheduler.advanceUntilIdle()

        vm.clearError()

        assertNull(vm.uiState.value.error)
    }
}
