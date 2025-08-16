// TradingStrategyDataClasses.kt - 모든 거래 전략 관련 데이터 클래스들

package com.example.ver20.dao

// 백테스팅 설정 데이터 클래스
data class CciStrategySettings(
    val timeframe: String = "4시간",
    val symbol: String = "BTCUSDT",
    val seedMoney: Double = 10000.0,
    val testPeriod: String = "1년",
    val startAmount: Double = 2000.0, // 시드머니의 20%
    val entryThreshold: Int = 110,
    val exitThreshold: Int = 100,
    val profitTarget: Double = 3.0, // 3%
    val feeRate: Double = 0.04 // 0.04%
)

// 백테스팅 결과 데이터 클래스
data class CciBacktestResult(
    val totalTrades: Int,
    val winningTrades: Int,
    val losingTrades: Int,
    val totalProfit: Double,
    val totalFees: Double,
    val maxDrawdown: Double,
    val finalSeedMoney: Double,
    val winRate: Double,
    val profitFactor: Double,
    val trades: List<TradeResult>
)

data class TradeResult(
    val type: String, // "LONG" or "SHORT"
    val entryPrice: Double,
    val exitPrice: Double,
    val amount: Double,
    val profit: Double,
    val fee: Double,
    val timestamp: String,
    val entryCCI: Double = 0.0,      // 👈 추가
    val previousCCI: Double = 0.0,   // 👈 추가
    val exitReason: String = "PROFIT" // 👈 추가
)

// 백테스팅 엔진에서 사용하는 데이터 클래스들
data class PriceCandle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

data class Position(
    val type: String, // "LONG" or "SHORT"
    val stages: MutableList<PositionStage>,
    var totalAmount: Double,
    var averagePrice: Double
)

data class PositionStage(
    val entryPrice: Double,
    val amount: Double,
    val timestamp: Long
)

data class TradeExecution(
    val type: String,
    val entryPrice: Double,
    val exitPrice: Double,
    val amount: Double,
    val grossProfit: Double,
    val fees: Double,
    val netProfit: Double,
    val exitType: String,
    val stages: Int,
    val timestamp: Long,
    val entryCCI: Double = 0.0,      // 👈 추가
    val previousCCI: Double = 0.0,   // 👈 추가
    val exitCCI: Double = 0.0        // 👈 추가
)