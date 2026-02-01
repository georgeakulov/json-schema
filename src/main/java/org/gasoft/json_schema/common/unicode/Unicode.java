package org.gasoft.json_schema.common.unicode;

import org.gasoft.json_schema.common.RangeCollections.IntRangeSet;

public class Unicode {

    private static final IntRangeSet VIRAMA = preloadUnicodeData();

    public static boolean isVirama(int codePoint) {
        return VIRAMA.contains(codePoint);
    }

    private static IntRangeSet preloadUnicodeData() {
        IntRangeSet rangeSet = new IntRangeSet();
        ParseUtils.forEachLine(
                "UnicodeDataShort.txt",
                line -> rangeSet.addAll(ParseUtils.parseNumbers(line))
        );
        return rangeSet;
    }
}
