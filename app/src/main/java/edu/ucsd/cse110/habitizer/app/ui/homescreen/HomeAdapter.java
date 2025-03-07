package edu.ucsd.cse110.habitizer.app.ui.homescreen;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.fragment.app.FragmentManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.BiConsumer;

import edu.ucsd.cse110.habitizer.app.R;
import edu.ucsd.cse110.habitizer.app.ui.dialog.EditRoutineNameDialogFragment;
import edu.ucsd.cse110.habitizer.lib.domain.Routine;

public class HomeAdapter extends BaseAdapter {
    private static final String TAG = "HomeAdapter";
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Context context;
    private final List<Routine> routines;
    private final Consumer<Integer> onRoutineStart;
    private final BiConsumer<Integer, String> onRoutineNameEdit;
    private final FragmentManager fragmentManager;

    public HomeAdapter(Context context, List<Routine> routines, 
                       Consumer<Integer> onRoutineStart,
                       BiConsumer<Integer, String> onRoutineNameEdit,
                       FragmentManager fragmentManager) {
        this.context = context;
        this.routines = routines;
        this.onRoutineStart = onRoutineStart;
        this.onRoutineNameEdit = onRoutineNameEdit;
        this.fragmentManager = fragmentManager;
    }

    @Override
    public int getCount() {
        return routines.size();
    }

    @Override
    public Object getItem(int position) {
        return routines.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.routine_list, parent, false);
        }

        TextView routineNameText = convertView.findViewById(R.id.routine_name);
        Button startRoutineButton = convertView.findViewById(R.id.start_routine_button);
        ImageButton editRoutineButton = convertView.findViewById(R.id.edit_routine_button);

        Routine routine = routines.get(position);
        routineNameText.setText(routine.getRoutineName());

        startRoutineButton.setOnClickListener(v -> {
            // Fetch routine from repository instead of local list
            onRoutineStart.accept(routine.getRoutineId());
        });

        // Set up edit button click listener
        editRoutineButton.setOnClickListener(v -> {
            Log.d(TAG, "Edit button clicked for routine: " + routine.getRoutineName());
            
            // Show the edit dialog
            EditRoutineNameDialogFragment dialog = EditRoutineNameDialogFragment.newInstance(
                routine.getRoutineId(), 
                routine.getRoutineName(),
                (routineId, newName) -> {
                    // Call the callback to handle the name update
                    onRoutineNameEdit.accept(routineId, newName);
                }
            );
            
            dialog.show(fragmentManager, "EditRoutineNameDialog");
        });

        return convertView;
    }
}
