// 수정된 AdvancedCciStrategyScreen.kt - 실제 전략에 맞게 업데이트

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import com.example.ver20.dao.CciStrategySettings
import com.example.ver20.dao.RealCciBacktestResult
import com.example.ver20.dao.RealCciStrategyEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedCciStrategyScreen(
    onBackClick: () -> Unit
) {
    var settings by remember { mutableStateOf(CciStrategySettings()) }
    var isRunning by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(false) }
    var backtestResult by remember { mutableStateOf<RealCciBacktestResult?>(null) }
    var showPositionHistory by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val realCciEngine = remember { RealCciStrategyEngine() }

    // 설정 변경 함수
    fun onSettingsChange(newSettings: CciStrategySettings) {
        settings = newSettings
    }

    // 포지션 내역 화면 표시
    if (showPositionHistory && backtestResult != null) {
        RealCciPositionHistoryScreen(
            backtestResult = backtestResult!!,
            onBackClick = { showPositionHistory = false }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "🎯 실제 CCI 물타기 전략",
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
                        IconButton(onClick = { settings = CciStrategySettings() }) {
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
                // 실제 전략 설명 카드
                RealCciStrategyExplanationCard()

                Spacer(modifier = Modifier.height(16.dp))

                // CCI 설정 카드
                CciConfigurationCard(settings = settings, onSettingsChange = ::onSettingsChange)

                Spacer(modifier = Modifier.height(16.dp))

                // 기본 설정 카드
                BasicSettingsCard(settings = settings, onSettingsChange = ::onSettingsChange)

                Spacer(modifier = Modifier.height(16.dp))

                // 물타기 전략 설명 카드
                RealAveragingDownStrategyCard()

                Spacer(modifier = Modifier.height(16.dp))

                // 손익절 설정 카드
                ProfitLossSettingsCard(settings = settings, onSettingsChange = ::onSettingsChange)

                Spacer(modifier = Modifier.height(16.dp))

                // 실행 컨트롤
                RealBacktestExecutionCard(
                    isRunning = isRunning,
                    onStart = {
                        isRunning = true
                        showResults = false
                        errorMessage = null

                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                backtestResult = realCciEngine.runRealCciBacktest(settings)
                                isRunning = false
                                showResults = true
                            } catch (e: Exception) {
                                isRunning = false
                                errorMessage = e.message
                            }
                        }
                    },
                    onStop = {
                        isRunning = false
                        showResults = false
                    }
                )

                if (isRunning) {
                    Spacer(modifier = Modifier.height(16.dp))
                    RealBacktestProgressCard()
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    ErrorCard(errorMessage = errorMessage!!)
                }

                if (showResults && backtestResult != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    RealBacktestResultCard(
                        result = backtestResult!!,
                        onShowPositionHistory = { showPositionHistory = true }
                    )
                }

                // 하단 여백
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun RealCciStrategyExplanationCard() {
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
                    "📈 실제 CCI 물타기 전략 (업로드된 전략)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "📋 핵심 규칙:\n" +
                        "• CCI 길이: 기본 20 (설정 가능)\n" +
                        "• 진입: CCI -110 뚫고 -100 회복시 롱, +110 뚫고 +100 회복시 숏\n" +
                        "• 시드머니의 20%로 시작\n\n" +
                        "💰 롱 물타기 시스템:\n" +
                        "• 1단계: 첫 진입 → 익절 시 전액매도 or 2% 손실시 현재물량만큼 추가매수\n" +
                        "• 2단계: 0.5% 수익시 절반매도 or 4% 손실시 현재물량만큼 추가매수\n" +
                        "• 3단계: 0.5% 수익시 절반매도 or 8% 손실시 현재물량만큼 추가매수\n" +
                        "• 4단계: 0.5% 수익시 절반매도 or 10% 손실시 손절\n\n" +
                        "🔴 숏 전략:\n" +
                        "• 물타기 없음, 손익절 퍼센트로만 청산",
                fontSize = 13.sp,
                color = Color(0xFF1565C0),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun CciConfigurationCard(
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
                "⚙️ CCI 설정",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF7B1FA2)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // CCI 길이 설정
            Text(
                "CCI 길이: ${settings.cciLength}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6A1B9A)
            )
            Slider(
                value = settings.cciLength.toFloat(),
                onValueChange = {
                    onSettingsChange(settings.copy(cciLength = it.toInt()))
                },
                valueRange = 14f..30f,
                steps = 15,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF9C27B0),
                    activeTrackColor = Color(0xFF9C27B0)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

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
        }
    }
}

