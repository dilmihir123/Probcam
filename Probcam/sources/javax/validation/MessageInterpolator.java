package javax.validation;

import java.util.Locale;
import javax.validation.metadata.ConstraintDescriptor;

public interface MessageInterpolator {

    public interface Context {
        ConstraintDescriptor<?> getConstraintDescriptor();

        Object getValidatedValue();
    }

    String interpolate(String str, Context context);

    String interpolate(String str, Context context, Locale locale);
}
