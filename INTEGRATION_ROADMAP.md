# 🗺️ FPS Game 통합 작업 로드맵

> **목표**: 새로운 LobbyFrame과 레거시 시스템을 완전히 통합하여 하나의 일관된 게임 흐름 구현

---

## 📊 현재 상황 분석

### 중복 시스템 발견
- **클라이언트 컨트롤러**: `src/` 와 `comfps-client/` 양쪽에 `ClientController` 존재
- **NetClient**: 마찬가지로 두 곳에 존재
- **UI 시스템**:
  - **새 시스템** (comfps-client): `LobbyFrame`, `MainLauncher`, `ChatPanel`
  - **레거시** (src): `LobbyFlowBootstrap`, `ClientLobbyBootstrap`, `PhaseIntegration`
- **서버**: `comfps-server/`에 통합 완료 ✅

### 통합해야 할 주요 기능
1. READY 상태 동기화 (서버 ↔ 클라이언트)
2. 팀 선택 서버 전송 및 브로드캐스트
3. 맵 투표 시스템 연결
4. 캐릭터 선택 서버 저장
5. Phase 시스템 통합 (LOBBY → VOTE → CHARACTER_SELECT → GAME)
6. 게임 화면 전환

---

## 📋 작업 플로우 (7단계)

### **PHASE 1: 코드 베이스 정리 및 중복 제거** ⏱️ 30분

**목표**: src/와 comfps-client/의 중복 클래스 정리

#### 1.1 중복 파일 식별 및 매핑
```
- ClientController: comfps-client 버전 사용 (최신)
- NetClient: comfps-client 버전 사용
- MainClient: 통합 필요 확인
```

#### 1.2 레거시 제거 대상
```
❌ src/com/fpsgame/client/ClientController.java
❌ src/com/fpsgame/client/NetClient.java
❌ src/com/fpsgame/client/LobbyFlowBootstrap.java
❌ src/com/fpsgame/client/ClientLobbyBootstrap.java
❌ src/com/fpsgame/client/PhaseIntegration.java
```

#### 1.3 유지할 유틸리티
```
✅ src/com/fpsgame/client/ui/* (UI 컴포넌트 재사용 가능)
✅ MapVoteManager 로직
✅ LobbyState 로직
```

**작업 체크리스트**:
- [ ] 중복 파일 목록 확정
- [ ] 의존성 분석 (어떤 코드가 레거시를 참조하는지)
- [ ] 안전하게 제거 또는 주석 처리
- [ ] 빌드 확인

---

### **PHASE 2: ClientController 통합 및 확장** ⏱️ 1시간

**목표**: comfps-client의 ClientController를 메인으로 확정하고 확장

#### 2.1 누락된 메서드 추가
```java
// ClientController.java에 추가할 메서드들
public void sendMapVote(int mapId) throws IOException { ... }
public void sendCharacterSelection(int charId) throws IOException { ... }
public void sendTeamSelection(int team) throws IOException { ... }
// sendReadyToggle(boolean ready)  // ✅ 이미 구현됨!
```

#### 2.2 Protocol 메시지 확인
**파일**: `comfps-common/src/main/java/com/fpsgame/common/Protocol.java`

필요한 메시지:
- `SELECTION` (team, character)
- `MAP_VOTE`
- `READY_TOGGLE`

#### 2.3 통합 테스트
- [ ] 메시지 송신 테스트
- [ ] 서버 수신 확인
- [ ] 브로드캐스트 동작 확인

**작업 파일**:
- `comfps-client/src/main/java/com/fpsgame/ClientController.java`
- `comfps-common/src/main/java/com/fpsgame/common/Protocol.java`

---

### **PHASE 3: LobbyFrame 서버 연동** ⏱️ 1.5시간

**목표**: LobbyFrame을 서버 상태와 완전히 동기화

