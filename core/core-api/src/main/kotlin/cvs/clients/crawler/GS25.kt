package cvs.clients.crawler

import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration
import java.util.regex.Pattern

// --- 상수 정의 ---
private const val BASE_URL = "http://gs25.gsretail.com/gscvs/ko/products/event-goods#;"
private const val SELECTOR_ITEM = "ul.prod_list li"
private const val SELECTOR_NEXT2 = ".paging .next2"
private const val SELECTOR_TAB_TOTAL = "#TOTAL"
private const val SLEEP_INTERVAL_MS = 500L

class GS25 : Cvs() {

    private fun moveToPage(driver: WebDriver, pageNum: Int) {
        val script = "goodsPageController.movePage($pageNum);"
        (driver as JavascriptExecutor).executeScript(script)
        waitForElement(driver, SELECTOR_ITEM)
        Thread.sleep(SLEEP_INTERVAL_MS)
    }

    private fun getLastPageNumber(driver: WebDriver): Int {
        WebDriverWait(driver, Duration.ofSeconds(WAIT_TIMEOUT_SEC))
            .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(SELECTOR_NEXT2)))

        val next2Elem = driver.findElement(By.cssSelector(SELECTOR_NEXT2))
        val onclickAttr = next2Elem.getAttribute("onclick")
        val pattern = Pattern.compile("movePage\\((\\d+)\\)")
        val matcher = pattern.matcher(onclickAttr)

        if (!matcher.find()) {
            throw RuntimeException("onclick에서 페이지 번호를 찾을 수 없습니다.")
        }

        return matcher.group(1).toInt()
    }

    private fun crawlAllPages(driver: WebDriver) {
        val lastPage = getLastPageNumber(driver)
        println("마지막 페이지 번호: $lastPage")

        moveToPage(driver, 1)
        for (pageNum in 1..lastPage) {
            println("\n=== $pageNum 페이지 탐색 중 ===")
            if (!findProductList(driver)) {
                println("상품이 없습니다. 종료.")
                break
            }
            if (pageNum < lastPage) {
                moveToPage(driver, pageNum + 1)
            }
        }

        println("\n모든 페이지 탐색 완료.")
    }

    // --- 핵심 기능 ---
    override fun findProductList(driver: WebDriver): Boolean {
        waitForElement(driver, SELECTOR_ITEM)
        Thread.sleep(SLEEP_INTERVAL_MS)

        val items = driver.findElements(By.cssSelector(SELECTOR_ITEM))
        if (items.isEmpty()) return false

        for (item in items) {
            try {
                val titleElem: WebElement = item.findElement(By.cssSelector(".tit"))
                val title = titleElem.text.trim()
                if (title.isBlank()) continue

                val price = item.findElement(By.cssSelector(".price")).text.trim()
                val imgUrl = item.findElement(By.cssSelector("img")).getAttribute("src")

                val flagElems = item.findElements(By.cssSelector(".flag_box span"))
                val flagText = flagElems.firstOrNull()?.text?.trim().orEmpty()

                println("상품 제목: $title")
                println("가격: $price")
                println("이미지 URL: $imgUrl")
                println("플래그: $flagText")
                println("---")
            } catch (e: Exception) {
                println("상품 파싱 실패: ${e.message}")
            }
        }

        return true
    }

    // --- 실행 부분 ---
    fun main() {
        val options = ChromeOptions()
        // options.addArguments("--headless=new")

        val driver = ChromeDriver(options)
        try {
            driver.get(BASE_URL)

            // 전체 탭 진입
            WebDriverWait(driver, Duration.ofSeconds(WAIT_TIMEOUT_SEC))
                .until(ExpectedConditions.elementToBeClickable(By.cssSelector(SELECTOR_TAB_TOTAL)))
            driver.findElement(By.cssSelector(SELECTOR_TAB_TOTAL)).click()
            Thread.sleep(SLEEP_INTERVAL_MS)

            // 전체 페이지 크롤링
            crawlAllPages(driver)
        } finally {
            driver.quit()
        }
    }
}