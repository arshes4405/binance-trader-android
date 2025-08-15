package com.example.ver20.dao

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface BinanceApi {
    // Spot 가격 조회
    @GET("api/v3/ticker/price")
    fun getSpotPrice(@Query("symbol") symbol: String): Call<PriceResponse>

    // Future 가격 조회
    @GET("fapi/v1/ticker/price")
    fun getFuturePrice(@Query("symbol") symbol: String): Call<PriceResponse>

    // 모든 Spot 가격 조회
    @GET("api/v3/ticker/price")
    fun getAllSpotPrices(): Call<List<PriceResponse>>

    // 모든 Future 가격 조회
    @GET("fapi/v1/ticker/price")
    fun getAllFuturePrices(): Call<List<PriceResponse>>
}

data class PriceResponse(
    val symbol: String,
    val price: String
)

// 확장된 코인 아이템 (Spot + Future 가격 포함)
data class CoinItem(
    val symbol: String,
    val spotPrice: String,
    val futurePrice: String,
    val isSpotLoading: Boolean = false,
    val isFutureLoading: Boolean = false
)