package javax.validation.bootstrap;

import javax.validation.Configuration;
import javax.validation.ValidationProviderResolver;

public interface GenericBootstrap {
    Configuration<?> configure();

    GenericBootstrap providerResolver(ValidationProviderResolver validationProviderResolver);
}
