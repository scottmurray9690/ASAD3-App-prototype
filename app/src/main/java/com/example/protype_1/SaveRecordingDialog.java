package com.example.protype_1;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class SaveRecordingDialog extends DialogFragment {

    public interface SaveRecordingDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
    }

    SaveRecordingDialogListener listener;
    private String message;

    public SaveRecordingDialog(String message){
        super();
        this.message = message;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (SaveRecordingDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(getParentFragment().toString()+" must implement CommDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog( Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(message+"\nWould you like to save the recording?")
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.onDialogNegativeClick(SaveRecordingDialog.this);
                    }
                })
                .setPositiveButton("save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.onDialogPositiveClick(SaveRecordingDialog.this);

                    }
                });
        return builder.create();
    }
}
