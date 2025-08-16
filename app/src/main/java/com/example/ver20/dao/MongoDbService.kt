// MongoDbService.kt - 사용자별 즐겨찾기 관리로 수정

package com.example.ver20.dao

import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody

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

interface RawApi {
    @GET("api")
    fun getFavoriteCoinsRaw(
        @Query("action") action: String = "getFavoriteCoins",
        @Query("username") username: String
    ): Call<okhttp3.ResponseBody>
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

        // 배열 응답을 직접 받기 위한 Raw API 호출
        val rawRetrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .build()

        val rawApi = rawRetrofit.create(RawApi::class.java)
        val rawCall = rawApi.getFavoriteCoinsRaw(username = username)

        Log.d("MongoDbService", "요청 URL: ${rawCall.request().url}")

        rawCall.enqueue(object : Callback<okhttp3.ResponseBody> {
            override fun onResponse(call: Call<okhttp3.ResponseBody>, response: Response<okhttp3.ResponseBody>) {
                Log.d("MongoDbService", "즐겨찾기 조회 응답: ${response.code()}")

                if (response.isSuccessful) {
                    try {
                        val jsonString = response.body()?.string() ?: ""
                        Log.d("MongoDbService", "원본 JSON 응답: $jsonString")

                        if (jsonString.trim().startsWith("[")) {
                            // 배열로 직접 응답하는 경우
                            val gson = com.google.gson.Gson()
                            val type = object : com.google.gson.reflect.TypeToken<List<FavoriteCoinData>>() {}.type
                            val coinList: List<FavoriteCoinData> = gson.fromJson(jsonString, type)
                            val symbols = coinList.map { it.symbol }

                            Log.d("MongoDbService", "✅ 배열 파싱 성공: $symbols (사용자: $username)")
                            callback(symbols, null)

                        } else if (jsonString.trim().startsWith("{")) {
                            // 객체로 응답하는 경우
                            val gson = com.google.gson.Gson()
                            val apiResponse = gson.fromJson(jsonString, ApiResponse::class.java)

                            if (apiResponse.success == true && apiResponse.data is List<*>) {
                                val symbols = (apiResponse.data as List<*>).mapNotNull { item ->
                                    when (item) {
                                        is Map<*, *> -> item["symbol"] as? String
                                        is String -> item
                                        else -> null
                                    }
                                }
                                Log.d("MongoDbService", "✅ 객체 파싱 성공: $symbols (사용자: $username)")
                                callback(symbols, null)
                            } else {
                                Log.e("MongoDbService", "❌ API 응답 오류: ${apiResponse.message}")
                                callback(emptyList(), apiResponse.message ?: "API 오류")
                            }
                        } else {
                            Log.e("MongoDbService", "❌ 알 수 없는 응답 형식: $jsonString")
                            callback(emptyList(), "알 수 없는 응답 형식")
                        }

                    } catch (e: Exception) {
                        Log.e("MongoDbService", "❌ JSON 파싱 오류: ${e.message}")
                        e.printStackTrace()
                        callback(emptyList(), "JSON 파싱 오류: ${e.message}")
                    }
                } else {
                    val error = "코인 조회 실패 (코드: ${response.code()})"
                    Log.e("MongoDbService", "❌ $error")
                    callback(emptyList(), error)
                }
            }

            override fun onFailure(call: Call<okhttp3.ResponseBody>, t: Throwable) {
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