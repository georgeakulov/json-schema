package org.gasoft.json_schema.common.email;

import org.gasoft.json_schema.common.RangeCollections;
import org.gasoft.json_schema.common.RangeCollections.Range;
import org.gasoft.json_schema.common.unicode.*;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class HostnameValidator {

    private static final String ONLY_ASCII_REGEX = "^([\\x00-\\x7f]+)$";
    private static final Predicate<String> ONLY_ASCII_PATTERN = Pattern.compile(ONLY_ASCII_REGEX).asMatchPredicate();
    private static final String VALID_LDH_CHARS_REGEX = "^([0-9a-z\\-]+)$";
    private static final Predicate<String> VALID_LDH_CHARS_PATTERN = Pattern.compile(VALID_LDH_CHARS_REGEX, Pattern.CASE_INSENSITIVE).asMatchPredicate();

    private static final RangeCollections.IntRangeMap<BiPredicate<String, Integer>> CONTEXTUAL_RULES = new RangeCollections.IntRangeMap<>();
    static {
        CONTEXTUAL_RULES.put(0x200c, HostnameValidator::zeroWidthNonJoiner);
        CONTEXTUAL_RULES.put(0x200d, HostnameValidator::zeroWidthJoiner);
        CONTEXTUAL_RULES.put(0x00b7, HostnameValidator::middleDot);
        CONTEXTUAL_RULES.put(0x0375, HostnameValidator::greekLowerNumeralSign);
        CONTEXTUAL_RULES.put(0x05f3, HostnameValidator::hebrewPunctuationGeresh);
        CONTEXTUAL_RULES.put(0x05f4, HostnameValidator::hebrewPunctuationGershayim);
        CONTEXTUAL_RULES.put(0x30fb, HostnameValidator::katakanaMiddleDot);
        CONTEXTUAL_RULES.put(new Range(0x0660, 0x0669), HostnameValidator::arabicIndicDigits);
        CONTEXTUAL_RULES.put(new Range(0x06f0, 0x06f9), HostnameValidator::extendedArabicIndicDigits);
    }

    private static final HostnameValidator INSTANCE = new HostnameValidator();

    private HostnameValidator() {
    }

    public static Predicate<String> getHostNameValidator() {
        return str -> {
            try {
                return INSTANCE.validateHostname(str);
            }
            catch(Exception e) {
                return false;
            }
        };
    }

    public static Predicate<String> getIDNAHostnameValidator() {
        return str -> {
            try {
                return INSTANCE.validateIDNAHostname(str);
            }
            catch(Exception e) {
                return false;
            }
        };
    }

    private boolean validateIDNAHostname(String hostname) {

        if(!checkFullLength(hostname)) {
            // too long
            return false;
        }

        if(ONLY_ASCII_PATTERN.test(hostname)) {
            return validateHostname(hostname);
        }

        String [] labels = hostname.split("[\u002e\u3002\uff0e\uff61]", -1);
        if(labels.length == 0) {
            // No labels
            return false;
        }

        for(String label : labels) {

            if(!commonLabelCheck(label)) {
                return false;
            }

            if(ONLY_ASCII_PATTERN.test(label)) {

                if(!validateDLHLabel(label)) {
                    // Invalid DLH label
                    return false;
                }
            }
            else {
                if(!validateUnicodeLabel(label)){
                    // Invalid U-label
                    return false;
                }
            }
        }
        return true;
    }

    private boolean validateHostname(String hostname) {

        if(!checkFullLength(hostname)) {
            // too long
            return false;
        }

        if(!ONLY_ASCII_PATTERN.test(hostname)) {
            // Contains nonascii
            return false;
        }

        String[] labels = hostname.split("\\.", -1);
        if(labels.length == 0) {
            // No labels
            return false;
        }

        for (String label : labels) {

            if(!commonLabelCheck(label)) {
                return false;
            }

            if(!validateDLHLabel(label)) {
                return false;
            }
        }
        return true;
    }

    private boolean commonLabelCheck(String label) {
        if(label.isEmpty()) {
            // label empty
            return false;
        }

        // label too long
        return label.length() <= 63;
    }

    private boolean validateDLHLabel(String label) {

        if(label.startsWith("xn--")) {
            // Invalid A-label
            return validateALabel(label);
        }
        else {

            // Invalid DLH
            return validateDLH(label);
        }
    }

    private boolean validateDLH(String label) {

        if(!VALID_LDH_CHARS_PATTERN.test(label)) {
            // Non valid DLH symbols found
            return false;
        }

        if(!checkHyphens(label)) {
            // Invalid hyphens found
            return false;
        }

        // Illegal ACE -- on 3 and 4 positions
        return label.length() <= 3 || !label.substring(2).startsWith("--");
    }

    private boolean validateALabel(String label) {
        label = label.toLowerCase();
        var unic = Punycode.decode(label.toLowerCase().substring(4));
        return validateUnicodeLabel(unic);
    }

    private boolean validateUnicodeLabel(String unic) {

        if(!checkHyphens(unic)) {
            // wrong hyphens
            return false;
        }

        int first = unic.codePointAt(0);
        int type = Character.getType(first);
        if(type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK || type == Character.ENCLOSING_MARK) {
            // Leading combining mark
            return false;
        }


        for(int idx = 0; idx < unic.length(); idx++) {
            int ch = unic.codePointAt(idx);

            if(IDNA.isUnassigned(ch)) {
                // Found unassigned symbol RFC5891 4.2.2
                return false;
            }

            if(IDNA.isDisallowed(ch)) {
                // Found disallowed symbol RFC5891 4.2.2
                return false;
            }

            if(IDNA.isContext(ch)) {

                if(!checkContext(ch, unic, idx)) {
                    // Illegal check rules for context symbols
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkFullLength(String hostname) {
        return hostname.length() <= 253;
    }

    private boolean checkContext(int code, String label, int pos) {
        var rule = CONTEXTUAL_RULES.get(code);
        Objects.requireNonNull(rule, () -> "Can`t find rule for " + code);
        return rule.test(label, pos);
    }

    private boolean checkHyphens(String label) {
        if(label.startsWith("-")) {
            // Found hyphen in label start
            return false;
        }

        // Found hyphen in label end
        return !label.endsWith("-");
    }

    // A1 https://www.rfc-editor.org/rfc/rfc5892#appendix-A
    private static boolean zeroWidthNonJoiner(String label, int pos) {

        if(pos > 0 && Unicode.isVirama(label.codePointAt(pos-1))) {
            return true;
        }

        return validBefore(label, pos) && validAfter(label, pos);
    }


    private static boolean validBefore(String label, int pos) {
        int idx = pos - 1;
        while(idx >= 0) {
            int codePoint = label.codePointAt(idx);
            if(JoinType.isNotTransparent(codePoint)) {
                return JoinType.isDual(codePoint) || JoinType.isLeft(codePoint);
            }
            idx --;
        }
        return false;
    }

    private static boolean validAfter(String label, int pos) {
        int idx = pos + 1;
        while(idx < label.length()) {
            int codePoint = label.codePointAt(idx);
            if(JoinType.isNotTransparent(codePoint)) {
                return JoinType.isDual(codePoint) || JoinType.isRight(codePoint);
            }
            idx ++;
        }
        return false;
    }

    // A2 https://www.rfc-editor.org/rfc/rfc5892#appendix-A
    private static boolean zeroWidthJoiner(String label, int pos) {
        return pos > 0 && Unicode.isVirama(label.codePointAt(pos - 1));
    }

    // A3 https://www.rfc-editor.org/rfc/rfc5892#appendix-A
    private static boolean middleDot(String label, int pos) {
        if(pos == 0 || pos == label.length() - 1) {
            // Middle dot at start or end
            return false;
        }
        if(label.codePointAt(pos - 1) != 0x6c) {
            // No prev char
            return false;
        }
        // No after char
        return label.charAt(pos + 1) == 0x6c;
    }

    // A4 https://www.rfc-editor.org/rfc/rfc5892#appendix-A
    private static boolean greekLowerNumeralSign(String label, int pos) {
        if(pos < label.length() - 1) {
            return Scripts.isInScript(Scripts.EScript.GREEK, label.codePointAt(pos + 1));
        }
        return false;
    }

    // A5 https://www.rfc-editor.org/rfc/rfc5892#appendix-A
    private static boolean hebrewPunctuationGeresh(String label, int pos) {
        return pos != 0 && Scripts.isInScript(Scripts.EScript.HEBREW, label.codePointAt(pos - 1));
    }

    // A6 https://www.rfc-editor.org/rfc/rfc5892#appendix-A
    private static boolean hebrewPunctuationGershayim(String label, int pos) {
        return pos != 0 && Scripts.isInScript(Scripts.EScript.HEBREW, label.codePointAt(pos - 1));
    }

    // A7 https://www.rfc-editor.org/rfc/rfc5892#appendix-A
    private static boolean katakanaMiddleDot(String label, int pos) {

        return label.codePoints()
                .anyMatch(code -> Scripts.isInScript(Scripts.EScript.KATAKANA, code)
                    || Scripts.isInScript(Scripts.EScript.HIRAGANA, code)
                    || Scripts.isInScript(Scripts.EScript.HAN, code)
                );
    }

    // A8 https://www.rfc-editor.org/rfc/rfc5892#appendix-A
    private static boolean arabicIndicDigits(String label, int pos) {
        return label.codePoints()
                .noneMatch(cp -> cp >= 0x6f0 && cp <= 0x6f9);
    }

    // A9 https://www.rfc-editor.org/rfc/rfc5892#appendix-A
    private static boolean extendedArabicIndicDigits(String label, int pos) {
        return label.codePoints()
                .noneMatch(cp -> cp >= 0x660 && cp <= 0x669);
    }
}
