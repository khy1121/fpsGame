# 카메라 시스템 설계 및 구현

## 개요

이 문서는 FPS 게임 클라이언트의 카메라 시스템과 관련 버그 수정 내용을 설명합니다.

## 카메라 시스템 아키텍처

### 핵심 개념

우리 게임은 **탑다운 뷰(Top-Down View)** 방식의 멀티플레이어 슈팅 게임으로, 다음과 같은 카메라 특성을 가집니다:

1. **플레이어 중심 카메라**: 카메라는 항상 내 캐릭터를 중심으로 위치
2. **고정 줌 레벨**: 2.0배 줌으로 전체 맵의 절반 크기만 보임
3. **화면 중앙 십자선**: 조준 표시기는 화면 중앙에 고정

### 좌표 시스템

```
┌─────────────────────────────────────┐
│         월드 좌표계 (World)          │
│  - 맵 전체: 6000 x 4000 units       │
│  - 플레이어 위치: (x, y) in world   │
└─────────────────────────────────────┘
                 ↓
         Viewport 변환
                 ↓
┌─────────────────────────────────────┐
│        스크린 좌표계 (Screen)        │
│  - 화면 크기: 가변 (예: 800x600px)  │
│  - 플레이어: 항상 화면 중앙         │
│  - 십자선: (width/2, height/2)      │
└─────────────────────────────────────┘
```

## 주요 컴포넌트

### 1. Viewport 클래스

**위치**: `src/com/fpsgame/client/model/Viewport.java`

**역할**:
- 월드 좌표 ↔ 스크린 좌표 변환
- 카메라 중심점 관리
- 줌/스케일 관리

**주요 메서드**:
```java
// 카메라 중심 설정 (플레이어 위치)
void setCenter(float worldX, float worldY)

// 월드 좌표 → 스크린 좌표
Point worldToScreen(float wx, float wy)

// 스크린 좌표 → 월드 좌표 (마우스 조준용)
Vec2 screenToWorld(int sx, int sy)

// 줌 레벨 설정
void setScale(float scale)
```

### 2. GamePanel 클래스

**위치**: `comfps-client/src/main/java/com/fpsgame/client/ui/GamePanel.java`

**카메라 관련 주요 메서드**:

#### `updateCamera()`
```java
private void updateCamera() {
    SnapshotV2.Entry me = getMyPlayer();
    if (me != null) {
        // 카메라를 내 플레이어 위치로 이동
        viewport.setCenter(me.x, me.y);
    }
}
```

#### `drawCrosshair(Graphics2D g)`
```java
private void drawCrosshair(Graphics2D g) {
    int w = getWidth();
    int h = getHeight();
    
    // 화면 중앙 고정 (카메라가 캐릭터를 따라가므로)
    int cx = w / 2;
    int cy = h / 2;
    
    // 십자선 렌더링...
}
```

#### `updateAimFromMouse(Point p)`
```java
private void updateAimFromMouse(Point p) {
    SnapshotV2.Entry me = getMyPlayer();
    if (me == null) return;
    
    // 마우스 스크린 좌표를 월드 좌표로 변환
    Vec2 worldPos = viewport.screenToWorld(p.x, p.y);
    
    // 내 캐릭터 위치에서 마우스 위치로의 각도 계산
    float dx = worldPos.x - me.x;
    float dy = worldPos.y - me.y;
    aimRad = (float) Math.atan2(dy, dx);
}
```

## 버그 수정 내역

### Issue #2: 조준 표시기 위치 오류

**문제**: 
- 십자선이 캐릭터의 월드 위치에 그려짐
- 카메라가 캐릭터를 따라가므로, 십자선도 함께 이동하여 화면에서 사라짐

**원인**:
```java
// ❌ 잘못된 구현 (수정 전)
Point myScreenPos = viewport.worldToScreen(me.x, me.y);
int cx = myScreenPos.x;
int cy = myScreenPos.y;
```

카메라가 캐릭터를 추적하므로, 캐릭터의 월드 좌표를 스크린 좌표로 변환하면 항상 화면 중앙 근처가 됩니다. 하지만 렌더링 순서나 타이밍에 따라 정확히 중앙이 아닐 수 있어 시각적 오류가 발생했습니다.

**해결책**:
```java
// ✅ 올바른 구현 (수정 후)
int cx = w / 2;
int cy = h / 2;
```

십자선을 단순히 화면 중앙에 고정합니다. 카메라가 이미 캐릭터를 따라가므로, 화면 중앙 = 캐릭터 위치입니다.

