package com.mooket.app.ui.screens.inventory.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mooket.app.data.model.PivotSummary
import com.mooket.app.ui.theme.*

/**
 * 透视表组件
 */
@Composable
fun PivotTable(
    summaries: List<PivotSummary>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(8.dp), spotColor = Color(0x14000000))
            .background(Color.White, RoundedCornerShape(8.dp))
    ) {
        // 表头
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Primary)
                .padding(vertical = 12.dp, horizontal = 8.dp)
        ) {
            PivotHeaderCell(text = "产品", modifier = Modifier.weight(2f))
            PivotHeaderCell(text = "总重量(KG)", modifier = Modifier.weight(1.5f))
            PivotHeaderCell(text = "总件数", modifier = Modifier.weight(1f))
            PivotHeaderCell(text = "条数", modifier = Modifier.weight(0.8f))
            PivotHeaderCell(text = "均价(元/KG)", modifier = Modifier.weight(1.5f))
            PivotHeaderCell(text = "预估盈利(元)", modifier = Modifier.weight(1.5f))
        }

        Divider(color = Border, thickness = 1.dp)

        // 数据行
        if (summaries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无数据",
                    color = TextHint,
                    fontSize = 14.sp
                )
            }
        } else {
            summaries.forEachIndexed { index, summary ->
                val isEvenRow = index % 2 == 0
                PivotDataRow(
                    summary = summary,
                    backgroundColor = if (isEvenRow) Color.White else PrimaryLight.copy(alpha = 0.3f)
                )
                if (index < summaries.lastIndex) {
                    Divider(color = Border.copy(alpha = 0.5f), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun PivotHeaderCell(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PivotDataRow(
    summary: PivotSummary,
    backgroundColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 产品名称
        Text(
            text = summary.productName,
            modifier = Modifier.weight(2f).padding(horizontal = 4.dp),
            color = TextPrimary,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        // 总重量
        Text(
            text = formatWeight(summary.totalWeight),
            modifier = Modifier.weight(1.5f).padding(horizontal = 4.dp),
            color = TextPrimary,
            fontSize = 12.sp,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // 总件数
        Text(
            text = summary.totalPieces.toString(),
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            color = TextPrimary,
            fontSize = 12.sp,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // 条数
        Text(
            text = summary.itemCount.toString(),
            modifier = Modifier.weight(0.8f).padding(horizontal = 4.dp),
            color = TextPrimary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // 均价
        Text(
            text = summary.avgCost?.let { String.format("%.2f", it) } ?: "-",
            modifier = Modifier.weight(1.5f).padding(horizontal = 4.dp),
            color = TextPrimary,
            fontSize = 12.sp,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // 预估盈利
        Text(
            text = formatProfit(summary.totalProfit),
            modifier = Modifier.weight(1.5f).padding(horizontal = 4.dp),
            color = if ((summary.totalProfit ?: 0.0) >= 0) Primary else Error,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 格式化重量
 */
private fun formatWeight(weight: Double): String {
    return when {
        weight >= 1000 -> String.format("%.1fT", weight / 1000)
        else -> String.format("%.0fKG", weight)
    }
}

/**
 * 格式化盈利
 */
private fun formatProfit(profit: Double?): String {
    if (profit == null || profit == 0.0) return "-"
    val prefix = if (profit > 0) "+" else ""
    return when {
        profit >= 10000 -> String.format("%s%.1f万", prefix, profit / 10000)
        else -> String.format("%s%.0f", prefix, profit)
    }
}
