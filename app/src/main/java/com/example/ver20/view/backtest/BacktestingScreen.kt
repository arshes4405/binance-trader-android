// BacktestingScreen.kt - 전략 목록 화면

package com.example.ver20.view.backtest

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 전략 데이터 클래스
data class TradingStrategy(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val difficulty: String,
    val expectedReturn: String,
    val riskLevel: String,
    val timeframe: String
)

@Composable
fun BacktestingScreen(modifier: Modifier = Modifier) {
    var selectedStrategy by remember { mutableStateOf<TradingStrategy?>(null) }

    // 전략이 선택되지 않았으면 목록 화면, 선택되었으면 상세 화면
    if (selectedStrategy == null) {
        StrategyListScreen(
            modifier = modifier,
            onStrategySelected = { strategy ->
                selectedStrategy = strategy
            }
        )
    } else {
        StrategyDetailScreen(
            modifier = modifier,
            strategy = selectedStrategy!!,
            onBackClick = {
                selectedStrategy = null
            }
        )
    }
}

@Composable
private fun StrategyListScreen(
    modifier: Modifier,
    onStrategySelected: (TradingStrategy) -> Unit
) {
    val strategies = listOf(
        TradingStrategy(
            id = "cci",
            name = "CCI 전략",
            description = "Commodity Channel Index를 활용한 과매수/과매도 전략",
            icon = Icons.Default.TrendingUp,
            difficulty = "중급",
            expectedReturn = "15-25%",
            riskLevel = "중간",
            timeframe = "4시간-1일"
        ),
        TradingStrategy(
            id = "rsi",
            name = "RSI 전략",
            description = "Relative Strength Index 기반 역추세 매매 전략",
            icon = Icons.Default.Analytics,
            difficulty = "초급",
            expectedReturn = "10-20%",
            riskLevel = "낮음",
            timeframe = "1시간-4시간"
        ),
        TradingStrategy(
            id = "bollinger",
            name = "볼린저 밴드 전략",
            description = "볼린저 밴드 상하한선 돌파/반등 전략",
            icon = Icons.Default.ShowChart,
            difficulty = "중급",
            expectedReturn = "12-22%",
            riskLevel = "중간",
            timeframe = "1시간-1일"
        )
    )

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
            BacktestingMainHeader()

            Spacer(modifier = Modifier.height(16.dp))

            // 안내 카드
            IntroductionCard()

            Spacer(modifier = Modifier.height(16.dp))

            // 전략 목록
            Text(
                "📊 매매 전략 선택",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(strategies) { strategy ->
                    StrategyCard(
                        strategy = strategy,
                        onClick = { onStrategySelected(strategy) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BacktestingMainHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "백테스팅",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            Text(
                "과거 데이터로 전략 성능 테스트",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }

        Icon(
            Icons.Default.Analytics,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = Color(0xFF2196F3)
        )
    }
}

@Composable
private fun IntroductionCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "백테스팅이란?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "과거의 실제 시장 데이터를 사용하여 매매 전략의 성과를 시뮬레이션하는 방법입니다.\n" +
                        "실제 투자 전에 전략의 수익성과 위험도를 미리 확인할 수 있습니다.",
                fontSize = 13.sp,
                color = Color(0xFFBF360C),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun StrategyCard(
    strategy: TradingStrategy,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 전략 아이콘
            Card(
                modifier = Modifier.size(60.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (strategy.id) {
                        "cci" -> Color(0xFFE3F2FD)
                        "rsi" -> Color(0xFFE8F5E8)
                        "bollinger" -> Color(0xFFF3E5F5)
                        else -> Color(0xFFF5F5F5)
                    }
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        strategy.icon,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                        tint = when (strategy.id) {
                            "cci" -> Color(0xFF2196F3)
                            "rsi" -> Color(0xFF4CAF50)
                            "bollinger" -> Color(0xFF9C27B0)
                            else -> Color.Gray
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 전략 정보
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    strategy.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    strategy.description,
                    fontSize = 12.sp,
                    color = Color(0xFF666666),
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 전략 정보 태그들
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    InfoChip(label = strategy.difficulty, color = Color(0xFFFF9800))
                    InfoChip(label = strategy.riskLevel, color = Color(0xFFF44336))
                    InfoChip(label = strategy.expectedReturn, color = Color(0xFF4CAF50))
                }
            }

            // 화살표 아이콘
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "전략 선택",
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun InfoChip(label: String, color: Color) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// 개별 전략 상세 화면 (업데이트된 버전)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StrategyDetailScreen(
    modifier: Modifier,
    strategy: TradingStrategy,
    onBackClick: () -> Unit
) {
    // CCI 전략인 경우 고급 화면으로 이동
    if (strategy.id == "cci") {
        AdvancedCciStrategyScreen(onBackClick = onBackClick)
    } else {
        // 다른 전략들은 기본 임시 화면
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            strategy.name,
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
                        containerColor = when (strategy.id) {
                            "rsi" -> Color(0xFF4CAF50)
                            "bollinger" -> Color(0xFF9C27B0)
                            else -> Color(0xFF2196F3)
                        }
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Construction,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFFFF9800)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "${strategy.name} 상세 화면",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "곧 구현될 예정입니다!",
                            fontSize = 14.sp,
                            color = Color(0xFFBF360C),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "현재 고급 CCI 전략만 완전히 구현되어 있습니다.\n" +
                                    "RSI와 볼린저 밴드 전략은 개발 중입니다.",
                            fontSize = 12.sp,
                            color = Color(0xFF795548),
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}