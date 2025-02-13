package edu.ucsd.cse110.habitizer.app.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import edu.ucsd.cse110.habitizer.app.R;


public class CreateTaskDialogFragment extends DialogFragment {
    public interface CreateTaskListener {
        void onTaskCreated(String taskName, boolean isPrepend);
    }

    private CreateTaskListener listener;

    public static CreateTaskDialogFragment newInstance(CreateTaskListener listener) {
        CreateTaskDialogFragment fragment = new CreateTaskDialogFragment();
        fragment.listener = listener;
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add New Task");

        // 加载 dialog_add_task.xml
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_task, null);
        builder.setView(view);

        // 获取控件
        EditText taskNameInput = view.findViewById(R.id.task_name_edit_text);
        RadioGroup insertModeGroup = view.findViewById(R.id.insert_mode_radio_button_group);
        RadioButton prependOption = view.findViewById(R.id.prepend_radio_button);

        // 设置 OK 按钮
        builder.setPositiveButton("OK", (dialog, which) -> {
            String taskName = taskNameInput.getText().toString().trim();
            boolean isPrepend = prependOption.isChecked();
            if (listener != null && !taskName.isEmpty()) {
                listener.onTaskCreated(taskName, isPrepend);
            }
        });

        // 设置 Cancel 按钮
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        return builder.create();
    }
}
