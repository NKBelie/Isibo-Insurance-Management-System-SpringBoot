package user.domain;

import exception.ValidationException;

public enum SystemRole {
    ADMINISTRATOR,
    INSURANCE_OFFICER,
    CUSTOMER;

    public static SystemRole from(String role) {
        if (role == null || role.isBlank()) {
            throw new ValidationException("A valid user role is required.");
        }

        return switch (role.trim().toUpperCase().replace(' ', '_').replace('-', '_')) {
            case "ADMIN", "ADMINISTRATOR" -> ADMINISTRATOR;
            case "OFFICER", "INSURANCE_OFFICER" -> INSURANCE_OFFICER;
            case "CUSTOMER" -> CUSTOMER;
            default -> throw new ValidationException("Role must be Administrator, Insurance Officer, or Customer.");
        };
    }

    public String authority() {
        return "ROLE_" + name();
    }

    public String label() {
        return switch (this) {
            case ADMINISTRATOR -> "Administrator";
            case INSURANCE_OFFICER -> "Insurance Officer";
            case CUSTOMER -> "Customer";
        };
    }
}
