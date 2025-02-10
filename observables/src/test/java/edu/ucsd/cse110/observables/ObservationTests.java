package edu.ucsd.cse110.observables;

import androidx.annotation.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("Subject Observation")
public class ObservationTests {
    private final String UPDATE_VALUE = "Updated!";

    @Nested
    @DisplayName("Given a subject")
    class GivenSubject {
        MutableSubject<String> subject = new PlainMutableSubject<>();

        @Nested
        @DisplayName("When an observer is registered")
        class WhenUpdated {
            @Test
            @DisplayName("Then it is called with a value immediately if the subject is initialized")
            void ThenReceivesCurrentValueInitialized() {
                subject.setValue(UPDATE_VALUE);
                var latch = new CountDownLatch(1);
                subject.observe(value -> {
                    assertThat(value, is(UPDATE_VALUE));
                    latch.countDown();
                });
            }

            @Test
            @DisplayName("Then it is not called if the subject is not initialized")
            void ThenNotReceivesUnInitialized() {
                assertThat(subject.isInitialized(), is(false));
                subject.observe(value -> fail());
            }

            @Test
            @DisplayName("Then it receives the current value immediately")
            void ThenReceivesCurrentValue() {
                subject.setValue(UPDATE_VALUE);
                var latch = new CountDownLatch(1);
                subject.observe(value -> {
                    assertThat(value, is(UPDATE_VALUE));
                    latch.countDown();
                });
            }

            @Test
            @DisplayName("Then it has observers")
            void ThenHasObservers() {
                subject.observe(value -> {
                });
                assertThat(subject.hasObservers(), is(true));
            }

            @Test
            @DisplayName("Then setValue notifies a single observer")
            void ThenSingleObserverIsNotified() {
                var latch = new CountDownLatch(1);

                subject.observe(value -> {
                    assertThat(value, is(UPDATE_VALUE));
                    latch.countDown();
                });

                assertDoesNotThrow(() -> {
                    subject.setValue(UPDATE_VALUE);
                    // Latch waits up to 1s for countdown to hit 0, or throws an exception.
                    latch.await(1, TimeUnit.SECONDS);
                }, "Timed out awaiting observer notification");
            }

            @Test
            @DisplayName("Then setValue notifies multiple observers")
            void ThenMultipleObserversAreAllNotified() {
                var latch = new CountDownLatch(2);

                // Equivalent Syntax #1: anonymous interface implementation instance
                //noinspection Convert2Lambda
                Observer<String> observer1 = new Observer<>() {
                    @Override
                    public void onChanged(@Nullable String value) {
                        assertThat(value, is(UPDATE_VALUE));
                        latch.countDown();
                    }
                };

                // Equivalent Syntax #2: lambda syntax
                Observer<String> observer2 = value -> {
                    assertThat(value, is(UPDATE_VALUE));
                    latch.countDown();
                };

                subject.observe(observer1);
                subject.observe(observer2);

                // *Both* observers must count down to hit 0!
                assertDoesNotThrow(() -> {
                    subject.setValue(UPDATE_VALUE);
                    latch.await(1, TimeUnit.SECONDS);
                }, "Timed out awaiting both observer notifications");
            }

            @Test
            @DisplayName("Then registering it again does not register it twice")
            void ThenRegisteringAgainDoesNothing() {
                var observer = subject.observe(value -> {
                });
                subject.observe(observer);

                assertThat(subject.hasObservers(), is(true));
                assertThat(subject.getObservers(), hasSize(1));
            }
        }

        @Nested
        @DisplayName("When it is not observed")
        class WhenNotUpdated {
            @Test
            @DisplayName("Then it has no observers")
            void ThenHasNoObservers() {
                assertThat(subject.hasObservers(), is(false));
                assertThat(subject.getObservers(), hasSize(0));
            }
        }

        @Nested
        @DisplayName("When an observer is unregistered")
        class WhenUnregistered {
            @Test
            @DisplayName("Then it has no observers")
            void ThenHasNoObservers() {
                var observer = subject.observe(value -> {
                });
                subject.removeObserver(observer);

                assertThat(subject.hasObservers(), is(false));
                assertThat(subject.getObservers(), hasSize(0));
            }
        }

        @Nested
        @DisplayName("When all observers are unregistered")
        class WhenAllUnregistered {
            @Test
            @DisplayName("Then it has no observers")
            void ThenHasNoObservers() {
                subject.observe(value -> {
                });
                subject.observe(value -> {
                });
                subject.observe(value -> {
                });
                subject.removeObservers();

                assertThat(subject.hasObservers(), is(false));
                assertThat(subject.getObservers(), hasSize(0));
            }
        }
    }
}
