package com.flashsphere.rainwaveplayer.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.flashsphere.rainwaveplayer.util.ClassUtils.getClassName
import com.flashsphere.rainwaveplayer.util.ClassUtils.getName
import com.flashsphere.rainwaveplayer.util.ClassUtils.getSimpleClassName
import com.flashsphere.rainwaveplayer.util.ClassUtils.getSimpleName
import org.junit.Test

class ClassUtilsTest {
    @Test
    fun getSimpleNameWorks() {
        assertThat(getSimpleClassName()).isEqualTo("ClassUtilsTest")
        assertThat(getSimpleName(ClassUtilsTest::class)).isEqualTo("ClassUtilsTest")
        assertThat(ClassUtilsTest::class.getSimpleClassName()).isEqualTo("ClassUtilsTest")
    }

    @Test
    fun getNameWorks() {
        assertThat(getClassName()).isEqualTo("com.flashsphere.rainwaveplayer.util.ClassUtilsTest")
        assertThat(getName(ClassUtilsTest::class)).isEqualTo("com.flashsphere.rainwaveplayer.util.ClassUtilsTest")
        assertThat(ClassUtilsTest::class.getClassName()).isEqualTo("com.flashsphere.rainwaveplayer.util.ClassUtilsTest")
    }
}
