package account.entity;

public enum Permission {
    CHANGE_PASS("change:pass"),
    READ_PAYMENT("read:payment"),
    WRITE_PAYMENT("write:payment"),
    MANAGE_USER("manage:user"),
    READ_SECURITY_EVENTS("read:security:events");

    private final String permission;

    Permission(String permission) {
        this.permission = permission;
    }

    public String getPermission() {
        return permission;
    }
}
