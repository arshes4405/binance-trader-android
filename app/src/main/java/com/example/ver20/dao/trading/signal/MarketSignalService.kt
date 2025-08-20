// MarketSignalService.kt - 안드로이드 앱용 완성된 버전

package com.example.ver20.dao.trading.signal

import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// API 응답 데이터 클래스
data class MarketSignalApiResponse(
    val success: Boolean,
    val message: String?,
    val data: Any? = null
)

// MongoDB API 인터페이스 (설정 및 신호 조회용)
interface MarketSignalApi {
    // 시세포착 설정 저장
    @GET("api")
    fun saveSignalConfig(
        @Query("action") action: String = "saveSignalConfig",
        @Query("username") username: String,
        @Query("signalType") signalType: String,
        @Query("symbol") symbol: String,
        @Query("timeframe") timeframe: String,
        @Query("checkInterval") checkInterval: Int,
        @Query("cciPeriod") cciPeriod: Int = 20,
        @Query("cciBreakoutValue") cciBreakoutValue: Double = 100.0,
        @Query("cciEntryValue") cciEntryValue: Double = 90.0,
        @Query("rsiPeriod") rsiPeriod: Int = 14,
        @Query("rsiOverbought") rsiOverbought: Double = 70.0,
        @Query("rsiOversold") rsiOversold: Double = 30.0,
        @Query("cortaFastMa") cortaFastMa: Int = 12,
        @Query("cortaSlowMa") cortaSlowMa: Int = 26,
        @Query("cortaSignalLine") cortaSignalLine: Int = 9,
        @Query("cortaVolumeFactor") cortaVolumeFactor: Double = 1.5,
        @Query("cortaRsiConfirm") cortaRsiConfirm: Boolean = true,
        @Query("seedMoney") seedMoney: Double,
        @Query("isActive") isActive: Boolean,
        @Query("autoTrading") autoTrading: Boolean
    ): Call<MarketSignalApiResponse>

    // 시세포착 설정 조회
    @GET("api")
    fun getSignalConfigs(
        @Query("action") action: String = "getSignalConfigs",
        @Query("username") username: String
    ): Call<MarketSignalApiResponse>

    // 시세포착 설정 삭제
    @GET("api")
    fun deleteSignalConfig(
        @Query("action") action: String = "deleteSignalConfig",
        @Query("configId") configId: String
    ): Call<MarketSignalApiResponse>

    // 시세포착 신호 조회 (표시용)
    @GET("api")
    fun getSignals(
        @Query("action") action: String = "getSignals",
        @Query("username") username: String,
        @Query("limit") limit: Int = 50
    ): Call<MarketSignalApiResponse>

    // 신호 읽음 처리
    @GET("api")
    fun markSignalAsRead(
        @Query("action") action: String = "markSignalAsRead",
        @Query("signalId") signalId: String
    ): Call<MarketSignalApiResponse>
}

class MarketSignalService {
    companion object {
        private const val TAG = "MarketSignalService"
    }

    private val baseUrl = "https://binance-trader-api.vercel.app/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(MarketSignalApi::class.java)

    // ===== 시세포착 설정 관리 =====

