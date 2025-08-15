package com.example.ver20

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// Vercel API 인터페이스
interface VercelApi {
    @GET("api")
    fun getFavoriteCoins(@Query("action") action: String = "getFavoriteCoins"): Call<ApiResponse>

    @POST("api")
    fun addFavoriteCoin(
        @Query("action") action: String = "addFavoriteCoin",
        @Body request: AddCoinRequest
    ): Call<ApiResponse>

    @HTTP(method = "DELETE", path = "api", hasBody = true)
    fun removeFavoriteCoin(
        @Query("action") action: String = "removeFavoriteCoin",
        @Body request: RemoveCoinRequest
    ): Call<ApiResponse>
}

// 요청/응답 데이터 클래스
data class ApiResponse(
    val success: Boolean,
    val data: Any? = null, // List<FavoriteCoinData>에서 Any로 변경
    val message: String? = null
)

data class FavoriteCoinData(
    val _id: String,
    val symbol: String,
    val addedAt: String
)

data class AddCoinRequest(
    val symbol: String
)

data class RemoveCoinRequest(
    val symbol: String
)

class MongoDbService {
    private val baseUrl = "https://binance-trader-api.vercel.app/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(VercelApi::class.java)

    // 즐겨찾기 코인 저장
    fun saveFavoriteCoin(symbol: String, callback: (Boolean, String?) -> Unit) {
        val request = AddCoinRequest(symbol)

        api.addFavoriteCoin(request = request).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    callback(true, "코인이 추가되었습니다")
                } else {
                    val error = response.body()?.message ?: "코인 추가 실패"
                    callback(false, error)
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                callback(false, "네트워크 오류: ${t.message}")
            }
        })
    }

    // 즐겨찾기 코인 조회
    fun getFavoriteCoins(callback: (List<String>, String?) -> Unit) {
        api.getFavoriteCoins().enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    try {
                        val data = response.body()?.data
                        val symbols = if (data is List<*>) {
                            data.mapNotNull { item ->
                                if (item is Map<*, *>) {
                                    item["symbol"] as? String
                                } else null
                            }
                        } else {
                            emptyList()
                        }
                        callback(symbols, null)
                    } catch (e: Exception) {
                        callback(emptyList(), "데이터 파싱 오류: ${e.message}")
                    }
                } else {
                    val error = response.body()?.message ?: "코인 조회 실패"
                    callback(emptyList(), error)
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                callback(emptyList(), "네트워크 오류: ${t.message}")
            }
        })
    }

    // 즐겨찾기 코인 삭제
    fun removeFavoriteCoin(symbol: String, callback: (Boolean, String?) -> Unit) {
        val request = RemoveCoinRequest(symbol)

        api.removeFavoriteCoin(request = request).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    callback(true, "코인이 삭제되었습니다")
                } else {
                    val error = response.body()?.message ?: "코인 삭제 실패"
                    callback(false, error)
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                callback(false, "네트워크 오류: ${t.message}")
            }
        })
    }
}