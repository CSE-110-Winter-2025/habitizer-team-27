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

import java.util.List;

import edu.ucsd.cse110.habitizer.app.R;
import edu.ucsd.cse110.habitizer.lib.data.InMemoryDataSource;
import edu.ucsd.cse110.habitizer.lib.domain.Task;
import edu.ucsd.cse110.habitizer.lib.domain.Routine;

public class TaskAdapter extends ArrayAdapter<Task> {
    private final Routine routine;
    private final InMemoryDataSource dataSource;

    // ViewHolder pattern for better performance
    static class ViewHolder {
        TextView taskName;
        CheckBox checkBox;
        TextView taskTime;
    }

    public TaskAdapter(Context context, int resource, List<Task> tasks,
                       Routine routine, InMemoryDataSource dataSource) {
        super(context, resource, tasks);
        this.routine = routine;
        this.dataSource = dataSource;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        // Validate position first
        if (position < 0 || position >= getCount()) {
            return convertView != null ? convertView :
                    LayoutInflater.from(getContext()).inflate(R.layout.task_page, parent, false);
        }

        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.task_page, parent, false);
            holder = new ViewHolder();
            holder.taskName = convertView.findViewById(R.id.task_name);
            holder.checkBox = convertView.findViewById(R.id.check_task);
            holder.taskTime = convertView.findViewById(R.id.task_time);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Task task = getItem(position);
        if (task == null) return convertView;

        // Clear previous listener to prevent recycling issues
        holder.checkBox.setOnCheckedChangeListener(null);

        // Bind data to views
        holder.taskName.setText(task.getTaskName());
        holder.checkBox.setChecked(task.isCompleted());
        updateTimeDisplay(holder.taskTime, task);
        holder.checkBox.setEnabled(!task.isCheckedOff());

        // Set position tag for correct item identification
        holder.checkBox.setTag(position);

        // Checkbox click handler
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int pos = (int) buttonView.getTag();
            Task currentTask = getItem(pos);

            if (currentTask != null && isChecked && !currentTask.isCheckedOff()) {
                handleTaskCompletion(currentTask, holder);
            }
        });

        return convertView;
    }

    private void handleTaskCompletion(Task task, ViewHolder holder) {
        // Disable checkbox immediately
        holder.checkBox.setEnabled(false);

        // Update business logic
        routine.completeTask(task.getTaskName());

        // Update data source
        dataSource.putRoutine(routine);

        // Handle auto-complete
        if (routine.autoCompleteRoutine()) {
            dataSource.putRoutine(routine);
        }

        // Update UI components
        updateTimeDisplay(holder.taskTime, task);
        notifyDataSetChanged();

        Log.d("TaskCompletion",
                "Completed: " + task.getTaskName() +
                        " | Duration: " + formatTime(task.getDuration()));
    }

    private void updateTimeDisplay(TextView taskTime, Task task) {
        taskTime.setText(task.isCompleted() ?
                formatTime(task.getDuration()) : "");
    }

    private String formatTime(long minutes) {
        return minutes > 0 ? String.format("%dm", minutes) : "";
    }
}