package org.gasoft.json_schema.common;

import org.jspecify.annotations.NonNull;

import java.text.MessageFormat;

public class SchemaCompileException extends RuntimeException {

    protected SchemaCompileException(String message) {
        super(message);
    }

    protected SchemaCompileException(String message, Throwable cause) {
        super(message, cause);
    }

    public static SchemaCompileException create(String msg, Object ... args) {
        return new SchemaCompileException(MessageFormat.format(msg, args));
    }


    public static SchemaCompileException create(Throwable thr, String msg, Object ... args) {
        return new SchemaCompileException(MessageFormat.format(msg, args), thr);
    }

    public static void checkIt(boolean value, String msg, Object ... args) {
        if(!value) {
            throw new SchemaCompileException(MessageFormat.format(msg, args));
        }
    }

    public static <T> @NonNull T checkNonNull(T obj, String msg, Object ... args) {
        if(obj == null) {
            throw new SchemaCompileException(MessageFormat.format(msg, args));
        }
        return obj;
    }
}
