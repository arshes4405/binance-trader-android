// MarketSignalModels.kt - 시세포착 관련 데이터 모델

package com.example.ver20.dao

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

// ===== 시그널 상태 정의 =====

/**
 * CCI 시그널 상태 열거형
 */
enum class CciSignalState {
    NO_BREAKOUT,     // 미돌파 (기본 상태)
    LONG_BREAKOUT,   // 롱 돌파 상태 (CCI가 -돌파값 아래로 내려감)
    SHORT_BREAKOUT   // 숏 돌파 상태 (CCI가 +돌파값 위로 올라감)
}

// ===== 시세포착 설정 데이터 =====

/**
 * 시세포착 설정 데이터 클래스
 */
data class MarketSignalConfig(
    val id: String = UUID.randomUUID().toString(),
    val username: String,
    val signalType: String, // "CCI", "RSI", "MA"
    val symbol: String,
    val timeframe: String, // "15m", "1h", "4h", "1d"
    val checkInterval: Int, // 진입체크 인터벌 (초)
    val isActive: Boolean = true,
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),

    // CCI 전용 설정
    val cciPeriod: Int = 20,
    val cciBreakoutValue: Double = 100.0, // 돌파값
    val cciEntryValue: Double = 90.0, // 진입값
    val seedMoney: Double = 1000.0 // 진입시드
) {
    /**
     * 설정 유효성 검증
     */
    fun isValid(): Boolean {
        return username.isNotBlank() &&
                symbol.isNotBlank() &&
                checkInterval > 0 &&
                cciPeriod > 0 &&
                cciBreakoutValue > 0 &&
                cciEntryValue > 0 &&
                cciEntryValue < cciBreakoutValue &&
                seedMoney > 0
    }
}

// ===== 시세포착 신호 데이터 =====

/**
 * 시세포착 신호 데이터 클래스
 */
data class MarketSignal(
    val id: String = UUID.randomUUID().toString(),
    val configId: String,
    val username: String,
    val symbol: String,
    val signalType: String,
    val direction: String, // "LONG", "SHORT"
    val timestamp: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
    val price: Double,
    val volume: Double,

    // CCI 관련 정보
    val cciValue: Double,
    val cciBreakoutValue: Double,
    val cciEntryValue: Double,
    val reason: String, // 진입 이유
    val timeframe: String,

    // 상태
    val status: String = "ACTIVE", // ACTIVE, EXPIRED, EXECUTED
    val isRead: Boolean = false
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
     * 시그널의 방향성 표시 아이콘
     */
    fun getDirectionIcon(): String {
        return when (direction) {
            "LONG" -> "📈"
            "SHORT" -> "📉"
            else -> "⚪"
        }
    }

    /**
     * 시그널의 간단한 요약 텍스트
     */
    fun getSummary(): String {
        return "${getDirectionIcon()} $symbol $direction @ $price (CCI: ${String.format("%.2f", cciValue)})"
    }
}

// ===== DB 기반 돌파 상태 데이터 =====

/**
 * DB에 저장되는 돌파 상태 데이터
 */
data class BreakoutStateData(
    val configId: String,
    val currentState: CciSignalState,
    val lastCciValue: Double,
    val breakoutValue: Double,
    val entryValue: Double,
    val lastCheckTime: Long
) {
    /**
     * 상태가 진입 대기 중인지 확인
     */
    fun isWaitingForEntry(): Boolean {
        return currentState == CciSignalState.LONG_BREAKOUT ||
                currentState == CciSignalState.SHORT_BREAKOUT
    }

    /**
     * 상태 설명 텍스트
     */
    fun getStateDescription(): String {
        return when (currentState) {
            CciSignalState.NO_BREAKOUT -> "미돌파 (대기 중)"
            CciSignalState.LONG_BREAKOUT -> "롱 돌파 (진입 대기)"
            CciSignalState.SHORT_BREAKOUT -> "숏 돌파 (진입 대기)"
        }
    }

    /**
     * 상태 표시 아이콘
     */
    fun getStateIcon(): String {
        return when (currentState) {
            CciSignalState.NO_BREAKOUT -> "⏳"
            CciSignalState.LONG_BREAKOUT -> "🔥📈"
            CciSignalState.SHORT_BREAKOUT -> "🔥📉"
        }
    }
}

