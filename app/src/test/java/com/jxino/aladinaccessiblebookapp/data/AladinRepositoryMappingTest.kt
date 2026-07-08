package com.jxino.aladinaccessiblebookapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AladinRepositoryMappingTest {
    @Test
    fun `filters only ebook mall type results`() {
        val results = listOf(
            AladinItem(title = "전자책", mallType = "EBOOK"),
            AladinItem(title = "종이책", mallType = "BOOK"),
            AladinItem(title = "소문자 전자책", mallType = "ebook"),
        ).toBookSearchResults()

        assertEquals(2, results.size)
        assertTrue(results.all { it.isEbook })
    }
}
