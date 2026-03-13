package com.example.paparellena.utils;

import android.content.Context;
import android.media.MediaPlayer;

public class SoundManager {
    private MediaPlayer mediaPlayer;
    private Context context;

    public SoundManager(Context context) {
        this.context = context;
    }

    public void playGameOverSound(int soundResourceId) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = MediaPlayer.create(context, soundResourceId);
        mediaPlayer.start();
    }

    public void stopSound() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
