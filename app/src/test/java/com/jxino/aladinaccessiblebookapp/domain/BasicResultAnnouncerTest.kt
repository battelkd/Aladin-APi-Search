package com.jxino.aladinaccessiblebookapp.domain

import org.junit.Assert.assertTrue
import org.junit.Test

class BasicResultAnnouncerTest {
    @Test
    fun `builds search result announcement from actual results`() {
        val announcement = BasicResultAnnouncer().buildSearchResultsAnnouncement(
            listOf(
                BookSearchResult(
                    title = "채식주의자",
                    author = "한강",
                    publisher = "창비",
                    priceSales = 13500,
                    priceStandard = 15000,
                    link = "https://example.com",
                    isbn = "123",
                    isbn13 = "1234567890123",
                    mallType = "EBOOK",
                    cover = null,
                    isAdult = false,
                    isFixedPrice = true,
                    fileFormats = listOf("ePub"),
                ),
            ),
        )

        assertTrue(announcement.contains("전자책 검색 결과 1건을 찾았습니다."))
        assertTrue(announcement.contains("1번. 제목 채식주의자."))
        assertTrue(announcement.contains("저자 한강."))
        assertTrue(announcement.contains("가격 13,500원."))
    }
}
