package tn.vermeg.gestionproduit.util;

public final class ValidationUtils {

    private ValidationUtils() {
    }

    public static void requireNonBlank(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
