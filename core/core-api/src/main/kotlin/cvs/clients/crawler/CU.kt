package cvs.clients.crawler

import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

class CU : CVS() {
    // ===== 상품 수집 =====
    override fun findProductList(driver: WebDriver): Boolean {
        waitForElement(driver, SELECTOR_PRODUCT_ITEM)
        val items = driver.findElements(By.cssSelector(SELECTOR_PRODUCT_ITEM))
        if (items.isEmpty()) {
            println("상품 항목이 없습니다.")
            return false
        }

        items.forEachIndexed { idx, item ->
            try {
                val title = item.findElement(By.cssSelector(".name")).text.trim()
                val price = item.findElement(By.cssSelector(".price strong")).text.trim()
                val imgUrl = item.findElement(By.cssSelector(".prod_img img")).getAttribute("src")

                val flagElems = item.findElements(By.cssSelector(".badge span"))
                val newElems = item.findElements(By.cssSelector(".tag .new"))

                val flagText = flagElems.firstOrNull()?.text?.trim().orEmpty()
                val isNew = newElems.isNotEmpty()

                println("[${idx + 1}] $title")
                println("가격: $price")
                println("이미지 URL: $imgUrl")
                println("플래그: ${flagText.ifBlank { "-" }}")
                println("NEW: $isNew")
                println("-".repeat(40))
            } catch (e: Exception) {
                println("상품 파싱 실패: ${e.message}")
            }
        }
        return true
    }

    // ===== '더보기' 버튼 처리 =====
    private fun clickAllPages(driver: WebDriver) {
        var pageCount = 0
        while (true) {
            try {
                Thread.sleep(SLEEP_SHORT_MS)
                // 버튼이 DOM 상 존재하고 보일 때만 처리
                val moreButton = WebDriverWait(driver, Duration.ofSeconds(3))
                    .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(SELECTOR_MORE_BUTTON)))

                if (!moreButton.isDisplayed) {
                    println("더보기 버튼이 숨겨졌습니다. 종료.")
                    break
                }

                pageCount += 1
                println("[더보기 클릭 ${pageCount}회차]")

                // onclick="nextPage(1)" 과 동일 동작
                (driver as JavascriptExecutor).executeScript("nextPage(1);")
                scrollToBottom(driver)
            } catch (_: Exception) {
                println("더 이상 '더보기' 버튼이 없습니다. 종료.")
                break
            }
        }
        println("총 ${pageCount}회 '더보기' 실행 완료.")
    }

    // ===== 전체 흐름 =====
    fun main() {
        val options = ChromeOptions()
        // options.addArguments("--headless=new")

        val driver = ChromeDriver(options)
        try {
            driver.get(BASE_URL)

            println("페이지 로딩 중...")
            waitForElement(driver, SELECTOR_PRODUCT_ITEM)
            scrollToBottom(driver)

            println("모든 상품을 불러오는 중...")
            clickAllPages(driver)

            println("\n상품 정보 수집 시작...")
            findProductList(driver)

            println("\n모든 상품 크롤링 완료.")
        } finally {
            driver.quit()
        }
    }
}

// ===== 상수 정의 =====
private const val BASE_URL =
    "https://cu.bgfretail.com/product/product.do?category=product&depth2=4&sf=N"

private const val SELECTOR_PRODUCT_ITEM = "div.prodListWrap ul li"
private const val SELECTOR_MORE_BUTTON = ".prodListBtn .prodListBtn-w a"







