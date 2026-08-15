#!/bin/bash
cat << 'INNER_EOF' > app/src/main/java/com/hyper/phone/android/ui/screens/SettingsScreen.kt
package com.hyper.phone.android.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hyper.phone.android.data.SettingsManager
import com.hyper.phone.android.data.SpamViewModel
import com.hyper.phone.android.ui.theme.ThemeManager
import com.hyper.phone.android.ui.theme.ThemeMode
import kotlinx.coroutines.launch

enum class SettingsPage(val title: String, val icon: ImageVector) {
    MAIN("Settings", Icons.Filled.Settings),
    CALL_AUTOMATION("Call & Automation", Icons.Filled.Call),
    OFFLINE_SPAM("Offline Spam & Blocking", Icons.Filled.Block),
    DISPLAY_THEME("Display & Theme Customization", Icons.Filled.Palette),
    SOUND_ALERTS("Sound, Flash & Alerts", Icons.Filled.Notifications),
    SIM_CONTACTS("SIM Rules & Contact Manager", Icons.Filled.SimCard),
    PRIVACY_SECURITY("Privacy & Security", Icons.Filled.Security)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(spamViewModel: SpamViewModel = viewModel()) {
    var currentPage by remember { mutableStateOf(SettingsPage.MAIN) }
    
    BackHandler(enabled = currentPage != SettingsPage.MAIN) {
        currentPage = SettingsPage.MAIN
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentPage.title) },
                navigationIcon = {
                    if (currentPage != SettingsPage.MAIN) {
                        IconButton(onClick = { currentPage = SettingsPage.MAIN }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (currentPage) {
                SettingsPage.MAIN -> MainSettingsList(onNavigate = { currentPage = it })
                SettingsPage.CALL_AUTOMATION -> CallAutomationSettings(spamViewModel.settingsManager)
                SettingsPage.OFFLINE_SPAM -> OfflineSpamSettings(spamViewModel)
                SettingsPage.DISPLAY_THEME -> DisplayThemeSettings()
                SettingsPage.SOUND_ALERTS -> SoundAlertsSettings()
                SettingsPage.SIM_CONTACTS -> SimContactsSettings()
                SettingsPage.PRIVACY_SECURITY -> PrivacySecuritySettings()
            }
        }
    }
}

@Composable
fun MainSettingsList(onNavigate: (SettingsPage) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        val pages = SettingsPage.entries.filter { it != SettingsPage.MAIN }
        items(pages.size) { index ->
            val page = pages[index]
            ListItem(
                headlineContent = { Text(page.title) },
                leadingContent = { Icon(page.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.clickable { onNavigate(page) }
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun CallAutomationSettings(settingsManager: SettingsManager) {
    val scope = rememberCoroutineScope()
    val autoRecord by settingsManager.autoRecordFlow.collectAsState(initial = false)
    val redialDelay by settingsManager.redialDelayFlow.collectAsState(initial = 5f)
    val redialAttempts by settingsManager.redialAttemptsFlow.collectAsState(initial = 3f)
    val autoAnswer by settingsManager.autoAnswerFlow.collectAsState(initial = false)
    val flipToMute by settingsManager.flipToMuteFlow.collectAsState(initial = true)
    val raiseToAnswer by settingsManager.raiseToAnswerFlow.collectAsState(initial = false)
    val shakeToReject by settingsManager.shakeToRejectFlow.collectAsState(initial = false)

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SettingsCategoryTitle("Auto Call Recorder")
        SettingsSwitch("Enable Auto-Recording", "Save call audio locally", autoRecord) { scope.launch { settingsManager.saveAutoRecord(it) } }
        
        HorizontalDivider()
        SettingsCategoryTitle("Auto Redial")
        SettingsSlider("Delay Between Calls (sec)", redialDelay, 1f, 30f) { scope.launch { settingsManager.saveRedialDelay(it) } }
        SettingsSlider("Max Attempts", redialAttempts, 1f, 10f) { scope.launch { settingsManager.saveRedialAttempts(it) } }

        HorizontalDivider()
        SettingsCategoryTitle("Hands-Free & Auto Answer")
        SettingsSwitch("Auto Answer on Headset", "Answer automatically when wired/Bluetooth headset is connected", autoAnswer) { scope.launch { settingsManager.saveAutoAnswer(it) } }

        HorizontalDivider()
        SettingsCategoryTitle("Gestures & Motion")
        SettingsSwitch("Flip to Mute", "Place phone face down to silence ringer", flipToMute) { scope.launch { settingsManager.saveFlipToMute(it) } }
        SettingsSwitch("Raise to Answer", "Answer incoming call when lifting to ear", raiseToAnswer) { scope.launch { settingsManager.saveRaiseToAnswer(it) } }
        SettingsSwitch("Shake to Reject", "Shake device vigorously to decline call", shakeToReject) { scope.launch { settingsManager.saveShakeToReject(it) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineSpamSettings(viewModel: SpamViewModel) {
    val scope = rememberCoroutineScope()
    val blockNonContacts by viewModel.settingsManager.blockNonContactsFlow.collectAsState(initial = false)
    val blockPrivate by viewModel.settingsManager.blockPrivateFlow.collectAsState(initial = false)
    val spamList by viewModel.spamList.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var inputNumber by remember { mutableStateOf("") }
    var inputType by remember { mutableStateOf("exact") }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SettingsCategoryTitle("Smart Offline Block Rules")
        SettingsSwitch("Block Non-Contacts", "Reject calls from numbers not in your phonebook", blockNonContacts) { scope.launch { viewModel.settingsManager.saveBlockNonContacts(it) } }
        SettingsSwitch("Block Private/Hidden", "Reject calls with no Caller ID", blockPrivate) { scope.launch { viewModel.settingsManager.saveBlockPrivate(it) } }

        HorizontalDivider()
        SettingsCategoryTitle("Local Spam Database")
        ListItem(
            headlineContent = { Text("Add Spam Rule") },
            supportingContent = { Text("Add exact number, prefix, or regex pattern") },
            leadingContent = { Icon(Icons.Filled.Add, null) },
            modifier = Modifier.clickable { showAddDialog = true }
        )
        
        if (spamList.isNotEmpty()) {
            spamList.forEach { spam ->
                ListItem(
                    headlineContent = { Text(spam.number) },
                    supportingContent = { Text("Type: \${spam.type}") },
                    trailingContent = { 
                        IconButton(onClick = { viewModel.removeSpam(spam) }) {
                            Icon(Icons.Filled.Delete, "Delete")
                        }
                    }
                )
            }
        }

        HorizontalDivider()
        SettingsCategoryTitle("Import / Export")
        ListItem(
            headlineContent = { Text("Export Spam List") },
            supportingContent = { Text("Export to CSV/JSON locally") },
            modifier = Modifier.clickable { /* Simulate Export */ }
        )
        ListItem(
            headlineContent = { Text("Import Spam List") },
            supportingContent = { Text("Import from CSV/JSON") },
            modifier = Modifier.clickable { /* Simulate Import */ }
        )
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Spam Rule") },
            text = {
                Column {
                    OutlinedTextField(
                        value = inputNumber,
                        onValueChange = { inputNumber = it },
                        label = { Text("Number or Pattern") }
                    )
                    Spacer(Modifier.height(8.dp))
                    SettingsDropdown("Match Type", inputType, listOf("exact", "prefix", "pattern")) { inputType = it }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    if(inputNumber.isNotEmpty()) viewModel.addSpam(inputNumber, inputType)
                    showAddDialog = false
                    inputNumber = ""
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun DisplayThemeSettings() {
    val currentTheme by ThemeManager.themeMode.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SettingsCategoryTitle("Global Appearance")
        ListItem(
            headlineContent = { Text("App Theme") },
            supportingContent = { 
                Text(when(currentTheme) {
                    ThemeMode.SYSTEM -> "System default"
                    ThemeMode.LIGHT -> "Light"
                    ThemeMode.DARK -> "Dark"
                }) 
            },
            modifier = Modifier.clickable { showThemeDialog = true }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose theme") },
            text = {
                Column {
                    ThemeOption(ThemeMode.SYSTEM, "System default", currentTheme) { ThemeManager.setTheme(it); showThemeDialog = false }
                    ThemeOption(ThemeMode.LIGHT, "Light Mode", currentTheme) { ThemeManager.setTheme(it); showThemeDialog = false }
                    ThemeOption(ThemeMode.DARK, "Dark Mode (AMOLED)", currentTheme) { ThemeManager.setTheme(it); showThemeDialog = false }
                }
            },
            confirmButton = { TextButton(onClick = { showThemeDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun SoundAlertsSettings() {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SettingsCategoryTitle("Offline TTS Caller Announcer")
        ListItem(headlineContent = { Text("TTS Announcer") }, supportingContent = { Text("Speak Caller Name (Coming soon)") })
    }
}

@Composable
fun SimContactsSettings() {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SettingsCategoryTitle("Dual SIM Smart Rules")
        ListItem(headlineContent = { Text("Prefix Routing") }, supportingContent = { Text("Coming soon") })
    }
}

@Composable
fun PrivacySecuritySettings() {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SettingsCategoryTitle("App Authentication Lock")
        ListItem(headlineContent = { Text("Enable App Lock") }, supportingContent = { Text("Coming soon") })
    }
}

// Helper Components
@Composable
fun SettingsCategoryTitle(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsSwitch(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    )
}

@Composable
fun SettingsSlider(title: String, value: Float, rangeStart: Float, rangeEnd: Float, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title)
            Text(value.toInt().toString())
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = rangeStart..rangeEnd
        )
    }
}

@Composable
fun SettingsDropdown(title: String, currentValue: String, options: List<String>, onSelected: (String) -> Unit = {}) {
    var expanded by remember { mutableStateOf(false) }
        
    Box {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(currentValue) },
            modifier = Modifier.clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { 
                        onSelected(option)
                        expanded = false 
                    }
                )
            }
        }
    }
}

@Composable
fun ThemeOption(mode: ThemeMode, label: String, currentMode: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(mode) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = mode == currentMode, onClick = { onSelect(mode) })
        Spacer(modifier = Modifier.width(16.dp))
        Text(label)
    }
}
INNER_EOF
