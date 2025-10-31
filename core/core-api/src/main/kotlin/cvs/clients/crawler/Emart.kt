package cvs.clients.crawler

import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions

// ===== 상수 설정 =====
private const val BASE_URL = "https://emart24.co.kr/goods/event"

private const val SELECTOR_ITEM = ".itemList .itemWrap"
private const val SELECTOR_DOUBLE_NEXT = ".pageNationWrap .doubleNext"
private const val SELECTOR_NEXT_BTN = ".nextButtons .next"
private const val SELECTOR_PAGE_FOCUS = ".pageNationWrap .pIndex.focus span"

class Emart24 : Cvs() {
    private val EVENT_MAPPING = mapOf(
        "onepl" to "1+1",
        "twopl" to "2+1",
        "gola" to "" // 골라담기 제외
    )

    override fun findProductList(driver: WebDriver): Boolean {
        val items = driver.findElements(By.cssSelector(SELECTOR_ITEM))
        if (items.isEmpty()) return false

        for (item in items) {
            try {
                val title = item.findElement(By.cssSelector(".itemTxtWrap .itemtitle p a")).text.trim()
                val price = item.findElement(By.cssSelector(".itemTxtWrap .price")).text.trim()
                val imgUrl = item.findElement(By.cssSelector(".itemSpImg img")).getAttribute("src")

                val eventElems = item.findElements(By.cssSelector(".itemTit .floatR"))
                var eventText = ""
                if (eventElems.isNotEmpty()) {
                    val className = eventElems[0].getAttribute("class")
                    eventText = EVENT_MAPPING.entries.firstOrNull { className.contains(it.key) }?.value ?: ""
                }

                println("상품 제목: $title")
                println("가격: $price")
                println("이미지 URL: $imgUrl")
                println("이벤트: $eventText")
                println("---")
            } catch (e: Exception) {
                println("상품 정보 파싱 실패: ${e.message}")
            }
        }

        return true
    }

    fun getLastPageNumber(driver: WebDriver): Int {
        waitForElement(driver, SELECTOR_DOUBLE_NEXT)

        while (true) {
            val doubleNext = driver.findElement(By.cssSelector(SELECTOR_DOUBLE_NEXT))
            val opacity = doubleNext.getCssValue("opacity").toDouble()

            if (opacity <= 0.3) {
                println("마지막 페이지 도달.")
                break
            }

            (driver as JavascriptExecutor).executeScript("arguments[0].click();", doubleNext)
            waitForElement(driver, SELECTOR_PAGE_FOCUS)
            Thread.sleep(SLEEP_SHORT_MS)
        }

        val lastPageElem = driver.findElement(By.cssSelector(SELECTOR_PAGE_FOCUS))
        val lastPage = lastPageElem.text.trim().toInt()
        println("마지막 페이지 번호: $lastPage")
        return lastPage
    }

    fun clickNextPage(driver: WebDriver): Boolean {
        return try {
            val nextBtn = driver.findElement(By.cssSelector(SELECTOR_NEXT_BTN))
            val opacity = nextBtn.getCssValue("opacity").toDouble()

            if (opacity <= 0.3) {
                println("마지막 페이지에 도달했습니다.")
                false
            } else {
                (driver as JavascriptExecutor).executeScript("arguments[0].click();", nextBtn)
                waitForElement(driver, SELECTOR_ITEM)
                Thread.sleep(SLEEP_SHORT_MS)
                scrollToBottom(driver)
                true
            }
        } catch (e: Exception) {
            println("다음 페이지 이동 실패: ${e.message}")
            false
        }
    }

    // ===== 전체 크롤링 =====
    fun crawlAllPages(driver: WebDriver) {
        println("[1] 마지막 페이지 번호 확인 중...")
        val lastPage = getLastPageNumber(driver)

        println("[2] 첫 페이지로 이동 중...")
        driver.get(BASE_URL)
        waitForElement(driver, SELECTOR_PAGE_FOCUS)
        scrollToBottom(driver)

        println("[3] 총 ${lastPage}페이지 탐색 시작.")
        var pageNum = 1

        while (true) {
            println("\n=== $pageNum 페이지 ===")
            if (!findProductList(driver)) {
                println("상품이 없습니다. 종료.")
                break
            }

            if (!clickNextPage(driver)) {
                break
            }

            pageNum++
        }

        println("\n모든 페이지 탐색 완료.")
    }

    // ===== 실행 엔트리포인트 =====
    fun main() {
        val options = ChromeOptions()
        // options.addArguments("--headless=new") // 필요 시 헤드리스 모드

        val driver = ChromeDriver(options)
        driver.get(BASE_URL)

        waitForElement(driver, SELECTOR_ITEM)
        scrollToBottom(driver)
        crawlAllPages(driver)

        driver.quit()
    }
}