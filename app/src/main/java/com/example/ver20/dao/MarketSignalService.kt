// MarketSignalService.kt - 상태 기반 시세포착 서비스 (완전 재작성)

package com.example.ver20.dao

import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import kotlin.math.*
import kotlinx.coroutines.*

// 시그널 상태 정의
enum class CciSignalState {
    NO_BREAKOUT,     // 미돌파 (기본 상태)
    LONG_BREAKOUT,   // 롱 돌파 상태 (CCI가 -돌파값 아래로 내려감)
    SHORT_BREAKOUT   // 숏 돌파 상태 (CCI가 +돌파값 위로 올라감)
}

// 시그널 상태 저장용 데이터 클래스
data class CciMonitoringState(
    val configId: String,
    val currentState: CciSignalState,
    val lastCciValue: Double,
    val lastCheckTime: Long,
    val breakoutValue: Double,
    val entryValue: Double
)

// MongoDB API 인터페이스 (시세포착용)
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
        @Query("cciPeriod") cciPeriod: Int,
        @Query("cciBreakoutValue") cciBreakoutValue: Double,
        @Query("cciEntryValue") cciEntryValue: Double,
        @Query("seedMoney") seedMoney: Double,
        @Query("isActive") isActive: Boolean
    ): Call<MarketSignalApiResponse>

    // 시세포착 설정 조회
    @GET("api")
    fun getSignalConfigs(
        @Query("action") action: String = "getSignalConfigs",
        @Query("username") username: String
    ): Call<MarketSignalApiResponse>

    // 시세포착 신호 저장
    @GET("api")
    fun saveSignal(
        @Query("action") action: String = "saveSignal",
        @Query("configId") configId: String,
        @Query("username") username: String,
        @Query("symbol") symbol: String,
        @Query("signalType") signalType: String,
        @Query("direction") direction: String,
        @Query("price") price: Double,
        @Query("volume") volume: Double,
        @Query("cciValue") cciValue: Double,
        @Query("cciBreakoutValue") cciBreakoutValue: Double,
        @Query("cciEntryValue") cciEntryValue: Double,
        @Query("reason") reason: String,
        @Query("timeframe") timeframe: String
    ): Call<MarketSignalApiResponse>

    // 시세포착 신호 조회
    @GET("api")
    fun getSignals(
        @Query("action") action: String = "getSignals",
        @Query("username") username: String,
        @Query("limit") limit: Int = 50
    ): Call<MarketSignalApiResponse>

    // 돌파 상태 저장
    @GET("api")
    fun saveBreakoutState(
        @Query("action") action: String = "saveBreakoutState",
        @Query("configId") configId: String,
        @Query("username") username: String,
        @Query("symbol") symbol: String,
        @Query("currentState") currentState: String,
        @Query("lastCciValue") lastCciValue: Double,
        @Query("breakoutValue") breakoutValue: Double,
        @Query("entryValue") entryValue: Double
    ): Call<MarketSignalApiResponse>

    // 돌파 상태 조회
    @GET("api")
    fun getBreakoutState(
        @Query("action") action: String = "getBreakoutState",
        @Query("configId") configId: String
    ): Call<MarketSignalApiResponse>

    // 모든 돌파 상태 조회
    @GET("api")
    fun getAllBreakoutStates(
        @Query("action") action: String = "getAllBreakoutStates",
        @Query("username") username: String
    ): Call<MarketSignalApiResponse>

    // 돌파 상태 삭제
    @GET("api")
    fun deleteBreakoutState(
        @Query("action") action: String = "deleteBreakoutState",
        @Query("configId") configId: String
    ): Call<MarketSignalApiResponse>

}

class MarketSignalService {
    companion object {
        private const val TAG = "MarketSignalService"
    }

