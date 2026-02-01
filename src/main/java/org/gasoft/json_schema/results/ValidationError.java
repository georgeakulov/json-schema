package org.gasoft.json_schema.results;

import org.gasoft.json_schema.results.IValidationResult.IValidationResultError;

import java.text.MessageFormat;

public class ValidationError extends AbstractValidationResult implements IValidationResultError {

    private final EErrorType errorType;
    private final Object[]  args;

    public static ValidationError create(IValidationId validationId, EErrorType errorType) {
        return new ValidationError(validationId, errorType, null);
    }

    public static ValidationError create(IValidationId validationId, EErrorType errorType, Object ... args) {
        return new ValidationError(validationId, errorType, args);
    }

    private ValidationError(IValidationId validationId, EErrorType errorType, Object[] args) {
        super(validationId);
        this.errorType = errorType;
        this.args = args;
    }

    @Override
    public String getError() {
        if(args == null) {
            return errorType.getDefaultErrorMsg();
        }
        return MessageFormat.format(errorType.getDefaultErrorMsg(), args);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ValidationError{");
        sb.append("id=").append(getId());
        sb.append(", msg=").append(getError());
        sb.append('}');
        return sb.toString();
    }
}
