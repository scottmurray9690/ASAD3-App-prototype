package com.example.protype_1;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * CommunicationViewModel
 * A very simple ViewModel required to keep track of whether or not the app is recording.
 *
 * This is used so CommunicationActivity and CommunicationFragment always agree on the recording state.
 */
public class CommunicationViewModel extends ViewModel {
    // Tracks whether or not breathing sounds are being recorded.
    // This is MutableLiveData so that CommunicationActivity can react to it changing.
    private MutableLiveData<Boolean> recording;
    
    public MutableLiveData<Boolean> getRecording() {
        if(recording == null) {
            recording = new MutableLiveData<Boolean>();
        }
        return recording;
    }
}