    private val baseUrl = "https://binance-trader-api.vercel.app/"
    private val binanceBaseUrl = "https://api.binance.com/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val binanceRetrofit = Retrofit.Builder()
        .baseUrl(binanceBaseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(MarketSignalApi::class.java)
    private val binanceApi = binanceRetrofit.create(BinanceKlineApi::class.java)

    // 각 설정별 상태 저장 (configId를 키로 사용)
    private val monitoringStates = mutableMapOf<String, CciMonitoringState>()

    // ===== 시세포착 설정 관련 =====

    /**
     * 시세포착 설정 저장
     */
    fun saveSignalConfig(
        config: MarketSignalConfig,
        callback: (Boolean, String?) -> Unit
    ) {
        Log.d(TAG, "설정 저장 시작: ${config.username} - ${config.signalType} - ${config.symbol}")

        api.saveSignalConfig(
            username = config.username,
            signalType = config.signalType,
            symbol = config.symbol,
            timeframe = config.timeframe,
            checkInterval = config.checkInterval,
            cciPeriod = config.cciPeriod,
            cciBreakoutValue = config.cciBreakoutValue,
            cciEntryValue = config.cciEntryValue,
            seedMoney = config.seedMoney,
            isActive = config.isActive
        ).enqueue(object : Callback<MarketSignalApiResponse> {
            override fun onResponse(call: Call<MarketSignalApiResponse>, response: Response<MarketSignalApiResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    Log.d(TAG, "설정 저장 응답: success=${result?.success}, message=${result?.message}")
                    callback(result?.success == true, result?.message)
                } else {
                    Log.e(TAG, "설정 저장 실패: HTTP ${response.code()}")
                    callback(false, "서버 응답 오류: ${response.code()}")
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
        Log.d(TAG, "설정 조회 시작: $username")

        api.getSignalConfigs(username = username).enqueue(object : Callback<MarketSignalApiResponse> {
            override fun onResponse(call: Call<MarketSignalApiResponse>, response: Response<MarketSignalApiResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    Log.d(TAG, "설정 조회 응답: success=${result?.success}")

                    if (result?.success == true) {
                        try {
                            val dataList = result.data as? List<*>

                            if (dataList != null) {
                                val configs = dataList.mapNotNull { item ->
                                    val dataMap = item as? Map<*, *>
                                    if (dataMap != null) {
                                        try {
                                            MarketSignalConfig(
                                                id = dataMap["_id"]?.toString() ?: "",
                                                username = dataMap["username"]?.toString() ?: "",
                                                signalType = dataMap["signalType"]?.toString() ?: "",
                                                symbol = dataMap["symbol"]?.toString() ?: "",
                                                timeframe = dataMap["timeframe"]?.toString() ?: "",
                                                checkInterval = (dataMap["checkInterval"] as? Number)?.toInt() ?: 900,
                                                isActive = dataMap["isActive"] as? Boolean ?: true,
                                                createdAt = dataMap["createdAt"]?.toString() ?: "",
                                                cciPeriod = (dataMap["cciPeriod"] as? Number)?.toInt() ?: 20,
                                                cciBreakoutValue = (dataMap["cciBreakoutValue"] as? Number)?.toDouble() ?: 100.0,
                                                cciEntryValue = (dataMap["cciEntryValue"] as? Number)?.toDouble() ?: 90.0,
                                                seedMoney = (dataMap["seedMoney"] as? Number)?.toDouble() ?: 1000.0
                                            )
                                        } catch (e: Exception) {
                                            Log.e(TAG, "설정 데이터 파싱 오류: ${e.message}")
                                            null
                                        }
                                    } else null
                                }

                                Log.d(TAG, "설정 조회 성공: ${configs.size}개")
                                callback(configs, null)
                            } else {
                                Log.d(TAG, "설정 데이터가 없음")
                                callback(emptyList(), null)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "응답 파싱 오류: ${e.message}")
                            callback(null, "응답 파싱 오류: ${e.message}")
                        }
                    } else {
                        callback(null, result?.message)
                    }
                } else {
                    Log.e(TAG, "설정 조회 실패: HTTP ${response.code()}")
                    callback(null, "서버 응답 오류: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<MarketSignalApiResponse>, t: Throwable) {
                Log.e(TAG, "설정 조회 네트워크 오류: ${t.message}")
                callback(null, "네트워크 오류: ${t.message}")
            }
        })
    }

    // ===== 시세포착 신호 관련 =====

    /**
     * 시세포착 신호 저장
     */
    fun saveSignal(
        signal: MarketSignal,
        callback: (Boolean, String?) -> Unit
    ) {
        Log.d(TAG, "신호 저장 시작: ${signal.username} - ${signal.symbol} - ${signal.direction}")

        api.saveSignal(
            configId = signal.configId,
            username = signal.username,
            symbol = signal.symbol,
            signalType = signal.signalType,
            direction = signal.direction,
            price = signal.price,
            volume = signal.volume,
            cciValue = signal.cciValue,
            cciBreakoutValue = signal.cciBreakoutValue,
            cciEntryValue = signal.cciEntryValue,
            reason = signal.reason,
            timeframe = signal.timeframe
        ).enqueue(object : Callback<MarketSignalApiResponse> {
            override fun onResponse(call: Call<MarketSignalApiResponse>, response: Response<MarketSignalApiResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    Log.d(TAG, "신호 저장 응답: success=${result?.success}")
                    callback(result?.success == true, result?.message)
                } else {
                    Log.e(TAG, "신호 저장 실패: HTTP ${response.code()}")
                    callback(false, "서버 응답 오류: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<MarketSignalApiResponse>, t: Throwable) {
                Log.e(TAG, "신호 저장 네트워크 오류: ${t.message}")
                callback(false, "네트워크 오류: ${t.message}")
            }
        })
    }

    /**
     * 시세포착 신호 조회
     */
    fun getSignals(
        username: String,
        callback: (List<MarketSignal>?, String?) -> Unit
    ) {
        Log.d(TAG, "신호 조회 시작: $username")

        api.getSignals(username = username).enqueue(object : Callback<MarketSignalApiResponse> {
            override fun onResponse(call: Call<MarketSignalApiResponse>, response: Response<MarketSignalApiResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    Log.d(TAG, "신호 조회 응답: success=${result?.success}")

                    if (result?.success == true) {
                        try {
                            val dataList = result.data as? List<*>

                            if (dataList != null) {
                                val signals = dataList.mapNotNull { item ->
                                    val dataMap = item as? Map<*, *>
                                    if (dataMap != null) {
                                        try {
                                            MarketSignal(
                                                id = dataMap["_id"]?.toString() ?: "",
                                                configId = dataMap["configId"]?.toString() ?: "",
                                                username = dataMap["username"]?.toString() ?: "",
                                                symbol = dataMap["symbol"]?.toString() ?: "",
                                                signalType = dataMap["signalType"]?.toString() ?: "",
                                                direction = dataMap["direction"]?.toString() ?: "",
                                                timestamp = dataMap["timestamp"]?.toString() ?: "",
                                                price = (dataMap["price"] as? Number)?.toDouble() ?: 0.0,
                                                volume = (dataMap["volume"] as? Number)?.toDouble() ?: 0.0,
                                                cciValue = (dataMap["cciValue"] as? Number)?.toDouble() ?: 0.0,
                                                cciBreakoutValue = (dataMap["cciBreakoutValue"] as? Number)?.toDouble() ?: 0.0,
                                                cciEntryValue = (dataMap["cciEntryValue"] as? Number)?.toDouble() ?: 0.0,
                                                reason = dataMap["reason"]?.toString() ?: "",
                                                timeframe = dataMap["timeframe"]?.toString() ?: "",
                                                status = dataMap["status"]?.toString() ?: "ACTIVE",
                                                isRead = dataMap["isRead"] as? Boolean ?: false
                                            )
                                        } catch (e: Exception) {
                                            Log.e(TAG, "신호 데이터 파싱 오류: ${e.message}")
                                            null
                                        }
                                    } else null
                                }

                                Log.d(TAG, "신호 조회 성공: ${signals.size}개")
                                callback(signals, null)
                            } else {
                                Log.d(TAG, "신호 데이터가 없음")
                                callback(emptyList(), null)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "응답 파싱 오류: ${e.message}")
                            callback(null, "응답 파싱 오류: ${e.message}")
                        }
                    } else {
                        callback(null, result?.message)
                    }
                } else {
                    Log.e(TAG, "신호 조회 실패: HTTP ${response.code()}")
                    callback(null, "서버 응답 오류: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<MarketSignalApiResponse>, t: Throwable) {
                Log.e(TAG, "신호 조회 네트워크 오류: ${t.message}")
                callback(null, "네트워크 오류: ${t.message}")
            }
        })
    }

    // ===== 상태 기반 실시간 모니터링 =====

    /**
     * 상태 기반 실시간 시세포착 모니터링
     */
    suspend fun startSignalMonitoring(
        config: MarketSignalConfig,
        onSignalDetected: (MarketSignal) -> Unit
    ) {
        Log.d(TAG, "🎯 상태 기반 시세포착 시작: ${config.symbol} (${config.timeframe} 차트)")
        Log.d(TAG, "설정: 돌파값=${config.cciBreakoutValue}, 진입값=${config.cciEntryValue}, 체크간격=${config.checkInterval/60}분")

        while (config.isActive) {
            try {
                // 1. 현재 CCI 값 계산
                val currentCci = getCurrentCci(config)

                if (currentCci != null) {
                    // 2. 상태 기반 시그널 처리
                    processStateBasedSignal(config, currentCci, onSignalDetected)
                }

                // 3. 설정된 인터벌만큼 대기
                delay(config.checkInterval * 1000L)

            } catch (e: Exception) {
                Log.e(TAG, "모니터링 오류: ${e.message}")
                delay(60000)
            }
        }
    }

    /**
     * 현재 CCI 값 계산
     */
    private suspend fun getCurrentCci(config: MarketSignalConfig): Double? {
        return try {
            val klineData = getKlineData(config.symbol, config.timeframe, 100)
            if (klineData.isNotEmpty()) {
                val cciValues = calculateCCI(klineData, config.cciPeriod)
                if (cciValues.isNotEmpty()) {
                    cciValues.last().cciValue
                } else null
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "CCI 계산 오류: ${e.message}")
            null
        }
    }

    /**
     * 상태 기반 시그널 처리 로직
     */
    private suspend fun processStateBasedSignal(
        config: MarketSignalConfig,
        currentCci: Double,
        onSignalDetected: (MarketSignal) -> Unit
    ) {
        val currentTime = System.currentTimeMillis()
        val configId = config.id

        // 현재 상태 가져오기 (없으면 초기 상태로 생성)
        val currentState = monitoringStates[configId] ?: CciMonitoringState(
            configId = configId,
            currentState = CciSignalState.NO_BREAKOUT,
            lastCciValue = currentCci,
            lastCheckTime = currentTime,
            breakoutValue = config.cciBreakoutValue,
            entryValue = config.cciEntryValue
        )

        Log.d(TAG, "📊 ${config.symbol} CCI: ${currentCci} (상태: ${currentState.currentState})")

        when (currentState.currentState) {
            CciSignalState.NO_BREAKOUT -> {
                // 미돌파 상태에서 돌파 조건 체크
                val newState = checkBreakoutCondition(currentState, currentCci, config)
                monitoringStates[configId] = newState

                if (newState.currentState != CciSignalState.NO_BREAKOUT) {
                    val direction = if (newState.currentState == CciSignalState.LONG_BREAKOUT) "LONG" else "SHORT"
                    Log.d(TAG, "🔥 돌파 감지: ${config.symbol} $direction (CCI: $currentCci)")
                }
            }

            CciSignalState.LONG_BREAKOUT -> {
                // 롱 돌파 상태에서 진입 조건 체크
                if (currentCci >= -config.cciEntryValue) {
                    // 진입 조건 만족 → 시그널 생성 및 상태 리셋
                    generateSignal(config, currentCci, "LONG", "CCI 롱 돌파 후 진입", onSignalDetected)

                    // 상태를 미돌파로 리셋
                    monitoringStates[configId] = currentState.copy(
                        currentState = CciSignalState.NO_BREAKOUT,
                        lastCciValue = currentCci,
                        lastCheckTime = currentTime
                    )

                    Log.d(TAG, "✅ 롱 진입 완료 및 상태 리셋: ${config.symbol}")
                } else {
                    // 진입 조건 미만족 → 상태 유지
                    monitoringStates[configId] = currentState.copy(
                        lastCciValue = currentCci,
                        lastCheckTime = currentTime
                    )
                }
            }

            CciSignalState.SHORT_BREAKOUT -> {
                // 숏 돌파 상태에서 진입 조건 체크
                if (currentCci <= config.cciEntryValue) {
                    // 진입 조건 만족 → 시그널 생성 및 상태 리셋
                    generateSignal(config, currentCci, "SHORT", "CCI 숏 돌파 후 진입", onSignalDetected)

                    // 상태를 미돌파로 리셋
                    monitoringStates[configId] = currentState.copy(
                        currentState = CciSignalState.NO_BREAKOUT,
                        lastCciValue = currentCci,
                        lastCheckTime = currentTime
                    )

                    Log.d(TAG, "✅ 숏 진입 완료 및 상태 리셋: ${config.symbol}")
                } else {
                    // 진입 조건 미만족 → 상태 유지
                    monitoringStates[configId] = currentState.copy(
                        lastCciValue = currentCci,
                        lastCheckTime = currentTime
                    )
                }
            }
        }
    }

    /**
     * 돌파 조건 체크 및 상태 업데이트
     */
    private fun checkBreakoutCondition(
        currentState: CciMonitoringState,
        currentCci: Double,
        config: MarketSignalConfig
    ): CciMonitoringState {
        return when {
            // 롱 돌파 조건: CCI가 -돌파값 아래로 내려감
            currentCci <= -config.cciBreakoutValue -> {
                currentState.copy(
                    currentState = CciSignalState.LONG_BREAKOUT,
                    lastCciValue = currentCci,
                    lastCheckTime = System.currentTimeMillis()
                )
            }

            // 숏 돌파 조건: CCI가 +돌파값 위로 올라감
            currentCci >= config.cciBreakoutValue -> {
                currentState.copy(
                    currentState = CciSignalState.SHORT_BREAKOUT,
                    lastCciValue = currentCci,
                    lastCheckTime = System.currentTimeMillis()
                )
            }

            // 돌파 조건 미만족
            else -> {
                currentState.copy(
                    lastCciValue = currentCci,
                    lastCheckTime = System.currentTimeMillis()
                )
            }
        }
    }

    /**
     * 시그널 생성 및 전송
     */
    private suspend fun generateSignal(
        config: MarketSignalConfig,
        currentCci: Double,
        direction: String,
        reason: String,
        onSignalDetected: (MarketSignal) -> Unit
    ) {
        try {
            // 현재 가격 정보 가져오기
            val klineData = getKlineData(config.symbol, config.timeframe, 1)
            val currentPrice = if (klineData.isNotEmpty()) klineData.last().close else 0.0
            val currentVolume = if (klineData.isNotEmpty()) klineData.last().volume else 0.0

            val signal = MarketSignal(
                configId = config.id,
                username = config.username,
                symbol = config.symbol,
                signalType = config.signalType,
                direction = direction,
                price = currentPrice,
                volume = currentVolume,
                cciValue = currentCci,
                cciBreakoutValue = config.cciBreakoutValue,
                cciEntryValue = config.cciEntryValue,
                reason = reason,
                timeframe = config.timeframe
            )

            Log.d(TAG, "🚨 시그널 생성: ${config.symbol} $direction (CCI: $currentCci, 가격: $currentPrice)")
            onSignalDetected(signal)

        } catch (e: Exception) {
            Log.e(TAG, "시그널 생성 오류: ${e.message}")
        }
    }

    // ===== CCI 계산 및 데이터 처리 =====

    /**
     * CCI 계산 함수
     */
    fun calculateCCI(klineData: List<KlineData>, period: Int = 20): List<CciValue> {
        if (klineData.size < period) return emptyList()

        val results = mutableListOf<CciValue>()

        for (i in period - 1 until klineData.size) {
            val periodData = klineData.subList(i - period + 1, i + 1)

            // Typical Price 계산
            val typicalPrices = periodData.map { it.typical }
            val smaTypical = typicalPrices.average()

            // Mean Deviation 계산
            val meanDeviation = typicalPrices.map { abs(it - smaTypical) }.average()

            // CCI 계산
            val cci = if (meanDeviation != 0.0) {
                (typicalPrices.last() - smaTypical) / (0.015 * meanDeviation)
            } else {
                0.0
            }

            results.add(
                CciValue(
                    timestamp = klineData[i].timestamp,
                    price = klineData[i].close,
                    volume = klineData[i].volume,
                    cciValue = cci
                )
            )
        }

        return results
    }

    /**
     * 바이낸스 K-line 데이터 가져오기
     */
    suspend fun getKlineData(
        symbol: String,
        interval: String,
        limit: Int = 100
    ): List<KlineData> {
        return withContext(Dispatchers.IO) {
            try {
                val response = binanceApi.getKlines(
                    symbol = symbol,
                    interval = interval,
                    limit = limit
                )

                val klineList = mutableListOf<KlineData>()

                if (response.isSuccessful && response.body() != null) {
                    val klineArray = response.body()!!

                    for (klineData in klineArray) {
                        try {
                            val klineArrayItem = klineData as List<*>
                            val kline = KlineData(
                                timestamp = (klineArrayItem[0] as? Number)?.toLong() ?: 0L,
                                open = (klineArrayItem[1] as? String)?.toDoubleOrNull() ?: 0.0,
                                high = (klineArrayItem[2] as? String)?.toDoubleOrNull() ?: 0.0,
                                low = (klineArrayItem[3] as? String)?.toDoubleOrNull() ?: 0.0,
                                close = (klineArrayItem[4] as? String)?.toDoubleOrNull() ?: 0.0,
                                volume = (klineArrayItem[5] as? String)?.toDoubleOrNull() ?: 0.0
                            )
                            klineList.add(kline)
                        } catch (e: Exception) {
                            Log.e(TAG, "K-line 데이터 파싱 오류: ${e.message}")
                            continue
                        }
                    }

                    Log.d(TAG, "K-line 데이터 조회 성공: ${klineList.size}개")
                    klineList
                } else {
                    Log.e(TAG, "K-line API 응답 실패: ${response.code()}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "K-line 데이터 가져오기 실패: ${e.message}")
                emptyList()
            }
        }
    }

    // ===== 유틸리티 함수들 =====

    /**
     * 모니터링 상태 초기화 (설정 변경 시 사용)
     */
    fun resetMonitoringState(configId: String) {
        monitoringStates.remove(configId)
        Log.d(TAG, "모니터링 상태 초기화: $configId")
    }

    /**
     * 모든 모니터링 상태 조회 (디버깅용)
     */
    fun getAllMonitoringStates(): Map<String, CciMonitoringState> {
        return monitoringStates.toMap()
    }

    /**
     * 특정 설정의 현재 상태 조회
     */
    fun getMonitoringState(configId: String): CciMonitoringState? {
        return monitoringStates[configId]
    }
}