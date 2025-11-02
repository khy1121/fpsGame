# FPS Mini (Java Socket Demo)

Java 소켓 프로그래밍으로 구현한 간단한 FPS 네트워크 데모입니다. 클라이언트/서버 구조, 가벼운 바이너리 프로토콜, Swing 기반 최소 UI를 포함합니다.

## 📚 상세 문서

프로젝트의 기술 스택과 주요 코드에 대한 **상세한 설명**을 보려면 다음 문서를 참고하세요:

- **[TECH_STACK.md](TECH_STACK.md)** - 기술 스택 및 코드 상세 분석 (핵심 문서!)
  - 프로젝트 개요 및 아키텍처
  - 기술 스택 상세 설명 (Java 17, Maven, TCP Socket, Swing)
  - 공통/서버/클라이언트 모듈 핵심 클래스 분석
  - 네트워크 프로토콜 상세 설명
  - 게임 플로우 및 FSM 설명
  - 캐릭터 시스템 분석
  - 빌드 및 실행 방법

- **[PROTOCOL.md](PROTOCOL.md)** - 네트워크 프로토콜 명세서
- **[DESIGN.md](DESIGN.md)** - 설계 문서 및 주차별 계획
- **[CHARACTER_SPECS.md](CHARACTER_SPECS.md)** - 캐릭터 및 맵 상세 명세

## 빠른 시작

- 요구사항: JDK 17+
- 빌드: Maven (`mvn clean install`) 또는 IDE(Eclipse/IntelliJ)로 import 후 실행

### 서버 실행

**Maven으로 실행:**
```bash
cd comfps-server
mvn exec:java
```

**또는 IDE에서:**
- 메인: `com.fpsgame.server.ServerMain`
- 기본 포트: 7777 (코드에서 변경 가능)

### 클라이언트 실행

**Maven으로 실행:**
```bash
cd comfps-client
mvn exec:java
```

**또는 IDE에서:**
- 메인: `com.fpsgame.MainLauncher`
- 기본 접속: `127.0.0.1:7777`
- 기능: 채팅, READY 토글, 팀/캐릭터 선택, 맵 투표, Ping, 상태/카운트다운/라운드 결과 표시

## 프로젝트 구조

```
fpsGame/
├── comfps-common/           # 공통: Protocol, Snapshot, 유틸 등
├── comfps-server/           # 서버: 메인/틱 루프/라우터/세션
└── comfps-client/           # 클라이언트: NetClient, UI, 모델
```

**자세한 구조는 [TECH_STACK.md](TECH_STACK.md)를 참고하세요.**

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
