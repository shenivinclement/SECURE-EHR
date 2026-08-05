package com.secureehr.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.secureehr.app.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    title: String,
    navController: NavController,
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "SecureEHR Menu",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Divider()
                
                val menuItems = listOf(
                    NavigationItem("Home", Icons.Default.Home, Screen.Dashboard.route),
                    NavigationItem("Records", Icons.Default.Description, Screen.Records.route),
                    NavigationItem("Hospitals", Icons.Default.LocalHospital, Screen.HospitalFinder.route),
                    NavigationItem("Consent", Icons.Default.AssignmentInd, Screen.Consent.route),
                    NavigationItem("AI Chat", Icons.Default.Chat, Screen.AIChat.route),
                    NavigationItem("Visits", Icons.Default.Timeline, Screen.Visits.route),
                    NavigationItem("Statistics", Icons.Default.BarChart, Screen.Statistics.route),
                    NavigationItem("Profile", Icons.Default.Person, Screen.Profile.route),
                    NavigationItem("Settings", Icons.Default.Settings, Screen.Settings.route),
                    NavigationItem("About", Icons.Default.Info, Screen.About.route),
                    NavigationItem("Sign Out", Icons.Default.Logout, Screen.Login.route)
                )

                menuItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (item.label == "Sign Out") {
                                navController.navigate(item.route) {
                                    popUpTo(0)
                                }
                            } else {
                                navController.navigate(item.route)
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                        navigationIconContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            content = content
        )
    }
}

data class NavigationItem(val label: String, val icon: ImageVector, val route: String)
