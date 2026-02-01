package org.gasoft.json_schema.common;

import org.gasoft.json_schema.common.RangeCollections.Range;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class IntRangeMapTest {

    RangeCollections.IntRangeMap<Integer> map = new RangeCollections.IntRangeMap<Integer>();

    @BeforeEach
    void setUp() {
        map = new RangeCollections.IntRangeMap<>();
    }

    @AfterEach
    void tearDown() {
        System.out.println(map);
    }

    @Test
    void testSingle() {
        map.put(1, 1);
        assertEquals(1, map.get(1));
        assertNull(map.get(2));
        assertNull(map.get(0));
    }

    @Test
    void testRange() {
        map.put(new Range(4, 6), 1);
        assertNull(map.get(3));
        IntStream.rangeClosed(4, 6)
                        .forEach(idx -> assertEquals(1, map.get(idx)));
        assertNull(map.get(7));
    }

    @Test
    void putNear() {
        map.put(new Range(1, 3), 1);
        map.put(new Range(4, 5), 2);
        IntStream.of(0, 7)
                .forEach(idx -> assertNull(map.get(idx)));
        IntStream.of(1, 2, 3)
                .forEach(idx -> assertEquals(1, map.get(idx)));
        IntStream.of(4, 5)
                .forEach(idx -> assertEquals(2, map.get(idx)));
    }

    @Test
    void putIntersectedRight() {
        map.put(new Range(1, 4), 1);
        map.put(new Range(3, 6), 2);
        IntStream.of(0, 7)
                .forEach(idx -> assertNull(map.get(idx)));
        IntStream.of(1, 2)
                .forEach(idx -> assertEquals(1, map.get(idx)));
        IntStream.of(3, 4, 5, 6)
                .forEach(idx -> assertEquals(2, map.get(idx)));
    }

    @Test
    void putIntersectedLeft() {
        map.put(new Range(2, 6), 1);
        map.put(new Range(1, 5), 2);
        IntStream.of(0, 7)
                .forEach(idx -> assertNull(map.get(idx)));
        IntStream.rangeClosed(1, 5)
                .forEach(idx -> assertEquals(2, map.get(idx)));
        IntStream.of(6)
                .forEach(idx -> assertEquals(1, map.get(idx)));
    }

    @Test
    void putInner() {
        map.put(new Range(1, 6), 1);
        map.put(new Range(2, 5), 2);
        IntStream.of(0, 7)
                .forEach(idx -> assertNull(map.get(idx)));
        IntStream.rangeClosed(2, 5)
                .forEach(idx -> assertEquals(2, map.get(idx)));
        IntStream.of(1, 6)
                .forEach(idx -> assertEquals(1, map.get(idx)));
    }

    @Test
    void putInnerLeft() {
        map.put(new Range(1, 6), 1);
        map.put(new Range(1, 5), 2);
        IntStream.of(0, 7)
                .forEach(idx -> assertNull(map.get(idx)));
        IntStream.rangeClosed(1, 5)
                .forEach(idx -> assertEquals(2, map.get(idx)));
        IntStream.of(6)
                .forEach(idx -> assertEquals(1, map.get(idx)));
    }

    @Test
    void putInnerRight() {
        map.put(new Range(1, 6), 1);
        map.put(new Range(2, 6), 2);
        IntStream.of(0, 7)
                .forEach(idx -> assertNull(map.get(idx)));
        IntStream.rangeClosed(2, 6)
                .forEach(idx -> assertEquals(2, map.get(idx)));
        IntStream.of(1)
                .forEach(idx -> assertEquals(1, map.get(idx)));
    }

    @Test
    void putReplaced() {
        map.put(new Range(1, 6), 1);
        map.put(new Range(1, 6), 2);
        IntStream.of(0, 7)
                .forEach(idx -> assertNull(map.get(idx)));
        IntStream.rangeClosed(1, 6)
                .forEach(idx -> assertEquals(2, map.get(idx)));
    }

    @Test
    void putReplacedMore() {
        map.put(new Range(2, 5), 1);
        map.put(new Range(1, 6), 2);
        IntStream.of(0, 7)
                .forEach(idx -> assertNull(map.get(idx)));
        IntStream.rangeClosed(1, 6)
                .forEach(idx -> assertEquals(2, map.get(idx)));
    }

    @Test
    void errorCreating() {
        assertThrows(IllegalArgumentException.class, () ->
            new Range(4,3)
        );
    }
}