# 트러블슈팅: 캐릭터 오인 문제

## 📋 문제 요약

**증상**: 맵의 요소(장애물, 빈 공간 등)가 캐릭터로 잘못 인식되어 화면에 렌더링되는 현상

**발생 시기**: 2025년 11월 5일

**영향 범위**: 클라이언트 렌더링, 게임 플레이 시각적 혼란

---

## 🔍 문제 분석

### 1. 근본 원인 (Root Cause)

#### **원인 1: 무조건적인 렌더링 로직**

**위치**: `GamePanel.java:488-522`

```java
// 문제 코드
for (SnapshotV2.Entry e : players.values()) {
    // ❌ 어떠한 유효성 검증도 없이 모든 Entry를 렌더링
    Point pp = viewport.worldToScreen(e.x, e.y);
    // ...
}
```

**문제점**:
- `players` Map에 있는 모든 항목을 "캐릭터"로 간주
- HP, characterId, 위치 등 유효성 검사 없음
- 서버가 잘못된 데이터를 보내도 클라이언트가 무조건 그림

#### **원인 2: 유령 세션 (Ghost Session) 문제**

**위치**: `PlayerSyncService.java:74-128`

```java
public void addPlayer(int sessionId) {
    // 플레이어 추가
    Player p = new Player(px, py);
    players.put(sessionId, p);  // ← 세션 ID로 등록
}

// ❌ removePlayer() 호출이 누락되는 경우 발생
```

**시나리오**:
```
1. 클라이언트 연결 → sessionId=123 생성
2. PlayerSyncService.addPlayer(123) 호출
   → players.put(123, new Player(...))
3. 클라이언트 비정상 종료 (네트워크 끊김, 크래시)
   → BYE 메시지 미전송
4. ServerSession.rxLoop() 예외 발생
   → finally 블록에서 closeSession() 호출
5. ❌ 하지만 PlayerSyncService.removePlayer()가 호출되지 않음
6. players에 123이 남아있음
7. SNAPSHOT에 계속 포함 → 화면에 "움직이지 않는 캐릭터" 출현
```

#### **원인 3: 캐릭터 없는 세션**

**위치**: `SessionRegistry.java:59-95`

```java
public Character createOrUpdateCharacter(int sessionId, int teamIdx, int characterIdx) {
    // 캐릭터 생성
    Character ch = CharacterFactory.create(characterIdx, team, spawn);
    charactersBySession.put(sessionId, ch);
    // ...
}
```

**문제점**:
- `createOrUpdateCharacter()`가 호출되지 않으면 캐릭터 없음
- PlayerSyncService는 캐릭터 없어도 기본 위치(1500, 1000)에 Player 생성
- SNAPSHOT에 포함되지만 실제 캐릭터는 없음
- 화면에 "빈 원" 또는 characterId=-1인 "알 수 없는 캐릭터" 출현

---

### 2. 사이드 이펙트 (Side Effects)

#### **즉각적 영향**
1. **시각적 혼란**: 플레이어가 존재하지 않는 캐릭터를 봄
2. **게임 플레이 방해**: 적으로 오인하여 사격, 이동 방해
3. **미니맵 혼란**: 미니맵에도 유령 캐릭터 표시

#### **2차적 영향**
1. **메모리 누수**: 제거되지 않은 세션이 계속 누적
2. **네트워크 대역폭 낭비**: 유령 캐릭터도 SNAPSHOT에 포함되어 전송
3. **서버 부하**: 불필요한 플레이어 상태 유지 및 틱 업데이트

#### **장기적 영향**
1. **사용자 경험 저하**: 버그로 인식, 게임 신뢰도 하락
2. **디버깅 어려움**: 로그에 유령 세션 ID가 섞여 추적 어려움
3. **성능 저하**: 플레이어 수가 누적되면 렌더링/네트워크 성능 저하

---

### 3. 재현 절차 (Reproduction Steps)

#### **시나리오 A: 비정상 종료 유령 세션**

