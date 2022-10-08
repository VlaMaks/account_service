package account.entity;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Set;
import java.util.stream.Collectors;

public enum Role {
    ROLE_USER(Set.of(Permission.CHANGE_PASS, Permission.READ_PAYMENT)),
    ROLE_ADMINISTRATOR(Set.of(Permission.CHANGE_PASS, Permission.MANAGE_USER)),
    ROLE_ACCOUNTANT(Set.of(Permission.CHANGE_PASS, Permission.READ_PAYMENT, Permission.WRITE_PAYMENT));

    private final Set<Permission> permissions;

    Role(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public Set<SimpleGrantedAuthority> getAuthorities() {
        return getPermissions().stream()
                .map(permission -> new SimpleGrantedAuthority(permission.getPermission()))
                .collect(Collectors.toSet());
    }
}
