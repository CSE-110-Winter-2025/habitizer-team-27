
package edu.ucsd.cse110.habitizer.app.ui.routine;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.time.LocalDateTime;
import java.util.ArrayList;

import edu.ucsd.cse110.habitizer.app.HabitizerApplication;
import edu.ucsd.cse110.habitizer.app.MainViewModel;
import edu.ucsd.cse110.habitizer.app.R;
import edu.ucsd.cse110.habitizer.app.databinding.FragmentRoutineScreenBinding;
import edu.ucsd.cse110.habitizer.app.ui.dialog.CreateTaskDialogFragment;
import edu.ucsd.cse110.habitizer.app.ui.dialog.SetRoutineTimeDialogFragment;
import edu.ucsd.cse110.habitizer.lib.domain.Routine;
import edu.ucsd.cse110.habitizer.lib.domain.Task;

public class RoutineFragment extends Fragment {
    private MainViewModel activityModel;
    private FragmentRoutineScreenBinding binding;
    private ArrayAdapter<Task> taskAdapter;

    private static final String ARG_ROUTINE_ID = "routine_id";
    private Routine currentRoutine;

    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private boolean isTimerRunning = true;
    private static final int UPDATE_INTERVAL_MS = 1000;

    public RoutineFragment() {
        // required empty public constructor
    }

    private void initTimerUpdates() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                updateTimeDisplay();

                timerHandler.postDelayed(this, UPDATE_INTERVAL_MS);
            }
        };

        // Start
        timerHandler.post(timerRunnable);
    }

    public static RoutineFragment newInstance(int routineId) {
        RoutineFragment fragment = new RoutineFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_ROUTINE_ID, routineId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        var modelOwner = requireActivity();
        var modelFactory = ViewModelProvider.Factory.from(MainViewModel.initializer);
        var modelProvider = new ViewModelProvider(modelOwner, modelFactory);
        this.activityModel = modelProvider.get(MainViewModel.class);

        int routineId = getArguments().getInt(ARG_ROUTINE_ID);
        isTimerRunning = true;

        // Get routine
        this.currentRoutine = activityModel.getRoutineRepository().getRoutine(routineId);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRoutineScreenBinding.inflate(inflater, container, false);

        initTimerUpdates();
        isTimerRunning = true;

        // Clear "completed" statuses of all tasks
        for (Task task : currentRoutine.getTasks()) {
            task.reset();
        }

        // Reset both timers

        // Initialize ListView and Adapter
        ListView taskListView = binding.routineList;
        taskAdapter = new TaskAdapter(
                requireContext(),
                R.layout.task_page,
                new ArrayList<>(),
                currentRoutine,
                ((HabitizerApplication) requireContext().getApplicationContext()).getDataSource()
        );
        taskListView.setAdapter(taskAdapter);

        // Observe task data
        activityModel.getRoutineRepository().find(currentRoutine.getRoutineId())
                .observe(routine -> {
                    taskAdapter.clear();
                    assert routine != null;
                    taskAdapter.addAll(routine.getTasks());
                    taskAdapter.notifyDataSetChanged();
                });

        binding.routineNameTask.setText(currentRoutine.getRoutineName());
        updateRoutineGoalDisplay(currentRoutine.getGoalTime());

        binding.addTaskButton.setOnClickListener(v -> {
            CreateTaskDialogFragment dialog = CreateTaskDialogFragment.newInstance(this::addTaskToRoutine);
            dialog.show(getParentFragmentManager(), "CreateTaskDialog");
        });

        binding.expectedTime.setOnClickListener(v -> {
            SetRoutineTimeDialogFragment dialog = SetRoutineTimeDialogFragment.newInstance(this::updateRoutineGoalDisplay);
            dialog.show(getParentFragmentManager(), "SetTimeDialog");
        });

        binding.endRoutineButton.setOnClickListener(v -> {
            isTimerRunning = false;
            currentRoutine.endRoutine(LocalDateTime.now());
            updateTimeDisplay();
            binding.endRoutineButton.setEnabled(false);
            binding.stopTimerButton.setEnabled(false);
        });

        binding.stopTimerButton.setOnClickListener(v -> {
            if (currentRoutine.isActive()) {
                // Pause at current simulated time
                currentRoutine.pauseTime(LocalDateTime.now());
                updateTimeDisplay();
            }
            binding.stopTimerButton.setEnabled(false);
            isTimerRunning = false;
        });

        binding.homeButton.setOnClickListener(v -> {
            // Navigate back to HomeScreenFragment
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });

        // Initial state setup
        binding.homeButton.setEnabled(false);

        binding.fastForwardButton.setOnClickListener(v -> {
            // Fast forward 30 seconds
            currentRoutine.fastForwardTime();

            // Force immediate UI update
            updateTimeDisplay();

            // If routine completed via FF, update state
            if (currentRoutine.autoCompleteRoutine()) {
                binding.endRoutineButton.setEnabled(false);
            }
        });

        return binding.getRoot();
    }

    private void addTaskToRoutine(String taskName) {
        if (taskName == null || taskName.trim().isEmpty()) return;

        // Create task with auto-increment ID
        int newTaskId = currentRoutine.getTasks().size() + 1;
        Task newTask = new Task(newTaskId, taskName, false);

        // Add to current routine
        currentRoutine.addTask(newTask);

        // Update repository
        activityModel.getRoutineRepository().save(currentRoutine);

    }

    private void updateRoutineGoalDisplay(@Nullable Integer newTime) {
        currentRoutine.updateGoalTime(newTime);
        @Nullable Integer goalTime = currentRoutine.getGoalTime();
        if (goalTime == null) {
            binding.expectedTime.setText("-");
        } else {
            binding.expectedTime.setText(String.format("%d%s", goalTime, "m"));
        }
    }

    private void updateTimeDisplay() {
        long minutes = currentRoutine.getRoutineDurationMinutes();
        // if (minutes == 0) binding.actualTime.setText("-");
        binding.actualTime.setText(String.format("%d%s", minutes, "m"));

        boolean isActive = currentRoutine.isActive();

        if (!currentRoutine.isActive()) {
            binding.endRoutineButton.setText("Routine Ended");
            binding.endRoutineButton.setEnabled(false);
        }

        // Control button states
        binding.endRoutineButton.setEnabled(isActive);
        binding.stopTimerButton.setEnabled(isActive);
        binding.fastForwardButton.setEnabled(isActive);
        binding.homeButton.setEnabled(!isActive);

        // Update task list times
        taskAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        timerHandler.removeCallbacks(timerRunnable);
    }


}