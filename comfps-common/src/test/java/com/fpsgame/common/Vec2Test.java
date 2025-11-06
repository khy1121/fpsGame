package com.fpsgame.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Vec2 단위 테스트
 */
public class Vec2Test {

    private static final float EPSILON = 0.0001f;

    // 기본 생성자: (0,0)으로 초기화되는지 확인
    @Test
    void testDefaultConstructor() {
        Vec2 v = new Vec2();
        assertEquals(0f, v.x, EPSILON);
        assertEquals(0f, v.y, EPSILON);
    }

    // 값 기반 생성자: 지정 값으로 설정되는지 확인
    @Test
    void testConstructorWithValues() {
        Vec2 v = new Vec2(3f, 4f);
        assertEquals(3f, v.x, EPSILON);
        assertEquals(4f, v.y, EPSILON);
    }

    // set(x,y): 구성요소가 올바르게 설정되는지 확인
    @Test
    void testSet() {
        Vec2 v = new Vec2();
        v.set(5f, 12f);
        assertEquals(5f, v.x, EPSILON);
        assertEquals(12f, v.y, EPSILON);
    }

    // set(Vec2): 다른 벡터의 값을 복사하는지 확인
    @Test
    void testSetVec2() {
        Vec2 v1 = new Vec2(3f, 4f);
        Vec2 v2 = new Vec2();
        v2.set(v1);
        assertEquals(3f, v2.x, EPSILON);
        assertEquals(4f, v2.y, EPSILON);
    }

    // copy(): 동일한 값이지만 다른 인스턴스를 반환하는지 확인
    @Test
    void testCopy() {
        Vec2 v1 = new Vec2(3f, 4f);
        Vec2 v2 = v1.copy();
        
        assertEquals(v1.x, v2.x, EPSILON);
        assertEquals(v1.y, v2.y, EPSILON);
        assertNotSame(v1, v2);
    }

    // add(x,y): 벡터 덧셈 확인
    @Test
    void testAdd() {
        Vec2 v = new Vec2(1f, 2f);
        v.add(3f, 4f);
        assertEquals(4f, v.x, EPSILON);
        assertEquals(6f, v.y, EPSILON);
    }

    // add(Vec2): 다른 벡터 더하기 확인
    @Test
    void testAddVec2() {
        Vec2 v1 = new Vec2(1f, 2f);
        Vec2 v2 = new Vec2(3f, 4f);
        v1.add(v2);
        assertEquals(4f, v1.x, EPSILON);
        assertEquals(6f, v1.y, EPSILON);
    }

    // sub(x,y): 벡터 뺄셈 확인
    @Test
    void testSub() {
        Vec2 v = new Vec2(5f, 7f);
        v.sub(2f, 3f);
        assertEquals(3f, v.x, EPSILON);
        assertEquals(4f, v.y, EPSILON);
    }

    // sub(Vec2): 다른 벡터 빼기 확인
    @Test
    void testSubVec2() {
        Vec2 v1 = new Vec2(5f, 7f);
        Vec2 v2 = new Vec2(2f, 3f);
        v1.sub(v2);
        assertEquals(3f, v1.x, EPSILON);
        assertEquals(4f, v1.y, EPSILON);
    }

    // scale(s): 스칼라 곱 확인
    @Test
    void testScale() {
        Vec2 v = new Vec2(2f, 3f);
        v.scale(2f);
        assertEquals(4f, v.x, EPSILON);
        assertEquals(6f, v.y, EPSILON);
    }

    // scale(x,y): 성분별 스케일 확인
    @Test
    void testScaleComponents() {
        Vec2 v = new Vec2(2f, 3f);
        v.scale(2f, 3f);
        assertEquals(4f, v.x, EPSILON);
        assertEquals(9f, v.y, EPSILON);
    }

    // len2(): 제곱 길이 확인
    @Test
    void testLen2() {
        Vec2 v = new Vec2(3f, 4f);
        assertEquals(25f, v.len2(), EPSILON);
    }

    // len(): 길이(피타고라스) 확인
    @Test
    void testLen() {
        Vec2 v = new Vec2(3f, 4f);
        assertEquals(5f, v.len(), EPSILON);
    }

    // normalize(): 단위 벡터로 정규화되는지 확인
    @Test
    void testNormalize() {
        Vec2 v = new Vec2(3f, 4f);
        v.normalize();
        
        assertEquals(1f, v.len(), EPSILON);
        assertEquals(0.6f, v.x, EPSILON);
        assertEquals(0.8f, v.y, EPSILON);
    }

    // normalize(): (0,0) 처리 확인(변화 없어야 함)
    @Test
    void testNormalizeZeroVector() {
        Vec2 v = new Vec2(0f, 0f);
        v.normalize();
        
        assertEquals(0f, v.x, EPSILON);
        assertEquals(0f, v.y, EPSILON);
    }

    // distance(): 두 점 사이 거리 확인
    @Test
    void testDistance() {
        Vec2 v1 = new Vec2(0f, 0f);
        Vec2 v2 = new Vec2(3f, 4f);
        
        assertEquals(5f, v1.distance(v2), EPSILON);
        assertEquals(5f, v2.distance(v1), EPSILON);
    }

    // dot(): 내적 결과 확인
    @Test
    void testDot() {
        Vec2 v1 = new Vec2(1f, 2f);
        Vec2 v2 = new Vec2(3f, 4f);
        
        // 1*3 + 2*4 = 11
        assertEquals(11f, v1.dot(v2), EPSILON);
    }

    // 체이닝: add -> scale -> sub 연속 호출 결과 확인
    @Test
    void testChaining() {
        Vec2 v = new Vec2(1f, 1f);
        v.add(1f, 1f).scale(2f).sub(1f, 1f);
        
        assertEquals(3f, v.x, EPSILON);
        assertEquals(3f, v.y, EPSILON);
    }

        // dot(): 내적 결과(중복 시나리오 명확화)
        @Test
        void testDotProduct() {
            Vec2 v1 = new Vec2(1f, 2f);
            Vec2 v2 = new Vec2(3f, 4f);
        
            // 1*3 + 2*4 = 11
            assertEquals(11f, v1.dot(v2), EPSILON);
        }

    // cross(): 외적(z성분) 결과 확인
        @Test
        void testCrossProduct() {
            Vec2 v1 = new Vec2(1f, 0f);
            Vec2 v2 = new Vec2(0f, 1f);
        
            // 1*1 - 0*0 = 1
            assertEquals(1f, v1.cross(v2), EPSILON);
        }

    // epsilonEquals(): 허용 오차 내 동등성 확인
        @Test
        void testEpsilonEquals() {
            Vec2 v1 = new Vec2(1.0f, 2.0f);
            Vec2 v2 = new Vec2(1.0001f, 2.0001f);
        
            assertTrue(v1.epsilonEquals(v2, 0.001f));
            assertFalse(v1.epsilonEquals(v2, 0.00001f));
        }

    // mul(): 새로운 벡터를 반환하고 원본은 변경되지 않아야 함
        @Test
        void testMul() {
            Vec2 v = new Vec2(2f, 3f);
            Vec2 result = v.mul(2f);
        
            assertEquals(4f, result.x, EPSILON);
            assertEquals(6f, result.y, EPSILON);
        
            // 원본은 변경되지 않음
            assertEquals(2f, v.x, EPSILON);
            assertEquals(3f, v.y, EPSILON);
    }
}
