// 개선된 TradeHistoryDetailScreen.kt - 실제 백테스트 데이터만 표시

package com.example.ver20.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import com.example.ver20.dao.CciBacktestResult
import com.example.ver20.dao.TradeResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeHistoryDetailScreen(
    backtestResult: CciBacktestResult,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 실제 백테스트 데이터만 필터링
    val validTrades = filterValidTrades(backtestResult.trades)
    val filteredResult = backtestResult.copy(trades = validTrades)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "실제 백테스트 거래내역",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "뒤로 가기",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2196F3)
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 실제 백테스트 결과 요약
            item {
                EnhancedBacktestSummaryCard(filteredResult)
            }

            // 데이터 검증 결과 표시
            item {
                DataValidationCard(backtestResult.trades.size, validTrades.size)
            }

            // 개별 거래 내역
            if (validTrades.isNotEmpty()) {
                // 시간순 정렬 (과거부터)
                val sortedTrades = validTrades.sortedBy { trade ->
                    try {
                        val format = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                        format.timeZone = TimeZone.getTimeZone("UTC")
                        format.parse(trade.timestamp)?.time ?: 0L
                    } catch (e: Exception) {
                        0L
                    }
                }

                items(sortedTrades.size) { index ->
                    EnhancedTradeCard(
                        trade = sortedTrades[index],
                        tradeNumber = index + 1
                    )
                }
            } else {
                item {
                    NoValidTradesCard()
                }
            }
        }
    }
}

// 실제 백테스트 데이터만 필터링
private fun filterValidTrades(trades: List<TradeResult>): List<TradeResult> {
    return trades.filter { trade ->
        // 1. CCI 값이 실제로 존재하는지 확인
        val hasCCIData = trade.entryCCI != 0.0 || trade.previousCCI != 0.0

        // 2. CCI 진입 조건이 실제로 맞는지 검증
        val isValidCCIEntry = when (trade.type) {
            "LONG" -> {
                // 롱: 이전 CCI < -110 && 진입 CCI >= -100
                trade.previousCCI < -110 && trade.entryCCI >= -100
            }
            "SHORT" -> {
                // 숏: 이전 CCI > +110 && 진입 CCI <= +100
                trade.previousCCI > 110 && trade.entryCCI <= 100
            }
            else -> false
        }

        // 3. 거래 금액이 유효한지 확인
        val hasValidAmount = trade.amount > 0

        // 4. 진입가와 청산가가 유효한지 확인
        val hasValidPrices = trade.entryPrice > 0 && trade.exitPrice > 0

        // 5. 타임스탬프가 유효한지 확인
        val hasValidTimestamp = trade.timestamp.isNotEmpty() && trade.timestamp != "Invalid Date"

        // 모든 조건을 만족하는 거래만 유효한 것으로 간주
        hasCCIData && isValidCCIEntry && hasValidAmount && hasValidPrices && hasValidTimestamp
    }
}

@Composable
fun EnhancedBacktestSummaryCard(result: CciBacktestResult) {
    val formatter = DecimalFormat("#,##0.00")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F5E8)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "📊 실제 백테스트 결과 요약",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "검증된 ${result.trades.size}개 거래 (CCI 조건 만족)",
                fontSize = 12.sp,
                color = Color(0xFF666666),
                fontStyle = FontStyle.Italic
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 주요 지표들
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryBackItem("총 수익률", "+${formatter.format((result.finalSeedMoney/10000 - 1) * 100)}%", Color(0xFF4CAF50))
                SummaryBackItem("승률", "${formatter.format(result.winRate)}%", Color(0xFF2196F3))
                SummaryBackItem("최대 손실", "${formatter.format(result.maxDrawdown)}%", Color(0xFFF44336))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 상세 지표들
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryBackItem("총 수익", "${formatter.format(result.totalProfit)}", Color(0xFF4CAF50))
                SummaryBackItem("총 수수료", "${formatter.format(result.totalFees)}", Color(0xFFF44336))
                SummaryBackItem("수익 팩터", formatter.format(result.profitFactor), Color(0xFFFF9800))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 실제 데이터 검증 표시
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE3F2FD)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = null,
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "실제 바이낸스 데이터 + CCI 조건 검증 완료",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1976D2)
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryBackItem(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            fontSize = 10.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun DataValidationCard(originalCount: Int, validCount: Int) {
    val filterRate = if (originalCount > 0) (validCount.toDouble() / originalCount * 100) else 0.0
    val filteredCount = originalCount - validCount

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (filteredCount > 0) Color(0xFFFFF3E0) else Color(0xFFE8F5E8)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (filteredCount > 0) Icons.Default.FilterAlt else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (filteredCount > 0) Color(0xFFFF9800) else Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "🔍 데이터 검증 결과",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (filteredCount > 0) Color(0xFFE65100) else Color(0xFF2E7D32)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "원본 거래 수: ${originalCount}개",
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
                Text(
                    "검증된 거래: ${validCount}개",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4CAF50)
                )
            }

            if (filteredCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "⚠️ 필터링된 거래: ${filteredCount}개 (CCI 조건 불만족 또는 잘못된 데이터)",
                    fontSize = 11.sp,
                    color = Color(0xFFE65100),
                    fontStyle = FontStyle.Italic
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "유효 데이터 비율: ${DecimalFormat("#.#").format(filterRate)}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (filterRate >= 90) Color(0xFF4CAF50) else Color(0xFFFF9800)
            )
        }
    }
}

