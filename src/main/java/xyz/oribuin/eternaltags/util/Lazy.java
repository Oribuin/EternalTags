package xyz.oribuin.eternaltags.util;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Supplier;

public class Lazy<T> implements Supplier<T> {

    private final Supplier<T> lazyLoader;
    private T value;

    public Lazy(@NotNull Supplier<T> lazyLoader) {
        Objects.requireNonNull(lazyLoader, "lazyLoader cannot be null");
        this.lazyLoader = lazyLoader;
    }

    @Override
    public @NotNull T get() {
        if (this.value == null)
            this.value = Objects.requireNonNull(this.lazyLoader.get(), "Lazy value cannot be null");
        return this.value;
    }

}
