// RealCciPositionHistoryScreen.kt - 실제 CCI 포지션 내역 화면

package com.example.ver20.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat
import com.example.ver20.dao.RealCciBacktestResult
import com.example.ver20.dao.CciPositionResult
import com.example.ver20.dao.CciTradeExecution

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealCciPositionHistoryScreen(
    backtestResult: RealCciBacktestResult,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedPositionId by remember { mutableStateOf<Int?>(null) }
    var selectedFilter by remember { mutableStateOf("ALL") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "📊 실제 CCI 포지션 내역",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
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
            // 🎯 전체 요약 카드
            item {
                RealCciOverviewCard(backtestResult)
            }

            // 📊 포지션 필터
            item {
                PositionFilterCard(selectedFilter) { filter ->
                    selectedFilter = filter
                }
            }

            // 📈 개별 포지션 내역들
            if (backtestResult.positions.isNotEmpty()) {
                val filteredPositions = when (selectedFilter) {
                    "PROFIT" -> backtestResult.positions.filter { it.totalProfit > 0 }
                    "LOSS" -> backtestResult.positions.filter { it.totalProfit <= 0 }
                    "LONG" -> backtestResult.positions.filter { it.type == "LONG" }
                    "SHORT" -> backtestResult.positions.filter { it.type == "SHORT" }
                    "STAGE1" -> backtestResult.positions.filter { it.maxStage == 1 }
                    "STAGE2+" -> backtestResult.positions.filter { it.maxStage >= 2 }
                    else -> backtestResult.positions
                }

                items(filteredPositions) { position ->
                    RealCciPositionCard(
                        position = position,
                        isExpanded = expandedPositionId == position.positionId,
                        onToggleExpand = {
                            expandedPositionId = if (expandedPositionId == position.positionId) null else position.positionId
                        }
                    )
                }
            } else {
                item {
                    NoPositionsCard()
                }
            }

            // 하단 여백
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun OverviewMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            fontSize = 10.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Text(
            value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun RealCciOverviewCard(result: RealCciBacktestResult) {
    val formatter = DecimalFormat("#,##0.00")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Analytics,
                    contentDescription = null,
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "📊 실제 CCI 물타기 전략 요약",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 주요 지표들
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OverviewMetric(
                    icon = Icons.Default.TrendingUp,
                    label = "총 포지션",
                    value = "${result.totalPositions}개",
                    color = Color(0xFF4CAF50)
                )
                OverviewMetric(
                    icon = Icons.Default.Layers,
                    label = "최대 단계",
                    value = "${result.maxStageReached}단계",
                    color = Color(0xFF2196F3)
                )
                OverviewMetric(
                    icon = Icons.Default.Timeline,
                    label = "평균 보유",
                    value = "${formatter.format(result.avgHoldingTime)}h",
                    color = Color(0xFFFF9800)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OverviewMetric(
                    icon = Icons.Default.AttachMoney,
                    label = "총 수익",
                    value = "${formatter.format(result.totalProfit)}",
                    color = if (result.totalProfit >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
                OverviewMetric(
                    icon = Icons.Default.Percent,
                    label = "승률",
                    value = "${formatter.format(result.winRate)}%",
                    color = Color(0xFF9C27B0)
                )
                OverviewMetric(
                    icon = Icons.Default.TrendingDown,
                    label = "최대 손실",
                    value = "${formatter.format(result.maxDrawdown)}%",
                    color = Color(0xFFF44336)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 전략 설정 표시
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E8)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "CCI길이: ${result.settings.cciLength} | 시간: ${result.settings.timeframe} | " +
                                "코인: ${result.settings.symbol.replace("USDT", "")} | " +
                                "시드: ${DecimalFormat("#,###").format(result.settings.seedMoney)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }
    }
}

@Composable
fun PositionFilterCard(
    selectedFilter: String,
    onFilterChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "📋 포지션 필터",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF424242)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 첫 번째 행
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    text = "전체",
                    isSelected = selectedFilter == "ALL",
                    onClick = { onFilterChange("ALL") }
                )
                FilterChip(
                    text = "수익",
                    isSelected = selectedFilter == "PROFIT",
                    onClick = { onFilterChange("PROFIT") }
                )
                FilterChip(
                    text = "손실",
                    isSelected = selectedFilter == "LOSS",
                    onClick = { onFilterChange("LOSS") }
                )
                FilterChip(
                    text = "롱",
                    isSelected = selectedFilter == "LONG",
                    onClick = { onFilterChange("LONG") }
                )
                FilterChip(
                    text = "숏",
                    isSelected = selectedFilter == "SHORT",
                    onClick = { onFilterChange("SHORT") }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 두 번째 행
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    text = "1단계",
                    isSelected = selectedFilter == "STAGE1",
                    onClick = { onFilterChange("STAGE1") }
                )
                FilterChip(
                    text = "2단계+",
                    isSelected = selectedFilter == "STAGE2+",
                    onClick = { onFilterChange("STAGE2+") }
                )
            }
        }
    }
}

