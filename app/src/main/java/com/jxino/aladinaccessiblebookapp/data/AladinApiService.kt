package com.jxino.aladinaccessiblebookapp.data

import retrofit2.http.GET
import retrofit2.http.Query

interface AladinApiService {
    @GET("ttb/api/ItemSearch.aspx")
    suspend fun searchEbooks(
        @Query("ttbkey") ttbKey: String,
        @Query("Query") query: String,
        @Query("QueryType") queryType: String = "Keyword",
        @Query("SearchTarget") searchTarget: String = "eBook",
        @Query("Start") start: Int = 1,
        @Query("MaxResults") maxResults: Int = 5,
        @Query("Sort") sort: String = "Accuracy",
        @Query("Cover") cover: String = "MidBig",
        @Query("Output") output: String = "JS",
        @Query("Version") version: String = "20131101",
        @Query("outofStockfilter") outOfStockFilter: Int = 1,
        @Query("OptResult") optResult: String = "fileFormatList",
    ): AladinSearchResponse
}
