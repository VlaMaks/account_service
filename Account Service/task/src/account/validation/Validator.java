package account.validation;

import account.entity.User;

public class Validator {

    private static boolean isNameValid(User user) {
        return user.getName() != null && !user.getName().isEmpty();
    }

    private static boolean isLastNameValid(User user) {
        return user.getLastName() != null && !user.getLastName().isEmpty();
    }

    private static boolean isEmailValid(User user) {
        return user.getEmail() != null && !user.getEmail().isEmpty() && user.getEmail().contains("@acme.com");
    }

    private static boolean isPasswordValid(User user) {
        return user.getPassword() != null && !user.getPassword().isEmpty();
    }

    public static boolean isUserValid(User user) {
        return isEmailValid(user) && isNameValid(user) && isLastNameValid(user) && isPasswordValid(user);
    }







}
