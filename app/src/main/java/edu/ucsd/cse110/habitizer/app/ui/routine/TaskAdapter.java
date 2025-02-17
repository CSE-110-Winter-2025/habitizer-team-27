
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
import androidx.core.util.Consumer;

import java.util.List;

import javax.sql.DataSource;

import edu.ucsd.cse110.habitizer.app.R;
import edu.ucsd.cse110.habitizer.app.databinding.TaskPageBinding;
import edu.ucsd.cse110.habitizer.app.ui.dialog.SetRoutineTimeDialogFragment;
import edu.ucsd.cse110.habitizer.lib.data.InMemoryDataSource;
import edu.ucsd.cse110.habitizer.lib.domain.Task;
import edu.ucsd.cse110.habitizer.lib.domain.Routine;

public class TaskAdapter extends ArrayAdapter<Task> {
    private final Routine routine;
    private final InMemoryDataSource dataSource;
    Consumer<Integer> onNameClick;

    public TaskAdapter(Context context,
                       int resource,
                       List<Task> tasks,
                       Routine routine,
                       InMemoryDataSource dataSource,
                       Consumer<Integer> onNameClick) {
        super(context, resource, tasks);  // Pass resource to super
        this.routine = routine;
        this.dataSource = dataSource;
        this.onNameClick = onNameClick;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        Task task = getItem(position);

        TaskPageBinding binding;
        if (convertView != null) {
            binding = TaskPageBinding.bind(convertView);

            binding.taskName.setOnClickListener(v -> {
                Log.d("Hello", "Hello");
            });
        } else {
            var layoutInflater = LayoutInflater.from(getContext());
            binding = TaskPageBinding.inflate(layoutInflater, parent, false);


        }

//        TextView taskName = convertView.findViewById(R.id.task_name);
//        CheckBox checkBox = convertView.findViewById(R.id.check_task);
//        TextView taskTime = convertView.findViewById(R.id.task_time);

       //  if (task != null) {
            binding.taskName.setText(task.getTaskName());
            binding.checkTask.setChecked(task.isCompleted());
            // Modified time display logic
            if (task.isCompleted()) {
                binding.taskTime.setText(formatTime(task.getDuration()));
            } else {
                binding.taskTime.setText("");
            }

            // This is so that we are notified if a checkbox is checked
            // checkBox.setOnCheckedChangeListener(null);
            binding.checkTask.setOnCheckedChangeListener((buttonView, isChecked) -> {
                task.setCheckedOff(true);
                binding.checkTask.setEnabled(false);
                notifyDataSetChanged();

                binding.taskTime.setText(formatTime(task.getDuration()));

                Log.d("Task completed", "Task took " + task.getDuration());
            });

            binding.taskName.setOnClickListener(v -> {
                Log.d("Test", "log test");
                var id = task.getTaskId();
                onNameClick.accept(id);
            });

            if (routine.autoCompleteRoutine()) {
                dataSource.putRoutine(routine); // Ensure data persistence
            }
      //   }

        return binding.getRoot();
    }

    private String formatTime(long minutes) {
        if (minutes <= 0) return "";

        return String.format("%dm", minutes);
    }
}

