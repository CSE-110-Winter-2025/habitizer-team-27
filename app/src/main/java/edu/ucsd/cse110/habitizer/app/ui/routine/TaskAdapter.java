
package edu.ucsd.cse110.habitizer.app.ui.routine;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.time.LocalDateTime;
import java.util.List;

import javax.sql.DataSource;

import edu.ucsd.cse110.habitizer.app.R;
import edu.ucsd.cse110.habitizer.lib.data.InMemoryDataSource;
import edu.ucsd.cse110.habitizer.lib.domain.Task;
import edu.ucsd.cse110.habitizer.lib.domain.Routine;

public class TaskAdapter extends ArrayAdapter<Task> {
    private final Routine routine;
    private final InMemoryDataSource dataSource;

    public TaskAdapter(Context context, int resource, List<Task> tasks, Routine routine, InMemoryDataSource dataSource) {
        super(context, resource, tasks);  // Pass resource to super
        this.routine = routine;
        this.dataSource = dataSource;
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

        checkBox.setOnCheckedChangeListener(null);
        if (task != null) {
            checkBox.setEnabled(!task.isCompleted());

            taskName.setText(task.getTaskName());
            //checkBox.setChecked(task.isCompleted());

            // Modified time display logic
//            if (task.isCompleted()) {
//                taskTime.setText(formatTime(task.getDuration()));
//            } else {
//                taskTime.setText("");
//            }

            // This is so that we are notified if a checkbox is checked
            // checkBox.setOnCheckedChangeListener(null);
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                task.setCheckedOff(isChecked);

                if (isChecked) {
                    routine.completeTask(task.getTaskName());
                    // Disable checkbox immediately after checking
                    checkBox.setEnabled(false);
                    notifyDataSetChanged();
                }

                checkBox.setChecked(task.isCheckedOff());
                checkBox.setEnabled(!task.isCheckedOff());

                if (task.isCheckedOff()) {
                    taskTime.setText(formatTime(task.getDuration()));
                }

                Log.d("Task_completed", "Task took " + task.getDuration());
            });

            if (routine.autoCompleteRoutine()) {
                dataSource.putRoutine(routine); // Ensure data persistence
            }
        }

        return convertView;
    }

    private String formatTime(long minutes) {
        if (minutes <= 0) return "";

        return String.format("%dm", minutes);
    }
}

