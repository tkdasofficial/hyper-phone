package com.hyper.phone.android.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.Color

data class DialKey(val number: String, val letters: String)
data class T9Contact(val name: String, val number: String)

val keys = listOf(
    DialKey("1", ""), DialKey("2", "A B C"), DialKey("3", "D E F"),
    DialKey("4", "G H I"), DialKey("5", "J K L"), DialKey("6", "M N O"),
    DialKey("7", "P Q R S"), DialKey("8", "T U V"), DialKey("9", "W X Y Z"),
    DialKey("*", ""), DialKey("0", "+"), DialKey("#", "")
)

@Composable
fun DialpadScreen(navController: NavController) {
    var number by remember { mutableStateOf("") }
    val context = LocalContext.current
    var allContacts by remember { mutableStateOf<List<T9Contact>>(emptyList()) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    allContacts = loadT9Contacts(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        allContacts = loadT9Contacts(context)
    }

    val matchedContacts = remember(number, allContacts) {
        if (number.isEmpty()) emptyList()
        else allContacts.filter { com.hyper.phone.android.utils.PhoneNumberUtils.normalize(it.number).contains(number) }.take(3)
    }

    Column(
        modifier = Modifier
            .fillMaxSize(), // Removed bottom padding so the Call button aligns with the floating Close (X) button
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Space for T9 Search Results
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (number.isNotEmpty()) {
                val exactMatchExists = matchedContacts.any { com.hyper.phone.android.utils.PhoneNumberUtils.normalize(it.number) == number }
                if (matchedContacts.isNotEmpty() || !exactMatchExists) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        if (!exactMatchExists) {
                            item {
                                ListItem(
                                    headlineContent = { Text("Create new contact", color = MaterialTheme.colorScheme.primary) },
                                    leadingContent = {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color.Transparent),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    modifier = Modifier.clickable { 
                                        navController.navigate(Screen.CreateContact.route + "?phone=${Uri.encode(number)}")
                                    }
                                )
                                HorizontalDivider(modifier = Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                        items(matchedContacts) { contact ->
                            ListItem(
                                headlineContent = { Text(contact.name, fontWeight = FontWeight.SemiBold) },
                                supportingContent = { Text(contact.number) },
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                },
                                modifier = Modifier.clickable { number = com.hyper.phone.android.utils.PhoneNumberUtils.normalize(contact.number) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        // Number display area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp) // Compressed from 80.dp to leave more space for suggestions
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFeatureSettings = "tnum"
                ),
                color = com.hyper.phone.android.ui.theme.PearlWhite,
                fontWeight = FontWeight.Medium,
                fontSize = 40.sp,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 64.dp) // more space for the backspace badge
            )
            
            if (number.isNotEmpty()) {
                com.hyper.phone.android.ui.components.SpringButton(
                    onClick = { number = number.dropLast(1) },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    com.hyper.phone.android.ui.components.VibrantBadge(
                        icon = Icons.Filled.Backspace,
                        gradient = com.hyper.phone.android.ui.theme.FrostedSlateGradient,
                        isActive = true
                    )
                }
            }
        }

        // Dialpad Grid
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            for (row in keys.chunked(3)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (key in row) {
                        DialpadButton(key) {
                            number += it.number
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp)) // Compressed slightly

        // Call Button row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .padding(bottom = 16.dp) // Bottom margin perfectly aligns with the Scaffold's Close (X) FAB
                .height(76.dp)
        ) {
            com.hyper.phone.android.ui.components.SpringButton(
                onClick = {
                    if (number.isNotEmpty()) {
                        context.safeMakeCall(number)
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                com.hyper.phone.android.ui.components.VibrantBadge(
                    icon = Icons.Filled.Call,
                    gradient = com.hyper.phone.android.ui.theme.EmeraldNeonGradient,
                    isActive = true,
                    modifier = Modifier.size(76.dp) // Make the call button badge larger
                )
            }
        }
    }
}

suspend fun loadT9Contacts(context: Context): List<T9Contact> = withContext(Dispatchers.IO) {
    val contactsList = mutableListOf<T9Contact>()
    try {
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
            null,
            null,
            null
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val name = if (nameIndex >= 0) it.getString(nameIndex) ?: "Unknown" else "Unknown"
                val number = if (numberIndex >= 0) it.getString(numberIndex) ?: "" else ""
                contactsList.add(T9Contact(name, number))
            }
        }
    } catch (e: SecurityException) {
        // Permission not granted
    }
    contactsList
}

@Composable
fun DialpadButton(key: DialKey, onClick: (DialKey) -> Unit) {
    com.hyper.phone.android.ui.components.SpringButton(
        onClick = { onClick(key) }
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(com.hyper.phone.android.ui.theme.FrostedGlass)
                .border(1.dp, com.hyper.phone.android.ui.theme.EdgeStroke, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = key.number, 
                    fontSize = 32.sp, 
                    fontWeight = FontWeight.SemiBold,
                    color = com.hyper.phone.android.ui.theme.PearlWhite,
                    style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum")
                )
                if (key.letters.isNotEmpty()) {
                    Text(
                        text = key.letters,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = com.hyper.phone.android.ui.theme.SlateGray
                    )
                }
            }
        }
    }
}
