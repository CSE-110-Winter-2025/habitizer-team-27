
package edu.ucsd.cse110.habitizer.app.ui.routine;

import android.os.Bundle;
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
import edu.ucsd.cse110.habitizer.lib.domain.Task;

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

        return binding.getRoot();
    }



}