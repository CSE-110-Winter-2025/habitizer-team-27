package edu.ucsd.cse110.habitizer.app;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import edu.ucsd.cse110.habitizer.app.databinding.ActivityMainBinding;
import edu.ucsd.cse110.habitizer.app.ui.homescreen.HomeScreenFragment;
import edu.ucsd.cse110.habitizer.app.ui.routine.RoutineFragment;
import edu.ucsd.cse110.habitizer.lib.data.InMemoryDataSource;
import edu.ucsd.cse110.habitizer.lib.domain.RoutineRepository;
import edu.ucsd.cse110.habitizer.lib.domain.TaskRepository;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding view;
    private boolean isShowingRoutine = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.app_name);

        this.view = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(view.getRoot());

        // Initialize with home screen fragment first
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, HomeScreenFragment.newInstance())
                .commit();
    }

    private void swapFragments() {
        if (isShowingRoutine) {
            int defaultRoutineId = 1;
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, RoutineFragment.newInstance(defaultRoutineId))
                    .commit();
        } else {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, HomeScreenFragment.newInstance())
                    .commit();
        }
        isShowingRoutine = !isShowingRoutine;
    }

}