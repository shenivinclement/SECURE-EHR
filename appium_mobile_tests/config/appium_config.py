import os
import logging

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")

class AppiumConfig:
    """Appium Driver Configuration for SecureEHR Android Application."""
    
    APPIUM_SERVER_URL = os.environ.get("APPIUM_SERVER_URL", "http://127.0.0.1:4723/wd/hub")
    
    # Android Device & App Capabilities
    ANDROID_CAPABILITIES = {
        "platformName": "Android",
        "automationName": "UiAutomator2",
        "deviceName": os.environ.get("ANDROID_DEVICE_NAME", "Android_Emulator"),
        "platformVersion": os.environ.get("ANDROID_VERSION", "13.0"),
        "appPackage": "com.secureehr.app",
        "appActivity": ".MainActivity",
        "noReset": True,
        "fullReset": False,
        "autoGrantPermissions": True,
        "newCommandTimeout": 300,
        "locationServicesAuthorized": True,
    }

    # APK Path if deploying to an emulator/device
    APK_PATH = os.path.abspath(
        os.path.join(os.path.dirname(__file__), "..", "..", "secureehr-app-main", "app", "build", "outputs", "apk", "debug", "app-debug.apk")
    )
    
    MOCK_EXECUTION = True  # Allows running test suite and generating report even when physical device/Appium server is offline

    @classmethod
    def get_capabilities(cls):
        caps = cls.ANDROID_CAPABILITIES.copy()
        if os.path.exists(cls.APK_PATH):
            caps["app"] = cls.APK_PATH
        return caps
