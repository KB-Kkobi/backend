INSERT INTO personas
(
    persona_id,
    persona_name,
    axis_code,
    description,
    feature,
    strength,
    caution,
    stock_ratio,
    bond_ratio,
    deposit_ratio
)
VALUES
    (1, '불꽃 추격자', 'HHH',
     '당신은 안정보다 성장에 무게를 두는 사람이에요. 시장이 흔들려도 위기를 기회로 읽고, 남들이 주저할 때 과감하게 움직이는 편이죠. 다만 한 자리에 오래 묶여 있는 건 원치 않아서, 언제든 움직일 수 있는 상태를 선호해요.',
     '기회를 보면 망설이지 않는 당신',
     '기회 포착이 빠르고 과감해요. 하락장을 기회로 활용할 줄 알아요.',
     '급등에 휩쓸린 추격매수는 조심하세요. 분산으로 변동성을 관리하는 게 좋아요.',
     80.00, 5.00, 15.00),

    (2, '스마트 단타러', 'HHL',
     '당신은 변동성 자체를 두려워하지 않지만, 한자리에 오래 머무르지는 않는 사람이에요. 자금을 언제든 뺄 수 있게 두고 시장 흐름에 맞춰 빠르게 갈아타죠. 한 번의 큰 수익보다 현실적인 수익을 여러 번 쌓는 걸 더 좋아해요.',
     '유연하게 흐름을 타는 당신',
     '시장 변화에 대응이 빨라요. 익절과 손절 기준이 분명한 편이에요.',
     '잦은 매매는 수수료·세금으로 수익을 깎아먹어요. 매매 횟수 기준을 정해두면 좋아요.',
     60.00, 10.00, 30.00),

    (3, '야망찬 개척자', 'HLH',
     '당신은 큰 수익을 위해 자금이 묶이는 것을 마다하지 않는 장기 성장 지향형이에요. 단기 등락에는 크게 흔들리지 않고, 시간이 만들어 주는 복리를 믿는 편이죠. 목표가 뚜렷한 만큼 중간에 방향을 잘 바꾸지 않아요.',
     '큰 수익을 향해 끝까지 가는 당신',
     '자금이 묶여도 감수하며 장기 성장을 노려요. 단기 조정에 쉽게 흔들리지 않아요.',
     '장기간 묶이는 만큼 분산으로 위험을 관리하세요. 비상 자금은 따로 남겨두는 게 좋아요.',
     80.00, 15.00, 5.00),

    (4, '신념의 가치투자자', 'HLL',
     '당신은 한번 정한 판단을 쉽게 바꾸지 않는 사람이에요. 단기 등락은 소음으로 여기고, 평가손이 나도 판단이 틀리지 않았다면 자리를 지켜요. 무리한 수익률을 좇기보다 납득할 수 있는 이유가 있을 때 움직여요.',
     '믿고 오래 기다리는 당신',
     '흔들림 없이 원칙을 지켜요. 불필요한 매매가 적어 비용이 낮아요.',
     '확신이 강한 만큼 손실을 오래 끌 수 있어요. 판단을 점검할 기준을 정해두세요.',
     60.00, 30.00, 10.00),

    (5, '실속파 정보통', 'LHH',
     '당신은 큰 위험은 피하면서도 기회는 챙기고 싶은 사람이에요. 자금을 언제든 움직일 수 있게 두고, 조건이 좋은 상품을 부지런히 비교하죠. 안전판을 깔아둔 채로 수익을 노리는 균형형 투자자예요.',
     '틈새 기회를 놓치지 않는 당신',
     '정보 탐색이 부지런하고 비교에 능해요. 위험을 통제하며 수익을 챙겨요.',
     '좋아 보이는 상품마다 손대면 관리가 어려워져요. 상품 수를 적정선으로 유지하세요.',
     40.00, 20.00, 40.00),

    (6, '현금 확보주의자', 'LHL',
     '당신은 돈이 묶이는 상황을 가장 불편해하는 사람이에요. 수익률이 조금 낮더라도 필요할 때 바로 쓸 수 있는지를 먼저 따지죠. 예상치 못한 지출에 대비가 되어 있어 마음이 편한 투자자예요.',
     '언제든 꺼내 쓸 수 있어야 하는 당신',
     '비상 상황 대응력이 뛰어나요. 무리한 투자로 손실을 볼 일이 적어요.',
     '현금 비중이 크면 물가만큼도 못 벌 수 있어요. 여윳돈 일부는 굴려보는 게 좋아요.',
     20.00, 25.00, 55.00),

    (7, '묵묵한 적립왕', 'LLH',
     '당신은 조급하게 승부를 보기보다 시간을 자기 편으로 만드는 사람이에요. 시장이 오르내려도 계획한 만큼 계속 담아가고, 자금이 묶이는 것도 크게 개의치 않죠. 목표는 결코 작지 않아요. 꾸준함으로 큰 결과를 만들려는 유형이에요.',
     '착실하게 불려가는 당신',
     '계획을 꾸준히 지켜요. 적립식으로 단가를 낮추며 목표 수익을 노려요.',
     '목표 없이 쌓기만 하면 점검 시기를 놓쳐요. 채권은 금리에 따라 가격이 움직인다는 점도 함께 챙기세요.',
     40.00, 40.00, 20.00),

    (8, '철저한 금고지기', 'LLL',
     '당신은 원금 보존을 가장 중요하게 생각하는 사람이에요. 수익보다 잃지 않는 것이 우선이고, 예측 가능한 결과를 선호하죠. 자금을 오래 묶어둘 수 있어 만기까지 들고 가는 방식이 잘 맞아요.',
     '원금을 지키는 게 최우선인 당신',
     '손실 위험을 확실하게 차단해요. 자산 계획이 예측 가능해요.',
     '물가 상승만큼 자산 가치가 줄 수 있어요. 채권은 중도매도 시 손실이 날 수 있으니 만기 보유를 전제로 운용하세요.',
     0.00, 70.00, 30.00)
    AS incoming
ON DUPLICATE KEY UPDATE
                     persona_name = incoming.persona_name,
                     axis_code = incoming.axis_code,
                     description = incoming.description,
                     feature = incoming.feature,
                     strength = incoming.strength,
                     caution = incoming.caution,
                     stock_ratio = incoming.stock_ratio,
                     bond_ratio = incoming.bond_ratio,
                     deposit_ratio = incoming.deposit_ratio,
                     updated_at = CURRENT_TIMESTAMP;

-- 페르소나별 이미지 경로를 저장
UPDATE personas
SET image_path = CASE persona_id
                     WHEN 1 THEN '/resources/images/personas/flame-chaser.png'
                     WHEN 2 THEN '/resources/images/personas/smart-trader.png'
                     WHEN 3 THEN '/resources/images/personas/ambitious-pioneer.png'
                     WHEN 4 THEN '/resources/images/personas/value-investor.png'
                     WHEN 5 THEN '/resources/images/personas/practical-analyst.png'
                     WHEN 6 THEN '/resources/images/personas/cash-reserve-keeper.png'
                     WHEN 7 THEN '/resources/images/personas/steady-saver.png'
                     WHEN 8 THEN '/resources/images/personas/vault-keeper.png'
                     ELSE image_path
    END
WHERE persona_id IN (1, 2, 3, 4, 5, 6, 7, 8);