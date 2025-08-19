// 수정된 CciStrategySettings.kt - 실제 전략에 맞게 업데이트

package com.example.ver20.dao.trading.backtest

// 가격 캔들 데이터 클래스 (누락된 부분 추가)
data class PriceCandle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)
// 실제 CCI 전략 설정 데이터 클래스
data class CciStrategySettings(
    val timeframe: String = "4시간",
    val symbol: String = "BTCUSDT",
    val seedMoney: Double = 10000.0,
    val testPeriod: String = "1년",
    val startAmount: Double = 2000.0, // 시드머니의 20%
    
    // CCI 관련 설정
    val cciLength: Int = 20, // 기본값 20으로 변경
    val entryThreshold: Int = 110,  // ±110에서 진입
    val exitThreshold: Int = 100,   // ±100에서 청산
    
    // 손익절 설정 (0.5% 단위로 조절 가능)
    val profitTarget: Double = 3.0, // 1단계 익절 목표 (기본 3%)
    val halfSellProfit: Double = 0.5, // 각 단계 절반매도 수익률 (0.5%)
    val stopLossPercent: Double = 10.0, // 4단계 최종 손절 (-10%)
    
    // 물타기 단계별 손실 기준
    val stage1Loss: Double = 2.0,  // 1단계: -2%
    val stage2Loss: Double = 4.0,  // 2단계: -4% 
    val stage3Loss: Double = 8.0,  // 3단계: -8%
    val stage4Loss: Double = 10.0, // 4단계: -10% (손절)
    
    val feeRate: Double = 0.04 // 0.04%
)

// 포지션 정보 (실제 물타기 방식으로 수정)
// 포지션 정보 (실제 물타기 방식으로 수정)
data class CciPosition(
    val type: String, // "LONG" or "SHORT"
    val stages: MutableList<CciPositionStage>,
    var currentStage: Int = 0, // 현재 단계 (0~4)
    var totalAmount: Double = 0.0,
    var totalCoins: Double = 0.0,
    var averagePrice: Double = 0.0,
    val timestamp: Long,
    val entryCCI: Double,
    val previousCCI: Double,
    val symbol: String = "TRADE" // 기본값 추가
)

data class CciPositionStage(
    val stage: Int,
    val entryPrice: Double,
    val amount: Double,
    val coins: Double,
    val timestamp: Long
)

// 물타기 거래 실행 정보
data class CciTradeExecution(
    val type: String, // "STAGE0_BUY", "STAGE1_BUY", "HALF_SELL", "PROFIT_EXIT", "STOP_LOSS"
    val stage: Int,
    val entryPrice: Double,
    val exitPrice: Double? = null,
    val amount: Double,
    val coins: Double,
    val fees: Double,
    val timestamp: Long,
    val entryCCI: Double = 0.0,
    val exitCCI: Double = 0.0,
    val profitRate: Double = 0.0,
    val reason: String = ""
)

// 실제 CCI 백테스트 결과
data class RealCciBacktestResult(
    val settings: CciStrategySettings,
    val totalPositions: Int,
    val completedPositions: Int,
    val winningPositions: Int,
    val losingPositions: Int,
    val totalTrades: Int, // 개별 매수/매도 거래 수
    val totalProfit: Double,
    val totalFees: Double,
    val maxDrawdown: Double,
    val finalSeedMoney: Double,
    val winRate: Double,
    val profitFactor: Double,
    val avgHoldingTime: Double, // 평균 보유 시간 (시간 단위)
    val maxStageReached: Int, // 최대 도달 단계
    val positions: List<CciPositionResult>,
    val trades: List<CciTradeExecution>
)

// 포지션별 결과
data class CciPositionResult(
    val positionId: Int,
    val type: String,
    val symbol: String,
    val maxStage: Int,
    val totalProfit: Double,
    val totalFees: Double,
    val finalResult: String, // "PROFIT_EXIT", "HALF_SELL_EXIT", "STOP_LOSS"
    val startTime: String,
    val endTime: String,
    val duration: String,
    val buyTrades: List<CciTradeExecution>,
    val sellTrades: List<CciTradeExecution>
)