// RealDataBacktestEngine.kt - 1주일 기간 추가 및 실제 API만 사용 (기존 파일 업데이트)

package com.example.ver20.dao

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

// ===========================================
// 바이낸스 K-line API 인터페이스
// ===========================================

interface BinanceKlineApi {
    @GET("api/v3/klines")
    suspend fun getKlines(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("limit") limit: Int = 1000,
        @Query("startTime") startTime: Long? = null,
        @Query("endTime") endTime: Long? = null
    ): retrofit2.Response<List<List<Any>>>
}

// ===========================================
// 실제 데이터 백테스트 엔진
// ===========================================

class RealDataBacktestEngine {
    companion object {
        private const val TAG = "RealDataBacktestEngine"
        private const val BINANCE_BASE_URL = "https://api.binance.com/"

        // 시간프레임 변환
        private fun getIntervalString(timeframe: String): String {
            return when (timeframe) {
                "1시간" -> "1h"
                "4시간" -> "4h"
                "1일" -> "1d"
                "1주" -> "1w"
                else -> "4h"
            }
        }

        // 기간별 데이터 개수 계산 (1주일 추가)
        private fun getDataLimit(period: String, timeframe: String): Int {
            return when (period) {
                "1주일" -> when (timeframe) {
                    "1시간" -> 168    // 7일 * 24시간
                    "4시간" -> 42     // 7일 * 6개 (24시간/4시간)
                    "1일" -> 7        // 7일
                    else -> 42
                }
                "3개월" -> when (timeframe) {
                    "1시간" -> 2160   // 90일 * 24시간
                    "4시간" -> 540    // 90일 * 6개
                    "1일" -> 90       // 90일
                    else -> 540
                }
                "6개월" -> when (timeframe) {
                    "1시간" -> 4320   // 180일 * 24시간
                    "4시간" -> 1080   // 180일 * 6개
                    "1일" -> 180      // 180일
                    else -> 1080
                }
                "1년" -> when (timeframe) {
                    "1시간" -> 8760   // 365일 * 24시간
                    "4시간" -> 2190   // 365일 * 6개
                    "1일" -> 365      // 365일
                    else -> 2190
                }
                "2년" -> when (timeframe) {
                    "1시간" -> 17520  // 730일 * 24시간
                    "4시간" -> 4380   // 730일 * 6개
                    "1일" -> 730      // 730일
                    else -> 4380
                }
                else -> 168 // 기본값: 1주일
            }
        }
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl(BINANCE_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(BinanceKlineApi::class.java)

    // 메인 백테스트 실행 함수
    suspend fun runRealDataBacktest(settings: CciStrategySettings): CciBacktestResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "🚀 정확한 CCI 물타기 전략 시작 - ${settings.symbol}, ${settings.timeframe}, ${settings.testPeriod}")

        try {
            // 1. 실제 바이낸스 데이터 가져오기
            val priceData = fetchRealPriceData(settings)

            if (priceData.isEmpty()) {
                Log.e(TAG, "❌ 가격 데이터를 가져올 수 없습니다 - API 호출 실패 또는 네트워크 오류")
                throw RuntimeException("바이낸스 API에서 가격 데이터를 가져올 수 없습니다.\n" +
                        "요청 정보: ${settings.symbol}, ${settings.timeframe}, ${settings.testPeriod}\n" +
                        "네트워크 연결 상태를 확인해주세요.")
            }

            // 2. CCI 계산
            val cciData = calculateCCI(priceData, 14)

            if (cciData.isEmpty()) {
                Log.e(TAG, "❌ CCI 계산 실패 - 데이터 부족")
                throw RuntimeException("CCI 지표 계산에 실패했습니다.\n" +
                        "가져온 데이터: ${priceData.size}개\n" +
                        "CCI 계산에는 최소 14개의 데이터가 필요합니다.")
            }

            // 3. 정확한 물타기 전략 실행
            val trades = executeAccurateStrategy(priceData, cciData, settings)

            // 4. 결과 계산
            return@withContext calculateResults(trades, settings)

        } catch (e: Throwable) {
            Log.e(TAG, "❌ 백테스팅 실행 중 오류: ${e.message}")
            throw e // 오류를 다시 던져서 UI에서 처리하도록
        }
    }

