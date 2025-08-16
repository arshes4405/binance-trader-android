// RealDataBacktestEngine.kt - 정확한 CCI 물타기 전략 + 모든 거래내역 추적

package com.example.ver20.dao

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import kotlin.math.abs

// 바이낸스 Kline API 인터페이스
interface BinanceKlineApi {
    @GET("api/v3/klines")
    suspend fun getKlines(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("limit") limit: Int = 1000,
        @Query("startTime") startTime: Long? = null,
        @Query("endTime") endTime: Long? = null
    ): Response<List<List<Any>>>
}

// 정확한 포지션 관리 클래스
data class AccuratePosition(
    val type: String, // "LONG" or "SHORT"
    var stage: Int = 0, // 0=첫진입, 1,2,3,4=물타기단계
    val entries: MutableList<PositionEntry>, // 각 매수 기록
    var totalAmount: Double = 0.0, // 총 투자금액
    var totalCoins: Double = 0.0, // 총 코인 수량
    var averagePrice: Double = 0.0, // 평균 단가
    val startAmount: Double, // 시작금 (시드머니 20%)
    val entryPreviousCCI: Double,
    val entryCurrentCCI: Double,
    var hasHalfSold: Boolean = false // 절반매도 했는지 여부
) {
    // 매수 기록 추가 및 평균 단가 재계산
    fun addEntry(price: Double, amount: Double, timestamp: Long) {
        val coins = amount / price
        entries.add(PositionEntry(price, amount, coins, timestamp))
        totalAmount += amount
        totalCoins += coins
        recalculateAverage()
    }

    // 평균 단가 재계산
    private fun recalculateAverage() {
        if (totalCoins > 0) {
            averagePrice = totalAmount / totalCoins
        }
    }

    // 현재 수익률 계산 (평단가 기준)
    fun getProfitRate(currentPrice: Double): Double {
        return if (type == "LONG") {
            (currentPrice - averagePrice) / averagePrice * 100
        } else {
            (averagePrice - currentPrice) / averagePrice * 100
        }
    }

    // 첫 진입가 대비 손실률 (0단계에서 1단계 진입용)
    fun getLossFromFirstEntry(currentPrice: Double): Double {
        if (entries.isEmpty()) return 0.0
        val firstPrice = entries[0].price
        return if (type == "LONG") {
            (firstPrice - currentPrice) / firstPrice * 100
        } else {
            (currentPrice - firstPrice) / firstPrice * 100
        }
    }

    // 평단가 대비 손실률 (1단계 이상에서 다음 단계 진입용)
    fun getLossFromAverage(currentPrice: Double): Double {
        return if (type == "LONG") {
            (averagePrice - currentPrice) / averagePrice * 100
        } else {
            (currentPrice - averagePrice) / averagePrice * 100
        }
    }

    // 다음 단계 진입 조건 확인
    fun shouldEnterNextStage(currentPrice: Double): Boolean {
        return when (stage) {
            0 -> getLossFromFirstEntry(currentPrice) >= 2.0 // 0→1단계: 첫 진입가 대비 2% 손실
            1 -> getLossFromAverage(currentPrice) >= 4.0 // 1→2단계: 평단가 대비 4% 손실
            2 -> getLossFromAverage(currentPrice) >= 8.0 // 2→3단계: 평단가 대비 8% 손실
            3 -> getLossFromAverage(currentPrice) >= 16.0 // 3→4단계: 평단가 대비 16% 손실
            else -> false // 4단계 이후는 더 이상 물타기 없음
        }
    }

    // 다음 단계 매수 금액 계산
    fun getNextStageAmount(): Double {
        return when (stage) {
            0 -> startAmount // 0→1단계: 시작금만큼
            1 -> startAmount * 2 // 1→2단계: 시작금 × 2
            2 -> startAmount * 4 // 2→3단계: 시작금 × 4
            3 -> startAmount * 8 // 3→4단계: 시작금 × 8
            else -> 0.0
        }
    }

    // 본절 도달 여부 확인 (손익 0% 지점)
    fun isBreakEven(currentPrice: Double): Boolean {
        val profitRate = getProfitRate(currentPrice)
        return profitRate >= -1.0 && profitRate <= 1.0 // ±1% 범위를 본절로 간주
    }

    // 절반매도 실행
    fun executeHalfSell(currentPrice: Double): Double {
        val halfCoins = totalCoins * 0.5
        val sellAmount = halfCoins * currentPrice

        totalCoins *= 0.5
        totalAmount *= 0.5 // 투자금도 절반으로 줄임
        hasHalfSold = true

        return sellAmount
    }
}