**커밋**: `Fix crosshair positioning to screen center`

### 테스트 추가

**파일**: `comfps-client/src/test/java/com/fpsgame/client/ui/GamePanelCameraFollowTest.java`

**목적**: 카메라가 플레이어를 정확히 추적하는지 검증

**테스트 시나리오**:
1. 플레이어를 `(1500, 1000)` 위치에 배치
2. 카메라 중심이 `(1500, 1000)`인지 확인
3. 줌 레벨이 3.0배인지 확인
4. 플레이어를 `(2000, 1200)`으로 이동
5. 카메라가 새 위치를 따라가는지 확인
6. 월드 원점의 스크린 좌표 변화 검증

**결과**: ✅ 모든 테스트 통과

```
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## 입력 처리 시스템

### 클라이언트 측

```java
// WASD 키 상태 추적
private volatile boolean keyW, keyA, keyS, keyD;

// 입력 타이머 (30Hz)
private Timer inputTimer;

// 입력을 서버로 전송
private void flushInput() {
    byte mask = 0;
    if (keyW) mask |= 1;  // UP
    if (keyS) mask |= 2;  // DOWN
    if (keyA) mask |= 4;  // LEFT
    if (keyD) mask |= 8;  // RIGHT
    
    InputSender sender = inputSender;
    if (sender != null) {
        sender.send(mask, aimRad);
    }
}
```

### 서버 측

**파일**: `comfps-server/src/main/java/com/fpsgame/server/PlayerSyncService.java`

```java
// 클라이언트 입력 수신
public void onInputFrame(ServerSession session, byte mask, Float aimNullable) {
    PlayerState ps = players.get(session.getId());
    if (ps != null) {
        ps.inputMask = mask;
        ps.aim = (aimNullable != null) ? aimNullable : 0f;
    }
}

// 물리 업데이트 (20Hz tick)
public void tick(double dt) {
    for (PlayerState ps : players.values()) {
        // 입력 마스크 기반으로 속도 계산
        float vx = 0, vy = 0;
        if ((ps.inputMask & 4) != 0) vx -= moveSpeed; // A (LEFT)
        if ((ps.inputMask & 8) != 0) vx += moveSpeed; // D (RIGHT)
        if ((ps.inputMask & 1) != 0) vy -= moveSpeed; // W (UP)
        if ((ps.inputMask & 2) != 0) vy += moveSpeed; // S (DOWN)
        
        // 위치 업데이트
        ps.x += vx * dt;
        ps.y += vy * dt;
        
        // 맵 경계 제한...
    }
}
```

## 네트워크 프로토콜

### SnapshotV2 프로토콜

**형식**: 바이너리, 모든 플레이어 상태 포함

**구조**:
```
[플레이어 수: 1 byte]
[플레이어 1 데이터]
  - id: 4 bytes (int)
  - x: 4 bytes (float)
  - y: 4 bytes (float)
  - aim: 4 bytes (float)
  - team: 1 byte
  - characterId: 1 byte
  - hp: 4 bytes (int)
  - tacticalCooldown: 4 bytes (float)
  - ultimateCooldown: 4 bytes (float)
[플레이어 2 데이터]
...
```

### 동기화 흐름

```
클라이언트 1                  서버                   클라이언트 2
    |                          |                          |
    |-- 입력 (W키) ----------->|                          |
    |   mask=1, aim=0.5        |                          |
    |                          |                          |
    |                      [물리 업데이트]                |
    |                      id=1 위치 변경                 |
    |                          |                          |
    |<----- 스냅샷 ------------|---------- 스냅샷 -------->|
    |   id=1(x,y) id=2(x,y)   |   id=1(x,y) id=2(x,y)   |
    |                          |                          |
  [렌더링]                                            [렌더링]
  - id=1: 내 캐릭터 (카메라 추적)                    - id=1: 다른 플레이어
  - id=2: 다른 플레이어                              - id=2: 내 캐릭터 (카메라 추적)
