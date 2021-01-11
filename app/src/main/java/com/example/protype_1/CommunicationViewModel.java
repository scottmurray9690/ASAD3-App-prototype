package com.example.protype_1;

import androidx.lifecycle.ViewModel;

public class CommunicationViewModel extends ViewModel {
    // Tracks whether or not breathing sounds are being recorded.
    private boolean recording;
    // getter
    public boolean isRecording() { return recording; }
    // setter
    public void setRecording(boolean recording) {
        this.recording = recording;
    }
}
