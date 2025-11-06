# 카메라와 이동 로직 검증 계획

## 1. 현재 상황 분석

### ✅ 올바르게 구현된 부분
1. **클라이언트**: `myId`를 사용하여 "내 플레이어"만 추적
   - `GamePanel.updateCamera()`: `players.get(myId)`로 카메라 위치 설정
   - `GamePanel.flushInput()`: 입력을 서버로만 전송 (클라이언트 예측 없음)

2. **서버**: 서버 권위 아키텍처
   - 클라이언트는 입력만 전송 (WASD 마스크)
   - 서버가 모든 위치 계산
   - 서버가 스냅샷 브로드캐스트

3. **카메라**: Viewport가 static 상태 없음
   - 매 프레임 `viewport.setCenter(me.x, me.y)` 호출
   - 다른 플레이어 위치는 카메라에 영향 없음

### ❌ 현재 문제점
1. **컴파일 오류**: comfps-client 모듈에 호환성 문제
   - `NetClient.java`: Protocol.sendAction() 메서드 누락
   - `LobbyFrame.java`: SnapshotV2.Entry 필드 불일치

2. **Spawn 위치 문제**: 
   - 플레이어가 비행기 위치(~1050, ~3300)에 스폰됨
   - 예상 위치: RED (450, 1700), BLUE (2550, 1700)

## 2. 단위 테스트 계획

### 테스트 파일
`comfps-client/src/test/java/com/fpsgame/client/CameraMovementTest.java`

### 테스트 케이스

#### Test 1: `testCameraFollowsOnlyMyPlayer()`
- **Given**: 두 플레이어 (myId=1 at (450, 1700), otherId=2 at (2550, 1700))
- **When**: 카메라 업데이트 (`updateCamera(myId, players, viewport)`)
- **Then**: 카메라는 (450, 1700)에 위치해야 함

#### Test 2: `testCameraIgnoresOtherPlayerMovement()`
- **Given**: 초기 상태, 카메라가 내 위치를 추적 중
- **When**: 상대방 플레이어만 이동 (2550 → 2600)
- **Then**: 카메라는 원래 위치 유지 (내 위치 변경 없음)

#### Test 3: `testCameraFollowsMyPlayerMovement()`
- **Given**: 초기 위치 (450, 1700)
- **When**: 내 플레이어가 이동 (450, 1700) → (500, 1650)
- **Then**: 카메라도 (500, 1650)로 이동

#### Test 4: `testCameraAtMapBoundaries()`
- **테스트 1**: 플레이어 at (0, 0) → 카메라도 (0, 0)
- **테스트 2**: 플레이어 at (3000, 2000) → 카메라도 (3000, 2000)

#### Test 5: `testCameraStaysWhenMyPlayerNotInSnapshot()`
- **Given**: 카메라가 (450, 1700) 추적 중
- **When**: 내 플레이어가 스냅샷에서 사라짐 (연결 끊김)
- **Then**: 카메라는 마지막 위치 유지

## 3. 통합 계획

### Step 1: 컴파일 오류 수정 (우선순위: 높음)
```java
// NetClient.java
- Protocol.sendAction() 메서드 확인/추가

// LobbyFrame.java
- SnapshotV2.Entry 필드 사용 수정 (hp, tacticalCd, ultimateCd)
```

### Step 2: Spawn 위치 수정 (이미 완료)
```java
// SessionRegistry.java
- RED: worldW * 0.15f = 450
- BLUE: worldW * 0.85f = 2550
- Both: worldH * 0.85f = 1700
- createOrUpdateCharacter() BEFORE addPlayer()
```

### Step 3: 단위 테스트 실행
```bash
mvn test -Dtest=CameraMovementTest -pl comfps-client
```

### Step 4: 통합 테스트
1. 서버 실행 (`MainServer`)
2. 클라이언트 2개 실행 (`MainLauncher`)
3. 검증 항목:
   - ✅ 첫 번째 클라이언트: RED 팀, (450, 1700) 스폰
   - ✅ 두 번째 클라이언트: BLUE 팀, (2550, 1700) 스폰
   - ✅ 각 클라이언트 카메라가 자기 캐릭터만 추적
   - ✅ WASD 입력 시 내 캐릭터만 이동
   - ✅ 상대방 캐릭터는 서버 스냅샷으로만 업데이트

## 4. 맵 및 장애물 설명

### Terminal 맵 구조
- **크기**: 3000 x 2000 (worldW x worldH)
- **중앙 비행기**: 약 (1050, 3300) - 맵 밖 (배경 요소)
- **박스 장애물**: 중앙 비행기 주변을 감싸는 박스 모양들
- **플레이 영역**: (0, 0) ~ (3000, 2000)

### Spawn 위치 (수정됨)
- **RED 팀**: (450, 1700) - 맵 왼쪽 하단
- **BLUE 팀**: (2550, 1700) - 맵 오른쪽 하단

## 5. 체크리스트

### 클라이언트 체크
- [x] WELCOME 수신 시 myPlayerId 저장 (`NetClient.onWelcome`, `GamePanel.setMyId()`)
- [x] 입력 처리에서 me(=players.get(myId))만 사용
- [x] 클라이언트 예측 없음 (서버 스냅샷만 사용)
- [x] 다른 플레이어에게 입력 벡터 적용 안 함

### 서버 체크
- [x] ClientSession에 playerId 바인딩
- [x] 커맨드 처리에서 세션 바인딩 playerId만 수정
- [x] 월드 루프에서 각 플레이어만 속도/위치 갱신
- [x] 스냅샷에 모든 플레이어 포함 (브로드캐스트)

### 카메라 체크
- [x] Camera가 static target/position 없음
- [x] GamePanel 렌더 루프에서 항상 me를 따라가도록 보장
- [x] 상태 업데이트에서 마지막 플레이어로 target 갈아치우는 코드 없음

### 상태 동기화 체크
- [x] 클라이언트 예측 없음 (me에만도 적용 안 함)
- [x] 서버 STATE 적용 시 me 좌표는 서버 값 그대로 사용
- [x] 타인도 서버 값 그대로 스냅샷 사용
- [x] 입력 버퍼/시퀀스 번호 없음 (단순 입력 전송)

## 6. 다음 단계

1. **컴파일 오류 수정** → comfps-client 빌드 성공
2. **단위 테스트 실행** → CameraMovementTest 통과
3. **통합 테스트** → MainServer + MainLauncher 실행 및 검증
4. **맵 장애물 검증** → 비행기와 박스 충돌 테스트
