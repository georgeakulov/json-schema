package org.gasoft.json_schema.common.unicode;

import org.gasoft.json_schema.common.RangeCollections;
import org.gasoft.json_schema.common.RangeCollections.IntRangeSet;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

class ParseUtils {

    static void forEachLine(String resourceName, Consumer<String> lineConsumer) {
        try {

            var cl = Thread.currentThread().getContextClassLoader();
            try(var is = new BufferedReader(new InputStreamReader(Objects.requireNonNull(cl.getResourceAsStream(resourceName))))){
                // Skip first line as title
                var line = is.readLine();
                while(line != null) {
                    //parse line
                    lineConsumer.accept(line);
                    line = is.readLine();
                }
            }
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    static NamedRange parseLine(String line) {
        if(line.isBlank()){
            return null;
        }
        int sep = line.indexOf(";");
        return new NamedRange(
                line.substring(0, sep),
                parseNumbers(line.substring(sep + 1))
        );
    }

    static IntRangeSet parseNumbers(String line) {
        IntRangeSet rangeSet = new IntRangeSet();
        Arrays.stream(line.split(","))
                .map(ParseUtils::parseItem)
                .forEach(rangeSet::add);
        return rangeSet;
    }

    private static RangeCollections.Range parseItem(String item) {
        var idx = item.indexOf("-");
        if(idx < 0) {
            return RangeCollections.Range.of(Integer.parseInt(item, 16));
        }
        return new RangeCollections.Range(
                Integer.parseInt(item, 0, idx, 16),
                Integer.parseInt(item, idx + 1, item.length(), 16)
        );
    }

    record NamedRange(String name, IntRangeSet rangeSet) {}
}
