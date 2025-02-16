package edu.ucsd.cse110.habitizer.app.ui.homescreen;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.BaseAdapter;
import java.util.List;
import edu.ucsd.cse110.habitizer.app.R;

public class HomeAdapter extends BaseAdapter {
    private final Context context;
    private final List<String> routineNames;
    private final Runnable onRoutineStart;

    public HomeAdapter(Context context, List<String> routineNames, Runnable onRoutineStart) {
        this.context = context;
        this.routineNames = routineNames;
        this.onRoutineStart = onRoutineStart;
    }

    @Override
    public int getCount() {
        return routineNames.size();
    }

    @Override
    public Object getItem(int position) {
        return routineNames.get(position);
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

        // Set routine name
        routineNameText.setText(routineNames.get(position));

        // Set button click listener
        startRoutineButton.setOnClickListener(v -> onRoutineStart.run());

        return convertView;
    }
}
