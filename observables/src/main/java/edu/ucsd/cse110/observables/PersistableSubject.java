package edu.ucsd.cse110.observables;

import androidx.annotation.Nullable;

public interface PersistableSubject<T> {
    /**
     * @return The current value, or null if {@link #isInitialized()} is false.
     */
    @Nullable
    T getValue();

    /**
     * @return True if this subject has been initialized with an explicit value.
     */
    boolean isInitialized();
}
