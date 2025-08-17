// MarketSignalModels.kt - 시세포착 관련 데이터 모델

package com.example.ver20.dao

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.*

// 시세포착 설정 데이터
data class MarketSignalConfig(
    val id: String = UUID.randomUUID().toString(),
    val username: String,
    val signalType: String, // "CCI", "RSI", "MA"
    val symbol: String,
    val timeframe: String, // "15m", "1h", "4h"
    val checkInterval: Int, // 진입체크 인터벌 (초)
    val isActive: Boolean = true,
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
    
    // CCI 전용 설정
    val cciPeriod: Int = 20,
    val cciBreakoutValue: Double = 100.0, // 돌파값
    val cciEntryValue: Double = 90.0, // 진입값
    val seedMoney: Double = 1000.0 // 진입시드
)

// 시세포착 신호 데이터
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
)

// CCI 계산 결과
data class CciValue(
    val timestamp: Long,
    val price: Double,
    val volume: Double,
    val cciValue: Double
)

// 시세 데이터 (K-line)
data class KlineData(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
) {
    val typical: Double get() = (high + low + close) / 3.0
}

// 포착 조건 체크 결과
data class SignalCheckResult(
    val hasSignal: Boolean,
    val direction: String? = null,
    val reason: String = "",
    val cciValue: Double = 0.0,
    val price: Double = 0.0,
    val volume: Double = 0.0
)

// API 응답 데이터
data class MarketSignalApiResponse(
    val success: Boolean,
    val data: Any? = null,
    val message: String? = null
)

// MongoDB 저장용 시세포착 설정
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
)

// MongoDB 저장용 시세포착 신호
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
)