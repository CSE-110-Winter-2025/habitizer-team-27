
package edu.ucsd.cse110.habitizer.app.ui.routine;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

import edu.ucsd.cse110.habitizer.app.R;
import edu.ucsd.cse110.habitizer.lib.domain.Task;

public class TaskAdapter extends ArrayAdapter<Task> {
    public TaskAdapter(Context context, int resource, List<Task> tasks) {
        super(context, resource, tasks);  // Pass resource to super
    }
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        Task task = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.task_page, parent, false);
        }

        TextView taskName = convertView.findViewById(R.id.task_name);
        CheckBox checkBox = convertView.findViewById(R.id.check_task);
        TextView taskTime = convertView.findViewById(R.id.task_time);

        if (task != null) {
            taskName.setText(task.getTaskName());
            checkBox.setChecked(task.isCompleted());
            taskTime.setText(formatTime(task.getDurationMinutes()));
        }

        return convertView;
    }

    private String formatTime(long minutes) {
        if (minutes <= 0) return "0 min";

        return String.format("%d min", minutes);
    }
}

