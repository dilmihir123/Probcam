package javax.validation.spi;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;

public interface ConfigurationState {
    ConstraintValidatorFactory getConstraintValidatorFactory();

    Set<InputStream> getMappingStreams();

    MessageInterpolator getMessageInterpolator();

    Map<String, String> getProperties();

    TraversableResolver getTraversableResolver();

    boolean isIgnoreXmlConfiguration();
}
