# GamePanel 카메라 시스템 개선 계획

## 현재 문제점

### 1. 맵 렌더링 문제
- 맵 이미지보다 실제 맵 영역이 더 넓어서 화면 밖으로 나감
- 맵 이미지가 전체 월드 크기에 맞춰 렌더링되지 않음

### 2. 동기화 문제
- 캐릭터, 조준선, 플레이어 위치가 동기화 안됨
- 서버에서 받은 스냅샷과 클라이언트 렌더링 위치가 불일치

### 3. 카메라 시스템
- 현재: 플레이어 고정, 주변 환경 이동 방식
- 목표: 맵 확대 + 캐릭터 중심 카메라 방식

## 해결 방안

### 1. 카메라 팔로우 모드 개선

**현재 코드 (GamePanel.java:310-326)**
```java
private void updateCamera() {
    if (cameraMode == CameraMode.MAP_OVERVIEW) {
        viewport.setCenter(worldW * 0.5f, worldH * 0.5f);
        maybeFitViewportToWorld();
        return;
    }

    SnapshotV2.Entry me = players.get(myId);
    if (me != null) {
        viewport.setScale(followModeScale);
        viewport.setCenter(me.x, me.y);
    }
}
```

**문제점:**
- 즉시 카메라가 이동해서 화면이 뚝뚝 끊김
- 부드러운 전환이 없음

**개선 코드:**
```java
private float smoothCameraX = 0f;
private float smoothCameraY = 0f;
private static final float CAMERA_LERP_FACTOR = 0.15f; // 부드러운 정도

private void updateCamera() {
    if (cameraMode == CameraMode.MAP_OVERVIEW) {
        viewport.setCenter(worldW * 0.5f, worldH * 0.5f);
        maybeFitViewportToWorld();
        return;
    }

    SnapshotV2.Entry me = players.get(myId);
    if (me != null) {
        viewport.setScale(followModeScale);
        
        // 부드러운 카메라 이동 (Lerp)
        smoothCameraX += (me.x - smoothCameraX) * CAMERA_LERP_FACTOR;
        smoothCameraY += (me.y - smoothCameraY) * CAMERA_LERP_FACTOR;
        
        viewport.setCenter(smoothCameraX, smoothCameraY);
    } else {
        // 초기화: 첫 번째 스냅샷이 들어올 때 카메라 위치 초기화
        if (myId >= 0 && smoothCameraX == 0f && smoothCameraY == 0f) {
            smoothCameraX = worldW * 0.5f;
            smoothCameraY = worldH * 0.5f;
        }
    }
}
```

### 2. 맵 렌더링 개선

**현재 코드 (GamePanel.java:467-481)**
```java
Rect viewBounds = viewport.getViewBounds(null);
Point topLeft = viewport.worldToScreen(viewBounds.x, viewBounds.y);
Point bottomRight = viewport.worldToScreen(viewBounds.x + viewBounds.w, viewBounds.y + viewBounds.h);
int viewW = bottomRight.x - topLeft.x;
int viewH = bottomRight.y - topLeft.y;

if (mapBackground != null) {
    g2.drawImage(mapBackground, topLeft.x, topLeft.y, viewW, viewH, null);
}
```

**문제점:**
- 맵 이미지가 현재 뷰포트 영역에만 맞춰짐
- 월드 전체 크기와 맵 이미지 크기가 연동되지 않음

**개선 코드:**
```java
// 맵 배경은 전체 월드 크기에 맞춰서 렌더링
if (mapBackground != null) {
    // 월드 (0, 0)과 (worldW, worldH)의 스크린 좌표 계산
    Point topLeft = viewport.worldToScreen(0, 0);
    Point bottomRight = viewport.worldToScreen(worldW, worldH);
    
    int mapW = bottomRight.x - topLeft.x;
    int mapH = bottomRight.y - topLeft.y;
    
    // 전체 맵을 월드 크기에 맞춰 렌더링
    g2.drawImage(mapBackground, topLeft.x, topLeft.y, mapW, mapH, null);
} else {
    // 폴백: 뷰포트 영역만 채우기
    Rect viewBounds = viewport.getViewBounds(null);
    Point vTopLeft = viewport.worldToScreen(viewBounds.x, viewBounds.y);
    Point vBottomRight = viewport.worldToScreen(viewBounds.x + viewBounds.w, viewBounds.y + viewBounds.h);
    int viewW = vBottomRight.x - vTopLeft.x;
    int viewH = vBottomRight.y - vTopLeft.y;
    
    g2.setColor(new Color(0x1e232b));
    g2.fillRect(vTopLeft.x, vTopLeft.y, viewW, viewH);
}
```

