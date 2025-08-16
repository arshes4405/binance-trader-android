// BacktestEngine.kt - 실제 백테스팅 로직 구현 (수정된 버전)

package com.example.ver20.dao

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.random.Random

// CCI 백테스팅 엔진
class CciBacktestEngine {
    companion object {
        private const val TAG = "CciBacktestEngine"
    }

    // 백테스팅 실행 메인 함수
    suspend fun runBacktest(settings: CciStrategySettings): CciBacktestResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "🚀 CCI 백테스팅 시작 - ${settings.symbol}, ${settings.timeframe}")

        // 1. 모의 가격 데이터 생성
        val priceData = generateMockPriceData(settings)

        // 2. CCI 계산
        val cciData = calculateCCI(priceData)

        // 3. 백테스팅 실행
        val trades = executeTradingStrategy(priceData, cciData, settings)

        // 4. 결과 계산
        return@withContext calculateResults(trades, settings)
    }

    // 모의 가격 데이터 생성
    private fun generateMockPriceData(settings: CciStrategySettings): List<PriceCandle> {
        val dataPoints = when (settings.testPeriod) {
            "3개월" -> if (settings.timeframe == "1시간") 2160 else 540
            "6개월" -> if (settings.timeframe == "1시간") 4320 else 1080
            "1년" -> if (settings.timeframe == "1시간") 8760 else 2190
            "2년" -> if (settings.timeframe == "1시간") 17520 else 4380
            else -> 2190
        }

        val basePrice = when (settings.symbol) {
            "BTCUSDT" -> 45000.0
            "ETHUSDT" -> 2800.0
            "BNBUSDT" -> 350.0
            "ADAUSDT" -> 1.2
            else -> 45000.0
        }

        val candles = mutableListOf<PriceCandle>()
        var currentPrice = basePrice
        val random = Random(System.currentTimeMillis())

        repeat(dataPoints) { index ->
            // 가격 변동 (±5% 범위)
            val change = (random.nextDouble() - 0.5) * 0.1 * currentPrice
            currentPrice += change

            // 캔들 생성 (간단한 모델)
            val high = currentPrice * (1 + random.nextDouble() * 0.02)
            val low = currentPrice * (1 - random.nextDouble() * 0.02)
            val open = if (index == 0) currentPrice else candles[index - 1].close

            candles.add(
                PriceCandle(
                    timestamp = System.currentTimeMillis() + index * 3600000L,
                    open = open,
                    high = high,
                    low = low,
                    close = currentPrice,
                    volume = random.nextDouble() * 1000
                )
            )
        }

        Log.d(TAG, "✅ 가격 데이터 생성 완료: ${candles.size}개")
        return candles
    }

    // CCI 계산
    private fun calculateCCI(priceData: List<PriceCandle>, period: Int = 14): List<Double> {
        if (priceData.size < period) return emptyList()

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

    // 거래 전략 실행
    private fun executeTradingStrategy(
        priceData: List<PriceCandle>,
        cciData: List<Double>,
        settings: CciStrategySettings
    ): List<TradeExecution> {
        val trades = mutableListOf<TradeExecution>()
        var currentSeedMoney = settings.seedMoney
        var currentPosition: Position? = null
        var previousCCI = 0.0

        for (i in 1 until cciData.size) {
            val currentCCI = cciData[i]
            val currentPrice = priceData[i + 13].close // CCI는 14기간 뒤부터 시작

            // 진입 신호 체크
            if (currentPosition == null) {
                // 롱 진입 조건: CCI가 -110을 뚫고 -100으로 회복
                if (previousCCI < -settings.entryThreshold && currentCCI >= -settings.exitThreshold) {
                    val startAmount = currentSeedMoney * 0.2
                    currentPosition = Position(
                        type = "LONG",
                        stages = mutableListOf(
                            PositionStage(
                                entryPrice = currentPrice,
                                amount = startAmount,
                                timestamp = priceData[i + 13].timestamp
                            )
                        ),
                        totalAmount = startAmount,
                        averagePrice = currentPrice
                    )
                    Log.d(TAG, "📈 롱 진입: $currentPrice, 금액: $startAmount")
                }
                // 숏 진입 조건: CCI가 +110을 뚫고 +100으로 회복
                else if (previousCCI > settings.entryThreshold && currentCCI <= settings.exitThreshold) {
                    val startAmount = currentSeedMoney * 0.2
                    currentPosition = Position(
                        type = "SHORT",
                        stages = mutableListOf(
                            PositionStage(
                                entryPrice = currentPrice,
                                amount = startAmount,
                                timestamp = priceData[i + 13].timestamp
                            )
                        ),
                        totalAmount = startAmount,
                        averagePrice = currentPrice
                    )
                    Log.d(TAG, "📉 숏 진입: $currentPrice, 금액: $startAmount")
                }
            }

            // 포지션 관리
            currentPosition?.let { position ->
                // 1단계는 첫 진입가 대비, 2단계부터는 평단가 대비로 계산
                val profitRate = if (position.type == "LONG") {
                    (currentPrice - position.averagePrice) / position.averagePrice * 100
                } else {
                    (position.averagePrice - currentPrice) / position.averagePrice * 100
                }

                // 물타기 손실률 계산 (1단계는 첫 진입가 대비, 2단계부터는 평단가 대비)
                val firstEntryPrice = position.stages[0].entryPrice
                val lossRateFromFirst = if (position.type == "LONG") {
                    (firstEntryPrice - currentPrice) / firstEntryPrice * 100
                } else {
                    (currentPrice - firstEntryPrice) / firstEntryPrice * 100
                }

                val lossRateFromAverage = if (position.type == "LONG") {
                    (position.averagePrice - currentPrice) / position.averagePrice * 100
                } else {
                    (currentPrice - position.averagePrice) / position.averagePrice * 100
                }

                // 익절 조건 (3% 수익)
                if (profitRate >= settings.profitTarget) {
                    val trade = createTradeExecution(position, currentPrice, "PROFIT", settings.feeRate)
                    trades.add(trade)
                    currentSeedMoney += trade.netProfit
                    currentPosition = null
                    Log.d(TAG, "💰 익절: ${trade.netProfit}")
                }
                // 물타기 로직
                else if (lossRateFromFirst >= 2.0 && position.stages.size == 1) {
                    // 1단계: 첫 진입가 대비 2% 손실시 추가매수
                    addAveragingStage(position, currentPrice, currentSeedMoney * 0.2)
                    Log.d(TAG, "📊 1단계 물타기: 첫 진입가 대비 ${lossRateFromFirst}% 손실")
                }
                else if (lossRateFromAverage >= 4.0 && position.stages.size == 2) {
                    // 2단계: 평단가 대비 4% 손실시 추가매수
                    addAveragingStage(position, currentPrice, currentSeedMoney * 0.4)
                    Log.d(TAG, "📊 2단계 물타기: 평단가 대비 ${lossRateFromAverage}% 손실")
                }
                else if (lossRateFromAverage >= 8.0 && position.stages.size == 3) {
                    // 3단계: 평단가 대비 8% 손실시 추가매수
                    addAveragingStage(position, currentPrice, currentSeedMoney * 0.8)
                    Log.d(TAG, "📊 3단계 물타기: 평단가 대비 ${lossRateFromAverage}% 손실")
                }
                else if (lossRateFromAverage >= 16.0 && position.stages.size == 4) {
                    // 4단계: 평단가 대비 16% 손실시 추가매수
                    addAveragingStage(position, currentPrice, currentSeedMoney * 1.6)
                    Log.d(TAG, "📊 4단계 물타기: 평단가 대비 ${lossRateFromAverage}% 손실")
                }

                // 본절 달성시 절반 매도 로직
                if (position.stages.size > 1 && profitRate >= -1.0) {
                    val halfSellTrade = createHalfSellTrade(position, currentPrice, settings.feeRate)
                    trades.add(halfSellTrade)
                    currentSeedMoney += halfSellTrade.netProfit
                    position.totalAmount *= 0.5
                }

                // 평단가 +4% 완전 청산
                if (position.stages.size > 1 && profitRate >= 4.0) {
                    val trade = createTradeExecution(position, currentPrice, "FULL_EXIT", settings.feeRate)
                    trades.add(trade)
                    currentSeedMoney += trade.netProfit
                    currentPosition = null
                    Log.d(TAG, "🎯 완전 청산: ${trade.netProfit}")
                }
            }

            previousCCI = currentCCI
        }

        // 미청산 포지션 강제 청산
        currentPosition?.let { position ->
            val finalPrice = priceData.last().close
            val trade = createTradeExecution(position, finalPrice, "FORCE_CLOSE", settings.feeRate)
            trades.add(trade)
            currentSeedMoney += trade.netProfit
        }

        Log.d(TAG, "✅ 거래 실행 완료: ${trades.size}개 거래")
        return trades
    }

    // 물타기 단계 추가
    private fun addAveragingStage(position: Position, currentPrice: Double, additionalAmount: Double) {
        position.stages.add(
            PositionStage(
                entryPrice = currentPrice,
                amount = additionalAmount,
                timestamp = System.currentTimeMillis()
            )
        )

        // 평균 단가 재계산
        val totalCost = position.stages.sumOf { it.entryPrice * it.amount }
        position.totalAmount = position.stages.sumOf { it.amount }
        position.averagePrice = totalCost / position.totalAmount

        Log.d(TAG, "📊 물타기 ${position.stages.size}단계: 평단가 ${position.averagePrice}")
    }

    // 거래 실행 생성
    private fun createTradeExecution(
        position: Position,
        exitPrice: Double,
        exitType: String,
        feeRate: Double
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
            stages = position.stages.size,
            timestamp = System.currentTimeMillis()
        )
    }

    // 절반 매도 거래 생성
    private fun createHalfSellTrade(
        position: Position,
        currentPrice: Double,
        feeRate: Double
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
            stages = position.stages.size,
            timestamp = System.currentTimeMillis()
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
        val profitFactor = if (losses > 0) profits / losses else 0.0

        // 최대 손실 계산 (간단한 버전)
        val maxDrawdown = trades.minOfOrNull { it.netProfit / settings.seedMoney * 100 } ?: 0.0

        val tradeResults = trades.map { trade ->
            TradeResult(
                type = trade.type,
                entryPrice = trade.entryPrice,
                exitPrice = trade.exitPrice,
                amount = trade.amount,
                profit = trade.netProfit,
                fee = trade.fees,
                timestamp = "시뮬레이션"
            )
        }

        Log.d(TAG, "📊 백테스팅 결과 - 총거래: ${trades.size}, 승률: $winRate%, 총수익: $totalProfit")

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
}