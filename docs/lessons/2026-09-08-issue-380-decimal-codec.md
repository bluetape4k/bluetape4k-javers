# Codec 왕복에서 수치와 표현을 함께 검증한다

BigDecimal을 Long 또는 Double로 축소하면 값이나 scale을 잃는다. 문자열로 변경하면
JSON 타입 계약도 바뀐다. 기존 serializer가 지원하는 Number를 그대로 전달하도록 했다.

기존 코드에서 새 decimal 검사 13건이 실패했다. Map과 12개 binary·압축 codec의
고정밀 값, 큰 정수, 음수, 0, scale, 중첩 구조를 각각 검증한다. 실제 JaVers entity와
이전 Long/Double map payload 읽기를 별도로 확인한다. 이미 손실된 데이터는 복구하지 않는다.

향후 codec 테스트는 JSON 동등성만 비교하지 않고 BigDecimal의 equals와 scale을
확인한다. 새 serializer나 숫자 변환을 도입하기 전에 기존 serializer의 지원 타입을 확인한다.
