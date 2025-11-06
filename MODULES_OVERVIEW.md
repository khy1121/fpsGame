# fpsGame 다중 모듈(Triple-Module) 구조 개요

이 문서는 프로젝트의 3개 모듈(comfps-common, comfps-server, comfps-client) 아키텍처, 핵심 파일, 그리고 중요한 코드 경로를 한눈에 볼 수 있도록 정리한 가이드입니다. 새로 합류한 팀원도 이 문서만 보면 전체 흐름을 파악하고, 빌드/실행/확장에 바로 착수할 수 있도록 구성했습니다.

---

## 모듈 구성 요약

- comfps-common
  - 공용 모델, 직렬화 포맷, 프로토콜(바이너리 프레임), 수학 유틸(Rect/Vec2), 캐릭터 베이스 클래스와 타입들
  - 클라이언트/서버가 동일한 구조체와 파서를 공유하여, 네트워크 메시지의 버그/파편화를 줄입니다.

- comfps-server
  - 게임 서버 실행 진입점과 라우터, 세션/로비/게임 상태 관리, 스냅샷 생성과 브로드캐스트
  - 로비 READY/팀 선택/맵 투표 → 매치 진행(페이즈/카운트다운/라운드 결과) → 실시간 스냅샷/프로젝타일 전송

- comfps-client
  - 런처 UI, 로비 화면, 게임 화면 렌더링(Swing), 입력 처리, 좌표 변환(Viewport)
  - 서버로 입력 전송, 서버에서 받는 스냅샷(SnapshotV2)과 투사체 목록을 화면에 그립니다.

---

## 빌드/실행

PowerShell 기준(Windows):

```powershell
# 전체 빌드
mvn -q -T 1C -DskipTests clean package

# 서버 실행(메인 클래스)
mvn -q -pl comfps-server exec:java -Dexec.mainClass=com.fpsgame.server.ServerMain

# 클라이언트 실행(런처)
mvn -q -pl comfps-client exec:java -Dexec.mainClass=com.fpsgame.MainLauncher
```

프로젝트 루트에 start-server.ps1 / start-client.ps1 스크립트도 있으므로, PowerShell에서 직접 실행할 수 있습니다.

---

## 핵심 흐름(End-to-End)

1) 클라이언트 시작 → 서버 접속 → Welcome 프레임 수신
- comfps-client MainLauncher → LobbyFrame → 서버 연결
- Protocol.WELCOME 파싱: myId, team, character, worldW, worldH, mapId
- Viewport.setWorldSize(worldW, worldH)로 카메라/스케일 초기화

2) 입력 처리 → 서버 전송
- GamePanel가 키보드/마우스 입력을 폴링하여 mask(WSAD)와 aim 각도를 전송
- 서버는 SessionRegistry에 입력을 반영하고, 틱마다 스냅샷을 작성

3) 스냅샷/프로젝타일 수신 → 렌더링
- SnapshotV2: 플레이어들 (id, x, y, aim, team, characterId, hp, …)
- Projectiles: (x, y, type …)
- Viewport.worldToScreen()으로 월드→스크린 변환 후 GamePanel이 그립니다.

---

## 모듈별 핵심 파일과 역할

### comfps-common (공용)

- `src/main/java/com/fpsgame/common/Protocol.java`
  - 길이-선행 바이너리 프레임: `[int length][byte opcode][payload…]`
  - Opcode: WELCOME/CHAT/PING/PONG/BYE/READY/SELECTION/MAP_VOTE/PHASE/COUNTDOWN/ROUND_RESULT/INPUT/SNAPSHOT/ACTION(+ PROJECTILES)
  - 주요 API
    - writeFrame/readFrame(DataOutput/DataInput)
    - sendChat/sendPing/sendWelcome … parseXXX(…)

- `src/main/java/com/fpsgame/common/SnapshotV2.java`
  - 스냅샷 엔트리 정의 및 파서
  - Entry: `{ id:int, x:float, y:float, aim:float, team:byte, characterId:byte, hp:int, tacticalCd:float, ultimateCd:float }`
  - parse(byte[])로 바이트 배열을 List<Entry>로 역직렬화

- `src/main/java/com/fpsgame/common/Rect.java`, `Vec2.java`
  - 월드/스크린 변환 및 충돌/카메라에 쓰이는 경량 구조체

- `src/main/java/com/fpsgame/character/*`
  - `Character` 베이스, 타입 별 파라미터(체력/이동/스킬 쿨다운 기본치 등)

### comfps-server (서버)

- `src/main/java/com/fpsgame/server/ServerMain.java`
  - 서버 진입점, 로비 훅(DefaultServerRouter.Hooks) 구현
  - READY 집계, 팀 균형, 맵 투표 타임아웃/결정, 페이즈 전환 브로드캐스트
  - GameServer에 진행 조건 설치(everyoneReady, voteComplete 등)