// ===== 시세 데이터 (K-line) =====

/**
 * 바이낸스 K-line 데이터
 */
data class KlineData(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
) {
    /**
     * 전형적인 가격 (CCI 계산용)
     */
    val typical: Double get() = (high + low + close) / 3.0

    /**
     * 변동폭 계산
     */
    val range: Double get() = high - low

    /**
     * 변동률 계산 (%)
     */
    val changePercent: Double get() = if (open != 0.0) ((close - open) / open) * 100 else 0.0

    /**
     * 상승/하락 여부
     */
    val isGreen: Boolean get() = close > open

    /**
     * 가격 정보 요약
     */
    fun getPriceSummary(): String {
        val changeSign = if (changePercent >= 0) "+" else ""
        return "O:$open H:$high L:$low C:$close (${changeSign}${String.format("%.2f", changePercent)}%)"
    }
}

// ===== CCI 계산 결과 =====

/**
 * CCI 값과 관련 정보
 */
data class CciValue(
    val timestamp: Long,
    val price: Double,
    val volume: Double,
    val cciValue: Double
) {
    /**
     * CCI 신호 강도 분류
     */
    fun getSignalStrength(): String {
        return when {
            abs(cciValue) > 200 -> "매우 강함"
            abs(cciValue) > 150 -> "강함"
            abs(cciValue) > 100 -> "보통"
            abs(cciValue) > 50 -> "약함"
            else -> "신호 없음"
        }
    }

    /**
     * CCI 값의 상태 (과매수/과매도)
     */
    fun getCciStatus(): String {
        return when {
            cciValue > 100 -> "과매수"
            cciValue < -100 -> "과매도"
            else -> "중립"
        }
    }
}

// ===== 신호 체크 결과 =====

/**
 * 시그널 체크 결과
 */
data class SignalCheckResult(
    val hasSignal: Boolean,
    val direction: String? = null,
    val reason: String = "",
    val cciValue: Double = 0.0,
    val price: Double = 0.0,
    val volume: Double = 0.0,
    val confidence: Double = 0.0 // 신호 신뢰도 (0.0 ~ 1.0)
) {
    /**
     * 신호 신뢰도 텍스트
     */
    fun getConfidenceText(): String {
        return when {
            confidence >= 0.8 -> "높음"
            confidence >= 0.6 -> "보통"
            confidence >= 0.4 -> "낮음"
            else -> "매우 낮음"
        }
    }
}

/**
 * 상태 기반 신호 결과 (중복 체크 포함)
 */
data class StateBasedSignalResult(
    val hasSignal: Boolean,
    val direction: String? = null,
    val reason: String = "",
    val price: Double = 0.0,
    val volume: Double = 0.0,
    val isDuplicate: Boolean = false,
    val stateChanged: Boolean = false
)

// ===== API 관련 데이터 모델 =====

/**
 * MongoDB API 응답 데이터
 */
data class MarketSignalApiResponse(
    val success: Boolean,
    val data: Any? = null,
    val message: String? = null,
    val timestamp: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
) {
    /**
     * 응답이 성공적이고 데이터가 있는지 확인
     */
    fun hasData(): Boolean {
        return success && data != null
    }
}

/**
 * MongoDB 저장용 시세포착 설정 요청
 */
data class MarketSignalConfigRequest(
    val username: String,
    val signalType: String,
    val symbol: String,
    val timeframe: String,
    val checkInterval: Int,
    val cciPeriod: Int,
    val cciBreakoutValue: Double,
    val cciEntryValue: Double,
    val seedMoney: Double,
    val isActive: Boolean
) {
    /**
     * MarketSignalConfig로 변환
     */
    fun toMarketSignalConfig(): MarketSignalConfig {
        return MarketSignalConfig(
            username = username,
            signalType = signalType,
            symbol = symbol,
            timeframe = timeframe,
            checkInterval = checkInterval,
            cciPeriod = cciPeriod,
            cciBreakoutValue = cciBreakoutValue,
            cciEntryValue = cciEntryValue,
            seedMoney = seedMoney,
            isActive = isActive
        )
    }
}

