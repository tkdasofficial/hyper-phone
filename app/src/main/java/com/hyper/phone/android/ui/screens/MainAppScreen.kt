package com.hyper.phone.android.ui.screens

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.navigation.NavController

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Favorites : Screen("favorites", "Favorites", Icons.Filled.Star)
    object Recents : Screen("recents", "Recents", Icons.Filled.History)
    object Contacts : Screen("contacts", "Contacts", Icons.Filled.Contacts)
    object Dialpad : Screen("dialpad", "Dialpad", Icons.Filled.Dialpad)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
    object CreateContact : Screen("create_contact", "Create Contact", Icons.Filled.PersonAdd)
    object ContactInfo : Screen("contact_info/{name}/{number}", "Contact Info", Icons.Filled.Info) {
        fun createRoute(name: String, number: String) = "contact_info/${android.net.Uri.encode(name)}/${android.net.Uri.encode(number)}"
    }
}

val items = listOf(
    Screen.Favorites,
    Screen.Recents,
    Screen.Contacts
)

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.CALL_PHONE
        )
    )

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            val context = LocalContext.current
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.75f)
            ) {
                Spacer(Modifier.height(12.dp))
                Text("Hyper Phone", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        navController.navigate(Screen.Settings.route)
                        scope.launch { drawerState.close() }
                    },
                    icon = { 
                        com.hyper.phone.android.ui.components.VibrantBadge(
                            icon = Icons.Filled.Settings,
                            gradient = com.hyper.phone.android.ui.theme.ElectricIndigoGradient,
                            isActive = true,
                            showHalo = false,
                            coreSize = 36.dp,
                            iconSize = 20.dp
                        )
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Terms & Conditions") },
                    selected = false,
                    onClick = {
                        android.widget.Toast.makeText(context, "Terms & Conditions", android.widget.Toast.LENGTH_SHORT).show()
                        scope.launch { drawerState.close() }
                    },
                    icon = { 
                        com.hyper.phone.android.ui.components.VibrantBadge(
                            icon = Icons.Filled.Description,
                            gradient = com.hyper.phone.android.ui.theme.MintEmeraldGradient,
                            isActive = true,
                            showHalo = false,
                            coreSize = 36.dp,
                            iconSize = 20.dp
                        )
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Privacy Policy") },
                    selected = false,
                    onClick = {
                        android.widget.Toast.makeText(context, "Privacy Policy", android.widget.Toast.LENGTH_SHORT).show()
                        scope.launch { drawerState.close() }
                    },
                    icon = { 
                        com.hyper.phone.android.ui.components.VibrantBadge(
                            icon = Icons.Filled.PrivacyTip,
                            gradient = com.hyper.phone.android.ui.theme.AmberGoldGradient,
                            isActive = true,
                            showHalo = false,
                            coreSize = 36.dp,
                            iconSize = 20.dp
                        )
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Help & Support") },
                    selected = false,
                    onClick = {
                        android.widget.Toast.makeText(context, "Help & Support", android.widget.Toast.LENGTH_SHORT).show()
                        scope.launch { drawerState.close() }
                    },
                    icon = { 
                        com.hyper.phone.android.ui.components.VibrantBadge(
                            icon = Icons.Filled.Help,
                            gradient = com.hyper.phone.android.ui.theme.SkyBlueGradient,
                            isActive = true,
                            showHalo = false,
                            coreSize = 36.dp,
                            iconSize = 20.dp
                        )
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                
                if (currentRoute in listOf(Screen.Favorites.route, Screen.Recents.route, Screen.Contacts.route)) {
                    PhoneSearchBar(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onProfileClick = { /* Profile click */ },
                        navController = navController
                    )
                }
            },
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                
                if (currentRoute != Screen.Settings.route) {
                    NavigationBar(containerColor = Color.Transparent) {
                        val currentDestination = navBackStackEntry?.destination
                        items.forEach { screen ->
                            val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                            val gradient = when (screen) {
                                Screen.Recents -> com.hyper.phone.android.ui.theme.MintEmeraldGradient
                                Screen.Dialpad -> com.hyper.phone.android.ui.theme.ElectricIndigoGradient
                                Screen.Contacts -> com.hyper.phone.android.ui.theme.AmberGoldGradient
                                Screen.Favorites -> com.hyper.phone.android.ui.theme.MagentaRoseGradient
                                else -> com.hyper.phone.android.ui.theme.ElectricIndigoGradient
                            }
                            NavigationBarItem(
                                icon = { 
                                    com.hyper.phone.android.ui.components.VibrantBadge(
                                        icon = screen.icon,
                                        gradient = gradient,
                                        isActive = isSelected
                                    ) 
                                },
                                label = null, // Badges don't use labels according to the spec, keeping it clean
                                selected = isSelected,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = Color.Transparent,
                                    selectedIconColor = Color.Transparent,
                                    unselectedIconColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                if (currentRoute != Screen.Settings.route) {
                    com.hyper.phone.android.ui.components.SpringButton(
                        onClick = {
                            if (currentRoute == Screen.Dialpad.route) {
                                navController.popBackStack()
                            } else {
                                navController.navigate(Screen.Dialpad.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    ) {
                        com.hyper.phone.android.ui.components.VibrantBadge(
                            icon = if (currentRoute == Screen.Dialpad.route) Icons.Filled.Close else Icons.Filled.Dialpad,
                            gradient = if (currentRoute == Screen.Dialpad.route) com.hyper.phone.android.ui.theme.FrostedSlateGradient else com.hyper.phone.android.ui.theme.ElectricIndigoGradient,
                            isActive = true
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(navController, startDestination = Screen.Recents.route, Modifier.padding(innerPadding)) {
                composable(Screen.Favorites.route) { FavoritesScreen(navController) }
                composable(Screen.Recents.route) { RecentsScreen(navController) }
                composable(Screen.Contacts.route) { ContactsScreen(navController) }
                composable(Screen.Dialpad.route) { DialpadScreen(navController) }
                composable(Screen.Settings.route) { SettingsScreen(onBack = { navController.popBackStack() }) }
                composable(
                    route = Screen.CreateContact.route + "?phone={phone}",
                    arguments = listOf(androidx.navigation.navArgument("phone") { 
                        type = androidx.navigation.NavType.StringType 
                        defaultValue = ""
                    })
                ) { backStackEntry -> 
                    val phone = backStackEntry.arguments?.getString("phone") ?: ""
                    CreateContactScreen(navController, phone) 
                }
                composable(
                    route = Screen.ContactInfo.route,
                    arguments = listOf(
                        androidx.navigation.navArgument("name") { type = androidx.navigation.NavType.StringType },
                        androidx.navigation.navArgument("number") { type = androidx.navigation.NavType.StringType }
                    )
                ) { backStackEntry ->
                    val name = backStackEntry.arguments?.getString("name") ?: ""
                    val number = backStackEntry.arguments?.getString("number") ?: ""
                    ContactInfoScreen(navController, name, number)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneSearchBar(onMenuClick: () -> Unit, onProfileClick: () -> Unit, navController: NavController) {
    var query by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var searchResults by remember { mutableStateOf<List<T9Contact>>(emptyList()) }

    LaunchedEffect(query, active) {
        if (active && query.isNotEmpty()) {
            val contacts = loadT9Contacts(context)
            searchResults = contacts.filter { 
                it.name.contains(query, ignoreCase = true) || com.hyper.phone.android.utils.PhoneNumberUtils.normalize(it.number).contains(com.hyper.phone.android.utils.PhoneNumberUtils.normalize(query)) 
            }
        } else {
            searchResults = emptyList()
        }
    }

    SearchBar(
        query = query,
        onQueryChange = { query = it },
        onSearch = { active = false },
        active = active,
        onActiveChange = { active = it },
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = if (active) 0.dp else 16.dp, vertical = if (active) 0.dp else 8.dp),
        placeholder = { Text("Search contacts & places") },
        leadingIcon = {
            if (active) {
                IconButton(onClick = { active = false; query = "" }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            } else {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Filled.Menu, "Menu")
                }
            }
        },
        trailingIcon = {
            if (!active) {
                IconButton(onClick = onProfileClick) {
                    Icon(Icons.Filled.AccountCircle, "Profile", modifier = Modifier.size(32.dp))
                }
            } else if (query.isNotEmpty()) {
                IconButton(onClick = { query = "" }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear")
                }
            }
        }
    ) {
        LazyColumn {
            items(searchResults.size) { index ->
                val contact = searchResults[index]
                ListItem(
                    headlineContent = { Text(contact.name) },
                    supportingContent = { Text(contact.number) },
                    leadingContent = { Icon(Icons.Filled.Person, contentDescription = null) },
                    modifier = Modifier.clickable {
                        active = false
                        query = ""
                        navController.navigate(Screen.ContactInfo.createRoute(contact.name, contact.number))
                    }
                )
            }
        }
    }
}
