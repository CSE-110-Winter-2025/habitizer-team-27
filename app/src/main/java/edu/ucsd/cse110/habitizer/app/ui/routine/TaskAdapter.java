package edu.ucsd.cse110.habitizer.app.ui.routine;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.Collection;

import edu.ucsd.cse110.habitizer.app.R;
import edu.ucsd.cse110.habitizer.app.data.LegacyLogicAdapter;
import edu.ucsd.cse110.habitizer.app.ui.dialog.CreateTaskDialogFragment;
import edu.ucsd.cse110.habitizer.app.ui.dialog.RenameTaskDialogFragment;
import edu.ucsd.cse110.habitizer.lib.domain.Task;
import edu.ucsd.cse110.habitizer.lib.domain.Routine;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentActivity;

public class TaskAdapter extends ArrayAdapter<Task> {
    private final Routine routine;
    private final LegacyLogicAdapter dataSource;
    private final FragmentManager fragmentManager;

    // ViewHolder pattern for better performance
    static class ViewHolder {
        TextView taskName;
        CheckBox checkBox;
        TextView taskTime;
        ImageButton moveUpButton;
        ImageButton moveDownButton;
        ImageButton renameButton;

    }

    public TaskAdapter(Context context, int resource, List<Task> tasks,
                       Routine routine, LegacyLogicAdapter dataSource, @Nullable FragmentManager fragmentManager) {
        super(context, resource, tasks);
        this.routine = routine;
        this.dataSource = dataSource;
        this.fragmentManager = fragmentManager;
        Log.d("TaskAdapter", "TaskAdapter created for routine: " +
                (routine != null ? routine.getRoutineName() : "null") +
                " with " + (tasks != null ? tasks.size() : 0) + " tasks");
        if (tasks != null && !tasks.isEmpty()) {
            for (int i = 0; i < tasks.size(); i++) {
                Task task = tasks.get(i);
                Log.d("TaskAdapter", "Initial task " + i + ": " +
                        (task != null ? task.getTaskName() : "null"));
            }
        }
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        Log.d("TaskAdapter", "getView called for position: " + position +
                " out of " + getCount() + " tasks");

        // Validate position first
        if (position < 0 || position >= getCount()) {
            Log.e("TaskAdapter", "Invalid position: " + position + ", count: " + getCount());
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
            holder.renameButton = convertView.findViewById(R.id.rename_button);
            holder.moveUpButton = convertView.findViewById(R.id.move_up_button);
            holder.moveDownButton = convertView.findViewById(R.id.move_down_button);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Task task = getItem(position);
        if (task == null) {
            Log.e("TaskAdapter", "Task at position " + position + " is null");
            return convertView;
        }

        Log.d("TaskAdapter", "Binding task at position " + position + ": " + task.getTaskName() +
                ", completed: " + task.isCompleted());

        // Clear previous listener to prevent recycling issues
        holder.checkBox.setOnCheckedChangeListener(null);

        // Bind data to views
        holder.taskName.setText(task.getTaskName());
        holder.checkBox.setChecked(task.isCompleted());
        updateTimeDisplay(holder.taskTime, task);
        holder.checkBox.setEnabled(!task.isCheckedOff());
        holder.moveUpButton.setTag(position);
        holder.moveDownButton.setTag(position);

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

        holder.renameButton.setOnClickListener(v -> {
            RenameTaskDialogFragment dialog = RenameTaskDialogFragment.newInstance(newName ->
                    renameTask(task, newName));
            dialog.show(fragmentManager, "RenameTaskDialog");
        });

        holder.moveUpButton.setOnClickListener((buttonView) -> {
            Object tag = buttonView.getTag();
            if (tag == null) {
                Log.e("TaskAdapter", "moveUpButton tag is null");
                return;
            }

            int pos = (int) tag;
            Task currentTask = getItem(pos);
            if (currentTask != null) {
                moveTaskUp(currentTask);
                notifyDataSetChanged();
            }
        });

        holder.moveDownButton.setOnClickListener((buttonView) -> {
            Object tag = buttonView.getTag();
            if (tag == null) {
                Log.e("TaskAdapter", "moveDownButton tag is null");
                return;
            }
            int pos = (int) tag;
            Task currentTask = getItem(pos);
            if (currentTask != null) {
                moveTaskDown(currentTask);
                notifyDataSetChanged();
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

    private void renameTask(Task task, String newName) {
        if (task == null || newName == null || newName.trim().isEmpty()) return;
        task.setTaskName(newName);
        dataSource.putRoutine(routine);
        notifyDataSetChanged();
    }
    private void moveTaskUp(Task task) {
        List<Task> tasks = routine.getTasks();

        if (tasks == null || tasks.isEmpty()) {
            Log.e("TaskAdapter", "moveTaskUp: Task list is null or empty");
            return;
        }
        routine.moveTaskUp(task);
        clear();
        addAll(tasks);
        notifyDataSetChanged();
    }

    private void moveTaskDown(Task task) {
        List<Task> tasks = routine.getTasks();

        if (tasks == null || tasks.isEmpty()) {
            Log.e("TaskAdapter", "moveTaskDown: Task list is null or empty");
            return;
        }
        routine.moveTaskDown(task);
        clear();
        addAll(tasks);
        notifyDataSetChanged();
    }

    private void updateTimeDisplay(TextView taskTime, Task task) {
        taskTime.setText(task.isCompleted() ?
                formatTime(task.getDuration()) : "");
    }

    private String formatTime(long minutes) {
        return minutes > 0 ? String.format("%dm", minutes) : "";
    }

    @Override
    public void clear() {
        Log.d("TaskAdapter", "Clearing all tasks from adapter");
        super.clear();
    }

    @Override
    public void addAll(Collection<? extends Task> collection) {
        Log.d("TaskAdapter", "Adding " + (collection != null ? collection.size() : 0) + " tasks to adapter");
        if (collection != null && !collection.isEmpty()) {
            int i = 0;
            for (Task task : collection) {
                Log.d("TaskAdapter", "Task " + i + ": " +
                        (task != null ? task.getTaskName() : "null"));
                i++;
            }
        }
        super.addAll(collection);
    }
}