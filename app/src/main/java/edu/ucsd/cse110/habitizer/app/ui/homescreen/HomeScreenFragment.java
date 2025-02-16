package edu.ucsd.cse110.habitizer.app.ui.homescreen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import edu.ucsd.cse110.habitizer.app.R;
import edu.ucsd.cse110.habitizer.app.ui.routine.RoutineFragment;
import edu.ucsd.cse110.habitizer.lib.domain.Routine;

import java.util.ArrayList;
import java.util.List;

public class HomeScreenFragment extends Fragment {
    private final List<Routine> routines = new ArrayList<>();

    public HomeScreenFragment() {
        // Required empty public constructor
    }

    public static HomeScreenFragment newInstance() {
        return new HomeScreenFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_page, container, false);
        if (routines.isEmpty()) {
            routines.add(new Routine(0, "Morning Routine"));
            routines.add(new Routine(1, "Evening Routine"));
        }

        ListView listView = view.findViewById(R.id.card_list);
        HomeAdapter adapter = new HomeAdapter(
                requireContext(),
                routines,
                this::navigateToRoutine
        );
        listView.setAdapter(adapter);


        return view;
    }

    private void navigateToRoutine(int routineId) {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, RoutineFragment.newInstance(routineId));
        transaction.addToBackStack(null);
        transaction.commit();
    }
    }