### 3. 입력 처리 개선

**현재 코드 (GamePanel.java:447-455)**
```java
private void flushInput() {
    InputSender sender = this.inputSender;
    if (sender == null) return;
    int mask = 0;
    if (keyW) mask |= 0x01;
    if (keyS) mask |= 0x02;
    if (keyA) mask |= 0x04;
    if (keyD) mask |= 0x08;
    sender.send((byte)(mask & 0xFF), aimRad);
}
```

**문제점:**
- 자신의 캐릭터가 스냅샷에 없어도 입력을 계속 전송
- 디버그 로그가 너무 많음

**개선 코드:**
```java
private void flushInput() {
    InputSender sender = this.inputSender;
    if (sender == null) return;
    
    // 자신의 캐릭터가 게임에 있을 때만 입력 전송
    SnapshotV2.Entry me = players.get(myId);
    if (me == null && myId >= 0) {
        // 자신의 캐릭터가 아직 스폰되지 않았거나 죽은 상태
        return;
    }
    
    int mask = 0;
    if (keyW) mask |= 0x01;
    if (keyS) mask |= 0x02;
    if (keyA) mask |= 0x04;
    if (keyD) mask |= 0x08;
    
    sender.send((byte)(mask & 0xFF), aimRad);
}
```

### 4. 조준선(Aim) 계산 개선

**현재 코드 (GamePanel.java:433-441)**
```java
private void updateAimFromMouse(Point p) {
    SnapshotV2.Entry me = players.get(myId);
    if (me == null || p == null) { aimRad = null; return; }
    
    com.fpsgame.common.Vec2 worldPos = viewport.screenToWorld(p.x, p.y, null);
    double dx = worldPos.x - me.x;
    double dy = worldPos.y - me.y;
    aimRad = (float) Math.atan2(dy, dx);
}
```

**개선 코드: (문제 없음, 유지)**
- 이 부분은 이미 올바르게 작동함
- 마우스 스크린 좌표 → 월드 좌표 변환 → 플레이어와의 각도 계산

## 적용 순서

1. **카메라 부드러운 이동** 추가
   - smoothCameraX, smoothCameraY 필드 추가
   - updateCamera() 메서드 수정

2. **맵 렌더링 개선**
   - paintComponent()의 맵 렌더링 부분 수정
   - 전체 월드 크기에 맞춰 맵 이미지 렌더링

3. **입력 처리 개선**
   - flushInput()에 유효성 체크 추가

4. **디버그 로그 정리**
   - 필요한 로그만 남기고 나머지 제거 또는 조건부로 변경

## 테스트 체크리스트

### 카메라 시스템
- [ ] 카메라가 플레이어를 부드럽게 따라다니는가?
- [ ] 맵 경계에서 카메라가 올바르게 제한되는가?
- [ ] 줌 레벨 변경이 정상 작동하는가?
- [ ] 플레이어가 화면 중앙에 유지되는가?

### 맵 렌더링
- [ ] 맵 이미지가 전체 월드에 맞춰 렌더링되는가?
- [ ] 맵 밖의 영역이 보이지 않는가?
- [ ] 확대/축소 시 맵이 정상적으로 보이는가?

### 캐릭터 동기화
- [ ] 자신의 캐릭터가 키보드로 조작되는가?
- [ ] 다른 캐릭터의 움직임이 보이는가?
- [ ] 조준선이 마우스 위치를 정확히 가리키는가?
- [ ] 여러 플레이어가 동시에 움직일 때 렌더링이 정상인가?

### 입력 처리
- [ ] 자신의 캐릭터만 조작되는가?
- [ ] 다른 캐릭터가 키입력에 반응하지 않는가?
- [ ] 죽었을 때 입력이 전송되지 않는가?

## 추가 개선 사항 (나중에)

1. **카메라 쉐이크 효과**: 총 발사, 피격 시
2. **카메라 앞서가기**: 플레이어가 보는 방향을 약간 더 보여주기
3. **데드존**: 플레이어가 화면 중앙에서 약간 벗어날 수 있도록
4. **줌 전환 애니메이션**: 부드러운 줌 인/아웃
5. **미니맵 개선**: 현재 카메라 시야 영역 표시

## 참고 사항

- 테스트 클래스 (`CameraFollowTest.java`)에서 검증된 방식 사용
- 기존 Viewport 클래스는 수정하지 않고 GamePanel만 수정
- 서버 코드는 수정하지 않음 (클라이언트만 개선)
