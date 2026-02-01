package org.gasoft.json_schema.common;

import org.gasoft.json_schema.results.IValidationResult.ISchemaLocator;

import java.text.MessageFormat;

public class LocatedSchemaCompileException extends SchemaCompileException {

    private final ISchemaLocator locator;

    private LocatedSchemaCompileException(ISchemaLocator locator, String message) {
        super(message);
        this.locator = locator;
    }

    public ISchemaLocator getLocator() {
        return locator;
    }

    private LocatedSchemaCompileException(ISchemaLocator locator, String message, Throwable cause) {
        super(message, cause);
        this.locator = locator;
    }

    @Override
    public String getMessage() {
        return locator + ": " + super.getMessage();
    }

    public static LocatedSchemaCompileException create(ISchemaLocator locator, Throwable cause, String message, Object ... args) {
        return new LocatedSchemaCompileException(locator, MessageFormat.format(message, args), cause);
    }

    public static LocatedSchemaCompileException create(ISchemaLocator locator, String message, Object ... args) {
        return new LocatedSchemaCompileException(locator, MessageFormat.format(message, args));
    }

    public static void checkIt(boolean value, ISchemaLocator locator, String msg, Object... args) {
        if(!value) {
            throw new LocatedSchemaCompileException(locator, MessageFormat.format(msg, args));
        }
    }
}
