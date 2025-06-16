package com.flashsphere.rainwaveplayer.flow

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.flashsphere.rainwaveplayer.BaseTest
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class FlowRepeatWhenTest : BaseTest() {
    @MockK
    private lateinit var connectivityObserverMock: ConnectivityObserver

    @Test
    fun repeatWhen_repeats_flow() = runTest(testDispatcher) {
        val func = mockk<() -> Boolean>()
        every { func() }.returnsMany(true, false)

        every { connectivityObserverMock.isConnected() } returns false
        every { connectivityObserverMock.connectivityFlow }
            .returnsMany(flowOf(false, true))

        val errors = mutableListOf<Throwable>()
        val results = mutableListOf<Int>()

        flowOf(1)
            .repeatWhen { func() }
            .catch { errors += it }
            .collect { results += it }

        verify(exactly = 2) { func() }
        assertThat(results).isEqualTo(listOf(1, 1))
        assertThat(errors).isEmpty()
    }
}
