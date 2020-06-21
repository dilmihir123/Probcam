package javax.validation.spi;

import javax.validation.ValidationProviderResolver;

public interface BootstrapState {
    ValidationProviderResolver getDefaultValidationProviderResolver();

    ValidationProviderResolver getValidationProviderResolver();
}
