package org.gasoft.json_schema.common;

import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RangeCollections {

    protected static class AbstractRangeMap<T> {
        protected final NavigableMap<Integer, T> map = new TreeMap<>();
        private final Function<T, Range> toRange;
        private final BiFunction<Range, T, T> toType;

        public AbstractRangeMap(Function<T, Range> toRange, BiFunction<Range, T, T> toType) {
            this.toRange = toRange;
            this.toType = toType;
        }

        public void putInt(int key, T value) {
            putInt(new Range(key, key), value);
        }

        public void putInt(Range range, T value) {
            removeAndPutInt(range, value);
        }

        @Nullable
        public T getInt(int key) {
            var ceiling = map.ceilingEntry(key);
            var floor = map.floorEntry(key);
            if(ceiling == null) {
                if(floor != null && toRange.apply(floor.getValue()).contains(key)) {
                    return floor.getValue();
                }
            }
            else {
                if(toRange.apply(ceiling.getValue()).contains(key)) {
                    return ceiling.getValue();
                }
                else if(floor != null && toRange.apply(floor.getValue()).contains(key)) {
                    return floor.getValue();
                }
            }
            return null;
        }

        protected void removeAndPutInt(Range range, T value) {
            List<Integer> intersected = null;
            var floor = map.floorEntry(range.min);
            if(floor != null) {
                var forward = map.tailMap(floor.getKey(), true);
                intersected = getIntersected(range, forward, checked -> checked.min <= range.max);
            }
            else {
                var ceil = map.ceilingEntry(range.max);
                if(ceil != null) {
                    var forward = map.headMap(ceil.getKey(), true).reversed();
                    intersected = getIntersected(range, forward, checked -> checked.max >= range.min);
                }
                else {
                    var middle = map.subMap(range.min, true, range.max, true);
                    if(!middle.isEmpty()) {
                        intersected = getIntersected(range, middle, checked -> checked.min <= range.max);
                    }
                }
            }

            List<T> newPayloads = new ArrayList<>();
            newPayloads.add(toType.apply(range, value));
            if(intersected != null) {
                intersected.stream()
                        .sorted()
                        .forEach(key -> {
                            var val = map.remove(key);
                            var anotherRange = toRange.apply(val);
                            anotherRange.fullSplit(range)
                                    .forEach(subRange -> newPayloads.add(toType.apply(subRange, val)));
                        });
            }
            tryJoin(newPayloads).forEach(pl -> map.put(toRange.apply(pl).min, pl));
        }

        protected List<T> tryJoin(List<T> of) {
            return of;
        }

        protected List<Integer> getIntersected(Range checked, Map<Integer, T> map, Predicate<Range> finisher) {
            List<Integer> intersected = new ArrayList<>();
            for (Map.Entry<Integer, T> entry : map.entrySet()) {
                var current = toRange.apply(entry.getValue());
                if(checked.isIntersected(current)) {
                    intersected.add(entry.getKey());
                }
                if(!finisher.test(current)) {
                    break;
                }
            }
            return intersected;
        }

        @Override
        public String toString() {
            return map.toString();
        }
    }

    public static class IntRangeMap<T> extends AbstractRangeMap<RangePayload<T>>{

        public IntRangeMap() {
            super(RangePayload::range, (val, pl) -> new RangePayload<>(val, pl.payload()));
        }

        public void put(int key, T value) {
            put(new Range(key, key), value);
        }

        public void put(Range range, T value) {
            putInt(range, new RangePayload<>(range, value));
        }

        @Nullable
        public T get(int key) {
            var res = getInt(key);
            return res == null ? null : res.payload();
        }
    }

    public static class IntRangeSet extends AbstractRangeMap<Range> {

        public IntRangeSet() {
            super(Function.identity(), (r, r1) -> r);
        }

        public void add(int value) {
            add(new Range(value, value));
        }

        public void add(Range range) {
            putInt(range, range);
        }

        public void addAll(IntRangeSet range) {
            range.map.values().forEach(this::add);
        }

        public boolean contains(int value) {
            return getInt(value) != null;
        }

        @Override
        protected List<Range> tryJoin(List<Range> of) {

            of.sort(Comparator.comparing(Range::min));

            // Add before and after
            Integer floor = map.floorKey(of.getFirst().min);
            Integer ceil = map.ceilingKey(of.getFirst().max);
            if(floor != null) {
                of.addFirst(map.remove(floor));
            }
            if(ceil != null) {
                of.add(map.remove(ceil));
            }

            return joinRanges(of);
        }

        private List<Range> joinRanges(List<Range> of) {
            List<Range> result = new ArrayList<>();
            Range prev = null;
            for (Range range : of) {
                if(prev == null) {
                    prev = range;
                    continue;
                }
                if(prev.isJoinBefore(range)) {
                    prev = new Range(prev.min, range.max);
                }
                else {
                    result.add(prev);
                    prev = range;
                }
            }
            result.add(prev);
            return result;
        }

        public int internalSize() {
            return map.size();
        }

        @Override
        public String toString() {
            String sb = "IntRangeSet{" + this.map.values().stream()
                    .map(Range::toString)
                    .collect(Collectors.joining(",")) +
                    '}';
            return sb;
        }
    }

    public record Range(int min, int max) {

        public static Range of(int value) {
            return new Range(value, value);
        }

        public Range {
            if(min > max) {
                throw new IllegalArgumentException("Illegal range bounds, min:" + min + ", max:" + max);
            }
        }

        public boolean isJoinBefore(Range after) {
            return this.max == after.min - 1;
        }

        public boolean isIntersected(Range range) {
            return (min <= range.min && max  >= range.min)
                    || (range.min <= min && range.max >= min);
        }

        public boolean contains(Range range) {
            return min <= range.min && max >= range.max;
        }

        boolean contains(int checked) {
            return min <= checked && max >= checked;
        }

        public List<Range> fullSplit(Range byRange) {
            List<Range> result = new ArrayList<>();
            if(byRange.contains(this)) {
                return result;
            }
            if(!this.isIntersected(byRange)) {
                return result;
            }
            if(min < byRange.min) {
                result.add(new Range(min, byRange.min - 1));
            }
            if(max > byRange.max) {
                result.add(new Range(byRange.max + 1, max));
            }
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Range range = (Range) o;
            return min == range.min && max == range.max;
        }

        @Override
        public int hashCode() {
            return Objects.hash(min, max);
        }

        @Override
        public String toString() {
            return "Range[" + min + ", " + max + ']';
        }
    }

    private record RangePayload<T>(Range range, T payload){

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("RangePayload{");
            sb.append("range=").append(range);
            sb.append(", payload=").append(payload);
            sb.append('}');
            return sb.toString();
        }
    }
}
