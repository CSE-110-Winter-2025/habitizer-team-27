
package edu.ucsd.cse110.habitizer.app.ui.routine;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import edu.ucsd.cse110.habitizer.app.MainViewModel;
import edu.ucsd.cse110.habitizer.app.R;
import edu.ucsd.cse110.habitizer.app.databinding.FragmentRoutineScreenBinding;
import edu.ucsd.cse110.habitizer.app.ui.dialog.CreateTaskDialogFragment;
import edu.ucsd.cse110.habitizer.app.ui.dialog.SetRoutineTimeDialogFragment;
import edu.ucsd.cse110.habitizer.lib.domain.Routine;
import edu.ucsd.cse110.habitizer.lib.domain.Task;
import edu.ucsd.cse110.observables.Subject;

public class RoutineFragment extends Fragment {
    private MainViewModel activityModel;
    private FragmentRoutineScreenBinding binding;
    private ArrayAdapter<Task> taskAdapter;

    private static final String ARG_ROUTINE_ID = "routine_id";
    private Routine currentRoutine;

    public RoutineFragment() {
        // required empty public constructor
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

        // Get routine
        this.currentRoutine = activityModel.getRoutineRepository().getRoutine(routineId);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRoutineScreenBinding.inflate(inflater, container, false);

        // Initialize ListView and Adapter
        ListView taskListView = binding.routineList;
        taskAdapter = new ArrayAdapter<Task>(
                requireContext(),
                R.layout.task_page,
                R.id.task_name,
                new ArrayList<>()
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

        // Observe routine goal time


        binding.routineNameTask.setText(currentRoutine.getRoutineName());

        binding.addTaskButton.setOnClickListener(v -> {
            CreateTaskDialogFragment dialog = CreateTaskDialogFragment.newInstance(this::addTaskToRoutine);
            dialog.show(getParentFragmentManager(), "CreateTaskDialog");
        });

        binding.expectedTime.setOnClickListener(v -> {
            SetRoutineTimeDialogFragment dialog = SetRoutineTimeDialogFragment.newInstance(currentRoutine.getRoutineId());
            dialog.show(getParentFragmentManager(), "SetRoutineTimeDialog");
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

        // Update ListView
        taskAdapter.notifyDataSetChanged();
    }
}