package org.gasoft.json_schema.common.unicode;

import org.gasoft.json_schema.common.RangeCollections.IntRangeSet;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Scripts {

    public enum EScript {

        GREEK("Greek"),
        HEBREW("Hebrew"),
        HIRAGANA("Hiragana"),
        KATAKANA("Katakana"),
        HAN("Han");

        private final String dictName;
        EScript(String dictName) {
            this.dictName = dictName;
        }

        public String getDictName() {
            return dictName;
        }
    }

    public static boolean isInScript(EScript script, int code) {
        return DATA.get(script).contains(code);
    }

    private static final EnumMap<EScript, IntRangeSet> DATA = preloadScript();

    private static EnumMap<EScript, IntRangeSet> preloadScript() {
        Map<String, EScript> reverse = Arrays.stream(EScript.values()).collect(Collectors.toMap(
                EScript::getDictName,
                Function.identity()
        ));
        EnumMap<EScript, IntRangeSet> result = new EnumMap<>(EScript.class);
        ParseUtils.forEachLine("ScriptsShort.txt", line -> {
            var parseResult = ParseUtils.parseLine(line);
            if(parseResult != null) {
                var script = reverse.get(parseResult.name());
                Objects.requireNonNull(script, "wrong resource data ");
                result.put(script, parseResult.rangeSet());
            }
        });
        return result;
    }
}
