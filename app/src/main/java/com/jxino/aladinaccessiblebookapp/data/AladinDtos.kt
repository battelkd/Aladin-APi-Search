package com.jxino.aladinaccessiblebookapp.data

import com.google.gson.annotations.SerializedName

data class AladinSearchResponse(
    @SerializedName("item") val item: List<AladinItem> = emptyList(),
)

data class AladinItem(
    @SerializedName("title") val title: String? = null,
    @SerializedName("author") val author: String? = null,
    @SerializedName("publisher") val publisher: String? = null,
    @SerializedName("priceSales") val priceSales: Int? = null,
    @SerializedName("priceStandard") val priceStandard: Int? = null,
    @SerializedName("link") val link: String? = null,
    @SerializedName("isbn") val isbn: String? = null,
    @SerializedName("isbn13") val isbn13: String? = null,
    @SerializedName("mallType") val mallType: String? = null,
    @SerializedName("cover") val cover: String? = null,
    @SerializedName("adult") val adult: Boolean? = null,
    @SerializedName("fixedPrice") val fixedPrice: Boolean? = null,
    @SerializedName("subInfo") val subInfo: AladinSubInfo? = null,
)

data class AladinSubInfo(
    @SerializedName("fileFormatList") val fileFormatList: List<String>? = null,
)
