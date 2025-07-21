package com.flashsphere.rainwaveplayer

import android.content.Context
import android.content.res.Resources
import androidx.annotation.CallSuper
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flashsphere.rainwaveplayer.internal.datastore.SavedStationsStore
import com.flashsphere.rainwaveplayer.junit4.TimberRule
import com.flashsphere.rainwaveplayer.repository.RainwaveService
import com.flashsphere.rainwaveplayer.repository.StationRepository
import com.flashsphere.rainwaveplayer.repository.UserRepository
import com.flashsphere.rainwaveplayer.ui.UiEventDelegate
import com.flashsphere.rainwaveplayer.util.CoroutineDispatchers
import com.flashsphere.rainwaveplayer.util.buildRetrofit
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import retrofit2.Retrofit

abstract class BaseTest {
    @JvmField @Rule
    val mockWebServer = MockWebServer()

    lateinit var testDispatcher: TestDispatcher
    lateinit var coroutineDispatchers: CoroutineDispatchers

    lateinit var retrofit: Retrofit
    lateinit var rainwaveService: RainwaveService

    @MockK
    lateinit var contextMock: Context
    @MockK
    lateinit var resourcesMock: Resources
    @MockK
    lateinit var dataStoreMock: DataStore<Preferences>
    @MockK
    lateinit var savedStationsStore: SavedStationsStore
    @MockK
    lateinit var userRepositoryMock: UserRepository
    @MockK
    lateinit var uiEventDelegate: UiEventDelegate

    @Before
    @CallSuper
    open fun setup() {
        MockKAnnotations.init(this)
        setupTestDispatcher()

        retrofit = buildRetrofit(mockWebServer)
        rainwaveService = retrofit.create(RainwaveService::class.java)

        setupMocks()
    }

    open fun setupTestDispatcher() {
        testDispatcher = StandardTestDispatcher()
    }

    fun setupAppCoroutineDispatchers(testScope: CoroutineScope) {
        coroutineDispatchers = CoroutineDispatchers(
            scope = testScope,
            compute = testDispatcher,
            io = testDispatcher,
            main = testDispatcher,
        )
    }

    open fun setupMocks() {
        every { contextMock.resources } returns resourcesMock
        resourcesMock.run {
            every { getString(R.string.station_name_1) } returns "Game"
            every { getString(R.string.station_name_2) } returns "OC ReMix"
            every { getString(R.string.station_name_3) } returns "Covers"
            every { getString(R.string.station_name_4) } returns "Chiptune"
            every { getString(R.string.station_name_5) } returns "All"
        }
        mockkStatic("com.flashsphere.rainwaveplayer.util.DataStoreExtKt")
    }

    fun createStationRepository() = StationRepository(
        context = contextMock,
        rainwaveService = rainwaveService,
        dataStore = dataStoreMock,
        savedStationsStore = savedStationsStore,
        userRepository = userRepositoryMock,
        coroutineDispatchers = coroutineDispatchers,
        uiEventDelegate = uiEventDelegate,
    )

    fun setupSavedStationsStoreMock() {
        coEvery { savedStationsStore.get() } returns null
        coEvery { savedStationsStore.save(any()) } returns true
    }

    companion object {
        @get:ClassRule
        @JvmStatic
        val timberRule = TimberRule()
    }
}
