package edu.ucsd.cse110.observables;

import androidx.annotation.VisibleForTesting;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

public class PlainMediatorSubject<T> extends PlainMutableSubject<T> implements MediatorSubject<T> {

    private final Queue<Source<?>> sources = new ConcurrentLinkedQueue<>();

    @Override
    public <S> void addSource(Subject<S> subject, Observer<S> observer) {
        var source = new Source<>(subject, observer);
        if (sources.contains(source)) return;
        sources.add(source);
        source.startObserving();
    }

    @Override
    public <S> void removeSource(Subject<S> subject) {
        sources.stream().filter(s -> s.subject.equals(subject)).forEach(Source::stopObserving);
        sources.removeIf(s -> s.subject.equals(subject));
    }

    /**
     * Get a list of source subjects (with repetitions!). This method is for testing purposes only.
     *
     * @return The list of sources.
     */
    @Override
    @VisibleForTesting
    public List<? extends Subject<?>> getSourceSubjects() {
        return sources.stream().map(s -> s.subject).toList();
    }

    // Container record for registered sources (subject + observer)
    private record Source<S>(Subject<S> subject, Observer<S> observer) {
        private void startObserving() {
            subject.observe(observer);
        }

        private void stopObserving() {
            subject.removeObserver(observer);
        }
    }
}
