package com.secureehr.app.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.*

interface ApiService {
    @POST("/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("/auth/register")
    suspend fun register(@Body requ3est: RegisterRequest): RegisterResponse

    @PUT("/auth/password")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): GenericResponse

    @GET("/records")
    suspend fun getRecords(@Header("Authorization") token: String): List<MedicalRecord>

    @POST("/records")
    suspend fun createRecord(
        @Header("Authorization") token: String,
        @Body record: MedicalRecord
    ): MedicalRecord

    @GET("/hospitals")
    suspend fun getHospitals(
        @Header("Authorization") token: String,
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("condition") condition: String? = null
    ): List<Hospital>

    @GET("/visits")
    suspend fun getVisits(@Header("Authorization") token: String): List<Visit>

    @POST("/ai/chat")
    suspend fun chat(@Header("Authorization") token: String, @Body request: ChatRequest): ChatResponse

    @GET("/consent")
    suspend fun getConsents(@Header("Authorization") token: String): List<Consent>

    @POST("/consent/grant")
    suspend fun grantConsent(
        @Header("Authorization") token: String,
        @Body request: ConsentGrantRequest
    ): Consent

    @POST("/consent/revoke")
    suspend fun revokeConsent(
        @Header("Authorization") token: String,
        @Body request: ConsentActionRequest
    ): Consent

    @GET("/auth/me")
    suspend fun getMyProfile(@Header("Authorization") token: String): UserProfile

    @PATCH("/auth/me/two-factor")
    suspend fun updateTwoFactor(
        @Header("Authorization") token: String,
        @Body request: TwoFactorRequest
    ): UserProfile

    @GET("/patients/me")
    suspend fun getMyPatient(@Header("Authorization") token: String): PatientProfile

    @PUT("/patients/me")
    suspend fun updateMyPatient(
        @Header("Authorization") token: String,
        @Body request: PatientUpdateRequest
    ): PatientProfile

    @PATCH("/patients/me/research-sharing")
    suspend fun updateResearchSharing(
        @Header("Authorization") token: String,
        @Body request: ResearchSharingRequest
    ): PatientProfile

    // Doctor endpoints
    @GET("/doctor/dashboard")
    suspend fun getDoctorDashboard(@Header("Authorization") token: String): DoctorDashboardData

    @GET("/doctor/patients")
    suspend fun getDoctorPatients(@Header("Authorization") token: String): List<DoctorPatient>

    @GET("/doctor/patients/{patientId}/records")
    suspend fun getPatientRecords(
        @Header("Authorization") token: String,
        @Path("patientId") patientId: Int
    ): List<MedicalRecord>

    @GET("/doctor/patients/{patientId}/visits")
    suspend fun getPatientVisits(
        @Header("Authorization") token: String,
        @Path("patientId") patientId: Int
    ): List<Visit>

    @GET("/doctor/search")
    suspend fun searchPatients(
        @Header("Authorization") token: String,
        @Query("q") query: String
    ): List<PatientSearchResult>

    @GET("/doctor/consents")
    suspend fun getDoctorConsents(@Header("Authorization") token: String): List<DoctorConsentView>
}

data class LoginRequest(val email: String, val password: String)

data class LoginResponse(val access_token: String, val token_type: String)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String
)

data class RegisterResponse(val message: String)

data class ChangePasswordRequest(
    @SerializedName("current_password") val currentPassword: String,
    @SerializedName("new_password") val newPassword: String
)

data class GenericResponse(val message: String)

data class MedicalRecord(
    @SerializedName("id") val id: String? = null,
    @SerializedName("diagnosis") val title: String? = null,
    @SerializedName("record_date") val date: String? = null,
    val doctor: String? = null,
    val hospital: String? = null,
    @SerializedName("notes") val notes: String? = null,
    val type: String? = null,
    @SerializedName("symptoms") val diagnosis: String? = null,
    @SerializedName("treatment") val treatment: String? = null,
    @SerializedName("medications") val prescription: String? = null
)

