-- V11: products 테이블에 apply_url 컬럼 추가 및 상품별 신청 URL 데이터 추가
-- ※ 운영 DB에는 해당 컬럼이 없을 수 있으므로 ADD를 조건부로 처리

-- [1] apply_url 컬럼 조건부 추가
SET @col_apply_url_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'products'
      AND COLUMN_NAME = 'apply_url'
);
SET @add_apply_url = IF(
    @col_apply_url_exists = 0,
    'ALTER TABLE products ADD COLUMN apply_url VARCHAR(2048) NULL COMMENT ''상품 신청 URL''',
    'SELECT 1'
);
PREPARE stmt FROM @add_apply_url;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- [2] 상품별 신청 URL 데이터 추가
UPDATE products SET apply_url = 'https://spot.wooribank.com/pot/Dream?withyou=PODEP0021&cc=c007095:c009166;c012263:c012399&PLM_PDCD=P010000109&PRD_CD=P010000109&ALL_GB=&depKind=A04' WHERE product_id = 1;
UPDATE products SET apply_url = 'https://www.jbbank.co.kr/' WHERE product_id = 17;
UPDATE products SET apply_url = 'https://obank.kbstar.com/quics?page=C016613&cc=b061496:b061645&isNew=N&prcode=DP01000428' WHERE product_id = 30;
UPDATE products SET apply_url = 'https://obank.kbstar.com/quics?page=C016613&cc=b061496:b061645&isNew=N&prcode=DP01000821' WHERE product_id = 31;
UPDATE products SET apply_url = 'https://obank.kbstar.com/quics?page=C016613&cc=b061496:b061645&isNew=N&prcode=DP01000942' WHERE product_id = 32;
UPDATE products SET apply_url = 'https://obank.kbstar.com/quics?page=C016613&cc=b061496:b061645&isNew=N&prcode=DP01001566' WHERE product_id = 33;
UPDATE products SET apply_url = 'https://bank.shinhan.com/index.jsp#020102010110' WHERE product_id = 34;
UPDATE products SET apply_url = 'https://smartmarket.nonghyup.com/servlet/BFDCW1021R.view' WHERE product_id = 36;
UPDATE products SET apply_url = 'https://smartmarket.nonghyup.com/servlet/BFDCW1021R.view' WHERE product_id = 37;
UPDATE products SET apply_url = 'https://smartmarket.nonghyup.com/servlet/BFDCW1021R.view' WHERE product_id = 38;
UPDATE products SET apply_url = 'https://smartmarket.nonghyup.com/servlet/BFDCW1021R.view' WHERE product_id = 39;
UPDATE products SET apply_url = 'https://www.kbanknow.com/web/product/deposit/codek-saving' WHERE product_id = 42;
UPDATE products SET apply_url = 'https://www.kbanknow.com/web/product/deposit/primary-saving' WHERE product_id = 46;
UPDATE products SET apply_url = 'https://www.kakaobank.com/products/savings' WHERE product_id = 52;
UPDATE products SET apply_url = 'https://spot.wooribank.com/pot/Dream?withyou=PODEP0001&cc=c011240:c009166;c012263:c012399&PRD_CD=P010002491&PRD_YN=Y#none' WHERE product_id = 117;
UPDATE products SET apply_url = 'https://www.standardchartered.co.kr/np/kr/pl/se/SavingDetail.jsp?id=2345' WHERE product_id = 118;
UPDATE products SET apply_url = 'https://obank.kbstar.com/quics?page=C016613&cc=b061496:b061645&isNew=N&prcode=DP01000938' WHERE product_id = 141;
UPDATE products SET apply_url = 'https://bank.shinhan.com/index.jsp#020102010110' WHERE product_id = 143;
UPDATE products SET apply_url = 'https://smartmarket.nonghyup.com/servlet/BFDCW1021R.view' WHERE product_id = 144;
UPDATE products SET apply_url = 'https://smartmarket.nonghyup.com/servlet/BFDCW1021R.view' WHERE product_id = 145;
UPDATE products SET apply_url = 'https://smartmarket.nonghyup.com/servlet/BFDCW1021R.view' WHERE product_id = 146;
UPDATE products SET apply_url = 'https://smartmarket.nonghyup.com/servlet/BFDCW1021R.view' WHERE product_id = 147;
UPDATE products SET apply_url = 'https://www.kbanknow.com/web/product/deposit/codek-fixed' WHERE product_id = 149;
UPDATE products SET apply_url = 'https://www.suhyup-bank.com/' WHERE product_id = 151;
UPDATE products SET apply_url = 'https://www.kakaobank.com/products/withdrawal' WHERE product_id = 153;
