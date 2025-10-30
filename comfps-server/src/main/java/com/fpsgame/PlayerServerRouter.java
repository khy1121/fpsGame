package com.fpsgame.server;

import com.fpsgame.common.character.projectile.ProjectileManager;

/**
 * 서버 권위 시뮬레이션/스냅샷 브로드캐스트의 최소 골격
 * - 고정 틱마다 호출되어 입력 집계/물리/스냅샷 준비를 이어갈 자리
 * - 현재는 투사체 매니저 업데이트와 간단한 로그만 수행
 */
public final class PlayerServerRouter {

    /** 기본 생성자(상태 없음) */
    public PlayerServerRouter() { }

    /**
     * 고정 틱마다 서버 상태를 갱신하고 필요하면 브로드캐스트한다.
     * @param context 브로드캐스트/로그 도우미(예: SessionRegistry)
     * @param dt      델타 시간(초). 예: 20Hz → 0.05f
     */
    public void tickAndBroadcast(DefaultServerRouter.ServerContext context, float dt) {
        if (context != null) context.log("tick dt=" + dt);

        // 투사체 업데이트(충돌/수명 등)
        try {
            ProjectileManager.getInstance().update(dt);
        } catch (Throwable ignore) {
            // 매니저가 비활성인 경우 등은 무시
        }
    }
}

