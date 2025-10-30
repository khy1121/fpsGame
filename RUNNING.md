# 🎮 FPS Game - 실행 가이드

**프로젝트**: comfps  
**버전**: 1.0.0-SNAPSHOT  
**Java 버전**: 17  
**빌드 도구**: Maven (선택) 또는 Eclipse/IntelliJ IDEA

---

## 📋 프로젝트 개요

멀티플레이어 FPS 게임 (Swing UI 기반)
- **클라이언트**: 게임 UI, 네트워크 연결, 입력 처리
- **서버**: 게임 로직, 플레이어 동기화, 맵 투표
- **공통**: 프로토콜, 캐릭터 시스템, 투사체 관리

---

## 🚀 빠른 시작 (Eclipse/IntelliJ)

### 1. 서버 실행
```
메인 클래스: com.fpsgame.server.ServerMain
포트: 12345 (기본값)
```

**Eclipse에서 실행:**
1. `src/com/fpsgame/server/ServerMain.java` 우클릭
2. `Run As` → `Java Application`

**IntelliJ에서 실행:**
1. `src/com/fpsgame/server/ServerMain.java` 우클릭
2. `Run 'ServerMain.main()'`

### 2. 클라이언트 실행
```
메인 클래스: com.fpsgame.client.ClientMain
서버 주소: localhost:12345
```

**실행 방법:**
1. `src/com/fpsgame/client/ClientMain.java` 우클릭
2. `Run As` → `Java Application`

---

## 🛠️ Maven으로 실행 (선택)

### 컴파일
```bash
mvn clean compile
```

### 서버 실행
```bash
mvn exec:java -P server
```

### 클라이언트 실행
```bash
mvn exec:java -P client
```

### 실행 가능한 JAR 생성
```bash
mvn clean package
java -jar target/comfps-1.0.0-SNAPSHOT.jar
```

---

## 📦 주요 엔트리 포인트

### 프로덕션 (실제 사용)

| 클래스 | 용도 | 설명 |
|--------|------|------|
| `com.fpsgame.server.ServerMain` | 🖥️ 서버 | 메인 게임 서버 (권장) |
| `com.fpsgame.client.ClientMain` | 🎮 클라이언트 | 데모/테스트용 클라이언트 |
| `com.fpsgame.client.ui.ClientApp` | 🖼️ UI 앱 | UI 런처 |
| `com.fpsgame.client.ui.GameFrame` | 🎯 게임 프레임 | 게임 메인 프레임 |

### 개발/테스트 (20+개)

| 클래스 | 용도 |
|--------|------|
| `com.fpsgame.client.ui.GameCanvasNetDemo` | 네트워크 렌더링 데모 |
| `com.fpsgame.client.ui.LobbyCenterStackPanel` | 로비 UI 테스트 |
| `com.fpsgame.client.ui.ChatPanel` | 채팅 UI 테스트 |
| `com.fpsgame.client.net.NetClientPhaseBridge` | 네트워크 브릿지 테스트 |

---

## 🎯 Maven 프로필

### client (기본 클라이언트)
```bash
mvn exec:java -P client
```
실행: `com.fpsgame.client.ClientMain`

### server (게임 서버)
```bash
mvn exec:java -P server
```
실행: `com.fpsgame.server.ServerMain`

### ui (UI 앱)
```bash
mvn exec:java -P ui
```
실행: `com.fpsgame.client.ui.ClientApp`

### demo (네트워크 데모)
```bash
mvn exec:java -P demo
```
실행: `com.fpsgame.client.ui.GameCanvasNetDemo`

---

## 📁 프로젝트 구조

