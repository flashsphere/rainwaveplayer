package com.flashsphere.rainwaveplayer.internal.datastore

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.flashsphere.rainwaveplayer.BaseTest
import com.flashsphere.rainwaveplayer.util.BlockStoreManager
import com.flashsphere.rainwaveplayer.util.PreferencesKeys
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.API_KEY
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.USER_ID
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.VERSION
import com.flashsphere.rainwaveplayer.util.UserCredentials
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

@Suppress("DEPRECATION")
class SettingsMigrationTest : BaseTest() {

    @MockK
    private lateinit var preferencesMock: MutablePreferences
    @MockK
    private lateinit var blockStoreMock: BlockStoreManager

    @Test
    fun shouldNotMigrateWhenVersionIsCurrent() = runTest {
        every { preferencesMock[VERSION.key] } returns VERSION.defaultValue

        val migration = SettingsMigration(contextMock, blockStoreMock)
        assertThat(migration.shouldMigrate(preferencesMock)).isFalse()
    }

    @Test
    fun shouldMigrateWhenVersionIsNotCurrent() = runTest {
        every { preferencesMock[VERSION.key] } returns 6

        val migration = SettingsMigration(contextMock, blockStoreMock)
        assertThat(migration.shouldMigrate(preferencesMock)).isTrue()
    }

    @Test
    fun shouldMigrateWhenVersionIsNull() = runTest {
        every { preferencesMock[VERSION.key] } returns null

        val migration = SettingsMigration(contextMock, blockStoreMock)
        assertThat(migration.shouldMigrate(preferencesMock)).isTrue()
    }

    @Test
    fun migrateRunsWhenVersionIs6()  = runTest {
        every { preferencesMock[VERSION.key] } returns 6
        every { preferencesMock[USER_ID.key] } returns USER_ID.defaultValue
        every { preferencesMock[API_KEY.key] } returns API_KEY.defaultValue

        val migration = SettingsMigration(contextMock, blockStoreMock)
        assertThat(migration.migrate(preferencesMock)).isEqualTo(preferencesMock)

        verify(exactly = 1) {
            preferencesMock.remove(PreferencesKeys.BUFFER_MAX)
            preferencesMock.remove(PreferencesKeys.BUFFER_FOR_PLAYBACK)
            preferencesMock.remove(PreferencesKeys.BUFFER_REBUFFER)
            contextMock.cacheDir
            preferencesMock.remove(PreferencesKeys.SYSTEM_RESUMPTION)
            preferencesMock[VERSION.key] = VERSION.defaultValue
        }
    }

    @Test
    fun migrateRunsWhenVersionIs7()  = runTest {
        every { preferencesMock[VERSION.key] } returns 7
        every { preferencesMock[USER_ID.key] } returns USER_ID.defaultValue
        every { preferencesMock[API_KEY.key] } returns API_KEY.defaultValue

        val migration = SettingsMigration(contextMock, blockStoreMock)
        assertThat(migration.migrate(preferencesMock)).isEqualTo(preferencesMock)

        verify(exactly = 0) {
            preferencesMock.remove(PreferencesKeys.BUFFER_MAX)
            preferencesMock.remove(PreferencesKeys.BUFFER_FOR_PLAYBACK)
            preferencesMock.remove(PreferencesKeys.BUFFER_REBUFFER)
        }
        verify(exactly = 1) {
            contextMock.cacheDir
            preferencesMock.remove(PreferencesKeys.SYSTEM_RESUMPTION)
            preferencesMock[VERSION.key] = VERSION.defaultValue
        }
    }

    @Test
    fun migrateRunsWhenVersionIsNull()  = runTest {
        every { preferencesMock[VERSION.key] } returns null
        every { preferencesMock[USER_ID.key] } returns USER_ID.defaultValue
        every { preferencesMock[API_KEY.key] } returns API_KEY.defaultValue

        val migration = SettingsMigration(contextMock, blockStoreMock)
        assertThat(migration.migrate(preferencesMock)).isEqualTo(preferencesMock)

        verify(exactly = 0) {
            preferencesMock.remove(PreferencesKeys.BUFFER_MAX)
            preferencesMock.remove(PreferencesKeys.BUFFER_FOR_PLAYBACK)
            preferencesMock.remove(PreferencesKeys.BUFFER_REBUFFER)
            contextMock.cacheDir
            preferencesMock.remove(PreferencesKeys.SYSTEM_RESUMPTION)
        }
        verify(exactly = 1) {
            preferencesMock[VERSION.key] = VERSION.defaultValue
        }
    }

    @Test
    fun migrateRunsWhenVersionIs8()  = runTest {
        every { preferencesMock[VERSION.key] } returns 8
        every { preferencesMock[USER_ID.key] } returns USER_ID.defaultValue
        every { preferencesMock[API_KEY.key] } returns API_KEY.defaultValue

        val migration = SettingsMigration(contextMock, blockStoreMock)
        assertThat(migration.migrate(preferencesMock)).isEqualTo(preferencesMock)

        verify(exactly = 0) {
            preferencesMock.remove(PreferencesKeys.BUFFER_MAX)
            preferencesMock.remove(PreferencesKeys.BUFFER_FOR_PLAYBACK)
            preferencesMock.remove(PreferencesKeys.BUFFER_REBUFFER)
            contextMock.cacheDir
        }
        verify(exactly = 1) {
            preferencesMock.remove(PreferencesKeys.SYSTEM_RESUMPTION)
            preferencesMock[VERSION.key] = VERSION.defaultValue
        }
    }

    @Test
    fun migrateRunsWhenVersionIs9()  = runTest {
        every { preferencesMock[VERSION.key] } returns 9
        every { preferencesMock[USER_ID.key] } returns USER_ID.defaultValue
        every { preferencesMock[API_KEY.key] } returns API_KEY.defaultValue

        val migration = SettingsMigration(contextMock, blockStoreMock)
        assertThat(migration.migrate(preferencesMock)).isEqualTo(preferencesMock)

        verify(exactly = 0) {
            preferencesMock.remove(PreferencesKeys.SYSTEM_RESUMPTION)
        }
        verify(exactly = 1) {
            preferencesMock.remove(PreferencesKeys.CRASH_REPORTING)
            preferencesMock.remove(PreferencesKeys.ANALYTICS)
            preferencesMock[VERSION.key] = VERSION.defaultValue
        }
    }

    @Test
    fun migrateRunsWhenVersionIs10()  = runTest {
        every { preferencesMock[VERSION.key] } returns 10
        every { preferencesMock[USER_ID.key] } returns 123
        every { preferencesMock[API_KEY.key] } returns "abc"
        every { blockStoreMock.store(any()) } just Runs

        val migration = SettingsMigration(contextMock, blockStoreMock)
        assertThat(migration.migrate(preferencesMock)).isEqualTo(preferencesMock)

        verify(exactly = 0) {
            preferencesMock.remove(PreferencesKeys.CRASH_REPORTING)
            preferencesMock.remove(PreferencesKeys.ANALYTICS)
        }
        verify(exactly = 1) {
            blockStoreMock.store(UserCredentials(123, "abc"))
            preferencesMock[VERSION.key] = VERSION.defaultValue
        }
    }

    override fun setupMocks() {
        super.setupMocks()
        every { preferencesMock.toMutablePreferences() } returns preferencesMock
        every { preferencesMock.toPreferences() } returns preferencesMock
        every { preferencesMock.remove(any(Preferences.Key::class)) } returns Unit
        every { preferencesMock[VERSION.key] = VERSION.defaultValue } returns Unit
        every { contextMock.cacheDir } returns mockk()
    }
}
