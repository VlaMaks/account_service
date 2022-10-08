package account.validation;

import account.entity.EmpSal;
import account.service.EmplService;
import account.service.UserService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public interface EmplPayrollsValidator extends Function<EmpSal, EmplPayrollsValidator.ValidationResult> {


    private static boolean isMonthCorrect(int month) {
        return month > 0 && month < 13;
    }

    private static int strMonthConvertToInt(String period) {
        String numberMonth = period.split("-")[0];
        if ("0".equals(numberMonth.charAt(0))) {
            numberMonth = String.valueOf(numberMonth.charAt(1));
        }

        int month = Integer.parseInt(numberMonth);

        if (isMonthCorrect(month)) {
            return month;
        }
        return -1;
    }
    static EmplPayrollsValidator isEmplUser(UserService userService) {
        return empSal -> userService.findUserByEmail(empSal.getEmployee()).isPresent() ? ValidationResult.SUCCESS :
                    ValidationResult.EMPL_NOT_USER;
    }

    static EmplPayrollsValidator isPeriodCorrect() {
        return empSal -> strMonthConvertToInt(empSal.getPeriod()) != -1 ? ValidationResult.SUCCESS :
                ValidationResult.PERIOD_NOT_CORRECT;
    }
    static EmplPayrollsValidator isSalaryNotNegative() {
        return empSal -> empSal.getSalary() > -1 ? ValidationResult.SUCCESS :
                ValidationResult.SALARY_NEGATIVE;
    }

    static EmplPayrollsValidator isPeriodUniq(Map<String, String> emplPatrolls, EmplService emplService) {
        return empSal -> emplPatrolls.get(empSal.getEmployee()) != null && emplPatrolls.get(empSal.getEmployee()) == empSal.getPeriod()
                ? ValidationResult.PERIOD_NOT_UNIQ :
                emplService.findByEmailAndPeriod(empSal.getEmployee(), empSal.getPeriod()).isEmpty() ?
                        ValidationResult.SUCCESS : ValidationResult.PERIOD_NOT_UNIQ;
    }

    default EmplPayrollsValidator and (EmplPayrollsValidator other) {
        return empSal -> {
            EmplPayrollsValidator .ValidationResult result = this.apply(empSal);
            return  result.equals(EmplPayrollsValidator .ValidationResult.SUCCESS) ? other.apply(empSal) : result;
        };
    }


    enum ValidationResult {
        SUCCESS,
        EMPL_NOT_USER,
        PERIOD_NOT_UNIQ,
        PERIOD_NOT_CORRECT,
        SALARY_NEGATIVE
    }
}
