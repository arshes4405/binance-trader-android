// SignalHistoryScreen.kt - 시그널 히스토리 화면 (새로 추가)

package com.example.ver20.view.signal

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
import com.example.ver20.dao.trading.signal.MarketSignal
import com.example.ver20.dao.trading.signal.MarketSignalService
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SignalHistoryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val userService = remember { UserService() }
    val marketSignalService = remember { MarketSignalService() }

    // 상태 변수
    var currentUser by remember { mutableStateOf<UserData?>(null) }
    var signals by remember { mutableStateOf<List<MarketSignal>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("ALL") }

    // 데이터 로드 함수
    fun loadSignals() {
        currentUser?.let { user ->
            isLoading = true
            marketSignalService.getSignals(user.username) { signalList, error ->
                if (error != null) {
                    // 에러 처리 (추후 토스트 메시지 등으로 표시)
                } else {
                    signals = signalList ?: emptyList()
                }
                isLoading = false
            }
        }
    }

    // 초기 로드
    LaunchedEffect(Unit) {
        val userData = userService.getUserFromPreferences(context)
        currentUser = userData
        loadSignals()
    }

    // 필터링된 시그널 목록
    val filteredSignals = when (selectedFilter) {
        "LONG" -> signals.filter { it.direction == "LONG" }
        "SHORT" -> signals.filter { it.direction == "SHORT" }
        "UNREAD" -> signals.filter { !it.isRead }
        else -> signals
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
                text = "시그널 히스토리",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            IconButton(
                onClick = { loadSignals() }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "새로고침",
                    tint = Color(0xFFFFD700)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 통계 카드
        StatisticsCard(signals = signals)

        Spacer(modifier = Modifier.height(16.dp))

        // 필터 버튼들
        FilterButtons(
            selectedFilter = selectedFilter,
            onFilterChange = { selectedFilter = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 시그널 목록
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFFFD700)
                )
            }
        } else if (filteredSignals.isEmpty()) {
            EmptyStateCard()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredSignals) { signal ->
                    SignalCard(
                        signal = signal,
                        onSignalClick = { 
                            // 시그널 상세 화면으로 이동하거나 읽음 처리
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatisticsCard(signals: List<MarketSignal>) {
    val totalSignals = signals.size
    val longSignals = signals.count { it.direction == "LONG" }
    val shortSignals = signals.count { it.direction == "SHORT" }
    val unreadSignals = signals.count { !it.isRead }

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
                text = "시그널 통계",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("전체", totalSignals, Color(0xFF2196F3))
                StatItem("롱", longSignals, Color(0xFF4CAF50))
                StatItem("숏", shortSignals, Color(0xFFF44336))
                StatItem("미읽음", unreadSignals, Color(0xFFFF9800))
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    count: Int,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun FilterButtons(
    selectedFilter: String,
    onFilterChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val filters = listOf(
            "ALL" to "전체",
            "LONG" to "롱",
            "SHORT" to "숏",
            "UNREAD" to "미읽음"
        )
        
        filters.forEach { (value, label) ->
            FilterChip(
                onClick = { onFilterChange(value) },
                label = { Text(label) },
                selected = selectedFilter == value,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFFD700),
                    selectedLabelColor = Color.Black,
                    containerColor = Color(0xFF2A2A2A),
                    labelColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun SignalCard(
    signal: MarketSignal,
    onSignalClick: (MarketSignal) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (signal.isRead) Color(0xFF1A1A1A) else Color(0xFF1E1E2E)
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
                    // 방향 아이콘
                    Icon(
                        imageVector = if (signal.direction == "LONG") Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = signal.direction,
                        tint = if (signal.direction == "LONG") Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = signal.symbol,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = signal.direction,
                        fontSize = 12.sp,
                        color = if (signal.direction == "LONG") Color(0xFF4CAF50) else Color(0xFFF44336),
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Text(
                    text = formatTime(signal.timestamp),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 가격 및 CCI 정보
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "가격: ${String.format("%.2f", signal.price)}",
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Text(
                        text = "CCI: ${String.format("%.1f", signal.cciValue)}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = signal.timeframe,
                        fontSize = 12.sp,
                        color = Color(0xFF2196F3)
                    )
                    if (!signal.isRead) {
                        Text(
                            text = "NEW",
                            fontSize = 10.sp,
                            color = Color(0xFFFF9800),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // 이유 (있는 경우)
            if (signal.reason.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = signal.reason,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard() {
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
                imageVector = Icons.Default.SignalCellularAlt,
                contentDescription = "시그널 없음",
                tint = Color.Gray,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "시그널이 없습니다",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "시세포착 설정을 활성화하면\n시그널이 여기에 표시됩니다",
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

// 상세 시간 포맷팅 함수
private fun formatDetailTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}