- (대표) `GameServer`, `DefaultServerRouter`, `SessionRegistry`
  - `SessionRegistry`: 세션→닉네임/팀/캐릭터/월드 객체 매핑, 브로드캐스트 헬퍼
  - `DefaultServerRouter`: 프레임 디스패치, Hooks 호출, Broadcaster 제공
  - `GameServer`: 라운드/월드 진행, 스냅샷/프로젝타일 생성, 조건에 따른 페이즈 전이

### comfps-client (클라이언트)

- `src/main/java/com/fpsgame/MainLauncher.java`
  - 런처 프레임. 닉네임 입력/설정/실행 버튼
  - LobbyFrame를 띄워 서버 접속/로비 진입

- `src/main/java/com/fpsgame/client/ui/LobbyFrame.java`
  - READY/팀/캐릭터 선택, 채팅, 맵 투표 UI. 서버와 Protocol로 통신

- `src/main/java/com/fpsgame/client/ui/GamePanel.java`
  - 렌더링의 핵심. 스냅샷/프로젝타일 수신 → Viewport로 좌표 변환 → 그리기
  - 플레이어 이미지 회전 렌더링(aim 각도), HP 바, 팀 인디케이터, ID 텍스트
  - 크로스헤어, 미니맵, HUD 텍스트

- `src/main/java/com/fpsgame/model/Viewport.java`
  - 카메라/줌/좌표 변환(월드↔스크린)
  - API: `resize(w,h)`, `setWorldSize(w,h)`, `setCenter(x,y)`, `worldToScreen(wx,wy)`, `screenToWorld(sx,sy)`

---

## 핵심 코드 하이라이트(요약 계약/포인트)

### 1) Protocol 프레임
- 입력/출력 계약
  - 입력: DataInput에서 `readInt → readByte → payload`
  - 출력: DataOutput에 `writeInt(1+len) → writeByte(opcode) → payload`
- 에러 모드
  - length<1, payload 불일치 시 IOException
- 성공 기준
  - Frame(opcode, payload)가 정상 복원, 상위 레벨 파서에서 유효 범위 체크

### 2) SnapshotV2 파싱
- 입력: `byte[] payload`
- 처리: 고정 순서로 `id,int → x,y,aim,float → team,chr,byte → hp,int → tactical/ultimateCd,float`
- 출력: `List<SnapshotV2.Entry>`
- 주의: count 상한(예: 100_000) 체크로 악의적 페이로드 방지

### 3) Viewport 좌표 변환
- `worldToScreen(wx, wy)`
  - `(wx - camX) * scale + screenW/2 → sx`
  - `(wy - camY) * scale + screenH/2 → sy`
- `screenToWorld(sx, sy)`
  - `(sx - screenW/2)/scale + camX → wx`
  - `(sy - screenH/2)/scale + camY → wy`
- 카메라 클램프: 화면이 월드보다 클 경우 중앙 고정

### 4) GamePanel 플레이어 렌더링(핵심 포인트)
- 캐릭터 스프라이트(64x64)를 플레이어 스크린 중심에 두고 `AffineTransform.rotate(aim)` 적용
- 이미지가 없으면 팀 색상의 원으로 폴백
- HP 바는 플레이어 위에 배치(기본 max=100), 팀 인디케이터와 ID 텍스트 표시

---

## 확장 포인트(빠른 가이드)

- 새로운 네트워크 메시지 추가
  - `Protocol.Opcode`에 상수 추가 → `sendXXX/parseXXX` 빌더/파서 작성 → 서버 라우터/클라 핸들러 연결

- 스냅샷 필드 확장
  - `SnapshotV2.Entry`에 필드 추가(버전 변경 고려) → 서버 생성 로직/클라 파서 동시 수정

- 렌더링/카메라 개선
  - `Viewport.setZoomLimits()`와 `fitToWorld()`로 줌 UX 개선
  - GamePanel에 레이어(배경/오브젝트/이펙트/HUD) 구분

---

## 문제 해결(FAQ)

- 조준선/플레이어 위치가 달라 보인다
  - 월드↔스크린 변환 일관성 확인(항상 Viewport 사용)
  - 조준선은 화면 중심이 아닌, 플레이어 스크린 좌표를 기준으로 그리는지 점검

- 한글/인코딩이 깨진다
  - 소스 파일 UTF-8로 저장, 폰트 지정 필요(런처는 한글 폰트 사용)

- 빌드 오류가 난다
  - `mvn -q -DskipTests clean package`로 의존 모듈 포함 일괄 빌드
  - 멀티 모듈에서 `-pl comfps-client -am`로 하위 의존까지 함께 빌드

---

## 다음 단계
- 캐릭터별 `maxHealth`를 공용 레지스트리에서 조회하도록 리팩터링(현재 기본값 100)
- 스냅샷 v3 설계: 탄환/이펙트/버프 디테일 증설, 압축 옵션
- 단위 테스트 보강: `ViewportTransformTest` 외 렌더링 좌표 검증 추가
