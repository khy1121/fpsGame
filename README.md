# FPS Mini (Java Socket Demo)

Java 소켓 프로그래밍으로 구현한 간단한 FPS 네트워크 데모입니다. 클라이언트/서버 구조, 가벼운 바이너리 프로토콜, Swing 기반 최소 UI를 포함합니다.

## 빠른 시작

- 요구사항: JDK 17+
- 빌드: IDE(Eclipse/IntelliJ)로 import 후 실행 또는 `javac`/`java` 직접 실행

### 서버 실행

- 메인: `src/com/fpsgame/server/ServerMain.java`
- 기본 포트: 7777 (코드에서 변경 가능)

### 클라이언트 실행

- 메인: `src/com/fpsgame/client/ClientMain.java`
- 기본 접속: `127.0.0.1:7777`
- 기능: 채팅, READY 토글, 팀/캐릭터 선택, 맵 투표, Ping, 상태/카운트다운/라운드 결과 표시

## 프로젝트 구조

```
src/
  com/fpsgame/common     # 공통: Protocol, Snapshot, 유틸 등
  com/fpsgame/server     # 서버: 메인/틱 루프/라우터/세션
  com/fpsgame/client     # 클라이언트: NetClient, ClientMain, UI
```

## 네트워크 프로토콜(요약)

상세는 `PROTOCOL.md` 참고. 모든 프레임은 [길이|opcode|payload] 순서로 전송됩니다.

주요 Opcode:
- CHAT, WELCOME, PING/PONG, PHASE_UPDATE, COUNTDOWN, ROUND_RESULT, READY_STATUS
- 선택/맵 투표 관련 전송: READY 토글, 팀/캐릭터 선택, 맵 투표

## 데모 흐름

1) 클라이언트 실행 → 서버 접속(WELCOME 수신)
2) 채팅/READY/선택/맵투표 등 상호작용
3) PHASE_UPDATE/COUNTDOWN/ROUND_RESULT 수신 → HUD/오버레이 갱신

## 개발 노트

- UI는 Swing 기반(EDT 안전성 고려). 네트워크 콜백은 `NetClient`에서 EDT로 디스패치 가능
- 스냅샷 모델: v1(위치/에임), v2(팀/캐릭터 메타 포함) 지원. `PlayerSnapshotBuffer`에서 보간 샘플 제공

## 라이선스

- 수업/데모 목적. 필요 시 프로젝트 내 주석/문서 참고
