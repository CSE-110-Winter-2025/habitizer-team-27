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
import edu.ucsd.cse110.habitizer.app.HabitizerApplication;
import edu.ucsd.cse110.habitizer.app.data.db.HabitizerRepository;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentActivity;

public class TaskAdapter extends ArrayAdapter<Task> {
    private final Routine routine;
    private final LegacyLogicAdapter dataSource;
    private final FragmentManager fragmentManager;
    private RoutineFragment routineFragment;

    // ViewHolder pattern for better performance
    static class ViewHolder {
        TextView taskName;
        CheckBox checkBox;
        TextView taskTime;
        ImageButton moveUpButton;
        ImageButton moveDownButton;
        ImageButton renameButton;
        ImageButton deleteButton;
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

    public void setRoutineFragment(RoutineFragment fragment) {
        this.routineFragment = fragment;
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
            holder.deleteButton = convertView.findViewById(R.id.imageButton4);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final Task task = getItem(position);
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
        holder.checkBox.setChecked(task.isCheckedOff());
        
        // Check if the fragment is paused
        boolean isFragmentPaused = (routineFragment != null && routineFragment.isPaused());
        
        // Disable checkbox if:
        // 1. The task is already checked off, OR
        // 2. The routine is not active, OR
        // 3. The fragment is paused (new condition to prevent task checking when paused)
        holder.checkBox.setEnabled(!task.isCheckedOff() && routine.isActive() && !isFragmentPaused);
        
        // Also disable up/down buttons and rename button when paused
        holder.moveUpButton.setEnabled(!isFragmentPaused);
        holder.moveDownButton.setEnabled(!isFragmentPaused);
        holder.renameButton.setEnabled(!isFragmentPaused);
        holder.deleteButton.setEnabled(!isFragmentPaused);
        
        updateTimeDisplay(holder.taskTime, task);
        holder.moveUpButton.setTag(position);
        holder.moveDownButton.setTag(position);
        holder.deleteButton.setTag(position);

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
        
        // Add click handler for delete button
        holder.deleteButton.setOnClickListener((buttonView) -> {
            Object tag = buttonView.getTag();
            if (tag == null) {
                Log.e("TaskAdapter", "deleteButton tag is null");
                return;
            }
            int pos = (int) tag;
            Task currentTask = getItem(pos);
            if (currentTask != null) {
                removeTask(currentTask);
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
            
            // Notify the RoutineFragment to update UI for ended routine
            if (routineFragment != null) {
                routineFragment.updateUIForEndedRoutine();
                Log.d("TaskAdapter", "Notified RoutineFragment that routine was auto-completed");
            } else {
                Log.e("TaskAdapter", "Unable to notify RoutineFragment - reference is null");
            }
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
        
        // Log the state before moving
        Log.d("TaskAdapter", "Before moveTaskUp: Task list = " + tasks);
        
        // Perform the move operation
        routine.moveTaskUp(task);
        
        // Log the state after moving
        Log.d("TaskAdapter", "After moveTaskUp: Task list = " + routine.getTasks());
        
        // Update the adapter's task list
        clear();
        addAll(tasks);
        
        // Update the UI
        notifyDataSetChanged();
        
        // Important: Save changes to the repository - use synchronous update for testing reliability
        try {
            // Get repository from application for direct synchronous update
            HabitizerRepository repository = HabitizerApplication.getRepository();
            
            // Update in data source for compatibility - use putRoutine instead of putRoutineSynchronously 
            Log.d("TaskAdapter", "Updating routine synchronously in repository");
            dataSource.putRoutine(routine);
            
            // Log the update
            Log.d("TaskAdapter", "Saved reordered routine to repository: " + routine.getRoutineName());
        } catch (Exception e) {
            Log.e("TaskAdapter", "Error during repository update", e);
        }
    }

    private void moveTaskDown(Task task) {
        List<Task> tasks = routine.getTasks();

        if (tasks == null || tasks.isEmpty()) {
            Log.e("TaskAdapter", "moveTaskDown: Task list is null or empty");
            return;
        }
        
        // Log the state before moving
        Log.d("TaskAdapter", "Before moveTaskDown: Task list = " + tasks);
        
        // Perform the move operation
        routine.moveTaskDown(task);
        
        // Log the state after moving
        Log.d("TaskAdapter", "After moveTaskDown: Task list = " + routine.getTasks());
        
        // Update the adapter's task list
        clear();
        addAll(tasks);
        
        // Update the UI
        notifyDataSetChanged();
        
        // Important: Save changes to the repository - use synchronous update for testing reliability
        try {
            // Get repository from application for direct synchronous update
            HabitizerRepository repository = HabitizerApplication.getRepository();
            
            // Update in data source for compatibility - use putRoutine instead of putRoutineSynchronously 
            Log.d("TaskAdapter", "Updating routine synchronously in repository");
            dataSource.putRoutine(routine);
            
            // Log the update
            Log.d("TaskAdapter", "Saved reordered routine to repository: " + routine.getRoutineName());
        } catch (Exception e) {
            Log.e("TaskAdapter", "Error during repository update", e);
        }
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

    /**
     * Remove a task from the routine
     * @param task The task to remove
     */
    private void removeTask(Task task) {
        if (task == null) {
            Log.e("TaskAdapter", "Cannot remove null task");
            return;
        }
        
        Log.d("TaskAdapter", "Removing task: " + task.getTaskName() + " (ID: " + task.getTaskId() + ")");
        
        // Remove task from the routine
        boolean removed = routine.removeTask(task);
        
        if (removed) {
            // Use the database method to properly update relationships
            if (routine.getRoutineId() != null && task.getTaskId() != null) {
                dataSource.removeTaskFromRoutine(routine.getRoutineId(), task.getTaskId());
                Log.d("TaskAdapter", "Task removed from database relationship");
            } else {
                // Fallback to the old method if IDs are not available
                dataSource.putRoutine(routine);
                Log.d("TaskAdapter", "Task removed using routine update");
            }
            
            // Update the adapter
            clear();
            addAll(routine.getTasks());
            notifyDataSetChanged();
            
            Log.d("TaskAdapter", "Task removed successfully, now " + routine.getTasks().size() + " tasks in routine");
        } else {
            Log.e("TaskAdapter", "Failed to remove task from routine");
        }
    }
}