# 🎮 FPS Game 프로젝트 학습 가이드

## 📖 이 문서는 무엇인가요?

프로젝트에 대해 공부하고 싶다는 요청에 따라, 프로젝트의 **기술 스택과 주요 코드를 상세하게 정리한 문서**를 작성했습니다.

## 📚 작성된 문서

### ⭐ TECH_STACK.md (신규 작성 - 핵심 문서!)
**42KB, 1,513줄의 상세한 기술 문서**

이 문서는 프로젝트의 모든 것을 담고 있습니다:

#### 1️⃣ 프로젝트 개요
- 프로젝트명 및 개발 목적
- 핵심 특징 (순수 Java TCP, 바이너리 프로토콜, 멀티 모듈 등)

#### 2️⃣ 기술 스택 상세 설명
- **Java 17** - LTS 버전, 최신 기능 활용
- **Apache Maven** - 멀티 모듈 빌드 도구
- **Java NIO TCP Socket** - 네트워킹 구현
- **Java Swing** - UI 프레임워크
- **JUnit 5** - 테스트 프레임워크
- 의존성 구조 및 플러그인 설명

#### 3️⃣ 프로젝트 구조
- 전체 디렉터리 구조
- 모듈별 역할 (common, server, client)
- 패키지 구조 및 파일 조직

#### 4️⃣ 공통 모듈 (comfps-common) 핵심 클래스 분석
- **Protocol.java** - 바이너리 프로토콜 구현
  - 프레임 구조 [길이|opcode|payload]
  - 13개 Opcode 상세 설명
  - 직렬화/역직렬화 메서드
  
- **GameEnums.java** - 게임 규칙 및 열거형
  - Phase (6단계 FSM)
  - Team, Character (10종), Map (3종)
  - 게임 규칙 상수
  
- **SnapshotV2.java** - 플레이어 상태 동기화
  - Entry 구조 (위치, 각도, 팀, 캐릭터)
  - 직렬화 형식
  - 사용 사례
  
- **캐릭터 시스템**
  - CharacterBase, CharacterStats
  - 10개 캐릭터 구현 예시 (Raven 상세)

- **수학 유틸리티**
  - Vec2, Rect, Mathf

#### 5️⃣ 서버 모듈 (comfps-server) 핵심 클래스 분석
- **ServerMain.java** - 서버 진입점
  - 8단계 초기화 과정 상세
  - LobbyHooks 구현
  
- **GameServer.java** - 게임 FSM
  - 상태 전이도
  - 각 Phase별 tick 로직
  - 조건 함수 주입 패턴
  
- **TcpServer.java** - TCP 서버
  - ServerSocket 리스닝
  - Accept 루프
  
- **SessionRegistry.java** - 세션 관리
  - 세션 등록/해제
  - 팀/캐릭터 선택 관리
  
- **DefaultServerRouter.java** - 메시지 라우팅
  - 프레임 라우팅 로직
  - 브로드캐스트 구현

#### 6️⃣ 클라이언트 모듈 (comfps-client) 핵심 클래스 분석
- **NetClient.java** - 네트워크 클라이언트
  - 연결 관리
  - 수신 스레드
  - 프레임 디스패치 (EDT 안전성)
  - 전송 메서드들
  
- **PlayerSnapshotBuffer.java** - 스냅샷 보간
  - 버퍼 구조
  - 선형 보간 알고리즘 (100ms 지연)
  - 부드러운 움직임 구현 원리
  
- **UI 컴포넌트**
  - GameFrame, LobbyPanel, HudPanel
  - Swing EDT 패턴

#### 7️⃣ 네트워크 프로토콜 상세
12개 주요 프로토콜 상세 설명:
- WELCOME, CHAT, PING/PONG
- READY_TOGGLE, SET_SELECTION, MAP_VOTE
- PHASE_UPDATE, COUNTDOWN, ROUND_RESULT
- READY_STATUS, SNAPSHOT, PROJECTILES

