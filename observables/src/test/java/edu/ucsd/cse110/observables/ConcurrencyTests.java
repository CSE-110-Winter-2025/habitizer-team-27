package edu.ucsd.cse110.observables;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class ConcurrencyTests {
    @Test
    void testNoConcurrentAccessModification() {
        var subject = new PlainMutableSubject<Integer>();

//        Observer<Integer> observer = () -> {
//            subject.removeObserver(observer);
//        };
//
//        subject.observe();

        assertDoesNotThrow(() -> subject.setValue(1));
    }
}