```

## 실제 로그 분석

### 정상 동작 로그

```
[GamePanel] ★ myId=1 입력 전송: mask=8 W=false S=false A=false D=true
[NetClient] ★ SNAPSHOT 수신: id=1(3125,610) id=2(3661,3612)
[GamePanel] ★ myId=1 스냅샷 받음: id=1★MY★ pos=(3124.9,610.3) id=2 pos=(3660.9,3612.3)
```

**분석**:
1. **입력 전송**: D키 누름 (mask=8)
2. **스냅샷 수신**: 내 캐릭터(id=1) x좌표 증가 (오른쪽 이동)
3. **렌더링**: id=1에 ★MY★ 표시 - 카메라가 추적 중

### 멀티플레이어 동작

```
클라이언트 1 로그:
[GamePanel] ★ myId=1 입력 전송: mask=4 W=false S=false A=true D=false
[GamePanel] ★ myId=1 스냅샷 받음: id=1★MY★ pos=(3117.4,610.3) id=2 pos=(3660.9,3612.3)

클라이언트 2 로그:
[GamePanel] ★ myId=2 입력 전송: mask=0 W=false S=false A=false D=false
[GamePanel] ★ myId=2 스냅샷 받음: id=1 pos=(3117.4,610.3) id=2★MY★ pos=(3660.9,3612.3)
```

**분석**:
- 클라이언트 1: id=1을 조작, id=2는 서버 데이터로만 렌더링
- 클라이언트 2: id=2를 조작, id=1은 서버 데이터로만 렌더링
- 각 클라이언트는 자기 캐릭터만 카메라 추적

## 설정 및 상수

### 카메라 설정

```java
// GamePanel.java
private static final float CAMERA_ZOOM = 2.0f;  // 줌 레벨
```

### 입력 설정

```java
// GamePanel.java
private static final int INPUT_TIMER_DELAY = 33;  // ~30Hz
private static final int RENDER_FPS = 60;         // 60 FPS
```

### 서버 설정

```java
// PlayerSyncService.java
private static final double TICK_RATE = 20.0;     // 20Hz
private static final float MOVE_SPEED = 150.0f;   // units/sec
```

## 테스트 가이드

### 단위 테스트 실행

```bash
# 카메라 팔로우 테스트
mvn test -Dtest=GamePanelCameraFollowTest -pl comfps-client

# 모든 클라이언트 테스트
mvn test -pl comfps-client
```

### 통합 테스트 (수동)

1. **서버 시작**:
```bash
java -cp "comfps-server\target\comfps-server-1.0-SNAPSHOT.jar;comfps-common\target\comfps-common-1.0-SNAPSHOT.jar" com.fpsgame.server.ServerMain --port 7777
```

2. **클라이언트 1 시작**:
```bash
java -cp "comfps-client\target\comfps-client-1.0-SNAPSHOT.jar;comfps-common\target\comfps-common-1.0-SNAPSHOT.jar" com.fpsgame.MainLauncher
```

3. **클라이언트 2 시작**: (새 터미널에서 같은 명령어)

4. **테스트 체크리스트**:
   - [ ] 십자선이 화면 중앙에 고정되어 있는가?
   - [ ] WASD 키를 누르면 내 캐릭터만 움직이는가?
   - [ ] 카메라가 내 캐릭터를 따라가는가?
   - [ ] 다른 플레이어의 움직임이 보이는가?
   - [ ] 마우스로 조준 방향 변경이 가능한가?

## 향후 개선 사항

### 카메라 시스템
- [ ] 카메라 스무딩 (부드러운 이동)
- [ ] 동적 줌 (전투 시 줌 인/아웃)
- [ ] 카메라 셰이크 효과 (폭발, 피격 시)
- [ ] 데드존(Deadzone) 추가 (캐릭터가 화면 중앙에서 약간 벗어나도 카메라 고정)

### 입력 시스템
- [ ] 입력 예측 (Client-side Prediction)
- [ ] 입력 버퍼링 (네트워크 지연 보상)
- [ ] 키 바인딩 커스터마이징

### 네트워크
- [ ] 델타 압축 (변경된 데이터만 전송)
- [ ] 보간(Interpolation) 개선
- [ ] 지터 버퍼(Jitter Buffer)

## 관련 문서

- [프로젝트 상태](PROJECT_STATUS.md)
- [통합 계획](INTEGRATION_PLAN.md)
- [프로토콜 명세](PROTOCOL.md)
- [캐릭터 사양](CHARACTER_SPECS.md)

## 변경 이력

| 날짜 | 버전 | 변경 내용 | 작성자 |
|------|------|-----------|--------|
| 2025-11-05 | 1.0 | 카메라 시스템 문서 초안 작성 | - |
| 2025-11-05 | 1.1 | 십자선 위치 버그 수정 추가 | - |
| 2025-11-05 | 1.2 | 테스트 케이스 및 검증 결과 추가 | - |
