-- V11: products 테이블에 apply_url 컬럼 추가 및 상품별 신청 URL 데이터 반영
-- 실제 products 컬럼:
-- kor_co_nm = 은행명 / fin_prdt_nm = 상품명 / product_type = SAVING, DEPOSIT / apply_url = URL
-- 엑셀의 '예금' → DEPOSIT, '적금' → SAVING

-- [1] apply_url 컬럼 추가
ALTER TABLE products
    ADD COLUMN apply_url VARCHAR(500) NULL;

-- [2] 상품별 신청 URL 데이터 반영
UPDATE products SET apply_url = 'https://spot.wooribank.com/pot/Dream?withyou=PODEP0001&cc=c011240:c009166;c012263:c012399&PRD_CD=P010002491&PRD_YN=Y#none' WHERE kor_co_nm = '우리은행' AND fin_prdt_nm = 'WON플러스예금' AND product_type = 'DEPOSIT';
UPDATE products SET apply_url = 'https://spot.wooribank.com/pot/Dream?withyou=PODEP0021&cc=c007095:c009166;c012263:c012399&PLM_PDCD=P010000109&PRD_CD=P010000109&ALL_GB=&depKind=A04' WHERE kor_co_nm = '우리은행' AND fin_prdt_nm = '우리SUPER주거래적금' AND product_type = 'SAVING';
UPDATE products SET apply_url = 'https://spot.wooribank.com/pot/Dream?withyou=PODEP0021&cc=c007095:c009166;c012263:c012399&PLM_PDCD=P010002353&PRD_CD=P010002353&ALL_GB=&depKind=A04' WHERE kor_co_nm = '우리은행' AND fin_prdt_nm = 'WON 적금' AND product_type = 'SAVING';
UPDATE products SET apply_url = 'https://obank.kbstar.com/quics?page=C016613&cc=b061496:b061645&isNew=N&prcode=DP01000938' WHERE kor_co_nm = '국민은행' AND fin_prdt_nm = 'KB Star 정기예금' AND product_type = 'DEPOSIT';
UPDATE products SET apply_url = 'https://obank.kbstar.com/quics?page=C016613&cc=b061496:b061645&isNew=N&prcode=DP01000428' WHERE kor_co_nm = '국민은행' AND fin_prdt_nm = 'KB국민프리미엄적금(정액)' AND product_type = 'SAVING';
UPDATE products SET apply_url = 'https://obank.kbstar.com/quics?page=C016613&cc=b061496:b061645&isNew=N&prcode=DP01000821' WHERE kor_co_nm = '국민은행' AND fin_prdt_nm = 'KB내맘대로적금' AND product_type = 'SAVING';
UPDATE products SET apply_url = 'https://obank.kbstar.com/quics?page=C016613&cc=b061496:b061645&isNew=N&prcode=DP01000942' WHERE kor_co_nm = '국민은행' AND fin_prdt_nm = 'KB맑은하늘적금' AND product_type = 'SAVING';
UPDATE products SET apply_url = 'https://obank.kbstar.com/quics?page=C016613&cc=b061496:b061645&isNew=N&prcode=DP01001566' WHERE kor_co_nm = '국민은행' AND fin_prdt_nm = 'KB 특★한 적금' AND product_type = 'SAVING';
UPDATE products SET apply_url = NULL WHERE kor_co_nm = '신한은행' AND fin_prdt_nm = '신한My플러스 정기예금' AND product_type = 'DEPOSIT';
UPDATE products SET apply_url = 'https://bank.shinhan.com/index.jsp#020102010110' WHERE kor_co_nm = '신한은행' AND fin_prdt_nm = '쏠편한 정기예금' AND product_type = 'DEPOSIT';
UPDATE products SET apply_url = 'https://bank.shinhan.com/index.jsp#020102010110' WHERE kor_co_nm = '신한은행' AND fin_prdt_nm = '신한 알.쏠 적금' AND product_type = 'SAVING';
-- [확인 필요 - 반영 제외] 주식회사 하나은행 / 하나의정기예금 / DEPOSIT
-- 원본 URL: https://www.hanabank.com/cont/mall/mall08/mall0801/mall080101/1479088_115126.j네
-- [확인 필요 - 반영 제외] 주식회사 하나은행 / 주거래하나 월복리적금 / SAVING
-- 원본 URL: https://www.hanabank.com/cont/mall/mall08/mall0801/mall080102/1455927_115157.j네
-- [확인 필요 - 반영 제외] 주식회사 하나은행 / 내맘적금 / SAVING
-- 원본 URL: https://www.hanabank.com/cont/mall/mall08/mall0801/mall080102/1461831_115157.j네
UPDATE products SET apply_url = 'https://smartmarket.nonghyup.com/servlet/BFDCW1021R.view' WHERE kor_co_nm = '농협은행주식회사' AND fin_prdt_nm = 'NH왈츠회전예금 II' AND product_type = 'DEPOSIT';
UPDATE products SET apply_url = 'https://smartmarket.nonghyup.com/servlet/BFDCW1021R.view' WHERE kor_co_nm = '농협은행주식회사' AND fin_prdt_nm = 'NH내가Green초록세상예금' AND product_type = 'DEPOSIT';
UPDATE products SET apply_url = 'https://smartmarket.nonghyup.com/servlet/BFDCW1021R.view' WHERE kor_co_nm = '농협은행주식회사' AND fin_prdt_nm = 'NH올원e예금' AND product_type = 'DEPOSIT';
UPDATE products SET apply_url = 'https://smartmarket.nonghyup.com/servlet/BFDCW1021R.view' WHERE kor_co_nm = '농협은행주식회사' AND fin_prdt_nm = 'NH고향사랑기부예금' AND product_type = 'DEPOSIT';
UPDATE products SET apply_url = NULL WHERE kor_co_nm = '농협은행주식회사' AND fin_prdt_nm = 'NH올원e 미니적금' AND product_type = 'SAVING';
UPDATE products SET apply_url = 'https://smartmarket.nonghyup.com/servlet/BFDCW1021R.view' WHERE kor_co_nm = '농협은행주식회사' AND fin_prdt_nm = 'NH1934월복리적금' AND product_type = 'SAVING';
UPDATE products SET apply_url = 'https://smartmarket.nonghyup.com/servlet/BFDCW1021R.view' WHERE kor_co_nm = '농협은행주식회사' AND fin_prdt_nm = 'NH내가Green초록세상적금' AND product_type = 'SAVING';
UPDATE products SET apply_url = 'https://smartmarket.nonghyup.com/servlet/BFDCW1021R.view' WHERE kor_co_nm = '농협은행주식회사' AND fin_prdt_nm = 'NH고향사랑기부적금' AND product_type = 'SAVING';
UPDATE products SET apply_url = 'https://smartmarket.nonghyup.com/servlet/BFDCW1021R.view' WHERE kor_co_nm = '농협은행주식회사' AND fin_prdt_nm = 'NH직장인월복리적금' AND product_type = 'SAVING';
UPDATE products SET apply_url = 'https://www.jbbank.co.kr/' WHERE kor_co_nm = '전북은행' AND fin_prdt_nm = 'JB 다이렉트예금통장 (만기일시지급식)' AND product_type = 'DEPOSIT';
UPDATE products SET apply_url = 'https://www.kbanknow.com/web/product/deposit/codek-fixed' WHERE kor_co_nm = '주식회사 케이뱅크' AND fin_prdt_nm = '코드K 정기예금' AND product_type = 'DEPOSIT';
UPDATE products SET apply_url = 'https://www.standardchartered.co.kr/np/kr/pl/se/SavingDetail.jsp?id=2345' WHERE kor_co_nm = '한국스탠다드차타드은행' AND fin_prdt_nm = 'e-그린세이브예금' AND product_type = 'DEPOSIT';
UPDATE products SET apply_url = 'https://www.suhyup-bank.com/' WHERE kor_co_nm = '수협은행' AND fin_prdt_nm = '헤이(Hey)정기예금' AND product_type = 'DEPOSIT';
UPDATE products SET apply_url = 'https://www.kakaobank.com/products/withdrawal' WHERE kor_co_nm = '주식회사 카카오뱅크' AND fin_prdt_nm = '카카오뱅크 정기예금' AND product_type = 'DEPOSIT';
UPDATE products SET apply_url = 'https://www.kbanknow.com/web/product/deposit/codek-saving' WHERE kor_co_nm = '주식회사 케이뱅크' AND fin_prdt_nm = '코드K 자유적금' AND product_type = 'SAVING';
UPDATE products SET apply_url = 'https://www.suhyup-bank.com/' WHERE kor_co_nm = '수협은행' AND fin_prdt_nm = 'Sh해양플라스틱Zero!적금 (자유적립식)' AND product_type = 'SAVING';
UPDATE products SET apply_url = 'https://www.kakaobank.com/products/savings' WHERE kor_co_nm = '주식회사 카카오뱅크' AND fin_prdt_nm = '카카오뱅크 자유적금' AND product_type = 'SAVING';
UPDATE products SET apply_url = 'https://www.jbbank.co.kr/' WHERE kor_co_nm = '전북은행' AND fin_prdt_nm = 'JB 다이렉트적금(정액적립식)' AND product_type = 'SAVING';
UPDATE products SET apply_url = 'https://www.kbanknow.com/web/product/deposit/primary-saving' WHERE kor_co_nm = '주식회사 케이뱅크' AND fin_prdt_nm = '주거래우대 자유적금' AND product_type = 'SAVING';
