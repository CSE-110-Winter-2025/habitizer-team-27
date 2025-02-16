package edu.ucsd.cse110.habitizer.app.ui.homescreen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import edu.ucsd.cse110.habitizer.app.R;
import edu.ucsd.cse110.habitizer.app.ui.routine.RoutineFragment;
import edu.ucsd.cse110.habitizer.lib.domain.Routine;
import edu.ucsd.cse110.habitizer.app.MainViewModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class HomeScreenFragment extends Fragment {
    private final List<Routine> routines = new ArrayList<>();
    private MainViewModel activityModel;

    public HomeScreenFragment() {
        // Required empty public constructor
    }

    public static HomeScreenFragment newInstance() {
        return new HomeScreenFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize ViewModel
        var modelOwner = requireActivity();
        var modelFactory = ViewModelProvider.Factory.from(MainViewModel.initializer);
        var modelProvider = new ViewModelProvider(modelOwner, modelFactory);
        this.activityModel = modelProvider.get(MainViewModel.class);

        // Initialize repository if empty
        if(activityModel.getRoutineRepository().count() == 0) {
            activityModel.getRoutineRepository().save(new Routine(0, "Morning Routine"));
            activityModel.getRoutineRepository().save(new Routine(1, "Evening Routine"));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_page, container, false);

        ListView listView = view.findViewById(R.id.card_list);
        HomeAdapter adapter = new HomeAdapter(
                requireContext(),
                routines,
                routineId -> {
                    // Save routine state to repository before navigation
                    Routine routine = activityModel.getRoutineRepository().getRoutine(routineId);
                    routine.startRoutine(LocalDateTime.now());
                    activityModel.getRoutineRepository().save(routine);
                    navigateToRoutine(routineId);
                }
        );
        listView.setAdapter(adapter);

        // Observe changes to the list of routines
        activityModel.getRoutineRepository().findAll().observe(routines -> {
            this.routines.clear();
            assert routines != null;
            this.routines.addAll(routines);
            adapter.notifyDataSetChanged();
        });


        return view;
    }

    private void navigateToRoutine(int routineId) {

        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, RoutineFragment.newInstance(routineId));
        transaction.addToBackStack(null);
        transaction.commit();
    }
    }
