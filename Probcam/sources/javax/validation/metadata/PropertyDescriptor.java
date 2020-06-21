package javax.validation.metadata;

public interface PropertyDescriptor extends ElementDescriptor {
    String getPropertyName();

    boolean isCascaded();
}