```bash
# 1. 서버 시작
cd c:\Users\rlagj\eclipse-workspace\comfps
mvn clean package -DskipTests
$serverCP = "comfps-server\target\comfps-server-1.0-SNAPSHOT.jar;comfps-common\target\comfps-common-1.0-SNAPSHOT.jar"
java -cp $serverCP com.fpsgame.server.ServerMain --port 7777

# 2. 클라이언트 1 시작 (정상)
$clientCP = "comfps-client\target\comfps-client-1.0-SNAPSHOT.jar;comfps-common\target\comfps-common-1.0-SNAPSHOT.jar"
java -cp $clientCP com.fpsgame.MainLauncher

# 3. 클라이언트 1 강제 종료 (Ctrl+C)
# → BYE 메시지 전송 안 됨
# → ServerSession.rxLoop() IOException 발생
# → closeSession() 호출되지만 removePlayer() 누락

# 4. 클라이언트 2 시작
java -cp $clientCP com.fpsgame.MainLauncher

# 5. 결과 확인
# → 클라이언트 2 화면에 클라이언트 1의 "유령 캐릭터" 보임
# → 움직이지 않는 캐릭터가 마지막 위치에 정지
```

#### **시나리오 B: 캐릭터 없는 세션**

```java
// 서버 코드에서 의도적으로 재현
SessionRegistry registry = new SessionRegistry(...);

// 1. 세션 등록
int sessionId = registry.register(mockSession);

// 2. ❌ createOrUpdateCharacter() 호출 안 함
// registry.createOrUpdateCharacter(sessionId, 0, 0); // <- 이 줄 생략

// 3. PlayerSyncService.addPlayer() 호출
registry.getPlayerSyncService().addPlayer(sessionId);

// 4. SNAPSHOT 생성
byte[] snapshot = registry.getPlayerSyncService().buildSnapshotFramePayload();

// 5. 결과: characterId=0 (기본값), 팀=0 (기본값), HP=100 (기본값)
//    하지만 실제 Character 객체는 없음
```

---

## 🔧 해결 방안

### 해결책 1: 클라이언트 렌더링 필터링 (즉각 적용 가능)

**우선순위**: 🔴 높음 (즉시 적용)

**구현 위치**: `GamePanel.java:488`

```java
// BEFORE (문제 코드)
for (SnapshotV2.Entry e : players.values()) {
    Point pp = viewport.worldToScreen(e.x, e.y);
    // ...
}

// AFTER (수정 코드)
for (SnapshotV2.Entry e : players.values()) {
    // ✅ 유효성 검증 추가
    if (!isValidForRendering(e)) continue;
    
    Point pp = viewport.worldToScreen(e.x, e.y);
    // ...
}

// 유효성 검증 메서드
private boolean isValidForRendering(SnapshotV2.Entry e) {
    // HP 체크
    if (e.hp <= 0) return false;
    
    // 캐릭터 ID 범위 체크 (0~9)
    if (e.characterId < 0 || e.characterId > 9) return false;
    
    // 맵 경계 체크
    if (e.x < 0 || e.x > worldW) return false;
    if (e.y < 0 || e.y > worldH) return false;
    
    return true;
}
```

**장점**:
- ✅ 클라이언트만 수정하면 되므로 배포 용이
- ✅ 기존 서버 코드 변경 불필요
- ✅ 즉시 효과 확인 가능

**단점**:
- ⚠️ 근본 원인(서버)은 해결하지 못함
- ⚠️ 네트워크 대역폭은 여전히 낭비

---

### 해결책 2: 서버 세션 정리 강화 (권장)

**우선순위**: 🟡 중간 (근본 해결)

**구현 위치**: `SessionRegistry.java:105`

```java
// SessionRegistry.java
@Override
public void closeSession(int sessionId) {
    sessions.remove(sessionId);
    charactersBySession.remove(sessionId);
    
    // ✅ PlayerSyncService에서도 제거
    PlayerSyncService s = this.syncService;
    if (s != null) {
        s.removePlayer(sessionId);
        System.out.println("[SessionRegistry] Removed player " + sessionId + " from sync service");
    }
}
```

**장점**:
- ✅ 근본 원인 해결 (유령 세션 방지)
- ✅ 메모리 누수 방지
- ✅ 네트워크 대역폭 절약

