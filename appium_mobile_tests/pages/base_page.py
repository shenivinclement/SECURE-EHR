import time
import logging

class BasePage:
    """Base Page Object for Appium mobile automation on SecureEHR Android app."""

    def __init__(self, driver=None, mock_mode=True):
        self.driver = driver
        self.mock_mode = mock_mode
        self.logger = logging.getLogger(self.__class__.__name__)

    def find_element(self, by_type, locator_value, timeout=10):
        if self.mock_mode or self.driver is None:
            self.logger.info(f"[MOCK] Finding element: {by_type}='{locator_value}'")
            return MockElement(locator_value)
        # Real Appium driver interaction
        from selenium.webdriver.support.ui import WebDriverWait
        from selenium.webdriver.support import expected_conditions as EC
        wait = WebDriverWait(self.driver, timeout)
        return wait.until(EC.presence_of_element_located((by_type, locator_value)))

    def click(self, by_type, locator_value):
        self.logger.info(f"Clicking element {by_type}='{locator_value}'")
        element = self.find_element(by_type, locator_value)
        element.click()

    def send_keys(self, by_type, locator_value, text):
        self.logger.info(f"Sending keys '{text}' to {by_type}='{locator_value}'")
        element = self.find_element(by_type, locator_value)
        element.send_keys(text)

    def is_displayed(self, by_type, locator_value):
        try:
            element = self.find_element(by_type, locator_value)
            return element.is_displayed()
        except Exception:
            return False

    def scroll_down(self):
        self.logger.info("Scrolling down screen")
        if not self.mock_mode and self.driver:
            window_size = self.driver.get_window_size()
            start_y = int(window_size['height'] * 0.8)
            end_y = int(window_size['height'] * 0.2)
            start_x = int(window_size['width'] * 0.5)
            self.driver.swipe(start_x, start_y, start_x, end_y, 800)

    def swipe_horizontal(self, direction="left"):
        self.logger.info(f"Swiping screen horizontal {direction}")
        if not self.mock_mode and self.driver:
            window_size = self.driver.get_window_size()
            y = int(window_size['height'] * 0.5)
            if direction == "left":
                start_x = int(window_size['width'] * 0.9)
                end_x = int(window_size['width'] * 0.1)
            else:
                start_x = int(window_size['width'] * 0.1)
                end_x = int(window_size['width'] * 0.9)
            self.driver.swipe(start_x, y, end_x, y, 600)


class MockElement:
    """Mock element representing Android UI components during test execution."""

    def __init__(self, locator_value):
        self.locator_value = locator_value
        self.text = f"MockText_{locator_value}"

    def click(self):
        return True

    def send_keys(self, text):
        self.text = text
        return True

    def is_displayed(self):
        return True

    def clear(self):
        self.text = ""
        return True
