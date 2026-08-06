package com.secureehr.app.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login") {
        const val ROUTE_PATTERN = "login?email={email}"
        fun routeWithEmail(email: String): String =
            "login?email=${java.net.URLEncoder.encode(email, "UTF-8")}"
    }
    object Register : Screen("register")
    object Dashboard : Screen("dashboard")
    object Records : Screen("records")
    object HospitalFinder : Screen("hospitals")
    object AIChat : Screen("aichat")
    object Consent : Screen("consent")
    object Visits : Screen("visits")
    object Statistics : Screen("statistics")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object About : Screen("about")
    object HospitalHistory : Screen("hospital_history")
    object BlockchainStatus : Screen("blockchain_status")
    // Doctor screens
    object DoctorDashboard : Screen("doctor_dashboard")
    object DoctorPatients : Screen("doctor_patients")
    object DoctorSearch : Screen("doctor_search")
    object DoctorConsents : Screen("doctor_consents")
    object PatientDetail : Screen("patient_detail/{patientId}/{patientName}") {
        fun route(patientId: Int, patientName: String): String {
            val encodedName = java.net.URLEncoder.encode(patientName, "UTF-8")
            return "patient_detail/$patientId/$encodedName"
        }
    }
}
