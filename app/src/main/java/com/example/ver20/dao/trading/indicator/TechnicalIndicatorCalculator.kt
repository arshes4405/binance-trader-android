// TechnicalIndicatorCalculator.kt - 수정된 기술적 지표 계산 DAO

package com.example.ver20.dao.trading.indicator

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.math.abs

class TechnicalIndicatorCalculator {

    companion object {
        private const val TAG = "TechnicalIndicatorCalculator"
        private const val BINANCE_API_BASE = "https://api.binance.com/api/v3/klines"
        private const val DEFAULT_LIMIT = 1000
    }

    private val httpClient = OkHttpClient()

    suspend fun calculateIndicators(
        symbol: String,
        timeframe: String,
        cciPeriod: Int = 20,
        rsiPeriod: Int = 7,
        includeInProgress: Boolean = true  // 기본값을 true로 변경 (바이낸스와 동일)
    ): IndicatorResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "지표 계산 시작: $symbol $timeframe (진행중 캔들 포함: $includeInProgress)")

            // 1. 바이낸스에서 OHLCV 데이터 수집
            val candleData = fetchCandleData(symbol, timeframe, includeInProgress)

            if (candleData.isEmpty()) {
                throw Exception("OHLCV 데이터 수집 실패")
            }

            // 2. CCI 계산 (수정됨)
            val cciValue = calculateCCI(candleData, cciPeriod)

            // 3. RSI 계산 (수정됨)
            val rsiValue = calculateRSI(candleData.map { it.close }, rsiPeriod)

            // 4. 결과 반환
            val result = IndicatorResult(
                symbol = symbol,
                timeframe = timeframe,
                timestamp = System.currentTimeMillis(),
                cci = cciValue,
                rsi = rsiValue,
                currentPrice = candleData.lastOrNull()?.close ?: 0.0,
                volume = candleData.lastOrNull()?.volume ?: 0.0
            )

            Log.d(TAG, "✅ $symbol $timeframe 계산 완료: CCI=${String.format("%.2f", cciValue)}, RSI=${String.format("%.2f", rsiValue)} (데이터: ${candleData.size}개)")
            result

        } catch (e: Exception) {
            Log.e(TAG, "지표 계산 실패: $symbol $timeframe - ${e.message}")
            throw e
        }
    }

    suspend fun calculateMultiTimeframeIndicators(
        symbol: String,
        timeframes: List<String> = listOf("15m", "1h", "4h", "1d"),
        cciPeriod: Int = 20,
        rsiPeriod: Int = 7,
        includeInProgress: Boolean = true  // 기본값을 true로 변경 (바이낸스와 동일)
    ): Map<String, IndicatorResult> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, IndicatorResult>()

        timeframes.forEach { timeframe ->
            try {
                val result = calculateIndicators(symbol, timeframe, cciPeriod, rsiPeriod, includeInProgress)
                results[timeframe] = result
            } catch (e: Exception) {
                Log.e(TAG, "시간대 $timeframe 계산 실패: ${e.message}")
            }
        }

        results
    }

    private suspend fun fetchCandleData(
        symbol: String,
        timeframe: String,
        includeInProgress: Boolean = true  // 기본값을 true로 변경
    ): List<CandleData> = withContext(Dispatchers.IO) {
        // RSI 계산을 위해 충분한 데이터 필요
        val limit = when (timeframe) {
            "15m" -> 500
            "1h" -> 200
            "4h" -> 200  // 4시간봉도 200개로 증가
            "1d" -> 200  // 1일봉도 200개로 증가
            else -> DEFAULT_LIMIT
        }

        val url = "$BINANCE_API_BASE?symbol=$symbol&interval=$timeframe&limit=$limit"
        val request = Request.Builder().url(url).build()

        val response = httpClient.newCall(request).execute()
        val jsonString = response.body?.string() ?: ""

        val allCandleData = parseKlineResponse(jsonString)

        // 바이낸스와 동일하게 동작: 기본적으로 모든 캔들 포함
        val finalCandles = if (!includeInProgress) {
            // includeInProgress가 명시적으로 false일 때만 마지막 캔들 제외
            val completedCandles = allCandleData.dropLast(1)
            Log.d(TAG, "$symbol $timeframe - 완성된 캔들만 사용: ${completedCandles.size}개")
            completedCandles
        } else {
            // 기본값: 모든 캔들 사용 (진행 중인 캔들 포함)
            Log.d(TAG, "$symbol $timeframe - 모든 캔들 사용 (진행 중 포함): ${allCandleData.size}개")
            allCandleData
        }

        // 디버깅 로그
        if (finalCandles.isNotEmpty()) {
            val dateFormat = java.text.SimpleDateFormat("MM-dd HH:mm:ss").apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }

            val lastCandle = finalCandles.last()
            val currentTime = System.currentTimeMillis()

            Log.d(TAG, "현재 시간: ${dateFormat.format(java.util.Date(currentTime))} UTC")
            Log.d(TAG, "마지막 사용 캔들: ${dateFormat.format(java.util.Date(lastCandle.timestamp))} UTC - Close: ${lastCandle.close}")

            // 진행 중인 캔들인지 확인
            val candleEndTime = when (timeframe) {
                "15m" -> lastCandle.timestamp + 15 * 60 * 1000L
                "1h" -> lastCandle.timestamp + 60 * 60 * 1000L
                "4h" -> lastCandle.timestamp + 4 * 60 * 60 * 1000L
                "1d" -> lastCandle.timestamp + 24 * 60 * 60 * 1000L
                else -> lastCandle.timestamp + 60 * 60 * 1000L
            }

            if (candleEndTime > currentTime) {
                Log.d(TAG, "⚠️ 현재 진행 중인 캔들 포함됨 (종료 예정: ${dateFormat.format(java.util.Date(candleEndTime))} UTC)")
            } else {
                Log.d(TAG, "✓ 완성된 캔들")
            }

            // 최근 5개 캔들의 종가 표시 (디버깅용)
            val last5Candles = finalCandles.takeLast(5)
            Log.d(TAG, "최근 5개 캔들 종가: ${last5Candles.joinToString(", ") { "%.2f".format(it.close) }}")
        }

        finalCandles
    }

    private fun parseKlineResponse(jsonString: String): List<CandleData> {
        return try {
            if (jsonString.isBlank()) return emptyList()

            val candles = mutableListOf<CandleData>()

            val cleanJson = jsonString.trim()
            if (cleanJson.startsWith("[") && cleanJson.endsWith("]")) {
                val jsonContent = cleanJson.substring(1, cleanJson.length - 1)
                val candleArrays = jsonContent.split("],[")

                for (candleStr in candleArrays) {
                    val cleanCandleStr = candleStr.replace("[", "").replace("]", "")
                    val values = cleanCandleStr.split(",")

                    if (values.size >= 6) {
                        try {
                            val timestamp = values[0].replace("\"", "").toLongOrNull() ?: continue
                            val open = values[1].replace("\"", "").toDoubleOrNull() ?: continue
                            val high = values[2].replace("\"", "").toDoubleOrNull() ?: continue
                            val low = values[3].replace("\"", "").toDoubleOrNull() ?: continue
                            val close = values[4].replace("\"", "").toDoubleOrNull() ?: continue
                            val volume = values[5].replace("\"", "").toDoubleOrNull() ?: continue

                            candles.add(CandleData(timestamp, open, high, low, close, volume))
                        } catch (e: Exception) {
                            continue
                        }
                    }
                }
            }

            candles
        } catch (e: Exception) {
            Log.e(TAG, "K-line 파싱 오류: ${e.message}")
            emptyList()
        }
    }

    /**
     * CCI 계산 - 수정된 버전
     * TradingView와 동일한 방식
     */
    private fun calculateCCI(candleData: List<CandleData>, period: Int): Double {
        if (candleData.size < period) {
            Log.w(TAG, "CCI 계산용 데이터 부족: ${candleData.size} < $period")
            return 0.0
        }

        // 최근 period 개의 캔들만 사용
        val recentCandles = candleData.takeLast(period)

        // Typical Price 계산
        val typicalPrices = recentCandles.map { (it.high + it.low + it.close) / 3.0 }

        // SMA (Simple Moving Average)
        val sma = typicalPrices.average()

        // Mean Absolute Deviation
        val meanDeviation = typicalPrices.map { abs(it - sma) }.average()

        // 현재 TP는 period 내의 마지막 캔들 사용 (수정됨)
        val currentTP = typicalPrices.last()

        // CCI 계산
        return if (meanDeviation != 0.0) {
            (currentTP - sma) / (0.015 * meanDeviation)
        } else {
            0.0
        }
    }

    /**
     * RSI 계산 - TradingView 방식으로 수정
     * RMA (Running Moving Average) 사용
     */
    private fun calculateRSI(prices: List<Double>, period: Int): Double {
        if (prices.size < period + 1) {
            Log.w(TAG, "RSI 계산용 데이터 부족: ${prices.size} < ${period + 1}")
            return 50.0
        }

        // 가격 변화량 계산
        val changes = mutableListOf<Double>()
        for (i in 1 until prices.size) {
            changes.add(prices[i] - prices[i-1])
        }

        if (changes.size < period) {
            return 50.0
        }

        // TradingView 방식: RMA (Running Moving Average) 사용
        var avgGain = 0.0
        var avgLoss = 0.0

        // 첫 번째 RMA 값 초기화 (SMA로 시작)
        for (i in 0 until period) {
            val change = changes[i]
            if (change > 0) {
                avgGain += change
            } else if (change < 0) {
                avgLoss += -change
            }
        }

        avgGain = avgGain / period
        avgLoss = avgLoss / period

        // RMA 계산 (TradingView pine script의 rma 함수와 동일)
        // alpha = 1 / period
        val alpha = 1.0 / period

        for (i in period until changes.size) {
            val change = changes[i]
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) -change else 0.0

            // RMA 공식: RMA = alpha * value + (1 - alpha) * prev_RMA
            avgGain = alpha * gain + (1 - alpha) * avgGain
            avgLoss = alpha * loss + (1 - alpha) * avgLoss
        }

        // RSI 계산
        val rsi = if (avgLoss == 0.0) {
            100.0
        } else {
            val rs = avgGain / avgLoss
            100.0 - (100.0 / (1.0 + rs))
        }

        Log.d(TAG, "RSI 계산 완료: ${String.format("%.2f", rsi)} (데이터: ${prices.size}개)")

        return rsi
    }
}

// 데이터 클래스
data class CandleData(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

data class IndicatorResult(
    val symbol: String,
    val timeframe: String,
    val timestamp: Long,
    val cci: Double,
    val rsi: Double,
    val currentPrice: Double,
    val volume: Double
)