package com.example.protype_1;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class CommunicationViewModel extends ViewModel {
    // Tracks whether or not breathing sounds are being recorded.
    private MutableLiveData<Boolean> recording;
    
    public MutableLiveData<Boolean> getRecording() {
        if(recording == null) {
            recording = new MutableLiveData<Boolean>();
        }
        return recording;
    }
}
