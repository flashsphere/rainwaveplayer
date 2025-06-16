package com.flashsphere.rainwaveplayer.model.category

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.flashsphere.rainwaveplayer.util.buildJson
import com.flashsphere.rainwaveplayer.util.readFile
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test

class AllCategoriesAdapterTest {
    private lateinit var json: Json

    @Before
    fun setup() {
        json = buildJson()
    }

    @Test
    fun adapter_works() {
        val jsonString = readFile(this.javaClass, "/json/all-groups.json")
        val allCategories = json.decodeFromString<AllCategories>(jsonString).categories

        assertThat(allCategories.size).isEqualTo(2)

        val category = allCategories[0]
        assertThat(category.id).isEqualTo(5333)
        assertThat(category.name).isEqualTo("100% Orange Juice")
    }
}
