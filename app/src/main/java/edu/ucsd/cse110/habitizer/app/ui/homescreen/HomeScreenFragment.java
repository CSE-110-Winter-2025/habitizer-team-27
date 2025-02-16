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
import java.util.ArrayList;
import java.util.List;

public class HomeScreenFragment extends Fragment {

    public HomeScreenFragment() {
        // Required empty public constructor
    }

    public static HomeScreenFragment newInstance() {
        return new HomeScreenFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_page, container, false);

        // Sample routine list (Replace with actual data from database)
        List<String> routineNames = new ArrayList<>();
        routineNames.add("Morning Routine");
        routineNames.add("Evening Routine");


        // Set up ListView with Custom Adapter
        ListView routineListView = view.findViewById(R.id.card_list);
        HomeAdapter adapter = new HomeAdapter(requireContext(), routineNames, this::navigateToRoutine);
        routineListView.setAdapter(adapter);

        return view;
    }

    private void navigateToRoutine() {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, RoutineFragment.newInstance());
        transaction.addToBackStack(null); // Allows back navigation
        transaction.commit();
    }
}
