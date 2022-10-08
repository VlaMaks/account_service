package account.exception;

import account.validation.UserPasswordValidator;

public class PasswordExceptionReason {

    public static String getPasswordExceptionReason(UserPasswordValidator.ValidationResult result) {
        return switch (result) {
            case PASSWORD_COMPROMISED -> "The password is in the hacker's database!";
            case LENGTH_NOT_VALID -> "Password length must be 12 chars minimum!";
            case PASSWORD_NOT_UNIQ -> "The passwords must be different!";
            default -> "";
        };
    }
}
