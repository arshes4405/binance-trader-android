package com.example.ver20
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface BinanceApi {
    @GET("api/v3/ticker/price")
    fun getPrice(@Query("symbol") symbol: String): Call<PriceResponse>

    // 여러 코인 동시 조회용 추가
    @GET("api/v3/ticker/price")
    fun getAllPrices(): Call<List<PriceResponse>>

    @GET("api/v3/ticker/price")
    fun getMultiplePrices(@Query("symbols") symbols: String): Call<List<PriceResponse>>
}

data class PriceResponse(
    val symbol: String,
    val price: String
)

// 코인 데이터 클래스 추가
data class CoinItem(
    val symbol: String,
    val price: String,
    val isLoading: Boolean = false
)