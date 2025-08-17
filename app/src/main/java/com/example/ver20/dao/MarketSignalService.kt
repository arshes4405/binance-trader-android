// MarketSignalService.kt - 시세포착 로직 서비스

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
}

class MarketSignalService {
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

    // 기존 프로젝트의 BinanceKlineApi 인터페이스 재사용
    private val binanceApi = binanceRetrofit.create(BinanceKlineApi::class.java)

    // CCI 계산 함수
    fun calculateCCI(klineData: List<KlineData>, period: Int = 20): List<CciValue> {
        if (klineData.size < period) return emptyList()

        val cciValues = mutableListOf<CciValue>()

        for (i in period - 1 until klineData.size) {
            val window = klineData.subList(i - period + 1, i + 1)
            val typicalPrices = window.map { it.typical }

            // SMA 계산
            val sma = typicalPrices.average()

            // Mean Deviation 계산
            val meanDeviation = typicalPrices.map { abs(it - sma) }.average()

            // CCI 계산
            val cci = if (meanDeviation > 0) {
                (typicalPrices.last() - sma) / (0.015 * meanDeviation)
            } else {
                0.0
            }

            cciValues.add(
                CciValue(
                    timestamp = klineData[i].timestamp,
                    price = klineData[i].close,
                    volume = klineData[i].volume,
                    cciValue = cci
                )
            )
        }

        return cciValues
    }

    // 시세 데이터 가져오기
    suspend fun getKlineData(symbol: String, interval: String, limit: Int = 100): List<KlineData> {
        return withContext(Dispatchers.IO) {
            try {
                val response = binanceApi.getKlines(symbol, interval, limit)

                if (response.isSuccessful) {
                    val klineList = mutableListOf<KlineData>()
                    val responseBody = response.body() ?: return@withContext emptyList()

                    for (kline in responseBody) {
                        try {
                            val klineArray = kline as List<*>
                            val klineData = KlineData(
                                timestamp = when (val ts = klineArray[0]) {
                                    is Number -> ts.toLong()
                                    is String -> ts.toLong()
                                    else -> 0L
                                },
                                open = (klineArray[1] as? String)?.toDoubleOrNull() ?: 0.0,
                                high = (klineArray[2] as? String)?.toDoubleOrNull() ?: 0.0,
                                low = (klineArray[3] as? String)?.toDoubleOrNull() ?: 0.0,
                                close = (klineArray[4] as? String)?.toDoubleOrNull() ?: 0.0,
                                volume = (klineArray[5] as? String)?.toDoubleOrNull() ?: 0.0
                            )
                            klineList.add(klineData)
                        } catch (e: Exception) {
                            Log.e("MarketSignalService", "K-line 데이터 파싱 오류: ${e.message}")
                            continue
                        }
                    }

                    klineList
                } else {
                    Log.e("MarketSignalService", "K-line API 응답 실패: ${response.code()}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("MarketSignalService", "K-line 데이터 가져오기 실패: ${e.message}")
                emptyList()
            }
        }
    }

    // CCI 시세포착 조건 체크
    fun checkCciSignal(
        cciValues: List<CciValue>,
        breakoutValue: Double,
        entryValue: Double
    ): SignalCheckResult {
        if (cciValues.size < 3) {
            return SignalCheckResult(false)
        }

        val current = cciValues.last()
        val previous = cciValues[cciValues.size - 2]
        val beforePrevious = cciValues[cciValues.size - 3]

        // 롱 진입 조건: CCI가 -돌파값 아래로 이탈 이후 -진입값 안으로 진입
        val longCondition =
            beforePrevious.cciValue > -breakoutValue && // 이전에는 돌파값 위
                    previous.cciValue <= -breakoutValue && // 돌파값 아래로 이탈
                    current.cciValue >= -entryValue // 진입값 안으로 진입

        // 숏 진입 조건: CCI가 +돌파값 위로 이탈 이후 +진입값 안으로 진입
        val shortCondition =
            beforePrevious.cciValue < breakoutValue && // 이전에는 돌파값 아래
                    previous.cciValue >= breakoutValue && // 돌파값 위로 이탈
                    current.cciValue <= entryValue // 진입값 안으로 진입

        return when {
            longCondition -> SignalCheckResult(
                hasSignal = true,
                direction = "LONG",
                reason = "CCI가 -${breakoutValue} 아래 이탈 후 -${entryValue} 진입",
                cciValue = current.cciValue,
                price = current.price,
                volume = current.volume
            )
            shortCondition -> SignalCheckResult(
                hasSignal = true,
                direction = "SHORT",
                reason = "CCI가 +${breakoutValue} 위 이탈 후 +${entryValue} 진입",
                cciValue = current.cciValue,
                price = current.price,
                volume = current.volume
            )
            else -> SignalCheckResult(false)
        }
    }

    // 시세포착 설정 저장
    fun saveSignalConfig(
        config: MarketSignalConfig,
        callback: (Boolean, String?) -> Unit
    ) {
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
                    callback(result?.success == true, result?.message)
                } else {
                    callback(false, "서버 응답 오류: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<MarketSignalApiResponse>, t: Throwable) {
                callback(false, "네트워크 오류: ${t.message}")
            }
        })
    }

