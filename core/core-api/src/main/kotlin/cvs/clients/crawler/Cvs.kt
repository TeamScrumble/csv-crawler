package cvs.clients.crawler

import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.stereotype.Component
import java.time.Duration

@Component
abstract class Cvs {
    companion object {
        const val WAIT_TIMEOUT_SEC = 5L
        const val SLEEP_SHORT_MS = 800L
        const val SLEEP_LONG_MS = 1_000L
    }
    // ===== 유틸 함수 =====
    fun waitForElement(driver: WebDriver, selector: String, timeoutSec: Long = WAIT_TIMEOUT_SEC) {
        WebDriverWait(driver, Duration.ofSeconds(timeoutSec))
            .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(selector)))
    }

    fun scrollToBottom(driver: WebDriver) {
        (driver as JavascriptExecutor).executeScript("window.scrollTo(0, document.body.scrollHeight);")
        Thread.sleep(SLEEP_SHORT_MS)
    }

    abstract fun findProductList(driver: WebDriver): Boolean
}