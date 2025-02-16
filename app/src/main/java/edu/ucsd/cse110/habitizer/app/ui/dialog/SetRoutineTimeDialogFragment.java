package edu.ucsd.cse110.habitizer.app.ui.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import edu.ucsd.cse110.habitizer.app.MainViewModel;
import edu.ucsd.cse110.habitizer.app.databinding.DialogEditRoutineTimeBinding;

import edu.ucsd.cse110.habitizer.app.R;



public class SetRoutineTimeDialogFragment extends DialogFragment {
    public interface SetTimeListener {
        void onRoutineChanged(Integer newTime);
    }

    private SetTimeListener listener;

    SetRoutineTimeDialogFragment() {
        // Required empty public constructor
    }

    public static SetRoutineTimeDialogFragment newInstance(SetTimeListener listener) {
        var fragment = new SetRoutineTimeDialogFragment();
        fragment.listener = listener;
        return fragment;
    }

//    @Override
//    public void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        var modelOwner = requireActivity();
//        var modelFactory = ViewModelProvider.Factory.from(MainViewModel.initializer);
//        var modelProvider = new ViewModelProvider(modelOwner, modelFactory);
//        this.activityModel = modelProvider.get(MainViewModel.class);
//    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Set Routine Goal Time");

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_edit_routine_time, null);
        builder.setView(view);

        EditText timeInput = view.findViewById(R.id.routine_goal_time_edit);

        builder.setPositiveButton("OK", (dialog, which) -> {
            int newTime = Integer.parseInt(timeInput.getText().toString());
            if (listener != null && !(newTime < 0)) {
                listener.onRoutineChanged(newTime);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        return builder.create();
    }
}
