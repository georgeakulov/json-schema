package org.gasoft.json_schema.common.unicode;

import org.gasoft.json_schema.common.RangeCollections.IntRangeSet;

import java.util.HashMap;
import java.util.Map;

public class JoinType {

    private static final Map<String, IntRangeSet> JOIN_TYPES = preloadJoinTypes();

    public static boolean isNotTransparent(int code) {
        return !evalType("T", code);
    }

    public static boolean isDual(int code) {
        return evalType("D", code);
    }

    public static boolean isLeft(int code) {
        return evalType("L", code);
    }

    public static boolean isRight(int code) {
        return evalType("R", code);
    }

    private static boolean evalType(String type, int code) {
        var range = JOIN_TYPES.get(type);
        return range != null && range.contains(code);
    }

    private static Map<String, IntRangeSet> preloadJoinTypes() {
        Map<String, IntRangeSet> result = new HashMap<>();
        ParseUtils.forEachLine("DerivedJoiningTypeShort.txt", line -> {
            var parseResult = ParseUtils.parseLine(line);
            if(parseResult != null) {
                result.put(parseResult.name(), parseResult.rangeSet());
            }
        });
        return result;
    }
}
