
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
        binding.addTaskButton.setOnClickListener(v -> {
            CreateTaskDialogFragment dialog = CreateTaskDialogFragment.newInstance(this::addTaskToRoutine);
            dialog.show(getParentFragmentManager(), "CreateTaskDialog");
        });
        return binding.getRoot();
    }
    private void addTaskToRoutine(String taskName, boolean isPrepend) {
        if (taskName == null || taskName.trim().isEmpty()) return;

        Log.d("RoutineFragment", "Adding task: " + taskName + " (Prepend: " + isPrepend + ")");

        // 获取 TaskRepository
        var taskRepository = activityModel.getTaskRepository();
        var tasksSubject = taskRepository.findAll();
        var tasks = tasksSubject.getValue();

        if (tasks == null) {
            Log.e("RoutineFragment", "tasksSubject.getValue() returned null!");
            return;
        }

        // **创建一个新的可变列表**
        List<Task> mutableTasks = new ArrayList<>(tasks);

        // 生成 Task ID
        int taskId = mutableTasks.isEmpty() ? 0 : mutableTasks.size();

        // 创建新任务
        Task newTask = new Task(taskId, taskName);

        // 插入任务
        if (isPrepend) {
            mutableTasks.add(0, newTask);
            Log.d("RoutineFragment", "Prepending task: " + taskName);
        } else {
            mutableTasks.add(newTask);
            Log.d("RoutineFragment", "Appending task: " + taskName);
        }

        // 存入 TaskRepository
        taskRepository.save(newTask);

        Log.d("RoutineFragment", "Task list size after: " + mutableTasks.size());

        // 🔹 更新 ListView
        taskAdapter.clear();
        taskAdapter.addAll(mutableTasks);
        taskAdapter.notifyDataSetChanged();
    }
}