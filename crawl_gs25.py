import re
import time
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait

# --- 상수 정의 ---
BASE_URL = "http://gs25.gsretail.com/gscvs/ko/products/event-goods#;"
SELECTOR_ITEM = "ul.prod_list li"
SELECTOR_NEXT2 = ".paging .next2"
SELECTOR_TAB_TOTAL = "#TOTAL"
WAIT_TIMEOUT = 5
SLEEP_INTERVAL = 0.5


# --- 유틸 함수 ---
def wait_for_products(driver):
    """상품 목록이 로드될 때까지 대기"""
    WebDriverWait(driver, WAIT_TIMEOUT).until(
        EC.presence_of_element_located((By.CSS_SELECTOR, SELECTOR_ITEM))
    )
    time.sleep(SLEEP_INTERVAL)


def move_to_page(driver, page_num):
    """지정한 페이지로 이동"""
    script = f"goodsPageController.movePage({page_num});"
    driver.execute_script(script)
    wait_for_products(driver)


# --- 핵심 기능 ---
def find_product_list(driver):
    """현재 페이지에서 상품 목록을 파싱"""
    wait_for_products(driver)

    items = driver.find_elements(By.CSS_SELECTOR, SELECTOR_ITEM)
    if not items:
        return False

    for item in items:
        title_elem = item.find_element(By.CSS_SELECTOR, ".tit")
        title = title_elem.text.strip()
        if not title:
            continue

        price = item.find_element(By.CSS_SELECTOR, ".price").text.strip()
        img_url = item.find_element(By.CSS_SELECTOR, "img").get_attribute("src")

        flag_elems = item.find_elements(By.CSS_SELECTOR, ".flag_box span")
        flag_text = flag_elems[0].text.strip() if flag_elems else ""

        print(f"상품 제목: {title}")
        print(f"가격: {price}")
        print(f"이미지 URL: {img_url}")
        print(f"플래그: {flag_text}")
        print("---")

    return True


def get_last_page_number(driver):
    """마지막 페이지 번호 추출"""
    WebDriverWait(driver, WAIT_TIMEOUT).until(
        EC.presence_of_element_located((By.CSS_SELECTOR, SELECTOR_NEXT2))
    )

    next2_elem = driver.find_element(By.CSS_SELECTOR, SELECTOR_NEXT2)
    onclick_attr = next2_elem.get_attribute("onclick")

    match = re.search(r"movePage\((\d+)\)", onclick_attr)
    if not match:
        raise Exception("onclick에서 페이지 번호를 찾을 수 없습니다.")

    return int(match.group(1))


def crawl_all_pages(driver):
    """모든 페이지 순회하며 상품 수집"""
    last_page = get_last_page_number(driver)
    print(f"마지막 페이지 번호: {last_page}")

    move_to_page(driver, 1)
    for page_num in range(1, last_page + 1):
        print(f"\n=== {page_num} 페이지 탐색 중 ===")
        if not find_product_list(driver):
            print("상품이 없습니다. 종료.")
            break
        if page_num < last_page:
            move_to_page(driver, page_num + 1)

    print("\n모든 페이지 탐색 완료.")


# --- 실행 부분 ---
def main():
    options = webdriver.ChromeOptions()
    # options.add_argument("--headless=new")

    driver = webdriver.Chrome(options=options)
    driver.get(BASE_URL)

    # 전체 탭 진입
    WebDriverWait(driver, WAIT_TIMEOUT).until(
        EC.element_to_be_clickable((By.CSS_SELECTOR, SELECTOR_TAB_TOTAL))
    )
    driver.find_element(By.CSS_SELECTOR, SELECTOR_TAB_TOTAL).click()
    time.sleep(SLEEP_INTERVAL)

    # 전체 페이지 크롤링
    crawl_all_pages(driver)

    driver.quit()


if __name__ == "__main__":
    main()