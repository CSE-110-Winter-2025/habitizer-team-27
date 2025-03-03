package edu.ucsd.cse110.habitizer.app.MS2Tests;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;

import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anything;
import static org.junit.Assert.assertTrue;

import androidx.test.espresso.Espresso;

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ucsd.cse110.habitizer.app.MainActivity;
import edu.ucsd.cse110.habitizer.app.R;
import edu.ucsd.cse110.habitizer.app.data.db.AppDatabase;
import edu.ucsd.cse110.habitizer.app.data.db.HabitizerRepository;
import edu.ucsd.cse110.habitizer.lib.domain.Routine;
import edu.ucsd.cse110.habitizer.lib.domain.Task;

@RunWith(AndroidJUnit4.class)

// Tests adding a routine (US1)
public class AddRoutineTests {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    // Helper function to get to last item

    // Tests adding a new routine by clicking the "Add Routine" button
    @Test
    public void testAddRoutine() {
//        HabitizerRepository repository = new HabitizerRepository();
//        repository.getRoutinesAsLiveData().getValue().size();
        // Clicks "Add Routine" button
        Espresso.onView(withText("Add Routine"))
                .perform(click());
        Espresso.onIdle();


        // Check that new routine with default name is at bottom
        // This will only work if the database is the default??
        onData(anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(2)
                .onChildView(withId(R.id.routine_name))
                .check(matches(withText("New Routine")));
    }

}

