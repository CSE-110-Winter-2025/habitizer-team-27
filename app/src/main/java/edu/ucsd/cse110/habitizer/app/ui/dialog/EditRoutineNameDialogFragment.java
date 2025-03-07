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

public class EditRoutineNameDialogFragment extends DialogFragment {
    private static final String ARG_ROUTINE_ID = "routine_id";
    private static final String ARG_CURRENT_NAME = "current_name";

    public interface EditRoutineNameListener {
        void onRoutineNameUpdated(int routineId, String newName);
    }

    private EditRoutineNameListener listener;
    private int routineId;
    private String currentName;

    public static EditRoutineNameDialogFragment newInstance(int routineId, String currentName, EditRoutineNameListener listener) {
        EditRoutineNameDialogFragment fragment = new EditRoutineNameDialogFragment();
        
        Bundle args = new Bundle();
        args.putInt(ARG_ROUTINE_ID, routineId);
        args.putString(ARG_CURRENT_NAME, currentName);
        fragment.setArguments(args);
        
        fragment.listener = listener;
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            routineId = getArguments().getInt(ARG_ROUTINE_ID);
            currentName = getArguments().getString(ARG_CURRENT_NAME);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit Routine Name");

        // Create EditText for the routine name and pre-fill with current name
        final EditText input = new EditText(requireContext());
        input.setText(currentName);
        input.setSelectAllOnFocus(true);
        builder.setView(input);

        // Setup Save button
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (listener != null && !newName.isEmpty()) {
                listener.onRoutineNameUpdated(routineId, newName);
            }
        });

        // Setup cancel button
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        return builder.create();
    }
} 