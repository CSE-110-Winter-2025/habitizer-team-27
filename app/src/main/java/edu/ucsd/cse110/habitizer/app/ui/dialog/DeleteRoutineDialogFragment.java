package edu.ucsd.cse110.habitizer.app.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;


import edu.ucsd.cse110.habitizer.app.MainViewModel;
import edu.ucsd.cse110.habitizer.app.data.LegacyLogicAdapter;
import edu.ucsd.cse110.habitizer.app.data.db.HabitizerRepository;

public class DeleteRoutineDialogFragment extends DialogFragment {
    private static final String ARG_ROUTINE_ID = "routine_id";
    private int routineId;

    LegacyLogicAdapter logicAdapter = new LegacyLogicAdapter();

    DeleteRoutineDialogFragment(){

    }

    public static DeleteRoutineDialogFragment newInstance(int routineId) {
        var fragment = new DeleteRoutineDialogFragment();

        Bundle args = new Bundle();
        args.putInt(ARG_ROUTINE_ID, routineId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            routineId = getArguments().getInt(ARG_ROUTINE_ID);
        }
        //repository = new HabitizerRepository(requireContext());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(requireContext())
                .setTitle("Delete Routine")
                .setMessage("Are you sure you want to delete this Routine?")
                .setPositiveButton("Delete", this::onPositiveButtonClick)
                .setNegativeButton("Cancel", this::onNegativeButtonClick)
                .create();

    }

    private void onPositiveButtonClick(DialogInterface dialog, int which) {
        if (logicAdapter != null) {
            logicAdapter.removeRoutine(routineId);
        }
        dialog.dismiss();
    }

    private void onNegativeButtonClick(DialogInterface dialog, int which) {
        dialog.cancel();
    }
}