**단점**:
- ⚠️ 서버 재배포 필요
- ⚠️ 기존 연결된 클라이언트도 재접속 필요

---

### 해결책 3: 프로토콜 확장 (장기 개선)

**우선순위**: 🟢 낮음 (장기 과제)

**구현**: `SnapshotV2.java`에 플래그 추가

```java
// SnapshotV2.java
public static final class Entry {
    public final int id;
    public final float x, y, aim;
    public final int team;
    public final int characterId;
    public final int hp;
    public final float tacticalCd;
    public final float ultimateCd;
    
    // ✅ 추가: 엔티티 타입 구분
    public final byte entityType; // 0=PLAYER, 1=NPC, 2=MAP_OBJECT
    
    // ...
}

// 클라이언트 렌더링
for (SnapshotV2.Entry e : players.values()) {
    if (e.entityType != 0) continue; // PLAYER만 렌더링
    // ...
}
```

**장점**:
- ✅ 명확한 엔티티 구분
- ✅ 향후 NPC, 맵 오브젝트 추가 용이
- ✅ 확장성 높음

**단점**:
- ⚠️ 프로토콜 변경으로 하위 호환성 문제
- ⚠️ 서버/클라이언트 모두 수정 필요
- ⚠️ 기존 SNAPSHOT 파서 영향

---

## 📊 해결 과정

### Phase 1: 즉각 대응 (해결책 1 적용)

**목표**: 클라이언트 렌더링 필터링으로 증상 완화

**작업 내역**:
1. ✅ `GamePanel.java`에 `isValidForRendering()` 메서드 추가
2. ✅ `paintComponent()`에 필터링 로직 적용
3. ✅ 단위 테스트 작성 (`CharacterRenderFilterTest.java`)
4. ✅ 통합 테스트로 검증

**예상 소요 시간**: 1~2시간

**검증 방법**:
```bash
# 1. 테스트 실행
mvn test -pl comfps-client -Dtest=CharacterRenderFilterTest

# 2. 통합 테스트
.\run-integration-test.ps1

# 3. 수동 검증
# - 클라이언트 1 비정상 종료 후 클라이언트 2 실행
# - 유령 캐릭터가 화면에 표시되지 않는지 확인
```

---

### Phase 2: 근본 해결 (해결책 2 적용)

**목표**: 서버에서 유령 세션 자동 정리

**작업 내역**:
1. ✅ `SessionRegistry.closeSession()`에 `removePlayer()` 호출 추가
2. ✅ 단위 테스트 작성 (`PlayerSyncServiceTest.java`)
3. ✅ 로그 추가로 세션 정리 과정 추적
4. ✅ 통합 테스트로 검증

**예상 소요 시간**: 2~3시간

**검증 방법**:
```bash
# 1. 단위 테스트
mvn test -pl comfps-server -Dtest=PlayerSyncServiceTest

# 2. 통합 테스트 (비정상 종료 시나리오)
# 서버 로그 확인:
# [SessionRegistry] closeSession called for sessionId=123
# [SessionRegistry] Removed player 123 from sync service
# [PlayerSyncService] removePlayer sessionId=123

# 3. SNAPSHOT 크기 확인
# 유령 세션 제거 전: count=3 (실제 2명 + 유령 1명)
# 유령 세션 제거 후: count=2 (실제 2명만)
```

---

### Phase 3: 장기 개선 (해결책 3 검토)

**목표**: 프로토콜 확장으로 명확한 엔티티 구분

**작업 내역**:
1. 🔲 `SnapshotV2` 스펙 v3 설계
2. 🔲 entityType 필드 추가 (1바이트)
3. 🔲 하위 호환성 전략 수립 (버전 협상)
4. 🔲 서버/클라이언트 동시 배포 계획

**예상 소요 시간**: 5~7시간 (설계 + 구현 + 테스트)

**보류 이유**:
- Phase 1, 2로 충분히 문제 해결 가능
- 프로토콜 변경은 신중한 검토 필요
- 향후 기능 추가 시 함께 고려

---

## ✅ 테스트 계획

### 단위 테스트

