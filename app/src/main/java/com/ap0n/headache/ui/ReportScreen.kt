package com.ap0n.headache.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ReportScreen(viewModel: MainViewModel) {
    val report by viewModel.analyticsReport.collectAsState()
    val headaches by viewModel.headaches.collectAsState()

    LaunchedEffect(headaches) {
        if (headaches.isNotEmpty()) {
            viewModel.refreshAnalytics()
        }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp)) {
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