package javax.validation;

public interface ValidatorFactory {
    ConstraintValidatorFactory getConstraintValidatorFactory();

    MessageInterpolator getMessageInterpolator();

    TraversableResolver getTraversableResolver();

    Validator getValidator();

    <T> T unwrap(Class<T> cls);

    ValidatorContext usingContext();
}
