package com.jxino.aladinaccessiblebookapp.domain

data class BookSearchResult(
    val title: String,
    val author: String,
    val publisher: String,
    val priceSales: Int,
    val priceStandard: Int,
    val link: String?,
    val isbn: String?,
    val isbn13: String?,
    val mallType: String?,
    val cover: String?,
    val isAdult: Boolean,
    val isFixedPrice: Boolean,
    val fileFormats: List<String>,
) {
    val isEbook: Boolean = mallType.equals("EBOOK", ignoreCase = true)
}
