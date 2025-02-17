package edu.ucsd.cse110.habitizer.app.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import edu.ucsd.cse110.habitizer.app.R;
import edu.ucsd.cse110.habitizer.lib.domain.Task;

public class RenameTaskDialogFragment extends DialogFragment {
    public interface RenameTaskListener {
        void onTaskRenamed(String taskName);
    }

    private RenameTaskListener listener;

    public static RenameTaskDialogFragment newInstance(RenameTaskListener listener) {
        RenameTaskDialogFragment fragment = new RenameTaskDialogFragment();
        fragment.listener = listener;
        return fragment;
    }

    @NonNull
    @Override

    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit Task Name");

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_rename_task, null);
        builder.setView(view);

        EditText taskNameInput = view.findViewById(R.id.rename_task_line);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String taskName = taskNameInput.getText().toString().trim();
            if (listener != null && !taskName.isEmpty()) {
                listener.onTaskRenamed(taskName);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        return builder.create();
    }
}