```
comfps/
├── src/
│   ├── com/fpsgame/
│   │   ├── client/          # 클라이언트 코드 (98 파일)
│   │   │   ├── ui/          # Swing UI 컴포넌트
│   │   │   ├── net/         # 네트워크 헬퍼
│   │   │   ├── model/       # 클라이언트 상태/모델
│   │   │   └── tools/       # 개발 도구
│   │   ├── common/          # 공통 유틸리티 (77 파일)
│   │   │   ├── character/   # 캐릭터 시스템
│   │   │   │   ├── types/   # Ghost, Assault 등
│   │   │   │   └── projectile/ # 투사체 (Bullet 등)
│   │   │   └── ...          # Protocol, Vec2, Rect 등
│   │   └── server/          # 서버 코드 (15 파일)
│   │       ├── ServerMain.java
│   │       ├── GameServer.java
│   │       └── ...
│   ├── main/resources/
│   │   └── assets/maps/     # 맵 이미지 (추가 필요)
│   └── test/java/           # 테스트 코드
├── bin/                      # 컴파일 출력 (Eclipse)
├── target/                   # Maven 빌드 출력
├── pom.xml                   # Maven 설정
└── README.md

총 Java 파일: 190개
컴파일 에러: 0개 ✅
```

---

## 🗺️ 맵 리소스 추가 (선택)

게임 배경 맵 이미지가 필요한 경우:

### 맵 이미지 위치
```
src/main/resources/assets/maps/
├── terminal.png (또는 .jpg)
├── warehouse.png
└── ...
```

### 맵 이미지가 없을 때
- 배경 없이 **그리드만 표시**됩니다
- 게임 진행에는 문제 없음

---

## 🔧 컴파일 (javac 직접 사용)

Maven 없이 javac로 컴파일:

```bash
cd src
javac -d ../bin -encoding UTF-8 com/fpsgame/**/*.java
```

실행:
```bash
cd ..
java -cp bin com.fpsgame.server.ServerMain
```

---

## 🧪 테스트

### JUnit 테스트 실행 (Maven)
```bash
mvn test
```

### 제외된 테스트
- `ViewportTransformTest.java` - JUnit 의존성 누락으로 제외됨

---

## 🐛 문제 해결

### 컴파일 에러 발생 시
```bash
# 1. bin 디렉토리 정리
rm -rf bin/*

# 2. 재컴파일
cd src
javac -d ../bin -encoding UTF-8 com/fpsgame/**/*.java
```

### Eclipse에서 에러 발생 시
1. `Project` → `Clean...` 선택
2. 프로젝트 선택 후 `Clean`
3. `Project` → `Build Project`

### IntelliJ에서 에러 발생 시
1. `File` → `Invalidate Caches...`
2. `Invalidate and Restart` 클릭

---

## 📝 주요 기능

### 서버
- ✅ TCP 서버 (포트 12345)
- ✅ 플레이어 세션 관리
- ✅ 맵 투표 시스템
- ✅ 라운드 컨트롤러
- ✅ 투사체 동기화 (ProjectilesV2)

### 클라이언트
- ✅ 네트워크 연결 (NetClient)
- ✅ 자동 재접속 (AutoReconnect)
- ✅ 로비 UI (캐릭터 선택, 맵 투표)
- ✅ 게임 캔버스 (렌더링)
- ✅ 채팅 시스템
- ✅ HUD (체력, 탄약, FPS)

### 공통
- ✅ 프로토콜 (Protocol.java)
- ✅ 캐릭터 시스템 (Ghost, Assault 등)
- ✅ 투사체 시스템 (Bullet, ProjectileManager)
- ✅ 스냅샷 동기화 (SnapshotV1, V2)

---

## 📚 추가 문서

- `CLASSES.md` - 클래스 구조 설명
- `DESIGN.md` - 디자인 문서
- `PROTOCOL.md` - 네트워크 프로토콜
- `PROJECT_STATUS.md` - 프로젝트 상태 분석
- `README.md` - 프로젝트 개요

---

## 🎉 성공적으로 설정 완료!

현재 프로젝트는:
- ✅ **컴파일 에러 0개**
- ✅ **단일 모듈 Maven 프로젝트**
- ✅ **Eclipse/IntelliJ에서 바로 실행 가능**
- ✅ **190개 Java 파일 정상 작동**

**다음 단계**: 서버 실행 → 클라이언트 실행 → 게임 시작! 🚀