    // 시세포착 신호 저장
    fun saveSignal(
        signal: MarketSignal,
        callback: (Boolean, String?) -> Unit
    ) {
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
                    callback(result?.success == true, result?.message)
                } else {
                    callback(false, "서버 응답 오류: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<MarketSignalApiResponse>, t: Throwable) {
                callback(false, "네트워크 오류: ${t.message}")
            }
        })
    }

    // 시세포착 설정 조회
    fun getSignalConfigs(
        username: String,
        callback: (List<MarketSignalConfig>?, String?) -> Unit
    ) {
        api.getSignalConfigs(username = username).enqueue(object : Callback<MarketSignalApiResponse> {
            override fun onResponse(call: Call<MarketSignalApiResponse>, response: Response<MarketSignalApiResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    if (result?.success == true) {
                        // 실제 구현에서는 데이터 파싱 로직 필요
                        callback(emptyList(), null)
                    } else {
                        callback(null, result?.message)
                    }
                } else {
                    callback(null, "서버 응답 오류: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<MarketSignalApiResponse>, t: Throwable) {
                callback(null, "네트워크 오류: ${t.message}")
            }
        })
    }

    // 시세포착 신호 조회
    fun getSignals(
        username: String,
        callback: (List<MarketSignal>?, String?) -> Unit
    ) {
        api.getSignals(username = username).enqueue(object : Callback<MarketSignalApiResponse> {
            override fun onResponse(call: Call<MarketSignalApiResponse>, response: Response<MarketSignalApiResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    if (result?.success == true) {
                        // 실제 구현에서는 데이터 파싱 로직 필요
                        callback(emptyList(), null)
                    } else {
                        callback(null, result?.message)
                    }
                } else {
                    callback(null, "서버 응답 오류: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<MarketSignalApiResponse>, t: Throwable) {
                callback(null, "네트워크 오류: ${t.message}")
            }
        })
    }

    // 실시간 시세포착 체크 (코루틴 사용)
    suspend fun startSignalMonitoring(
        config: MarketSignalConfig,
        onSignalDetected: (MarketSignal) -> Unit
    ) {
        while (config.isActive) {
            try {
                // K-line 데이터 가져오기
                val klineData = getKlineData(config.symbol, config.timeframe, 100)

                if (klineData.isNotEmpty()) {
                    // CCI 계산
                    val cciValues = calculateCCI(klineData, config.cciPeriod)

                    if (cciValues.isNotEmpty()) {
                        // 시그널 체크
                        val signalResult = checkCciSignal(
                            cciValues,
                            config.cciBreakoutValue,
                            config.cciEntryValue
                        )

                        if (signalResult.hasSignal) {
                            val signal = MarketSignal(
                                configId = config.id,
                                username = config.username,
                                symbol = config.symbol,
                                signalType = config.signalType,
                                direction = signalResult.direction!!,
                                price = signalResult.price,
                                volume = signalResult.volume,
                                cciValue = signalResult.cciValue,
                                cciBreakoutValue = config.cciBreakoutValue,
                                cciEntryValue = config.cciEntryValue,
                                reason = signalResult.reason,
                                timeframe = config.timeframe
                            )

                            onSignalDetected(signal)
                        }
                    }
                }

                // 설정된 인터벌만큼 대기
                delay(config.checkInterval * 1000L)

            } catch (e: Exception) {
                Log.e("MarketSignalService", "시세포착 모니터링 오류: ${e.message}")
                delay(60000) // 오류 시 1분 대기
            }
        }
    }
}