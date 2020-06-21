package javax.validation;

import java.util.Set;

public class ConstraintViolationException extends ValidationException {
    private final Set<ConstraintViolation<?>> constraintViolations;

    public ConstraintViolationException(String message, Set<ConstraintViolation<?>> constraintViolations2) {
        super(message);
        this.constraintViolations = constraintViolations2;
    }

    public ConstraintViolationException(Set<ConstraintViolation<?>> constraintViolations2) {
        this.constraintViolations = constraintViolations2;
    }

    public Set<ConstraintViolation<?>> getConstraintViolations() {
        return this.constraintViolations;
    }
}
