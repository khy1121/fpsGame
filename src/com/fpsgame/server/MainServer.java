package com.fpsgame.server;

import com.fpsgame.common.Protocol;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * (정리본) 레거시 메인 서버 래퍼.
 *
 * <p>역할</p>
 * <ul>
 *   <li>이전 프로젝트에 남아있는 MainServer 참조를 {@link ServerMain}으로 위임</li>
 *   <li>레거시 코드가 사용하는 내부 {@code Client} 타입의 시그니처를
 *       <b>byte 오퍼코드</b> 기반으로 교정하여 컴파일 오류 제거</li>
 *   <li>호환을 위해 널리 쓰이던 메서드의 오버로드(예: {@code send(...)})를 함께 제공</li>
 * </ul>
 *
 * <p>왜 필요한가?</p>
 * 레거시 코드에서 {@code new MainServer.Client(...).send(Protocol.Opcode.CHAT, payload)}처럼
 * 호출하는 부분이 있어, {@code Client#send(byte, byte[])} 오버로드가 반드시 존재해야 합니다.
 * 본 파일은 그 오버로드를 제공하고 내부적으로 {@link Protocol#write(DataOutputStream, byte, byte[])}
 * 를 호출합니다.
 */
@Deprecated
public final class MainServer {

    private MainServer() {}

    /** 레거시 엔트리 → ServerMain 위임 */
    public static void main(String[] args) {
        try {
            ServerMain.main(args);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    // =====================================================================
    // 레거시 호환용 내부 타입
    // =====================================================================

    /**
     * 과거 코드에서 사용하던 내부 타입에 대한 최소 구현.
     * <p>실제 네트 송신은 제공된 {@link DataOutputStream}으로 수행한다.</p>
     */
    @Deprecated
    public static final class Client {
        private final DataOutputStream out;

        public Client(DataOutputStream out) {
            this.out = out;
        }

        /**
         * 표준 오버로드: <b>byte</b> 오퍼코드를 사용.
         */
        public void send(byte opcode, byte[] payload) throws IOException {
            Protocol.write(out, opcode, payload);
        }

        /**
         * (레거시) int도 허용하여 암묵 캐스팅.
         */
        public void send(int opcode, byte[] payload) throws IOException {
            Protocol.write(out, (byte) opcode, payload);
        }

        /**
         * (레거시 호환) 잘못된 호출 패턴 방지용 헬퍼.
         * <p>과거 일부 코드에서 {@code Protocol.Opcode} <em>타입</em> 자체를 인자로 넘기는
         * 실수가 있었음. 컴파일러가 byte로 변환할 수 없는 경우가 있어, 다음과 같은 실수 호출을
         * 방지하기 위한 가드 메서드입니다.</p>
         * <pre>
         *   // 잘못된 예 (타입 자체를 넘김):
         *   client.send(Protocol.Opcode, payload); // 컴파일 에러
         * </pre>
         */
        @Deprecated
        public void send(Object wrongOpcodeMarker, byte[] payload) throws IOException {
            throw new IOException("잘못된 send 호출: Protocol.Opcode 타입 자체가 아니라 상수(예: Protocol.CHAT)를 전달하세요.");
        }

        /** 편의: 채팅 전송 */
        public void sendChat(String text) throws IOException {
            Protocol.sendChat(out, text);
        }

        /** 편의: 정상 종료 프레임 전송 */
        public void sendBye() throws IOException {
            Protocol.sendBye(out);
        }
    }
}
