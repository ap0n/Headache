package com.ap0n.headache.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ap0n.headache.domain.model.QuestionType
import com.ap0n.headache.domain.model.WizardQuestion
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FactorsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val questions by viewModel.wizardQuestions.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var questionToEdit by remember { mutableStateOf<WizardQuestion?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Manage Factors") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Add Factor")
            }
        }
    ) { padding ->
        LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            items(questions) { q ->
                ListItem(
                    headlineContent = { Text(q.text) },
                    supportingContent = { Text(q.type.name) },
                    trailingContent = {
                        IconButton(onClick = { viewModel.deleteQuestion(q.id) }) {
                            Icon(Icons.Default.Delete, "Delete")
                        }
                    },
                    modifier = Modifier.clickable {
                        questionToEdit = q
                        showAddDialog = true
                    }
                )
                HorizontalDivider()
            }
        }
    }

    if (showAddDialog) {
        FactorEditorDialog(
            existingQuestion = questionToEdit,
            onDismiss = {
                showAddDialog = false
                questionToEdit = null
            },
            onSave = { q ->
                viewModel.saveQuestion(q)
                showAddDialog = false
                questionToEdit = null
            }
        )
    }
}

@Composable
fun FactorEditorDialog(
    existingQuestion: WizardQuestion?,
    onDismiss: () -> Unit,
    onSave: (WizardQuestion) -> Unit
) {
    var text by remember { mutableStateOf(existingQuestion?.text ?: "") }
    var type by remember { mutableStateOf(existingQuestion?.type ?: QuestionType.BOOLEAN) }
    var optionsText by remember {
        mutableStateOf(
            existingQuestion?.options?.joinToString(",") ?: ""
        )
    }

    // Logic: If editing, keep ID. If new, generate ID.
    // Logic: If renaming, we might technically want a new ID to avoid messing up old analytics
    // but for simplicity here we keep the ID so we can correct typos.
    val id = existingQuestion?.id ?: UUID.randomUUID().toString()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingQuestion == null) "New Factor" else "Edit Factor") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Question / Label") }
                )
                Spacer(Modifier.height(8.dp))

                Text("Type:", style = MaterialTheme.typography.labelMedium)
                // Simple Type Selector
                Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState())) {
                    QuestionType.values().forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.name) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }

                if (type == QuestionType.SINGLE_CHOICE) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = optionsText,
                        onValueChange = { optionsText = it },
                        label = { Text("Options (comma separated)") },
                        placeholder = { Text("Sunny,Rainy,Cloudy") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        WizardQuestion(
                            id = id,
                            text = text,
                            type = type,
                            options = if (optionsText.isNotBlank()) optionsText.split(",")
                                .map { it.trim() } else emptyList(),
                            min = if (type == QuestionType.NUMERIC) 0f else null,
                            max = if (type == QuestionType.NUMERIC) 10f else null
                        )
                    )
                },
                enabled = text.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}