# FPS Game (comfps)

Java 소켓 프로그래밍으로 구현한 멀티플레이어 FPS 게임입니다. 서버 권위형 아키텍처, 바이너리 프로토콜, Swing 기반 UI를 포함합니다.

## 📋 주요 기능

- ✅ 멀티플레이어 서버-클라이언트 (TCP 소켓)
- ✅ 10개 캐릭터 시스템 (각 캐릭터별 고유 능력)
- ✅ 3개 맵 (terminal, neonCity, forestOutpost)
- ✅ 팀 기반 게임플레이 (RED vs BLUE)
- ✅ 실시간 동기화 (30Hz 서버 틱, 60fps 클라이언트 렌더링)
- ✅ HUD, 미니맵, 크로스헤어
- ✅ 맵 투표 시스템
- ✅ 채팅 시스템

## 🚀 빠른 시작

### 요구사항
- JDK 17+
- Maven 3.6+

### 1. 프로젝트 빌드
```bash
mvn clean package -DskipTests
```

### 2. 서버 실행
```bash
java -cp "comfps-server/target/comfps-server-1.0-SNAPSHOT.jar;comfps-common/target/comfps-common-1.0-SNAPSHOT.jar" com.fpsgame.server.MainServer
```

서버 포트: **7777**

### 3. 클라이언트 실행 (여러 창 가능)
```bash
java -cp "comfps-client/target/comfps-client-1.0-SNAPSHOT.jar;comfps-common/target/comfps-common-1.0-SNAPSHOT.jar" com.fpsgame.MainLauncher
```

### 4. 게임 조작
- **이동**: WASD
- **조준**: 마우스
- **기본 공격**: 마우스 좌클릭
- **전술 스킬**: E
- **궁극기**: Q
- **미니맵 토글**: M

## 📁 프로젝트 구조

```
comfps/
├── comfps-common/          # 공통 모듈
│   ├── Protocol.java       # 네트워크 프로토콜
│   ├── SnapshotV2.java     # 플레이어 상태 동기화
│   ├── ProjectilesV2.java  # 투사체 동기화
│   └── character/          # 캐릭터 시스템
│       ├── Character.java
│       ├── types/          # 10개 캐릭터 클래스
│       └── projectile/     # 투사체 타입
├── comfps-client/          # 클라이언트 모듈
│   ├── MainLauncher.java   # 메인 엔트리
│   ├── ui/                 # UI 컴포넌트
│   │   ├── GamePanel.java  # 게임 화면 렌더링
│   │   ├── LobbyFrame.java # 로비 UI
│   │   └── ChatPanel.java  # 채팅
│   └── net/                # 네트워크 클라이언트
└── comfps-server/          # 서버 모듈
    ├── MainServer.java     # 메인 엔트리
    ├── GameServer.java     # 게임 루프 (30Hz)
    ├── SessionRegistry.java # 세션 관리
    └── RoundController.java # 라운드 관리
```

## 🎮 게임 플로우

1. **로비**: 닉네임 입력, 설정
2. **맵 투표**: 3개 맵 중 투표
3. **캐릭터 선택**: 팀 선택 + 캐릭터 선택
4. **카운트다운**: 게임 시작 전 준비
5. **게임 플레이**: 5라운드 Best of 3
6. **라운드 결과**: 승리/패배 표시

## 📡 네트워크 프로토콜

상세는 `PROTOCOL.md` 참고. 모든 프레임은 [길이|opcode|payload] 형식.

주요 Opcode:
- **CHAT**: 채팅 메시지
- **WELCOME**: 서버 연결 성공
- **PHASE**: 게임 단계 전환
- **SNAPSHOT**: 플레이어 위치/상태 동기화
- **PROJECTILES**: 투사체 동기화
- **ACTION**: 스킬 사용

## 🎨 캐릭터 목록

1. **Raven** - 공격형 (빠른 이동, 대쉬)
2. **Piper** - 저격수 (장거리 저격)
3. **Bulldog** - 탱커 (미니건, 방어)
4. **Sage** - 지원형 (힐링)
5. **Ghost** - 암살자 (은신)
6. **Sniper** - 정밀 저격수
7. **Tank** - 방어형 (샷건, 방어막)
8. **General** - 지휘관 (버프)
9. **Technician** - 엔지니어 (지뢰, 터렛)
10. **Wildcat** - 돌격병

## 📚 추가 문서

### 시스템 설계
- `CAMERA_SYSTEM.md` - **카메라 시스템 설계 및 구현** (2025-11-05 추가)
- `DESIGN.md` - 전체 시스템 설계
- `PROTOCOL.md` - 네트워크 프로토콜 명세
- `CHARACTER_SPECS.md` - 캐릭터 상세 사양

### 프로젝트 관리
- `PROJECT_STATUS.md` - 프로젝트 현황
- `SPRINT_BACKLOG_W10_W14.md` - 스프린트 진행 상황
- `INTEGRATION_PLAN.md` - 통합 계획

### 버그 & 수정
- `BUG_FIXES.md` - **버그 수정 리포트** (2025-11-05 추가)

### 실행 가이드
- `RUNNING.md` - 상세 실행 가이드
- `10주차_완성현황.md` - 현재 완성도 보고서

## 🧪 테스트

### 단위 테스트 실행
```bash
# 전체 테스트
mvn test

# 특정 모듈 테스트
mvn test -pl comfps-client
mvn test -pl comfps-server
mvn test -pl comfps-common

# 특정 테스트 클래스
mvn test -Dtest=GamePanelCameraFollowTest -pl comfps-client
```

### 테스트 커버리지
- ✅ 카메라 추적 테스트 (`GamePanelCameraFollowTest`)
- 🔶 네트워크 프로토콜 테스트 (일부)
- ❌ 전투 시스템 테스트 (미구현)

## 📊 현재 완성도

**전체**: 87%  
- ✅ 네트워크 시스템 (100%)
- ✅ GamePanel 렌더링 (100%)
- ✅ 카메라 시스템 (100%) ⬆️ 2025-11-05 개선
- ✅ HUD 시스템 (95%)
- 🔶 파티클/이펙트 (40%)
- ❌ 사운드 시스템 (0%)

## 🐛 최근 버그 수정 (2025-11-05)

- ✅ **Issue #2**: 십자선 위치 오류 수정
  - 문제: 십자선이 캐릭터 월드 좌표에 그려짐
  - 해결: 화면 중앙에 고정
  - 상세: `BUG_FIXES.md` 참고

- ✅ **카메라 시스템 검증**
  - 테스트 케이스 추가
  - 문서화 완료
  - 상세: `CAMERA_SYSTEM.md` 참고

## 🔧 개발 환경

- **언어**: Java 17
- **빌드**: Maven (멀티모듈)
- **UI**: Swing (Graphics2D)
- **네트워크**: TCP 소켓, Binary 프로토콜

## 📝 라이선스

교육/데모 목적의 프로젝트입니다.

---

**개발자**: [프로젝트 팀]  
**최종 업데이트**: 2025-11-05  
**버전**: 1.2.0 (카메라 시스템 개선)
