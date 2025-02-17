package edu.ucsd.cse110.habitizer.app.ui.routine;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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

    static class ViewHolder {
        TextView taskName;
        TextView taskTime;
        View taskItem;
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
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.task_page, parent, false);
            holder = new ViewHolder();
            holder.taskName = convertView.findViewById(R.id.task_name);
            holder.taskTime = convertView.findViewById(R.id.task_time);
            holder.taskItem = convertView.findViewById(R.id.task_item); // Critical ID match
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Task task = getItem(position);
        if (task == null) return convertView;

        // Update UI components
        holder.taskName.setText(task.getTaskName());
        updateTimeDisplay(holder.taskTime, task);

        // Set click listener properly
        holder.taskItem.setOnClickListener(v -> {
            Log.d("TaskAdapter", "Clicked on task: " + task.getTaskName());
            if (!task.isCompleted()) {
                handleTaskCompletion(task, holder);
            }
        });

        return convertView;
    }

    private void handleTaskCompletion(Task task, ViewHolder holder) {
        // Update model first
        routine.completeTask(task.getTaskName());
        dataSource.putRoutine(routine);

        // Then update UI
        updateTimeDisplay(holder.taskTime, task);
        notifyDataSetChanged(); // Refresh the list

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


