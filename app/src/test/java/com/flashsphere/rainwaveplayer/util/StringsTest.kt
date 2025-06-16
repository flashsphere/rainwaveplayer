package com.flashsphere.rainwaveplayer.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.flashsphere.rainwaveplayer.util.Strings.fromBase64
import com.flashsphere.rainwaveplayer.util.Strings.toBase64
import com.flashsphere.rainwaveplayer.util.Strings.toEmpty
import org.junit.Test

class StringsTest {
    @Test
    fun nullToEmptyTest() {
        val nullString: String? = null
        assertThat(nullString.toEmpty()).isEqualTo("")

        val valueString = "test"
        assertThat(valueString.toEmpty()).isEqualTo("test")
    }

    @Test
    fun fromBase64Test() {
        val input = "dGhpcyBpcyBhIHN0cmluZyB3aXRoIHJhaW53YXZlIHVybDogcnc6Ly91c2VyaWQ6YXBpa2V5QHJhaW53YXZlLmNj"
        val expectedOutput = "this is a string with rainwave url: rw://userid:apikey@rainwave.cc"
        assertThat(input.fromBase64()).isEqualTo(expectedOutput)
    }

    @Test
    fun toBase64Test() {
        val input = "this is a string with rainwave url: rw://userid:apikey@rainwave.cc"
        val expectedOutput = "dGhpcyBpcyBhIHN0cmluZyB3aXRoIHJhaW53YXZlIHVybDogcnc6Ly91c2VyaWQ6YXBpa2V5QHJhaW53YXZlLmNj"
        assertThat(input.toBase64()).isEqualTo(expectedOutput)
    }
}