각 프로토콜마다:
- 방향 (Client→Server, Server→Client 등)
- 페이로드 구조 (바이트 레벨)
- 설명 및 사용 사례

#### 8️⃣ 게임 플로우 상세
- **전체 흐름도** (ASCII 다이어그램)
- **6단계 상세 설명**:
  1. LOBBY - 플레이어 모집 및 준비
  2. VOTE - 맵 투표
  3. COUNTDOWN - 라운드 시작 카운트다운
  4. ROUND_RUNNING - 게임 플레이
  5. ROUND_RESULT - 결과 표시
  6. MATCH_END - 매치 종료

각 단계마다:
- 목적, 활동, 다음 단계 조건
- 코드 예시
- 클라이언트 UI 설명

#### 9️⃣ 캐릭터 시스템
10개 캐릭터 상세 표:
- 역할, 체력, 이동속도
- 기본 공격, E 스킬, Q 궁극기

#### 🔟 빌드 및 실행
- Maven 빌드 방법 (전체/모듈별)
- 서버/클라이언트 실행 방법 (3가지)
- 테스트 실행
- 배포 준비

#### 1️⃣1️⃣ 학습 가이드
- **초급**: Protocol, GameEnums 이해
- **중급**: FSM, 보간, 라우팅 학습
- **고급**: 새 기능 추가, 최적화

---

## 🎯 학습 순서 추천

### 1단계: 개요 파악 (30분)
```
1. TECH_STACK.md의 "프로젝트 개요" 읽기
2. "기술 스택" 섹션 읽기
3. "프로젝트 구조" 확인
```

### 2단계: 공통 모듈 이해 (1시간)
```
1. TECH_STACK.md의 "공통 모듈" 섹션 정독
2. 실제 코드 확인:
   - comfps-common/src/main/java/com/fpsgame/Protocol.java
   - comfps-common/src/main/java/com/fpsgame/GameEnums.java
   - comfps-common/src/main/java/com/fpsgame/SnapshotV2.java
```

### 3단계: 네트워크 프로토콜 학습 (1시간)
```
1. TECH_STACK.md의 "네트워크 프로토콜" 섹션 정독
2. PROTOCOL.md 참고
3. Protocol.java 코드 분석
```

### 4단계: 서버 이해 (1.5시간)
```
1. TECH_STACK.md의 "서버 모듈" 섹션 정독
2. 실제 코드 확인:
   - comfps-server/src/main/java/com/fpsgame/ServerMain.java
   - comfps-server/src/main/java/com/fpsgame/GameServer.java
3. 서버 실행해보기
```

### 5단계: 클라이언트 이해 (1.5시간)
```
1. TECH_STACK.md의 "클라이언트 모듈" 섹션 정독
2. 실제 코드 확인:
   - comfps-client/src/main/java/com/fpsgame/NetClient.java
   - comfps-client/src/main/java/com/fpsgame/model/PlayerSnapshotBuffer.java
3. 클라이언트 실행해보기
```

### 6단계: 게임 플로우 이해 (1시간)
```
1. TECH_STACK.md의 "게임 플로우" 섹션 정독
2. DESIGN.md 참고
3. 서버+클라이언트 동시 실행하여 전체 플로우 체험
```

### 7단계: 심화 학습 (자유)
```
1. 캐릭터 시스템 분석
2. 새 캐릭터 추가 실습
3. 프로토콜 최적화 연구
4. NIO.2 비동기 전환 연구
```

---

## 📂 모든 문서 목록

| 문서 | 크기 | 설명 |
|------|------|------|
| **TECH_STACK.md** | 42KB | **📌 핵심 기술 문서** (이번에 새로 작성!) |
| README.md | 2.9KB | 프로젝트 소개 및 빠른 시작 (업데이트됨) |
| PROTOCOL.md | 2.7KB | 네트워크 프로토콜 요약 |
| DESIGN.md | 5.4KB | 설계 문서 및 주차별 계획 |
| CHARACTER_SPECS.md | 6.2KB | 캐릭터 및 맵 상세 명세 |

