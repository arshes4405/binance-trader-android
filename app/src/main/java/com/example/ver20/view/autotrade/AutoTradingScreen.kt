// AutoTradingScreen.kt - 자동매매 관리 화면 (신규 생성)

package com.example.ver20.view.autotrade

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ver20.dao.mongoDB.UserData
import com.example.ver20.dao.mongoDB.UserService
import com.example.ver20.dao.trading.signal.MarketSignalConfig
import com.example.ver20.dao.trading.signal.MarketSignalService
import java.text.SimpleDateFormat
import java.util.*

// 자동매매 포지션 데이터 클래스
data class AutoTradingPosition(
    val id: String,
    val symbol: String,
    val direction: String, // "LONG", "SHORT"
    val entryPrice: Double,
    val currentPrice: Double,
    val quantity: Double,
    val entryTime: Long,
    val status: String, // "ACTIVE", "CLOSED"
    val unrealizedPnL: Double,
    val realizedPnL: Double = 0.0
) {
    fun getPnLPercentage(): Double {
        return if (direction == "LONG") {
            ((currentPrice - entryPrice) / entryPrice) * 100
        } else {
            ((entryPrice - currentPrice) / entryPrice) * 100
        }
    }

    fun isProfit(): Boolean = getPnLPercentage() > 0
}

@Composable
fun AutoTradingScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val userService = remember { UserService() }
    val marketSignalService = remember { MarketSignalService() }

    // 상태 변수
    var currentUser by remember { mutableStateOf<UserData?>(null) }
    var autoTradingConfigs by remember { mutableStateOf<List<MarketSignalConfig>>(emptyList()) }
    var activePositions by remember { mutableStateOf<List<AutoTradingPosition>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var totalPnL by remember { mutableStateOf(0.0) }
    var todayPnL by remember { mutableStateOf(0.0) }

    // 임시 목 데이터 (실제 API 구현까지)
    fun loadMockPositions() {
        activePositions = listOf(
            AutoTradingPosition(
                id = "pos1",
                symbol = "BTCUSDT",
                direction = "LONG",
                entryPrice = 43500.0,
                currentPrice = 44200.0,
                quantity = 0.023,
                entryTime = System.currentTimeMillis() - 3600000, // 1시간 전
                status = "ACTIVE",
                unrealizedPnL = 16.1
            ),
            AutoTradingPosition(
                id = "pos2",
                symbol = "ETHUSDT",
                direction = "SHORT",
                entryPrice = 2650.0,
                currentPrice = 2680.0,
                quantity = 0.377,
                entryTime = System.currentTimeMillis() - 7200000, // 2시간 전
                status = "ACTIVE",
                unrealizedPnL = -11.3
            )
        )
        totalPnL = 245.7
        todayPnL = 4.8
    }

    // 더미 데이터 대신 실제 데이터 로드 함수
    fun loadAutoTradingData() {
        currentUser?.let { user ->
            isLoading = true
            // 자동매매가 활성화된 설정들만 가져오기
            marketSignalService.getSignalConfigs(user.username) { configs, _ ->
                configs?.let {
                    autoTradingConfigs = it.filter { config -> config.autoTrading }
                }
                // TODO: 실제 포지션 데이터 로드 (추후 API 구현 필요)
                loadMockPositions() // 임시로 목 데이터 사용
                isLoading = false
            }
        }
    }

    // 초기 로드
    LaunchedEffect(Unit) {
        val userData = userService.getUserFromPreferences(context)
        currentUser = userData
        loadAutoTradingData()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        // 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "자동매매",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Row {
                IconButton(
                    onClick = { loadAutoTradingData() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "새로고침",
                        tint = Color(0xFFFFD700)
                    )
                }

                IconButton(
                    onClick = {
                        // 자동매매 설정 화면으로 이동
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "설정",
                        tint = Color(0xFFFFD700)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 수익 현황 카드
        PnLSummaryCard(
            totalPnL = totalPnL,
            todayPnL = todayPnL,
            activePositions = activePositions.size
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 활성 설정 개수 표시
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E1E)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "활성 자동매매 설정",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Text(
                        text = "신호 발생시 자동으로 거래 실행",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Text(
                    text = "${autoTradingConfigs.size}개",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 활성 포지션 제목
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "활성 포지션 (${activePositions.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (activePositions.isNotEmpty()) {
                TextButton(
                    onClick = {
                        // 전체 포지션 강제 청산 확인 다이얼로그
                    }
                ) {
                    Text(
                        text = "전체 청산",
                        color = Color(0xFFF44336)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 포지션 목록
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFFFD700)
                )
            }
        } else if (activePositions.isEmpty()) {
            EmptyPositionsCard()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(activePositions) { position ->
                    PositionCard(
                        position = position,
                        onClosePosition = {
                            // 포지션 청산 로직
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PnLSummaryCard(
    totalPnL: Double,
    todayPnL: Double,
    activePositions: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "수익 현황",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PnLItem(
                    label = "총 수익",
                    amount = totalPnL,
                    currency = "USDT"
                )

                PnLItem(
                    label = "오늘 수익",
                    amount = todayPnL,
                    currency = "USDT"
                )

                PnLItem(
                    label = "활성 포지션",
                    amount = activePositions.toDouble(),
                    currency = "개",
                    isCount = true
                )
            }
        }
    }
}

@Composable
private fun PnLItem(
    label: String,
    amount: Double,
    currency: String,
    isCount: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isCount) amount.toInt().toString() else String.format("%.1f", amount),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                isCount -> Color(0xFF2196F3)
                amount > 0 -> Color(0xFF4CAF50)
                amount < 0 -> Color(0xFFF44336)
                else -> Color.White
            }
        )
        Text(
            text = "$label ($currency)",
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PositionCard(
    position: AutoTradingPosition,
    onClosePosition: (AutoTradingPosition) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 헤더 (Symbol, Direction, Time)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (position.direction == "LONG") Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = position.direction,
                        tint = if (position.direction == "LONG") Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = position.symbol,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = position.direction,
                        fontSize = 12.sp,
                        color = if (position.direction == "LONG") Color(0xFF4CAF50) else Color(0xFFF44336),
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = formatTime(position.entryTime),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 가격 정보
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "진입: ${String.format("%.2f", position.entryPrice)}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "현재: ${String.format("%.2f", position.currentPrice)}",
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    Text(
                        text = "수량: ${String.format("%.3f", position.quantity)}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${if (position.isProfit()) "+" else ""}${String.format("%.1f", position.getPnLPercentage())}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (position.isProfit()) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                    Text(
                        text = "${if (position.unrealizedPnL > 0) "+" else ""}${String.format("%.1f", position.unrealizedPnL)} USDT",
                        fontSize = 13.sp,
                        color = if (position.unrealizedPnL > 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 청산 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { onClosePosition(position) }
                ) {
                    Text(
                        text = "청산",
                        color = Color(0xFFF44336)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPositionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = "자동매매 없음",
                tint = Color.Gray,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "활성 포지션이 없습니다",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "자동매매 설정을 활성화하고\n시그널이 발생하면 자동으로 거래가 시작됩니다",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

// 시간 포맷팅 함수
private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}