# 네트워크 프로토콜 요약서

코드는 `com.fpsgame.common.Protocol`에 정의되어 있으며, 모든 프레임은 다음 형식을 따릅니다.

- Frame = [length | opcode | payload]
  - length: payload 길이(바이트)
  - opcode: 1바이트
  - payload: opcode 별 직렬화 데이터

## 주요 Opcode

아래 항목은 코드에서 사용되는 대표적인 opcode/페이로드를 요약한 것입니다. 실제 직렬화/역직렬화 로직은 `Protocol` 정적 메서드를 참고하세요.

- CHAT
  - 설명: 채팅 텍스트 전송
  - 송신: `Protocol.sendChat(out, text)`
  - 수신 파서: `Protocol.parseChat(payload)` → `String`

- WELCOME
  - 설명: 접속 환영/환경 정보
  - 파서: `Protocol.parseWelcome(payload)` → `Welcome { myId, worldW, worldH, mapId }`

- PING / PONG
  - 설명: RTT 측정
  - 송신: `Protocol.sendPing(out, nonce)`
  - 파서: `Protocol.parseNonce(payload)` → `long nonce`

- PHASE_UPDATE
  - 설명: 게임 진행 단계 알림
  - 파서: `Protocol.parsePhaseUpdate(payload)` → `int phaseCode`
  - 표시 예: LOBBY/VOTE/COUNTDOWN/ROUND_RUNNING/ROUND_RESULT/MATCH_END

- COUNTDOWN
  - 설명: 카운트다운(초)
  - 파서: `Protocol.parseCountdown(payload)` → `int seconds`

- ROUND_RESULT
  - 설명: 라운드 결과/스코어
  - 파서: `Protocol.parseRoundResult(payload)` → `RoundResult { winnerTeam, blueRounds, redRounds, matchEnded }`

- READY_STATUS
  - 설명: READY 상태 집계(준비 인원/총 인원)
  - 파서: `Protocol.parseReadyStatus(payload)` → `{ ready, total }`

- READY_TOGGLE (클라이언트 → 서버 전송 함수 존재)
  - 설명: READY 상태 변경
  - 송신: `Protocol.sendReadyToggle(out, boolean ready)`

- SET_SELECTION (클라이언트 → 서버 전송)
  - 설명: 팀/캐릭터 선택
  - 송신: `Protocol.sendSetSelection(out, int team, int character)`

- MAP_VOTE (클라이언트 → 서버 전송)
  - 설명: 맵 투표
  - 송신: `Protocol.sendMapVote(out, int mapId)`

## 스냅샷(Snapshot)

- Snapshot v1: 위치(x, y) + 에임(aim) 중심. 파서 `SnapshotV1.parseToMap(payload)` → `Map<Integer, Entry>`
- Snapshot v2: 팀/캐릭터 메타 포함. 파서 `SnapshotV2.parseToMap(payload)` → `Map<Integer, Entry>`
- 클라이언트 보간: `PlayerSnapshotBuffer`
  - 입력: `push(payload, recvNs)`
  - 샘플링: `sample(nowNs)` 또는 `sampleEx(nowNs)`(팀/캐릭터 포함)

## 참고

- 직렬화 포맷/엔디언/문자열 인코딩 등 세부사항은 `Protocol` 구현을 기준으로 하며, 변경 시 본 문서 업데이트 필요
- 신설 opcode 추가 시: 전송 함수, 파서, UI/서버 처리 경로 동시 반영 권장
