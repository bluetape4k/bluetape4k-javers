# Kover Coverage 정책

## 현재 상태

`bluetape4k-javers`는 module test job에서 CI용 Kover XML report를 생성하고,
Nightly에서 full-scope Kover XML report를 집계한다. 현재 어떤 module도 실패를
유발하는 coverage threshold를 강제하지 않는다.

## 정책

상태: report-only transition.

Core diff/audit code와 infrastructure persistence module은 별도 threshold를 가져야
한다. Redis/Kafka test가 외부 service behavior에 의존하기 때문이다.

## Threshold 계획

- Kover를 build gate가 아니라 trend signal로 취급한다.
- CI와 Nightly XML report, coverage artifact upload를 사용해 coverage regression을
  식별한다.
- module에 coverage repair가 필요하면 초점이 좁은 issue를 연다. 기본 enforcement
  mechanism으로 failing threshold를 도입하지 않는다.

## CI/Nightly 계약

CI와 Nightly는 coverage artifact를 upload하고 trend visibility를 유지한다. 요청된
Kover XML report가 없거나 생성될 수 없으면 실패해야 한다. 다만 future issue가 그
gate를 명시적으로 재도입하지 않는 한, module이 고정 coverage percentage 아래에
있다는 이유만으로 실패해서는 안 된다.