// 매수 기록 클래스
data class PositionEntry(
    val price: Double,
    val amount: Double,
    val coins: Double,
    val timestamp: Long
)

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

        // 기간별 데이터 개수 계산
        private fun getDataLimit(period: String, timeframe: String): Int {
            return when (period) {
                "3개월" -> when (timeframe) {
                    "1시간" -> 2160
                    "4시간" -> 540
                    "1일" -> 90
                    else -> 540
                }
                "6개월" -> when (timeframe) {
                    "1시간" -> 4320
                    "4시간" -> 1080
                    "1일" -> 180
                    else -> 1080
                }
                "1년" -> when (timeframe) {
                    "1시간" -> 8760
                    "4시간" -> 2190
                    "1일" -> 365
                    else -> 2190
                }
                "2년" -> when (timeframe) {
                    "1시간" -> 17520
                    "4시간" -> 4380
                    "1일" -> 730
                    else -> 4380
                }
                else -> 1000
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
        Log.d(TAG, "🚀 정확한 CCI 물타기 전략 시작 - ${settings.symbol}, ${settings.timeframe}")

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

                    if (klines.isEmpty()) {
                        Log.w(TAG, "⚠️ 빈 응답 수신 - 더 이상 데이터가 없습니다")
                        break
                    }

                    val candles = try {
                        klines.map { kline ->
                            PriceCandle(
                                timestamp = (kline[0] as Number).toLong(),
                                open = (kline[1] as String).toDouble(),
                                high = (kline[2] as String).toDouble(),
                                low = (kline[3] as String).toDouble(),
                                close = (kline[4] as String).toDouble(),
                                volume = (kline[5] as String).toDouble()
                            )
                        }.reversed()
                    } catch (parseError: Throwable) {
                        Log.e(TAG, "❌ 데이터 파싱 오류: ${parseError.message}")
                        throw RuntimeException("바이낸스 API 응답 데이터 파싱 실패\n" +
                                "요청 #$requestCount 에서 오류 발생\n" +
                                "원시 데이터 형식이 예상과 다릅니다\n" +
                                "오류: ${parseError.message}")
                    }

                    allCandles.addAll(0, candles)
                    endTime = candles.first().timestamp - 1
                    currentLimit -= candles.size

                    Log.d(TAG, "📊 데이터 누적: ${allCandles.size}/${limit} (${String.format("%.1f", allCandles.size.toDouble()/limit*100)}%)")
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
            val startDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(java.util.Date(finalData.first().timestamp))
            val endDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(java.util.Date(finalData.last().timestamp))
            Log.d(TAG, "📅 데이터 기간: $startDate ~ $endDate")

            // 🔍 샘플 데이터 검증 (처음 3개, 마지막 3개)
            Log.d(TAG, "🔍 데이터 샘플 검증:")
            for (i in 0 until minOf(3, finalData.size)) {
                val sample = finalData[i]
                val sampleTime = java.text.SimpleDateFormat("MM-dd HH:mm").format(java.util.Date(sample.timestamp))
                Log.d(TAG, "  시작 #$i: $sampleTime, 종가=${sample.close}")
            }

            for (i in maxOf(0, finalData.size - 3) until finalData.size) {
                val sample = finalData[i]
                val sampleTime = java.text.SimpleDateFormat("MM-dd HH:mm").format(java.util.Date(sample.timestamp))
                Log.d(TAG, "  끝 #$i: $sampleTime, 종가=${sample.close}")
            }

            finalData

        } catch (e: Throwable) {
            Log.e(TAG, "❌ fetchRealPriceData 실패: ${e.message}")
            throw e // 오류를 그대로 전파
        }
    }

    // CCI 계산
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

    // CCI 진입 신호 체크
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

    // 정확한 물타기 전략 실행
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

        // 🔍 인덱스 매핑 검증
        if (priceData.size < cciData.size + 13) {
            Log.e(TAG, "❌ 데이터 크기 불일치: 가격=${priceData.size}, CCI=${cciData.size}")
            throw RuntimeException("가격 데이터가 부족합니다. CCI 계산을 위해 ${cciData.size + 13}개가 필요하지만 ${priceData.size}개만 있습니다.")
        }

        for (i in 1 until cciData.size) {
            val currentCCI = cciData[i]
            val currentPrice = priceData[i + 13].close // 🔍 여기가 문제!
            val currentTimestamp = priceData[i + 13].timestamp

            // 🐛 디버깅: 가격과 시간 검증
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm")
            val timeString = dateFormat.format(java.util.Date(currentTimestamp))

            if (i < 5) { // 처음 5개 데이터만 로깅
                Log.d(TAG, "🔍 데이터 검증 #$i: 시간=$timeString, 가격=$currentPrice, CCI=${String.format("%.1f", currentCCI)}")
            }

            // 진입 신호 체크 (포지션이 없을 때만)
            if (currentPosition == null) {
                val entrySignal = checkEntrySignal(previousCCI, currentCCI, settings)

                if (entrySignal != null) {
                    val startAmount = currentSeedMoney * 0.2

                    // 🔍 진입 시점 상세 로깅
                    val priceIndex = i + 13

                    Log.d(TAG, "🎯 ${entrySignal} 진입 감지!")
                    Log.d(TAG, "  시간: $timeString (인덱스: CCI=$i, 가격=$priceIndex)")
                    Log.d(TAG, "  가격: $currentPrice")
                    Log.d(TAG, "  CCI: ${String.format("%.1f", previousCCI)} → ${String.format("%.1f", currentCCI)}")
                    Log.d(TAG, "  투자금: ${String.format("%.2f", startAmount)}")

                    currentPosition = AccuratePosition(
                        type = entrySignal,
                        stage = 0, // 0단계부터 시작
                        entries = mutableListOf(),
                        startAmount = startAmount,
                        entryPreviousCCI = previousCCI,
                        entryCurrentCCI = currentCCI
                    )

                    // 첫 진입 (0단계)
                    currentPosition.addEntry(currentPrice, startAmount, currentTimestamp)

                    // 🔄 첫 진입 매수 거래 기록
                    val firstBuyTrade = createBuyTrade(currentPosition, currentPrice, startAmount, settings.feeRate, currentTimestamp, 0)
                    trades.add(firstBuyTrade)
                }
            }

            // 포지션 관리
            currentPosition?.let { position ->
                val profitRate = position.getProfitRate(currentPrice)

                // 1. 첫 진입(0단계) 3% 익절 체크
                if (position.stage == 0 && profitRate >= settings.profitTarget) {
                    val trade = createCompleteExit(position, currentPrice, "STAGE0_PROFIT", settings.feeRate, currentTimestamp)
                    trades.add(trade)
                    currentSeedMoney += trade.netProfit
                    currentPosition = null
                    Log.d(TAG, "💰 0단계 3% 익절: ${String.format("%.2f", trade.netProfit)}")
                }
                // 2. 각 단계별 평단가 +4% 완전 익절 체크
                else if (position.stage > 0 && profitRate >= 4.0) {
                    val trade = createCompleteExit(position, currentPrice, "COMPLETE_PROFIT", settings.feeRate, currentTimestamp)
                    trades.add(trade)
                    currentSeedMoney += trade.netProfit
                    currentPosition = null
                    Log.d(TAG, "🎯 ${position.stage}단계 +4% 완전 익절: ${String.format("%.2f", trade.netProfit)}")
                }
                // 3. 본절 도달시 절반매도 (1단계 이상이고 아직 절반매도 안한 경우)
                else if (position.stage > 0 && !position.hasHalfSold && position.isBreakEven(currentPrice)) {
                    val halfSellAmount = position.executeHalfSell(currentPrice)
                    val halfTrade = createHalfSellTrade(position, currentPrice, halfSellAmount, settings.feeRate, currentTimestamp)
                    trades.add(halfTrade)
                    currentSeedMoney += halfTrade.netProfit
                    Log.d(TAG, "📊 ${position.stage}단계 본절 절반매도: ${String.format("%.2f", halfTrade.netProfit)}")
                }
                // 4. 다음 단계 진입 조건 체크
                else if (position.shouldEnterNextStage(currentPrice) && position.stage < 4) {
                    val nextAmount = position.getNextStageAmount()

                    // 🔄 물타기 매수 거래 기록
                    val buyTrade = createBuyTrade(position, currentPrice, nextAmount, settings.feeRate, currentTimestamp, position.stage + 1)
                    trades.add(buyTrade)

                    position.addEntry(currentPrice, nextAmount, currentTimestamp)
                    position.stage++
                    position.hasHalfSold = false // 새 단계에서는 절반매도 리셋

                    Log.d(TAG, "📈 ${position.stage}단계 물타기 매수: 가격 $currentPrice, 추가금액 ${String.format("%.2f", nextAmount)}")
                    Log.d(TAG, "   📊 새 평단가: ${String.format("%.2f", position.averagePrice)}")
                }
            }

            previousCCI = currentCCI
        }

        // 미청산 포지션 강제 청산
        currentPosition?.let { position ->
            val finalPrice = priceData.last().close
            val finalTimestamp = priceData.last().timestamp
            val trade = createCompleteExit(position, finalPrice, "FORCE_CLOSE", settings.feeRate, finalTimestamp)
            trades.add(trade)
            currentSeedMoney += trade.netProfit
            Log.d(TAG, "🔒 강제 청산: ${String.format("%.2f", trade.netProfit)}")
        }

        Log.d(TAG, "✅ 거래 실행 완료: ${trades.size}개 거래, 최종 시드머니: ${String.format("%.2f", currentSeedMoney)}")
        return trades
    }

    // 매수 거래 생성
    private fun createBuyTrade(
        position: AccuratePosition,
        buyPrice: Double,
        buyAmount: Double,
        feeRate: Double,
        timestamp: Long,
        stage: Int
    ): TradeExecution {
        val fee = buyAmount * feeRate / 100

        return TradeExecution(
            type = "${position.type}_BUY", // "LONG_BUY" or "SHORT_BUY"
            entryPrice = buyPrice,
            exitPrice = 0.0, // 매수는 exitPrice 없음
            amount = buyAmount,
            grossProfit = 0.0, // 매수는 수익 없음
            fees = fee,
            netProfit = -fee, // 매수는 수수료만 손실
            exitType = "STAGE${stage}_BUY",
            stages = stage,
            timestamp = timestamp,
            entryCCI = position.entryCurrentCCI,
            previousCCI = position.entryPreviousCCI,
            exitCCI = 0.0
        )
    }

    // 완전 청산 거래 생성
    private fun createCompleteExit(
        position: AccuratePosition,
        exitPrice: Double,
        exitType: String,
        feeRate: Double,
        timestamp: Long
    ): TradeExecution {
        val entryFee = position.totalAmount * feeRate / 100
        val exitAmount = position.totalCoins * exitPrice
        val exitFee = exitAmount * feeRate / 100
        val totalFee = entryFee + exitFee

        val grossProfit = exitAmount - position.totalAmount
        val netProfit = grossProfit - totalFee

        return TradeExecution(
            type = position.type,
            entryPrice = position.averagePrice,
            exitPrice = exitPrice,
            amount = position.totalAmount,
            grossProfit = grossProfit,
            fees = totalFee,
            netProfit = netProfit,
            exitType = exitType,
            stages = position.stage + 1, // 0단계도 1로 표시
            timestamp = timestamp,
            entryCCI = position.entryCurrentCCI,
            previousCCI = position.entryPreviousCCI,
            exitCCI = 0.0
        )
    }

    // 절반매도 거래 생성
    private fun createHalfSellTrade(
        position: AccuratePosition,
        currentPrice: Double,
        sellAmount: Double,
        feeRate: Double,
        timestamp: Long
    ): TradeExecution {
        val fee = sellAmount * feeRate / 100
        val grossProfit = sellAmount - (position.totalAmount * 0.5)
        val netProfit = grossProfit - fee

        return TradeExecution(
            type = position.type,
            entryPrice = position.averagePrice,
            exitPrice = currentPrice,
            amount = sellAmount,
            grossProfit = grossProfit,
            fees = fee,
            netProfit = netProfit,
            exitType = "HALF_SELL",
            stages = position.stage,
            timestamp = timestamp,
            entryCCI = position.entryCurrentCCI,
            previousCCI = position.entryPreviousCCI,
            exitCCI = 0.0
        )
    }

    // 최종 결과 계산
    private fun calculateResults(trades: List<TradeExecution>, settings: CciStrategySettings): CciBacktestResult {
        val winningTrades = trades.count { it.netProfit > 0 }
        val losingTrades = trades.count { it.netProfit <= 0 }
        val totalProfit = trades.sumOf { it.netProfit }
        val totalFees = trades.sumOf { it.fees }
        val winRate = if (trades.isNotEmpty()) (winningTrades.toDouble() / trades.size) * 100 else 0.0

        val profits = trades.filter { it.netProfit > 0 }.sumOf { it.netProfit }
        val losses = abs(trades.filter { it.netProfit <= 0 }.sumOf { it.netProfit })
        val profitFactor = if (losses > 0) profits / losses else if (profits > 0) 999.0 else 0.0

        // 최대 손실 계산
        var runningBalance = settings.seedMoney
        var maxBalance = runningBalance
        var maxDrawdown = 0.0

        trades.forEach { trade ->
            runningBalance += trade.netProfit
            if (runningBalance > maxBalance) {
                maxBalance = runningBalance
            }
            val currentDrawdown = (maxBalance - runningBalance) / maxBalance * 100
            if (currentDrawdown > maxDrawdown) {
                maxDrawdown = currentDrawdown
            }
        }

        // TradeResult 생성 시 정확한 시간 포맷 사용
        val tradeResults = trades.map { trade ->
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm") // 년도 포함
            val timeString = dateFormat.format(java.util.Date(trade.timestamp))

            TradeResult(
                type = trade.type,
                entryPrice = trade.entryPrice,
                exitPrice = trade.exitPrice,
                amount = trade.amount,
                profit = trade.netProfit,
                fee = trade.fees,
                timestamp = timeString, // 정확한 시간 표시
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