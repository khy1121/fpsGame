package com.fpsgame.common;

/**
 * 맵의 벽을 표현하는 클래스
 */
public class Wall {
    private final Vec2 start;    // 벽의 시작점
    private final Vec2 end;      // 벽의 끝점
    
    /**
     * 벽 생성자
     * @param start 시작점
     * @param end 끝점
     */
    public Wall(Vec2 start, Vec2 end) {
        this.start = start;
        this.end = end;
    }
    
    /**
     * 선분과 점 사이의 최단 거리를 계산
     * @param point 점의 위치
     * @return 점과 선분 사이의 최단 거리
     */
    public float distanceTo(Vec2 point) {
        // Avoid mutating start/end/point vectors; compute using components
        float wallX = end.x - start.x;
        float wallY = end.y - start.y;
        float psX = point.x - start.x;
        float psY = point.y - start.y;

        float denom = wallX * wallX + wallY * wallY;
        float t = denom > 0f ? (psX * wallX + psY * wallY) / denom : 0f;
        if (t < 0f) t = 0f; else if (t > 1f) t = 1f;

        float projX = start.x + wallX * t;
        float projY = start.y + wallY * t;

        float dx = point.x - projX;
        float dy = point.y - projY;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * 두 선분이 교차하는지 확인
     * @param lineStart 선분의 시작점
     * @param lineEnd 선분의 끝점
     * @return 교차 여부
     */
    public boolean intersects(Vec2 lineStart, Vec2 lineEnd) {
        // 선분 AB = 벽
        float ax = start.x;
        float ay = start.y;
        float bx = end.x;
        float by = end.y;
        
        // 선분 CD = 이동 경로
        float cx = lineStart.x;
        float cy = lineStart.y;
        float dx = lineEnd.x;
        float dy = lineEnd.y;
        
        float denominator = (bx - ax) * (dy - cy) - (by - ay) * (dx - cx);
        
        // 선분이 평행한 경우
        if (denominator == 0) return false;
        
        float t = ((cy - ay) * (dx - cx) - (cx - ax) * (dy - cy)) / denominator;
        float s = ((cy - ay) * (bx - ax) - (cx - ax) * (by - ay)) / denominator;
        
        return t >= 0 && t <= 1 && s >= 0 && s <= 1;
    }
}
