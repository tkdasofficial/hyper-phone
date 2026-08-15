package com.hyper.phone.android.ui.screens

import android.content.ContentProviderOperation
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateContactScreen(navController: NavController, initialPhone: String = "") {
    var firstName by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf(initialPhone) }
    var email by remember { mutableStateOf("") }
    var birthday by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var showAddressField by remember { mutableStateOf(false) }
    var showEmailField by remember { mutableStateOf(false) }
    var showBirthdayField by remember { mutableStateOf(false) }
    
    val countryCodes = listOf(
        Triple("🇮🇳", "+91", "India"),
        Triple("🇺🇸", "+1", "US"),
        Triple("🇬🇧", "+44", "UK"),
        Triple("🇷🇺", "+7", "Russia"),
        Triple("🇯🇵", "+81", "Japan"),
        Triple("🇨🇳", "+86", "China")
    )
    var selectedCountry by remember { mutableStateOf(countryCodes[0]) }
    var countryDropdownExpanded by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Contact", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("Cancel", fontSize = MaterialTheme.typography.titleMedium.fontSize)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (firstName.isNotBlank() && phone.isNotBlank()) {
                                val fullName = listOf(firstName, surname).filter { it.isNotBlank() }.joinToString(" ")
                                val fullPhone = "${selectedCountry.second} $phone"
                                coroutineScope.launch {
                                    val success = saveContact(context, fullName, fullPhone, email, birthday, address, note)
                                    if (success) {
                                        Toast.makeText(context, "Contact saved", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    } else {
                                        Toast.makeText(context, "Failed to save contact", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Please enter first name and phone", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleMedium.fontSize)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))
            // Avatar
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { imagePicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = "Add photo",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Add Photo", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Group 1: Name & Company
            SettingsGroup {
                CustomTextField(value = firstName, onValueChange = { firstName = it }, placeholder = "First name")
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                CustomTextField(value = surname, onValueChange = { surname = it }, placeholder = "Last name")
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                CustomTextField(value = company, onValueChange = { company = it }, placeholder = "Company")
            }

            // Group 2: Phone
            SettingsGroup {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { countryDropdownExpanded = true }
                                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                        ) {
                            Text(selectedCountry.first)
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(4.dp))
                            Text(selectedCountry.second, color = MaterialTheme.colorScheme.onSurface)
                        }
                        DropdownMenu(
                            expanded = countryDropdownExpanded,
                            onDismissRequest = { countryDropdownExpanded = false }
                        ) {
                            countryCodes.forEach { country ->
                                DropdownMenuItem(
                                    text = { Text("${country.first} ${country.third} (${country.second})") },
                                    onClick = {
                                        selectedCountry = country
                                        countryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    CustomTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        placeholder = "Phone",
                        keyboardType = KeyboardType.Phone,
                        modifier = Modifier.weight(1f)
                    )
                    if (phone.isNotEmpty()) {
                        IconButton(onClick = { phone = "" }, modifier = Modifier.padding(end = 4.dp)) {
                            Icon(Icons.Filled.Cancel, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Group 3: Notes
            SettingsGroup {
                TextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("Notes", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }

            // Group 4: Add more info
            SettingsGroup {
                if (!showAddressField) {
                    ListItem(
                        headlineContent = { Text("Add address", color = MaterialTheme.colorScheme.primary) },
                        leadingContent = { 
                            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                            }
                        },
                        modifier = Modifier.clickable { showAddressField = true },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                } else {
                    CustomTextField(value = address, onValueChange = { address = it }, placeholder = "Address")
                }
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                if (!showEmailField) {
                    ListItem(
                        headlineContent = { Text("Add email", color = MaterialTheme.colorScheme.primary) },
                        leadingContent = { 
                            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                            }
                        },
                        modifier = Modifier.clickable { showEmailField = true },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                } else {
                    CustomTextField(value = email, onValueChange = { email = it }, placeholder = "Email", keyboardType = KeyboardType.Email)
                }
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                if (!showBirthdayField) {
                    ListItem(
                        headlineContent = { Text("Add birthday", color = MaterialTheme.colorScheme.primary) },
                        leadingContent = { 
                            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                            }
                        },
                        modifier = Modifier.clickable { showBirthdayField = true },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                } else {
                    CustomTextField(value = birthday, onValueChange = { birthday = it }, placeholder = "Birthday (YYYY-MM-DD)")
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun CustomTextField(
    value: String, 
    onValueChange: (String) -> Unit, 
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
        singleLine = true
    )
}

suspend fun saveContact(context: Context, name: String, phone: String, email: String = "", birthday: String = "", address: String = "", note: String = ""): Boolean = withContext(Dispatchers.IO) {
    try {
        val ops = ArrayList<ContentProviderOperation>()
        val rawContactInsertIndex = ops.size
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build()
        )

        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                .build()
        )

        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build()
        )

        if (email.isNotBlank()) {
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Email.DATA, email)
                    .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                    .build()
            )
        }

        if (birthday.isNotBlank()) {
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Event.START_DATE, birthday)
                    .withValue(ContactsContract.CommonDataKinds.Event.TYPE, ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY)
                    .build()
            )
        }

        if (address.isNotBlank()) {
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, address)
                    .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME)
                    .build()
            )
        }

        if (note.isNotBlank()) {
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Note.NOTE, note)
                    .build()
            )
        }

        context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
