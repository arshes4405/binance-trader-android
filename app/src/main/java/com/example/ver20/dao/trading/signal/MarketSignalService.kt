// MarketSignalService.kt - DB 기반 상태 시세포착 서비스

package com.example.ver20.dao.trading.signal

import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import kotlin.math.*
import kotlinx.coroutines.*

// 바이낸스 K-line API 인터페이스
interface BinanceKlineApiForSinalService {
    @GET("api/v3/klines")
    fun getKlines(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("limit") limit: Int = 500
    ): Call<List<List<Any>>>
}

// MongoDB API 인터페이스
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

    // DB 기반 돌파 상태 저장
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

    // DB 기반 돌파 상태 조회
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
    private val binanceApi = binanceRetrofit.create(BinanceKlineApiForSinalService::class.java)

    // ===== 시세포착 설정 관리 =====

    /**
     * 시세포착 설정 저장
     */
    fun saveSignalConfig(
        config: MarketSignalConfig,
        callback: (Boolean, String?) -> Unit
    ) {
        Log.d(TAG, "시세포착 설정 저장: ${config.symbol}")

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
            override fun onResponse(
                call: Call<MarketSignalApiResponse>,
                response: Response<MarketSignalApiResponse>
            ) {
                if (response.isSuccessful) {
                    val result = response.body()
                    if (result?.success == true) {
                        Log.d(TAG, "설정 저장 성공")
                        callback(true, null)
                    } else {
                        Log.e(TAG, "설정 저장 실패: ${result?.message}")
                        callback(false, result?.message)
                    }
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
        Log.d(TAG, "시세포착 설정 조회: $username")

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
                                            try {
                                                MarketSignalConfig(
                                                    id = dataMap["_id"]?.toString() ?: "",
                                                    username = dataMap["username"]?.toString() ?: "",
                                                    signalType = dataMap["signalType"]?.toString() ?: "CCI",
                                                    symbol = dataMap["symbol"]?.toString() ?: "",
                                                    timeframe = dataMap["timeframe"]?.toString() ?: "15m",
                                                    checkInterval = (dataMap["checkInterval"] as? Number)?.toInt() ?: 300,
                                                    isActive = dataMap["isActive"] as? Boolean ?: true,
                                                    createdAt = dataMap["createdAt"]?.toString() ?: "",
                                                    cciPeriod = (dataMap["cciPeriod"] as? Number)?.toInt() ?: 20,
                                                    cciBreakoutValue = (dataMap["cciBreakoutValue"] as? Number)?.toDouble() ?: 100.0,
                                                    cciEntryValue = (dataMap["cciEntryValue"] as? Number)?.toDouble() ?: 90.0,
                                                    seedMoney = (dataMap["seedMoney"] as? Number)?.toDouble() ?: 1000.0
                                                )
                                            } catch (e: Exception) {
                                                Log.e(TAG, "설정 파싱 오류: ${e.message}")
                                                null
                                            }
                                        } else null
                                    }
                                } else {
                                    emptyList()
                                }

                                Log.d(TAG, "설정 조회 성공: ${configs.size}개")
                                callback(configs, null)
                            } catch (e: Exception) {
                                Log.e(TAG, "응답 파싱 오류: ${e.message}")
                                callback(null, "응답 파싱 오류: ${e.message}")
                            }
                        } else {
                            callback(null, result?.message)
                        }
                    } else {
                        callback(null, "서버 오류: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<MarketSignalApiResponse>, t: Throwable) {
                    Log.e(TAG, "설정 조회 네트워크 오류: ${t.message}")
                    callback(null, "네트워크 오류: ${t.message}")
                }
            })
    }

    // ===== DB 기반 돌파 상태 관리 =====

    /**
     * 돌파 상태를 DB에서 조회
     */
    private suspend fun getBreakoutStateFromDB(configId: String): BreakoutStateData? {
        return suspendCancellableCoroutine { continuation ->
            api.getBreakoutState(configId = configId)
                .enqueue(object : Callback<MarketSignalApiResponse> {
                    override fun onResponse(
                        call: Call<MarketSignalApiResponse>,
                        response: Response<MarketSignalApiResponse>
                    ) {
                        if (response.isSuccessful) {
                            val result = response.body()
                            if (result?.success == true) {
                                val stateData = result.data
                                if (stateData is Map<*, *>) {
                                    val dataMap = stateData as Map<String, Any>
                                    val breakoutState = BreakoutStateData(
                                        configId = dataMap["configId"]?.toString() ?: configId,
                                        currentState = CciSignalState.valueOf(
                                            dataMap["currentState"]?.toString() ?: "NO_BREAKOUT"
                                        ),
                                        lastCciValue = (dataMap["lastCciValue"] as? Number)?.toDouble() ?: 0.0,
                                        breakoutValue = (dataMap["breakoutValue"] as? Number)?.toDouble() ?: 100.0,
                                        entryValue = (dataMap["entryValue"] as? Number)?.toDouble() ?: 90.0,
                                        lastCheckTime = System.currentTimeMillis()
                                    )
                                    Log.d(TAG, "DB에서 상태 조회 성공: $configId - ${breakoutState.currentState}")
                                    continuation.resume(breakoutState, null)
                                } else {
                                    Log.d(TAG, "DB에 상태가 없음: $configId")
                                    continuation.resume(null, null)
                                }
                            } else {
                                Log.d(TAG, "DB 상태 조회 결과 없음: $configId")
                                continuation.resume(null, null)
                            }
                        } else {
                            Log.e(TAG, "DB 상태 조회 HTTP 오류: ${response.code()}")
                            continuation.resume(null, null)
                        }
                    }

                    override fun onFailure(call: Call<MarketSignalApiResponse>, t: Throwable) {
                        Log.e(TAG, "DB 상태 조회 네트워크 오류: ${t.message}")
                        continuation.resume(null, null)
                    }
                })
        }
    }

    /**
     * 돌파 상태를 DB에 저장
     */
    private suspend fun saveBreakoutStateToDB(state: BreakoutStateData, config: MarketSignalConfig): Boolean {
        return suspendCancellableCoroutine { continuation ->
            api.saveBreakoutState(
                configId = state.configId,
                username = config.username,
                symbol = config.symbol,
                currentState = state.currentState.name,
                lastCciValue = state.lastCciValue,
                breakoutValue = state.breakoutValue,
                entryValue = state.entryValue
            ).enqueue(object : Callback<MarketSignalApiResponse> {
                override fun onResponse(
                    call: Call<MarketSignalApiResponse>,
                    response: Response<MarketSignalApiResponse>
                ) {
                    if (response.isSuccessful) {
                        val result = response.body()
                        if (result?.success == true) {
                            Log.d(TAG, "DB 상태 저장 성공: ${state.configId} - ${state.currentState}")
                            continuation.resume(true, null)
                        } else {
                            Log.e(TAG, "DB 상태 저장 실패: ${result?.message}")
                            continuation.resume(false, null)
                        }
                    } else {
                        Log.e(TAG, "DB 상태 저장 HTTP 오류: ${response.code()}")
                        continuation.resume(false, null)
                    }
                }

                override fun onFailure(call: Call<MarketSignalApiResponse>, t: Throwable) {
                    Log.e(TAG, "DB 상태 저장 네트워크 오류: ${t.message}")
                    continuation.resume(false, null)
                }
            })
        }
    }

    // ===== 상태 기반 실시간 모니터링 =====

    /**
     * DB 기반 상태 시세포착 모니터링
     */
    suspend fun startSignalMonitoring(
        config: MarketSignalConfig,
        onSignalDetected: (MarketSignal) -> Unit
    ) {
        Log.d(TAG, "🎯 DB 기반 상태 시세포착 시작: ${config.symbol} (${config.timeframe})")
        Log.d(TAG, "설정: 돌파값=${config.cciBreakoutValue}, 진입값=${config.cciEntryValue}")

        while (config.isActive) {
            try {
                // 1. 현재 CCI 계산
                val currentCci = getCurrentCci(config)
                if (currentCci == null) {
                    delay(config.checkInterval * 1000L)
                    continue
                }

                // 2. DB에서 현재 상태 조회
                var currentState = getBreakoutStateFromDB(config.id) ?: BreakoutStateData(
                    configId = config.id,
                    currentState = CciSignalState.NO_BREAKOUT,
                    lastCciValue = currentCci,
                    breakoutValue = config.cciBreakoutValue,
                    entryValue = config.cciEntryValue,
                    lastCheckTime = System.currentTimeMillis()
                )

                Log.d(TAG, "📊 ${config.symbol} CCI: $currentCci (상태: ${currentState.currentState})")

                // 3. 상태별 로직 처리
                val newState = when (currentState.currentState) {
                    CciSignalState.NO_BREAKOUT -> {
                        // 미돌파에서 돌파 체크
                        if (currentCci <= -config.cciBreakoutValue) {
                            // 롱 돌파 감지
                            Log.d(TAG, "🔥 롱 돌파 감지: ${config.symbol} (CCI: $currentCci)")
                            currentState.copy(
                                currentState = CciSignalState.LONG_BREAKOUT,
                                lastCciValue = currentCci,
                                lastCheckTime = System.currentTimeMillis()
                            )
                        } else if (currentCci >= config.cciBreakoutValue) {
                            // 숏 돌파 감지
                            Log.d(TAG, "🔥 숏 돌파 감지: ${config.symbol} (CCI: $currentCci)")
                            currentState.copy(
                                currentState = CciSignalState.SHORT_BREAKOUT,
                                lastCciValue = currentCci,
                                lastCheckTime = System.currentTimeMillis()
                            )
                        } else {
                            // 돌파 없음
                            currentState.copy(
                                lastCciValue = currentCci,
                                lastCheckTime = System.currentTimeMillis()
                            )
                        }
                    }

                    CciSignalState.LONG_BREAKOUT -> {
                        // 롱 돌파 상태에서 진입 체크
                        if (currentCci >= -config.cciEntryValue) {
                            // 진입 조건 만족 → 시그널 생성 및 리셋
                            val latestPrice = getLatestPrice(config.symbol)
                            if (latestPrice != null) {
                                val signal = MarketSignal(
                                    configId = config.id,
                                    username = config.username,
                                    symbol = config.symbol,
                                    signalType = config.signalType,
                                    direction = "LONG",
                                    price = latestPrice.close,
                                    volume = latestPrice.volume,
                                    cciValue = currentCci,
                                    cciBreakoutValue = config.cciBreakoutValue,
                                    cciEntryValue = config.cciEntryValue,
                                    reason = "CCI 롱 돌파 후 진입",
                                    timeframe = config.timeframe
                                )

                                // 시그널 저장 및 콜백
                                saveSignal(signal) { success, _ ->
                                    if (success) {
                                        onSignalDetected(signal)
                                        Log.d(TAG, "✅ 롱 진입 완료: ${config.symbol}")
                                    }
                                }
                            }

                            // 상태 리셋
                            currentState.copy(
                                currentState = CciSignalState.NO_BREAKOUT,
                                lastCciValue = currentCci,
                                lastCheckTime = System.currentTimeMillis()
                            )
                        } else {
                            // 진입 조건 미만족 → 상태 유지
                            currentState.copy(
                                lastCciValue = currentCci,
                                lastCheckTime = System.currentTimeMillis()
                            )
                        }
                    }

                    CciSignalState.SHORT_BREAKOUT -> {
                        // 숏 돌파 상태에서 진입 체크
                        if (currentCci <= config.cciEntryValue) {
                            // 진입 조건 만족 → 시그널 생성 및 리셋
                            val latestPrice = getLatestPrice(config.symbol)
                            if (latestPrice != null) {
                                val signal = MarketSignal(
                                    configId = config.id,
                                    username = config.username,
                                    symbol = config.symbol,
                                    signalType = config.signalType,
                                    direction = "SHORT",
                                    price = latestPrice.close,
                                    volume = latestPrice.volume,
                                    cciValue = currentCci,
                                    cciBreakoutValue = config.cciBreakoutValue,
                                    cciEntryValue = config.cciEntryValue,
                                    reason = "CCI 숏 돌파 후 진입",
                                    timeframe = config.timeframe
                                )

                                // 시그널 저장 및 콜백
                                saveSignal(signal) { success, _ ->
                                    if (success) {
                                        onSignalDetected(signal)
                                        Log.d(TAG, "✅ 숏 진입 완료: ${config.symbol}")
                                    }
                                }
                            }

                            // 상태 리셋
                            currentState.copy(
                                currentState = CciSignalState.NO_BREAKOUT,
                                lastCciValue = currentCci,
                                lastCheckTime = System.currentTimeMillis()
                            )
                        } else {
                            // 진입 조건 미만족 → 상태 유지
                            currentState.copy(
                                lastCciValue = currentCci,
                                lastCheckTime = System.currentTimeMillis()
                            )
                        }
                    }
                }

                // 4. 상태 변화가 있으면 DB에 저장
                if (newState.currentState != currentState.currentState ||
                    abs(newState.lastCciValue - currentState.lastCciValue) > 0.01) {
                    saveBreakoutStateToDB(newState, config)
                }

                delay(config.checkInterval * 1000L)

            } catch (e: Exception) {
                Log.e(TAG, "모니터링 오류: ${e.message}")
                delay(30000) // 30초 후 재시도
            }
        }
    }

    // ===== CCI 계산 및 바이낸스 API =====

    /**
     * 현재 CCI 값 계산
     */
    private suspend fun getCurrentCci(config: MarketSignalConfig): Double? {
        return suspendCancellableCoroutine { continuation ->
            val interval = when (config.timeframe) {
                "15m" -> "15m"
                "1h" -> "1h"
                "4h" -> "4h"
                "1d" -> "1d"
                else -> "15m"
            }

            binanceApi.getKlines(
                symbol = config.symbol,
                interval = interval,
                limit = config.cciPeriod + 50
            ).enqueue(object : Callback<List<List<Any>>> {
                override fun onResponse(
                    call: Call<List<List<Any>>>,
                    response: Response<List<List<Any>>>
                ) {
                    if (response.isSuccessful) {
                        val klines = response.body()
                        if (klines != null && klines.size >= config.cciPeriod) {
                            try {
                                val klineData = klines.map { kline ->
                                    KlineData(
                                        timestamp = (kline[0] as Number).toLong(),
                                        open = (kline[1] as String).toDouble(),
                                        high = (kline[2] as String).toDouble(),
                                        low = (kline[3] as String).toDouble(),
                                        close = (kline[4] as String).toDouble(),
                                        volume = (kline[5] as String).toDouble()
                                    )
                                }

                                val cci = calculateCCI(klineData.takeLast(config.cciPeriod), config.cciPeriod)
                                continuation.resume(cci, null)
                            } catch (e: Exception) {
                                Log.e(TAG, "CCI 계산 오류: ${e.message}")
                                continuation.resume(null, null)
                            }
                        } else {
                            Log.e(TAG, "K-line 데이터 부족")
                            continuation.resume(null, null)
                        }
                    } else {
                        Log.e(TAG, "K-line API 오류: ${response.code()}")
                        continuation.resume(null, null)
                    }
                }

                override fun onFailure(call: Call<List<List<Any>>>, t: Throwable) {
                    Log.e(TAG, "K-line API 네트워크 오류: ${t.message}")
                    continuation.resume(null, null)
                }
            })
        }
    }

    /**
     * 최신 가격 정보 조회
     */
    private suspend fun getLatestPrice(symbol: String): KlineData? {
        return suspendCancellableCoroutine { continuation ->
            binanceApi.getKlines(symbol = symbol, interval = "1m", limit = 1)
                .enqueue(object : Callback<List<List<Any>>> {
                    override fun onResponse(
                        call: Call<List<List<Any>>>,
                        response: Response<List<List<Any>>>
                    ) {
                        if (response.isSuccessful) {
                            val klines = response.body()
                            if (klines != null && klines.isNotEmpty()) {
                                try {
                                    val kline = klines[0]
                                    val klineData = KlineData(
                                        timestamp = (kline[0] as Number).toLong(),
                                        open = (kline[1] as String).toDouble(),
                                        high = (kline[2] as String).toDouble(),
                                        low = (kline[3] as String).toDouble(),
                                        close = (kline[4] as String).toDouble(),
                                        volume = (kline[5] as String).toDouble()
                                    )
                                    continuation.resume(klineData, null)
                                } catch (e: Exception) {
                                    continuation.resume(null, null)
                                }
                            } else {
                                continuation.resume(null, null)
                            }
                        } else {
                            continuation.resume(null, null)
                        }
                    }

                    override fun onFailure(call: Call<List<List<Any>>>, t: Throwable) {
                        continuation.resume(null, null)
                    }
                })
        }
    }

    /**
     * CCI 계산
     */
    private fun calculateCCI(klineData: List<KlineData>, period: Int): Double {
        if (klineData.size < period) return 0.0

        val recentData = klineData.takeLast(period)
        val typicalPrices = recentData.map { it.typical }

        // Simple Moving Average of Typical Price
        val sma = typicalPrices.average()

        // Mean Deviation
        val meanDeviation = typicalPrices.map { abs(it - sma) }.average()

        if (meanDeviation == 0.0) return 0.0

        // CCI = (Typical Price - SMA) / (0.015 * Mean Deviation)
        val latestTypical = typicalPrices.last()
        return (latestTypical - sma) / (0.015 * meanDeviation)
    }

    // ===== 시그널 저장 =====

    /**
     * 시그널 저장
     */
    fun saveSignal(signal: MarketSignal, callback: (Boolean, String?) -> Unit) {
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
            override fun onResponse(
                call: Call<MarketSignalApiResponse>,
                response: Response<MarketSignalApiResponse>
            ) {
                if (response.isSuccessful) {
                    val result = response.body()
                    callback(result?.success == true, result?.message)
                } else {
                    callback(false, "HTTP ${response.code()}")
                }
            }

            override fun onFailure(call: Call<MarketSignalApiResponse>, t: Throwable) {
                callback(false, t.message)
            }
        })
    }

    /**
     * 시그널 조회
     */
    fun getSignals(
        username: String,
        limit: Int = 50,
        callback: (List<MarketSignal>?, String?) -> Unit
    ) {
        Log.d(TAG, "시그널 조회: $username")

        api.getSignals(username = username, limit = limit)
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
                                            try {
                                                MarketSignal(
                                                    id = dataMap["_id"]?.toString() ?: "",
                                                    configId = dataMap["configId"]?.toString() ?: "",
                                                    username = dataMap["username"]?.toString() ?: "",
                                                    symbol = dataMap["symbol"]?.toString() ?: "",
                                                    signalType = dataMap["signalType"]?.toString() ?: "CCI",
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
                                                Log.e(TAG, "시그널 파싱 오류: ${e.message}")
                                                null
                                            }
                                        } else null
                                    }
                                } else {
                                    emptyList()
                                }

                                Log.d(TAG, "시그널 조회 성공: ${signals.size}개")
                                callback(signals, null)
                            } catch (e: Exception) {
                                Log.e(TAG, "응답 파싱 오류: ${e.message}")
                                callback(null, "응답 파싱 오류: ${e.message}")
                            }
                        } else {
                            callback(null, result?.message)
                        }
                    } else {
                        callback(null, "서버 오류: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<MarketSignalApiResponse>, t: Throwable) {
                    Log.e(TAG, "시그널 조회 네트워크 오류: ${t.message}")
                    callback(null, "네트워크 오류: ${t.message}")
                }
            })
    }

    // ===== 상태 관리 유틸리티 =====

    /**
     * 모든 돌파 상태 조회
     */
    fun getAllBreakoutStates(
        username: String,
        callback: (List<BreakoutStateData>?, String?) -> Unit
    ) {
        api.getAllBreakoutStates(username = username)
            .enqueue(object : Callback<MarketSignalApiResponse> {
                override fun onResponse(
                    call: Call<MarketSignalApiResponse>,
                    response: Response<MarketSignalApiResponse>
                ) {
                    if (response.isSuccessful) {
                        val result = response.body()
                        if (result?.success == true) {
                            try {
                                val statesData = result.data
                                val states = if (statesData is List<*>) {
                                    statesData.mapNotNull { item ->
                                        if (item is Map<*, *>) {
                                            val dataMap = item as Map<String, Any>
                                            try {
                                                BreakoutStateData(
                                                    configId = dataMap["configId"]?.toString() ?: "",
                                                    currentState = CciSignalState.valueOf(
                                                        dataMap["currentState"]?.toString() ?: "NO_BREAKOUT"
                                                    ),
                                                    lastCciValue = (dataMap["lastCciValue"] as? Number)?.toDouble() ?: 0.0,
                                                    breakoutValue = (dataMap["breakoutValue"] as? Number)?.toDouble() ?: 100.0,
                                                    entryValue = (dataMap["entryValue"] as? Number)?.toDouble() ?: 90.0,
                                                    lastCheckTime = System.currentTimeMillis()
                                                )
                                            } catch (e: Exception) {
                                                Log.e(TAG, "상태 파싱 오류: ${e.message}")
                                                null
                                            }
                                        } else null
                                    }
                                } else {
                                    emptyList()
                                }

                                callback(states, null)
                            } catch (e: Exception) {
                                callback(null, "파싱 오류: ${e.message}")
                            }
                        } else {
                            callback(null, result?.message)
                        }
                    } else {
                        callback(null, "서버 오류: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<MarketSignalApiResponse>, t: Throwable) {
                    callback(null, "네트워크 오류: ${t.message}")
                }
            })
    }

    /**
     * 돌파 상태 삭제
     */
    fun deleteBreakoutState(
        configId: String,
        callback: (Boolean, String?) -> Unit
    ) {
        api.deleteBreakoutState(configId = configId)
            .enqueue(object : Callback<MarketSignalApiResponse> {
                override fun onResponse(
                    call: Call<MarketSignalApiResponse>,
                    response: Response<MarketSignalApiResponse>
                ) {
                    if (response.isSuccessful) {
                        val result = response.body()
                        callback(result?.success == true, result?.message)
                    } else {
                        callback(false, "서버 오류: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<MarketSignalApiResponse>, t: Throwable) {
                    callback(false, "네트워크 오류: ${t.message}")
                }
            })
    }
}