@Composable
fun BasicSettingsCard(
    settings: CciStrategySettings,
    onSettingsChange: (CciStrategySettings) -> Unit
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
                "📊 기본 설정",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 시간프레임 선택
            Text(
                "시간프레임:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF388E3C)
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
                            selectedContainerColor = Color(0xFF4CAF50),
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
                color = Color(0xFF388E3C)
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
                            selectedContainerColor = Color(0xFF4CAF50),
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
                color = Color(0xFF388E3C)
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
                    thumbColor = Color(0xFF4CAF50),
                    activeTrackColor = Color(0xFF4CAF50)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 백테스팅 기간
            Text(
                "백테스팅 기간:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF388E3C)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val periods = listOf("1주일", "3개월", "6개월", "1년")
                periods.forEach { period ->
                    FilterChip(
                        onClick = { onSettingsChange(settings.copy(testPeriod = period)) },
                        label = { Text(period, fontSize = 12.sp) },
                        selected = settings.testPeriod == period,
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
fun RealAveragingDownStrategyCard() {
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
                "📊 실제 물타기 전략 상세",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE65100)
            )

            Spacer(modifier = Modifier.height(12.dp))

            val stages = listOf(
                "1단계 (0→1)" to "평균단가 대비 2% 손실 → 현재물량만큼 추가매수 → 익절시 전액매도",
                "2단계 (1→2)" to "평균단가 대비 4% 손실 → 현재물량만큼 추가매수 → 0.5% 수익시 절반매도",
                "3단계 (2→3)" to "평균단가 대비 8% 손실 → 현재물량만큼 추가매수 → 0.5% 수익시 절반매도",
                "4단계 손절" to "평균단가 대비 10% 손실 → 전량 손절 (더 이상 물타기 없음)"
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
                            color = Color(0xFFE65100)
                        )
                        Text(
                            description,
                            fontSize = 12.sp,
                            color = Color(0xFFBF360C),
                            lineHeight = 16.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFEBEE)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        "🔴 숏 전략:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                    Text(
                        "물타기 없음. 설정된 손익절 퍼센트로만 청산",
                        fontSize = 11.sp,
                        color = Color(0xFFD32F2F)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfitLossSettingsCard(
    settings: CciStrategySettings,
    onSettingsChange: (CciStrategySettings) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "💰 손익절 설정",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFC62828)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1단계 익절률
            Text(
                "1단계 익절률: ${settings.profitTarget}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFD32F2F)
            )
            Slider(
                value = settings.profitTarget.toFloat(),
                onValueChange = {
                    onSettingsChange(settings.copy(profitTarget = it.toDouble()))
                },
                valueRange = 0.5f..10f,
                steps = 18, // 0.5% 단위
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFF44336),
                    activeTrackColor = Color(0xFFF44336)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 절반매도 수익률
            Text(
                "절반매도 수익률: ${settings.halfSellProfit}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFD32F2F)
            )
            Slider(
                value = settings.halfSellProfit.toFloat(),
                onValueChange = {
                    onSettingsChange(settings.copy(halfSellProfit = it.toDouble()))
                },
                valueRange = 0.5f..3f,
                steps = 4, // 0.5% 단위
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFF44336),
                    activeTrackColor = Color(0xFFF44336)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 최종 손절률
            Text(
                "최종 손절률: ${settings.stopLossPercent}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFD32F2F)
            )
            Slider(
                value = settings.stopLossPercent.toFloat(),
                onValueChange = {
                    onSettingsChange(settings.copy(stopLossPercent = it.toDouble()))
                },
                valueRange = 5f..20f,
                steps = 29, // 0.5% 단위
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFF44336),
                    activeTrackColor = Color(0xFFF44336)
                )
            )
        }
    }
}

@Composable
fun RealBacktestExecutionCard(
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
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
            Text(
                "🚀 실제 CCI 물타기 전략 실행",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
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
                    Text("실제 CCI 물타기 전략 시작", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RealBacktestProgressCard() {
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
                "실제 CCI 물타기 전략 백테스팅 진행 중...",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1976D2)
            )
            Text(
                "바이낸스 실제 데이터로 정확한 물타기 시뮬레이션 중",
                fontSize = 12.sp,
                color = Color(0xFF1565C0)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF3E5F5)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        "📊 실행 단계:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7B1FA2)
                    )
                    Text(
                        "1️⃣ 바이낸스 실제 가격 데이터 수집\n" +
                                "2️⃣ CCI 지표 계산 (설정된 길이)\n" +
                                "3️⃣ 실제 물타기 전략 시뮬레이션\n" +
                                "4️⃣ 포지션별 수익률 분석",
                        fontSize = 10.sp,
                        color = Color(0xFF8E24AA),
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorCard(errorMessage: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = Color(0xFFF44336),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "❌ 백테스팅 실행 오류",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC62828)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                errorMessage,
                fontSize = 12.sp,
                color = Color(0xFFD32F2F),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun RealBacktestResultCard(
    result: RealCciBacktestResult,
    onShowPositionHistory: () -> Unit
) {
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
                "📈 실제 CCI 물타기 전략 결과",
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
                ResultMetric("총 수익률", "+${formatter.format((result.finalSeedMoney/result.settings.seedMoney - 1) * 100)}%", Color(0xFF4CAF50))
                ResultMetric("승률", "${formatter.format(result.winRate)}%", Color(0xFF2196F3))
                ResultMetric("최대 손실", "${formatter.format(result.maxDrawdown)}%", Color(0xFFF44336))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 상세 결과
            val detailResults = mapOf(
                "총 포지션 수" to "${result.totalPositions}개",
                "완료된 포지션" to "${result.completedPositions}개",
                "승리 포지션" to "${result.winningPositions}개",
                "손실 포지션" to "${result.losingPositions}개",
                "총 거래 횟수" to "${result.totalTrades}회",
                "총 수익" to "${formatter.format(result.totalProfit)}",
                "총 수수료" to "${formatter.format(result.totalFees)}",
                "수익 팩터" to formatter.format(result.profitFactor),
                "평균 보유시간" to "${formatter.format(result.avgHoldingTime)}시간",
                "최대 도달단계" to "${result.maxStageReached}단계",
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
                    onClick = onShowPositionHistory,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("포지션내역", fontSize = 12.sp)
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

            Spacer(modifier = Modifier.height(12.dp))

            // 실제 데이터 사용 표시
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE3F2FD)
                )
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "✅ 실제 바이낸스 데이터 + 업로드된 CCI 물타기 전략 완전 구현",
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
fun ResultMetric(label: String, value: String, color: Color) {
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