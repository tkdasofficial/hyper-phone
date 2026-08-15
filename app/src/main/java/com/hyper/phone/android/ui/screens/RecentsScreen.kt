package com.hyper.phone.android.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CallLog
import android.text.format.DateUtils
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CallLogEntry(
    val number: String,
    val name: String?,
    val type: Int,
    val date: Long,
    val duration: Long
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RecentsScreen(navController: NavController) {
    val context = LocalContext.current
    var callLogs by remember { mutableStateOf<List<CallLogEntry>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedLogs by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val filters = listOf("All", "Missed", "Incoming", "Outgoing", "Blocked")

    val scope = rememberCoroutineScope()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                scope.launch(Dispatchers.Main) {
                    callLogs = loadCallLogs(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        callLogs = loadCallLogs(context)
    }

    val filteredLogs = callLogs.filter { log ->
        when (selectedFilter) {
            "Missed" -> log.type == CallLog.Calls.MISSED_TYPE || log.type == CallLog.Calls.REJECTED_TYPE
            "Incoming" -> log.type == CallLog.Calls.INCOMING_TYPE
            "Outgoing" -> log.type == CallLog.Calls.OUTGOING_TYPE
            "Blocked" -> log.type == CallLog.Calls.BLOCKED_TYPE
            else -> true
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (selectedLogs.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedLogs = emptySet() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear selection")
                    }
                    Text("${selectedLogs.size} selected", modifier = Modifier.weight(1f))
                    IconButton(onClick = { /* Delete logic */ selectedLogs = emptySet() }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                }
            }
        } else {
            // iOS Style Segmented Control-ish Header
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { filter ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { selectedFilter = filter },
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                text = filter, 
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            if (filteredLogs.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No recent calls.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(filteredLogs, key = { it.date }) { log ->
                    val isMissed = log.type == CallLog.Calls.MISSED_TYPE || log.type == CallLog.Calls.REJECTED_TYPE || log.type == CallLog.Calls.BLOCKED_TYPE
                    val icon = when (log.type) {
                        CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
                        CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
                        CallLog.Calls.MISSED_TYPE, CallLog.Calls.REJECTED_TYPE, CallLog.Calls.BLOCKED_TYPE -> Icons.AutoMirrored.Filled.CallMissed
                        else -> Icons.AutoMirrored.Filled.CallReceived
                    }
                    val textColor = if (isMissed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    
                    val dismissState = rememberSwipeToDismissBoxState(
                        positionalThreshold = { totalDistance -> totalDistance * 0.4f },
                        confirmValueChange = { dismissValue ->
                            when (dismissValue) {
                                SwipeToDismissBoxValue.StartToEnd -> {
                                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${log.number}"))
                                    try { context.startActivity(intent) } catch (e: Exception) { android.widget.Toast.makeText(context, "Error making call", android.widget.Toast.LENGTH_SHORT).show() }
                                    false
                                }
                                SwipeToDismissBoxValue.EndToStart -> {
                                    navController.navigate(Screen.ContactInfo.createRoute(log.name ?: log.number, log.number))
                                    false
                                }
                                else -> false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val direction = dismissState.dismissDirection
                            val color by animateColorAsState(
                                when (dismissState.targetValue) {
                                    SwipeToDismissBoxValue.StartToEnd -> Color(0xFF4CAF50)
                                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.primary
                                    else -> Color.Transparent
                                }
                            )
                            val alignment = when (direction) {
                                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                else -> Alignment.Center
                            }
                            val bgIcon = when (direction) {
                                SwipeToDismissBoxValue.StartToEnd -> Icons.Filled.Message
                                SwipeToDismissBoxValue.EndToStart -> Icons.Filled.Info
                                else -> null
                            }

                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(color)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = alignment
                            ) {
                                if (bgIcon != null) {
                                    Icon(bgIcon, contentDescription = null, tint = Color.White)
                                }
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (selectedLogs.contains(log.date)) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background)
                                .combinedClickable(
                                    onClick = {
                                        if (selectedLogs.isNotEmpty()) {
                                            if (selectedLogs.contains(log.date)) {
                                                selectedLogs = selectedLogs - log.date
                                            } else {
                                                selectedLogs = selectedLogs + log.date
                                            }
                                        } else {
                                            // Action to call
                                            context.safeMakeCall(log.number)
                                        }
                                    },
                                    onLongClick = {
                                        if (selectedLogs.contains(log.date)) {
                                            selectedLogs = selectedLogs - log.date
                                        } else {
                                            selectedLogs = selectedLogs + log.date
                                        }
                                    }
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Initial Avatar
                            val avatarColor = com.hyper.phone.android.utils.ColorUtils.getAvatarColor(log.name ?: log.number)
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(avatarColor),
                                contentAlignment = Alignment.Center
                            ) {
                                val initial = log.name?.firstOrNull()?.toString()?.uppercase() ?: log.number.firstOrNull()?.toString() ?: "?"
                                Text(
                                    text = initial,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            // Name/Number & Type
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = log.name ?: log.number,
                                    color = textColor,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = icon, 
                                        contentDescription = null, 
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant, 
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Mobile", // Placeholder for actual type lookup
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            
                            // Date & Info
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = DateUtils.getRelativeTimeSpanString(log.date).toString(),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { navController.navigate(Screen.ContactInfo.createRoute(log.name ?: log.number, log.number)) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = "Info",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

suspend fun loadCallLogs(context: Context): List<CallLogEntry> = withContext(Dispatchers.IO) {
    val logs = mutableListOf<CallLogEntry>()
    val resolvedCache = mutableMapOf<String, String?>()
    try {
        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
            ),
            null,
            null,
            CallLog.Calls.DATE + " DESC LIMIT 100"
        )

        cursor?.use {
            val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
            val nameIndex = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)
            val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
            val durationIndex = it.getColumnIndex(CallLog.Calls.DURATION)

            while (it.moveToNext()) {
                val number = if (numberIndex >= 0) it.getString(numberIndex) else ""
                var name = if (nameIndex >= 0) it.getString(nameIndex) else null
                
                if (number.isNotBlank()) {
                    if (resolvedCache.containsKey(number)) {
                        name = resolvedCache[number] ?: name
                    } else {
                        val resolved = com.hyper.phone.android.utils.ContactResolver.resolveContact(context, number)
                        val resolvedName = resolved?.name?.takeIf { it.isNotBlank() }
                        resolvedCache[number] = resolvedName
                        if (resolvedName != null) {
                            name = resolvedName
                        }
                    }
                }
                
                val type = if (typeIndex >= 0) it.getInt(typeIndex) else 0
                val date = if (dateIndex >= 0) it.getLong(dateIndex) else 0L
                val duration = if (durationIndex >= 0) it.getLong(durationIndex) else 0L

                logs.add(CallLogEntry(number, name, type, date, duration))
            }
        }
    } catch (e: SecurityException) {
        // Permission not granted
    }
    logs
}
