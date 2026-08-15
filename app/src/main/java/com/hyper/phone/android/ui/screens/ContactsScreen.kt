package com.hyper.phone.android.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Contact(val id: String, val name: String, val number: String)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContactsScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var selectedContacts by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    val listState = rememberLazyListState()

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch {
                    contacts = loadContacts(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        contacts = loadContacts(context)
    }

    val groupedContacts = remember(contacts) {
        contacts.groupBy { 
            val firstChar = it.name.firstOrNull()?.uppercaseChar()
            if (firstChar != null && firstChar.isLetter()) firstChar.toString() else "#"
        }.toSortedMap()
    }
    
    val alphabet = remember(groupedContacts) { groupedContacts.keys.toList() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (selectedContacts.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedContacts = emptySet() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear selection")
                        }
                        Text("${selectedContacts.size} selected", modifier = Modifier.weight(1f))
                        IconButton(onClick = { /* Delete logic */ selectedContacts = emptySet() }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
            
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                item {
                    ListItem(
                        headlineContent = { Text("Create new contact", color = MaterialTheme.colorScheme.primary) },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.PersonAdd, 
                                    contentDescription = "Create new contact",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        },
                        modifier = Modifier.clickable {
                            navController.navigate(Screen.CreateContact.route)
                        }
                    )
                }

                if (contacts.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No contacts found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    groupedContacts.forEach { (initial, contactsForInitial) ->
                        stickyHeader(key = initial) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f))
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = initial,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        items(contactsForInitial, key = { it.id }) { contact ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                positionalThreshold = { totalDistance -> totalDistance * 0.4f },
                                confirmValueChange = { dismissValue ->
                                    when (dismissValue) {
                                        SwipeToDismissBoxValue.StartToEnd -> {
                                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${contact.number}"))
                                            context.startActivity(intent)
                                            false
                                        }
                                        SwipeToDismissBoxValue.EndToStart -> {
                                            navController.navigate(Screen.ContactInfo.createRoute(contact.name, contact.number))
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
                                    val icon = when (direction) {
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
                                        if (icon != null) {
                                            Icon(icon, contentDescription = null, tint = Color.White)
                                        }
                                    }
                                }
                            ) {
                                ListItem(
                                    headlineContent = { Text(contact.name, style = MaterialTheme.typography.bodyLarge) },
                                    leadingContent = {
                                        val avatarColor = com.hyper.phone.android.utils.ColorUtils.getAvatarColor(contact.name)
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(avatarColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = contact.name.firstOrNull()?.toString()?.uppercase() ?: "?",
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .background(if (selectedContacts.contains(contact.id)) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                                        .combinedClickable(
                                            onClick = {
                                                if (selectedContacts.isNotEmpty()) {
                                                    if (selectedContacts.contains(contact.id)) {
                                                        selectedContacts = selectedContacts - contact.id
                                                    } else {
                                                        selectedContacts = selectedContacts + contact.id
                                                    }
                                                } else {
                                                    navController.navigate(Screen.ContactInfo.createRoute(contact.name, contact.number))
                                                }
                                            },
                                            onLongClick = {
                                                if (selectedContacts.contains(contact.id)) {
                                                    selectedContacts = selectedContacts - contact.id
                                                } else {
                                                    selectedContacts = selectedContacts + contact.id
                                                }
                                            }
                                        )
                                )
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

        // A-Z Scrubber
        if (alphabet.isNotEmpty()) {
            var isDragging by remember { mutableStateOf(false) }
            var currentLetterIndex by remember { mutableIntStateOf(-1) }
            
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
                    .pointerInput(alphabet) {
                        detectVerticalDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = { 
                                isDragging = false 
                                currentLetterIndex = -1
                            },
                            onDragCancel = { 
                                isDragging = false
                                currentLetterIndex = -1
                            }
                        ) { change, _ ->
                            val heightPerItem = size.height / alphabet.size.toFloat()
                            val index = (change.position.y / heightPerItem).toInt().coerceIn(0, alphabet.lastIndex)
                            
                            if (index != currentLetterIndex) {
                                currentLetterIndex = index
                                val letter = alphabet[index]
                                // Find item index in LazyColumn
                                var itemIndex = 1 // Offset for "Create contact" item
                                for ((key, value) in groupedContacts) {
                                    if (key == letter) break
                                    // +1 for header, + size of items
                                    itemIndex += 1 + value.size 
                                }
                                coroutineScope.launch {
                                    listState.scrollToItem(itemIndex)
                                }
                            }
                        }
                    },
                verticalArrangement = Arrangement.Center
            ) {
                alphabet.forEachIndexed { index, letter ->
                    Text(
                        text = letter,
                        fontSize = 11.sp,
                        fontWeight = if (index == currentLetterIndex) FontWeight.Bold else FontWeight.Medium,
                        color = if (index == currentLetterIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(vertical = 1.dp, horizontal = 4.dp)
                            .clickable {
                                currentLetterIndex = index
                                var itemIndex = 1
                                for ((key, value) in groupedContacts) {
                                    if (key == letter) break
                                    itemIndex += 1 + value.size 
                                }
                                coroutineScope.launch {
                                    listState.animateScrollToItem(itemIndex)
                                }
                            }
                    )
                }
            }
        }
    }
}

suspend fun loadContacts(context: Context): List<Contact> = withContext(Dispatchers.IO) {
    val contactsList = mutableListOf<Contact>()
    val seenNames = mutableSetOf<String>()

    try {
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val id = if (idIndex >= 0) it.getString(idIndex) else ""
                val name = if (nameIndex >= 0) it.getString(nameIndex) ?: "Unknown" else "Unknown"
                val number = if (numberIndex >= 0) it.getString(numberIndex) ?: "" else ""
                
                if (name !in seenNames && number.isNotBlank()) {
                    seenNames.add(name)
                    contactsList.add(Contact(id, name, number))
                }
            }
        }
    } catch (e: SecurityException) {
        // Permission not granted
    }

    contactsList
}