    // 실제 바이낸스 데이터 가져오기
    suspend fun fetchRealPriceData(settings: CciStrategySettings): List<PriceCandle> {
        return try {
            Log.d(TAG, "📡 바이낸스 API 호출 시작")
            Log.d(TAG, "요청 정보: 심볼=${settings.symbol}, 시간프레임=${settings.timeframe}, 기간=${settings.testPeriod}")

            val interval = getIntervalString(settings.timeframe)
            val limit = getDataLimit(settings.testPeriod, settings.timeframe)

            Log.d(TAG, "변환된 정보: interval=$interval, limit=$limit")

            val allCandles = mutableListOf<PriceCandle>()
            var currentLimit = limit
            var endTime: Long? = null
            var requestCount = 0

            while (allCandles.size < limit && currentLimit > 0) {
                val requestLimit = minOf(currentLimit, 1000)
                requestCount++

                Log.d(TAG, "📞 API 요청 #$requestCount: symbol=${settings.symbol}, interval=$interval, limit=$requestLimit")

                val response = try {
                    api.getKlines(
                        symbol = settings.symbol,
                        interval = interval,
                        limit = requestLimit,
                        endTime = endTime
                    )
                } catch (networkError: Throwable) {
                    Log.e(TAG, "❌ 네트워크 오류 발생: ${networkError.message}")
                    throw RuntimeException("바이낸스 API 네트워크 오류\n" +
                            "요청 #$requestCount 실패\n" +
                            "URL: ${BINANCE_BASE_URL}api/v3/klines\n" +
                            "파라미터: symbol=${settings.symbol}, interval=$interval, limit=$requestLimit\n" +
                            "오류: ${networkError.message}")
                }

                if (response.isSuccessful && response.body() != null) {
                    val klines = response.body()!!
                    Log.d(TAG, "✅ API 응답 성공: ${klines.size}개 데이터 수신")

                    val newCandles = klines.mapNotNull { kline ->
                        try {
                            if (kline.size >= 6) {
                                PriceCandle(
                                    timestamp = (kline[0] as Double).toLong(),
                                    open = (kline[1] as String).toDouble(),
                                    high = (kline[2] as String).toDouble(),
                                    low = (kline[3] as String).toDouble(),
                                    close = (kline[4] as String).toDouble(),
                                    volume = (kline[5] as String).toDouble()
                                )
                            } else null
                        } catch (parseError: Exception) {
                            Log.w(TAG, "⚠️ 캔들 데이터 파싱 오류: ${parseError.message}")
                            null
                        }
                    }

                    if (newCandles.isNotEmpty()) {
                        allCandles.addAll(0, newCandles) // 앞쪽에 추가 (오래된 데이터)
                        endTime = newCandles.first().timestamp - 1
                        currentLimit -= newCandles.size
                        Log.d(TAG, "📈 누적 데이터: ${allCandles.size}개 (목표: $limit 개)")
                    } else {
                        Log.w(TAG, "⚠️ 파싱된 데이터가 없음")
                        break
                    }
                } else {
                    val errorCode = response.code()
                    val errorBody = response.errorBody()?.string() ?: "알 수 없는 오류"

                    Log.e(TAG, "❌ API 호출 실패: HTTP $errorCode")
                    Log.e(TAG, "오류 내용: $errorBody")

                    val errorMessage = when (errorCode) {
                        400 -> "잘못된 요청 (400)\n심볼명이나 시간프레임을 확인해주세요"
                        403 -> "접근 금지 (403)\nIP 제한이 있을 수 있습니다"
                        429 -> "요청 한도 초과 (429)\n잠시 후 다시 시도해주세요"
                        500 -> "서버 오류 (500)\n바이낸스 서버에 문제가 있습니다"
                        else -> "HTTP $errorCode 오류"
                    }

                    throw RuntimeException("바이낸스 API 호출 실패\n" +
                            "요청 #$requestCount 실패\n" +
                            "URL: ${BINANCE_BASE_URL}api/v3/klines\n" +
                            "파라미터: symbol=${settings.symbol}, interval=$interval, limit=$requestLimit\n" +
                            "$errorMessage\n" +
                            "상세: $errorBody")
                }

                // API 호출 간격 (Rate Limit 방지)
                kotlinx.coroutines.delay(100)
            }

            if (allCandles.isEmpty()) {
                throw RuntimeException("데이터 수집 실패\n" +
                        "총 ${requestCount}번의 API 요청을 시도했지만 유효한 데이터를 받지 못했습니다\n" +
                        "심볼: ${settings.symbol}\n" +
                        "시간프레임: ${settings.timeframe}\n" +
                        "기간: ${settings.testPeriod}")
            }

            val finalData = allCandles.takeLast(limit)
            Log.d(TAG, "✅ 데이터 수집 완료: ${finalData.size}개 (요청 횟수: $requestCount)")

            // 데이터 품질 체크 및 타임스탬프 검증
            val startDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(finalData.first().timestamp))
            val endDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(finalData.last().timestamp))
            Log.d(TAG, "📅 데이터 기간: $startDate ~ $endDate")

