import time
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait


# ===== 상수 설정 =====
BASE_URL = "https://emart24.co.kr/goods/event"
WAIT_TIMEOUT = 5
SLEEP_SHORT = 0.3
SLEEP_LONG = 0.5

SELECTOR_ITEM = ".itemList .itemWrap"
SELECTOR_DOUBLE_NEXT = ".pageNationWrap .doubleNext"
SELECTOR_NEXT_BTN = ".nextButtons .next"
SELECTOR_PAGE_FOCUS = ".pageNationWrap .pIndex.focus span"

EVENT_MAPPING = {
    "onepl": "1+1",
    "twopl": "2+1",
    "gola": "",  # 골라담기 제외
}


# ===== 유틸리티 함수 =====
def wait_for_element(driver, selector, timeout=WAIT_TIMEOUT):
    WebDriverWait(driver, timeout).until(
        EC.presence_of_element_located((By.CSS_SELECTOR, selector))
    )


def scroll_to_bottom(driver):
    driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")
    time.sleep(SLEEP_SHORT)


# ===== 핵심 기능 =====
def find_product_list(driver):
    """현재 페이지의 상품 목록을 탐색"""
    # time.sleep(SLEEP_LONG)
    items = driver.find_elements(By.CSS_SELECTOR, SELECTOR_ITEM)
    if not items:
        return False

    for item in items:
        try:
            title = item.find_element(By.CSS_SELECTOR, ".itemTxtWrap .itemtitle p a").text.strip()
            price = item.find_element(By.CSS_SELECTOR, ".itemTxtWrap .price").text.strip()
            img_url = item.find_element(By.CSS_SELECTOR, ".itemSpImg img").get_attribute("src")

            event_elems = item.find_elements(By.CSS_SELECTOR, ".itemTit .floatR")
            event_text = ""
            if event_elems:
                class_name = event_elems[0].get_attribute("class")
                event_text = next((v for k, v in EVENT_MAPPING.items() if k in class_name), "")

            print(f"상품 제목: {title}")
            print(f"가격: {price}")
            print(f"이미지 URL: {img_url}")
            print(f"이벤트: {event_text}")
            print("---")

        except Exception as e:
            print(f"상품 정보 파싱 실패: {e}")

    return True


def get_last_page_number(driver):
    """마지막 페이지 번호 탐색"""
    wait_for_element(driver, SELECTOR_DOUBLE_NEXT)

    while True:
        double_next = driver.find_element(By.CSS_SELECTOR, SELECTOR_DOUBLE_NEXT)
        opacity = double_next.value_of_css_property("opacity")

        if float(opacity) <= 0.3:
            print("마지막 페이지 도달.")
            break

        driver.execute_script("arguments[0].click();", double_next)
        wait_for_element(driver, SELECTOR_PAGE_FOCUS)
        time.sleep(SLEEP_SHORT)

    last_page_elem = driver.find_element(By.CSS_SELECTOR, SELECTOR_PAGE_FOCUS)
    last_page = int(last_page_elem.text.strip())
    print(f"마지막 페이지 번호: {last_page}")
    return last_page


def click_next_page(driver):
    """다음 페이지로 이동하고 스크롤"""
    try:
        next_btn = driver.find_element(By.CSS_SELECTOR, SELECTOR_NEXT_BTN)
        opacity = next_btn.value_of_css_property("opacity")

        if float(opacity) <= 0.3:
            print("마지막 페이지에 도달했습니다.")
            return False

        driver.execute_script("arguments[0].click();", next_btn)
        wait_for_element(driver, SELECTOR_ITEM)
        time.sleep(SLEEP_SHORT)
        scroll_to_bottom(driver)
        return True

    except Exception as e:
        print("다음 페이지 이동 실패:", e)
        return False


# ===== 전체 크롤링 =====
def crawl_all_pages(driver):
    """전체 페이지 순회"""
    print("[1] 마지막 페이지 번호 확인 중...")
    last_page = get_last_page_number(driver)

    print("[2] 첫 페이지로 이동 중...")
    driver.get(BASE_URL)
    wait_for_element(driver, SELECTOR_PAGE_FOCUS)
    scroll_to_bottom(driver)

    print(f"[3] 총 {last_page}페이지 탐색 시작.")
    page_num = 1
    while True:
        print(f"\n=== {page_num} 페이지 ===")
        if not find_product_list(driver):
            print("상품이 없습니다. 종료.")
            break

        if not click_next_page(driver):
            break

        page_num += 1

    print("\n모든 페이지 탐색 완료.")


# ===== 실행 엔트리포인트 =====
def main():
    options = webdriver.ChromeOptions()
    # options.add_argument("--headless=new")

    driver = webdriver.Chrome(options=options)
    driver.get(BASE_URL)

    wait_for_element(driver, SELECTOR_ITEM)
    scroll_to_bottom(driver)
    crawl_all_pages(driver)

    driver.quit()


if __name__ == "__main__":
    main()