package com.example.protype_1;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

/**
 * SaveRecordingDialog
 * A dialog to prompt users to save or discard the recording after recording is stopped.
 */
public class SaveRecordingDialog extends DialogFragment {

    /**
     * Listener interface used so that CommunicationActivity can respond to the users selection (save/discard).
     */
    public interface SaveRecordingDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
    }

    SaveRecordingDialogListener listener;
    private String message;

    // Simple constructor allowing for a different message to be displayed depending on why recording was stopped.
    public SaveRecordingDialog(String message){
        super();
        this.message = message;
    }

    @Override
    // set up the listener when this dialog is attached to an activity
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (SaveRecordingDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(getParentFragment().toString()+" must implement CommDialogListener");
        }
    }

    @Override
    // sets up the dialog
    public Dialog onCreateDialog( Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(message+"\nWould you like to save the recording?")
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // the listener responds to the user's selection
                        listener.onDialogNegativeClick(SaveRecordingDialog.this);
                    }
                })
                .setPositiveButton("save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // the listener responds to the user's selection
                        listener.onDialogPositiveClick(SaveRecordingDialog.this);

                    }
                });
        return builder.create();
    }
}
