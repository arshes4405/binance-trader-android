// AdvancedCciStrategyScreen.kt - 고급 CCI 전략 화면 (수정된 버전)

package com.example.ver20.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import com.example.ver20.dao.CciBacktestEngine
import com.example.ver20.dao.CciStrategySettings
import com.example.ver20.dao.CciBacktestResult
import com.example.ver20.dao.TradeResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedCciStrategyScreen(
    onBackClick: () -> Unit
) {
    var settings by remember { mutableStateOf(CciStrategySettings()) }
    var isRunning by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(false) }
    var backtestResult by remember { mutableStateOf<CciBacktestResult?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "고급 CCI 전략 백테스팅",
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
                actions = {
                    IconButton(onClick = { /* 설정 초기화 */ }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "설정 초기화",
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
            // 전략 설명 카드
            StrategyOverviewCard()

            Spacer(modifier = Modifier.height(16.dp))

            // 진입/청산 조건 카드
            EntryExitConditionsCard(
                settings = settings,
                onSettingsChange = { settings = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 물타기 전략 카드
            AveragingDownStrategyCard()

            Spacer(modifier = Modifier.height(16.dp))

            // 백테스팅 설정 카드
            BacktestSettingsCard(
                settings = settings,
                onSettingsChange = { settings = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 실행 컨트롤
            BacktestExecutionCard(
                isRunning = isRunning,
                onStart = {
                    isRunning = true
                    showResults = false
                    CoroutineScope(Dispatchers.Main).launch {
                        // 실제 백테스팅 엔진 사용
                        val engine = CciBacktestEngine()
                        backtestResult = engine.runBacktest(settings)
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
                BacktestProgressCard()
            }

            if (showResults && backtestResult != null) {
                Spacer(modifier = Modifier.height(16.dp))
                BacktestResultCard(backtestResult!!)
            }

            // 하단 여백 추가 (잘림 방지)
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun StrategyOverviewCard() {
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
                    "고급 CCI 물타기 전략",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "📈 진입 조건:\n" +
                        "• 4시간봉 CCI가 -110을 뚫고 -100으로 회복시 롱 진입\n" +
                        "• 4시간봉 CCI가 +110을 뚫고 +100으로 회복시 숏 진입\n\n" +
                        "💰 수익 관리:\n" +
                        "• 기본 익절: 3% 수익시 청산\n" +
                        "• 물타기: 4단계 평균단가 매수 시스템\n" +
                        "• 시드머니의 20%씩 진입",
                fontSize = 13.sp,
                color = Color(0xFF1565C0),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun EntryExitConditionsCard(
    settings: CciStrategySettings,
    onSettingsChange: (CciStrategySettings) -> Unit
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
                "⚙️ 진입/청산 설정",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF7B1FA2)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // CCI 임계값 설정
            Text(
                "CCI 진입 임계값: ±${settings.entryThreshold}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6A1B9A)
            )
            Slider(
                value = settings.entryThreshold.toFloat(),
                onValueChange = {
                    onSettingsChange(settings.copy(entryThreshold = it.toInt()))
                },
                valueRange = 100f..150f,
                steps = 49,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF9C27B0),
                    activeTrackColor = Color(0xFF9C27B0)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "CCI 청산 임계값: ±${settings.exitThreshold}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6A1B9A)
            )
            Slider(
                value = settings.exitThreshold.toFloat(),
                onValueChange = {
                    onSettingsChange(settings.copy(exitThreshold = it.toInt()))
                },
                valueRange = 80f..120f,
                steps = 39,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF9C27B0),
                    activeTrackColor = Color(0xFF9C27B0)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "익절 목표: ${settings.profitTarget}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6A1B9A)
            )
            Slider(
                value = settings.profitTarget.toFloat(),
                onValueChange = {
                    onSettingsChange(settings.copy(profitTarget = it.toDouble()))
                },
                valueRange = 1f..10f,
                steps = 89,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF9C27B0),
                    activeTrackColor = Color(0xFF9C27B0)
                )
            )
        }
    }
}

@Composable
private fun AveragingDownStrategyCard() {
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
                "📊 물타기 전략 상세",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )

            Spacer(modifier = Modifier.height(12.dp))

            val stages = listOf(
                "1단계" to "첫 진입가 대비 2% 손실시 → 시드머니 20% 추가매수 → 본절시 절반매도 → 평단가+4% 완전청산",
                "2단계" to "평단가 대비 4% 손실시 → 시드머니 40% 추가매수 → 본절시 절반매도 → 평단가+4% 완전청산",
                "3단계" to "평단가 대비 8% 손실시 → 시드머니 80% 추가매수 → 본절시 절반매도 → 평단가+4% 완전청산",
                "4단계" to "평단가 대비 16% 손실시 → 시드머니 160% 추가매수 → 본절시 절반매도 → 평단가+4% 완전청산"
            )

            stages.forEach { (stage, description) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            stage,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                        Text(
                            description,
                            fontSize = 12.sp,
                            color = Color(0xFF388E3C),
                            lineHeight = 16.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun BacktestSettingsCard(
    settings: CciStrategySettings,
    onSettingsChange: (CciStrategySettings) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "🔧 백테스팅 설정",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE65100)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 시간프레임 선택
            Text(
                "시간프레임:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFBF360C)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val timeframes = listOf("1시간", "4시간")
                timeframes.forEach { timeframe ->
                    FilterChip(
                        onClick = { onSettingsChange(settings.copy(timeframe = timeframe)) },
                        label = { Text(timeframe, fontSize = 12.sp) },
                        selected = settings.timeframe == timeframe,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF9800),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 코인 선택
            Text(
                "거래 코인:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFBF360C)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val symbols = listOf("BTCUSDT", "ETHUSDT", "BNBUSDT", "ADAUSDT")
                symbols.forEach { symbol ->
                    FilterChip(
                        onClick = {
                            onSettingsChange(settings.copy(
                                symbol = symbol,
                                startAmount = settings.seedMoney * 0.2
                            ))
                        },
                        label = { Text(symbol.replace("USDT", ""), fontSize = 12.sp) },
                        selected = settings.symbol == symbol,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF9800),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 시드머니 설정
            Text(
                "시드머니: ${DecimalFormat("#,###").format(settings.seedMoney)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFBF360C)
            )
            Text(
                "시작금액: ${DecimalFormat("#,###").format(settings.startAmount)} (시드머니의 20%)",
                fontSize = 12.sp,
                color = Color(0xFF795548)
            )
            Slider(
                value = settings.seedMoney.toFloat(),
                onValueChange = {
                    val newSeedMoney = it.toDouble()
                    onSettingsChange(settings.copy(
                        seedMoney = newSeedMoney,
                        startAmount = newSeedMoney * 0.2
                    ))
                },
                valueRange = 1000f..100000f,
                steps = 99,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFF9800),
                    activeTrackColor = Color(0xFFFF9800)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 백테스팅 기간
            Text(
                "백테스팅 기간:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFBF360C)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val periods = listOf("3개월", "6개월", "1년", "2년")
                periods.forEach { period ->
                    FilterChip(
                        onClick = { onSettingsChange(settings.copy(testPeriod = period)) },
                        label = { Text(period, fontSize = 12.sp) },
                        selected = settings.testPeriod == period,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF9800),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun BacktestExecutionCard(
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "🚀 고급 CCI 전략 실행",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFC62828)
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
                    Text("백테스팅 중지", fontWeight = FontWeight.Bold)
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
                    Text("물타기 전략 시작", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun BacktestProgressCard() {
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
                "고급 CCI 전략 백테스팅 진행 중...",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1976D2)
            )
            Text(
                "물타기 시나리오와 수수료를 계산하고 있습니다",
                fontSize = 12.sp,
                color = Color(0xFF1565C0)
            )
        }
    }
}

@Composable
private fun BacktestResultCard(result: CciBacktestResult) {
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
            Text(
                "📈 고급 CCI 전략 백테스팅 결과",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 핵심 성과 지표
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ResultMetric("총 수익률", "+${formatter.format((result.finalSeedMoney/10000 - 1) * 100)}%", Color(0xFF4CAF50))
                ResultMetric("승률", "${formatter.format(result.winRate)}%", Color(0xFF2196F3))
                ResultMetric("최대 손실", "${formatter.format(result.maxDrawdown)}%", Color(0xFFF44336))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 상세 결과
            val detailResults = mapOf(
                "총 거래 횟수" to "${result.totalTrades}회",
                "승리 거래" to "${result.winningTrades}회",
                "손실 거래" to "${result.losingTrades}회",
                "총 수익" to "${formatter.format(result.totalProfit)}",
                "총 수수료" to "${formatter.format(result.totalFees)}",
                "수익 팩터" to formatter.format(result.profitFactor),
                "최종 시드머니" to "${formatter.format(result.finalSeedMoney)}"
            )

            detailResults.forEach { (label, value) ->
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
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2E7D32)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 액션 버튼들
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { /* 상세 거래 내역 보기 */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("거래내역", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { /* 결과 저장 */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("결과저장", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ResultMetric(label: String, value: String, color: Color) {
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
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}