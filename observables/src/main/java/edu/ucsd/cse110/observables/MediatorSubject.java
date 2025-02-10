package edu.ucsd.cse110.observables;

import androidx.annotation.MainThread;
import androidx.annotation.VisibleForTesting;

import java.util.List;

/**
 * Interface for mediator subjects. This is an advanced subject that observes other subjects,
 * which you will likely not ever need to use directly. It's used to implement some utility
 * methods.
 *
 * @param <T> The type of the value that the subject holds.
 */
public interface MediatorSubject<T> extends MutableSubject<T> {
    /**
     * Register a source subject to be observed.
     * <p>
     *
     * @param subject  The source subject to observe.
     * @param observer The observer to notify when the source subject changes.
     * @param <S>      The type of the subject subject.
     */
    @MainThread
    <S> void addSource(Subject<S> subject, Observer<S> observer);


    /**
     * Unregister a source subject that was previously registered.
     *
     * @param source The source subject to stop observing.
     * @param <S>    The type of the source subject.
     */
    @MainThread
    <S> void removeSource(Subject<S> source);

    /**
     * Get a list of source subjects (with repetitions!). This method is for testing purposes only.
     *
     * @return The list of sources.
     */
    @VisibleForTesting
    List<? extends Subject<?>> getSourceSubjects();
}