#### 3.1 READY 상태 동기화
```java
// LobbyFrame.java
@Override
public void onReadyStatus(int ready, int total) {
    SwingUtilities.invokeLater(() -> {
        // 서버 브로드캐스트 수신
        // 다른 플레이어의 READY 상태를 슬롯에 표시
        // UI 업데이트: "Player-123 (READY)" 형식
        chatPanel.appendSystemMessage("READY: " + ready + "/" + total);
    });
}
```

#### 3.2 팀 선택 서버 전송
```java
private void selectTeam(int team) {
    // READY 상태일 때는 팀 변경 불가
    if (isReady) {
        chatPanel.appendSystemMessage("READY 상태에서는 팀 변경이 불가능합니다.");
        return;
    }
    
    selectedTeam = team;
    // 팀 버튼 UI 업데이트...
    
    // 서버로 팀 선택 전송 ⭐ 추가
    try {
        controller.sendTeamSelection(team);
    } catch (IOException ex) {
        chatPanel.appendSystemMessage("팀 선택 전송 실패: " + ex.getMessage());
    }
}
```

#### 3.3 다른 플레이어 정보 표시
```java
// onWelcome() 확장
@Override
public void onWelcome(Protocol.Welcome welcome) {
    myId = welcome.myId;
    // 서버에서 플레이어 목록 받기
    // 각 팀 슬롯에 다른 플레이어 표시
    // 실시간 업데이트
}
```

**작업 체크리스트**:
- [ ] `onReadyStatus()` 구현
- [ ] `selectTeam()`에 서버 전송 추가
- [ ] 플레이어 목록 브로드캐스트 메시지 정의
- [ ] 슬롯 UI 업데이트 로직 구현

**작업 파일**:
- `comfps-client/src/main/java/com/fpsgame/client/ui/LobbyFrame.java`
- `comfps-client/src/main/java/com/fpsgame/ClientController.java`

---

### **PHASE 4: 맵 투표 시스템 연결** ⏱️ 1시간

**목표**: 맵 선택이 서버와 동기화되도록 구현

#### 4.1 MapInfoPanel 이벤트 추가
```java
// MapInfoPanel.java
private void loadMapImage(String mapId, JLabel imageLabel) {
    // 기존 이미지 로딩...
    
    // 클릭 리스너 추가 ⭐
    imageLabel.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (onMapSelected != null) {
                onMapSelected.accept(mapId);
            }
        }
    });
}

// 콜백 설정
public void setOnMapSelected(Consumer<String> callback) {
    this.onMapSelected = callback;
}
```

#### 4.2 LobbyFrame에서 연결
```java
// LobbyFrame.java - buildUI()
mapInfoPanel.setOnMapSelected(mapId -> {
    try {
        int mapIdInt = mapIdToInt(mapId); // 맵 ID 변환
        controller.sendMapVote(mapIdInt);
        chatPanel.appendSystemMessage("맵 투표: " + mapId);
    } catch (IOException ex) {
        chatPanel.appendSystemMessage("맵 투표 실패: " + ex.getMessage());
    }
});
```

#### 4.3 서버 집계 결과 수신
```java
// onWelcome() 또는 새 Protocol 메시지로 맵 결정 받기
// 서버 MapVoteManager가 승자 결정
// 월드 사이즈 브로드캐스트
// UI에 최종 선택 맵 표시
```

#### 4.4 투표 현황 표시 (선택사항)
- 각 맵별 득표 수 표시
- 실시간 업데이트

**작업 체크리스트**:
- [ ] MapInfoPanel 클릭 리스너 추가
- [ ] 맵 ID 매핑 함수 구현
- [ ] `sendMapVote()` 호출 연결
- [ ] 서버 집계 결과 UI 반영

**작업 파일**:
- `comfps-client/src/main/java/com/fpsgame/client/ui/MapInfoPanel.java`
- `comfps-client/src/main/java/com/fpsgame/client/ui/LobbyFrame.java`
- `comfps-server/src/main/java/com/fpsgame/GameServer.java`

---

### **PHASE 5: 캐릭터 선택 시스템 연결** ⏱️ 45분

