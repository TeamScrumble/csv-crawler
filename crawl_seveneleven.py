import time
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait


# ===== 상수 설정 =====
BASE_URL = "https://www.7-eleven.co.kr/product/bestdosirakList.asp"

SELECTOR_PRODUCT_ITEM = "div.dosirak_list ul li"
SELECTOR_MORE_BUTTON = ".btn_more a"

WAIT_TIMEOUT = 5
SLEEP_SHORT = 0.7
SLEEP_LONG = 1.0


# ===== 유틸 함수 =====
def wait_for_element(driver, selector, timeout=WAIT_TIMEOUT):
    """특정 요소가 나타날 때까지 대기"""
    WebDriverWait(driver, timeout).until(
        EC.presence_of_all_elements_located((By.CSS_SELECTOR, selector))
    )


def scroll_to_bottom(driver):
    """페이지 하단으로 스크롤"""
    driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")
    time.sleep(SLEEP_SHORT)


# ===== 상품 탐색 =====
def find_product_list(driver):
    """현재 페이지에 표시된 상품 리스트를 탐색"""
    wait_for_element(driver, SELECTOR_PRODUCT_ITEM)
    items = driver.find_elements(By.CSS_SELECTOR, SELECTOR_PRODUCT_ITEM)

    if not items:
        print("상품 항목이 없습니다.")
        return False

    count = 0

    for index, item in enumerate(items):
        if index == 0:  # 첫 번째 항목은 스킵
            continue

        try:
            title_elems = item.find_elements(By.CSS_SELECTOR, ".pic_product .infowrap .name")
            if not title_elems:
                continue
            title = title_elems[0].text.strip()

            price_elems = item.find_elements(By.CSS_SELECTOR, ".pic_product .infowrap .price span")
            price = price_elems[0].text.strip() if price_elems else ""

            img_elems = item.find_elements(By.CSS_SELECTOR, ".pic_product img")
            img_url = img_elems[0].get_attribute("src") if img_elems else ""

            new_elems = item.find_elements(By.CSS_SELECTOR, "ul.tag_list_01 .ico_tag_03")
            is_new = bool(new_elems)

            count += 1
            print(f"[{count}] {title}")
            print(f"가격: {price}")
            print(f"이미지 URL: {img_url}")
            print(f"NEW: {is_new}")
            print("-" * 40)

        except Exception as e:
            print(f"상품 파싱 실패 (index={index}): {e}")

    print(f"총 상품 수: {count}")
    return True


# ===== 더보기 버튼 처리 =====
def click_all_pages(driver):
    """'더보기' 버튼이 사라질 때까지 클릭하여 모든 상품을 불러옴"""
    page_count = 0

    while True:
        try:
            time.sleep(SLEEP_SHORT)
            more_button = WebDriverWait(driver, 3).until(
                EC.presence_of_element_located((By.CSS_SELECTOR, SELECTOR_MORE_BUTTON))
            )

            if not more_button.is_displayed():
                print("더보기 버튼이 숨겨졌습니다. 종료.")
                break

            page_count += 1
            print(f"[{page_count}] 더보기 클릭 중...")

            # onclick="fncMore('');" 동일 동작
            driver.execute_script("fncMore('');")
            scroll_to_bottom(driver)

        except Exception:
            print("더 이상 '더보기' 버튼이 없습니다. 종료.")
            break

    print(f"총 {page_count}회 더보기 실행 완료.")


# ===== 전체 크롤링 흐름 =====
def main():
    """세븐일레븐 도시락 페이지 전체 상품 크롤링"""
    options = webdriver.ChromeOptions()
    # options.add_argument("--headless=new")

    driver = webdriver.Chrome(options=options)
    driver.get(BASE_URL)

    print("페이지 로딩 중...")
    wait_for_element(driver, SELECTOR_PRODUCT_ITEM)
    scroll_to_bottom(driver)

    print("모든 상품을 불러오는 중...")
    click_all_pages(driver)

    print("\n상품 정보 수집 시작...")
    find_product_list(driver)

    print("\n모든 상품 크롤링 완료.")
    driver.quit()


# ===== 실행 진입점 =====
if __name__ == "__main__":
    main()