@Composable
fun EnhancedTradeCard(trade: TradeResult, tradeNumber: Int) {
    val formatter = DecimalFormat("#,##0.00")
    val cciFormatter = DecimalFormat("#,##0.0")
    val coinFormatter = DecimalFormat("#,##0.######")

    val isProfit = trade.profit >= 0
    val profitColor = if (isProfit) Color(0xFF4CAF50) else Color(0xFFF44336)

    // 코인 수량 계산
    val coinQuantity = trade.amount / trade.entryPrice

    // 한국시간 변환
    val koreanTime = convertToKoreanTime(trade.timestamp)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isProfit) Color(0xFFE8F5E8) else Color(0xFFFFEBEE)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 거래 헤더 (검증 상태 포함)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (trade.type == "LONG") Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (trade.type == "LONG") Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = "검증된 거래",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "거래 #$tradeNumber (${trade.type})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF424242)
                        )
                        Text(
                            getEnhancedTradeTypeDescription(trade),
                            fontSize = 10.sp,
                            color = Color(0xFF9E9E9E),
                            fontStyle = FontStyle.Italic
                        )
                    }
                }

                Text(
                    "${if (isProfit) "+" else ""}${formatter.format(trade.profit)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = profitColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 검증된 CCI 분석
            VerifiedCCIAnalysisCard(trade, cciFormatter)

            Spacer(modifier = Modifier.height(12.dp))

            // 거래 정보
            EnhancedTradeInfoSection(trade, formatter, coinFormatter)

            Spacer(modifier = Modifier.height(8.dp))

            // 시간 정보
            Text(
                "거래 시간: $koreanTime (KST)",
                fontSize = 10.sp,
                color = Color(0xFF999999)
            )

            // 수익률
            val profitRate = ((trade.exitPrice - trade.entryPrice) / trade.entryPrice * 100).let {
                if (trade.type == "SHORT") -it else it
            }

            Text(
                "수익률: ${if (profitRate >= 0) "+" else ""}${formatter.format(profitRate)}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (profitRate >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    }
}

@Composable
fun VerifiedCCIAnalysisCard(trade: TradeResult, cciFormatter: DecimalFormat) {
    val isValidEntry = when (trade.type) {
        "LONG" -> trade.previousCCI < -110 && trade.entryCCI >= -100
        "SHORT" -> trade.previousCCI > 110 && trade.entryCCI <= 100
        else -> false
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isValidEntry) Color(0xFFE8F5E8) else Color(0xFFFFEBEE)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isValidEntry) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (isValidEntry) Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "🎯 검증된 CCI 진입 분석",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // CCI 값 표시
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "이전 CCI: ${cciFormatter.format(trade.previousCCI)}",
                    fontSize = 11.sp,
                    color = Color(0xFF666666)
                )
                Text(
                    "진입 CCI: ${cciFormatter.format(trade.entryCCI)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (trade.type == "LONG") Color(0xFF2196F3) else Color(0xFFFF5722)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 조건 분석
            if (trade.type == "LONG") {
                Text(
                    "롱 조건: CCI < -110 → CCI ≥ -100",
                    fontSize = 10.sp,
                    color = Color(0xFF666666)
                )

                Text(
                    "조건 충족: ${if (isValidEntry) "✅ 성공" else "❌ 실패"}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isValidEntry) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            } else {
                Text(
                    "숏 조건: CCI > +110 → CCI ≤ +100",
                    fontSize = 10.sp,
                    color = Color(0xFF666666)
                )

                Text(
                    "조건 충족: ${if (isValidEntry) "✅ 성공" else "❌ 실패"}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isValidEntry) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }

            if (isValidEntry) {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE3F2FD)
                    )
                ) {
                    Text(
                        "✅ 실제 백테스트 전략 조건에 완전히 부합하는 거래",
                        fontSize = 9.sp,
                        color = Color(0xFF1976D2),
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedTradeInfoSection(trade: TradeResult, formatter: DecimalFormat, coinFormatter: DecimalFormat) {
    val coinQuantity = trade.amount / trade.entryPrice

    // 가격 정보
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "진입가: ${formatter.format(trade.entryPrice)}",
            fontSize = 12.sp,
            color = Color(0xFF666666)
        )
        Text(
            "청산가: ${formatter.format(trade.exitPrice)}",
            fontSize = 12.sp,
            color = Color(0xFF666666)
        )
    }

    Spacer(modifier = Modifier.height(4.dp))

    // 수량 정보
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "코인 수량: ${coinFormatter.format(coinQuantity)}",
            fontSize = 12.sp,
            color = Color(0xFF666666)
        )
        Text(
            "거래금액: ${formatter.format(trade.amount)}",
            fontSize = 12.sp,
            color = Color(0xFF666666)
        )
    }

    Spacer(modifier = Modifier.height(4.dp))

    // 수수료 및 순수익
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "수수료: ${formatter.format(trade.fee)}",
            fontSize = 12.sp,
            color = Color(0xFF666666)
        )
        Text(
            "청산 이유: ${getExitReasonText(trade.exitReason)}",
            fontSize = 12.sp,
            color = Color(0xFF666666)
        )
    }
}