**목표**: 캐릭터 선택이 서버에 저장되도록 구현

#### 5.1 CharacterSelectPanel 이벤트 추가
```java
// CharacterSelectPanel.java
private JComponent buildCard(String characterId) {
    // ...
    JButton choose = new JButton("Choose");
    choose.addActionListener(e -> sendSelection(characterId));
    // ...
}

private void sendSelection(String characterId) {
    Consumer<String> sender = this.selectionSender;
    if (sender != null) {
        sender.accept(characterId);
    }
}
```

#### 5.2 LobbyFrame에서 연결
```java
// LobbyFrame.java
charSelectPanel.setOnCharacterSelected(charId -> {
    try {
        int charIdInt = charIdToInt(charId); // 캐릭터 ID 변환
        controller.sendCharacterSelection(charIdInt);
        chatPanel.appendSystemMessage("캐릭터 선택: " + charId);
    } catch (IOException ex) {
        chatPanel.appendSystemMessage("캐릭터 선택 실패: " + ex.getMessage());
    }
});
```

#### 5.3 서버 확인 및 저장
- 서버가 플레이어의 선택 저장
- 브로드캐스트 또는 onWelcome으로 확인

#### 5.4 UI 피드백
- 선택 완료 표시
- 다른 플레이어의 선택도 표시 (선택사항)

**작업 체크리스트**:
- [ ] CharacterSelectPanel 콜백 설정
- [ ] 캐릭터 ID 매핑 함수 구현
- [ ] `sendCharacterSelection()` 호출 연결
- [ ] 서버 저장 확인

**작업 파일**:
- `comfps-client/src/main/java/com/fpsgame/client/ui/CharacterSelectPanel.java`
- `comfps-client/src/main/java/com/fpsgame/client/ui/LobbyFrame.java`

---

### **PHASE 6: Phase 시스템 통합** ⏱️ 1.5시간

**목표**: 게임 진행 Phase에 따라 UI 자동 전환

#### 6.1 Phase Enum 정의 확인
**파일**: `comfps-common/src/main/java/com/fpsgame/common/GameEnums.java`

```java
public enum Phase {
    LOBBY,              // 대기실
    VOTE,               // 맵 투표
    CHARACTER_SELECT,   // 캐릭터 선택
    COUNTDOWN,          // 시작 카운트다운
    PLAYING,            // 게임 중
    ROUND_END           // 라운드 종료
}
```

#### 6.2 LobbyFrame Phase 리스너 추가
```java
// LobbyFrame.java
@Override
public void onPhaseUpdate(int phaseCode) {
    SwingUtilities.invokeLater(() -> {
        switch (phaseCode) {
            case 0: // LOBBY
                // 초기 화면
                tabbedPane.setSelectedIndex(0); // Map Info
                chatPanel.appendSystemMessage("=== 로비 단계 ===");
                break;
                
            case 1: // VOTE
                // Map Info 탭 활성화
                tabbedPane.setSelectedIndex(0);
                chatPanel.appendSystemMessage("=== 맵 투표 시작 ===");
                break;
                
            case 2: // CHARACTER_SELECT
                // Character Select 탭 활성화
                tabbedPane.setSelectedIndex(1);
                chatPanel.appendSystemMessage("=== 캐릭터 선택 시작 ===");
                break;
                
            case 3: // COUNTDOWN
                // 시작 카운트다운 표시
                chatPanel.appendSystemMessage("=== 게임 곧 시작! ===");
                break;
                
            default:
                chatPanel.appendSystemMessage("PHASE=" + phaseCode);
        }
    });
}
```

#### 6.3 Phase 전환 애니메이션 (선택사항)
- 탭 자동 전환
- 시각적 피드백
- 사운드 효과

**작업 체크리스트**:
- [ ] Phase enum 확인
- [ ] `onPhaseUpdate()` 구현
- [ ] 각 Phase별 UI 동작 정의
- [ ] 테스트 (서버에서 Phase 변경 시뮬레이션)

