// StrategySelectionScreen.kt - 시세포착 전략 선택 화면

package com.example.ver20.view.signal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.ver20.dao.trading.signal.MarketSignalConfig
import com.example.ver20.dao.trading.signal.StrategyDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrategySelectionScreen(
    onBackClick: () -> Unit,
    onStrategySelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        // 커스텀 헤더 (TopAppBar 대신)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A237E))
                .padding(horizontal = 16.dp, vertical = 2.dp), // vertical 패딩 줄임
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = Color.White
                )
            }
            Text(
                text = "전략 선택",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 18.sp
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "시세포착 전략을 선택하세요",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // RSI 전략
            item {
                StrategyCard(
                    title = "RSI 전략",
                    subtitle = "Relative Strength Index",
                    description = "과매수/과매도 구간을 활용한 역추세 매매 전략",
                    icon = Icons.Default.ShowChart,
                    color = Color(0xFF4CAF50),
                    features = listOf(
                        "RSI 14 기본 설정",
                        "과매수 70, 과매도 30",
                        "단순하고 신뢰성 높은 지표",
                        "초보자 추천"
                    ),
                    riskLevel = "낮음",
                    complexity = "쉬움",
                    onClick = { onStrategySelected("RSI") }
                )
            }

            // CCI 전략
            item {
                StrategyCard(
                    title = "CCI 전략",
                    subtitle = "Commodity Channel Index",
                    description = "채널 이탈을 포착하는 브레이크아웃 전략",
                    icon = Icons.Default.TrendingUp,
                    color = Color(0xFF2196F3),
                    features = listOf(
                        "CCI 20 기본 설정",
                        "돌파값 100, 진입값 90",
                        "강한 추세 포착에 유리",
                        "중급자 추천"
                    ),
                    riskLevel = "중간",
                    complexity = "보통",
                    onClick = { onStrategySelected("CCI") }
                )
            }

            // 코르타 전략
            item {
                StrategyCard(
                    title = "코르타 전략",
                    subtitle = "CORTA Composite Strategy",
                    description = "MACD + RSI + 거래량을 결합한 복합 전략",
                    icon = Icons.Default.AutoAwesome,
                    color = Color(0xFFFFD700),
                    features = listOf(
                        "MACD(12,26,9) + RSI 확인",
                        "거래량 배율 검증",
                        "다중 지표 확인으로 정확도 향상",
                        "고급자 추천"
                    ),
                    riskLevel = "높음",
                    complexity = "어려움",
                    onClick = { onStrategySelected("CORTA") },
                    isPremium = true
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StrategyCard(
    title: String,
    subtitle: String,
    description: String,
    icon: ImageVector,
    color: Color,
    features: List<String>,
    riskLevel: String,
    complexity: String,
    isPremium: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(32.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            if (isPremium) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "PREMIUM",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier
                                        .background(
                                            Color(0xFFFFD700),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                // 난이도 표시
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = complexity,
                        fontSize = 12.sp,
                        color = color,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "위험도: $riskLevel",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 설명
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color.White,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 특징 목록
            features.forEach { feature ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = feature,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 선택 버튼
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = color,
                    contentColor = if (color == Color(0xFFFFD700)) Color.Black else Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "이 전략 선택",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}