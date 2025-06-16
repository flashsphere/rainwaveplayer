package com.flashsphere.rainwaveplayer.util

import android.content.Context
import android.content.res.Resources
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.flashsphere.rainwaveplayer.R
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale

class FormatterTest {

    private lateinit var originalLocale: Locale

    @Before
    fun setup() {
        originalLocale = Locale.getDefault()
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun formatNumberOrdinalEn() {
        val resources = mockk<Resources>()
        every { resources.getString(R.string.position_ordinal) } returns """
            {position, selectordinal,
                one {#st}
                two {#nd}
                few {#rd}
                other {#th}
            }
        """.trimIndent()

        val context = mockk<Context>()
        every { context.resources } returns resources

        Locale.setDefault(Locale.ENGLISH)

        assertThat(Formatter.formatNumberOrdinal(context, 1)).isEqualTo("1st")
        assertThat(Formatter.formatNumberOrdinal(context, 2)).isEqualTo("2nd")
        assertThat(Formatter.formatNumberOrdinal(context, 3)).isEqualTo("3rd")
        assertThat(Formatter.formatNumberOrdinal(context, 4)).isEqualTo("4th")
        assertThat(Formatter.formatNumberOrdinal(context, 11)).isEqualTo("11th")
        assertThat(Formatter.formatNumberOrdinal(context, 12)).isEqualTo("12th")
        assertThat(Formatter.formatNumberOrdinal(context, 13)).isEqualTo("13th")
        assertThat(Formatter.formatNumberOrdinal(context, 14)).isEqualTo("14th")
        assertThat(Formatter.formatNumberOrdinal(context, 21)).isEqualTo("21st")
        assertThat(Formatter.formatNumberOrdinal(context, 22)).isEqualTo("22nd")
    }

    @Test
    fun formatNumberOrdinalFr() {
        val resources = mockk<Resources>()
        every { resources.getString(R.string.position_ordinal) } returns """
            {position, selectordinal,
                one {#er}
                other {#e}
            }
        """.trimIndent()

        val context = mockk<Context>()
        every { context.resources } returns resources

        Locale.setDefault(Locale.FRENCH)

        assertThat(Formatter.formatNumberOrdinal(context, 1)).isEqualTo("1er")
        assertThat(Formatter.formatNumberOrdinal(context, 2)).isEqualTo("2e")
        assertThat(Formatter.formatNumberOrdinal(context, 3)).isEqualTo("3e")
        assertThat(Formatter.formatNumberOrdinal(context, 4)).isEqualTo("4e")
        assertThat(Formatter.formatNumberOrdinal(context, 11)).isEqualTo("11e")
        assertThat(Formatter.formatNumberOrdinal(context, 12)).isEqualTo("12e")
        assertThat(Formatter.formatNumberOrdinal(context, 13)).isEqualTo("13e")
        assertThat(Formatter.formatNumberOrdinal(context, 14)).isEqualTo("14e")
        assertThat(Formatter.formatNumberOrdinal(context, 21)).isEqualTo("21e")
        assertThat(Formatter.formatNumberOrdinal(context, 22)).isEqualTo("22e")
    }
}
