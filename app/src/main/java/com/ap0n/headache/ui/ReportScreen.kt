package com.ap0n.headache.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ap0n.headache.domain.model.HeadacheEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Helper data class for our charts
data class AggregatedData(val label: String, val frequency: Int, val avgSeverity: Float)

enum class TimeAggregation { WEEKLY, MONTHLY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(viewModel: MainViewModel) {
    val report by viewModel.analyticsReport.collectAsState()
    val headaches by viewModel.headaches.collectAsState()

    // State to track whether the user wants Weekly or Monthly view
    var aggregation by remember { mutableStateOf(TimeAggregation.MONTHLY) }

    LaunchedEffect(headaches) {
        if (headaches.isNotEmpty()) {
            viewModel.refreshAnalytics()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Analytics") }) }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            item {
                // Time Toggle (Weekly vs Monthly)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilterChip(
                        selected = aggregation == TimeAggregation.WEEKLY,
                        onClick = { aggregation = TimeAggregation.WEEKLY },
                        label = { Text("Weekly") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    FilterChip(
                        selected = aggregation == TimeAggregation.MONTHLY,
                        onClick = { aggregation = TimeAggregation.MONTHLY },
                        label = { Text("Monthly") }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Process the data based on selection
                val chartData = groupHeadacheData(headaches, aggregation)

                // Render the Charts
                AggregatedCharts(chartData)

                HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))
            }

            // Correlations Section (Your existing code)
            item {
                Text("Correlations", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Which factors increase severity?",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (report.isEmpty()) {
                item { Text("Not enough data to analyze yet.") }
            }

            items(report) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            item.factorKey.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(item.description, style = MaterialTheme.typography.bodyMedium)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            "Based on ${item.count} samples",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

// --- DATA PROCESSING LOGIC ---
fun groupHeadacheData(
    headaches: List<HeadacheEntry>,
    aggregation: TimeAggregation
): List<AggregatedData> {
    if (headaches.isEmpty()) return emptyList()

    // 1. Sort chronologically so our groups are in order
    val sorted = headaches.sortedBy { it.timestamp }
    val calendar = Calendar.getInstance()

    // 2. Group the entries
    val grouped = sorted.groupBy { entry ->
        calendar.timeInMillis = entry.timestamp
        if (aggregation == TimeAggregation.MONTHLY) {
            // Group by "Jan 26"
            SimpleDateFormat("MMM yy", Locale.getDefault()).format(calendar.time)
        } else {
            // Group by Week starting date (e.g., "Feb 12")
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            SimpleDateFormat("MMM dd", Locale.getDefault()).format(calendar.time)
        }
    }

    // 3. Calculate frequency and average severity for each group
    return grouped.map { (label, entries) ->
        val frequency = entries.size
        val avgSev = entries.map { it.severity }.average().toFloat()
        AggregatedData(label, frequency, avgSev)
    }.takeLast(6) // Only show the last 6 weeks/months to keep the chart clean
}

// --- VISUAL CHARTS COMPONENT ---
@Composable
fun AggregatedCharts(data: List<AggregatedData>) {
    if (data.isEmpty()) {
        Text("No headache data to display yet.", style = MaterialTheme.typography.bodyMedium)
        return
    }

    val maxFrequency = data.maxOf { it.frequency }.coerceAtLeast(1)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Frequency (Count)", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))

        // Frequency Chart
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { point ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(point.frequency.toString(), style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .fillMaxHeight(
                                (point.frequency.toFloat() / maxFrequency).coerceAtLeast(
                                    0.05f
                                )
                            )
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Average Severity (0-10)", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))

        // Severity Chart
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { point ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        String.format(Locale.getDefault(), "%.1f", point.avgSeverity),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .fillMaxHeight((point.avgSeverity / 10f).coerceAtLeast(0.05f))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(
                                if (point.avgSeverity >= 7f) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.tertiary
                            )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = point.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}