**작업 파일**:
- `comfps-client/src/main/java/com/fpsgame/client/ui/LobbyFrame.java`
- `comfps-common/src/main/java/com/fpsgame/common/GameEnums.java`

---

### **PHASE 7: 게임 화면 연결 및 최종 통합** ⏱️ 2시간

**목표**: 로비 → 게임 → 결과 전체 흐름 완성

#### 7.1 게임 화면 전환
```java
// LobbyFrame.java
@Override
public void onPhaseUpdate(int phaseCode) {
    SwingUtilities.invokeLater(() -> {
        if (phaseCode == 4) { // PLAYING
            // LobbyFrame 숨기고 GameFrame 표시
            this.setVisible(false);
            if (gameFrame == null) {
                gameFrame = new GameFrame(controller);
            }
            gameFrame.setVisible(true);
        }
    });
}
```

#### 7.2 게임 종료 후 복귀
```java
// GameFrame.java
@Override
public void onRoundResult(Protocol.RoundResult rr) {
    // 라운드 결과 표시
    // 매치 종료 시 LobbyFrame으로 복귀
    if (rr.matchEnded) {
        this.setVisible(false);
        lobbyFrame.setVisible(true);
        lobbyFrame.resetState(); // 상태 초기화
    }
}
```

#### 7.3 전체 플로우 테스트
```
런처 → 로비 → 맵투표 → 캐릭터선택 → 게임 → 결과 → 로비
```

#### 7.4 레거시 코드 정리
```
삭제 또는 아카이빙:
- src/com/fpsgame/client/LobbyFlowBootstrap.java
- src/com/fpsgame/client/ClientLobbyBootstrap.java
- src/com/fpsgame/client/PhaseIntegration.java
- 기타 사용하지 않는 UI 파일들
```

**작업 체크리스트**:
- [ ] GameFrame 연결
- [ ] Phase 전환 로직 구현
- [ ] 라운드 결과 처리
- [ ] 상태 초기화 메서드 구현
- [ ] 전체 플로우 통합 테스트
- [ ] 레거시 코드 정리

**작업 파일**:
- `comfps-client/src/main/java/com/fpsgame/MainLauncher.java`
- `comfps-client/src/main/java/com/fpsgame/client/ui/LobbyFrame.java`
- `comfps-client/src/main/java/com/fpsgame/ui/GameFrame.java`

---

## 🎯 우선순위별 작업 순서

### 옵션 A: 빠른 완성 (2~3시간)
```
PHASE 2 → PHASE 3 → PHASE 4 → PHASE 5
(컨트롤러 확장 → READY → 맵투표 → 캐릭터)
```

### 옵션 B: 완전 통합 (5~7시간)
```
PHASE 1 → PHASE 2 → PHASE 3 → PHASE 4 → PHASE 5 → PHASE 6 → PHASE 7
(정리 → 컨트롤러 → READY → 맵 → 캐릭터 → Phase → 게임전환)
```

### 옵션 C: 단계별 검증 (각 기능별 완성 후 다음 단계)
```
PHASE 2 (테스트) → PHASE 3 (테스트) → PHASE 4 (테스트) → ...
```

---

## 💡 추가로 통합해야 할 시스템들

### 1. 네트워크 동기화
- ✅ Protocol 메시지 정의 완료
- ⚠️ 플레이어 목록 브로드캐스트 필요
- ⚠️ 팀 변경 브로드캐스트 필요
- ⚠️ 맵 투표 현황 브로드캐스트

### 2. 서버 상태 관리
- ✅ LobbyState (READY 관리)
- ✅ MapVoteManager (맵 투표)
- ⚠️ 팀 배정 관리 추가 필요
- ⚠️ 캐릭터 선택 저장 추가 필요

### 3. UI 동기화
- ⚠️ 다른 플레이어의 팀/캐릭터 선택 실시간 표시
- ⚠️ 투표 현황 실시간 업데이트
- ⚠️ Phase 전환 시 UI 자동 변경
- ⚠️ 슬롯 실시간 업데이트

