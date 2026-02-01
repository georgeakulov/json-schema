package org.gasoft.json_schema.common;

import org.gasoft.json_schema.common.RangeCollections.IntRangeSet;
import org.gasoft.json_schema.common.RangeCollections.Range;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class IntRangeSetTest {

    IntRangeSet set = new IntRangeSet();

    @BeforeEach
    void setUp() {
        set = new IntRangeSet();
    }

    @AfterEach
    void tearDown() {
        System.out.println(set);
    }

    @Test
    void testSingle() {
        set.add(1);
        assertTrue(set.contains(1));
        assertFalse(set.contains(2));
        assertFalse(set.contains(0));
    }

    @Test
    void testJoinAfter() {
        set.add(new Range(1, 6));
        set.add(new Range(7, 8));
        assertEquals(1, set.internalSize());
    }

    @Test
    void testInsertInMiddle() {
        set.add(new Range(1, 2));
        assertEquals(1, set.internalSize());
        set.add(new Range(5, 6));
        assertEquals(2, set.internalSize());
        set.add(new Range(3, 4));
        assertEquals(1, set.internalSize());
    }

    @Test
    void testCombine() {
        set.add(new Range(1, 2));
        assertEquals(1, set.internalSize());
        set.add(new Range(4, 5));
        assertEquals(2, set.internalSize());
        set.add(new Range(7, 8));
        assertEquals(3, set.internalSize());
        set.add(new Range(3, 6));
        assertEquals(1, set.internalSize());
    }

    @Test
    void testAddAll() {
        set.add(new Range(1, 2));
        set.add(new Range(4, 5));
        assertEquals(2, set.internalSize());
        var set1 = new IntRangeSet();
        set1.add(3);
        set.addAll(set1);
        assertEquals(1, set.internalSize());
    }

    @Test
    void testRange() {
        set.add(new Range(4, 6));
        IntStream.of(3, 7)
                        .forEach(idx -> assertFalse(set.contains(idx)));
        IntStream.rangeClosed(4, 6)
                        .forEach(idx -> assertTrue(set.contains(idx)));
    }

    @Test
    void putNear() {
        set.add(new Range(1, 3));
        set.add(new Range(4, 5));
        IntStream.of(0, 7)
                .forEach(idx -> assertFalse(set.contains(idx)));
        IntStream.of(1, 2, 3, 4, 5)
                .forEach(idx -> assertTrue(set.contains(idx)));
    }

    @Test
    void putIntersectedRight() {
        set.add(new Range(1, 4));
        set.add(new Range(3, 6));
        IntStream.of(0, 7)
                .forEach(idx -> assertFalse(set.contains(idx)));
        IntStream.rangeClosed(1, 6)
                .forEach(idx -> assertTrue(set.contains(idx)));
    }

    @Test
    void putIntersectedLeft() {
        set.add(new Range(2, 6));
        set.add(new Range(1, 5));
        IntStream.of(0, 7)
                .forEach(idx -> assertFalse(set.contains(idx)));
        IntStream.rangeClosed(1, 6)
                .forEach(idx -> assertTrue(set.contains(idx)));
    }

    @Test
    void putInner() {
        set.add(new Range(1, 6));
        set.add(new Range(2, 5));
        IntStream.of(0, 7)
                .forEach(idx -> assertFalse(set.contains(idx)));
        IntStream.rangeClosed(1, 6)
                .forEach(idx -> assertTrue(set.contains(idx)));
    }

    @Test
    void putInnerLeft() {
        set.add(new Range(1, 6));
        set.add(new Range(1, 5));
        IntStream.of(0, 7)
                .forEach(idx -> assertFalse(set.contains(idx)));
        IntStream.rangeClosed(1, 6)
                .forEach(idx -> assertTrue(set.contains(idx)));
    }

    @Test
    void putInnerRight() {
        set.add(new Range(1, 6));
        set.add(new Range(2, 6));
        IntStream.of(0, 7)
                .forEach(idx -> assertFalse(set.contains(idx)));
        IntStream.rangeClosed(1, 6)
                .forEach(idx -> assertTrue(set.contains(idx)));
    }

    @Test
    void putReplaced() {
        set.add(new Range(1, 6));
        set.add(new Range(1, 6));
        IntStream.of(0, 7)
                .forEach(idx -> assertFalse(set.contains(idx)));
        IntStream.rangeClosed(1, 6)
                .forEach(idx -> assertTrue(set.contains(idx)));
    }

    @Test
    void putReplacedMore() {
        set.add(new Range(2, 5));
        set.add(new Range(1, 6));
        IntStream.of(0, 7)
                .forEach(idx -> assertFalse(set.contains(idx)));
        IntStream.rangeClosed(1, 6)
                .forEach(idx -> assertTrue(set.contains(idx)));
    }

    @Test
    void errorCreating() {
        assertThrows(IllegalArgumentException.class, () ->
            new Range(4,3)
        );
    }
}