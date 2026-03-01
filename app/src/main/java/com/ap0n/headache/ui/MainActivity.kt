package com.ap0n.headache.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ap0n.headache.R
import dagger.hilt.android.AndroidEntryPoint

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