@Composable
fun FilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .background(
                color = if (isSelected) Color(0xFF2196F3) else Color.White,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) Color(0xFF2196F3) else Color(0xFFCCCCCC),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = if (isSelected) Color.White else Color(0xFF666666),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}


// 숏 매도(진입) 상세 카드
@Composable
fun CciShortSellDetailCard(sell: CciTradeExecution, formatter: DecimalFormat) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 매도 진입 아이콘
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = Color(0xFFF44336),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "🔴 숏 매도 진입",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                    Text(
                        formatTimestamp(sell.timestamp),
                        fontSize = 10.sp,
                        color = Color(0xFF666666)
                    )
                }

                Text(
                    "가격: ${formatter.format(sell.entryPrice)} | 금액: ${formatter.format(sell.amount)}",
                    fontSize = 11.sp,
                    color = Color(0xFF424242)
                )

                Text(
                    "코인: ${DecimalFormat("#,##0.######").format(sell.coins)} | 수수료: ${formatter.format(sell.fees)}",
                    fontSize = 11.sp,
                    color = Color(0xFF666666)
                )

                Text(
                    "이유: ${sell.reason}",
                    fontSize = 10.sp,
                    color = Color(0xFF666666)
                )

                Text(
                    "진입CCI: ${DecimalFormat("#,##0.0").format(sell.entryCCI)}",
                    fontSize = 10.sp,
                    color = Color(0xFFF44336),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// 숏 매수(청산) 상세 카드
@Composable
fun CciShortBuyDetailCard(buy: CciTradeExecution, formatter: DecimalFormat) {
    val profitColor = if (buy.profitRate >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (buy.profitRate >= 0) Color(0xFFE8F5E8) else Color(0xFFFFEBEE)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 청산 아이콘
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = profitColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (buy.type.contains("PROFIT")) Icons.Default.TrendingUp else Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "🟢 ${getShortExitTypeText(buy.type)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = profitColor
                    )
                    Text(
                        formatTimestamp(buy.timestamp),
                        fontSize = 10.sp,
                        color = Color(0xFF666666)
                    )
                }

                Text(
                    "가격: ${formatter.format(buy.exitPrice ?: 0.0)} | 금액: ${formatter.format(buy.amount)}",
                    fontSize = 11.sp,
                    color = Color(0xFF424242)
                )

                Text(
                    "코인: ${DecimalFormat("#,##0.######").format(buy.coins)} | 수수료: ${formatter.format(buy.fees)}",
                    fontSize = 11.sp,
                    color = Color(0xFF666666)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "수익률: ${if (buy.profitRate >= 0) "+" else ""}${formatter.format(buy.profitRate)}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = profitColor
                    )
                    Text(
                        "CCI: ${DecimalFormat("#,##0.0").format(buy.exitCCI)}",
                        fontSize = 10.sp,
                        color = Color(0xFF666666)
                    )
                }

                Text(
                    "이유: ${buy.reason}",
                    fontSize = 10.sp,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}

