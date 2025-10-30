package com.fpsgame.client.model;

/**
 * 클라이언트 로컬 플레이어 상태 모델(경량).
 * <p>
 * - 이름, 팀, 위치(x,y), 체력 등의 기본 속성 보유
 * - 스레드 안전을 위해 간단한 동기화 사용(렌더/입력 스레드 혼용 대비)
 * - 네트워크 동기화/서버 권위는 상위 계층에서 처리
 */
public final class PlayerState {

    /** 팀 정의(클라이언트 단순 표시용) */
    public enum Team {
        RED, BLUE
    }

    private String name;
    private Team team;

    // 월드 좌표(wu)
    private float x;
    private float y;

    // 체력(데모용 단순 수치)
    private int hp = 100;
    private int hpMax = 100;

    public PlayerState(String name, Team team) {
        this.name = (name == null || name.isBlank()) ? "Player" : name.trim();
        this.team = team == null ? Team.RED : team;
    }

    // ---------------------------------------------------------------------
    // 위치/이동
    // ---------------------------------------------------------------------

    public synchronized void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public synchronized void translate(float dx, float dy) {
        this.x += dx;
        this.y += dy;
    }

    public synchronized float getX() { return x; }
    public synchronized float getY() { return y; }

    // ---------------------------------------------------------------------
    // 이름/팀
    // ---------------------------------------------------------------------

    public synchronized String getName() { return name; }

    public synchronized void setName(String name) {
        this.name = (name == null || name.isBlank()) ? this.name : name.trim();
    }

    public synchronized Team getTeam() { return team; }

    public synchronized void setTeam(Team team) {
        if (team != null) this.team = team;
    }

    // ---------------------------------------------------------------------
    // 체력
    // ---------------------------------------------------------------------

    public synchronized int getHp() { return hp; }
    public synchronized int getHpMax() { return hpMax; }

    public synchronized void setHpMax(int hpMax) {
        this.hpMax = Math.max(1, hpMax);
        this.hp = Math.min(this.hp, this.hpMax);
    }

    /** 데미지를 적용(0 이하 무시) */
    public synchronized void damage(int amount) {
        if (amount <= 0) return;
        this.hp = Math.max(0, this.hp - amount);
    }

    /** 치유 적용(0 이하 무시) */
    public synchronized void heal(int amount) {
        if (amount <= 0) return;
        this.hp = Math.min(this.hpMax, this.hp + amount);
    }

    /** 사망 여부 */
    public synchronized boolean isDead() {
        return hp <= 0;
    }

    @Override
    public synchronized String toString() {
        return "PlayerState{" +
                "name='" + name + '\'' +
                ", team=" + team +
                ", x=" + x +
                ", y=" + y +
                ", hp=" + hp + "/" + hpMax +
                '}';
    }
}
