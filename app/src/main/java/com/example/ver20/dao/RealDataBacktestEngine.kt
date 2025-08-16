// 개량된 RealDataBacktestEngine.kt - 더 체계적이고 효율적인 CCI 물타기 전략

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

// 개량된 포지션 관리 클래스
data class EnhancedPosition(
    val type: String,
    val stages: MutableList<PositionStage>,
    var totalAmount: Double,
    var averagePrice: Double,
    val entryPreviousCCI: Double,
    val entryCurrentCCI: Double,
    val startAmount: Double
) {
    // 현재 단계 반환
    fun getCurrentStage(): Int = stages.size

    // 첫 진입가 반환
    fun getFirstEntryPrice(): Double = stages.first().entryPrice

    // 평단가 기준 수익률 계산
    fun getProfitRate(currentPrice: Double): Double {
        return if (type == "LONG") {
            (currentPrice - averagePrice) / averagePrice * 100
        } else {
            (averagePrice - currentPrice) / averagePrice * 100
        }
    }

    // 첫 진입가 대비 손실률
    fun getLossFromFirst(currentPrice: Double): Double {
        val firstPrice = getFirstEntryPrice()
        return if (type == "LONG") {
            (firstPrice - currentPrice) / firstPrice * 100
        } else {
            (currentPrice - firstPrice) / firstPrice * 100
        }
    }

    // 평단가 대비 손실률
    fun getLossFromAverage(currentPrice: Double): Double {
        return if (type == "LONG") {
            (averagePrice - currentPrice) / averagePrice * 100
        } else {
            (currentPrice - averagePrice) / averagePrice * 100
        }
    }

    // 다음 물타기 필요 금액 계산
    fun getNextAveragingAmount(): Double {
        return when (getCurrentStage()) {
            1 -> startAmount * 1.0  // 2단계: 시작금 * 1
            2 -> startAmount * 2.0  // 3단계: 시작금 * 2
            3 -> startAmount * 4.0  // 4단계: 시작금 * 4
            4 -> startAmount * 8.0  // 5단계: 시작금 * 8
            else -> 0.0
        }
    }

    // 물타기 트리거 손실률 반환
    fun getAveragingTrigger(): Double {
        return when (getCurrentStage()) {
            1 -> 2.0   // 첫 진입가 대비 2% 손실
            2 -> 4.0   // 평단가 대비 4% 손실
            3 -> 8.0   // 평단가 대비 8% 손실
            4 -> 16.0  // 평단가 대비 16% 손실
            else -> 100.0 // 더 이상 물타기 없음
        }
    }
}

