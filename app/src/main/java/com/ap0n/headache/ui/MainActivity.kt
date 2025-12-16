package com.ap0n.headache.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ap0n.headache.R
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HeadacheApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeadacheApp() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = hiltViewModel()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate("home") },
                    icon = {
                        Image(
                            painter = painterResource(id = R.drawable.ic_home),
                            contentDescription = "Log",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("Log") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate("wizard") },
                    icon = {
                        Image(
                            painter = painterResource(id = R.drawable.ic_add),
                            contentDescription = "Add",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("Add") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        viewModel.refreshAnalytics()
                        navController.navigate("report")
                    },
                    icon = {
                        Image(
                            painter = painterResource(id = R.drawable.ic_analytics),
                            contentDescription = "Stats",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("Stats") }
                )
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onSettingsClick = { navController.navigate("factors") },
                    onHeadacheClick = { id ->
                        navController.navigate("edit/$id")
                    }
                )
            }
            composable("wizard") { AddHeadacheScreen(viewModel) { navController.popBackStack() } }
            composable("report") { ReportScreen(viewModel) }
            composable("edit/{id}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")
                if (id != null) {
                    EditScreen(viewModel, id) { navController.popBackStack() }
                }
            }
            composable("factors") {
                FactorsScreen(viewModel) { navController.popBackStack() }
            }
        }
    }
}

// --- Screens ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onSettingsClick: () -> Unit,
    onHeadacheClick: (String) -> Unit
) {
    val headaches by viewModel.headaches.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Headache Log") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Manage Factors")
                    }
                }
            )
        }
    ) {

        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            item {
                Text("Headache Log", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
            }
            items(headaches) { h ->
                Card(
                    onClick = { onHeadacheClick(h.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                SimpleDateFormat(
                                    "MMM dd, HH:mm",
                                    Locale.getDefault()
                                ).format(Date(h.timestamp))
                            )
                            Badge { Text("Severity: ${h.severity}") }
                        }
                        if (h.notes.isNotEmpty()) Text(
                            h.notes,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReportScreen(viewModel: MainViewModel) {
    val report by viewModel.analyticsReport.collectAsState()

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