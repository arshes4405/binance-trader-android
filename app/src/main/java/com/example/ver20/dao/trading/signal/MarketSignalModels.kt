// MarketSignalModels.kt - 시세포착 관련 데이터 모델 (앱용 간소화 버전)

package com.example.ver20.dao.trading.signal

import java.text.SimpleDateFormat
import java.util.*

// ===== 시세포착 설정 데이터 =====

/**
 * 시세포착 설정 데이터 클래스 (UI 및 서버 통신용)
 */
data class MarketSignalConfig(
    val id: String = UUID.randomUUID().toString(),
    val username: String,
    val signalType: String, // "CCI", "RSI", "MA"
    val symbol: String,
    val timeframe: String, // "15m", "1h", "4h", "1d"
    val checkInterval: Int, // 진입체크 인터벌 (분 단위)
    val isActive: Boolean = true,
    val autoTrading: Boolean = false, // 자동매매 활성화 여부
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

    /**
     * 설정 요약 정보
     */
    fun getSummary(): String {
        return "$signalType • $symbol • $timeframe (${checkInterval}분)"
    }

    /**
     * 자동매매 상태 텍스트
     */
    fun getAutoTradingStatusText(): String {
        return if (autoTrading) "자동매매 ON" else "수동 확인"
    }
}

// ===== 시세포착 신호 데이터 =====

/**
 * 시세포착 신호 데이터 클래스 (UI 표시용)
 */
data class MarketSignal(
    val id: String = UUID.randomUUID().toString(),
    val configId: String,
    val username: String,
    val symbol: String,
    val signalType: String,
    val direction: String, // "LONG", "SHORT"
    val timestamp: Long = System.currentTimeMillis(),
    val price: Double,
    val volume: Double,

    // CCI 관련 정보
    val cciValue: Double,
    val cciBreakoutValue: Double,
    val cciEntryValue: Double,
    val reason: String = "",
    val timeframe: String = "",
    val status: String = "ACTIVE",
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
        return "${getDirectionIcon()} $symbol $direction @ ${String.format("%.2f", price)}"
    }

    /**
     * 타임스탬프를 포맷된 문자열로 변환
     */
    fun getFormattedTime(): String {
        return SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    /**
     * 상세 타임스탬프
     */
    fun getDetailedTime(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    }

    /**
     * 신호 신뢰도 (CCI 값 기준)
     */
    fun getConfidenceLevel(): String {
        val strength = kotlin.math.abs(cciValue) / cciBreakoutValue
        return when {
            strength >= 2.0 -> "매우 강함"
            strength >= 1.5 -> "강함"
            strength >= 1.0 -> "보통"
            else -> "약함"
        }
    }
}

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