### 4. 게임 진행 통합
- ⚠️ Phase FSM 연동
- ⚠️ 라운드 시작/종료 처리
- ⚠️ 스코어 관리
- ⚠️ 게임 화면 ↔ 로비 화면 전환

---

## 📝 진행 상황 추적

### 완료된 작업
- [x] 기본 LobbyFrame UI 구현
- [x] MainLauncher 구현
- [x] ChatPanel 구현
- [x] 팀 버튼 스타일링
- [x] READY/CANCEL 버튼 로직
- [x] 채팅 포맷 ("[닉네임] : 메시지")
- [x] OptionsWindow 통합
- [x] 팀 선택 시 READY 상태 잠금

### 진행 중인 작업
- [ ] PHASE 1: 코드 정리
- [ ] PHASE 2: ClientController 확장
- [ ] PHASE 3: 서버 연동
- [ ] PHASE 4: 맵 투표
- [ ] PHASE 5: 캐릭터 선택
- [ ] PHASE 6: Phase 시스템
- [ ] PHASE 7: 게임 화면 연결

---

## 🚀 시작 가이드

1. **현재 브랜치 확인**
   ```bash
   git branch
   # feature/multi-module
   ```

2. **최신 상태 동기화**
   ```bash
   git pull origin feature/multi-module
   ```

3. **새 작업 브랜치 생성 (선택)**
   ```bash
   git checkout -b feature/integration-phase-1
   ```

4. **작업 시작**
   - 원하는 PHASE 선택
   - 체크리스트 참고하여 진행
   - 각 단계마다 커밋

5. **테스트**
   ```bash
   mvn clean compile
   mvn -pl comfps-server exec:java  # 서버 실행
   mvn -pl comfps-client exec:java  # 클라이언트 실행
   ```

---

## 📚 참고 자료

### 주요 파일 위치
```
comfps/
├── comfps-common/
│   └── src/main/java/com/fpsgame/common/
│       ├── Protocol.java           # 네트워크 프로토콜
│       └── GameEnums.java          # Phase, Team, Character 등
│
├── comfps-client/
│   └── src/main/java/com/fpsgame/
│       ├── MainLauncher.java       # 런처
│       ├── ClientController.java   # 네트워크 컨트롤러
│       └── client/ui/
│           ├── LobbyFrame.java     # 로비 메인
│           ├── ChatPanel.java      # 채팅
│           ├── MapInfoPanel.java   # 맵 정보
│           └── CharacterSelectPanel.java
│
└── comfps-server/
    └── src/main/java/com/fpsgame/
        ├── GameServer.java         # 게임 서버
        ├── LobbyState.java         # 로비 상태
        ├── MapVoteManager.java     # 맵 투표
        └── RoundController.java    # 라운드 FSM
```

### 네트워크 메시지
```java
// 클라이언트 → 서버
Protocol.sendChat(out, message)
Protocol.sendReadyToggle(out, ready)
Protocol.sendMapVote(out, mapId)         // 구현 필요
Protocol.sendSelection(out, team, char)  // 구현 필요

// 서버 → 클라이언트
Protocol.WELCOME
Protocol.CHAT
Protocol.PHASE_UPDATE
Protocol.COUNTDOWN
Protocol.READY_STATUS
Protocol.ROUND_RESULT
```

---

## ⚠️ 주의사항

1. **인코딩**: 모든 파일 UTF-8 without BOM 유지
2. **폰트**: 한글은 "맑은 고딕" 사용
3. **스레드 안전성**: UI 업데이트는 반드시 `SwingUtilities.invokeLater()` 사용
4. **에러 처리**: 네트워크 메서드는 IOException catch 필수
5. **상태 관리**: 서버가 Single Source of Truth
6. **커밋**: 각 PHASE 완료 시마다 커밋

---

**작성일**: 2025-10-31  
**버전**: 1.0  
**작성자**: GitHub Copilot  
**프로젝트**: FPS Game (Multi-Module)
