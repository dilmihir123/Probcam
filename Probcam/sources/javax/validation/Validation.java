package javax.validation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.validation.bootstrap.GenericBootstrap;
import javax.validation.bootstrap.ProviderSpecificBootstrap;
import javax.validation.spi.BootstrapState;
import javax.validation.spi.ValidationProvider;

public class Validation {

    private static class DefaultValidationProviderResolver implements ValidationProviderResolver {
        private static final String SERVICES_FILE = ("META-INF/services/" + ValidationProvider.class.getName());
        private static final Map<ClassLoader, List<ValidationProvider<?>>> providersPerClassloader = new WeakHashMap();

        private DefaultValidationProviderResolver() {
        }

        public List<ValidationProvider<?>> getValidationProviders() {
            List<ValidationProvider<?>> providers;
            InputStream stream;
            ClassLoader classloader = GetClassLoader.fromContext();
            if (classloader == null) {
                classloader = GetClassLoader.fromClass(DefaultValidationProviderResolver.class);
            }
            synchronized (providersPerClassloader) {
                providers = (List) providersPerClassloader.get(classloader);
            }
            if (providers == null) {
                providers = new ArrayList<>();
                String name = null;
                try {
                    Enumeration<URL> providerDefinitions = classloader.getResources(SERVICES_FILE);
                    while (providerDefinitions.hasMoreElements()) {
                        stream = ((URL) providerDefinitions.nextElement()).openStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(stream), 100);
                        name = reader.readLine();
                        while (name != null) {
                            String name2 = name.trim();
                            if (!name2.startsWith("#")) {
                                providers.add((ValidationProvider) loadClass(name2, DefaultValidationProviderResolver.class).newInstance());
                            }
                            name = reader.readLine();
                        }
                        stream.close();
                    }
                    synchronized (providersPerClassloader) {
                        providersPerClassloader.put(classloader, providers);
                    }
                } catch (IOException e) {
                    throw new ValidationException("Unable to read " + SERVICES_FILE, e);
                } catch (ClassNotFoundException e2) {
                    throw new ValidationException("Unable to load Bean Validation provider " + name, e2);
                } catch (IllegalAccessException e3) {
                    throw new ValidationException("Unable to instanciate Bean Validation provider" + name, e3);
                } catch (InstantiationException e4) {
                    throw new ValidationException("Unable to instanciate Bean Validation provider" + name, e4);
                } catch (Throwable th) {
                    stream.close();
                    throw th;
                }
            }
            return providers;
        }

        private static Class<?> loadClass(String name, Class<?> caller) throws ClassNotFoundException {
            try {
                ClassLoader loader = GetClassLoader.fromContext();
                if (loader != null) {
                    return loader.loadClass(name);
                }
            } catch (ClassNotFoundException e) {
                if (caller == null) {
                    throw e;
                }
            }
            return Class.forName(name, true, GetClassLoader.fromClass(caller));
        }
    }

    private static class GenericBootstrapImpl implements GenericBootstrap, BootstrapState {
        private ValidationProviderResolver defaultResolver;
        private ValidationProviderResolver resolver;

        private GenericBootstrapImpl() {
        }

        public GenericBootstrap providerResolver(ValidationProviderResolver resolver2) {
            this.resolver = resolver2;
            return this;
        }

        public ValidationProviderResolver getValidationProviderResolver() {
            return this.resolver;
        }

        public ValidationProviderResolver getDefaultValidationProviderResolver() {
            if (this.defaultResolver == null) {
                this.defaultResolver = new DefaultValidationProviderResolver();
            }
            return this.defaultResolver;
        }

        public Configuration<?> configure() {
            ValidationProviderResolver resolver2 = this.resolver == null ? getDefaultValidationProviderResolver() : this.resolver;
            try {
                if (resolver2.getValidationProviders().size() == 0) {
                    throw new ValidationException("Unable to find a default provider");
                }
                try {
                    return ((ValidationProvider) resolver2.getValidationProviders().get(0)).createGenericConfiguration(this);
                } catch (RuntimeException re) {
                    throw new ValidationException("Unable to instantiate Configuration.", re);
                }
            } catch (RuntimeException re2) {
                throw new ValidationException("Unable to get available provider resolvers.", re2);
            }
        }
    }

    private static class GetClassLoader implements PrivilegedAction<ClassLoader> {
        private final Class<?> clazz;

        public static ClassLoader fromContext() {
            GetClassLoader action = new GetClassLoader(null);
            if (System.getSecurityManager() != null) {
                return (ClassLoader) AccessController.doPrivileged(action);
            }
            return action.run();
        }

        public static ClassLoader fromClass(Class<?> clazz2) {
            if (clazz2 == null) {
                throw new IllegalArgumentException("Class is null");
            }
            GetClassLoader action = new GetClassLoader(clazz2);
            if (System.getSecurityManager() != null) {
                return (ClassLoader) AccessController.doPrivileged(action);
            }
            return action.run();
        }

        private GetClassLoader(Class<?> clazz2) {
            this.clazz = clazz2;
        }

        public ClassLoader run() {
            if (this.clazz != null) {
                return this.clazz.getClassLoader();
            }
            return Thread.currentThread().getContextClassLoader();
        }
    }

    private static class ProviderSpecificBootstrapImpl<T extends Configuration<T>, U extends ValidationProvider<T>> implements ProviderSpecificBootstrap<T> {
        private ValidationProviderResolver resolver;
        private final Class<U> validationProviderClass;

        public ProviderSpecificBootstrapImpl(Class<U> validationProviderClass2) {
            this.validationProviderClass = validationProviderClass2;
        }

        public ProviderSpecificBootstrap<T> providerResolver(ValidationProviderResolver resolver2) {
            this.resolver = resolver2;
            return this;
        }

        public T configure() {
            if (this.validationProviderClass == null) {
                throw new ValidationException("builder is mandatory. Use Validation.byDefaultProvider() to use the generic provider discovery mechanism");
            }
            GenericBootstrapImpl state = new GenericBootstrapImpl();
            if (this.resolver == null) {
                this.resolver = state.getDefaultValidationProviderResolver();
            } else {
                state.providerResolver(this.resolver);
            }
            try {
                for (ValidationProvider provider : this.resolver.getValidationProviders()) {
                    if (this.validationProviderClass.isAssignableFrom(provider.getClass())) {
                        return ((ValidationProvider) this.validationProviderClass.cast(provider)).createSpecializedConfiguration(state);
                    }
                }
                throw new ValidationException("Unable to find provider: " + this.validationProviderClass);
            } catch (RuntimeException re) {
                throw new ValidationException("Unable to get available provider resolvers.", re);
            }
        }
    }

    public static ValidatorFactory buildDefaultValidatorFactory() {
        return byDefaultProvider().configure().buildValidatorFactory();
    }

    public static GenericBootstrap byDefaultProvider() {
        return new GenericBootstrapImpl();
    }

    public static <T extends Configuration<T>, U extends ValidationProvider<T>> ProviderSpecificBootstrap<T> byProvider(Class<U> providerType) {
        return new ProviderSpecificBootstrapImpl(providerType);
    }
}
