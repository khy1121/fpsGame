# 🎯 FPS 게임 시스템 통합 계획서

**작성일**: 2025-10-31  
**목표**: 모든 기능을 완전한 플레이 가능한 게임으로 통합

---

## 📊 현재 상태 분석

### 모듈 구성
```
comfps-root/
├── comfps-common/    (77개 Java 파일) - 공유 로직
│   ├── Protocol.java        - 네트워크 프로토콜
│   ├── GameEnums.java       - 팀/캐릭터/맵/Phase 정의
│   ├── Character 시스템     - 10개 캐릭터 + 스킬
│   └── 유틸리티 (Vec2, World, Mathf 등)
│
├── comfps-client/    (97개 Java 파일) - 클라이언트
│   ├── model/               - 클라이언트 상태 관리
│   ├── net/                 - 네트워크 통신 레이어
│   ├── ui/                  - Swing UI 컴포넌트
│   └── tools/               - 개발/디버그 도구
│
└── comfps-server/    (15개 Java 파일) - 서버
    ├── ServerMain.java      - 서버 엔트리
    ├── SessionRegistry.java - 세션 관리
    ├── RoundController.java - 라운드 로직
    └── PlayerSyncService.java - 동기화
```

---

## 🎯 통합 목표

### MVP (Minimum Viable Product)
1. **로비 → 캐릭터 선택 → 게임 → 결과** 전체 플로우 동작
2. **10개 캐릭터** 스킬 시스템 작동
3. **3개 맵** 순환 플레이
4. **라운드 기반** 게임 (5라운드, Best of 3)
5. **안정적인 네트워크** 동기화

### 제거 항목 (명세 확인)
- ❌ 아이템 시스템
- ❌ 경제/상점
- ❌ 무기 교체
- ❌ 탄약 시스템

---

## 📅 통합 단계별 계획

### Phase 1: 엔트리 포인트 통합 및 검증 (Day 1)
**목표**: 클라이언트-서버 연결 확립 및 기본 플로우 확인

#### 1.1 서버 검증
- [ ] `ServerMain` 실행 확인
- [ ] TCP 포트 7777 리스닝 확인
- [ ] 세션 연결/해제 로그 확인

#### 1.2 클라이언트 검증  
- [ ] `ClientMain` 실행 확인
- [ ] 서버 연결 성공 확인
- [ ] 기본 채팅 송수신 확인

#### 1.3 통합 테스트
```bash
# 터미널 1: 서버
mvn -pl comfps-server exec:java

# 터미널 2: 클라이언트
mvn -pl comfps-client exec:java
```

**완료 조건**: 
- 서버-클라이언트 연결 성공
- WELCOME 패킷 수신
- PING/PONG 정상 동작

---

### Phase 2: 로비 시스템 통합 (Day 2-3)
**목표**: 로비 UI + READY 시스템 + 팀/캐릭터 선택

#### 2.1 UI 컴포넌트 확인
- [ ] `LobbyPanel` - 로비 메인 UI
- [ ] `LobbyReadyPanel` - READY 버튼 + 플레이어 리스트
- [ ] `CharacterSelectPanel` - 캐릭터 선택 UI
- [ ] `MapVotePanel` - 맵 투표 UI

#### 2.2 네트워크 통합
- [ ] READY 토글 송수신
- [ ] 팀/캐릭터 선택 동기화
- [ ] 맵 투표 집계

#### 2.3 Phase Binder 활성화
- [ ] `LobbyUiInstaller` - 로비 UI 설치
- [ ] `CharacterSelectPhaseBinder` - 캐릭터 선택 바인딩
- [ ] `MapVotePhaseBinder` - 맵 투표 바인딩

**완료 조건**:
- 로비에서 READY 누르면 모든 클라이언트에 반영
- 캐릭터 선택 시 서버에 전송
- 모든 플레이어 READY시 다음 Phase로 전환

---

### Phase 3: 게임 플레이 통합 (Day 4-6)
**목표**: 실제 게임 화면 + 캐릭터 이동 + 스킬 사용

#### 3.1 GameCanvas 연동
- [ ] `GameCanvas` - 게임 메인 렌더링
- [ ] `HudPanel` - 체력/쿨다운 표시
- [ ] `Viewport` - 카메라/스크롤

#### 3.2 캐릭터 시스템 통합
- [ ] Character 클래스 (10종) 로드
- [ ] Ability 시스템 (Basic/Tactical/Ultimate)
- [ ] 쿨다운 타이머 UI 연동

#### 3.3 입력 처리
- [ ] `ClientInputPump` - 키보드/마우스 입력
- [ ] `Keybinds` - 키 매핑
- [ ] 서버로 입력 전송 (Protocol.Input)

#### 3.4 스냅샷 동기화
- [ ] `PlayerSnapshotBuffer` - 위치 보간
- [ ] `SnapshotV2` - 팀/캐릭터 메타데이터
- [ ] 렌더링 루프와 연동

**완료 조건**:
- 캐릭터가 맵에 스폰
- WASD로 이동 가능
- 마우스 에임 표시
- 스킬 키 누르면 쿨다운 시작

---

### Phase 4: 라운드 플로우 통합 (Day 7-8)
**목표**: PHASE 전환 + 카운트다운 + 라운드 결과

