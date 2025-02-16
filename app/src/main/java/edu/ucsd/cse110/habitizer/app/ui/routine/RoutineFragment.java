
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

import edu.ucsd.cse110.habitizer.app.MainViewModel;
import edu.ucsd.cse110.habitizer.app.R;
import edu.ucsd.cse110.habitizer.app.databinding.FragmentRoutineScreenBinding;
import edu.ucsd.cse110.habitizer.app.ui.dialog.CreateTaskDialogFragment;
import edu.ucsd.cse110.habitizer.lib.domain.Task;
import edu.ucsd.cse110.observables.Subject;

public class RoutineFragment extends Fragment {
    private MainViewModel activityModel;
    private FragmentRoutineScreenBinding binding;
    private ArrayAdapter<Task> taskAdapter;


    public RoutineFragment() {
        // required empty public constructor
    }

    public static RoutineFragment newInstance() {
        RoutineFragment fragment = new RoutineFragment();
        Bundle args = new Bundle();
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
        activityModel.getTaskRepository().findAll().observe(
                tasks -> {
                    taskAdapter.clear();
                    taskAdapter.addAll(tasks);
                    taskAdapter.notifyDataSetChanged();
                }
        );

        // Create add task listener
        binding.addTaskButton.setOnClickListener(v -> {
            CreateTaskDialogFragment dialog = CreateTaskDialogFragment.newInstance(this::addTaskToRoutine);
            dialog.show(getParentFragmentManager(), "CreateTaskDialog");
        });

        // Create go home listener
        binding.homeButton.setOnClickListener(v -> {
            // swap to home fragment (?)
        });

        binding.stopTimerButton.setOnClickListener(v -> {
        });

        binding.fastForwardButton.setOnClickListener(v -> {
            // fast forward timer
        });

        return binding.getRoot();
    }

    private void addTaskToRoutine(String taskName, boolean isPrepend) {
        if (taskName == null || taskName.trim().isEmpty()) return;

        Log.d("RoutineFragment", "Adding task: " + taskName + " (Prepend: " + isPrepend + ")");

        // Get TaskRepository
        var taskRepository = activityModel.getTaskRepository();
        var tasksSubject = taskRepository.findAll();
        var tasks = tasksSubject.getValue();

        if (tasks == null) {
            Log.e("RoutineFragment", "tasksSubject.getValue() returned null!");
            return;
        }

        // create a mutable list
        List<Task> mutableTasks = new ArrayList<>(tasks);

        // Generate Task ID
        int taskId = mutableTasks.isEmpty() ? 0 : mutableTasks.size();

        // Create new task
        Task newTask = new Task(taskId, taskName, false);

        // Insert task
        if (isPrepend) {
            mutableTasks.add(0, newTask);
            Log.d("RoutineFragment", "Prepending task: " + taskName);
        } else {
            mutableTasks.add(newTask);
            Log.d("RoutineFragment", "Appending task: " + taskName);
        }

        // Save TaskRepository
        taskRepository.save(newTask);

        Log.d("RoutineFragment", "Task list size after: " + mutableTasks.size());

        // Update ListView
        taskAdapter.clear();
        taskAdapter.addAll(mutableTasks);
        taskAdapter.notifyDataSetChanged();
    }
}