package com.secureehr.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.secureehr.app.ui.screens.*

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) { SplashScreen(navController) }
        composable(Screen.Login.route) { LoginScreen(navController) }
        composable(Screen.Register.route) { RegisterScreen(navController) }
        composable(Screen.Dashboard.route) { DashboardScreen(navController) }
        composable(Screen.Records.route) { RecordsScreen(navController) }
        composable(Screen.HospitalFinder.route) { HospitalFinderScreen(navController) }
        composable(Screen.AIChat.route) { AIChatScreen(navController) }
        composable(Screen.Consent.route) { ConsentScreen(navController) }
        composable(Screen.Visits.route) { VisitsScreen(navController) }
        composable(Screen.Statistics.route) { StatisticsScreen(navController) }
        composable(Screen.Profile.route) { ProfileScreen(navController) }
        composable(Screen.Settings.route) { SettingsScreen(navController) }
        composable(Screen.About.route) { AboutScreen(navController) }
        composable(Screen.HospitalHistory.route) { HospitalHistoryScreen(navController) }
        composable(Screen.BlockchainStatus.route) { BlockchainStatusScreen(navController) }
        // Doctor screens
        composable(Screen.DoctorDashboard.route) { DoctorDashboardScreen(navController) }
        composable(Screen.DoctorPatients.route) { DoctorPatientsScreen(navController) }
        composable(Screen.DoctorSearch.route) { DoctorSearchScreen(navController) }
        composable(Screen.DoctorConsents.route) { DoctorConsentsScreen(navController) }
        composable(
            route = Screen.PatientDetail.route,
            arguments = listOf(
                navArgument("patientId") { type = NavType.IntType },
                navArgument("patientName") { type = NavType.StringType; defaultValue = "Patient" }
            )
        ) { backStackEntry ->
            PatientDetailScreen(
                navController = navController,
                patientId = backStackEntry.arguments?.getInt("patientId") ?: 0,
                patientName = backStackEntry.arguments?.getString("patientName") ?: "Patient"
            )
        }
    }
}
