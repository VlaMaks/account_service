package account.validation;



import account.entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.function.Function;

public interface UserPasswordValidator extends Function<String, UserPasswordValidator.ValidationResult> {
    List<String> arr = List.of("PasswordForJanuary", "PasswordForFebruary", "PasswordForMarch", "PasswordForApril",
            "PasswordForMay", "PasswordForJune", "PasswordForJuly", "PasswordForAugust",
            "PasswordForSeptember", "PasswordForOctober", "PasswordForNovember", "PasswordForDecember");

    static UserPasswordValidator isLengthValid() {
        return pass -> pass.length() < 12 ?  ValidationResult.LENGTH_NOT_VALID : ValidationResult.SUCCESS;
    }

    static UserPasswordValidator isNotCompr() {
        return pass ->  arr.contains(pass) ? ValidationResult.PASSWORD_COMPROMISED : ValidationResult.SUCCESS;
    }

    static UserPasswordValidator isUniq(User user, PasswordEncoder encoder) {
        return pass ->  encoder.matches(pass, user.getPassword()) ? ValidationResult.PASSWORD_NOT_UNIQ : ValidationResult.SUCCESS;
    }

    default UserPasswordValidator and (UserPasswordValidator other) {
        return pass -> {
            ValidationResult result = this.apply(pass);
            return  result.equals(ValidationResult.SUCCESS) ? other.apply(pass) : result;
        };
    }

    enum ValidationResult {
        SUCCESS,
        LENGTH_NOT_VALID,
        PASSWORD_NOT_UNIQ,
        PASSWORD_COMPROMISED
    }
}
