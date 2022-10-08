package account.exception;

import account.validation.EmplPayrollsValidator;


public class EmplSalExceptionReason {

    public static String getEmplSalExceptionReason(EmplPayrollsValidator.ValidationResult result) {

        return switch (result) {
            case PERIOD_NOT_UNIQ -> "Period for the employee must be unique; ";
            case EMPL_NOT_USER   -> "Employee is not a user; ";
            case SALARY_NEGATIVE -> "Salary should not be negative; ";
            case PERIOD_NOT_CORRECT -> "Period is not correct; ";
            default -> "";
        };
    }
}
