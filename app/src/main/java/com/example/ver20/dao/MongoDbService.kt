// MongoDbService.kt - 사용자별 즐겨찾기 관리로 수정

package com.example.ver20.dao

import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// GET 방식 API 인터페이스 - 사용자별 관리
interface VercelApi {
    // 사용자별 즐겨찾기 코인 조회
    @GET("api")
    fun getFavoriteCoins(
        @Query("action") action: String = "getFavoriteCoins",
        @Query("username") username: String
    ): Call<ApiResponse>

    // 사용자별 즐겨찾기 코인 추가
    @GET("api")
    fun addFavoriteCoin(
        @Query("action") action: String = "addFavoriteCoin",
        @Query("username") username: String,
        @Query("symbol") symbol: String
    ): Call<ApiResponse>

    // 사용자별 즐겨찾기 코인 삭제
    @GET("api")
    fun removeFavoriteCoin(
        @Query("action") action: String = "removeFavoriteCoin",
        @Query("username") username: String,
        @Query("symbol") symbol: String
    ): Call<ApiResponse>

    // 기존 사용자 관련 API들
    @GET("api")
    fun saveUserSettings(
        @Query("action") action: String = "saveUserSettings",
        @Query("username") username: String,
        @Query("email") email: String,
        @Query("password") password: String,
        @Query("createdAt") createdAt: String
    ): Call<ApiResponse>

    @GET("api")
    fun getUserSettings(
        @Query("action") action: String = "getUserSettings",
        @Query("username") username: String
    ): Call<ApiResponse>
}

// 응답 데이터 클래스
data class ApiResponse(
    val success: Boolean,
    val data: Any? = null,
    val message: String? = null
)

data class FavoriteCoinData(
    val _id: String? = null,
    val username: String,
    val symbol: String,
    val addedAt: String
)

class MongoDbService {
    private val baseUrl = "https://binance-trader-api.vercel.app/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(VercelApi::class.java)