/**
 * MongoDB 저장용 시세포착 신호 요청
 */
data class MarketSignalRequest(
    val configId: String,
    val username: String,
    val symbol: String,
    val signalType: String,
    val direction: String,
    val price: Double,
    val volume: Double,
    val cciValue: Double,
    val cciBreakoutValue: Double,
    val cciEntryValue: Double,
    val reason: String,
    val timeframe: String
) {
    /**
     * MarketSignal로 변환
     */
    fun toMarketSignal(): MarketSignal {
        return MarketSignal(
            configId = configId,
            username = username,
            symbol = symbol,
            signalType = signalType,
            direction = direction,
            price = price,
            volume = volume,
            cciValue = cciValue,
            cciBreakoutValue = cciBreakoutValue,
            cciEntryValue = cciEntryValue,
            reason = reason,
            timeframe = timeframe
        )
    }
}

// ===== 시간대별 설정 =====

/**
 * 시간대별 체크 간격 설정
 */
enum class TimeframeInterval(val timeframe: String, val defaultInterval: Int) {
    M15("15m", 300),    // 5분 간격
    H1("1h", 600),      // 10분 간격
    H4("4h", 1800),     // 30분 간격
    D1("1d", 3600);     // 1시간 간격

    companion object {
        fun fromTimeframe(timeframe: String): TimeframeInterval {
            return values().find { it.timeframe == timeframe } ?: M15
        }
    }
}

// ===== 시그널 통계 =====

/**
 * 시그널 통계 데이터
 */
data class SignalStatistics(
    val totalSignals: Int,
    val longSignals: Int,
    val shortSignals: Int,
    val successRate: Double,
    val averageProfit: Double,
    val maxProfit: Double,
    val maxLoss: Double,
    val activeSignals: Int
) {
    /**
     * 롱/숏 비율 계산
     */
    fun getLongShortRatio(): String {
        if (totalSignals == 0) return "0:0"
        return "$longSignals:$shortSignals"
    }

    /**
     * 성공률 텍스트
     */
    fun getSuccessRateText(): String {
        return "${String.format("%.1f", successRate * 100)}%"
    }
}

// ===== 모니터링 상태 =====

/**
 * 실시간 모니터링 상태
 */
data class MonitoringStatus(
    val configId: String,
    val symbol: String,
    val isActive: Boolean,
    val lastCheckTime: Long,
    val currentCci: Double,
    val currentState: CciSignalState,
    val errorCount: Int = 0,
    val lastError: String? = null
) {
    /**
     * 모니터링이 정상 상태인지 확인
     */
    fun isHealthy(): Boolean {
        val timeSinceLastCheck = System.currentTimeMillis() - lastCheckTime
        return isActive && errorCount < 5 && timeSinceLastCheck < 600000 // 10분 이내
    }

    /**
     * 상태 요약 텍스트
     */
    fun getStatusSummary(): String {
        val healthStatus = if (isHealthy()) "정상" else "비정상"
        return "$symbol: $healthStatus (CCI: ${String.format("%.2f", currentCci)}, 상태: ${currentState.name})"
    }
}

// ===== 설정 검증 결과 =====

/**
 * 설정 검증 결과
 */
data class ConfigValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
) {
    /**
     * 오류가 있는지 확인
     */
    fun hasErrors(): Boolean = errors.isNotEmpty()

    /**
     * 경고가 있는지 확인
     */
    fun hasWarnings(): Boolean = warnings.isNotEmpty()

    /**
     * 모든 메시지를 하나의 문자열로 결합
     */
    fun getAllMessages(): String {
        val allMessages = mutableListOf<String>()
        if (errors.isNotEmpty()) {
            allMessages.add("오류: ${errors.joinToString(", ")}")
        }
        if (warnings.isNotEmpty()) {
            allMessages.add("경고: ${warnings.joinToString(", ")}")
        }
        return allMessages.joinToString("\n")
    }
}