# 카메라 팔로우 시스템 테스트 가이드

## 테스트 목적

이 테스트는 실제 게임 코드에 적용하기 전에 다음 사항들을 검증합니다:

1. **맵 확대 + 캐릭터 중심 카메라**: 맵 이미지가 확대되어 보이고 캐릭터를 중심으로 카메라가 따라다니는지 확인
2. **캐릭터 조작 분리**: 각 플레이어가 자신의 캐릭터만 조작할 수 있는지 확인
3. **다중 캐릭터 동기화**: 다른 캐릭터들의 움직임이 올바르게 표시되는지 확인

## 실행 방법

```powershell
cd c:\Users\rlagj\eclipse-workspace\comfps
java -cp "comfps-client/target/test-classes;comfps-client/target/classes;comfps-common/target/classes" com.fpsgame.client.test.CameraFollowTest
```

## 조작법

### 기본 조작
- **W, A, S, D**: 현재 선택된 플레이어 이동
- **1, 2, 3**: 플레이어 전환 (각 플레이어를 순차적으로 조작해볼 수 있음)
- **+/-**: 카메라 줌 인/아웃
- **Space**: 다른 플레이어들의 자동 이동 토글

### 화면 표시
- **노란색 테두리**: 현재 조작 중인 플레이어
- **파란색 원**: 블루팀 플레이어
- **빨간색 원**: 레드팀 플레이어
- **초록색 화살표**: 플레이어의 이동 방향과 속도
- **화면 중앙 십자선**: 카메라 중심

## 테스트 시나리오

### 시나리오 1: 카메라 팔로우 확인
1. 플레이어 1로 시작 (기본)
2. WASD로 이동
3. **확인 사항**:
   - 카메라가 플레이어를 부드럽게 따라다니는가?
   - 맵이 확대되어 보이는가?
   - 플레이어가 화면 중앙에 위치하는가?

### 시나리오 2: 플레이어 전환 확인
1. 숫자키 2를 눌러 플레이어 2로 전환
2. WASD로 이동
3. **확인 사항**:
   - 카메라가 새로운 플레이어를 따라가는가?
   - 이전 플레이어(1번)는 움직이지 않는가?
   - 노란색 테두리가 올바른 플레이어에 표시되는가?

### 시나리오 3: 다중 캐릭터 시뮬레이션
1. Space를 눌러 다른 플레이어들의 자동 이동 활성화
2. WASD로 현재 플레이어 이동
3. **확인 사항**:
   - 다른 플레이어들이 자동으로 움직이는가?
   - 현재 플레이어만 키보드로 조작되는가?
   - 모든 플레이어의 움직임이 동시에 렌더링되는가?

### 시나리오 4: 줌 레벨 확인
1. +를 여러 번 눌러 줌 인
2. -를 여러 번 눌러 줌 아웃
3. **확인 사항**:
   - 줌 레벨 변경 시 맵과 캐릭터 크기가 함께 변하는가?
   - 카메라 중심이 유지되는가?
   - 맵 경계 체크가 올바르게 작동하는가?

### 시나리오 5: 맵 경계 확인
1. 맵의 모서리로 이동 (예: 왼쪽 위)
2. 계속 이동 시도
3. **확인 사항**:
   - 캐릭터가 맵 밖으로 나가지 않는가?
   - 카메라가 맵 밖을 보여주지 않는가?
   - 코너에서도 카메라가 안정적인가?

## 검증 포인트

### ✅ 성공 기준
- [ ] 카메라가 선택된 플레이어를 부드럽게 따라다님
- [ ] 맵이 적절히 확대되어 표시됨 (기본 0.5x = 2배 확대)
- [ ] 각 플레이어가 독립적으로 조작됨
- [ ] 플레이어 전환 시 카메라가 즉시 새 플레이어를 추적
- [ ] 여러 캐릭터가 동시에 움직일 때 모두 올바르게 렌더링됨
- [ ] 맵 경계에서 카메라와 캐릭터가 올바르게 제한됨
- [ ] 줌 인/아웃이 부드럽게 작동함

### ❌ 실패 사례
- 카메라가 플레이어를 놓침
- 다른 플레이어가 키보드 입력에 반응
- 맵이 확대되지 않고 전체가 보임
- 캐릭터나 조준선 위치가 어긋남
- 맵 밖의 영역이 보임

## 주요 구현 사항

### 1. 월드-스크린 좌표 변환
```java
// 카메라 중심을 화면 중앙에 배치
float offsetX = screenW / 2f - cameraX * cameraScale;
float offsetY = screenH / 2f - cameraY * cameraScale;

// 월드 좌표 -> 스크린 좌표
int screenX = (int) (worldX * cameraScale + offsetX);
int screenY = (int) (worldY * cameraScale + offsetY);
```

### 2. 카메라 팔로우 (부드러운 이동)
```java
float lerpFactor = 0.1f; // 부드러운 정도 (낮을수록 부드러움)
cameraX += (playerX - cameraX) * lerpFactor;
cameraY += (playerY - cameraY) * lerpFactor;
```

### 3. 카메라 경계 제한
```java
float halfViewW = getWidth() / (2f * cameraScale);
float halfViewH = getHeight() / (2f * cameraScale);

cameraX = Math.max(halfViewW, Math.min(cameraX, WORLD_WIDTH - halfViewW));
cameraY = Math.max(halfViewH, Math.min(cameraY, WORLD_HEIGHT - halfViewH));
```

### 4. 플레이어 조작 분리
```java
// 현재 선택된 플레이어만 키보드로 조작
if (player.controlled) {
    if (keyW) player.vy -= speed;
    if (keyS) player.vy += speed;
    // ...
}

// 다른 플레이어는 자동 이동 또는 서버 동기화 위치만 사용
```

## 다음 단계

이 테스트가 성공적으로 작동하면:

1. **GamePanel.java 수정**: 테스트에서 검증된 카메라 시스템을 적용
2. **Viewport.java 개선**: 카메라 팔로우 모드 추가
3. **입력 처리 개선**: 자신의 캐릭터만 조작하도록 필터링 강화
4. **서버 동기화**: 다른 플레이어의 위치를 서버에서 받아 표시

## 문제 발생 시

테스트가 제대로 작동하지 않는 경우:

1. **컴파일 오류**: 
   ```powershell
   mvn -pl comfps-client test-compile
   ```

2. **창이 안 뜨는 경우**: 
   - Java AWT/Swing이 정상적으로 설치되어 있는지 확인
   - JDK 버전 확인 (Java 17 사용 중)

3. **성능 문제**: 
   - cameraScale을 조정 (더 작은 값 = 더 확대 = 더 무거움)
   - 맵 이미지 크기 조정

## 설정 조정

테스트 클래스에서 다음 상수들을 조정할 수 있습니다:

```java
// 월드 크기
private static final float WORLD_WIDTH = 3000f;
private static final float WORLD_HEIGHT = 2000f;

// 초기 카메라 줌 (0.5 = 2배 확대)
float cameraScale = 0.5f;

// 카메라 부드러움 (0.1 = 매우 부드러움, 1.0 = 즉시 따라감)
float lerpFactor = 0.1f;

// 플레이어 이동 속도
float speed = 300f; // 픽셀/초
```
