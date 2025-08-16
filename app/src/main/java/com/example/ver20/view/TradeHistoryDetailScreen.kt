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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat
import com.example.ver20.dao.CciBacktestResult
import com.example.ver20.dao.TradeResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeHistoryDetailScreen(
    backtestResult: CciBacktestResult,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "거래내역 상세",
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 백테스트 결과 요약
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E8)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "📊 백테스트 결과 요약",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "총 ${backtestResult.trades.size}개 거래 (시간순 정렬)",
                            fontSize = 12.sp,
                            color = Color(0xFF666666),
                            fontStyle = FontStyle.Italic
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val formatter = DecimalFormat("#,##0.00")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ResultSummaryItem("총 거래", "${backtestResult.totalTrades}회", Color(0xFF2196F3))
                            ResultSummaryItem("승률", "${formatter.format(backtestResult.winRate)}%", Color(0xFF4CAF50))
                            ResultSummaryItem("수익률", "+${formatter.format((backtestResult.finalSeedMoney/10000 - 1) * 100)}%", Color(0xFFFF9800))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 추가 정보
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ResultSummaryItem("총 수익", "${formatter.format(backtestResult.totalProfit)}", Color(0xFF4CAF50))
                            ResultSummaryItem("총 수수료", "${formatter.format(backtestResult.totalFees)}", Color(0xFFF44336))
                            ResultSummaryItem("최대 손실", "${formatter.format(backtestResult.maxDrawdown)}%", Color(0xFFFF5722))
                        }
                    }
                }
            }

            // 개별 거래 내역 (시간순 정렬 - 과거 데이터가 1번)
            if (backtestResult.trades.isNotEmpty()) {
                // 거래를 시간순으로 정렬 (timestamp 기준)
                val sortedTrades = backtestResult.trades.sortedBy { trade ->
                    // timestamp를 파싱하여 정렬 (MM-dd HH:mm 형식)
                    try {
                        val format = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
                        format.parse(trade.timestamp)?.time ?: 0L
                    } catch (e: Exception) {
                        // 파싱 실패시 원본 문자열로 정렬
                        trade.timestamp.hashCode().toLong()
                    }
                }

                items(sortedTrades.size) { index ->
                    val trade = sortedTrades[index]
                    // 과거 거래부터 1번으로 시작
                    TradeItemCard(trade = trade, index = index + 1)
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3E0)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color(0xFFFF9800)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                "거래 내역이 없습니다",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )

                            Text(
                                "백테스팅을 실행하면 상세한 거래 내역을 확인할 수 있습니다",
                                fontSize = 14.sp,
                                color = Color(0xFFBF360C),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultSummaryItem(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            fontSize = 12.sp,
            color = Color.Gray
        )
        Text(
            value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun TradeItemCard(trade: TradeResult, index: Int) {
    val isProfit = trade.profit >= 0
    val profitColor = if (isProfit) Color(0xFF4CAF50) else Color(0xFFF44336)
    val formatter = DecimalFormat("#,##0.00")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isProfit) Color(0xFFE8F5E8) else Color(0xFFFFEBEE)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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
                    Text(
                        "거래 #$index (${trade.type})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF424242)
                    )
                }

                Text(
                    "${if (isProfit) "+" else ""}${formatter.format(trade.profit)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = profitColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "수량: ${formatter.format(trade.amount)}",
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
                Text(
                    "수수료: ${formatter.format(trade.fee)}",
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "거래 시간: ${trade.timestamp}",
                fontSize = 10.sp,
                color = Color(0xFF999999)
            )

            // 수익률 계산 및 표시
            val profitRate = ((trade.exitPrice - trade.entryPrice) / trade.entryPrice * 100).let {
                if (trade.type == "SHORT") -it else it
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "수익률: ${if (profitRate >= 0) "+" else ""}${formatter.format(profitRate)}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (profitRate >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    }
}

// 기존 TradeHistoryScreen 함수도 유지 (다른 곳에서 사용할 수 있음)
@Composable
fun TradeHistoryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "거래내역",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    Text(
                        "백테스팅 및 실제 거래 기록",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Icon(
                    Icons.Default.List,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color(0xFF2196F3)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 안내 메시지
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Analytics,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFFFF9800)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "거래내역을 보려면 백테스팅을 실행하세요",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "백테스팅 화면에서 CCI 전략을 실행한 후\n'거래내역' 버튼을 클릭하면 상세한 거래 기록을 확인할 수 있습니다.",
                        fontSize = 14.sp,
                        color = Color(0xFFBF360C),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8F5E8)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "📊 거래내역에서 확인할 수 있는 정보:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            val features = listOf(
                                "💰 각 거래별 수익/손실",
                                "📈 진입가 및 청산가",
                                "⏰ 거래 시간 및 기간",
                                "🔄 롱/숏포지션 구분",
                                "📊 물타기 단계별 분석",
                                "🎯 승률 및 수익률 통계"
                            )

                            features.forEach { feature ->
                                Text(
                                    "• $feature",
                                    fontSize = 12.sp,
                                    color = Color(0xFF388E3C),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 추가 기능 설명
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF3E5F5)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "🔮 향후 추가될 기능들",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7B1FA2)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val futureFeatures = listOf(
                        "📱 실제 거래 API 연동",
                        "📊 실시간 포지션 추적",
                        "🔔 거래 알림 설정",
                        "📈 P&L 차트 및 분석",
                        "💾 거래 데이터 내보내기",
                        "🤖 자동매매 실행 로그"
                    )

                    futureFeatures.forEach { feature ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF9C27B0)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                feature,
                                fontSize = 13.sp,
                                color = Color(0xFF8E24AA)
                            )
                        }
                    }
                }
            }
        }
    }
}