// MarketSignalModels.kt - 시세포착 관련 데이터 모델 (다중 전략 지원)

package com.example.ver20.dao.trading.signal

import java.text.SimpleDateFormat
import java.util.*

// ===== 시세포착 설정 데이터 =====

/**
 * 시세포착 설정 데이터 클래스 (다중 전략 지원)
 */
data class MarketSignalConfig(
    val id: String = UUID.randomUUID().toString(),
    val configId: String = "", // 서버에서 생성되는 실제 설정 ID
    val username: String,
    val signalType: String, // "RSI", "CCI", "CORTA"
    val symbol: String,
    val timeframe: String, // "15m", "1h", "4h", "1d"
    val checkInterval: Int, // 진입체크 인터벌 (분 단위)
    val isActive: Boolean = true,
    val autoTrading: Boolean = false, // 자동매매 활성화 여부
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
    val seedMoney: Double = 1000.0, // 공통 진입시드

    // CCI 전용 설정
    val cciPeriod: Int = 20,
    val cciBreakoutValue: Double = 100.0, // 돌파값
    val cciEntryValue: Double = 90.0, // 진입값

    // RSI 전용 설정
    val rsiPeriod: Int = 14,
    val rsiOverbought: Double = 70.0, // 과매수 구간
    val rsiOversold: Double = 30.0, // 과매도 구간

    // 코르타 전용 설정 (복합 지표)
    val cortaFastMa: Int = 12, // 빠른 이동평균
    val cortaSlowMa: Int = 26, // 느린 이동평균
    val cortaSignalLine: Int = 9, // 시그널 라인
    val cortaVolumeFactor: Double = 1.5, // 거래량 임계값
    val cortaRsiConfirm: Boolean = true // RSI 확인 여부
) {
    /**
     * 전략별 설정 유효성 검증
     */
    fun isValid(): Boolean {
        val baseValid = username.isNotBlank() &&
                symbol.isNotBlank() &&
                checkInterval > 0 &&
                seedMoney > 0

        return when (signalType) {
            "CCI" -> baseValid &&
                    cciPeriod > 0 &&
                    cciBreakoutValue > 0 &&
                    cciEntryValue > 0 &&
                    cciEntryValue < cciBreakoutValue

            "RSI" -> baseValid &&
                    rsiPeriod > 0 &&
                    rsiOverbought > rsiOversold &&
                    rsiOverbought <= 100 &&
                    rsiOversold >= 0

            "CORTA" -> baseValid &&
                    cortaFastMa > 0 &&
                    cortaSlowMa > cortaFastMa &&
                    cortaSignalLine > 0 &&
                    cortaVolumeFactor > 0

            else -> baseValid
        }
    }

    /**
     * 전략별 설정 요약 정보
     */
    fun getSummary(): String {
        val strategyInfo = when (signalType) {
            "CCI" -> "CCI(${cciPeriod}) ${cciBreakoutValue}↑/${cciEntryValue}↓"
            "RSI" -> "RSI(${rsiPeriod}) ${rsiOverbought}/${rsiOversold}"
            "CORTA" -> "CORTA ${cortaFastMa}/${cortaSlowMa}/${cortaSignalLine}"
            else -> signalType
        }
        return "$strategyInfo • $symbol • $timeframe (${checkInterval}분)"
    }

    /**
     * 자동매매 상태 텍스트
     */
    fun getAutoTradingStatusText(): String {
        return if (autoTrading) "자동매매 ON" else "수동 확인"
    }

    /**
     * 전략별 컬러 코드
     */
    fun getStrategyColor(): Long {
        return when (signalType) {
            "CCI" -> 0xFF2196F3 // 파란색
            "RSI" -> 0xFF4CAF50 // 초록색
            "CORTA" -> 0xFFFFD700 // 금색
            else -> 0xFF9E9E9E // 회색
        }
    }

    /**
     * 전략별 아이콘 이름
     */
    fun getStrategyIcon(): String {
        return when (signalType) {
            "CCI" -> "TrendingUp"
            "RSI" -> "ShowChart"
            "CORTA" -> "AutoAwesome"
            else -> "Analytics"
        }
    }
}

// ===== 시세포착 신호 데이터 =====

/**
 * 시세포착 신호 데이터 클래스 (다중 전략 지원)
 */
