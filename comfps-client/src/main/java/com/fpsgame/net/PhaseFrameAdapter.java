package com.fpsgame.client.net;

import com.fpsgame.client.NetClient;
import com.fpsgame.client.model.ClientPhaseBus;

import javax.swing.*;

/**
 * PhaseFrameAdapter(페이즈 프레임 어댑터)
 * - 기존 코드 호환을 위한 경량 no-op 구현
 * - 컴파일 의존성 제거 목적이며, 추후 필요 시 실제 바인딩/패킷 처리로 확장
 * 공개 API: 생성자 오버로드, maybeInstall(), maybeInstallOn(), installTo(), attach()/detach(), handle()
 */
public class PhaseFrameAdapter {

    // 선택: 클라이언트 페이즈 버스(이벤트 전달용)
    private final ClientPhaseBus bus;
    // 선택: 프레임/넷클라이언트 참조
    private JFrame frame;
    private NetClient net;

    /** 기본 생성자(버스는 싱글턴 사용) */
    public PhaseFrameAdapter() { this(ClientPhaseBus.get()); }

    /** 버스를 주입받는 생성자 */
    public PhaseFrameAdapter(ClientPhaseBus bus) { this.bus = (bus != null ? bus : ClientPhaseBus.get()); }

    /** 선택 설치(준비 확인용, no-op) */
    public static void maybeInstall() { ClientPhaseBus.get(); }

    /** 프레임에 선택 설치(no-op) */
    @SuppressWarnings("unused")
    public static void maybeInstallOn(JFrame frame) {
        if (frame == null) return;
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> maybeInstallOn(frame));
            return;
        }
        // no-op
    }

    /** 프레임/넷클라이언트를 받아 어댑터를 설치(참조 저장) */
    public static PhaseFrameAdapter installTo(JFrame frame, NetClient net) {
        PhaseFrameAdapter a = new PhaseFrameAdapter();
        a.attach(frame, net);
        return a;
    }

    /** 어댑터 부착(참조 저장) */
    public void attach(JFrame frame, NetClient net) {
        this.frame = frame;
        this.net = net;
        // 추후 HUD/버스 바인딩이 필요하면 여기서 처리
    }

    /** 어댑터 해제(참조 제거) */
    public void detach() { this.frame = null; this.net = null; }

    // 패킷 처리(호환용 no-op) -------------------------------------------
    /** 서버 패킷 처리 스텁(no-op). 필요 시 bus 이벤트 브로드캐스트로 확장 */
    public void handle(byte opcode, byte[] payload) { /* no-op */ }
    /** int opcode 오버로드 */
    public void handle(int opcode, byte[] payload) { handle((byte) opcode, payload); }
}

