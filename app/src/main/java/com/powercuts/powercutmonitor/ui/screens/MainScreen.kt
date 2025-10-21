package com.powercuts.powercutmonitor.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.powercuts.powercutmonitor.data.db.entities.PowerEvent
import com.powercuts.powercutmonitor.data.db.entities.DailyTotal
import com.powercuts.powercutmonitor.data.db.entities.TodayTotals
import com.powercuts.powercutmonitor.data.prefs.PrefsManager
import com.powercuts.powercutmonitor.ui.theme.PowerCutMonitorTheme
import com.powercuts.powercutmonitor.ui.viewmodels.MainViewModel
import com.powercuts.powercutmonitor.ui.viewmodels.MainViewModelFactory
import com.powercuts.powercutmonitor.util.BatteryOptimizationUtils
import com.powercuts.powercutmonitor.util.Constants
import com.powercuts.powercutmonitor.util.DateTimeUtils
import com.powercuts.powercutmonitor.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(LocalContext.current))
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefsManager = remember { PrefsManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val currentThemeMode by prefsManager.themeMode.collectAsState(initial = Constants.DEFAULT_THEME_MODE)
    
    // Safe state collection with default values
    val isMonitoring by viewModel.isMonitoring.collectAsState(initial = false)
    val todayTotals by viewModel.todayTotals.collectAsState(initial = TodayTotals(0, 0, "0s"))
    val recentEvents by viewModel.recentEvents.collectAsState(initial = emptyList())
    val groupedEvents by viewModel.groupedEvents.collectAsState(initial = emptyList())
    val dailyTotals by viewModel.dailyTotals.collectAsState(initial = emptyList())
        val isLoading by viewModel.isLoading.collectAsState(initial = false)
        val errorMessage by viewModel.errorMessage.collectAsState(initial = null)
        val exportFile by viewModel.exportFile.collectAsState(initial = null)

    // Dialog states
    var showDeleteDialog by remember { mutableStateOf<PowerEvent?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }
    var showBatteryOptimizationDialog by remember { mutableStateOf(false) }
    var showUnusedAppSettingsDialog by remember { mutableStateOf(false) }
    var userInitiatedBatteryDialog by remember { mutableStateOf(false) }
    
    // Battery optimization status - reactive to app lifecycle
    var isBatteryOptimized by remember { mutableStateOf(false) }
    
    // Update battery optimization status when app resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val wasOptimized = isBatteryOptimized
                isBatteryOptimized = BatteryOptimizationUtils.isIgnoringBatteryOptimizations(context)
                
                // Only show unused app settings dialog if user initiated the battery optimization dialog
                if (!wasOptimized && isBatteryOptimized && userInitiatedBatteryDialog && !showUnusedAppSettingsDialog) {
                    showUnusedAppSettingsDialog = true
                    userInitiatedBatteryDialog = false // Reset the flag
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        // Initial check
        isBatteryOptimized = BatteryOptimizationUtils.isIgnoringBatteryOptimizations(context)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Handle export file sharing
    LaunchedEffect(exportFile) {
        exportFile?.let { file ->
            try {
                // Create sharing intent
                val shareIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    type = "application/zip"
                    putExtra(android.content.Intent.EXTRA_STREAM, 
                        androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                    )
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Power Cuts Log Export")
                    putExtra(android.content.Intent.EXTRA_TEXT, "Power cuts data export from Power Cuts Log app")
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Power Cuts Data"))
                viewModel.clearExportFile()
            } catch (e: Exception) {
                // Handle sharing error
                viewModel.clearExportFile()
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Power Cuts Log",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            },
            actions = {
                // Theme dropdown menu
                Box {
                    IconButton(onClick = { showThemeMenu = true }) {
                        Icon(
                            imageVector = when (currentThemeMode) {
                                Constants.THEME_MODE_LIGHT -> Icons.Default.LightMode
                                Constants.THEME_MODE_DARK -> Icons.Default.DarkMode
                                else -> if (isSystemInDarkTheme()) Icons.Default.DarkMode else Icons.Default.LightMode
                            },
                            contentDescription = "Theme"
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showThemeMenu,
                        onDismissRequest = { showThemeMenu = false },
                        modifier = Modifier
                            .width(160.dp)
                            .background(
                                color = when (currentThemeMode) {
                                    Constants.THEME_MODE_DARK -> Color(0xFF2C2C2E)  // Dark mode: dark grey
                                    Constants.THEME_MODE_LIGHT -> Color(0xFFF8F8FF)  // Light mode: light lavender
                                    else -> if (isSystemInDarkTheme()) Color(0xFF2C2C2E) else Color(0xFFF8F8FF)  // Auto: follow system
                                },
                                shape = RoundedCornerShape(12.dp)
                            ),
                        offset = DpOffset(x = 0.dp, y = 4.dp)
                    ) {
                        // Auto option
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "Auto",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (currentThemeMode == Constants.THEME_MODE_SYSTEM) 
                                        FontWeight.Medium 
                                    else 
                                        FontWeight.Normal,
                                    color = if (currentThemeMode == Constants.THEME_MODE_SYSTEM)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                coroutineScope.launch {
                                    prefsManager.setThemeMode(Constants.THEME_MODE_SYSTEM)
                                }
                                showThemeMenu = false
                            },
                            trailingIcon = {
                                if (currentThemeMode == Constants.THEME_MODE_SYSTEM) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .then(
                                    if (currentThemeMode == Constants.THEME_MODE_SYSTEM)
                                        Modifier.background(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    else
                                        Modifier
                                )
                        )
                        
                        // Dark option
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "Dark",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (currentThemeMode == Constants.THEME_MODE_DARK) 
                                        FontWeight.Medium 
                                    else 
                                        FontWeight.Normal,
                                    color = if (currentThemeMode == Constants.THEME_MODE_DARK)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                coroutineScope.launch {
                                    prefsManager.setThemeMode(Constants.THEME_MODE_DARK)
                                }
                                showThemeMenu = false
                            },
                            trailingIcon = {
                                if (currentThemeMode == Constants.THEME_MODE_DARK) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .then(
                                    if (currentThemeMode == Constants.THEME_MODE_DARK)
                                        Modifier.background(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    else
                                        Modifier
                                )
                        )
                        
                        // Light option
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "Light",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (currentThemeMode == Constants.THEME_MODE_LIGHT) 
                                        FontWeight.Medium 
                                    else 
                                        FontWeight.Normal,
                                    color = if (currentThemeMode == Constants.THEME_MODE_LIGHT)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                coroutineScope.launch {
                                    prefsManager.setThemeMode(Constants.THEME_MODE_LIGHT)
                                }
                                showThemeMenu = false
                            },
                            trailingIcon = {
                                if (currentThemeMode == Constants.THEME_MODE_LIGHT) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .then(
                                    if (currentThemeMode == Constants.THEME_MODE_LIGHT)
                                        Modifier.background(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    else
                                        Modifier
                                )
                        )
                    }
                }
                
                // About button
                IconButton(onClick = { showAboutDialog = true }) {
                    Icon(Icons.Default.Info, contentDescription = "About App")
                }
            }
        )

        // Monitoring Status Indicator
        if (isMonitoring && !isBatteryOptimized) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "⚠️ Battery optimization may stop monitoring",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Tap 'Fix' to disable battery optimization",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    // Refresh button
                    IconButton(
                        onClick = { 
                            isBatteryOptimized = BatteryOptimizationUtils.isIgnoringBatteryOptimizations(context)
                        }
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh status",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    // Fix button
                    Button(
                        onClick = { 
                            userInitiatedBatteryDialog = true
                            showBatteryOptimizationDialog = true 
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onErrorContainer,
                            contentColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Fix",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Main Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status Cards
            item {
                StatusCardsRow(
                    todayTotals = todayTotals,
                    isLoading = isLoading
                )
            }

            // Action Buttons
            item {
                ActionButtonsRow(
                    onStartFresh = { viewModel.toggleMonitoring() },
                    onRefresh = { viewModel.refreshData() },
                    onShare = { showExportDialog = true },
                    isLoading = isLoading,
                    isMonitoring = isMonitoring
                )
            }

            // Add a Spacer here to increase the gap by 100%
            item {
                Spacer(modifier = Modifier.height(24.dp)) // Adds 24dp, making total 36dp with spacedBy(12.dp)
            }

            // Recent Events
            if (groupedEvents.isEmpty()) {
                item {
                    EmptyStateCard()
                }
            } else {
                item {
                    Text(
                        text = "Recent Events",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                groupedEvents.forEach { groupedEvent ->
                    // Date header
                    item {
                        DateHeaderCard(date = groupedEvent.date)
                    }
                    
                    // Events for this date
                    groupedEvent.events.forEach { event ->
                        item {
                            EventCard(
                                event = event,
                                onDelete = { showDeleteDialog = event }
                            )
                        }
                    }
                }
            }

            // Daily Totals
            if (dailyTotals.isNotEmpty()) {
                item {
                    Text(
                        text = "Daily Summary",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(dailyTotals) { dailyTotal ->
                    DailyTotalCard(dailyTotal = dailyTotal)
                }
            }
        }
    }

    // Delete Confirmation Dialog
    showDeleteDialog?.let { event ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Event") },
            text = { Text("Are you sure you want to delete this power cut event?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEvent(event)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Export Dialog
    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onExport = { scope ->
                // Trigger export in ViewModel with the actual scope
                viewModel.exportAndShare(scope)
                showExportDialog = false
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false }
        )
    }

    // Battery Optimization Dialog
    if (showBatteryOptimizationDialog) {
        BatteryOptimizationDialog(
            context = context,
            onDismiss = { 
                showBatteryOptimizationDialog = false
                // Refresh battery optimization status when dialog is dismissed
                isBatteryOptimized = BatteryOptimizationUtils.isIgnoringBatteryOptimizations(context)
            },
            onOpenSettings = {
                BatteryOptimizationUtils.requestIgnoreBatteryOptimizations(context)
                showBatteryOptimizationDialog = false
            }
        )
    }

    // Unused App Settings Dialog (shown after battery optimization)
    if (showUnusedAppSettingsDialog) {
        UnusedAppSettingsDialog(
            context = context,
            onDismiss = { 
                showUnusedAppSettingsDialog = false
            },
            onOpenSettings = {
                BatteryOptimizationUtils.openUnusedAppSettings(context)
                showUnusedAppSettingsDialog = false
            }
        )
    }

    // Error Snackbar
    errorMessage?.let { error ->
        LaunchedEffect(error) {
            // Show error message
        }
    }
}

@Composable
fun StatusCardsRow(
    todayTotals: TodayTotals,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Power Cuts Card
        Card(
            modifier = Modifier
                .weight(1f)
                .height(90.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaddingValues(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 16.dp)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Power Cuts",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = todayTotals.count.toString(),
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 36.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Cuts today",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        // Total Off Time Card
        Card(
            modifier = Modifier
                .weight(1f)
                .height(90.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaddingValues(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 16.dp)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Total Off Time",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = todayTotals.formattedDuration,
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 36.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Power-off time",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
fun ActionButtonsRow(
    onStartFresh: () -> Unit,
    onRefresh: () -> Unit,
    onShare: () -> Unit,
    isLoading: Boolean,
    isMonitoring: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onStartFresh,
            enabled = !isLoading,
            modifier = Modifier
                .weight(1f)
                .height(44.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
        ) {
            Icon(
                if (isMonitoring) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = if (isMonitoring) "Stop" else "Start",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }

        OutlinedButton(
            onClick = onRefresh,
            enabled = !isLoading,
            modifier = Modifier
                .weight(1f)
                .height(44.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "Refresh",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }

        OutlinedButton(
            onClick = onShare,
            enabled = !isLoading,
            modifier = Modifier
                .weight(1f)
                .height(44.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(
                Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "Export",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun DateHeaderCard(date: java.time.LocalDate) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = DateTimeUtils.formatDateHeader(date),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EventCard(
    event: PowerEvent,
    onDelete: () -> Unit
) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time range on the left
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = DateTimeUtils.formatEventTime(event.startEpochMs),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )

                if (event.endEpochMs != null) {
                    Text(
                        text = " → ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = DateTimeUtils.formatEventTime(event.endEpochMs),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    Text(
                        text = " → Ongoing",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Duration badge in the center
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.padding(horizontal = 6.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = FormatUtils.formatDurationForList(event.durationSec ?: 0),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }

            // Delete button on the right
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Event",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun DailyTotalCard(
    dailyTotal: DailyTotal
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = DateTimeUtils.getDateDisplayName(dailyTotal.date),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${dailyTotal.eventCount} power cuts",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Text(
                text = FormatUtils.formatDurationForList(dailyTotal.totalDurationSec),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.PowerOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Power Cuts Recorded",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Start monitoring to track power outages",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onExport: (ExportScope) -> Unit
) {
    var selectedScope by remember { mutableStateOf(ExportScope.LAST_30_DAYS) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Data") },
        text = {
            Column {
                Text("Choose the time range to export:")
                Spacer(modifier = Modifier.height(16.dp))
                
                ExportScope.values().forEach { scope ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedScope == scope,
                            onClick = { selectedScope = scope }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(scope.displayName)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onExport(selectedScope) }
            ) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("About Power Cuts Log") },
        text = {
            Column {
                Text(
                    text = "Power Cuts Log helps you track and monitor power cuts while your phone is plugged in. Works offline; data stays on your device.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Button Functions:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "• Start/Stop: Begin or end power monitoring",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "• Refresh(Optional): Reload current data (not needed; data updates live)",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "• Export: Share power cut data as ZIP",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Features:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "• Automatic power cut detection",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "• Real-time monitoring with notifications",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "• Daily statistics and summaries",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "• Data export for analysis",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Version 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Contact: support@markedburg.com",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnusedAppSettingsDialog(
    context: Context,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Unused App Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Step 2 of 2: Disable 'Manage app if unused'",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "This prevents Android from hibernating the app:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "1. Tap 'Open App Settings' below",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "2. Find 'Unused app settings' section",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "3. Turn OFF 'Manage app if unused' toggle",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "4. Return to this app",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = "🎉 After this step, your app will monitor continuously!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Open App Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Skip for now")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryOptimizationDialog(
    context: Context,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Battery Optimization",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Step 1 of 2: Disable Battery Optimization",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "This prevents Android from stopping the app to save battery:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "1. Tap 'Open Battery Settings' below",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "2. Click 'Allow' when prompted",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "3. Return to this app",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = "💡 After you return, we'll guide you through the second setting!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Open Battery Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

enum class ExportScope(val displayName: String) {
    TODAY("Today"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    ALL_TIME("All Time")
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    PowerCutMonitorTheme {
        MainScreen()
    }
}