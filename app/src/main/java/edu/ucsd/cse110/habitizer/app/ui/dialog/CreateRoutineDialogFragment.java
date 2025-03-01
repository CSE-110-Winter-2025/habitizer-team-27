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

public class CreateRoutineDialogFragment extends DialogFragment {
    public interface CreateRoutineListener {
        void onRoutineCreated(String routineName);
    }

    private CreateRoutineListener listener;

    public static CreateRoutineDialogFragment newInstance(CreateRoutineListener listener) {
        CreateRoutineDialogFragment fragment = new CreateRoutineDialogFragment();
        fragment.listener = listener;
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Create New Routine");

        // Create a simple EditText for the routine name
        final EditText input = new EditText(requireContext());
        input.setHint("Enter routine name");
        builder.setView(input);

        // Setup OK button
        builder.setPositiveButton("Create", (dialog, which) -> {
            String routineName = input.getText().toString().trim();
            if (listener != null && !routineName.isEmpty()) {
                listener.onRoutineCreated(routineName);
            }
        });

        // Setup cancel button
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        return builder.create();
    }
} 