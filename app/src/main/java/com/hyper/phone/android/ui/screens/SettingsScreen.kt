package com.hyper.phone.android.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    DISPLAY_THEME("Display & Theme", Icons.Filled.Palette),
    SOUND_ALERTS("Sound & Alerts", Icons.Filled.Notifications),
    SIM_CONTACTS("SIM & Contacts", Icons.Filled.SimCard),
    PRIVACY_SECURITY("Privacy & Security", Icons.Filled.Security)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(spamViewModel: SpamViewModel = viewModel(), onBack: () -> Unit = {}) {
    var currentPage by remember { mutableStateOf(SettingsPage.MAIN) }
    
    BackHandler {
        if (currentPage != SettingsPage.MAIN) {
            currentPage = SettingsPage.MAIN
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            val configuration = LocalConfiguration.current
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 16.dp)
            ) {
                IconButton(onClick = { 
                    if (currentPage != SettingsPage.MAIN) currentPage = SettingsPage.MAIN else onBack() 
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = currentPage.title,
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (currentPage) {
                SettingsPage.MAIN -> MainSettingsList(onNavigate = { currentPage = it })
                SettingsPage.CALL_AUTOMATION -> CallAutomationSettings(spamViewModel.settingsManager)
                SettingsPage.OFFLINE_SPAM -> OfflineSpamSettings(spamViewModel)
                SettingsPage.DISPLAY_THEME -> DisplayThemeSettings()
                SettingsPage.SOUND_ALERTS -> SoundAlertsSettings(spamViewModel.settingsManager)
                SettingsPage.SIM_CONTACTS -> SimContactsSettings(spamViewModel.settingsManager)
                SettingsPage.PRIVACY_SECURITY -> PrivacySecuritySettings(spamViewModel.settingsManager)
            }
        }
    }
}

@Composable
fun MainSettingsList(onNavigate: (SettingsPage) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingsGroup {
                val pages = SettingsPage.entries.filter { it != SettingsPage.MAIN }
                pages.forEachIndexed { index, page ->
                    val gradient = when (page) {
                        SettingsPage.CALL_AUTOMATION -> com.hyper.phone.android.ui.theme.MintEmeraldGradient
                        SettingsPage.OFFLINE_SPAM -> com.hyper.phone.android.ui.theme.CrimsonRubyGradient
                        SettingsPage.DISPLAY_THEME -> com.hyper.phone.android.ui.theme.SkyBlueGradient
                        SettingsPage.SOUND_ALERTS -> com.hyper.phone.android.ui.theme.AmberGoldGradient
                        SettingsPage.SIM_CONTACTS -> com.hyper.phone.android.ui.theme.ElectricIndigoGradient
                        SettingsPage.PRIVACY_SECURITY -> com.hyper.phone.android.ui.theme.FrostedSlateGradient
                        else -> com.hyper.phone.android.ui.theme.ElectricIndigoGradient
                    }
                    ListItem(
                        headlineContent = { Text(page.title, fontWeight = FontWeight.Medium) },
                        leadingContent = { 
                            com.hyper.phone.android.ui.components.VibrantBadge(
                                icon = page.icon,
                                gradient = gradient,
                                isActive = true,
                                showHalo = false,
                                coreSize = 40.dp,
                                iconSize = 24.dp
                            )
                        },
                        trailingContent = {
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        modifier = Modifier.clickable { onNavigate(page) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    if (index < pages.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
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

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsCategoryTitle("Auto Call Recorder")
            SettingsGroup {
                SettingsSwitch("Enable Auto-Recording", "Save call audio locally", autoRecord) { scope.launch { settingsManager.saveAutoRecord(it) } }
            }
        }
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsCategoryTitle("Auto Redial")
            SettingsGroup {
                SettingsSlider("Delay Between Calls (sec)", redialDelay, 1f, 30f) { scope.launch { settingsManager.saveRedialDelay(it) } }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsSlider("Max Attempts", redialAttempts, 1f, 10f) { scope.launch { settingsManager.saveRedialAttempts(it) } }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsCategoryTitle("Hands-Free & Auto Answer")
            SettingsGroup {
                SettingsSwitch("Auto Answer on Headset", "Answer automatically when headset is connected", autoAnswer) { scope.launch { settingsManager.saveAutoAnswer(it) } }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsCategoryTitle("Gestures & Motion")
            SettingsGroup {
                SettingsSwitch("Flip to Mute", "Place phone face down to silence ringer", flipToMute) { scope.launch { settingsManager.saveFlipToMute(it) } }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsSwitch("Raise to Answer", "Answer incoming call when lifting to ear", raiseToAnswer) { scope.launch { settingsManager.saveRaiseToAnswer(it) } }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsSwitch("Shake to Reject", "Shake device vigorously to decline call", shakeToReject) { scope.launch { settingsManager.saveShakeToReject(it) } }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineSpamSettings(viewModel: SpamViewModel) {
    val scope = rememberCoroutineScope()
    val blockNonContacts by viewModel.settingsManager.blockNonContactsFlow.collectAsState(initial = false)
    val blockPrivate by viewModel.settingsManager.blockPrivateFlow.collectAsState(initial = false)
    val minimizeIncoming by viewModel.settingsManager.minimizeIncomingFlow.collectAsState(initial = false)
    val spamList by viewModel.spamList.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var inputNumber by remember { mutableStateOf("") }
    var inputType by remember { mutableStateOf("exact") }
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { viewModel.exportToCsv(context, it) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importFromCsv(context, it) }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsCategoryTitle("Call UI Settings")
            SettingsGroup {
                SettingsSwitch("Minimize Incoming Calls", "Show Heads-Up Notification instead of full screen", minimizeIncoming) { scope.launch { viewModel.settingsManager.saveMinimizeIncoming(it) } }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsCategoryTitle("Smart Offline Block Rules")
            SettingsGroup {
                SettingsSwitch("Block Non-Contacts", "Reject calls from unknown numbers", blockNonContacts) { scope.launch { viewModel.settingsManager.saveBlockNonContacts(it) } }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsSwitch("Block Private/Hidden", "Reject calls with no Caller ID", blockPrivate) { scope.launch { viewModel.settingsManager.saveBlockPrivate(it) } }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsCategoryTitle("Local Spam Database")
            SettingsGroup {
                ListItem(
                    headlineContent = { Text("Add Spam Rule", color = MaterialTheme.colorScheme.primary) },
                    supportingContent = { Text("Add exact number, prefix, or regex pattern") },
                    leadingContent = { Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable { showAddDialog = true },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                
                if (spamList.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    spamList.forEachIndexed { index, spam ->
                        ListItem(
                            headlineContent = { Text(spam.number) },
                            supportingContent = { Text("Type: ${spam.type}") },
                            trailingContent = { 
                                IconButton(onClick = { viewModel.removeSpam(spam) }) {
                                    Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        if (index < spamList.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsCategoryTitle("Import / Export")
            SettingsGroup {
                ListItem(
                    headlineContent = { Text("Export Spam List") },
                    supportingContent = { Text("Export to CSV locally") },
                    modifier = Modifier.clickable { exportLauncher.launch("spam_list.csv") },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ListItem(
                    headlineContent = { Text("Import Spam List") },
                    supportingContent = { Text("Import from CSV") },
                    modifier = Modifier.clickable { importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*")) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Spam Rule") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = inputNumber,
                        onValueChange = { inputNumber = it },
                        label = { Text("Number or Pattern") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    SettingsDropdown("Match Type", inputType, listOf("exact", "prefix", "pattern", "country_code")) { inputType = it }
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

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsCategoryTitle("Global Appearance")
            SettingsGroup {
                ListItem(
                    headlineContent = { Text("App Theme") },
                    supportingContent = { 
                        Text(when(currentTheme) {
                            ThemeMode.SYSTEM -> "System default"
                            ThemeMode.LIGHT -> "Light"
                            ThemeMode.DARK -> "Dark"
                        }) 
                    },
                    modifier = Modifier.clickable { showThemeDialog = true },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
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
fun SoundAlertsSettings(settingsManager: SettingsManager) {
    val scope = rememberCoroutineScope()
    val ttsAnnouncer by settingsManager.ttsAnnouncerFlow.collectAsState(initial = false)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsCategoryTitle("Offline TTS Caller Announcer")
            SettingsGroup {
                SettingsSwitch("TTS Announcer", "Speak Caller Name loudly when phone rings", ttsAnnouncer) { scope.launch { settingsManager.saveTtsAnnouncer(it) } }
            }
        }
    }
}

@Composable
fun SimContactsSettings(settingsManager: SettingsManager) {
    val scope = rememberCoroutineScope()
    val prefixRouting by settingsManager.prefixRoutingFlow.collectAsState(initial = false)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsCategoryTitle("Dual SIM Smart Rules")
            SettingsGroup {
                SettingsSwitch("Prefix Routing", "Automatically route specific prefixes to SIM 1 or 2", prefixRouting) { scope.launch { settingsManager.savePrefixRouting(it) } }
            }
        }
    }
}

@Composable
fun PrivacySecuritySettings(settingsManager: SettingsManager) {
    val scope = rememberCoroutineScope()
    val appLock by settingsManager.appLockFlow.collectAsState(initial = false)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsCategoryTitle("App Authentication Lock")
            SettingsGroup {
                SettingsSwitch("Enable App Lock", "Require biometrics to open dialer", appLock) { scope.launch { settingsManager.saveAppLock(it) } }
            }
        }
    }
}

// Helper Components
@Composable
fun SettingsCategoryTitle(title: String) {
    Text(
        text = title.uppercase(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsSwitch(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        modifier = Modifier.clickable { onCheckedChange(!checked) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun SettingsSlider(title: String, value: Float, rangeStart: Float, rangeEnd: Float, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, fontWeight = FontWeight.Medium)
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
            modifier = Modifier.clickable { expanded = true },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
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
