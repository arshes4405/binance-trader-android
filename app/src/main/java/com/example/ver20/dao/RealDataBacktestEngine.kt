// RealDataBacktestEngine.kt - 실제 바이낸스 데이터를 이용한 백테스팅 엔진

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

// 실제 데이터 백테스팅 엔진
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

    // 백테스팅 실행 (실제 데이터 사용)
    suspend fun runRealDataBacktest(settings: CciStrategySettings): CciBacktestResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "🚀 실제 데이터 CCI 백테스팅 시작 - ${settings.symbol}, ${settings.timeframe}")

        // 1. 실제 바이낸스 데이터 가져오기
        val priceData = fetchRealPriceData(settings)
        
        if (priceData.isEmpty()) {
            Log.e(TAG, "❌ 가격 데이터를 가져올 수 없습니다")
            return@withContext CciBacktestResult(
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

        // 2. CCI 계산
        val cciData = calculateCCI(priceData, settings.entryThreshold)

        // 3. 백테스팅 실행
        val trades = executeTradingStrategy(priceData, cciData, settings)

        // 4. 결과 계산
        return@withContext calculateResults(trades, settings)
    }

    // 실제 바이낸스 데이터 가져오기
    private suspend fun fetchRealPriceData(settings: CciStrategySettings): List<PriceCandle> {
        return try {
            Log.d(TAG, "📡 바이낸스에서 실제 데이터 가져오는 중: ${settings.symbol}")
            
            val interval = getIntervalString(settings.timeframe)
            val limit = getDataLimit(settings.testPeriod, settings.timeframe)
            
            // 여러 번 요청하여 충분한 데이터 확보
            val allCandles = mutableListOf<PriceCandle>()
            var currentLimit = limit
            var endTime: Long? = null
            
            while (allCandles.size < limit && currentLimit > 0) {
                val requestLimit = minOf(currentLimit, 1000) // 바이낸스 API 최대 1000개 제한
                
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
                        // 바이낸스 Kline 형식: [openTime, open, high, low, close, volume, closeTime, ...]
                        PriceCandle(
                            timestamp = (kline[0] as Number).toLong(),
                            open = (kline[1] as String).toDouble(),
                            high = (kline[2] as String).toDouble(),
                            low = (kline[3] as String).toDouble(),
                            close = (kline[4] as String).toDouble(),
                            volume = (kline[5] as String).toDouble()
                        )
                    }.reversed() // 최신 데이터부터 오므로 역순 정렬
                    
                    allCandles.addAll(0, candles) // 앞쪽에 추가
                    
                    // 다음 요청을 위한 endTime 설정 (가장 오래된 데이터의 시간)
                    endTime = candles.first().timestamp - 1
                    currentLimit -= candles.size
                    
                    Log.d(TAG, "📊 데이터 수집 중: ${allCandles.size}/${limit}")
                } else {
                    Log.e(TAG, "❌ API 요청 실패: ${response.code()}")
                    break
                }
                
                // API 호출 제한을 위한 잠시 대기
                kotlinx.coroutines.delay(100)
            }
            
            Log.d(TAG, "✅ 실제 데이터 로드 완료: ${allCandles.size}개")
            Log.d(TAG, "📈 데이터 범위: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(java.util.Date(allCandles.first().timestamp))} ~ ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(java.util.Date(allCandles.last().timestamp))}")
            
            allCandles.takeLast(limit) // 최신 데이터만 사용
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 실제 데이터 로드 실패: ${e.message}")
            emptyList()
        }
    }

    // CCI 계산 (수정된 버전)
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

    // 실제 데이터 기반 거래 전략 실행
    private fun executeTradingStrategy(
        priceData: List<PriceCandle>,
        cciData: List<Double>,
        settings: CciStrategySettings
    ): List<TradeExecution> {
        val trades = mutableListOf<TradeExecution>()
        var currentSeedMoney = settings.seedMoney
        var currentPosition: Position? = null
        var previousCCI = 0.0

        Log.d(TAG, "📊 거래 전략 실행 시작 - CCI 데이터: ${cciData.size}개")

        for (i in 1 until cciData.size) {
            val currentCCI = cciData[i]
            val currentPrice = priceData[i + 13].close // CCI는 14기간 뒤부터 시작
            val currentTimestamp = priceData[i + 13].timestamp

            // 진입 신호 체크
            if (currentPosition == null) {
                // 롱 진입 조건: CCI가 -entryThreshold를 뚫고 -exitThreshold로 회복
                if (previousCCI < -settings.entryThreshold && currentCCI >= -settings.exitThreshold) {
                    val startAmount = currentSeedMoney * 0.2
                    currentPosition = Position(
                        type = "LONG",
                        stages = mutableListOf(
                            PositionStage(
                                entryPrice = currentPrice,
                                amount = startAmount,
                                timestamp = currentTimestamp
                            )
                        ),
                        totalAmount = startAmount,
                        averagePrice = currentPrice
                    )
                    Log.d(TAG, "📈 롱 진입: $currentPrice (CCI: ${currentCCI.toInt()})")
                }
                // 숏 진입 조건: CCI가 +entryThreshold를 뚫고 +exitThreshold로 회복
                else if (previousCCI > settings.entryThreshold && currentCCI <= settings.exitThreshold) {
                    val startAmount = currentSeedMoney * 0.2
                    currentPosition = Position(
                        type = "SHORT",
                        stages = mutableListOf(
                            PositionStage(
                                entryPrice = currentPrice,
                                amount = startAmount,
                                timestamp = currentTimestamp
                            )
                        ),
                        totalAmount = startAmount,
                        averagePrice = currentPrice
                    )
                    Log.d(TAG, "📉 숏 진입: $currentPrice (CCI: ${currentCCI.toInt()})")
                }
            }

            // 포지션 관리 (기존 로직과 동일)
            currentPosition?.let { position ->
                val profitRate = if (position.type == "LONG") {
                    (currentPrice - position.averagePrice) / position.averagePrice * 100
                } else {
                    (position.averagePrice - currentPrice) / position.averagePrice * 100
                }

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

                // 익절 조건
                if (profitRate >= settings.profitTarget) {
                    val trade = createTradeExecution(position, currentPrice, "PROFIT", settings.feeRate, currentTimestamp)
                    trades.add(trade)
                    currentSeedMoney += trade.netProfit
                    currentPosition = null
                    Log.d(TAG, "💰 익절: ${String.format("%.2f", trade.netProfit)} (수익률: ${String.format("%.2f", profitRate)}%)")
                }
                // 물타기 로직
                else if (lossRateFromFirst >= 2.0 && position.stages.size == 1) {
                    addAveragingStage(position, currentPrice, currentSeedMoney * 0.2, currentTimestamp)
                    Log.d(TAG, "📊 1단계 물타기: ${String.format("%.2f", lossRateFromFirst)}% 손실")
                }
                else if (lossRateFromAverage >= 4.0 && position.stages.size == 2) {
                    addAveragingStage(position, currentPrice, currentSeedMoney * 0.4, currentTimestamp)
                    Log.d(TAG, "📊 2단계 물타기: ${String.format("%.2f", lossRateFromAverage)}% 손실")
                }
                else if (lossRateFromAverage >= 8.0 && position.stages.size == 3) {
                    addAveragingStage(position, currentPrice, currentSeedMoney * 0.8, currentTimestamp)
                    Log.d(TAG, "📊 3단계 물타기: ${String.format("%.2f", lossRateFromAverage)}% 손실")
                }
                else if (lossRateFromAverage >= 16.0 && position.stages.size == 4) {
                    addAveragingStage(position, currentPrice, currentSeedMoney * 1.6, currentTimestamp)
                    Log.d(TAG, "📊 4단계 물타기: ${String.format("%.2f", lossRateFromAverage)}% 손실")
                }

                // 본절 달성시 절반 매도
                if (position.stages.size > 1 && profitRate >= -1.0) {
                    val halfSellTrade = createHalfSellTrade(position, currentPrice, settings.feeRate, currentTimestamp)
                    trades.add(halfSellTrade)
                    currentSeedMoney += halfSellTrade.netProfit
                    position.totalAmount *= 0.5
                    Log.d(TAG, "📊 절반 매도 (본절): ${String.format("%.2f", halfSellTrade.netProfit)}")
                }

                // 완전 청산
                if (position.stages.size > 1 && profitRate >= 4.0) {
                    val trade = createTradeExecution(position, currentPrice, "FULL_EXIT", settings.feeRate, currentTimestamp)
                    trades.add(trade)
                    currentSeedMoney += trade.netProfit
                    currentPosition = null
                    Log.d(TAG, "🎯 완전 청산: ${String.format("%.2f", trade.netProfit)}")
                }
            }

            previousCCI = currentCCI
        }

        // 미청산 포지션 강제 청산
        currentPosition?.let { position ->
            val finalPrice = priceData.last().close
            val finalTimestamp = priceData.last().timestamp
            val trade = createTradeExecution(position, finalPrice, "FORCE_CLOSE", settings.feeRate, finalTimestamp)
            trades.add(trade)
            currentSeedMoney += trade.netProfit
            Log.d(TAG, "🔒 강제 청산: ${String.format("%.2f", trade.netProfit)}")
        }

        Log.d(TAG, "✅ 거래 실행 완료: ${trades.size}개 거래, 최종 시드머니: ${String.format("%.2f", currentSeedMoney)}")
        return trades
    }

    // 물타기 단계 추가 (타임스탬프 포함)
    private fun addAveragingStage(position: Position, currentPrice: Double, additionalAmount: Double, timestamp: Long) {
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

    // 거래 실행 생성 (타임스탬프 포함)
    private fun createTradeExecution(
        position: Position,
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
            stages = position.stages.size,
            timestamp = timestamp
        )
    }

    // 절반 매도 거래 생성 (타임스탬프 포함)
    private fun createHalfSellTrade(
        position: Position,
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
            stages = position.stages.size,
            timestamp = timestamp
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

        // 최대 손실 계산 (개선된 버전)
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

        val tradeResults = trades.map { trade ->
            val dateFormat = java.text.SimpleDateFormat("MM-dd HH:mm")
            TradeResult(
                type = trade.type,
                entryPrice = trade.entryPrice,
                exitPrice = trade.exitPrice,
                amount = trade.amount,
                profit = trade.netProfit,
                fee = trade.fees,
                timestamp = dateFormat.format(java.util.Date(trade.timestamp))
            )
        }

        Log.d(TAG, "📊 백테스팅 결과 - 총거래: ${trades.size}, 승률: ${String.format("%.1f", winRate)}%, 총수익: ${String.format("%.2f", totalProfit)}")

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