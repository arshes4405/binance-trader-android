// AdvancedCciStrategyScreen.kt - 고급 CCI 전략 화면 (수정된 버전)

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
import android.util.Log
import com.example.ver20.dao.CciStrategySettings
import com.example.ver20.dao.CciBacktestResult
import com.example.ver20.dao.TradeResult
import com.example.ver20.dao.RealDataBacktestEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedCciStrategyScreen(
    onBackClick: () -> Unit
) {
    var settings by remember { mutableStateOf(CciStrategySettings()) }
    var isRunning by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(false) }
    var backtestResult by remember { mutableStateOf<CciBacktestResult?>(null) }
    var showTradeHistory by remember { mutableStateOf(false) }

    // 설정 변경 함수
    fun onSettingsChange(newSettings: CciStrategySettings) {
        settings = newSettings
    }

    // 거래내역 화면 표시
    if (showTradeHistory && backtestResult != null) {
        TradeHistoryDetailScreen(
            backtestResult = backtestResult!!,
            onBackClick = { showTradeHistory = false }
        )
    } else {
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
                // 전략 설명 카드
                StrategyOverviewCard()

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

                Spacer(modifier = Modifier.height(16.dp))

                // 진입/청산 조건 카드
                EntryExitConditionsCard(
                    settings = settings,
                    onSettingsChange = ::onSettingsChange
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 물타기 전략 카드
                AveragingDownStrategyCard()

                Spacer(modifier = Modifier.height(16.dp))

                // 백테스팅 설정 카드
                BacktestSettingsCard(
                    settings = settings,
                    onSettingsChange = ::onSettingsChange
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 실행 컨트롤
                BacktestExecutionCard(
                    isRunning = isRunning,
                    onStart = {
                        isRunning = true
                        showResults = false
                        CoroutineScope(Dispatchers.Main).launch {
                            Log.d("AdvancedCciStrategy", "🚀 실제 바이낸스 데이터 백테스팅 시작")

                            // 네트워크 접근 가능 여부 확인
                            try {
                                val realDataEngine = RealDataBacktestEngine()
                                Log.d("AdvancedCciStrategy", "📡 백테스팅 엔진 생성 완료")

                                backtestResult = realDataEngine.runRealDataBacktest(settings)
                                Log.d("AdvancedCciStrategy", "✅ 백테스팅 실행 완료")

                                // 결과 검증
                                backtestResult?.let { result ->
                                    Log.d("AdvancedCciStrategy", "📊 결과 확인: ${result.trades.size}개 거래")
                                    if (result.trades.isNotEmpty()) {
                                        result.trades.forEachIndexed { index, trade ->
                                            Log.d("AdvancedCciStrategy", "거래 #${index + 1}: ${trade.type}, 시간: ${trade.timestamp}")
                                            Log.d("AdvancedCciStrategy", "  진입CCI: ${trade.entryCCI}, 이전CCI: ${trade.previousCCI}")
                                            Log.d("AdvancedCciStrategy", "  청산이유: ${trade.exitReason}, 수익: ${trade.profit}")
                                        }
                                    }
                                }

                                isRunning = false
                                showResults = true

                            } catch (e: Exception) {
                                Log.e("AdvancedCciStrategy", "❌ 백테스팅 실행 중 오류")
                                Log.e("AdvancedCciStrategy", "오류 메시지: ${e.message}")
                                Log.e("AdvancedCciStrategy", "오류 타입: ${e.javaClass.simpleName}")
                                e.printStackTrace()

                                // 임시로 더미 데이터 사용 중단하고 오류 표시
                                isRunning = false
                                showResults = false

                                // 사용자에게 오류 알림 (임시)
                                Log.e("AdvancedCciStrategy", "🚨 실제 데이터 로드 실패 - 네트워크나 API 문제일 수 있음")
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
                    BacktestProgressCard()
                }

                if (showResults && backtestResult != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    BacktestResultCard(
                        result = backtestResult!!,
                        onShowTradeHistory = { showTradeHistory = true }
                    )
                }

                // 하단 여백 추가 (잘림 방지)
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun StrategyOverviewCard() {
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
                    "📈 실제 데이터 기반 고급 CCI 물타기 전략",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "🔗 실시간 바이낸스 데이터:\n" +
                        "• 실제 시장 데이터로 정확한 백테스팅\n" +
                        "• 진짜 가격 변동성과 거래량 반영\n" +
                        "• 시장 상황에 따른 CCI 지표 정확도 향상\n\n" +
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
fun EntryExitConditionsCard(
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
fun AveragingDownStrategyCard() {
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
fun BacktestSettingsCard(
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

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "수수료율: ${settings.feeRate}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFBF360C)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE3F2FD)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        "📋 백테스팅 설정 요약:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    Text(
                        "• 시간프레임: ${settings.timeframe}\n" +
                                "• 거래 코인: ${settings.symbol}\n" +
                                "• 테스트 기간: ${settings.testPeriod}\n" +
                                "• 시드머니: ${DecimalFormat("#,###").format(settings.seedMoney)}\n" +
                                "• 수수료율: ${settings.feeRate}%\n" +
                                "• 데이터 소스: 바이낸스 실시간 API\n" +
                                "• 시간대: 한국시간(KST) 표시",
                        fontSize = 11.sp,
                        color = Color(0xFF1565C0),
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun BacktestExecutionCard(
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
                "🚀 실제 데이터 CCI 전략 실행",
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
                    Text("실제 데이터 백테스팅 시작", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BacktestProgressCard() {
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
                "고급 CCI 실제 데이터 백테스팅 진행 중...",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1976D2)
            )
            Text(
                "바이낸스에서 실제 가격 데이터를 가져와 분석하고 있습니다",
                fontSize = 12.sp,
                color = Color(0xFF1565C0)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 진행 단계 표시
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF3E5F5)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        "📊 진행 단계:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7B1FA2)
                    )
                    Text(
                        "1️⃣ 바이낸스 API에서 실제 가격 데이터 수집\n" +
                                "2️⃣ CCI 지표 계산 (14기간 평균)\n" +
                                "3️⃣ 물타기 전략 시뮬레이션 실행\n" +
                                "4️⃣ 수익률 및 위험도 분석\n" +
                                "🔍 진입 조건: 롱(-110→-100), 숏(+110→+100)",
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
fun BacktestResultCard(
    result: CciBacktestResult,
    onShowTradeHistory: () -> Unit
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
                "📈 실제 데이터 CCI 백테스팅 결과",
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
                ResultMetric("이 수익률", "+${formatter.format((result.finalSeedMoney/10000 - 1) * 100)}%", Color(0xFF4CAF50))
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
                    onClick = onShowTradeHistory,
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
                        "실제 바이낸스 시장 데이터 기반 백테스팅 완료",
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

// 더미 데이터 생성 함수 (CCI 값 포함) - 네트워크 문제시 임시 사용
fun createDummyBacktestResult(): CciBacktestResult {
    Log.d("AdvancedCciStrategy", "🔄 더미 데이터 생성 중 (CCI 값 포함)")

    val trades = listOf(
        TradeResult(
            type = "LONG",
            entryPrice = 70000.0,
            exitPrice = 72800.0,
            amount = 2000.0,
            profit = 80.0,
            fee = 5.6,
            timestamp = "06-06 05:00", // 6월 6일 5시 (UTC) = 14시 (KST)
            entryCCI = -95.3,      // 진입시 CCI (롱 조건 만족)
            previousCCI = -118.7,  // 이전 CCI (-110 아래)
            exitReason = "PROFIT"
        ),
        TradeResult(
            type = "SHORT",
            entryPrice = 72000.0,
            exitPrice = 71200.0,
            amount = 2000.0,
            profit = 22.4,
            fee = 5.76,
            timestamp = "06-06 09:15", // 잘못된 시그널 예시
            entryCCI = 85.2,       // 진입시 CCI (숏 조건 불만족!)
            previousCCI = 125.4,   // 이전 CCI (+110 위)
            exitReason = "PROFIT"
        ),
        TradeResult(
            type = "LONG",
            entryPrice = 69500.0,
            exitPrice = 68200.0,
            amount = 2040.0, // 이전 수익으로 시드머니 증가
            profit = -52.0,
            fee = 8.16,
            timestamp = "06-07 02:45",
            entryCCI = -98.1,      // 진입시 CCI (롱 조건 만족)
            previousCCI = -112.8,  // 이전 CCI (-110 아래)
            exitReason = "STOP_LOSS"
        )
    )

    Log.d("AdvancedCciStrategy", "✅ 더미 데이터 생성 완료: ${trades.size}개 거래 (실제같은 CCI 값 포함)")

    return CciBacktestResult(
        totalTrades = trades.size,
        winningTrades = trades.count { it.profit > 0 },
        losingTrades = trades.count { it.profit < 0 },
        totalProfit = trades.sumOf { it.profit },
        totalFees = trades.sumOf { it.fee },
        maxDrawdown = 7.5,
        finalSeedMoney = 10050.4,
        winRate = 66.7,
        profitFactor = 1.96,
        trades = trades
    )
}