// CciStrategyScreen.kt - 새로 생성할 파일

package com.example.ver20.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CciStrategyScreen(
    onBackClick: () -> Unit
) {
    var cciPeriod by remember { mutableIntStateOf(14) }
    var overboughtLevel by remember { mutableIntStateOf(100) }
    var oversoldLevel by remember { mutableIntStateOf(-100) }
    var selectedTimeframe by remember { mutableIntStateOf(0) }
    var selectedSymbol by remember { mutableIntStateOf(0) }
    var isRunning by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "CCI 전략 백테스팅",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // CCI 전략 설명
            CciStrategyExplanationCard()

            Spacer(modifier = Modifier.height(16.dp))

            // CCI 파라미터 설정
            CciParameterCard(
                cciPeriod = cciPeriod,
                overboughtLevel = overboughtLevel,
                oversoldLevel = oversoldLevel,
                onCciPeriodChange = { cciPeriod = it },
                onOverboughtChange = { overboughtLevel = it },
                onOversoldChange = { oversoldLevel = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 공통 설정 (시간대, 심볼)
            CommonSettingsCard(
                selectedTimeframe = selectedTimeframe,
                selectedSymbol = selectedSymbol,
                onTimeframeChange = { selectedTimeframe = it },
                onSymbolChange = { selectedSymbol = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 실행 버튼
            BacktestControlCard(
                isRunning = isRunning,
                onStart = {
                    isRunning = true
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(3000)
                        isRunning = false
                        showResults = true
                    }
                },
                onStop = {
                    isRunning = false
                    showResults = false
                }
            )

            if (isRunning) {
                Spacer(modifier = Modifier.height(16.dp))
                LoadingCard("CCI 전략")
            }

            if (showResults) {
                Spacer(modifier = Modifier.height(16.dp))
                CciResultsCard()
            }
        }
    }
}

@Composable
private fun CciStrategyExplanationCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "CCI (Commodity Channel Index) 전략",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "• CCI가 +100을 돌파하면 매수 신호 (과매수 구간 진입)\n" +
                "• CCI가 -100을 돌파하면 매도 신호 (과매도 구간 진입)\n" +
                "• 0선을 기준으로 추세 방향 판단\n" +
                "• 일반적으로 14일 기간 사용",
                fontSize = 13.sp,
                color = Color(0xFF1565C0),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun CciParameterCard(
    cciPeriod: Int,
    overboughtLevel: Int,
    oversoldLevel: Int,
    onCciPeriodChange: (Int) -> Unit,
    onOverboughtChange: (Int) -> Unit,
    onOversoldChange: (Int) -> Unit
) {
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
                "⚙️ CCI 파라미터 설정",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF7B1FA2)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // CCI 기간
            Text(
                "CCI 기간: $cciPeriod",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6A1B9A)
            )
            Slider(
                value = cciPeriod.toFloat(),
                onValueChange = { onCciPeriodChange(it.toInt()) },
                valueRange = 5f..50f,
                steps = 44,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF9C27B0),
                    activeTrackColor = Color(0xFF9C27B0)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 과매수 레벨
            Text(
                "과매수 레벨: +$overboughtLevel",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6A1B9A)
            )
            Slider(
                value = overboughtLevel.toFloat(),
                onValueChange = { onOverboughtChange(it.toInt()) },
                valueRange = 50f..200f,
                steps = 149,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF9C27B0),
                    activeTrackColor = Color(0xFF9C27B0)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 과매도 레벨
            Text(
                "과매도 레벨: $oversoldLevel",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6A1B9A)
            )
            Slider(
                value = oversoldLevel.toFloat(),
                onValueChange = { onOversoldChange(it.toInt()) },
                valueRange = -200f..-50f,
                steps = 149,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF9C27B0),
                    activeTrackColor = Color(0xFF9C27B0)
                )
            )
        }
    }
}

@Composable
private fun CommonSettingsCard(
    selectedTimeframe: Int,
    selectedSymbol: Int,
    onTimeframeChange: (Int) -> Unit,
    onSymbolChange: (Int) -> Unit
) {
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
                "📊 백테스팅 설정",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 시간 프레임
            Text(
                "시간 프레임:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF388E3C)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val timeframes = listOf("1시간", "4시간", "1일", "1주")
                timeframes.forEachIndexed { index, timeframe ->
                    FilterChip(
                        onClick = { onTimeframeChange(index) },
                        label = { Text(timeframe, fontSize = 12.sp) },
                        selected = selectedTimeframe == index,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF4CAF50),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 거래 쌍
            Text(
                "거래 쌍:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF388E3C)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val symbols = listOf("BTCUSDT", "ETHUSDT", "BNBUSDT", "ADAUSDT")
                symbols.forEachIndexed { index, symbol ->
                    FilterChip(
                        onClick = { onSymbolChange(index) },
                        label = { Text(symbol, fontSize = 12.sp) },
                        selected = selectedSymbol == index,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF4CAF50),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun BacktestControlCard(
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "🚀 백테스팅 실행",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE65100)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isRunning) {
                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("중지", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CCI 백테스팅 시작", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LoadingCard(strategyName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE1F5FE)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = Color(0xFF2196F3))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "$strategyName 백테스팅 진행 중...",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1976D2)
            )
        }
    }
}

@Composable
private fun CciResultsCard() {
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
                "📈 CCI 전략 백테스팅 결과",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // CCI 전략 특화 결과
            val results = mapOf(
                "총 수익률" to "+18.3%",
                "CCI 신호 횟수" to "23회",
                "승률" to "65.2%",
                "평균 보유 기간" to "2.3일",
                "최대 손실" to "-5.8%",
                "샤프 비율" to "1.72"
            )

            results.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        label,
                        fontSize = 13.sp,
                        color = Color(0xFF388E3C)
                    )
                    Text(
                        value,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}