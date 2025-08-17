// RealCciStrategyEngine.kt - 롱/숏 완전 분리 버전

package com.example.ver20.dao

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*


class RealCciStrategyEngine {
    companion object {
        private const val TAG = "RealCciStrategyEngine"
    }

    private val dataEngine = RealDataBacktestEngine()

    // 메인 백테스트 실행 함수
    suspend fun runRealCciBacktest(settings: CciStrategySettings): RealCciBacktestResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "🚀 실제 CCI 물타기 전략 시작")
        Log.d(TAG, "설정: ${settings.symbol}, ${settings.timeframe}, CCI길이=${settings.cciLength}")

        try {
            // 1. 실제 바이낸스 데이터 가져오기
            val priceData = dataEngine.fetchRealPriceData(settings)

            if (priceData.isEmpty()) {
                throw RuntimeException("가격 데이터를 가져올 수 없습니다")
            }

            // 2. CCI 계산 (설정된 길이로)
            val cciData = calculateCCI(priceData, settings.cciLength)

            if (cciData.isEmpty()) {
                throw RuntimeException("CCI 계산에 실패했습니다")
            }

            // 3. 실제 물타기 전략 실행
            val result = executeRealCciStrategy(priceData, cciData, settings)

            return@withContext result

        } catch (e: Throwable) {
            Log.e(TAG, "❌ 백테스팅 실행 중 오류: ${e.message}")
            throw e
        }
    }

    // CCI 계산 함수 (설정 가능한 길이)
    private fun calculateCCI(priceData: List<PriceCandle>, period: Int): List<Double> {
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

        Log.d(TAG, "✅ CCI 계산 완료: ${cciValues.size}개 (길이: $period)")
        return cciValues
    }

    // 실제 CCI 물타기 전략 실행
    private fun executeRealCciStrategy(
        priceData: List<PriceCandle>,
        cciData: List<Double>,
        settings: CciStrategySettings
    ): RealCciBacktestResult {

        val trades = mutableListOf<CciTradeExecution>()
        val positions = mutableListOf<CciPositionResult>()
        var currentSeedMoney = settings.seedMoney
        var currentPosition: CciPosition? = null
        var previousCCI = 0.0
        var positionId = 0

        Log.d(TAG, "📊 실제 CCI 물타기 전략 실행")
        Log.d(TAG, "가격 데이터: ${priceData.size}개, CCI 데이터: ${cciData.size}개")

        // CCI 데이터는 설정된 길이만큼 늦게 시작
        for (i in 1 until cciData.size) {
            val currentCCI = cciData[i]
            val priceIndex = i + settings.cciLength - 1

            if (priceIndex >= priceData.size) break

            val currentPrice = priceData[priceIndex].close
            val currentTimestamp = priceData[priceIndex].timestamp

            // 진입 신호 체크
            if (currentPosition == null) {
                val signal = checkRealEntrySignal(previousCCI, currentCCI, settings)

                if (signal != null) {
                    positionId++

                    if (signal == "LONG") {
                        // 롱 포지션 생성 및 실행
                        val (longPosition, longTrades) = createAndExecuteLongPosition(
                            positionId, currentPrice, currentTimestamp, currentCCI, previousCCI, settings
                        )
                        currentPosition = longPosition
                        trades.addAll(longTrades)
                    } else {
                        // 숏 포지션 생성 및 실행
                        val (shortPosition, shortTrades) = createAndExecuteShortPosition(
                            positionId, currentPrice, currentTimestamp, currentCCI, previousCCI, settings
                        )
                        currentPosition = shortPosition
                        trades.addAll(shortTrades)
                    }

                    Log.d(TAG, "🎯 진입: $signal @ $currentPrice, CCI: $currentCCI")
                }
            } else {
                // 포지션 관리
                currentPosition?.let { position ->
                    val (actions, isPositionClosed) = if (position.type == "LONG") {
                        manageLongPositionComplete(position, currentPrice, currentCCI, settings, currentTimestamp)
                    } else {
                        manageShortPositionComplete(position, currentPrice, currentCCI, settings, currentTimestamp)
                    }

                    trades.addAll(actions)

                    // 포지션 종료 체크
                    if (isPositionClosed) {
                        val positionResult = if (position.type == "LONG") {
                            createLongPositionResult(position, trades, positionId)
                        } else {
                            createShortPositionResult(position, trades, positionId)
                        }
                        positions.add(positionResult)

                        // 시드머니 업데이트
                        val positionProfit = positionResult.totalProfit
                        currentSeedMoney += positionProfit

                        Log.d(TAG, "🏁 포지션 종료: ${position.type}, 수익: $positionProfit")
                        currentPosition = null
                    }
                }
            }

            previousCCI = currentCCI
        }

        // 최종 결과 계산
        return calculateFinalResult(trades, positions, settings, currentSeedMoney)
    }

    // 실제 진입 신호 체크
    private fun checkRealEntrySignal(previousCCI: Double, currentCCI: Double, settings: CciStrategySettings): String? {
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

    // ========================================
    // 롱 포지션 전용 메소드들
    // ========================================

    // 롱 포지션 생성 및 첫 진입
    private fun createAndExecuteLongPosition(
        positionId: Int,
        entryPrice: Double,
        timestamp: Long,
        entryCCI: Double,
        previousCCI: Double,
        settings: CciStrategySettings
    ): Pair<CciPosition, List<CciTradeExecution>> {

        val position = CciPosition(
            type = "LONG",
            stages = mutableListOf(),
            currentStage = 0,
            timestamp = timestamp,
            entryCCI = entryCCI,
            previousCCI = previousCCI
        )

        val entryTrade = executeLongEntry(position, entryPrice, 0, settings.startAmount, timestamp, entryCCI, settings)

        Log.d(TAG, "📈 롱 포지션 생성 #$positionId: 가격=$entryPrice, 금액=${settings.startAmount}")

        return Pair(position, listOf(entryTrade))
    }

    // 롱 진입 실행
    private fun executeLongEntry(
        position: CciPosition,
        price: Double,
        stage: Int,
        amount: Double,
        timestamp: Long,
        cci: Double,
        settings: CciStrategySettings
    ): CciTradeExecution {

        val coins = amount / price
        val fee = amount * settings.feeRate / 100

        // 포지션에 단계 추가
        val stageInfo = CciPositionStage(
            stage = stage,
            entryPrice = price,
            amount = amount,
            coins = coins,
            timestamp = timestamp
        )
        position.stages.add(stageInfo)
        position.currentStage = stage

        // 평균단가 재계산
        position.totalAmount += amount
        position.totalCoins += coins
        position.averagePrice = position.totalAmount / position.totalCoins

        Log.d(TAG, "롱 매수 - 단계: $stage, 가격: $price, 금액: $amount, 평균단가: ${position.averagePrice}")

        return CciTradeExecution(
            type = "LONG_BUY_STAGE$stage",
            stage = stage,
            entryPrice = price,
            amount = amount,
            coins = coins,
            fees = fee,
            timestamp = timestamp,
            entryCCI = cci,
            reason = getLongEntryReason(stage)
        )
    }

    // 롱 포지션 관리 (완전 분리)
    private fun manageLongPositionComplete(
        position: CciPosition,
        currentPrice: Double,
        currentCCI: Double,
        settings: CciStrategySettings,
        timestamp: Long
    ): Pair<List<CciTradeExecution>, Boolean> {

        val actions = mutableListOf<CciTradeExecution>()
        val averagePrice = position.averagePrice
        val profitRate = (currentPrice - averagePrice) / averagePrice * 100
        val lossRate = (averagePrice - currentPrice) / averagePrice * 100

        // 1. 익절 조건 체크
        val targetProfit = if (position.currentStage == 0) settings.profitTarget else settings.halfSellProfit

        if (profitRate >= targetProfit) {
            if (position.currentStage == 0) {
                // 1단계에서 익절 시 전액 매도
                val exitAction = createLongExitAction(position, currentPrice, timestamp, currentCCI, "LONG_PROFIT_EXIT", settings)
                actions.add(exitAction)
                Log.d(TAG, "롱 익절 완료: 수익률 $profitRate%")
                return Pair(actions, true)
            } else {
                // 다른 단계에서는 절반 매도
                val halfSellAction = createLongHalfSellAction(position, currentPrice, timestamp, currentCCI, settings)
                actions.add(halfSellAction)
                Log.d(TAG, "롱 절반매도: 수익률 $profitRate%")
            }
        }

        // 2. 물타기 조건 체크
        when (position.currentStage) {
            0 -> {
                if (lossRate >= settings.stage1Loss) {
                    val additionalAmount = position.totalAmount
                    val stageEntry = executeLongEntry(position, currentPrice, 1, additionalAmount, timestamp, currentCCI, settings)
                    actions.add(stageEntry)
                    Log.d(TAG, "롱 1단계 물타기: 손실률 $lossRate%")
                }
            }
            1 -> {
                if (lossRate >= settings.stage2Loss) {
                    val additionalAmount = position.totalAmount
                    val stageEntry = executeLongEntry(position, currentPrice, 2, additionalAmount, timestamp, currentCCI, settings)
                    actions.add(stageEntry)
                    Log.d(TAG, "롱 2단계 물타기: 손실률 $lossRate%")
                }
            }
            2 -> {
                if (lossRate >= settings.stage3Loss) {
                    val additionalAmount = position.totalAmount
                    val stageEntry = executeLongEntry(position, currentPrice, 3, additionalAmount, timestamp, currentCCI, settings)
                    actions.add(stageEntry)
                    Log.d(TAG, "롱 3단계 물타기: 손실률 $lossRate%")
                }
            }
            3 -> {
                if (lossRate >= settings.stage4Loss) {
                    val stopLossAction = createLongExitAction(position, currentPrice, timestamp, currentCCI, "LONG_STOP_LOSS", settings)
                    actions.add(stopLossAction)
                    Log.d(TAG, "롱 손절 완료: 손실률 $lossRate%")
                    return Pair(actions, true)
                }
            }
        }

        return Pair(actions, false)
    }

    // 롱 전액 청산
    private fun createLongExitAction(
        position: CciPosition,
        exitPrice: Double,
        timestamp: Long,
        exitCCI: Double,
        exitType: String,
        settings: CciStrategySettings
    ): CciTradeExecution {

        val totalCoins = position.totalCoins
        val exitAmount = totalCoins * exitPrice
        val fee = exitAmount * settings.feeRate / 100
        val profitRate = (exitPrice - position.averagePrice) / position.averagePrice * 100

        Log.d(TAG, "롱 청산 - 진입평균가: ${position.averagePrice}, 청산가: $exitPrice, 수익률: $profitRate%")

        return CciTradeExecution(
            type = exitType,
            stage = position.currentStage,
            entryPrice = position.averagePrice,
            exitPrice = exitPrice,
            amount = exitAmount,
            coins = totalCoins,
            fees = fee,
            timestamp = timestamp,
            exitCCI = exitCCI,
            profitRate = profitRate,
            reason = getLongExitReason(exitType)
        )
    }

    // 롱 절반 매도
    private fun createLongHalfSellAction(
        position: CciPosition,
        exitPrice: Double,
        timestamp: Long,
        exitCCI: Double,
        settings: CciStrategySettings
    ): CciTradeExecution {

        val halfCoins = position.totalCoins / 2
        val exitAmount = halfCoins * exitPrice
        val fee = exitAmount * settings.feeRate / 100
        val profitRate = (exitPrice - position.averagePrice) / position.averagePrice * 100

        // 포지션 정보 업데이트
        position.totalCoins = halfCoins
        position.totalAmount = halfCoins * position.averagePrice
        position.currentStage = maxOf(0, position.currentStage - 1)

        return CciTradeExecution(
            type = "LONG_HALF_SELL",
            stage = position.currentStage,
            entryPrice = position.averagePrice,
            exitPrice = exitPrice,
            amount = exitAmount,
            coins = halfCoins,
            fees = fee,
            timestamp = timestamp,
            exitCCI = exitCCI,
            profitRate = profitRate,
            reason = "0.5% 수익시 절반 매도"
        )
    }

    // ========================================
    // 숏 포지션 전용 메소드들
    // ========================================

    // 숏 포지션 생성 및 첫 진입
    private fun createAndExecuteShortPosition(
        positionId: Int,
        entryPrice: Double,
        timestamp: Long,
        entryCCI: Double,
        previousCCI: Double,
        settings: CciStrategySettings
    ): Pair<CciPosition, List<CciTradeExecution>> {

        val position = CciPosition(
            type = "SHORT",
            stages = mutableListOf(),
            currentStage = 0,
            timestamp = timestamp,
            entryCCI = entryCCI,
            previousCCI = previousCCI
        )

        val entryTrade = executeShortEntry(position, entryPrice, settings.startAmount, timestamp, entryCCI, settings)

        Log.d(TAG, "📉 숏 포지션 생성 #$positionId: 가격=$entryPrice, 금액=${settings.startAmount}")

        return Pair(position, listOf(entryTrade))
    }

    // 숏 진입 실행 (매도)
    private fun executeShortEntry(
        position: CciPosition,
        price: Double,
        amount: Double,
        timestamp: Long,
        cci: Double,
        settings: CciStrategySettings
    ): CciTradeExecution {

        val coins = amount / price
        val fee = amount * settings.feeRate / 100

        // 숏은 단일 진입만
        position.totalAmount = amount
        position.totalCoins = coins
        position.averagePrice = price

        Log.d(TAG, "숏 매도 진입 - 가격: $price, 금액: $amount, 코인: $coins")

        return CciTradeExecution(
            type = "SHORT_SELL_ENTRY",
            stage = 0,
            entryPrice = price,
            amount = amount,
            coins = coins,
            fees = fee,
            timestamp = timestamp,
            entryCCI = cci,
            reason = "CCI 과매수 회복 신호 (숏 매도 진입)"
        )
    }

    // 숏 포지션 관리 (완전 분리)
    private fun manageShortPositionComplete(
        position: CciPosition,
        currentPrice: Double,
        currentCCI: Double,
        settings: CciStrategySettings,
        timestamp: Long
    ): Pair<List<CciTradeExecution>, Boolean> {

        val actions = mutableListOf<CciTradeExecution>()
        val entryPrice = position.averagePrice

        // 숏 수익률: (진입가 - 현재가) / 진입가 * 100
        val profitRate = (entryPrice - currentPrice) / entryPrice * 100
        val lossRate = (currentPrice - entryPrice) / entryPrice * 100

        Log.d(TAG, "숏 포지션 체크 - 진입가: $entryPrice, 현재가: $currentPrice, 수익률: $profitRate%, 손실률: $lossRate%")

        // 익절 조건
        if (profitRate >= settings.profitTarget) {
            val exitAction = createShortExitAction(position, currentPrice, timestamp, currentCCI, "SHORT_PROFIT_EXIT", settings)
            actions.add(exitAction)
            Log.d(TAG, "숏 익절 완료: 수익률 $profitRate% (${entryPrice} → ${currentPrice})")
            return Pair(actions, true)
        }

        // 손절 조건
        if (lossRate >= settings.stopLossPercent) {
            val stopLossAction = createShortExitAction(position, currentPrice, timestamp, currentCCI, "SHORT_STOP_LOSS", settings)
            actions.add(stopLossAction)
            Log.d(TAG, "숏 손절 완료: 손실률 $lossRate% (${entryPrice} → ${currentPrice})")
            return Pair(actions, true)
        }

        return Pair(actions, false)
    }

    // 숏 청산 (매수)
    private fun createShortExitAction(
        position: CciPosition,
        exitPrice: Double,
        timestamp: Long,
        exitCCI: Double,
        exitType: String,
        settings: CciStrategySettings
    ): CciTradeExecution {

        val totalCoins = position.totalCoins
        val exitAmount = totalCoins * exitPrice
        val fee = exitAmount * settings.feeRate / 100
        val profitRate = (position.averagePrice - exitPrice) / position.averagePrice * 100

        Log.d(TAG, "숏 청산 - 진입가: ${position.averagePrice}, 청산가: $exitPrice, 수익률: $profitRate%")

        return CciTradeExecution(
            type = exitType,
            stage = 0,
            entryPrice = position.averagePrice,
            exitPrice = exitPrice,
            amount = exitAmount,
            coins = totalCoins,
            fees = fee,
            timestamp = timestamp,
            exitCCI = exitCCI,
            profitRate = profitRate,
            reason = getShortExitReason(exitType)
        )
    }

    // ========================================
    // 결과 생성 메소드들 (분리)
    // ========================================

    // 롱 포지션 결과 생성
    private fun createLongPositionResult(
        position: CciPosition,
        allTrades: List<CciTradeExecution>,
        positionId: Int
    ): CciPositionResult {

        val positionTrades = allTrades.filter { trade ->
            trade.timestamp >= position.timestamp && trade.type.contains("LONG")
        }.sortedBy { it.timestamp }

        val buyTrades = positionTrades.filter { it.type.contains("BUY") }
        val sellTrades = positionTrades.filter { it.type.contains("SELL") || it.type.contains("EXIT") }

        // 롱 손익: 매도금액 - 매수금액
        val totalBuyAmount = buyTrades.sumOf { it.amount }
        val totalSellAmount = sellTrades.sumOf { it.amount }
        val totalFees = positionTrades.sumOf { it.fees }
        val totalProfit = totalSellAmount - totalBuyAmount - totalFees

        val startTime = formatTimestamp(positionTrades.first().timestamp)
        val endTime = formatTimestamp(positionTrades.last().timestamp)
        val duration = calculateDuration(positionTrades.first().timestamp, positionTrades.last().timestamp)

        Log.d(TAG, "롱 포지션 결과 #$positionId: 매수=$totalBuyAmount, 매도=$totalSellAmount, 수수료=$totalFees, 순손익=$totalProfit")

        return CciPositionResult(
            positionId = positionId,
            type = "LONG",
            symbol = "BTCUSDT",
            maxStage = position.currentStage,
            totalProfit = totalProfit,
            totalFees = totalFees,
            finalResult = sellTrades.lastOrNull()?.type ?: "INCOMPLETE",
            startTime = startTime,
            endTime = endTime,
            duration = duration,
            buyTrades = buyTrades,
            sellTrades = sellTrades
        )
    }

    // 숏 포지션 결과 생성
    private fun createShortPositionResult(
        position: CciPosition,
        allTrades: List<CciTradeExecution>,
        positionId: Int
    ): CciPositionResult {

        val positionTrades = allTrades.filter { trade ->
            trade.timestamp >= position.timestamp && trade.type.contains("SHORT")
        }.sortedBy { it.timestamp }

        val sellTrades = positionTrades.filter { it.type.contains("SELL") }  // 진입 (매도)
        val buyTrades = positionTrades.filter { it.type.contains("EXIT") }   // 청산 (매수)

        // 숏 손익: 매도금액(진입) - 매수금액(청산)
        val totalSellAmount = sellTrades.sumOf { it.amount }
        val totalBuyAmount = buyTrades.sumOf { it.amount }
        val totalFees = positionTrades.sumOf { it.fees }
        val totalProfit = totalSellAmount - totalBuyAmount - totalFees

        val startTime = formatTimestamp(positionTrades.first().timestamp)
        val endTime = formatTimestamp(positionTrades.last().timestamp)
        val duration = calculateDuration(positionTrades.first().timestamp, positionTrades.last().timestamp)

        Log.d(TAG, "숏 포지션 결과 #$positionId: 매도=$totalSellAmount, 매수=$totalBuyAmount, 수수료=$totalFees, 순손익=$totalProfit")

        return CciPositionResult(
            positionId = positionId,
            type = "SHORT",
            symbol = "BTCUSDT",
            maxStage = 0,  // 숏은 물타기 없음
            totalProfit = totalProfit,
            totalFees = totalFees,
            finalResult = buyTrades.lastOrNull()?.type ?: "INCOMPLETE",
            startTime = startTime,
            endTime = endTime,
            duration = duration,
            buyTrades = buyTrades,
            sellTrades = sellTrades
        )
    }

    // 최종 결과 계산
    private fun calculateFinalResult(
        trades: List<CciTradeExecution>,
        positions: List<CciPositionResult>,
        settings: CciStrategySettings,
        finalSeedMoney: Double
    ): RealCciBacktestResult {

        val completedPositions = positions.filter { it.finalResult != "INCOMPLETE" }
        val winningPositions = completedPositions.count { it.totalProfit > 0 }
        val losingPositions = completedPositions.count { it.totalProfit <= 0 }

        val totalProfit = positions.sumOf { it.totalProfit }
        val totalFees = positions.sumOf { it.totalFees }
        val winRate = if (completedPositions.isNotEmpty()) {
            (winningPositions.toDouble() / completedPositions.size) * 100
        } else 0.0

        val profits = positions.filter { it.totalProfit > 0 }.sumOf { it.totalProfit }
        val losses = abs(positions.filter { it.totalProfit <= 0 }.sumOf { it.totalProfit })
        val profitFactor = if (losses > 0) profits / losses else if (profits > 0) Double.POSITIVE_INFINITY else 0.0

        // 최대 손실 계산
        var maxDrawdown = 0.0
        var peak = settings.seedMoney
        var currentBalance = settings.seedMoney

        for (position in positions) {
            currentBalance += position.totalProfit
            if (currentBalance > peak) {
                peak = currentBalance
            }
            val currentDrawdown = (peak - currentBalance) / peak * 100
            if (currentDrawdown > maxDrawdown) {
                maxDrawdown = currentDrawdown
            }
        }

        return RealCciBacktestResult(
            settings = settings,
            totalPositions = positions.size,
            completedPositions = completedPositions.size,
            winningPositions = winningPositions,
            losingPositions = losingPositions,
            totalTrades = trades.size,
            totalProfit = totalProfit,
            totalFees = totalFees,
            maxDrawdown = maxDrawdown,
            finalSeedMoney = finalSeedMoney,
            winRate = winRate,
            profitFactor = profitFactor,
            avgHoldingTime = calculateAvgHoldingTime(positions),
            maxStageReached = positions.maxOfOrNull { it.maxStage } ?: 0,
            positions = positions,
            trades = trades
        )
    }

    // 유틸리티 함수들
    private fun getLongEntryReason(stage: Int): String {
        return when (stage) {
            0 -> "CCI 과매도 회복 신호 (롱 매수 진입)"
            1 -> "평균단가 대비 2% 손실 - 1단계 물타기"
            2 -> "평균단가 대비 4% 손실 - 2단계 물타기"
            3 -> "평균단가 대비 8% 손실 - 3단계 물타기"
            else -> "${stage}단계 물타기"
        }
    }

    private fun getLongExitReason(exitType: String): String {
        return when (exitType) {
            "LONG_PROFIT_EXIT" -> "롱 익절 목표 달성"
            "LONG_STOP_LOSS" -> "롱 손절 조건 도달"
            "LONG_HALF_SELL" -> "롱 0.5% 수익시 절반 매도"
            else -> exitType
        }
    }

    private fun getShortExitReason(exitType: String): String {
        return when (exitType) {
            "SHORT_PROFIT_EXIT" -> "숏 익절 목표 달성 (가격 하락)"
            "SHORT_STOP_LOSS" -> "숏 손절 조건 도달 (가격 상승)"
            else -> exitType
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    private fun calculateDuration(startTime: Long, endTime: Long): String {
        val diffInMillis = endTime - startTime
        val diffInMinutes = diffInMillis / (1000 * 60)
        val hours = diffInMinutes / 60
        val minutes = diffInMinutes % 60

        return when {
            hours > 24 -> "${hours / 24}일 ${hours % 24}시간"
            hours > 0 -> "${hours}시간 ${minutes}분"
            else -> "${minutes}분"
        }
    }

    private fun calculateAvgHoldingTime(positions: List<CciPositionResult>): Double {
        if (positions.isEmpty()) return 0.0

        val totalHours = positions.sumOf { position ->
            // duration 문자열에서 시간 추출 (간단한 계산)
            when {
                position.duration.contains("일") -> {
                    val days = position.duration.substringBefore("일").toDoubleOrNull() ?: 0.0
                    days * 24
                }
                position.duration.contains("시간") -> {
                    position.duration.substringBefore("시간").toDoubleOrNull() ?: 0.0
                }
                else -> 1.0 // 기본값
            }
        }

        return totalHours / positions.size
    }
}