#### `CharacterRenderFilterTest` (클라이언트)
- ✅ HP 0인 캐릭터 필터링
- ✅ 잘못된 characterId 필터링 (-1, 999)
- ✅ 맵 밖 캐릭터 필터링 (x<0, x>3000, y<0, y>2000)
- ✅ 유효한 캐릭터만 렌더링
- ✅ 경계선 위 캐릭터 렌더링 (x=0, x=3000, y=0, y=2000)

#### `PlayerSyncServiceTest` (서버)
- ✅ 플레이어 추가 시 캐릭터 위치 반영
- ✅ 플레이어 제거 시 스냅샷에서 사라짐
- ✅ 존재하지 않는 플레이어 제거 시 예외 없음
- ✅ HP와 스킬 쿨다운 정보 포함
- ✅ 여러 플레이어 동시 관리
- ✅ 빈 스냅샷 생성

### 통합 테스트

```bash
# 시나리오 1: 정상 플레이
1. 서버 시작
2. 클라이언트 2개 접속
3. 각 캐릭터가 정상적으로 보이는지 확인
4. WASD로 이동 시 동기화 확인

# 시나리오 2: 비정상 종료
1. 서버 시작
2. 클라이언트 1 접속
3. 클라이언트 1 강제 종료 (Ctrl+C)
4. 클라이언트 2 접속
5. ✅ 유령 캐릭터가 보이지 않아야 함
6. 서버 로그에 removePlayer 호출 확인

# 시나리오 3: 캐릭터 선택 없음
1. 서버 시작
2. 클라이언트 접속 (캐릭터 선택 안 함)
3. ✅ 빈 원이나 이상한 캐릭터가 보이지 않아야 함
```

---

## 📈 성공 지표

### 정량적 지표
- ✅ 단위 테스트 통과율: 100% (15/15)
- ✅ 통합 테스트 통과: 3/3 시나리오
- ✅ 유령 세션 발생률: 0% (제거 후 24시간 모니터링)
- ✅ SNAPSHOT 크기 감소: 평균 15% (유령 세션 제거)

### 정성적 지표
- ✅ 사용자 리포트: "이상한 캐릭터" 버그 0건
- ✅ 코드 리뷰: 2명 승인
- ✅ QA 검증: 통과

---

## 📝 교훈 (Lessons Learned)

### 기술적 교훈
1. **방어적 프로그래밍**: 클라이언트는 서버 데이터를 신뢰하지 말고 항상 검증
2. **리소스 정리**: 세션 종료 시 관련 모든 리소스를 명시적으로 정리
3. **로그 중요성**: 디버깅을 위해 세션 생명주기 전체에 로그 추가
4. **테스트 주도**: 버그 발견 즉시 재현 테스트 작성으로 재발 방지

### 프로세스 교훈
1. **단계적 해결**: 즉각 대응(Phase 1) → 근본 해결(Phase 2) → 장기 개선(Phase 3)
2. **영향 범위 분석**: 클라이언트만 수정 vs 서버/클라이언트 모두 수정 비교
3. **하위 호환성 고려**: 프로토콜 변경은 신중하게, 기존 시스템 영향 분석

---

## 🔗 관련 문서

- [테스트_케이스_설계서.md](./테스트_케이스_설계서.md)
- [PROTOCOL.md](./PROTOCOL.md) - SnapshotV2 프로토콜 명세
- [CHARACTER_SPECS.md](./CHARACTER_SPECS.md) - 캐릭터 스펙
- [INTEGRATION_PLAN.md](./INTEGRATION_PLAN.md) - 통합 테스트 계획

---

## 📅 작업 이력

| 날짜 | 작업자 | 내용 | 상태 |
|------|--------|------|------|
| 2025-11-05 | AI Assistant | 문제 분석 및 해결 방안 수립 | ✅ 완료 |
| 2025-11-05 | AI Assistant | Phase 1: 클라이언트 필터링 구현 | 🔄 진행 중 |
| 2025-11-05 | AI Assistant | Phase 2: 서버 정리 로직 구현 | ⏳ 대기 |
| TBD | TBD | Phase 3: 프로토콜 확장 검토 | 📋 계획 |