            // 🔍 샘플 데이터 검증 (처음 3개, 마지막 3개)
            Log.d(TAG, "🔍 데이터 샘플 검증:")
            for (i in 0 until minOf(3, finalData.size)) {
                val sample = finalData[i]
                val sampleTime = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(sample.timestamp))
                Log.d(TAG, "  시작 #$i: $sampleTime, 종가=${sample.close}")
            }

            for (i in maxOf(0, finalData.size - 3) until finalData.size) {
                val sample = finalData[i]
                val sampleTime = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(sample.timestamp))
                Log.d(TAG, "  끝 #$i: $sampleTime, 종가=${sample.close}")
            }

            finalData

        } catch (e: Throwable) {
            Log.e(TAG, "❌ fetchRealPriceData 실패: ${e.message}")
            throw e // 오류를 그대로 전파
        }
    }

    // CCI 계산 함수 (기존 코드 유지)
    private fun calculateCCI(priceData: List<PriceCandle>, period: Int = 14): List<Double> {
        if (priceData.size < period) {
            Log.w(TAG, "⚠️ CCI 계산을 위한 데이터가 부족합니다: ${priceData.size} < $period")
            return emptyList()
        }

        val cciValues = mutableListOf<Double>()

        for (i in period - 1 until priceData.size) {
            val typicalPrices = mutableListOf<Double>()

            // Typical Price 계산 (H+L+C)/3
            for (j in i - period + 1..i) {
                val tp = (priceData[j].high + priceData[j].low + priceData[j].close) / 3.0
                typicalPrices.add(tp)
            }

            // Simple Moving Average
            val sma = typicalPrices.average()

            // Mean Deviation 계산
            val meanDeviation = typicalPrices.map { abs(it - sma) }.average()

            // CCI 계산
            val currentTP = (priceData[i].high + priceData[i].low + priceData[i].close) / 3.0
            val cci = if (meanDeviation != 0.0) {
                (currentTP - sma) / (0.015 * meanDeviation)
            } else {
                0.0
            }

            cciValues.add(cci)
        }

        Log.d(TAG, "✅ CCI 계산 완료: ${cciValues.size}개")
        return cciValues
    }

    // 정확한 물타기 전략 실행 (기존 로직 수정)
    private fun executeAccurateStrategy(
        priceData: List<PriceCandle>,
        cciData: List<Double>,
        settings: CciStrategySettings
    ): List<TradeExecution> {
        val trades = mutableListOf<TradeExecution>()
        var currentSeedMoney = settings.seedMoney
        var currentPosition: AccuratePosition? = null
        var previousCCI = 0.0

        Log.d(TAG, "📊 정확한 CCI 물타기 전략 실행 시작")
        Log.d(TAG, "가격 데이터: ${priceData.size}개, CCI 데이터: ${cciData.size}개")

        // 인덱스 매핑 수정 (CCI 데이터는 14개 늦게 시작)
        for (i in 1 until cciData.size) {
            val currentCCI = cciData[i]
            val priceIndex = i + 13 // CCI 인덱스를 가격 데이터 인덱스로 변환

            if (priceIndex >= priceData.size) break

            val currentPrice = priceData[priceIndex].close
            val currentTimestamp = priceData[priceIndex].timestamp

            // 진입 시그널 체크
            if (currentPosition == null) {
                val signal = checkEntrySignal(previousCCI, currentCCI, settings)

                if (signal != null) {
                    currentPosition = AccuratePosition(
                        type = signal,
                        entryPrice = currentPrice,
                        amount = settings.startAmount,
                        timestamp = currentTimestamp,
                        entryCCI = currentCCI,
                        previousCCI = previousCCI
                    )
                    Log.d(TAG, "🎯 진입: $signal @ $currentPrice, CCI: $currentCCI")
                }
            } else {
                // 청산 조건 체크
                val exitSignal = checkExitSignal(currentPosition, currentCCI, currentPrice, settings)

                if (exitSignal != null) {
                    val trade = createTrade(currentPosition, currentPrice, currentTimestamp, exitSignal, settings)
                    trades.add(trade)
                    currentSeedMoney += trade.netProfit
                    Log.d(TAG, "🏁 청산: ${currentPosition.type} @ $currentPrice, 수익: ${trade.netProfit}")
                    currentPosition = null
                }
            }

            previousCCI = currentCCI
        }

        return trades
    }

    // 진입 신호 체크 (기존 코드 유지)
    private fun checkEntrySignal(previousCCI: Double, currentCCI: Double, settings: CciStrategySettings): String? {
        // 롱 진입 조건: 이전 CCI < -110 && 현재 CCI >= -100
        val longCondition = previousCCI < -settings.entryThreshold && currentCCI >= -settings.exitThreshold

        // 숏 진입 조건: 이전 CCI > +110 && 현재 CCI <= +100
        val shortCondition = previousCCI > settings.entryThreshold && currentCCI <= settings.exitThreshold

        return when {
            longCondition -> "LONG"
            shortCondition -> "SHORT"
            else -> null
        }
    }

    // 청산 신호 체크
    private fun checkExitSignal(
        position: AccuratePosition,
        currentCCI: Double,
        currentPrice: Double,
        settings: CciStrategySettings
    ): String? {
        return when (position.type) {
            "LONG" -> {
                when {
                    currentCCI > settings.exitThreshold -> "PROFIT"
                    currentPrice < position.entryPrice * (1 - settings.profitTarget / 100) -> "STOP_LOSS"
                    else -> null
                }
            }
            "SHORT" -> {
                when {
                    currentCCI < -settings.exitThreshold -> "PROFIT"
                    currentPrice > position.entryPrice * (1 + settings.profitTarget / 100) -> "STOP_LOSS"
                    else -> null
                }
            }
            else -> null
        }
    }

    // 거래 생성
    private fun createTrade(
        position: AccuratePosition,
        exitPrice: Double,
        timestamp: Long,
        exitReason: String,
        settings: CciStrategySettings
    ): TradeExecution {
        val grossProfit = if (position.type == "LONG") {
            (exitPrice - position.entryPrice) / position.entryPrice * position.amount
        } else {
            (position.entryPrice - exitPrice) / position.entryPrice * position.amount
        }

        val fees = position.amount * settings.feeRate / 100 * 2 // 진입 + 청산

        return TradeExecution(
            type = position.type,
            entryPrice = position.entryPrice,
            exitPrice = exitPrice,
            amount = position.amount,
            grossProfit = grossProfit,
            fees = fees,
            netProfit = grossProfit - fees,
            exitType = exitReason,
            stages = 1,
            timestamp = timestamp,
            entryCCI = position.entryCCI,
            previousCCI = position.previousCCI
        )
    }

    // 결과 계산 (기존 코드 수정)
    private fun calculateResults(trades: List<TradeExecution>, settings: CciStrategySettings): CciBacktestResult {
        val winningTrades = trades.count { it.netProfit > 0 }
        val losingTrades = trades.count { it.netProfit <= 0 }
        val totalProfit = trades.sumOf { it.netProfit }
        val totalFees = trades.sumOf { it.fees }
        val winRate = if (trades.isNotEmpty()) (winningTrades.toDouble() / trades.size) * 100 else 0.0

        val profits = trades.filter { it.netProfit > 0 }.sumOf { it.netProfit }
        val losses = abs(trades.filter { it.netProfit <= 0 }.sumOf { it.netProfit })
        val profitFactor = if (losses > 0) profits / losses else if (profits > 0) Double.POSITIVE_INFINITY else 0.0

        // 최대 손실 계산
        var maxDrawdown = 0.0
        var peak = settings.seedMoney
        var currentBalance = settings.seedMoney

        for (trade in trades) {
            currentBalance += trade.netProfit
            if (currentBalance > peak) {
                peak = currentBalance
            }
            val currentDrawdown = (peak - currentBalance) / peak * 100
            if (currentDrawdown > maxDrawdown) {
                maxDrawdown = currentDrawdown
            }
        }

        // TradeResult 생성 시 정확한 시간 포맷 사용
        val tradeResults = trades.map { trade ->
            val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            val timeString = dateFormat.format(Date(trade.timestamp))

            TradeResult(
                type = trade.type,
                entryPrice = trade.entryPrice,
                exitPrice = trade.exitPrice,
                amount = trade.amount,
                profit = trade.netProfit,
                fee = trade.fees,
                timestamp = timeString,
                entryCCI = trade.entryCCI,
                previousCCI = trade.previousCCI,
                exitReason = trade.exitType
            )
        }

        Log.d(TAG, "📊 백테스트 결과 - 총거래: ${trades.size}, 승률: ${String.format("%.1f", winRate)}%, 총수익: ${String.format("%.2f", totalProfit)}")

        return CciBacktestResult(
            totalTrades = trades.size,
            winningTrades = winningTrades,
            losingTrades = losingTrades,
            totalProfit = totalProfit,
            totalFees = totalFees,
            maxDrawdown = maxDrawdown,
            finalSeedMoney = settings.seedMoney + totalProfit,
            winRate = winRate,
            profitFactor = profitFactor,
            trades = tradeResults
        )
    }

    // 빈 결과 생성
    private fun createEmptyResult(settings: CciStrategySettings): CciBacktestResult {
        return CciBacktestResult(
            totalTrades = 0,
            winningTrades = 0,
            losingTrades = 0,
            totalProfit = 0.0,
            totalFees = 0.0,
            maxDrawdown = 0.0,
            finalSeedMoney = settings.seedMoney,
            winRate = 0.0,
            profitFactor = 0.0,
            trades = emptyList()
        )
    }
}

// 정확한 포지션 클래스
data class AccuratePosition(
    val type: String,
    val entryPrice: Double,
    val amount: Double,
    val timestamp: Long,
    val entryCCI: Double,
    val previousCCI: Double
)