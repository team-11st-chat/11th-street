# Issue #44 채팅 메시지 보관·백업 정책 반영

> 상위 추적 이슈: [#9](https://github.com/team-11st-chat/11th-street/issues/9) · 실행 이슈: [#44](https://github.com/team-11st-chat/11th-street/issues/44) · 근거: [wiki/Policies#실시간-채팅-정책-및-보관-규칙](https://github.com/team-11st-chat/11th-street/wiki/Policies#실시간-채팅-정책-및-보관-규칙)

## MVP 반영 범위

- 활성 메시지 조회는 메인 RDB의 `chat_message.sent_at` 기준 최근 30일 이상인 메시지만 반환한다.
- 30일이 지난 메시지는 활성 조회 대상에서 제외하며, 운영 배치가 도입되면 삭제 직전 JSON 파일로 압축해 AWS S3에 백업한 뒤 메인 RDB에서 Hard Delete한다.
- MVP에서는 백업 이력 RDB 테이블을 별도로 두지 않고, 활성 메시지 테이블에는 조회 가능한 메시지만 남기는 정책을 따른다.

## MVP 제외 범위

- AWS S3 백업 파일 암호화는 초기 MVP 구현 범위에서 제외한다.
- 관리자용 백업 복구 도구는 초기 MVP 구현 범위에서 제외한다.
- 백업 암호화와 복구 자동화는 추후 법적 보안 감사 및 복구 자동화 시점의 확장 작업으로 분리한다.

## 검증

- `ChatMessageRepository` 통합 테스트로 최근 30일 경계 안의 메시지만 반환되고, 30일 이전 메시지는 커서 조회에서도 제외되는지 확인한다.
- `ChatMessageService`는 현재 시각에서 30일을 뺀 기준 시각을 repository 조회 조건으로 전달한다.
