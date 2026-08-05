from .base_page import BasePage

class SplashPage(BasePage):
    """Page Object for Splash Screen."""
    LOGO = "com.secureehr.app:id/splash_logo"
    TITLE = "com.secureehr.app:id/splash_title"
    GET_STARTED_BTN = "com.secureehr.app:id/btn_get_started"

    def navigate_to_login(self):
        self.click("id", self.GET_STARTED_BTN)


class LoginPage(BasePage):
    """Page Object for Login Screen."""
    USERNAME_INPUT = "com.secureehr.app:id/input_email_username"
    PASSWORD_INPUT = "com.secureehr.app:id/input_password"
    LOGIN_BTN = "com.secureehr.app:id/btn_login"
    BIOMETRIC_BTN = "com.secureehr.app:id/btn_biometric_auth"
    REGISTER_LINK = "com.secureehr.app:id/link_register"
    TOGGLE_ROLE = "com.secureehr.app:id/switch_doctor_patient"
    FORGOT_PASSWORD_LINK = "com.secureehr.app:id/link_forgot_password"

    def login(self, username, password, role="patient"):
        self.send_keys("id", self.USERNAME_INPUT, username)
        self.send_keys("id", self.PASSWORD_INPUT, password)
        self.click("id", self.LOGIN_BTN)

    def login_with_biometrics(self):
        self.click("id", self.BIOMETRIC_BTN)


class RegisterPage(BasePage):
    """Page Object for Registration Screen."""
    FULL_NAME_INPUT = "com.secureehr.app:id/input_full_name"
    EMAIL_INPUT = "com.secureehr.app:id/input_register_email"
    PHONE_INPUT = "com.secureehr.app:id/input_phone"
    PASSWORD_INPUT = "com.secureehr.app:id/input_register_password"
    CONFIRM_PASSWORD_INPUT = "com.secureehr.app:id/input_confirm_password"
    TERMS_CHECKBOX = "com.secureehr.app:id/checkbox_terms"
    REGISTER_BTN = "com.secureehr.app:id/btn_submit_register"


class DashboardPage(BasePage):
    """Page Object for Patient Dashboard."""
    USER_WELCOME_LABEL = "com.secureehr.app:id/txt_welcome"
    HEALTH_SCORE_CARD = "com.secureehr.app:id/card_health_score"
    QUICK_RECORDS_BTN = "com.secureehr.app:id/btn_quick_records"
    QUICK_CONSENT_BTN = "com.secureehr.app:id/btn_quick_consent"
    EMERGENCY_SOS_BTN = "com.secureehr.app:id/btn_emergency_sos"
    NAV_BOTTOM_RECORDS = "com.secureehr.app:id/nav_records"
    NAV_BOTTOM_CONSENT = "com.secureehr.app:id/nav_consent"
    NAV_BOTTOM_HOSPITALS = "com.secureehr.app:id/nav_hospitals"
    NAV_BOTTOM_CHAT = "com.secureehr.app:id/nav_ai_chat"
    NAV_BOTTOM_PROFILE = "com.secureehr.app:id/nav_profile"


class DoctorDashboardPage(BasePage):
    """Page Object for Doctor Dashboard."""
    DOCTOR_TITLE = "com.secureehr.app:id/txt_doctor_title"
    PATIENTS_COUNT = "com.secureehr.app:id/txt_patient_count"
    PENDING_CONSENTS_CARD = "com.secureehr.app:id/card_pending_consents"
    SEARCH_PATIENT_INPUT = "com.secureehr.app:id/input_search_patient"
    PATIENT_LIST_RECYCLER = "com.secureehr.app:id/recycler_patients"


class MedicalRecordsPage(BasePage):
    """Page Object for Medical Records Screen."""
    SEARCH_RECORD_INPUT = "com.secureehr.app:id/input_search_record"
    FILTER_LAB_REPORTS = "com.secureehr.app:id/chip_filter_lab"
    FILTER_PRESCRIPTIONS = "com.secureehr.app:id/chip_filter_prescriptions"
    ADD_RECORD_FAB = "com.secureehr.app:id/fab_add_record"
    RECORD_ITEM = "com.secureehr.app:id/item_medical_record"
    DECRYPT_BTN = "com.secureehr.app:id/btn_decrypt_record"


class ConsentManagerPage(BasePage):
    """Page Object for Zero-Knowledge Consent Manager."""
    ACTIVE_CONSENTS_TAB = "com.secureehr.app:id/tab_active_consents"
    REVOKED_CONSENTS_TAB = "com.secureehr.app:id/tab_revoked_consents"
    GRANT_CONSENT_BTN = "com.secureehr.app:id/btn_grant_new_consent"
    REVOKE_BTN = "com.secureehr.app:id/btn_revoke_access"
    DURATION_SLIDER = "com.secureehr.app:id/slider_duration_hours"
    ACCESS_AUDIT_LOG = "com.secureehr.app:id/recycler_audit_log"


class HospitalFinderPage(BasePage):
    """Page Object for Hospital & ER Finder."""
    MAP_VIEW = "com.secureehr.app:id/map_hospitals"
    SEARCH_LOCATION = "com.secureehr.app:id/input_search_location"
    EMERGENCY_ONLY_TOGGLE = "com.secureehr.app:id/toggle_emergency_only"
    HOSPITAL_CARD = "com.secureehr.app:id/card_hospital_item"
    CALL_HOSPITAL_BTN = "com.secureehr.app:id/btn_call_hospital"
    GET_DIRECTIONS_BTN = "com.secureehr.app:id/btn_get_directions"


class BlockchainStatusPage(BasePage):
    """Page Object for Blockchain Audit Trail & Integrity Screen."""
    NODE_STATUS_INDICATOR = "com.secureehr.app:id/indicator_node_status"
    LATEST_BLOCK_HASH = "com.secureehr.app:id/txt_latest_block_hash"
    VERIFY_INTEGRITY_BTN = "com.secureehr.app:id/btn_verify_chain_integrity"
    TRANSACTION_LIST = "com.secureehr.app:id/recycler_blockchain_tx"


class AIChatPage(BasePage):
    """Page Object for AI Health Assistant Screen."""
    CHAT_RECYCLER = "com.secureehr.app:id/recycler_chat_messages"
    MESSAGE_INPUT = "com.secureehr.app:id/input_chat_message"
    SEND_BTN = "com.secureehr.app:id/btn_send_message"
    DISCLAIMER_BANNER = "com.secureehr.app:id/banner_ai_disclaimer"


class SettingsPage(BasePage):
    """Page Object for Settings Screen."""
    THEME_DARK_MODE_TOGGLE = "com.secureehr.app:id/switch_dark_mode"
    BIOMETRIC_ENABLE_TOGGLE = "com.secureehr.app:id/switch_enable_biometric"
    NOTIFICATIONS_TOGGLE = "com.secureehr.app:id/switch_notifications"
    CLEAR_CACHE_BTN = "com.secureehr.app:id/btn_clear_cache"
    LOGOUT_BTN = "com.secureehr.app:id/btn_logout"