---

## 💡 핵심 개념 요약

### 1. 아키텍처
```
Client (Swing UI) ← TCP Socket → Server (Game Logic)
         ↓                              ↓
    NetClient.java                 TcpServer.java
         ↓                              ↓
   Protocol 직렬화 ←────────────→ Protocol 역직렬화
         ↓                              ↓
   UI 업데이트                      FSM + 브로드캐스트
```

### 2. 프로토콜 구조
```
┌────────────────┬───────────┬──────────────────┐
│  Length (4B)   │ Opcode(1B)│   Payload (N)    │
└────────────────┴───────────┴──────────────────┘
```

### 3. 게임 FSM
```
LOBBY → VOTE → COUNTDOWN → ROUND_RUNNING → ROUND_RESULT → MATCH_END
   ↑                            ↓              ↓
   └────────────────────────────┴──────────────┘
```

### 4. 보간 알고리즘
```
현재 시각 - 100ms = 목표 시각
목표 시각을 둘러싼 두 스냅샷 찾기
→ 선형 보간 (lerp)
→ 부드러운 움직임
```

---

## 🚀 빠른 실행 가이드

### 1. 빌드
```bash
cd /path/to/fpsGame
mvn clean install
```

### 2. 서버 실행
```bash
cd comfps-server
mvn exec:java
```

### 3. 클라이언트 실행 (새 터미널)
```bash
cd comfps-client
mvn exec:java
```

### 4. 체험
1. 클라이언트 창에서 팀 선택 (Red/Blue)
2. 캐릭터 선택
3. READY 버튼 클릭
4. (2명 이상이면) 맵 투표
5. 카운트다운 후 게임 시작!

---

## 🎓 다음 단계

### 프로젝트 이해를 위한 TODO
- [ ] TECH_STACK.md 전체 정독 (약 3-4시간)
- [ ] 서버 코드 따라가며 분석
- [ ] 클라이언트 코드 따라가며 분석
- [ ] 실제 실행하여 네트워크 패킷 확인 (Wireshark 사용 가능)
- [ ] 새 캐릭터 추가 실습
- [ ] 새 맵 추가 실습

### 심화 학습 주제
- [ ] Java NIO.2 비동기 I/O 적용
- [ ] 프로토콜 압축 (zlib, snappy)
- [ ] 델타 인코딩으로 대역폭 최적화
- [ ] UDP 적용 연구 (신뢰성 vs 지연)
- [ ] 게임 로직 확장 (충돌 감지, 데미지 계산)

---

## 📞 질문이 있다면?

모든 주요 코드와 개념이 **TECH_STACK.md**에 상세히 설명되어 있습니다.

각 섹션마다:
- 코드 예시
- 다이어그램
- 설명
- 사용 사례

가 포함되어 있으니, 궁금한 부분을 찾아서 읽어보세요!

---

## ✅ 작업 완료 요약

이번 작업으로 다음이 완료되었습니다:

✅ **TECH_STACK.md 작성** (42KB, 1,513줄)
  - 프로젝트 개요
  - 기술 스택 상세 (Java 17, Maven, TCP, Swing, JUnit)
  - 공통/서버/클라이언트 모듈 분석
  - 네트워크 프로토콜 12종 상세
  - 게임 플로우 6단계 설명
  - 캐릭터 시스템 분석
  - 빌드/실행 가이드
  - 학습 로드맵

✅ **README.md 업데이트**
  - TECH_STACK.md 링크 추가
  - Maven 빌드 명령어 추가
  - 문서 구조 개선

✅ **.gitignore 개선**
  - target/ 디렉터리 제외
  - *.class, *.jar 제외
  - IDE 파일 제외

---

**이제 프로젝트의 모든 기술과 코드를 체계적으로 공부할 수 있습니다! 🎉**