@Composable
fun NoValidTradesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color(0xFFF44336)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "검증된 거래 내역이 없습니다",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD32F2F)
            )

            Text(
                "CCI 조건을 만족하는 실제 백테스트 거래가 발견되지 않았습니다.\n" +
                        "백테스트 설정이나 기간을 조정해보세요.",
                fontSize = 14.sp,
                color = Color(0xFFBF360C),
                textAlign = TextAlign.Center
            )
        }
    }
}

// 유틸리티 함수들 (개선된 버전)
fun getEnhancedTradeTypeDescription(trade: TradeResult): String {
    return when {
        trade.exitReason == "HALF_SELL" -> "물타기 후 부분청산 (검증됨)"
        trade.exitReason == "FULL_EXIT" -> "물타기 후 완전청산 (검증됨)"
        trade.exitReason == "PROFIT" && trade.amount >= 3000 -> "물타기 완료 후 익절 (검증됨)"
        trade.exitReason == "PROFIT" -> "단일 포지션 익절 (검증됨)"
        trade.exitReason == "STOP_LOSS" -> "손절 청산 (검증됨)"
        trade.exitReason == "FORCE_CLOSE" -> "강제 청산 (검증됨)"
        else -> "실제 백테스트 거래"
    }
}

fun getExitReasonText(exitReason: String): String {
    return when (exitReason) {
        "PROFIT" -> "익절"
        "HALF_SELL" -> "부분매도"
        "FULL_EXIT" -> "완전청산"
        "STOP_LOSS" -> "손절"
        "FORCE_CLOSE" -> "강제청산"
        else -> exitReason
    }
}

// 한국시간 변환 함수 (기존과 동일)
fun convertToKoreanTime(timestamp: String): String {
    return try {
        val inputFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MM월 dd일 HH:mm", Locale.getDefault())

        // UTC → KST 변환
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val utcDate = inputFormat.parse(timestamp)

        outputFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")

        if (utcDate != null) {
            outputFormat.format(utcDate)
        } else {
            addNineHours(timestamp)
        }
    } catch (e: Exception) {
        addNineHours(timestamp)
    }
}

// 수동으로 9시간 추가 (기존과 동일)
private fun addNineHours(timestamp: String): String {
    return try {
        val parts = timestamp.split(" ")
        if (parts.size == 2) {
            val datePart = parts[0] // MM-dd
            val timePart = parts[1] // HH:mm
            val timeComponents = timePart.split(":")

            if (timeComponents.size == 2) {
                val hour = timeComponents[0].toInt()
                val minute = timeComponents[1].toInt()

                val newHour = (hour + 9) % 24
                val dayChange = if (hour + 9 >= 24) 1 else 0

                if (dayChange > 0) {
                    val monthDay = datePart.split("-")
                    if (monthDay.size == 2) {
                        val month = monthDay[0].toInt()
                        val day = monthDay[1].toInt() + 1
                        "${month}월 ${day}일 ${String.format("%02d:%02d", newHour, minute)}"
                    } else {
                        "${datePart}(+1일) ${String.format("%02d:%02d", newHour, minute)}"
                    }
                } else {
                    val monthDay = datePart.split("-")
                    if (monthDay.size == 2) {
                        val month = monthDay[0].toInt()
                        val day = monthDay[1].toInt()
                        "${month}월 ${day}일 ${String.format("%02d:%02d", newHour, minute)}"
                    } else {
                        "$datePart ${String.format("%02d:%02d", newHour, minute)}"
                    }
                }
            } else {
                "$timestamp (변환실패)"
            }
        } else {
            "$timestamp (형식오류)"
        }
    } catch (e: Exception) {
        "$timestamp (KST변환실패)"
    }
}