data class MarketSignal(
    val id: String = UUID.randomUUID().toString(),
    val configId: String,
    val username: String,
    val symbol: String,
    val signalType: String, // "RSI", "CCI", "CORTA"
    val direction: String, // "LONG", "SHORT"
    val timestamp: Long = System.currentTimeMillis(),
    val price: Double,
    val volume: Double,
    val reason: String = "",
    val timeframe: String = "",
    val status: String = "ACTIVE",
    val isRead: Boolean = false,

    // CCI 관련 정보
    val cciValue: Double = 0.0,
    val cciBreakoutValue: Double = 0.0,
    val cciEntryValue: Double = 0.0,

    // RSI 관련 정보
    val rsiValue: Double = 0.0,
    val rsiOverbought: Double = 0.0,
    val rsiOversold: Double = 0.0,

    // 코르타 관련 정보
    val cortaMacdLine: Double = 0.0,
    val cortaSignalLine: Double = 0.0,
    val cortaHistogram: Double = 0.0,
    val cortaVolumeRatio: Double = 0.0,
    val cortaRsiConfirm: Double = 0.0
) {
    /**
     * 시그널의 유효성 검증
     */
    fun isValid(): Boolean {
        return configId.isNotBlank() &&
                username.isNotBlank() &&
                symbol.isNotBlank() &&
                direction in listOf("LONG", "SHORT") &&
                price > 0 &&
                volume >= 0
    }

    /**
     * 전략별 주요 지표 값 가져오기
     */
    fun getPrimaryIndicatorValue(): String {
        return when (signalType) {
            "CCI" -> "CCI: ${String.format("%.1f", cciValue)}"
            "RSI" -> "RSI: ${String.format("%.1f", rsiValue)}"
            "CORTA" -> "MACD: ${String.format("%.4f", cortaMacdLine)}"
            else -> ""
        }
    }

    /**
     * 전략별 보조 지표 값 가져오기
     */
    fun getSecondaryIndicatorValue(): String {
        return when (signalType) {
            "CCI" -> "목표: ${String.format("%.1f", if (direction == "LONG") cciBreakoutValue else -cciBreakoutValue)}"
            "RSI" -> "임계: ${if (direction == "LONG") String.format("%.1f", rsiOversold) else String.format("%.1f", rsiOverbought)}"
            "CORTA" -> "Signal: ${String.format("%.4f", cortaSignalLine)}"
            else -> ""
        }
    }

    /**
     * 시그널 강도 계산 (0-100)
     */
    fun getSignalStrength(): Int {
        return when (signalType) {
            "CCI" -> {
                val strength = if (direction == "LONG") {
                    ((cciValue - cciEntryValue) / (cciBreakoutValue - cciEntryValue) * 100).coerceIn(0.0, 100.0)
                } else {
                    (((-cciValue) - cciEntryValue) / (cciBreakoutValue - cciEntryValue) * 100).coerceIn(0.0, 100.0)
                }
                strength.toInt()
            }
            "RSI" -> {
                val strength = if (direction == "LONG") {
                    ((rsiOversold - rsiValue) / rsiOversold * 100).coerceIn(0.0, 100.0)
                } else {
                    ((rsiValue - rsiOverbought) / (100 - rsiOverbought) * 100).coerceIn(0.0, 100.0)
                }
                strength.toInt()
            }
            "CORTA" -> {
                // MACD 히스토그램의 절댓값을 기반으로 강도 계산
                (Math.abs(cortaHistogram) * 1000).coerceIn(0.0, 100.0).toInt()
            }
            else -> 50
        }
    }
}

// ===== 전략별 기본값 제공 =====

/**
 * 전략별 기본 설정값 제공
 */
object StrategyDefaults {
    fun getCciDefaults() = mapOf(
        "cciPeriod" to 20,
        "cciBreakoutValue" to 100.0,
        "cciEntryValue" to 90.0
    )

    fun getRsiDefaults() = mapOf(
        "rsiPeriod" to 14,
        "rsiOverbought" to 70.0,
        "rsiOversold" to 30.0
    )

    fun getCortaDefaults() = mapOf(
        "cortaFastMa" to 12,
        "cortaSlowMa" to 26,
        "cortaSignalLine" to 9,
        "cortaVolumeFactor" to 1.5,
        "cortaRsiConfirm" to true
    )

    fun getDefaultConfig(signalType: String, username: String): MarketSignalConfig {
        return when (signalType) {
            "CCI" -> MarketSignalConfig(
                username = username,
                signalType = "CCI",
                symbol = "BTCUSDT",
                timeframe = "15m",
                checkInterval = 15
            )
            "RSI" -> MarketSignalConfig(
                username = username,
                signalType = "RSI",
                symbol = "BTCUSDT",
                timeframe = "15m",
                checkInterval = 15
            )
            "CORTA" -> MarketSignalConfig(
                username = username,
                signalType = "CORTA",
                symbol = "BTCUSDT",
                timeframe = "15m",
                checkInterval = 15
            )
            else -> MarketSignalConfig(
                username = username,
                signalType = "CCI",
                symbol = "BTCUSDT",
                timeframe = "15m",
                checkInterval = 15
            )
        }
    }
}