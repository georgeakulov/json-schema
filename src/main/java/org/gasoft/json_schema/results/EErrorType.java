package org.gasoft.json_schema.results;

public enum EErrorType {

    CONST("Value {0} not equals to const {1}"),
    CONTAINS_MIN("The array must contains at least {0} valid items. Actual: {1}"),
    CONTAINS_MAX("The array can contains no more than {0} valid items. Actual: {1}"),
    DEPENDENCIES("The dependencies for [{0}] are not satisfied"),
    DEPENDENT_REQUIRED("The required dependencies for [{0}] are not satisfied"),
    ENUM("The value {0} not declared in enum {1}"),
    EXCLUSIVE_MAXIMUM("Value {0} greater than {1}"),
    EXCLUSIVE_MINIMUM("Value {0} less or equal than {1}"),
    FORMAT("Value {0} not conform to format {1}"),
    MAXIMUM("Value {0} greater than {1}"),
    MINIMUM("Value {0} less than {1}"),
    MAX_ITEMS("Required max array size {0}, Actual size: {1}"),
    MIN_ITEMS("Required minimum items amount is {0}, Actual size: {1}"),
    MAX_PROPERTIES("Maximum allowed properties count {0}. Actual: {1}"),
    MIN_PROPERTIES("Require min properties count {0}. Actual: {1}"),
    MULTIPLE_OF("The node value of {0} not conform to multipleOf value {1}"),
    NOT("The subschema validation was successfully. Result will be inverter"),
    ONE_OF_EMPTY("None of the results were successful."),
    ONE_OF_MORE_THAN_ONE("More than one successful results."),
    ANY_OF("None of the variants were successful."),
    PATTERN("The value {0} not conform to pattern: {1}"),
    REQUIRED("Some required properties {0} are missing"),
    FALSE_SCHEMA("Because schema is false"),
    TYPE("The node value {0} not conform to type {1}"),
    UNIQUE_ITEMS("At least one item {0} not unique"),
    MAX_LENGTH("The length of {0} must be less than or equal to {1}. Actual: {2}"),
    MIN_LENGTH("The length of {0} must be greater or equal than  {1}. Actual: {2}")

    ;
    private final String defaultErrorMsg;

    EErrorType(String s) {
        defaultErrorMsg = s;
    }

    public String getDefaultErrorMsg() {
        return defaultErrorMsg;
    }
}
