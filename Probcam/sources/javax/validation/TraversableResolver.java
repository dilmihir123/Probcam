package javax.validation;

import java.lang.annotation.ElementType;
import javax.validation.Path.Node;

public interface TraversableResolver {
    boolean isCascadable(Object obj, Node node, Class<?> cls, Path path, ElementType elementType);

    boolean isReachable(Object obj, Node node, Class<?> cls, Path path, ElementType elementType);
}
