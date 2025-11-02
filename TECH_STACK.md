# FPS Game 프로젝트 기술 스택 및 코드 상세 분석

## 📋 목차
1. [프로젝트 개요](#프로젝트-개요)
2. [기술 스택](#기술-스택)
3. [프로젝트 구조](#프로젝트-구조)
4. [공통 모듈 (comfps-common)](#공통-모듈-comfps-common)
5. [서버 모듈 (comfps-server)](#서버-모듈-comfps-server)
6. [클라이언트 모듈 (comfps-client)](#클라이언트-모듈-comfps-client)
7. [네트워크 프로토콜](#네트워크-프로토콜)
8. [게임 플로우](#게임-플로우)
9. [캐릭터 시스템](#캐릭터-시스템)
10. [빌드 및 실행](#빌드-및-실행)

---

## 프로젝트 개요

### 프로젝트명
**FPS Game (Java Socket Demo)** - 멀티플레이어 FPS 네트워크 게임

### 개발 목적
- Java 소켓 프로그래밍 학습 및 실전 적용
- 클라이언트-서버 아키텍처 구현
- 실시간 네트워크 게임 프로토콜 설계
- 멀티플레이어 게임 동기화 메커니즘 구현

### 핵심 특징
- **순수 Java TCP 소켓 통신**: 별도의 게임 엔진 없이 Java 표준 라이브러리만 사용
- **바이너리 프로토콜**: 효율적인 데이터 전송을 위한 커스텀 바이너리 프로토콜
- **멀티 모듈 구조**: Maven 기반 모듈화 설계 (common, client, server)
- **실시간 동기화**: 스냅샷 기반 플레이어 상태 동기화 및 보간
- **게임 FSM**: 명확한 게임 상태 관리 (로비 → 투표 → 카운트다운 → 라운드 → 결과)

---

## 기술 스택

### 🔧 핵심 기술

#### 1. 프로그래밍 언어
- **Java 17** (LTS)
  - 최신 Java 기능 활용 (switch expression, record 등)
  - JDK 17의 강력한 타입 시스템과 성능 개선 활용
  
#### 2. 빌드 도구
- **Apache Maven 3.x**
  - 멀티 모듈 프로젝트 관리
  - 의존성 관리 (Dependency Management)
  - 라이프사이클 관리 (compile, test, package)
  
  **주요 플러그인:**
  ```xml
  - maven-compiler-plugin (3.11.0): Java 17 컴파일
  - maven-surefire-plugin (3.2.5): 단위 테스트 실행
  - maven-jar-plugin (3.3.0): 실행 가능한 JAR 생성
  - exec-maven-plugin (3.1.0): Java 애플리케이션 실행
  ```

#### 3. 네트워킹
- **Java NIO 기반 TCP Socket**
  - `java.net.Socket`: 클라이언트 소켓 통신
  - `java.net.ServerSocket`: 서버 소켓 리스닝
  - `DataInputStream/DataOutputStream`: 바이너리 데이터 직렬화/역직렬화
  
  **특징:**
  - 블로킹 I/O 모델 (별도 스레드 관리)
  - 커스텀 프레임 기반 프로토콜 ([길이|opcode|payload])
  - 바이트 레벨 제어로 효율적인 대역폭 사용

#### 4. UI 프레임워크
- **Java Swing**
  - 크로스 플랫폼 GUI 구현
  - EDT (Event Dispatch Thread) 기반 스레드 안전성
  - 커스텀 패널 및 컴포넌트 구현
  
  **주요 컴포넌트:**
  ```java
  - JFrame: 메인 윈도우
  - JPanel: 게임 화면, 로비, HUD
  - JTextArea: 채팅 인터페이스
  - Custom Canvas: 게임 렌더링
  ```

#### 5. 테스트 프레임워크
- **JUnit 5 (Jupiter) 5.11.0**
  - 현대적인 테스트 프레임워크
  - Assertions, Assumptions, Lifecycle 관리
  - 파라미터화 테스트 지원

### 📦 의존성 구조

```
comfps-root (parent POM)
├── comfps-common (공통 라이브러리)
│   └── JUnit 5 (test scope)
├── comfps-server (서버)
│   ├── comfps-common (compile)
│   └── JUnit 5 (test scope)
└── comfps-client (클라이언트)
    ├── comfps-common (compile)
    └── JUnit 5 (test scope)
```

**의존성 흐름:**
- Server → Common (프로토콜, 유틸리티 사용)
- Client → Common (프로토콜, 유틸리티 사용)
- Server ↔ Client (독립적, 직접 의존 없음)

### 🛠 개발 도구

- **IDE**: Eclipse / IntelliJ IDEA (프로젝트 파일 포함)
- **버전 관리**: Git
- **문서화**: Markdown (README, PROTOCOL, DESIGN 등)

---

## 프로젝트 구조

### 디렉터리 구조

```
fpsGame/
├── pom.xml                      # 루트 POM (멀티 모듈 정의)
├── .gitignore                   # Git 무시 파일
├── README.md                    # 프로젝트 소개
├── PROTOCOL.md                  # 네트워크 프로토콜 명세
├── DESIGN.md                    # 설계 문서
├── CHARACTER_SPECS.md           # 캐릭터/맵 명세
├── TECH_STACK.md               # 기술 스택 상세 (본 문서)
│
├── comfps-common/              # 공통 모듈
│   ├── pom.xml
│   └── src/
│       └── main/java/com/fpsgame/
│           ├── Protocol.java              # 네트워크 프로토콜
│           ├── GameEnums.java             # 공용 열거형
│           ├── SnapshotV1.java, V2.java   # 스냅샷 직렬화
│           ├── ProjectilesV2.java         # 투사체 데이터
│           ├── Vec2.java, Rect.java       # 수학 유틸
│           ├── World.java, Wall.java      # 맵 데이터
│           └── character/                 # 캐릭터 시스템
│               ├── CharacterBase.java
│               ├── CharacterStats.java
│               └── types/                 # 10개 캐릭터 구현
│
├── comfps-server/              # 서버 모듈
│   ├── pom.xml
│   └── src/
│       └── main/java/com/fpsgame/server/
│           ├── ServerMain.java           # 서버 진입점
│           ├── GameServer.java           # 게임 FSM
│           ├── TcpServer.java            # TCP 서버
│           ├── SessionRegistry.java      # 세션 관리
│           ├── ServerRouter.java         # 라우팅 인터페이스
│           ├── DefaultServerRouter.java  # 기본 라우터
│           ├── LobbyState.java           # 로비 상태
│           ├── MapVoteManager.java       # 맵 투표 관리
│           └── RoundController.java      # 라운드 제어
│
└── comfps-client/              # 클라이언트 모듈
    ├── pom.xml
    └── src/
        └── main/java/com/fpsgame/
            ├── MainLauncher.java         # 클라이언트 진입점
            ├── NetClient.java            # 네트워크 클라이언트
            ├── model/                    # 클라이언트 모델
            │   ├── ClientPhaseModel.java
            │   ├── PlayerSnapshotBuffer.java
            │   ├── Settings.java
            │   └── Keybinds.java
            ├── ui/                       # UI 컴포넌트
            │   ├── GameFrame.java
            │   ├── LobbyPanel.java
            │   ├── HudPanel.java
            │   ├── ChatPanel.java
            │   └── CharacterSelectDialog.java
            └── net/                      # 네트워크 레이어
                ├── NetClientPhaseBridge.java
                └── PhaseFrameAdapter.java
```

### 모듈별 역할

#### comfps-common (공통 라이브러리)
- **역할**: 클라이언트와 서버가 공통으로 사용하는 코드
- **내용**:
  - 네트워크 프로토콜 정의 및 직렬화/역직렬화
  - 게임 규칙 및 열거형 (Phase, Team, Character, Map)
  - 수학 유틸리티 (벡터, 사각형)
  - 게임 월드 데이터 구조
  - 캐릭터 베이스 클래스 및 스탯

#### comfps-server (서버)
- **역할**: 게임 로직 실행 및 네트워크 동기화
- **내용**:
  - TCP 서버 구현
  - 클라이언트 세션 관리
  - 게임 FSM (Finite State Machine)
  - 로비/투표/라운드 로직
  - 브로드캐스트 라우팅

#### comfps-client (클라이언트)
- **역할**: 게임 표시 및 사용자 입력 처리
- **내용**:
  - TCP 클라이언트 구현
  - Swing 기반 UI
  - 스냅샷 보간 (Interpolation)
  - 로컬 입력 처리 및 전송
  - HUD, 로비, 채팅 UI

---

## 공통 모듈 (comfps-common)

### 📌 핵심 클래스 분석

#### 1. Protocol.java - 네트워크 프로토콜의 핵심

**목적**: 바이너리 프레임 기반 통신 프로토콜 정의

**프레임 구조**:
```
[4 bytes: length] [1 byte: opcode] [N bytes: payload]
```

**주요 Opcode 정의**:
```java
public static final class Opcode {
    // 핵심 프로토콜
    public static final byte WELCOME       = 0x01;  // 서버 환영
    public static final byte CHAT          = 0x02;  // 채팅
    public static final byte PING          = 0x03;  // 핑
    public static final byte PONG          = 0x04;  // 퐁
    public static final byte BYE           = 0x05;  // 연결 종료
    
    // 로비 관련
    public static final byte READY_TOGGLE  = 0x10;  // 준비 토글
    public static final byte SET_SELECTION = 0x11;  // 팀/캐릭터 선택
    public static final byte MAP_VOTE      = 0x12;  // 맵 투표
    
    // 매치 진행
    public static final byte PHASE_UPDATE  = 0x20;  // 단계 업데이트
    public static final byte COUNTDOWN     = 0x21;  // 카운트다운
    public static final byte ROUND_RESULT  = 0x22;  // 라운드 결과
    public static final byte READY_STATUS  = 0x23;  // 준비 상태
    
    // 게임플레이
    public static final byte INPUT         = 0x30;  // 입력
    public static final byte SNAPSHOT      = 0x31;  // 스냅샷
    public static final byte PROJECTILES   = 0x32;  // 투사체
    public static final byte ACTION        = 0x33;  // 액션
}
```

**핵심 메서드**:

```java
// 프레임 쓰기
public static void writeFrame(DataOutput out, byte opcode, byte[] payload)

// 프레임 읽기
public static Frame readFrame(DataInput in)

// 채팅 전송
public static void sendChat(DataOutput out, String text)

// 환영 메시지 파싱
public static Welcome parseWelcome(byte[] payload)

// 핑/퐁 전송
public static void sendPing(DataOutput out, long nonce)
public static long parseNonce(byte[] payload)
```

**직렬화 유틸리티**:
```java
// 정수 직렬화 (Big Endian)
putInt(ByteArrayOutputStream baos, int v)
getInt(byte[] buf, int off)

// 문자열 직렬화 (UTF-8, 길이 선행)
putUtf8(ByteArrayOutputStream baos, String s)
getUtf8(byte[] buf, int off)
```

**설계 특징**:
- **바이트 레벨 제어**: 네트워크 대역폭 최소화
- **타입 안전성**: 각 Opcode별 전용 파서/직렬화 함수
- **확장성**: 새 Opcode 추가 시 기존 코드 영향 최소화

---

#### 2. GameEnums.java - 게임 규칙 및 열거형

**Phase (게임 단계)**:
```java
public enum Phase {
    LOBBY,          // 대기실 (준비 수집)
    VOTE,           // 맵 투표
    COUNTDOWN,      // 라운드 시작 카운트다운
    ROUND_RUNNING,  // 라운드 진행
    ROUND_RESULT,   // 라운드 결과
    MATCH_END       // 매치 종료
}
```

**Team (팀)**:
```java
public enum Team {
    RED("Red"),
    BLUE("Blue");
    
    public String displayName();
    public static Team fromName(String s);
}
```

**CharacterId (캐릭터)**:
```java
public enum CharacterId {
    RAVEN,      // 돌격병
    PIPER,      // 저격수
    BULLDOG,    // 중화기병
    SAGE,       // 지원가
    GHOST,      // 첩보원
    GENERAL,    // 지휘관
    TECHNICIAN, // 공병
    WILDCAT,    // 샷건 전문가
    SKULL,      // 용병
    STEAM,      // 특수부대
    SNIPER,     // 저격수 (레거시)
    TANK;       // 탱커 (레거시)
    
    public String displayName();
    public static CharacterId fromName(String s);
}
```

**MapId (맵)**:
```java
public enum MapId {
    TERMINAL("Terminal"),           // 폐쇄된 창고
    NEON_CITY("Neon City"),         // 네온 도시 옥상
    FOREST_OUTPOST("Forest Outpost"); // 고대 사원
    
    public String displayName();
    public static MapId fromName(String s);
    public static MapId defaultMap();
}
```

**Rules (게임 규칙)**:
```java
public static final class Rules {
    public static final int MAX_ROUNDS = 5;              // 최대 라운드
    public static final int WINS_TO_TAKE_MATCH = 3;     // 승리 조건 (3선승)
    public static final int ROUND_COUNTDOWN_SEC = 5;    // 카운트다운 시간
}
```

---

#### 3. SnapshotV2.java - 플레이어 상태 스냅샷

**목적**: 효율적인 플레이어 상태 동기화

**Entry 구조**:
```java
public static final class Entry {
    public int playerId;     // 플레이어 ID
    public float x, y;       // 위치
    public float aim;        // 조준 각도
    public int team;         // 팀 (0=RED, 1=BLUE)
    public int character;    // 캐릭터 ID
}
```

**직렬화 형식**:
```
[4 bytes: count]
for each player:
    [4 bytes: id]
    [4 bytes: float x]
    [4 bytes: float y]
    [4 bytes: float aim]
    [1 byte: team]
    [1 byte: character]
```

**핵심 메서드**:
```java
// 스냅샷 직렬화
public static byte[] serializeToBytes(List<Entry> entries)

// 스냅샷 역직렬화
public static Map<Integer, Entry> parseToMap(byte[] payload)
```

**사용 사례**:
- 서버: 매 틱마다 모든 플레이어 상태를 스냅샷으로 브로드캐스트
- 클라이언트: 수신한 스냅샷을 버퍼에 저장 후 보간하여 부드러운 움직임 구현

---

#### 4. 캐릭터 시스템

**CharacterBase.java - 캐릭터 베이스 클래스**:
```java
public abstract class CharacterBase {
    protected CharacterStats stats;     // 스탯
    protected float cooldownQ;          // Q 스킬 쿨다운
    protected float cooldownE;          // E 스킬 쿨다운
    protected float cooldownBasic;      // 기본 공격 쿨다운
    
    // 스킬 실행
    public abstract void useBasicAttack(GameContext ctx);
    public abstract void useSkillE(GameContext ctx);
    public abstract void useUltimateQ(GameContext ctx);
}
```

**CharacterStats.java - 캐릭터 스탯**:
```java
public final class CharacterStats {
    public final int maxHealth;      // 최대 체력
    public final float moveSpeed;    // 이동 속도
    public final float basicDamage;  // 기본 공격 데미지
    public final float basicCooldown; // 기본 공격 쿨타임
    public final float skillECooldown; // E 스킬 쿨타임
    public final float ultimateQCooldown; // Q 궁극기 쿨타임
}
```

**캐릭터 구현 예시 - Raven (돌격병)**:
```java
public class Raven extends CharacterBase {
    public Raven() {
        super(new CharacterStats(
            150,    // maxHealth
            5.0f,   // moveSpeed
            15.0f,  // basicDamage
            0.15f,  // basicCooldown
            8.0f,   // skillECooldown (대시)
            45.0f   // ultimateQCooldown (광폭화)
        ));
    }
    
    @Override
    public void useBasicAttack(GameContext ctx) {
        // AR 점사 구현
    }
    
    @Override
    public void useSkillE(GameContext ctx) {
        // 전술 돌진 (50px 대시)
    }
    
    @Override
    public void useUltimateQ(GameContext ctx) {
        // 전탄 발사 (사격 속도 2배)
    }
}
```

---

#### 5. 수학 및 유틸리티

**Vec2.java - 2D 벡터**:
```java
public final class Vec2 {
    public final float x, y;
    
    public Vec2 add(Vec2 other);
    public Vec2 sub(Vec2 other);
    public Vec2 mul(float scalar);
    public float dot(Vec2 other);
    public float length();
    public Vec2 normalize();
}
```

**Rect.java - 2D 사각형 (AABB)**:
```java
public final class Rect {
    public final float x, y, w, h;
    
    public boolean contains(float px, float py);
    public boolean intersects(Rect other);
}
```

**Mathf.java - 수학 유틸리티**:
```java
public final class Mathf {
    public static float lerp(float a, float b, float t);
    public static float clamp(float v, float min, float max);
    public static float angleDiff(float a, float b);
}
```

---

## 서버 모듈 (comfps-server)

### 📌 핵심 클래스 분석

#### 1. ServerMain.java - 서버 진입점

**주요 흐름**:
```java
public static void main(String[] args) {
    // 1. TCP 서버 생성 (포트 7777)
    TcpServer tcpServer = new TcpServer(7777);
    
    // 2. 세션 레지스트리 생성
    SessionRegistry registry = new SessionRegistry();
    
    // 3. 게임 서버 생성
    GameServer gameServer = new GameServer();
    
    // 4. 로비 훅 설정 (준비, 투표, 선택 처리)
    LobbyHooks hooks = new LobbyHooks(registry, gameServer);
    
    // 5. 라우터 생성 (메시지 라우팅)
    DefaultServerRouter router = new DefaultServerRouter(
        registry, tcpServer, hooks
    );
    
    // 6. TCP 서버 시작 (클라이언트 연결 수락)
    tcpServer.start(session -> {
        registry.register(session);
        router.handleSession(session);
    });
    
    // 7. 게임 서버 틱 루프 시작 (20Hz)
    NetTickLoop tickLoop = new NetTickLoop(50); // 50ms = 20Hz
    tickLoop.start(() -> gameServer.tick());
    
    // 8. 서버 종료 대기 (Ctrl+C 또는 "quit" 입력)
    waitForShutdown();
}
```

**LobbyHooks - 로비 이벤트 처리**:
```java
static class LobbyHooks implements DefaultServerRouter.Hooks {
    private LobbyState lobbyState;         // 준비 상태
    private MapVoteManager voteManager;    // 맵 투표
    
    @Override
    public void onReadyToggle(int sessionId, boolean ready) {
        lobbyState.setReady(sessionId, ready);
        broadcastReadyStatus();
    }
    
    @Override
    public void onSetSelection(int sessionId, int team, int character) {
        registry.setCharacterTeam(sessionId, team);
        registry.setCharacterId(sessionId, character);
    }
    
    @Override
    public void onMapVote(int sessionId, int mapId) {
        voteManager.vote(sessionId, mapId);
        checkVoteComplete();
    }
}
```

---

#### 2. GameServer.java - 게임 FSM (유한 상태 기계)

**상태 전이도**:
```
LOBBY → everyoneReady() → VOTE
VOTE → voteComplete() → COUNTDOWN
COUNTDOWN → countdownFinished() → ROUND_RUNNING
ROUND_RUNNING → roundOver() → ROUND_RESULT
ROUND_RESULT → nextRound() → COUNTDOWN or MATCH_END
MATCH_END → (게임 종료)
```

**핵심 메서드**:
```java
public void tick() {
    switch (currentPhase) {
        case LOBBY:
            if (everyoneReady.get()) {
                transitionTo(Phase.VOTE);
            }
            break;
            
        case VOTE:
            if (voteComplete.get()) {
                transitionTo(Phase.COUNTDOWN);
            }
            break;
            
        case COUNTDOWN:
            countdownTicks--;
            if (countdownTicks <= 0) {
                transitionTo(Phase.ROUND_RUNNING);
            }
            break;
            
        case ROUND_RUNNING:
            // 게임 로직 실행
            if (checkRoundOver()) {
                transitionTo(Phase.ROUND_RESULT);
            }
            break;
            
        case ROUND_RESULT:
            resultDisplayTicks--;
            if (resultDisplayTicks <= 0) {
                if (matchEnded) {
                    transitionTo(Phase.MATCH_END);
                } else {
                    transitionTo(Phase.COUNTDOWN);
                }
            }
            break;
    }
}
```

**조건 함수 주입**:
```java
public void setConditions(
    BooleanSupplier everyoneReady,
    BooleanSupplier voteComplete,
    BooleanSupplier roundOver,
    BooleanSupplier matchEnded
) {
    this.everyoneReady = everyoneReady;
    this.voteComplete = voteComplete;
    // ...
}
```

---

#### 3. TcpServer.java - TCP 서버 구현

**서버 소켓 리스닝**:
```java
public void start(Consumer<ServerSession> onNewSession) {
    ServerSocket serverSocket = new ServerSocket(port);
    
    // Accept 루프 (별도 스레드)
    acceptThread = new Thread(() -> {
        while (running.get()) {
            try {
                Socket clientSocket = serverSocket.accept();
                ServerSession session = new ServerSession(
                    nextSessionId++, clientSocket
                );
                onNewSession.accept(session);
            } catch (IOException e) {
                // 연결 에러 처리
            }
        }
    });
    
    acceptThread.start();
}
```

---

#### 4. SessionRegistry.java - 세션 관리

**세션 등록/해제**:
```java
public class SessionRegistry {
    private Map<Integer, ServerSession> sessions = new ConcurrentHashMap<>();
    private Map<Integer, Integer> teamSelection = new ConcurrentHashMap<>();
    private Map<Integer, Integer> characterSelection = new ConcurrentHashMap<>();
    
    public void register(ServerSession session) {
        sessions.put(session.id(), session);
    }
    
    public void unregister(int sessionId) {
        sessions.remove(sessionId);
        teamSelection.remove(sessionId);
        characterSelection.remove(sessionId);
    }
    
    public void setCharacterTeam(int sessionId, int team) {
        teamSelection.put(sessionId, team);
    }
    
    public int getCharacterTeam(int sessionId) {
        return teamSelection.getOrDefault(sessionId, -1);
    }
}
```

---

#### 5. DefaultServerRouter.java - 메시지 라우팅

**프레임 라우팅**:
```java
public void handleSession(ServerSession session) {
    // 환영 메시지 전송
    sendWelcome(session);
    
    // 프레임 수신 루프
    while (session.isConnected()) {
        Protocol.Frame frame = session.readFrame();
        
        switch (frame.opcode) {
            case Protocol.CHAT:
                String text = Protocol.parseChat(frame.payload);
                broadcastChat(session.id(), text);
                break;
                
            case Protocol.PING:
                long nonce = Protocol.parseNonce(frame.payload);
                session.sendPong(nonce);
                break;
                
            case Protocol.READY_TOGGLE:
                boolean ready = Protocol.parseReadyToggle(frame.payload);
                hooks.onReadyToggle(session.id(), ready);
                break;
                
            case Protocol.SET_SELECTION:
                // 팀/캐릭터 선택 처리
                break;
                
            // ... 기타 opcode 처리
        }
    }
}
```

**브로드캐스트**:
```java
public void broadcastPhaseUpdate(Phase phase) {
    byte[] payload = Protocol.serializePhaseUpdate(phase);
    for (ServerSession s : registry.getAllSessions()) {
        s.sendFrame(Protocol.PHASE_UPDATE, payload);
    }
}
```

---

## 클라이언트 모듈 (comfps-client)

### 📌 핵심 클래스 분석

#### 1. NetClient.java - 네트워크 클라이언트

**연결 관리**:
```java
public synchronized void connect(String host, int port, int timeoutMs) 
    throws IOException {
    
    socket = new Socket();
    socket.connect(new InetSocketAddress(host, port), timeoutMs);
    socket.setTcpNoDelay(true); // Nagle 알고리즘 비활성화
    
    in = new DataInputStream(socket.getInputStream());
    out = new DataOutputStream(socket.getOutputStream());
    
    startRxThread(); // 수신 스레드 시작
    
    listener.onOpen(this);
}
```

**수신 스레드**:
```java
private void startRxThread() {
    rxThread = new Thread(() -> {
        try {
            while (running.get()) {
                Protocol.Frame frame = Protocol.readFrame(in);
                dispatchFrame(frame);
            }
        } catch (EOFException e) {
            listener.onDisconnected("Server closed connection");
        } catch (IOException e) {
            listener.onDisconnected("Connection error: " + e.getMessage());
        }
    });
    
    rxThread.start();
}
```

**프레임 디스패치**:
```java
private void dispatchFrame(Protocol.Frame frame) {
    Runnable task = () -> {
        switch (frame.opcode) {
            case Protocol.WELCOME:
                listener.onWelcome(Protocol.parseWelcome(frame.payload));
                break;
                
            case Protocol.CHAT:
                listener.onChat(Protocol.parseChat(frame.payload));
                break;
                
            case Protocol.PHASE_UPDATE:
                listener.onPhaseUpdate(Protocol.parsePhaseUpdate(frame.payload));
                break;
                
            case Protocol.SNAPSHOT:
                List<SnapshotV2.Entry> entries = 
                    SnapshotV2.parseToList(frame.payload);
                listener.onSnapshotV2(entries);
                break;
                
            // ... 기타 opcode 처리
        }
    };
    
    // EDT 디스패치 (Swing 스레드 안전성)
    if (dispatchOnEdt) {
        SwingUtilities.invokeLater(task);
    } else {
        task.run();
    }
}
```

**전송 메서드**:
```java
public synchronized void sendChat(String text) throws IOException {
    Protocol.sendChat(out, text);
}

public synchronized void sendReadyToggle(boolean ready) throws IOException {
    Protocol.sendReadyToggle(out, ready);
}

public synchronized void sendSetSelection(int team, int character) 
    throws IOException {
    Protocol.sendSetSelection(out, team, character);
}
```

---

#### 2. PlayerSnapshotBuffer.java - 스냅샷 보간

**목적**: 서버에서 받은 스냅샷을 버퍼링하고 보간하여 부드러운 움직임 구현

**버퍼 구조**:
```java
public class PlayerSnapshotBuffer {
    private static class TimedSnapshot {
        long timestampNs;           // 수신 시각 (나노초)
        Map<Integer, Entry> data;   // 플레이어 데이터
    }
    
    private Deque<TimedSnapshot> buffer = new ArrayDeque<>();
    private long interpDelayNs = 100_000_000L; // 100ms 지연
}
```

**스냅샷 추가**:
```java
public void push(byte[] snapshotPayload, long recvNs) {
    Map<Integer, Entry> data = SnapshotV2.parseToMap(snapshotPayload);
    buffer.addLast(new TimedSnapshot(recvNs, data));
    
    // 오래된 스냅샷 제거 (1초 이상)
    while (!buffer.isEmpty() && 
           nowNs - buffer.peekFirst().timestampNs > 1_000_000_000L) {
        buffer.removeFirst();
    }
}
```

**보간 샘플링**:
```java
public Map<Integer, Entry> sample(long nowNs) {
    long targetNs = nowNs - interpDelayNs; // 100ms 과거 시점
    
    // targetNs를 둘러싼 두 스냅샷 찾기
    TimedSnapshot before = null, after = null;
    for (TimedSnapshot ts : buffer) {
        if (ts.timestampNs <= targetNs) {
            before = ts;
        } else {
            after = ts;
            break;
        }
    }
    
    if (before == null || after == null) {
        return before != null ? before.data : after.data;
    }
    
    // 선형 보간
    float t = (targetNs - before.timestampNs) / 
              (float)(after.timestampNs - before.timestampNs);
    
    Map<Integer, Entry> result = new HashMap<>();
    for (int playerId : before.data.keySet()) {
        Entry e1 = before.data.get(playerId);
        Entry e2 = after.data.get(playerId);
        
        if (e2 != null) {
            result.put(playerId, interpolate(e1, e2, t));
        } else {
            result.put(playerId, e1);
        }
    }
    
    return result;
}

private Entry interpolate(Entry e1, Entry e2, float t) {
    return new Entry(
        e1.playerId,
        Mathf.lerp(e1.x, e2.x, t),      // X 보간
        Mathf.lerp(e1.y, e2.y, t),      // Y 보간
        Mathf.lerpAngle(e1.aim, e2.aim, t), // 각도 보간
        e2.team,
        e2.character
    );
}
```

---

#### 3. UI 컴포넌트

**GameFrame.java - 메인 프레임**:
```java
public class GameFrame extends JFrame {
    private GameCanvas canvas;      // 게임 렌더링
    private HudPanel hudPanel;      // HUD 오버레이
    private ChatPanel chatPanel;    // 채팅
    
    public GameFrame() {
        setTitle("FPS Game");
        setSize(1280, 720);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        
        // 레이아웃 구성
        setLayout(new BorderLayout());
        add(canvas, BorderLayout.CENTER);
        add(hudPanel, BorderLayout.NORTH);
        add(chatPanel, BorderLayout.SOUTH);
    }
}
```

**LobbyPanel.java - 로비 UI**:
```java
public class LobbyPanel extends JPanel {
    private JButton readyButton;
    private JComboBox<String> teamSelector;
    private JComboBox<String> characterSelector;
    private JList<String> playerList;
    
    public LobbyPanel(NetClient client) {
        setLayout(new GridBagLayout());
        
        // 준비 버튼
        readyButton.addActionListener(e -> {
            boolean newState = !isReady;
            client.sendReadyToggle(newState);
            isReady = newState;
        });
        
        // 팀 선택
        teamSelector.addActionListener(e -> {
            String team = (String) teamSelector.getSelectedItem();
            int teamId = team.equals("Red") ? 0 : 1;
            client.sendSetSelection(teamId, currentCharacter);
        });
    }
}
```

**HudPanel.java - HUD 표시**:
```java
public class HudPanel extends JPanel {
    private JLabel phaseLabel;
    private JLabel countdownLabel;
    private JLabel scoreLabel;
    
    public void updatePhase(Phase phase) {
        phaseLabel.setText("Phase: " + phase.name());
    }
    
    public void updateCountdown(int seconds) {
        countdownLabel.setText("Starting in: " + seconds);
    }
    
    public void updateScore(int redRounds, int blueRounds) {
        scoreLabel.setText(
            String.format("Score - Red: %d | Blue: %d", 
                         redRounds, blueRounds)
        );
    }
}
```

---

## 네트워크 프로토콜

### 프레임 기반 통신

**기본 프레임 구조**:
```
┌────────────────┬───────────┬──────────────────┐
│  Length (4B)   │ Opcode(1B)│   Payload (N)    │
└────────────────┴───────────┴──────────────────┘
```

### 주요 프로토콜 상세

#### 1. WELCOME (0x01) - 서버 환영
**방향**: Server → Client  
**페이로드**:
```
[4B: myId] [4B: worldW] [4B: worldH] [4B: mapId]
```
**설명**: 클라이언트 연결 시 플레이어 ID와 월드 정보 전송

#### 2. CHAT (0x02) - 채팅
**방향**: Client ↔ Server ↔ All Clients  
**페이로드**:
```
[2B: textLength] [N bytes: UTF-8 text]
```
**설명**: 채팅 메시지 브로드캐스트

#### 3. PING (0x03) / PONG (0x04) - RTT 측정
**방향**: Client → Server → Client  
**페이로드**:
```
[8B: nonce (timestamp)]
```
**설명**: 
- 클라이언트가 현재 시각을 nonce로 PING 전송
- 서버가 동일한 nonce로 PONG 응답
- 클라이언트가 RTT 계산: `currentTime - nonce`

#### 4. READY_TOGGLE (0x10) - 준비 토글
**방향**: Client → Server  
**페이로드**:
```
[1B: ready (0=false, 1=true)]
```
**설명**: 로비에서 준비 상태 변경

#### 5. SET_SELECTION (0x11) - 팀/캐릭터 선택
**방향**: Client → Server  
**페이로드**:
```
[4B: teamId] [4B: characterId]
```
**설명**: 플레이어의 팀 및 캐릭터 선택

#### 6. MAP_VOTE (0x12) - 맵 투표
**방향**: Client → Server  
**페이로드**:
```
[4B: mapId]
```
**설명**: 맵 선택 투표

#### 7. PHASE_UPDATE (0x20) - 단계 업데이트
**방향**: Server → All Clients  
**페이로드**:
```
[4B: phaseCode]
```
**phaseCode 매핑**:
```
0 = LOBBY
1 = VOTE
2 = COUNTDOWN
3 = ROUND_RUNNING
4 = ROUND_RESULT
5 = MATCH_END
```

#### 8. COUNTDOWN (0x21) - 카운트다운
**방향**: Server → All Clients  
**페이로드**:
```
[4B: seconds]
```
**설명**: 라운드 시작 카운트다운 (5→4→3→2→1)

#### 9. ROUND_RESULT (0x22) - 라운드 결과
**방향**: Server → All Clients  
**페이로드**:
```
[4B: winnerTeam] [4B: blueRounds] [4B: redRounds] [1B: matchEnded]
```
**설명**: 라운드 종료 시 승자 및 스코어 전송

#### 10. READY_STATUS (0x23) - 준비 상태
**방향**: Server → All Clients  
**페이로드**:
```
[4B: readyCount] [4B: totalCount]
또는 확장 버전:
[4B: count]
for each player:
    [4B: sessionId] [1B: ready] [4B: team] [4B: character]
```

#### 11. SNAPSHOT (0x31) - 플레이어 스냅샷
**방향**: Server → All Clients (20Hz)  
**페이로드**:
```
[4B: playerCount]
for each player:
    [4B: playerId]
    [4B: float x]
    [4B: float y]
    [4B: float aim]
    [1B: team]
    [1B: character]
```
**설명**: 게임 진행 중 모든 플레이어 상태 동기화

#### 12. PROJECTILES (0x32) - 투사체
**방향**: Server → All Clients  
**페이로드**:
```
[4B: count]
for each projectile:
    [4B: id] [4B: float x] [4B: float y] [4B: float vx] [4B: float vy]
```

---

## 게임 플로우

### 전체 흐름도

```
                          ┌─────────────┐
                          │   서버 시작   │
                          └──────┬──────┘
                                 │
                          ┌──────▼──────┐
                    ┌────►│    LOBBY     │
                    │     └──────┬──────┘
                    │            │ everyoneReady?
                    │            │ (인원>=2, 모두 READY, 팀 선택, 밸런스)
                    │     ┌──────▼──────┐
                    │     │     VOTE     │
                    │     └──────┬──────┘
                    │            │ voteComplete?
                    │            │ (전원 투표 또는 타임아웃)
                    │     ┌──────▼──────┐
                    │     │  COUNTDOWN   │◄────┐
                    │     └──────┬──────┘     │
                    │            │ 5초 경과    │
                    │            │            │
                    │     ┌──────▼──────┐     │
                    │     │ROUND_RUNNING│     │
                    │     └──────┬──────┘     │
                    │            │ roundOver? │
                    │            │            │
                    │     ┌──────▼──────┐     │
                    │     │ROUND_RESULT │     │
                    │     └──────┬──────┘     │
                    │            │            │
                    │            ├─ matchEnded? No ─┘
                    │            │
                    │            │ Yes
                    │     ┌──────▼──────┐
                    └─────┤  MATCH_END   │
                          └──────┬──────┘
                                 │ 다시 시작
                                 └──────┐
                                        │
                                   (재시작)
```

### 단계별 상세 설명

#### 1. LOBBY (대기실)
**목적**: 플레이어가 모이고 준비하는 단계

**활동**:
- 플레이어 접속 대기
- 팀 선택 (RED/BLUE)
- 캐릭터 선택 (10종)
- READY 토글

**다음 단계 조건**:
```java
총 인원 >= 2
&& 모두 READY
&& 모든 플레이어 팀 선택 완료
&& 팀 밸런스: |RED - BLUE| <= 1
```

**클라이언트 UI**:
- 플레이어 목록 표시 (팀, 캐릭터, READY 상태)
- READY 버튼
- 팀 선택 콤보박스
- 캐릭터 선택 다이얼로그

---

#### 2. VOTE (맵 투표)
**목적**: 플레이할 맵 선택

**활동**:
- 각 플레이어가 맵 투표 (TERMINAL, NEON_CITY, FOREST_OUTPOST)
- 투표 현황 실시간 표시

**다음 단계 조건**:
```java
전원 투표 완료 || 투표 타임아웃 (30초)
```

**투표 집계**:
```java
// 다수결 방식
Map<MapId, Integer> votes = new HashMap<>();
for (Vote v : allVotes) {
    votes.merge(v.mapId, 1, Integer::sum);
}
MapId winner = votes.entrySet().stream()
    .max(Map.Entry.comparingByValue())
    .get().getKey();
```

---

#### 3. COUNTDOWN (카운트다운)
**목적**: 라운드 시작 준비

**활동**:
- 5 → 4 → 3 → 2 → 1 카운트다운
- 플레이어 스폰 위치 설정
- 무기/스킬 초기화

**다음 단계 조건**:
```java
카운트다운 == 0
```

**클라이언트 UI**:
- 화면 중앙에 큰 숫자 표시
- "Get Ready!" 메시지

---

#### 4. ROUND_RUNNING (라운드 진행)
**목적**: 실제 게임 플레이

**활동**:
- 플레이어 이동/공격/스킬 사용
- 스냅샷 브로드캐스트 (20Hz)
- 충돌 감지 및 데미지 처리
- 승리 조건 확인

**승리 조건** (예시):
```java
// 전멸: 한 팀 모두 사망
Team redAlive = countAlive(Team.RED);
Team blueAlive = countAlive(Team.BLUE);

if (redAlive == 0) {
    winnerTeam = Team.BLUE;
} else if (blueAlive == 0) {
    winnerTeam = Team.RED;
}

// 또는 시간 제한 (5분)
if (elapsedTime > 300_000) {
    winnerTeam = getTeamWithMoreHP();
}
```

---

#### 5. ROUND_RESULT (라운드 결과)
**목적**: 라운드 결과 표시 및 스코어 갱신

**활동**:
- 승리 팀 발표
- 라운드 스코어 갱신
- 매치 종료 여부 확인 (3선승)

**다음 단계 조건**:
```java
if (redRounds >= 3 || blueRounds >= 3) {
    matchEnded = true;
    nextPhase = MATCH_END;
} else {
    nextPhase = COUNTDOWN; // 다음 라운드
}
```

**클라이언트 UI**:
- "Red Team Wins!" / "Blue Team Wins!"
- 스코어보드 (Red 2 - 1 Blue)
- MVP 표시 (킬/데미지 최다)

---

#### 6. MATCH_END (매치 종료)
**목적**: 게임 종료 및 통계 표시

**활동**:
- 최종 승자 발표
- 전체 통계 표시 (킬/데스/어시스트)
- 로비로 복귀 옵션

**클라이언트 UI**:
- 최종 스코어
- 개인 통계
- "Play Again" 버튼

---

## 캐릭터 시스템

### 캐릭터 목록 및 특징

| 캐릭터 | 역할 | 체력 | 이속 | 기본 공격 | E 스킬 | Q 궁극기 |
|--------|------|------|------|-----------|--------|----------|
| **Raven** | 돌격병 | 150 | 5.0 | AR 점사 | 전술 돌진 (50px) | 전탄 발사 (공속 2배) |
| **Piper** | 저격수 | 100 | 4.5 | 저격총 | 레이더 핑 | 궁극 조준 (즉발) |
| **Bulldog** | 중화기병 | 200 | 3.5 | 미니건 | 대형 방패 (3초) | 충격파 (넉백) |
| **Sage** | 지원가 | 120 | 5.0 | SMG | 치유 필드 | 집중 치유 (100%) |
| **Ghost** | 첩보원 | 110 | 5.5 | 소음 권총 | 은신 | 배후 습격 (순간이동) |
| **Wildcat** | 샷건 전문가 | 140 | 4.8 | 자동 샷건 | 쇠약 투척 | 분쇄 사격 (관통) |
| **Technician** | 공병 | 130 | 4.5 | 플라즈마 라이플 | 점착 지뢰 | 자동 포탑 |
| **General** | 지휘관 | 150 | 4.7 | 전술 AR (3점사) | 사기 진작 (버프) | 공중 폭격 |
| **Steam** | 특수부대 | 140 | 5.0 | 하이테크 AR | EMP 수류탄 | 전술 초기화 |
| **Skull** | 용병 | 160 | 4.6 | 전투용 카빈 | 아드레날린 | 생명력 흡수 |

---

## 빌드 및 실행

### 🔨 빌드 방법

#### 1. 전체 빌드
```bash
cd /path/to/fpsGame
mvn clean install
```

#### 2. 모듈별 빌드
```bash
# 공통 모듈만
mvn clean install -pl comfps-common

# 서버만
mvn clean install -pl comfps-server

# 클라이언트만
mvn clean install -pl comfps-client
```

#### 3. 테스트 제외 빌드
```bash
mvn clean install -DskipTests
```

---

### 🚀 실행 방법

#### 서버 실행

**방법 1: Maven exec 플러그인**
```bash
cd comfps-server
mvn exec:java
```

**방법 2: JAR 파일 실행**
```bash
cd comfps-server
mvn package
java -jar target/comfps-server-1.0-SNAPSHOT.jar
```

**방법 3: IDE에서 실행**
```
Main Class: com.fpsgame.server.ServerMain
```

**서버 포트**: 7777 (기본값)

---

#### 클라이언트 실행

**방법 1: Maven exec 플러그인**
```bash
cd comfps-client
mvn exec:java
```

**방법 2: JAR 파일 실행**
```bash
cd comfps-client
mvn package
java -jar target/comfps-client-1.0-SNAPSHOT.jar
```

**방법 3: IDE에서 실행**
```
Main Class: com.fpsgame.MainLauncher
```

**기본 접속**: localhost:7777

---

### 🧪 테스트 실행

```bash
# 전체 테스트
mvn test

# 특정 모듈 테스트
mvn test -pl comfps-client

# 특정 테스트 클래스
mvn test -Dtest=ViewportTransformTest
```

---

### 📦 배포 준비

```bash
# 실행 가능한 JAR 생성 (의존성 포함)
mvn clean package

# 생성물 위치
# comfps-server/target/comfps-server-1.0-SNAPSHOT.jar
# comfps-client/target/comfps-client-1.0-SNAPSHOT.jar
```

---

## 추가 참고 자료

### 프로젝트 문서
- `README.md`: 프로젝트 소개 및 빠른 시작
- `PROTOCOL.md`: 네트워크 프로토콜 명세서
- `DESIGN.md`: 설계 문서 및 주차별 계획
- `CHARACTER_SPECS.md`: 캐릭터 및 맵 상세 명세
- `RUNNING.md`: 실행 방법 상세

### 주요 패키지 구조
```
com.fpsgame.common.*        # 공통 유틸리티 및 프로토콜
com.fpsgame.server.*        # 서버 로직
com.fpsgame.client.*        # 클라이언트 로직
com.fpsgame.client.ui.*     # UI 컴포넌트
com.fpsgame.client.model.*  # 클라이언트 모델
com.fpsgame.character.*     # 캐릭터 시스템
```

### 코드 스타일
- **언어**: Java 17
- **주석**: 한글 (프로젝트 내부용)
- **명명 규칙**: camelCase (변수/메서드), PascalCase (클래스)
- **들여쓰기**: 4 spaces

---

## 🎓 학습 가이드

### 초급 - 기본 이해
1. `Protocol.java` 읽기 → 프레임 구조 이해
2. `GameEnums.java` 읽기 → 게임 규칙 파악
3. `ServerMain.main()` 추적 → 서버 흐름 이해
4. `NetClient.connect()` 추적 → 클라이언트 연결 이해

### 중급 - 심화 학습
1. `GameServer.tick()` 분석 → FSM 패턴 학습
2. `PlayerSnapshotBuffer` 분석 → 보간 알고리즘 이해
3. `DefaultServerRouter` 분석 → 메시지 라우팅 패턴
4. `LobbyPanel` 분석 → Swing UI/EDT 학습

### 고급 - 확장 및 최적화
1. 새 캐릭터 추가하기
2. 새 맵 구현하기
3. 프로토콜 최적화 (압축, 델타 인코딩)
4. NIO.2 기반 비동기 서버로 전환

---

## 📞 문의 및 기여

본 프로젝트는 학습 목적으로 제작되었습니다.  
이슈 리포팅 및 개선 제안은 GitHub Issues를 이용해주세요.

**Repository**: https://github.com/khy1121/fpsGame