    /**
     * 시세포착 설정 저장
     */
    fun saveSignalConfig(
        config: MarketSignalConfig,
        callback: (Boolean, String?) -> Unit
    ) {
        Log.d(TAG, "설정 저장 요청: ${config.symbol} - ${config.signalType}")

        api.saveSignalConfig(
            username = config.username,
            signalType = config.signalType,
            symbol = config.symbol,
            timeframe = config.timeframe,
            checkInterval = config.checkInterval,
            cciPeriod = config.cciPeriod,
            cciBreakoutValue = config.cciBreakoutValue,
            cciEntryValue = config.cciEntryValue,
            rsiPeriod = config.rsiPeriod,
            rsiOverbought = config.rsiOverbought,
            rsiOversold = config.rsiOversold,
            cortaFastMa = config.cortaFastMa,
            cortaSlowMa = config.cortaSlowMa,
            cortaSignalLine = config.cortaSignalLine,
            cortaVolumeFactor = config.cortaVolumeFactor,
            cortaRsiConfirm = config.cortaRsiConfirm,
            seedMoney = config.seedMoney,
            isActive = config.isActive,
            autoTrading = config.autoTrading
        ).enqueue(object : Callback<MarketSignalApiResponse> {
            override fun onResponse(
                call: Call<MarketSignalApiResponse>,
                response: Response<MarketSignalApiResponse>
            ) {
                if (response.isSuccessful) {
                    val result = response.body()
                    Log.d(TAG, "설정 저장 응답: ${result?.success}")
                    callback(result?.success == true, result?.message)
                } else {
                    Log.e(TAG, "설정 저장 HTTP 오류: ${response.code()}")
                    callback(false, "서버 오류: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<MarketSignalApiResponse>, t: Throwable) {
                Log.e(TAG, "설정 저장 네트워크 오류: ${t.message}")
                callback(false, "네트워크 오류: ${t.message}")
            }
        })
    }

    /**
     * 시세포착 설정 조회
     */
    fun getSignalConfigs(
        username: String,
        callback: (List<MarketSignalConfig>?, String?) -> Unit
    ) {
        Log.d(TAG, "설정 조회 요청: $username")

        api.getSignalConfigs(username = username)
            .enqueue(object : Callback<MarketSignalApiResponse> {
                override fun onResponse(
                    call: Call<MarketSignalApiResponse>,
                    response: Response<MarketSignalApiResponse>
                ) {
                    if (response.isSuccessful) {
                        val result = response.body()
                        if (result?.success == true) {
                            try {
                                val configsData = result.data
                                val configs = if (configsData is List<*>) {
                                    configsData.mapNotNull { item ->
                                        if (item is Map<*, *>) {
                                            val dataMap = item as Map<String, Any>
                                            MarketSignalConfig(
                                                id = dataMap["_id"]?.toString() ?: "",
                                                username = dataMap["username"]?.toString() ?: "",
                                                signalType = dataMap["signalType"]?.toString() ?: "CCI",
                                                symbol = dataMap["symbol"]?.toString() ?: "",
                                                timeframe = dataMap["timeframe"]?.toString() ?: "",
                                                checkInterval = (dataMap["checkInterval"] as? Number)?.toInt() ?: 15,
                                                isActive = dataMap["isActive"] as? Boolean ?: true,
                                                autoTrading = dataMap["autoTrading"] as? Boolean ?: false,
                                                seedMoney = (dataMap["seedMoney"] as? Number)?.toDouble() ?: 1000.0,
                                                // CCI 설정
                                                cciPeriod = (dataMap["cciPeriod"] as? Number)?.toInt() ?: 20,
                                                cciBreakoutValue = (dataMap["cciBreakoutValue"] as? Number)?.toDouble() ?: 100.0,
                                                cciEntryValue = (dataMap["cciEntryValue"] as? Number)?.toDouble() ?: 90.0,
                                                // RSI 설정
                                                rsiPeriod = (dataMap["rsiPeriod"] as? Number)?.toInt() ?: 14,
                                                rsiOverbought = (dataMap["rsiOverbought"] as? Number)?.toDouble() ?: 70.0,
                                                rsiOversold = (dataMap["rsiOversold"] as? Number)?.toDouble() ?: 30.0,
                                                // 코르타 설정
                                                cortaFastMa = (dataMap["cortaFastMa"] as? Number)?.toInt() ?: 12,
                                                cortaSlowMa = (dataMap["cortaSlowMa"] as? Number)?.toInt() ?: 26,
                                                cortaSignalLine = (dataMap["cortaSignalLine"] as? Number)?.toInt() ?: 9,
                                                cortaVolumeFactor = (dataMap["cortaVolumeFactor"] as? Number)?.toDouble() ?: 1.5,
                                                cortaRsiConfirm = dataMap["cortaRsiConfirm"] as? Boolean ?: true,
                                                createdAt = dataMap["createdAt"]?.toString() ?: ""
                                            )
                                        } else null
                                    }
                                } else {
                                    emptyList()
                                }
                                Log.d(TAG, "설정 조회 성공: ${configs.size}개")
                                callback(configs, null)
                            } catch (e: Exception) {
                                Log.e(TAG, "설정 파싱 오류: ${e.message}")
                                callback(null, "파싱 오류: ${e.message}")
                            }
                        } else {
                            Log.e(TAG, "설정 조회 실패: ${result?.message}")
                            callback(null, result?.message)
                        }
                    } else {
                        Log.e(TAG, "설정 조회 HTTP 오류: ${response.code()}")
                        callback(null, "서버 오류: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<MarketSignalApiResponse>, t: Throwable) {
                    Log.e(TAG, "설정 조회 네트워크 오류: ${t.message}")
                    callback(null, "네트워크 오류: ${t.message}")
                }
            })
    }

    /**
     * 시세포착 설정 삭제
     */
    fun deleteSignalConfig(
        configId: String,
        callback: (Boolean, String?) -> Unit
    ) {
        Log.d(TAG, "설정 삭제 요청: $configId")

        api.deleteSignalConfig(configId = configId)
            .enqueue(object : Callback<MarketSignalApiResponse> {
                override fun onResponse(
                    call: Call<MarketSignalApiResponse>,
                    response: Response<MarketSignalApiResponse>
                ) {
                    if (response.isSuccessful) {
                        val result = response.body()
                        Log.d(TAG, "설정 삭제 응답: ${result?.success}")
                        callback(result?.success == true, result?.message)
                    } else {
                        Log.e(TAG, "설정 삭제 HTTP 오류: ${response.code()}")
                        callback(false, "서버 오류: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<MarketSignalApiResponse>, t: Throwable) {
                    Log.e(TAG, "설정 삭제 네트워크 오류: ${t.message}")
                    callback(false, "네트워크 오류: ${t.message}")
                }
            })
    }

    /**
     * 시세포착 신호 조회 (표시용)
     */
    fun getSignals(
        username: String,
        callback: (List<MarketSignal>?, String?) -> Unit
    ) {
        Log.d(TAG, "신호 조회 요청: $username")

        api.getSignals(username = username)
            .enqueue(object : Callback<MarketSignalApiResponse> {
                override fun onResponse(
                    call: Call<MarketSignalApiResponse>,
                    response: Response<MarketSignalApiResponse>
                ) {
                    if (response.isSuccessful) {
                        val result = response.body()
                        if (result?.success == true) {
                            try {
                                val signalsData = result.data
                                val signals = if (signalsData is List<*>) {
                                    signalsData.mapNotNull { item ->
                                        if (item is Map<*, *>) {
                                            val dataMap = item as Map<String, Any>
                                            MarketSignal(
                                                id = dataMap["_id"]?.toString() ?: "",
                                                configId = dataMap["configId"]?.toString() ?: "",
                                                username = dataMap["username"]?.toString() ?: "",
                                                symbol = dataMap["symbol"]?.toString() ?: "",
                                                signalType = dataMap["signalType"]?.toString() ?: "",
                                                direction = dataMap["direction"]?.toString() ?: "",
                                                timestamp = (dataMap["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                                price = (dataMap["price"] as? Number)?.toDouble() ?: 0.0,
                                                volume = (dataMap["volume"] as? Number)?.toDouble() ?: 0.0,
                                                reason = dataMap["reason"]?.toString() ?: "",
                                                timeframe = dataMap["timeframe"]?.toString() ?: "",
                                                status = dataMap["status"]?.toString() ?: "ACTIVE",
                                                isRead = dataMap["isRead"] as? Boolean ?: false,
                                                // CCI 관련
                                                cciValue = (dataMap["cciValue"] as? Number)?.toDouble() ?: 0.0,
                                                cciBreakoutValue = (dataMap["cciBreakoutValue"] as? Number)?.toDouble() ?: 0.0,
                                                cciEntryValue = (dataMap["cciEntryValue"] as? Number)?.toDouble() ?: 0.0,
                                                // RSI 관련
                                                rsiValue = (dataMap["rsiValue"] as? Number)?.toDouble() ?: 0.0,
                                                rsiOverbought = (dataMap["rsiOverbought"] as? Number)?.toDouble() ?: 0.0,
                                                rsiOversold = (dataMap["rsiOversold"] as? Number)?.toDouble() ?: 0.0,
                                                // 코르타 관련
                                                cortaMacdLine = (dataMap["cortaMacdLine"] as? Number)?.toDouble() ?: 0.0,
                                                cortaSignalLine = (dataMap["cortaSignalLine"] as? Number)?.toDouble() ?: 0.0,
                                                cortaHistogram = (dataMap["cortaHistogram"] as? Number)?.toDouble() ?: 0.0,
                                                cortaVolumeRatio = (dataMap["cortaVolumeRatio"] as? Number)?.toDouble() ?: 0.0,
                                                cortaRsiConfirm = (dataMap["cortaRsiConfirm"] as? Number)?.toDouble() ?: 0.0
                                            )
                                        } else null
                                    }
                                } else {
                                    emptyList()
                                }
                                Log.d(TAG, "신호 조회 성공: ${signals.size}개")
                                callback(signals, null)
                            } catch (e: Exception) {
                                Log.e(TAG, "신호 파싱 오류: ${e.message}")
                                callback(null, "파싱 오류: ${e.message}")
                            }
                        } else {
                            Log.e(TAG, "신호 조회 실패: ${result?.message}")
                            callback(null, result?.message)
                        }
                    } else {
                        Log.e(TAG, "신호 조회 HTTP 오류: ${response.code()}")
                        callback(null, "서버 오류: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<MarketSignalApiResponse>, t: Throwable) {
                    Log.e(TAG, "신호 조회 네트워크 오류: ${t.message}")
                    callback(null, "네트워크 오류: ${t.message}")
                }
            })
    }

    /**
     * 신호 읽음 처리
     */
    fun markSignalAsRead(
        signalId: String,
        callback: (Boolean, String?) -> Unit
    ) {
        Log.d(TAG, "신호 읽음 처리 요청: $signalId")

        api.markSignalAsRead(signalId = signalId)
            .enqueue(object : Callback<MarketSignalApiResponse> {
                override fun onResponse(
                    call: Call<MarketSignalApiResponse>,
                    response: Response<MarketSignalApiResponse>
                ) {
                    if (response.isSuccessful) {
                        val result = response.body()
                        Log.d(TAG, "신호 읽음 처리 응답: ${result?.success}")
                        callback(result?.success == true, result?.message)
                    } else {
                        Log.e(TAG, "신호 읽음 처리 HTTP 오류: ${response.code()}")
                        callback(false, "서버 오류: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<MarketSignalApiResponse>, t: Throwable) {
                    Log.e(TAG, "신호 읽음 처리 네트워크 오류: ${t.message}")
                    callback(false, "네트워크 오류: ${t.message}")
                }
            })
    }
}