    // 사용자별 즐겨찾기 코인 저장
    fun saveFavoriteCoin(
        username: String,
        symbol: String,
        callback: (Boolean, String?) -> Unit
    ) {
        Log.d("MongoDbService", "사용자별 코인 추가: username=$username, symbol=$symbol")

        api.addFavoriteCoin(username = username, symbol = symbol).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                Log.d("MongoDbService", "코인 추가 응답: ${response.code()}")
                Log.d("MongoDbService", "요청 URL: ${call.request().url}")

                if (response.isSuccessful && response.body()?.success == true) {
                    Log.d("MongoDbService", "✅ 코인 추가 성공: $symbol (사용자: $username)")
                    callback(true, "코인이 추가되었습니다")
                } else {
                    val error = response.body()?.message ?: "코인 추가 실패"
                    Log.e("MongoDbService", "❌ 코인 추가 실패: $error")
                    callback(false, error)
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("MongoDbService", "❌ 네트워크 오류: ${t.message}")
                callback(false, "네트워크 오류: ${t.message}")
            }
        })
    }

    // 사용자별 즐겨찾기 코인 조회
    fun getFavoriteCoins(
        username: String,
        callback: (List<String>, String?) -> Unit
    ) {
        Log.d("MongoDbService", "사용자별 즐겨찾기 코인 조회: username=$username")

        api.getFavoriteCoins(username = username).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                Log.d("MongoDbService", "즐겨찾기 조회 응답: ${response.code()}")
                Log.d("MongoDbService", "요청 URL: ${call.request().url}")

                if (response.isSuccessful) {
                    try {
                        // 직접 배열로 응답이 오는 경우
                        val data = response.body()?.data
                        val symbols = if (data is List<*>) {
                            data.mapNotNull { item ->
                                when (item) {
                                    is Map<*, *> -> item["symbol"] as? String
                                    is String -> item
                                    else -> null
                                }
                            }
                        } else {
                            // 혹시 서버에서 배열을 직접 반환하는 경우를 위한 처리
                            emptyList()
                        }

                        Log.d("MongoDbService", "✅ 즐겨찾기 조회 성공: $symbols (사용자: $username)")
                        callback(symbols, null)
                    } catch (e: Exception) {
                        Log.e("MongoDbService", "❌ 데이터 파싱 오류: ${e.message}")
                        callback(emptyList(), "데이터 파싱 오류: ${e.message}")
                    }
                } else {
                    val error = "코인 조회 실패 (코드: ${response.code()})"
                    Log.e("MongoDbService", "❌ $error")
                    callback(emptyList(), error)
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("MongoDbService", "❌ 네트워크 오류: ${t.message}")
                callback(emptyList(), "네트워크 오류: ${t.message}")
            }
        })
    }

    // 사용자별 즐겨찾기 코인 삭제
    fun removeFavoriteCoin(
        username: String,
        symbol: String,
        callback: (Boolean, String?) -> Unit
    ) {
        Log.d("MongoDbService", "사용자별 코인 삭제: username=$username, symbol=$symbol")

        api.removeFavoriteCoin(username = username, symbol = symbol).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                Log.d("MongoDbService", "코인 삭제 응답: ${response.code()}")
                Log.d("MongoDbService", "요청 URL: ${call.request().url}")

                if (response.isSuccessful && response.body()?.success == true) {
                    Log.d("MongoDbService", "✅ 코인 삭제 성공: $symbol (사용자: $username)")
                    callback(true, "코인이 삭제되었습니다")
                } else {
                    val error = response.body()?.message ?: "코인 삭제 실패"
                    Log.e("MongoDbService", "❌ 코인 삭제 실패: $error")
                    callback(false, error)
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("MongoDbService", "❌ 네트워크 오류: ${t.message}")
                callback(false, "네트워크 오류: ${t.message}")
            }
        })
    }

    // 기존 사용자 관련 함수들 (변경 없음)
    fun saveUserSettings(
        username: String,
        email: String,
        hashedPassword: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val createdAt = System.currentTimeMillis().toString()

        Log.d("MongoDbService", "=== 유저 저장 요청 (GET 방식) ===")
        Log.d("MongoDbService", "URL: ${baseUrl}api")
        Log.d("MongoDbService", "Parameters: action=saveUserSettings, username=$username, email=$email")

        api.saveUserSettings(
            username = username,
            email = email,
            password = hashedPassword,
            createdAt = createdAt
        ).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                Log.d("MongoDbService", "=== 유저 저장 응답 ===")
                Log.d("MongoDbService", "응답 코드: ${response.code()}")
                Log.d("MongoDbService", "요청 URL: ${call.request().url}")

                if (response.isSuccessful) {
                    val responseBody = response.body()
                    Log.d("MongoDbService", "응답 body: $responseBody")

                    if (responseBody?.success == true) {
                        Log.d("MongoDbService", "✅ 유저 저장 성공!")
                        callback(true, "사용자 설정이 저장되었습니다")
                    } else {
                        val error = responseBody?.message ?: "success가 false"
                        Log.e("MongoDbService", "❌ API 응답 오류: $error")
                        callback(false, error)
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("MongoDbService", "❌ HTTP 오류: ${response.code()}, $errorBody")
                    callback(false, "HTTP 오류: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("MongoDbService", "❌ 네트워크 오류: ${t.message}")
                callback(false, "네트워크 오류: ${t.message}")
            }
        })
    }

    fun getUserSettings(
        username: String,
        callback: (Boolean, Map<String, Any>?, String?) -> Unit
    ) {
        Log.d("MongoDbService", "사용자 설정 조회: $username")

        api.getUserSettings(username = username).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    try {
                        val data = response.body()?.data
                        if (data is Map<*, *>) {
                            @Suppress("UNCHECKED_CAST")
                            val userData = data as Map<String, Any>
                            callback(true, userData, null)
                        } else {
                            callback(false, null, "데이터 형식 오류")
                        }
                    } catch (e: Exception) {
                        callback(false, null, "데이터 파싱 오류: ${e.message}")
                    }
                } else {
                    val error = response.body()?.message ?: "사용자 조회 실패"
                    callback(false, null, error)
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                callback(false, null, "네트워크 오류: ${t.message}")
            }
        })
    }
}