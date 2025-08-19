package com.example.ver20.dao.mongoDB

import retrofit2.Call
import retrofit2.http.*

interface MongoDbApi {
    @Headers("Content-Type: application/json")
    @POST("action/insertOne")
    fun insertDocument(
        @Header("api-key") apiKey: String,
        @Body request: MongoInsertRequest
    ): Call<MongoResponse>

    @Headers("Content-Type: application/json")
    @POST("action/find")
    fun findDocuments(
        @Header("api-key") apiKey: String,
        @Body request: MongoFindRequest
    ): Call<MongoFindResponse>
}

data class MongoInsertRequest(
    val dataSource: String = "MyCloude",
    val database: String = "binance_trader",
    val collection: String,
    val document: Any
)

data class MongoFindRequest(
    val dataSource: String = "MyCloude",
    val database: String = "binance_trader",
    val collection: String,
    val filter: Map<String, Any> = emptyMap()
)

data class MongoResponse(
    val insertedId: String?
)

data class MongoFindResponse(
    val documents: List<Map<String, Any>>
)