// 액션 타입 정의
data class PositionAction(
    val type: String, // HOLD, PROFIT_EXIT, AVERAGING, HALF_SELL, FULL_EXIT, STOP_LOSS
    val amount: Double = 0.0,
    val reason: String = ""
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
        Log.d(TAG, "🚀 개량된 CCI 물타기 전략 시작 - ${settings.symbol}, ${settings.timeframe}")

        // 1. 실제 바이낸스 데이터 가져오기
        val priceData = fetchRealPriceData(settings)

        if (priceData.isEmpty()) {
            Log.e(TAG, "❌ 가격 데이터를 가져올 수 없습니다")
            return@withContext createEmptyResult(settings)
        }

        // 2. 최적화된 CCI 계산
        val cciData = calculateOptimizedCCI(priceData, 14)

        // 3. 개량된 전략 실행
        val trades = executeEnhancedStrategy(priceData, cciData, settings)

        // 4. 결과 계산
        return@withContext calculateResults(trades, settings)
    }

    // 실제 바이낸스 데이터 가져오기 (기존과 동일)
    suspend fun fetchRealPriceData(settings: CciStrategySettings): List<PriceCandle> {
        return try {
            Log.d(TAG, "📡 바이낸스에서 실제 데이터 가져오는 중: ${settings.symbol}")

            val interval = getIntervalString(settings.timeframe)
            val limit = getDataLimit(settings.testPeriod, settings.timeframe)

            val allCandles = mutableListOf<PriceCandle>()
            var currentLimit = limit
            var endTime: Long? = null

            while (allCandles.size < limit && currentLimit > 0) {
                val requestLimit = minOf(currentLimit, 1000)

                val response = api.getKlines(
                    symbol = settings.symbol,
                    interval = interval,
                    limit = requestLimit,
                    endTime = endTime
                )

                if (response.isSuccessful && response.body() != null) {
                    val klines = response.body()!!

                    if (klines.isEmpty()) break

                    val candles = klines.map { kline ->
                        PriceCandle(
                            timestamp = (kline[0] as Number).toLong(),
                            open = (kline[1] as String).toDouble(),
                            high = (kline[2] as String).toDouble(),
                            low = (kline[3] as String).toDouble(),
                            close = (kline[4] as String).toDouble(),
                            volume = (kline[5] as String).toDouble()
                        )
                    }.reversed()

                    allCandles.addAll(0, candles)
                    endTime = candles.first().timestamp - 1
                    currentLimit -= candles.size

                    Log.d(TAG, "📊 데이터 수집 중: ${allCandles.size}/${limit}")
                } else {
                    Log.e(TAG, "❌ API 요청 실패: ${response.code()}")
                    break
                }

                kotlinx.coroutines.delay(100)
            }

            Log.d(TAG, "✅ 실제 데이터 로드 완료: ${allCandles.size}개")
            allCandles.takeLast(limit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ 실제 데이터 로드 실패: ${e.message}")
            emptyList()
        }
    }

    // 최적화된 CCI 계산
    private fun calculateOptimizedCCI(priceData: List<PriceCandle>, period: Int = 14): List<Double> {
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

        Log.d(TAG, "✅ 최적화된 CCI 계산 완료: ${cciValues.size}개")
        return cciValues
    }

    // 개량된 진입 신호 체크
    private fun checkEntrySignal(previousCCI: Double, currentCCI: Double, settings: CciStrategySettings): String? {
        // 롱 진입 조건: CCI가 -entryThreshold를 뚫고 -exitThreshold로 회복
        val longCondition = previousCCI < -settings.entryThreshold && currentCCI >= -settings.exitThreshold

        // 숏 진입 조건: CCI가 +entryThreshold를 뚫고 +exitThreshold로 회복
        val shortCondition = previousCCI > settings.entryThreshold && currentCCI <= settings.exitThreshold

        return when {
            longCondition -> "LONG"
            shortCondition -> "SHORT"
            else -> null
        }
    }

    // 개량된 포지션 액션 결정
    private fun determinePositionAction(position: EnhancedPosition, currentPrice: Double, settings: CciStrategySettings): PositionAction {
        val profitRate = position.getProfitRate(currentPrice)
        val lossFromFirst = position.getLossFromFirst(currentPrice)
        val lossFromAverage = position.getLossFromAverage(currentPrice)
        val currentStage = position.getCurrentStage()

        // 1. 익절 조건 체크 (최우선)
        if (profitRate >= settings.profitTarget) {
            return PositionAction("PROFIT_EXIT", 0.0, "익절 달성: ${String.format("%.2f", profitRate)}%")
        }

        // 2. 물타기 조건 체크 (단계별)
        when (currentStage) {
            1 -> {
                if (lossFromFirst >= 2.0) {
                    return PositionAction("AVERAGING", position.getNextAveragingAmount(), "1단계 물타기: ${String.format("%.2f", lossFromFirst)}% 손실")
                }
            }
            2, 3, 4 -> {
                if (lossFromAverage >= position.getAveragingTrigger()) {
                    return PositionAction("AVERAGING", position.getNextAveragingAmount(), "${currentStage}단계 물타기: ${String.format("%.2f", lossFromAverage)}% 손실")
                }
            }
        }

        // 3. 본절 달성시 절반 매도 (물타기 포지션만)
        if (currentStage > 1 && profitRate >= -1.0) {
            return PositionAction("HALF_SELL", position.totalAmount * 0.5, "본절 달성 절반매도")
        }

        // 4. 완전 청산 조건 (물타기 후 +4% 수익)
        if (currentStage > 1 && profitRate >= 4.0) {
            return PositionAction("FULL_EXIT", 0.0, "물타기 완전청산: ${String.format("%.2f", profitRate)}%")
        }

        // 5. 홀드
        return PositionAction("HOLD", 0.0, "포지션 유지")
    }

    // 개량된 전략 실행
    private fun executeEnhancedStrategy(
        priceData: List<PriceCandle>,
        cciData: List<Double>,
        settings: CciStrategySettings
    ): List<TradeExecution> {
        val trades = mutableListOf<TradeExecution>()
        var currentSeedMoney = settings.seedMoney
        var currentPosition: EnhancedPosition? = null
        var previousCCI = 0.0

        Log.d(TAG, "📊 개량된 CCI 전략 실행 시작 - CCI 데이터: ${cciData.size}개")

        for (i in 1 until cciData.size) {
            val currentCCI = cciData[i]
            val currentPrice = priceData[i + 13].close
            val currentTimestamp = priceData[i + 13].timestamp

            // 진입 신호 체크
            if (currentPosition == null) {
                val entrySignal = checkEntrySignal(previousCCI, currentCCI, settings)

                if (entrySignal != null) {
                    val startAmount = currentSeedMoney * 0.2

                    currentPosition = EnhancedPosition(
                        type = entrySignal,
                        stages = mutableListOf(
                            PositionStage(currentPrice, startAmount, currentTimestamp)
                        ),
                        totalAmount = startAmount,
                        averagePrice = currentPrice,
                        entryPreviousCCI = previousCCI,
                        entryCurrentCCI = currentCCI,
                        startAmount = startAmount
                    )

                    val dateFormat = java.text.SimpleDateFormat("MM-dd HH:mm")
                    val timeString = dateFormat.format(java.util.Date(currentTimestamp))

                    Log.d(TAG, "🎯 ${entrySignal} 진입: $timeString, 가격: $currentPrice, 금액: ${String.format("%.2f", startAmount)}")
                    Log.d(TAG, "   💡 CCI: ${String.format("%.1f", previousCCI)} → ${String.format("%.1f", currentCCI)}")
                }
            }

            // 포지션 관리
            currentPosition?.let { position ->
                val action = determinePositionAction(position, currentPrice, settings)

                when (action.type) {
                    "PROFIT_EXIT" -> {
                        val trade = createTradeExecutionWithCCI(position, currentPrice, "PROFIT", settings.feeRate, currentTimestamp)
                        trades.add(trade)
                        currentSeedMoney += trade.netProfit
                        currentPosition = null
                        Log.d(TAG, "💰 익절: ${String.format("%.2f", trade.netProfit)} - ${action.reason}")
                    }

                    "AVERAGING" -> {
                        addAveragingStage(position, currentPrice, action.amount, currentTimestamp)
                        Log.d(TAG, "📊 ${action.reason}")
                    }

                    "HALF_SELL" -> {
                        val halfTrade = createHalfSellTradeWithCCI(position, currentPrice, settings.feeRate, currentTimestamp)
                        trades.add(halfTrade)
                        currentSeedMoney += halfTrade.netProfit
                        position.totalAmount *= 0.5
                        Log.d(TAG, "📊 절반 매도: ${String.format("%.2f", halfTrade.netProfit)} - ${action.reason}")
                    }

                    "FULL_EXIT" -> {
                        val trade = createTradeExecutionWithCCI(position, currentPrice, "FULL_EXIT", settings.feeRate, currentTimestamp)
                        trades.add(trade)
                        currentSeedMoney += trade.netProfit
                        currentPosition = null
                        Log.d(TAG, "🎯 완전 청산: ${String.format("%.2f", trade.netProfit)} - ${action.reason}")
                    }
                }
            }

            previousCCI = currentCCI
        }

        // 미청산 포지션 강제 청산
        currentPosition?.let { position ->
            val finalPrice = priceData.last().close
            val finalTimestamp = priceData.last().timestamp
            val trade = createTradeExecutionWithCCI(position, finalPrice, "FORCE_CLOSE", settings.feeRate, finalTimestamp)
            trades.add(trade)
            currentSeedMoney += trade.netProfit
            Log.d(TAG, "🔒 강제 청산: ${String.format("%.2f", trade.netProfit)}")
        }

        Log.d(TAG, "✅ 거래 실행 완료: ${trades.size}개 거래, 최종 시드머니: ${String.format("%.2f", currentSeedMoney)}")
        return trades
    }

    // CCI 값이 포함된 거래 실행 생성 (기존과 동일하지만 개선된 로깅)
    private fun createTradeExecutionWithCCI(
        position: EnhancedPosition,
        exitPrice: Double,
        exitType: String,
        feeRate: Double,
        timestamp: Long
    ): TradeExecution {
        val entryFee = position.totalAmount * feeRate / 100
        val exitFee = position.totalAmount * feeRate / 100
        val totalFee = entryFee + exitFee

        val grossProfit = if (position.type == "LONG") {
            (exitPrice - position.averagePrice) * (position.totalAmount / position.averagePrice)
        } else {
            (position.averagePrice - exitPrice) * (position.totalAmount / position.averagePrice)
        }

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
            stages = position.getCurrentStage(),
            timestamp = timestamp,
            entryCCI = position.entryCurrentCCI,
            previousCCI = position.entryPreviousCCI,
            exitCCI = 0.0 // 현재 CCI 값으로 설정 가능
        )
    }

    // CCI 값이 포함된 절반 매도 거래 생성 (기존과 동일)
    private fun createHalfSellTradeWithCCI(
        position: EnhancedPosition,
        currentPrice: Double,
        feeRate: Double,
        timestamp: Long
    ): TradeExecution {
        val halfAmount = position.totalAmount * 0.5
        val fee = halfAmount * feeRate / 100

        val grossProfit = if (position.type == "LONG") {
            (currentPrice - position.averagePrice) * (halfAmount / position.averagePrice)
        } else {
            (position.averagePrice - currentPrice) * (halfAmount / position.averagePrice)
        }

        return TradeExecution(
            type = position.type,
            entryPrice = position.averagePrice,
            exitPrice = currentPrice,
            amount = halfAmount,
            grossProfit = grossProfit,
            fees = fee,
            netProfit = grossProfit - fee,
            exitType = "HALF_SELL",
            stages = position.getCurrentStage(),
            timestamp = timestamp,
            entryCCI = position.entryCurrentCCI,
            previousCCI = position.entryPreviousCCI,
            exitCCI = 0.0
        )
    }

    // 물타기 단계 추가 (기존과 동일하지만 개선된 계산)
    private fun addAveragingStage(position: EnhancedPosition, currentPrice: Double, additionalAmount: Double, timestamp: Long) {
        position.stages.add(
            PositionStage(
                entryPrice = currentPrice,
                amount = additionalAmount,
                timestamp = timestamp
            )
        )

        // 평균 단가 재계산
        val totalCost = position.stages.sumOf { it.entryPrice * it.amount }
        position.totalAmount = position.stages.sumOf { it.amount }
        position.averagePrice = totalCost / position.totalAmount
    }

    // 최종 결과 계산 (기존과 동일)
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

        // TradeResult 생성
        val tradeResults = trades.map { trade ->
            val dateFormat = java.text.SimpleDateFormat("MM-dd HH:mm")
            TradeResult(
                type = trade.type,
                entryPrice = trade.entryPrice,
                exitPrice = trade.exitPrice,
                amount = trade.amount,
                profit = trade.netProfit,
                fee = trade.fees,
                timestamp = dateFormat.format(java.util.Date(trade.timestamp)),
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

    // 빈 결과 생성 (기존과 동일)
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