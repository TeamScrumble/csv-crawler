package cvs.clients.crawler

import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

// ===== 상수 설정 =====
private const val BASE_URL = "https://www.7-eleven.co.kr/product/bestdosirakList.asp"

private const val SELECTOR_PRODUCT_ITEM = "div.dosirak_list ul li"
private const val SELECTOR_MORE_BUTTON = ".btn_more a"

private const val WAIT_TIMEOUT_SEC = 5L
private const val SLEEP_SHORT_MS = 700L
private const val SLEEP_LONG_MS = 1_000L

class SevenEleven : CVS() {
    // ===== 더보기 버튼 처리 =====
    private fun clickAllPages(driver: WebDriver) {
        var pageCount = 0

        while (true) {
            try {
                Thread.sleep(cvs.clients.crawler.SLEEP_SHORT_MS)
                val moreButton = WebDriverWait(driver, Duration.ofSeconds(3))
                    .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(SELECTOR_MORE_BUTTON)))

                if (!moreButton.isDisplayed) {
                    println("더보기 버튼이 숨겨졌습니다. 종료.")
                    break
                }

                pageCount++
                println("[$pageCount] 더보기 클릭 중...")

                // onclick="fncMore('');" 과 동일
                (driver as JavascriptExecutor).executeScript("fncMore('');")
                scrollToBottom(driver)
            } catch (_: Exception) {
                println("더 이상 '더보기' 버튼이 없습니다. 종료.")
                break
            }
        }

        println("총 ${pageCount}회 더보기 실행 완료.")
    }

    // ===== 상품 탐색 =====
    override fun findProductList(driver: WebDriver): Boolean {
        waitForElement(driver, SELECTOR_PRODUCT_ITEM)
        val items = driver.findElements(By.cssSelector(SELECTOR_PRODUCT_ITEM))

        if (items.isEmpty()) {
            println("상품 항목이 없습니다.")
            return false
        }

        var count = 0

        for ((index, item) in items.withIndex()) {
            // 첫 번째 항목 스킵
            if (index == 0) continue

            try {
                val titleElems = item.findElements(By.cssSelector(".pic_product .infowrap .name"))
                if (titleElems.isEmpty()) continue
                val title = titleElems[0].text.trim()

                val priceElems = item.findElements(By.cssSelector(".pic_product .infowrap .price span"))
                val price = if (priceElems.isNotEmpty()) priceElems[0].text.trim() else ""

                val imgElems = item.findElements(By.cssSelector(".pic_product img"))
                val imgUrl = if (imgElems.isNotEmpty()) imgElems[0].getAttribute("src") else ""

                val newElems = item.findElements(By.cssSelector("ul.tag_list_01 .ico_tag_03"))
                val isNew = newElems.isNotEmpty()

                count++
                println("[$count] $title")
                println("가격: $price")
                println("이미지 URL: $imgUrl")
                println("NEW: $isNew")
                println("-".repeat(40))
            } catch (e: Exception) {
                println("상품 파싱 실패 (index=$index): ${e.message}")
            }
        }

        println("총 상품 수: $count")
        return true
    }

    // ===== 전체 크롤링 흐름 =====
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