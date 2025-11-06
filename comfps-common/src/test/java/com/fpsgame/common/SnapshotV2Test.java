package com.fpsgame.common;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * SnapshotV2 바이너리 프로토콜 단위 테스트
 * 
 * 테스트 시나리오:
 * 1. 빌드 및 파싱 라운드트립 (build → parse)
 * 2. 빈 리스트 처리
 * 3. 여러 플레이어 엔트리 처리
 * 4. parseToMap() 변환
 * 5. 잘못된 버전 처리
 */
public class SnapshotV2Test {
    
    @Test
    void testBuildAndParseRoundtrip() throws IOException {
        // Given: 2명의 플레이어 엔트리
        List<SnapshotV2.Entry> original = List.of(
            new SnapshotV2.Entry(1, 450f, 1700f, 0.5f, 0, 0, 100, 0f, 0f),      // RED팀 플레이어 1
            new SnapshotV2.Entry(2, 2550f, 1700f, -0.5f, 1, 1, 150, 2.5f, 10f)  // BLUE팀 플레이어 2
        );
        
        // When: 빌드 후 파싱
        byte[] binary = SnapshotV2.build(original);
        List<SnapshotV2.Entry> parsed = SnapshotV2.parse(binary);
        
        // Then: 원본과 동일해야 함
        assertEquals(2, parsed.size());
        
        // 첫 번째 플레이어 검증
        SnapshotV2.Entry p1 = parsed.get(0);
        assertEquals(1, p1.id);
        assertEquals(450f, p1.x, 0.001f);
        assertEquals(1700f, p1.y, 0.001f);
        assertEquals(0.5f, p1.aim, 0.001f);
        assertEquals(0, p1.team);           // RED
        assertEquals(0, p1.characterId);
        assertEquals(100, p1.hp);
        assertEquals(0f, p1.tacticalCd, 0.001f);
        assertEquals(0f, p1.ultimateCd, 0.001f);
        
        // 두 번째 플레이어 검증
        SnapshotV2.Entry p2 = parsed.get(1);
        assertEquals(2, p2.id);
        assertEquals(2550f, p2.x, 0.001f);
        assertEquals(1700f, p2.y, 0.001f);
        assertEquals(-0.5f, p2.aim, 0.001f);
        assertEquals(1, p2.team);           // BLUE
        assertEquals(1, p2.characterId);
        assertEquals(150, p2.hp);
        assertEquals(2.5f, p2.tacticalCd, 0.001f);
        assertEquals(10f, p2.ultimateCd, 0.001f);
    }
    
    @Test
    void testEmptyList() throws IOException {
        // Given: 빈 리스트
        List<SnapshotV2.Entry> empty = List.of();
        
        // When: 빌드 후 파싱
        byte[] binary = SnapshotV2.build(empty);
        List<SnapshotV2.Entry> parsed = SnapshotV2.parse(binary);
        
        // Then: 빈 리스트 반환
        assertNotNull(parsed);
        assertTrue(parsed.isEmpty());
    }
    
    @Test
    void testNullPayloadReturnsEmptyList() throws IOException {
        // When: null 페이로드 파싱
        List<SnapshotV2.Entry> parsed = SnapshotV2.parse(null);
        
        // Then: 빈 리스트 반환 (예외 없음)
        assertNotNull(parsed);
        assertTrue(parsed.isEmpty());
    }
    
    @Test
    void testParseToMap() throws IOException {
        // Given: 3명의 플레이어
        List<SnapshotV2.Entry> entries = List.of(
            new SnapshotV2.Entry(10, 100f, 200f, 0f, 0, 0, 100, 0f, 0f),
            new SnapshotV2.Entry(20, 300f, 400f, 1.0f, 1, 1, 150, 5f, 15f),
            new SnapshotV2.Entry(30, 500f, 600f, -1.0f, 0, 2, 200, 0f, 0f)
        );
        
        byte[] binary = SnapshotV2.build(entries);
        
        // When: parseToMap() 사용
        Map<Integer, SnapshotV2.Entry> map = SnapshotV2.parseToMap(binary);
        
        // Then: ID로 조회 가능
        assertEquals(3, map.size());
        assertTrue(map.containsKey(10));
        assertTrue(map.containsKey(20));
        assertTrue(map.containsKey(30));
        
        assertEquals(100f, map.get(10).x, 0.001f);
        assertEquals(300f, map.get(20).x, 0.001f);
        assertEquals(500f, map.get(30).x, 0.001f);
    }
    
    @Test
    void testInvalidVersionThrowsException() {
        // Given: 잘못된 버전 (v1 페이로드)
        byte[] invalidVersion = new byte[] {
            0x01,  // version = 1 (not 2)
            0, 0, 0, 0  // count = 0
        };
        
        // When/Then: IOException 발생
        IOException exception = assertThrows(IOException.class, () -> {
            SnapshotV2.parse(invalidVersion);
        });
        
        assertTrue(exception.getMessage().contains("unsupported snapshot version"));
    }
    
    @Test
    void testInvalidCountThrowsException() {
        // Given: 음수 카운트
        byte[] invalidCount = new byte[] {
            0x02,                      // version = 2
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF  // count = -1
        };
        
        // When/Then: IOException 발생
        IOException exception = assertThrows(IOException.class, () -> {
            SnapshotV2.parse(invalidCount);
        });
        
        assertTrue(exception.getMessage().contains("invalid count"));
    }
    
    @Test
    void testTruncatedPayloadThrowsException() {
        // Given: 불완전한 페이로드 (count는 1인데 데이터 없음)
        byte[] truncated = new byte[] {
            0x02,                      // version = 2
            0, 0, 0, 1,                // count = 1
            0, 0, 0, 1                 // id = 1만 있고 나머지 필드 없음
        };
        
        // When/Then: IOException 발생
        IOException exception = assertThrows(IOException.class, () -> {
            SnapshotV2.parse(truncated);
        });
        
        assertTrue(exception.getMessage().contains("truncated"));
    }
    
    @Test
    void testFloatingPointPrecision() throws IOException {
        // Given: 다양한 float 값
        List<SnapshotV2.Entry> entries = List.of(
            new SnapshotV2.Entry(1, 1234.5678f, -9876.5432f, 3.14159f, 0, 0, 100, 0.123f, 99.999f)
        );
        
        // When: 빌드 후 파싱
        byte[] binary = SnapshotV2.build(entries);
        List<SnapshotV2.Entry> parsed = SnapshotV2.parse(binary);
        
        // Then: float 정밀도 유지 (약 6-7 자릿수)
        SnapshotV2.Entry p = parsed.get(0);
        assertEquals(1234.5678f, p.x, 0.001f);
        assertEquals(-9876.5432f, p.y, 0.001f);
        assertEquals(3.14159f, p.aim, 0.00001f);
        assertEquals(0.123f, p.tacticalCd, 0.00001f);
        assertEquals(99.999f, p.ultimateCd, 0.001f);
    }
    
    @Test
    void testBinaryFormatSize() throws IOException {
        // Given: 1개 엔트리
        List<SnapshotV2.Entry> entries = List.of(
            new SnapshotV2.Entry(1, 0f, 0f, 0f, 0, 0, 100, 0f, 0f)
        );
        
        // When: 빌드
        byte[] binary = SnapshotV2.build(entries);
        
        // Then: 예상 크기 = 1(version) + 4(count) + (4+4+4+4+1+1+4+4+4) = 5 + 30 = 35 bytes
        assertEquals(35, binary.length);
    }
}