@Composable
fun RealCciPositionCard(
    position: CciPositionResult,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val formatter = DecimalFormat("#,##0.00")
    val isProfit = position.totalProfit >= 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() },
        colors = CardDefaults.cardColors(
            containerColor = if (isProfit) Color(0xFFE8F5E8) else Color(0xFFFFEBEE)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 포지션 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (position.type == "LONG") Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (position.type == "LONG") Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "${position.symbol.replace("USDT", "")} ${position.type} #${position.positionId}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF424242)
                        )
                        Text(
                            "최대 ${position.maxStage}단계 | ${position.duration}",
                            fontSize = 11.sp,
                            color = Color(0xFF666666)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Color(0xFF666666),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        "${if (isProfit) "+" else ""}${formatter.format(position.totalProfit)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isProfit) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                    Text(
                        "수수료: ${formatter.format(position.totalFees)}",
                        fontSize = 10.sp,
                        color = Color(0xFF666666)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 기본 정보 - 포지션 타입에 따라 다르게 표시
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "결과: ${getResultText(position.finalResult)}",
                    fontSize = 12.sp,
                    color = getResultColor(position.finalResult)
                )
                if (position.type == "LONG") {
                    Text(
                        "거래: ${position.buyTrades.size}매수 ${position.sellTrades.size}매도",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                } else {
                    Text(
                        "거래: 1매도 1매수",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }
            }

            Text(
                "시작: ${position.startTime} → 종료: ${position.endTime}",
                fontSize = 11.sp,
                color = Color(0xFF666666)
            )

            // 확장된 세부 내용
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))

                if (position.type == "LONG") {
                    // 롱의 경우: 매수 → 매도 순서
                    if (position.buyTrades.isNotEmpty()) {
                        Text(
                            "🔵 매수 거래 기록 (${position.buyTrades.size}건)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        position.buyTrades.forEach { buy ->
                            CciBuyTradeDetailCard(buy, formatter)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }

                    if (position.sellTrades.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "🟢 매도 거래 기록 (${position.sellTrades.size}건)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        position.sellTrades.forEach { sell ->
                            CciSellTradeDetailCard(sell, formatter)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                } else {
                    // 숏의 경우: 매도(진입) → 매수(청산) 순서
                    if (position.sellTrades.isNotEmpty()) {
                        Text(
                            "🔴 매도 거래 기록 (진입)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF44336)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        position.sellTrades.forEach { sell ->
                            CciShortSellDetailCard(sell, formatter)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }

                    if (position.buyTrades.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "🟢 매수 거래 기록 (청산)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        position.buyTrades.forEach { buy ->
                            CciShortBuyDetailCard(buy, formatter)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CciBuyTradeDetailCard(buy: CciTradeExecution, formatter: DecimalFormat) {
    // 거래 타입에 따른 색상 및 텍스트 결정
    val isBuyAction = buy.type.contains("BUY") || buy.type.contains("STAGE") && buy.type.contains("BUY")
    val cardColor = if (isBuyAction) Color(0xFFE3F2FD) else Color(0xFFFFF3E0)
    val circleColor = if (isBuyAction) Color(0xFF2196F3) else Color(0xFFFF9800)
    val actionText = if (isBuyAction) "✅ 매수" else "📤 매도"

    Card(
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 단계 표시
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = circleColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${buy.stage}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "$actionText ${getStageText(buy.stage)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isBuyAction) Color(0xFF1976D2) else Color(0xFFE65100)
                    )
                    Text(
                        formatTimestamp(buy.timestamp),
                        fontSize = 10.sp,
                        color = Color(0xFF666666)
                    )
                }

                Text(
                    "가격: ${formatter.format(buy.entryPrice)} | 금액: ${formatter.format(buy.amount)}",
                    fontSize = 11.sp,
                    color = Color(0xFF424242)
                )

                Text(
                    "코인: ${DecimalFormat("#,##0.######").format(buy.coins)} | 수수료: ${formatter.format(buy.fees)}",
                    fontSize = 11.sp,
                    color = Color(0xFF666666)
                )

                Text(
                    "이유: ${buy.reason}",
                    fontSize = 10.sp,
                    color = Color(0xFF666666)
                )

                // CCI 정보
                Text(
                    "진입CCI: ${DecimalFormat("#,##0.0").format(buy.entryCCI)}",
                    fontSize = 10.sp,
                    color = Color(0xFF2196F3),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
@Composable
fun CciSellTradeDetailCard(sell: CciTradeExecution, formatter: DecimalFormat) {
    val profitColor = if (sell.profitRate >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
    val bgColor = when (sell.type) {
        "PROFIT_EXIT" -> Color(0xFFE8F5E8)
        "HALF_SELL" -> Color(0xFFFFF3E0)
        else -> Color(0xFFFFEBEE)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = bgColor
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 매도 타입 아이콘
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = profitColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (sell.type) {
                        "PROFIT_EXIT" -> Icons.Default.TrendingUp
                        "HALF_SELL" -> Icons.Default.CallSplit
                        else -> Icons.Default.Close
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "🟢 ${getSellTypeText(sell.type)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = profitColor
                    )
                    Text(
                        formatTimestamp(sell.timestamp),
                        fontSize = 10.sp,
                        color = Color(0xFF666666)
                    )
                }

                Text(
                    "가격: ${formatter.format(sell.exitPrice ?: 0.0)} | 금액: ${formatter.format(sell.amount)}",
                    fontSize = 11.sp,
                    color = Color(0xFF424242)
                )

                Text(
                    "코인: ${DecimalFormat("#,##0.######").format(sell.coins)} | 수수료: ${formatter.format(sell.fees)}",
                    fontSize = 11.sp,
                    color = Color(0xFF666666)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "수익률: ${if (sell.profitRate >= 0) "+" else ""}${formatter.format(sell.profitRate)}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = profitColor
                    )
                    Text(
                        "CCI: ${DecimalFormat("#,##0.0").format(sell.exitCCI)}",
                        fontSize = 10.sp,
                        color = Color(0xFF666666)
                    )
                }

                Text(
                    "이유: ${sell.reason}",
                    fontSize = 10.sp,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}

@Composable
fun NoPositionsCard() {
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
                "포지션 내역이 없습니다",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD32F2F)
            )

            Text(
                "설정된 CCI 조건을 만족하는 포지션이 발견되지 않았습니다.",
                fontSize = 14.sp,
                color = Color(0xFFBF360C),
                textAlign = TextAlign.Center
            )
        }
    }
}

// 유틸리티 함수들
fun getStageText(stage: Int): String {
    return when (stage) {
        0 -> "첫 진입"
        1 -> "1단계 물타기"
        2 -> "2단계 물타기"
        3 -> "3단계 물타기"
        4 -> "4단계 물타기"
        else -> "${stage}단계 물타기"
    }
}

fun getSellTypeText(type: String): String {
    return when (type) {
        "PROFIT_EXIT" -> "익절 매도"
        "HALF_SELL" -> "절반 매도"
        "STOP_LOSS" -> "손절 매도"
        "FORCE_CLOSE" -> "강제 청산"
        else -> "완전 청산"
    }
}

fun getResultText(result: String): String {
    return when (result) {
        "PROFIT_EXIT" -> "익절 완료"
        "HALF_SELL_EXIT" -> "절반매도 완료"
        "STOP_LOSS" -> "손절 완료"
        "FORCE_CLOSE" -> "강제 청산"
        "INCOMPLETE" -> "미완료"
        else -> result
    }
}

fun getShortExitTypeText(type: String): String {
    return when (type) {
        "SHORT_PROFIT_EXIT" -> "숏 익절 청산"
        "SHORT_STOP_LOSS" -> "숏 손절 청산"
        else -> "숏 청산"
    }
}

fun getResultColor(result: String): Color {
    return when (result) {
        "PROFIT_EXIT", "HALF_SELL_EXIT" -> Color(0xFF4CAF50)
        "STOP_LOSS", "FORCE_CLOSE" -> Color(0xFFF44336)
        "INCOMPLETE" -> Color(0xFFFF9800)
        else -> Color(0xFF666666)
    }
}

fun formatTimestamp(timestamp: Long): String {
    val dateFormat = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
    return dateFormat.format(java.util.Date(timestamp))
}