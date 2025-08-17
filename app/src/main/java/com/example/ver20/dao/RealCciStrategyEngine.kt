// RealCciStrategyEngine.kt - 실제 CCI 전략 구현

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
                    currentPosition = CciPosition(
                        type = signal,
                        stages = mutableListOf(),
                        currentStage = 0,
                        timestamp = currentTimestamp,
                        entryCCI = currentCCI,
                        previousCCI = previousCCI
                    )

                    // 첫 진입 (1단계)
                    val firstStage = executeStageEntry(
                        position = currentPosition,
                        price = currentPrice,
                        stage = 0,
                        amount = settings.startAmount,
                        timestamp = currentTimestamp,
                        cci = currentCCI,
                        settings = settings
                    )

                    trades.add(firstStage)
                    Log.d(TAG, "🎯 진입: $signal @ $currentPrice, CCI: $currentCCI")
                }
            } else {
                // 포지션 관리 (null 체크 추가)
                currentPosition?.let { position ->
                    val actions = managePosition(position, currentPrice, currentCCI, settings, currentTimestamp)

                    actions.forEach { action ->
                        trades.add(action)

                        // 포지션 종료 체크
                        if (action.type in listOf("PROFIT_EXIT", "STOP_LOSS", "COMPLETE_EXIT")) {
                            val positionResult = createPositionResult(position, trades, positionId)
                            positions.add(positionResult)

                            // 시드머니 업데이트
                            val positionProfit = positionResult.totalProfit
                            currentSeedMoney += positionProfit

                            Log.d(TAG, "🏁 포지션 종료: ${position.type}, 수익: $positionProfit")
                            currentPosition = null
                        }
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

    // 단계별 진입 실행
    private fun executeStageEntry(
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

        // 거래 타입 설정 (숏과 롱 구분)
        val tradeType = if (position.type == "LONG") {
            "STAGE${stage}_BUY"
        } else {
            "STAGE${stage}_SHORT_SELL" // 숏은 매도로 시작
        }

        Log.d(TAG, "${position.type} 진입 - 단계: $stage, 가격: $price, 금액: $amount, 평균단가: ${position.averagePrice}")

        return CciTradeExecution(
            type = tradeType,
            stage = stage,
            entryPrice = price,
            amount = amount,
            coins = coins,
            fees = fee,
            timestamp = timestamp,
            entryCCI = cci,
            reason = getStageEntryReason(stage, position.type)
        )
    }

    // 포지션 관리 (실제 전략 로직)
    private fun managePosition(
        position: CciPosition,
        currentPrice: Double,
        currentCCI: Double,
        settings: CciStrategySettings,
        timestamp: Long
    ): List<CciTradeExecution> {

        val actions = mutableListOf<CciTradeExecution>()

        when (position.type) {
            "LONG" -> {
                // 롱 포지션 관리
                actions.addAll(manageLongPosition(position, currentPrice, currentCCI, settings, timestamp))
            }
            "SHORT" -> {
                // 숏 포지션 관리 (물타기 없음, 손익절만)
                actions.addAll(manageShortPosition(position, currentPrice, currentCCI, settings, timestamp))
            }
        }

        return actions
    }

    // 롱 포지션 관리 (물타기 포함)
    private fun manageLongPosition(
        position: CciPosition,
        currentPrice: Double,
        currentCCI: Double,
        settings: CciStrategySettings,
        timestamp: Long
    ): List<CciTradeExecution> {

        val actions = mutableListOf<CciTradeExecution>()
        val averagePrice = position.averagePrice
        val currentLossRate = (currentPrice - averagePrice) / averagePrice * 100

        // 1. 익절 조건 체크 (1단계: 설정된 익절률, 다른 단계: 0.5%)
        val profitRate = (currentPrice - averagePrice) / averagePrice * 100
        val targetProfit = if (position.currentStage == 0) settings.profitTarget else settings.halfSellProfit

        if (profitRate >= targetProfit) {
            if (position.currentStage == 0) {
                // 1단계에서 익절 시 전액 매도
                val exitAction = createExitAction(position, currentPrice, timestamp, currentCCI, "PROFIT_EXIT", settings)
                actions.add(exitAction)
            } else {
                // 다른 단계에서는 절반 매도
                val halfSellAction = createHalfSellAction(position, currentPrice, timestamp, currentCCI, settings)
                actions.add(halfSellAction)
            }
            return actions
        }

        // 2. 물타기 조건 체크
        when (position.currentStage) {
            0 -> {
                if (currentLossRate <= -settings.stage1Loss) {
                    // 현재 물량만큼 추가 매수
                    val additionalAmount = position.totalAmount
                    val stageEntry = executeStageEntry(position, currentPrice, 1, additionalAmount, timestamp, currentCCI, settings)
                    actions.add(stageEntry)
                }
            }
            1 -> {
                if (currentLossRate <= -settings.stage2Loss) {
                    val additionalAmount = position.totalAmount
                    val stageEntry = executeStageEntry(position, currentPrice, 2, additionalAmount, timestamp, currentCCI, settings)
                    actions.add(stageEntry)
                }
            }
            2 -> {
                if (currentLossRate <= -settings.stage3Loss) {
                    val additionalAmount = position.totalAmount
                    val stageEntry = executeStageEntry(position, currentPrice, 3, additionalAmount, timestamp, currentCCI, settings)
                    actions.add(stageEntry)
                }
            }
            3 -> {
                if (currentLossRate <= -settings.stage4Loss) {
                    // 4단계에서 -10% 손실 시 손절
                    val stopLossAction = createExitAction(position, currentPrice, timestamp, currentCCI, "STOP_LOSS", settings)
                    actions.add(stopLossAction)
                }
            }
        }

        return actions
    }

    // 숏 포지션 관리 (물타기 없음)
    private fun manageShortPosition(
        position: CciPosition,
        currentPrice: Double,
        currentCCI: Double,
        settings: CciStrategySettings,
        timestamp: Long
    ): List<CciTradeExecution> {

        val actions = mutableListOf<CciTradeExecution>()
        val averagePrice = position.averagePrice

        // 숏의 수익률 계산 수정: (진입가 - 현재가) / 진입가 * 100
        val profitRate = (averagePrice - currentPrice) / averagePrice * 100
        val lossRate = (currentPrice - averagePrice) / averagePrice * 100

        Log.d(TAG, "숏 포지션 관리 - 진입가: $averagePrice, 현재가: $currentPrice, 수익률: $profitRate%, 손실률: $lossRate%")

        // 숏은 단순 손익절만
        if (profitRate >= settings.profitTarget) {
            // 익절: 가격이 하락하여 수익
            val exitAction = createExitAction(position, currentPrice, timestamp, currentCCI, "PROFIT_EXIT", settings)
            actions.add(exitAction)
            Log.d(TAG, "숏 익절 실행: 수익률 $profitRate%")
        } else if (lossRate >= settings.stopLossPercent) {
            // 손절: 가격이 상승하여 손실
            val stopLossAction = createExitAction(position, currentPrice, timestamp, currentCCI, "STOP_LOSS", settings)
            actions.add(stopLossAction)
            Log.d(TAG, "숏 손절 실행: 손실률 $lossRate%")
        }

        return actions
    }

    // 전액 청산 액션 생성
    private fun createExitAction(
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

        // 수익률 계산 수정
        val profitRate = if (position.type == "LONG") {
            // 롱: (매도가 - 매수가) / 매수가 * 100
            (exitPrice - position.averagePrice) / position.averagePrice * 100
        } else {
            // 숏: (매수가 - 매도가) / 매수가 * 100 (숏은 높은 가격에 매도 후 낮은 가격에 매수)
            (position.averagePrice - exitPrice) / position.averagePrice * 100
        }

        // 실제 손익 계산 (수수료 포함)
        val actualProfit = if (position.type == "LONG") {
            // 롱: 매도금액 - 매수금액 - 수수료
            exitAmount - position.totalAmount - fee
        } else {
            // 숏: 매수금액 - 매도금액 - 수수료 (숏은 반대)
            position.totalAmount - exitAmount - fee
        }

        Log.d(TAG, "${position.type} 청산 - 진입가: ${position.averagePrice}, 청산가: $exitPrice, 수익률: $profitRate%, 실제손익: $actualProfit")

        return CciTradeExecution(
            type = exitType,
            stage = position.currentStage,
            entryPrice = position.averagePrice,
            exitPrice = exitPrice,
            amount = if (position.type == "LONG") exitAmount else actualProfit + position.totalAmount, // 숏은 실제 수익 반영
            coins = totalCoins,
            fees = fee,
            timestamp = timestamp,
            exitCCI = exitCCI,
            profitRate = profitRate,
            reason = getExitReason(exitType)
        )
    }

    // 절반 매도 액션 생성
    private fun createHalfSellAction(
        position: CciPosition,
        exitPrice: Double,
        timestamp: Long,
        exitCCI: Double,
        settings: CciStrategySettings
    ): CciTradeExecution {

        val halfCoins = position.totalCoins / 2
        val exitAmount = halfCoins * exitPrice
        val fee = exitAmount * settings.feeRate / 100
        val profitRate = if (position.type == "LONG") {
            (exitPrice - position.averagePrice) / position.averagePrice * 100
        } else {
            (position.averagePrice - exitPrice) / position.averagePrice * 100
        }

        // 포지션 정보 업데이트
        position.totalCoins = halfCoins
        position.totalAmount = halfCoins * position.averagePrice
        position.currentStage = maxOf(0, position.currentStage - 1) // 한 단계 하락

        return CciTradeExecution(
            type = "HALF_SELL",
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

    // 포지션 결과 생성
    private fun createPositionResult(
        position: CciPosition,
        allTrades: List<CciTradeExecution>,
        positionId: Int
    ): CciPositionResult {

        val positionTrades = allTrades.filter { trade ->
            trade.timestamp >= position.timestamp &&
                    (trade.type.contains("STAGE") || trade.type in listOf("HALF_SELL", "PROFIT_EXIT", "STOP_LOSS"))
        }

        val buyTrades = positionTrades.filter {
            if (position.type == "LONG") {
                it.type.contains("BUY")
            } else {
                // 숏의 경우: 최종 청산이 "매수"가 됨
                it.type in listOf("PROFIT_EXIT", "STOP_LOSS", "COMPLETE_EXIT")
            }
        }
        val sellTrades = positionTrades.filter {
            if (position.type == "LONG") {
                !it.type.contains("BUY")
            } else {
                // 숏의 경우: 진입이 "매도"가 됨
                it.type.contains("SHORT_SELL") || it.type.contains("STAGE")
            }
        }

        // 손익 계산 수정
        val totalProfit = if (position.type == "LONG") {
            // 롱: 매도금액 - 매수금액
            sellTrades.sumOf { it.amount } - buyTrades.sumOf { it.amount }
        } else {
            // 숏: 매수금액 - 매도금액 (숏은 높은 가격에서 매도 시작, 낮은 가격에서 매수 종료)
            buyTrades.sumOf { it.amount } - sellTrades.sumOf { it.amount }
        }

        val totalFees = positionTrades.sumOf { it.fees }

        val startTime = formatTimestamp(position.timestamp)
        val endTime = formatTimestamp(sellTrades.lastOrNull()?.timestamp ?: position.timestamp)
        val duration = calculateDuration(position.timestamp, sellTrades.lastOrNull()?.timestamp ?: position.timestamp)

        val finalProfit = totalProfit - totalFees

        Log.d(TAG, "포지션 결과 생성 - ${position.type} #$positionId: 총손익=$totalProfit, 수수료=$totalFees, 최종손익=$finalProfit")

        return CciPositionResult(
            positionId = positionId,
            type = position.type,
            symbol = "BTCUSDT", // 실제로는 설정에서 가져와야 함
            maxStage = position.currentStage,
            totalProfit = finalProfit,
            totalFees = totalFees,
            finalResult = sellTrades.lastOrNull()?.type ?: "INCOMPLETE",
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
    private fun getStageEntryReason(stage: Int, type: String): String {
        return when (stage) {
            0 -> "CCI 진입 신호 (${if (type == "LONG") "과매도 회복" else "과매수 회복"})"
            1 -> "평균단가 대비 2% 손실 - 현재물량만큼 추가매수"
            2 -> "평균단가 대비 4% 손실 - 현재물량만큼 추가매수"
            3 -> "평균단가 대비 8% 손실 - 현재물량만큼 추가매수"
            else -> "물타기 매수"
        }
    }

    private fun getExitReason(exitType: String): String {
        return when (exitType) {
            "PROFIT_EXIT" -> "익절 목표 달성"
            "STOP_LOSS" -> "손절 조건 도달"
            "HALF_SELL" -> "0.5% 수익시 절반 매도"
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