#### 4.1 Phase 시스템
- [ ] `ClientPhaseBus` - Phase 이벤트 버스
- [ ] `PhaseHudBinder` - HUD에 Phase 표시
- [ ] Phase별 UI 전환
  - LOBBY → 로비 화면
  - MAP_VOTE → 맵 투표
  - CHARACTER_SELECT → 캐릭터 선택
  - COUNTDOWN → 카운트다운 오버레이
  - ROUND → 게임 플레이

#### 4.2 카운트다운/라운드 결과
- [ ] `CountdownOverlayBinder` - 카운트다운 표시
- [ ] `RoundResultOverlayBinder` - 결과 표시
- [ ] `CenterMessageOverlay` - 중앙 메시지

#### 4.3 서버 로직
- [ ] `RoundController` - 라운드 진행
- [ ] 승리 조건 판정
- [ ] 라운드 종료 → 재시작

**완료 조건**:
- Phase 자동 전환
- 3초 카운트다운 후 라운드 시작
- 라운드 종료 시 결과 표시
- 5라운드 종료 시 매치 종료

---

### Phase 5: 네트워크 안정화 (Day 9-10)
**목표**: 지연 보정 + 에러 처리 + 재연결

#### 5.1 RTT 모니터링
- [ ] `RttMonitor` - 핑 측정
- [ ] `LatencyIndicator` - 레이턴시 표시
- [ ] 보간 딜레이 조정

#### 5.2 예외 처리
- [ ] 연결 끊김 감지
- [ ] 자동 재연결 (`AutoReconnect`)
- [ ] 타임아웃 처리

#### 5.3 동기화 품질
- [ ] 스냅샷 보간 파라미터 튜닝
- [ ] 서버 틱레이트 안정화 (30Hz)
- [ ] 패킷 손실 대응

**완료 조건**:
- 핑 100ms 이하에서 안정적 플레이
- 연결 끊김 시 재연결 시도
- 동기화 오차 최소화

---

### Phase 6: UI/UX 폴리싱 (Day 11-12)
**목표**: 시각적 완성도 + 사용성 개선

#### 6.1 HUD 개선
- [ ] 미니맵 추가
- [ ] 팀원 정보 표시
- [ ] 스킬 쿨다운 애니메이션
- [ ] 데미지 피드백

#### 6.2 설정 UI
- [ ] `SettingsPanel` - 감도/화질/키바인드
- [ ] `KeybindEditorPanel` - 키 재설정
- [ ] 설정 저장/로드

#### 6.3 시각 효과
- [ ] 스킬 이펙트 (`ParticleManager`)
- [ ] 사운드 훅 (`SoundManager`)
- [ ] 화면 쉐이크

**완료 조건**:
- 직관적인 UI
- 설정 변경 가능
- 시각/청각 피드백 충분

---

### Phase 7: 최종 QA 및 릴리스 준비 (Day 13-14)
**목표**: 버그 수정 + 문서화 + 배포

#### 7.1 테스트 시나리오
- [ ] 1vs1 매치 10회
- [ ] 10개 캐릭터 모두 플레이
- [ ] 3개 맵 모두 플레이
- [ ] 네트워크 지연 시뮬레이션

#### 7.2 문서 정리
- [ ] README.md 업데이트
- [ ] PROTOCOL.md 완성
- [ ] 플레이 가이드 작성

#### 7.3 코드 정리
- [ ] 미사용 코드 제거
- [ ] 주석 정리
- [ ] 로그 레벨 조정

**완료 조건**:
- 크리티컬 버그 0개
- 문서 완성
- 릴리스 준비 완료

---

## 🔧 기술 체크리스트

### 네트워크 프로토콜
- [x] CHAT
- [x] WELCOME
- [x] PING/PONG
- [x] PHASE_UPDATE
- [x] COUNTDOWN
- [x] ROUND_RESULT
- [x] READY_STATUS
- [x] SET_SELECTION
- [x] MAP_VOTE
- [ ] SNAPSHOT_V2 (완전 통합)
- [ ] INPUT (클라→서버)

### UI 컴포넌트 상태
- [x] ChatWindow
- [x] LobbyPanel
- [x] CharacterSelectPanel
- [x] MapVotePanel
- [x] GameCanvas
- [x] HudPanel
- [ ] 미니맵 (신규 생성 필요)
- [ ] 팀 스코어보드

### 캐릭터 시스템
- [x] 10개 캐릭터 클래스 구현
- [x] Ability 인터페이스
- [ ] 스킬 → HUD 쿨다운 연동
- [ ] 스킬 → 서버 이펙트 동기화

### 서버 로직
- [x] SessionRegistry
- [x] RoundController
- [x] MapVoteManager
- [ ] PlayerSyncService 완성
- [ ] 충돌 감지
- [ ] 데미지 계산

---

## 🚀 다음 액션

**지금 시작할 작업**:
1. 서버 실행 및 로그 확인
2. 클라이언트 실행 및 연결 테스트
3. 기본 채팅 송수신 검증

**명령어**:
```bash
# 서버 시작
mvn -pl comfps-server exec:java

# 클라이언트 시작 (새 터미널)
mvn -pl comfps-client exec:java
```

**성공 기준**:
- [ ] 서버: "TCP server started on /0.0.0.0:7777"
- [ ] 클라이언트: 연결 UI 표시
- [ ] Connect 클릭 시 WELCOME 수신

---

## 📝 메모

### 우선순위 높은 통합
1. ClientController ↔ UI 연동
2. Phase 자동 전환
3. 캐릭터 스폰/이동

### 주의사항
- EDT 스레드 안전성 (Swing UI 업데이트)
- 네트워크 예외 처리
- 서버 틱 타이밍
