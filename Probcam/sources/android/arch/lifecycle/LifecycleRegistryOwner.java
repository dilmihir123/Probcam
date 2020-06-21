package android.arch.lifecycle;

public interface LifecycleRegistryOwner extends LifecycleOwner {
    LifecycleRegistry getLifecycle();
}