data class Hospital(
    val name: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val rating: Float = 0f,
    val specialty: String = "",
    @SerializedName("cure_rate") val cureRate: Float = 0f,
    val phone: String = "",
    val website: String = "",
    @SerializedName("opening_hours") val openingHours: String = "Mon–Fri: 8am–8pm, Sat: 9am–5pm",
    val specializations: List<String> = emptyList(),
    @SerializedName("ai_reason") val aiReason: String = ""
)

data class Visit(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("visit_date") val date: String? = null,
    @SerializedName("reason") val purpose: String? = null,
    @SerializedName("doctor_name") val doctor: String? = null,
    val status: String? = null,
    @SerializedName("hospital_name") val hospital: String? = null,
    val department: String? = null,
    val doctorSpecialization: String? = null,
    val diagnosis: String? = null,
    val treatment: String? = null,
    val medications: String? = null,
    val followUpDate: String? = null,
    val cost: String? = null,
    val insuranceClaimed: String? = null,
    val notes: String? = null,
    val endDate: String? = null
)

data class ChatRequest(val message: String)

data class ChatResponse(val response: String)

data class Consent(
    @SerializedName("id") val id: String? = null,
    @SerializedName("doctor_name") val doctor_name: String? = null,
    @SerializedName("hospital_name") val hospital_name: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("expiry_date") val expiry_date: String? = null,
    @SerializedName("specialization") val specialization: String? = null
)

data class ConsentActionRequest(@SerializedName("consent_id") val consentId: Int)

data class ConsentGrantRequest(
    @SerializedName("doctor_name") val doctorName: String,
    @SerializedName("hospital_name") val hospitalName: String,
    val specialization: String? = null,
    @SerializedName("expiry_date") val expiryDate: String? = null
)

data class UserProfile(
    val name: String? = null,
    val email: String? = null,
    val role: String? = null,
    @SerializedName("two_factor_enabled") val twoFactorEnabled: Boolean? = null
)

data class TwoFactorRequest(@SerializedName("two_factor_enabled") val twoFactorEnabled: Boolean)

data class PatientProfile(
    val name: String? = null,
    @SerializedName("date_of_birth") val dateOfBirth: String? = null,
    val gender: String? = null,
    val phone: String? = null,
    val address: String? = null,
    @SerializedName("blood_group") val bloodGroup: String? = null,
    val allergies: String? = null,
    @SerializedName("emergency_contact") val emergencyContact: String? = null,
    @SerializedName("research_data_sharing") val researchDataSharing: Boolean? = null
)

data class ResearchSharingRequest(@SerializedName("research_data_sharing") val researchDataSharing: Boolean)

data class PatientUpdateRequest(
    val name: String? = null,
    @SerializedName("date_of_birth") val dateOfBirth: String? = null,
    val gender: String? = null,
    val phone: String? = null,
    val address: String? = null,
    @SerializedName("blood_group") val bloodGroup: String? = null,
    val allergies: String? = null,
    @SerializedName("emergency_contact") val emergencyContact: String? = null
)

data class DoctorDashboardData(
    @SerializedName("active_patients") val activePatients: Int = 0,
    @SerializedName("total_consents") val totalConsents: Int = 0,
    @SerializedName("accessible_records") val accessibleRecords: Int = 0
)

data class DoctorPatient(
    @SerializedName("patient_id") val id: Int? = null,
    val name: String? = null,
    val gender: String? = null,
    @SerializedName("blood_group") val bloodGroup: String? = null,
    @SerializedName("date_of_birth") val dateOfBirth: String? = null,
    @SerializedName("primary_condition") val primaryCondition: String? = null,
    @SerializedName("total_records") val recordCount: Int? = null,
    @SerializedName("hospital_name") val hospitalName: String? = null,
    @SerializedName("consent_expiry_date") val expiryDate: String? = null
)

data class PatientSearchResult(
    val id: Int? = null,
    val name: String? = null,
    val gender: String? = null,
    @SerializedName("has_consent") val hasConsent: Boolean? = null
)

data class DoctorConsentView(
    val id: Int? = null,
    @SerializedName("patient_name") val patientName: String? = null,
    @SerializedName("patient_id") val patientId: Int? = null,
    val status: String? = null,
    @SerializedName("expiry_date") val expiryDate: String? = null,
    val specialization: String? = null,
    @SerializedName("hospital_name") val hospitalName: String? = null
)
