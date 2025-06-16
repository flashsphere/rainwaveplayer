package com.flashsphere.rainwaveplayer.model.user

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.flashsphere.rainwaveplayer.repository.RainwaveService
import com.flashsphere.rainwaveplayer.util.buildJson
import com.flashsphere.rainwaveplayer.util.readFile
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test

class UserAdapterTest {
    private lateinit var json: Json

    @Before
    fun setup() {
        json = buildJson()
    }

    @Test
    fun adapter_works() {
        val jsonString = readFile(this.javaClass, "/json/user-info.json")
        val user = json.decodeFromString<UserInfoResponse>(jsonString).user

        assertThat(user.id).isEqualTo(222)
        assertThat(user.name).isEqualTo("pramoda")
        assertThat(user.avatar).isEqualTo("/static/images4/user.svg")
        assertThat(user.getAvatarUrl()).isEqualTo("${RainwaveService.BASE_URL}/static/images4/user.svg")
        assertThat(user.requestsPaused).isEqualTo(false)
        assertThat(user.requestPosition).isEqualTo(3)
    }

    @Test
    fun error_response_works() {
        val jsonString = readFile(this.javaClass, "/json/user-info-error.json")
        val response = json.decodeFromString<UserInfoErrorResponse>(jsonString)

        val result = response.result
        assertThat(result.code).isEqualTo(403)
        assertThat(result.success).isEqualTo(false)
        assertThat(result.text).isEqualTo("Authorization failed.")
    }
}
