import time
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait


# ===== 상수 정의 =====
BASE_URL = "https://cu.bgfretail.com/product/product.do?category=product&depth2=4&sf=N"

SELECTOR_PRODUCT_ITEM = "div.prodListWrap ul li"
SELECTOR_MORE_BUTTON = ".prodListBtn .prodListBtn-w a"

WAIT_TIMEOUT = 5
SLEEP_SHORT = 0.8
SLEEP_LONG = 1.0


# ===== 유틸 함수 =====
def wait_for_element(driver, selector, timeout=WAIT_TIMEOUT):
    WebDriverWait(driver, timeout).until(
        EC.presence_of_element_located((By.CSS_SELECTOR, selector))
    )


def scroll_to_bottom(driver):
    """페이지 끝까지 스크롤"""
    driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")
    time.sleep(SLEEP_SHORT)


# ===== 상품 수집 =====
def find_product_list(driver):
    """현재 페이지의 모든 상품 정보를 탐색"""
    wait_for_element(driver, SELECTOR_PRODUCT_ITEM)
    items = driver.find_elements(By.CSS_SELECTOR, SELECTOR_PRODUCT_ITEM)

    if not items:
        print("상품 항목이 없습니다.")
        return False

    for idx, item in enumerate(items, start=1):
        try:
            title = item.find_element(By.CSS_SELECTOR, ".name").text.strip()
            price = item.find_element(By.CSS_SELECTOR, ".price strong").text.strip()
            img_url = item.find_element(By.CSS_SELECTOR, ".prod_img img").get_attribute("src")

            flag_elems = item.find_elements(By.CSS_SELECTOR, ".badge span")
            new_elems = item.find_elements(By.CSS_SELECTOR, ".tag .new")

            flag_text = flag_elems[0].text.strip() if flag_elems else ""
            is_new = bool(new_elems)

            print(f"[{idx}] {title}")
            print(f"가격: {price}")
            print(f"이미지 URL: {img_url}")
            print(f"플래그: {flag_text or '-'}")
            print(f"NEW: {is_new}")
            print("-" * 40)

        except Exception as e:
            print(f"상품 파싱 실패: {e}")

    return True


# ===== '더보기' 버튼 처리 =====
def click_all_pages(driver):
    """'더보기' 버튼이 사라질 때까지 클릭하며 상품을 모두 불러온다."""
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
            print(f"[더보기 클릭 {page_count}회차]")

            # onclick="nextPage(1)"과 동일한 동작
            driver.execute_script("nextPage(1);")
            scroll_to_bottom(driver)

        except Exception:
            print("더 이상 '더보기' 버튼이 없습니다. 종료.")
            break

    print(f"총 {page_count}회 '더보기' 실행 완료.")


# ===== 전체 흐름 =====
def main():
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


# ===== 진입점 =====
if __name__ == "__main__":
    main()