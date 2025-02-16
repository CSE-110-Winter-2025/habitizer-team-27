package edu.ucsd.cse110.habitizer.app.ui.homescreen;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.BaseAdapter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

import edu.ucsd.cse110.habitizer.app.R;
import edu.ucsd.cse110.habitizer.lib.domain.Routine;

public class HomeAdapter extends BaseAdapter {
    private final Context context;
    private final List<Routine> routines;
    private final Consumer<Integer> onRoutineStart;

    public HomeAdapter(Context context, List<Routine> routines, Consumer<Integer>  onRoutineStart) {
        this.context = context;
        this.routines = routines;
        this.onRoutineStart = onRoutineStart;
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


        Routine routine = routines.get(position);
        routineNameText.setText(routine.getRoutineName());

        startRoutineButton.setOnClickListener(v -> {
            onRoutineStart.accept(routine.getRoutineId());
            routine.startRoutine(LocalDateTime.now());
        });

        return convertView;
    }
}
