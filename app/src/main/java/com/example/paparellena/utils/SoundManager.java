package com.example.paparellena.utils;

import android.content.Context;
import android.media.MediaPlayer;
import com.example.paparellena.R;

public class SoundManager {
    private Context context;
    private MediaPlayer backgroundMusic;
    private MediaPlayer gameOverPlayer;

    public SoundManager(Context context) {
        this.context = context;
    }

    public void playBackgroundMusic() {
        try {
            if (backgroundMusic == null) {
                backgroundMusic = MediaPlayer.create(context, R.raw.sarabi_toto_africa);
                if (backgroundMusic != null) {
                    backgroundMusic.setLooping(true);
                    backgroundMusic.setVolume(0.3f, 0.3f);
                }
            }
            if (backgroundMusic != null && !backgroundMusic.isPlaying()) {
                backgroundMusic.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopBackgroundMusic() {
        if (backgroundMusic != null) {
            try {
                if (backgroundMusic.isPlaying()) {
                    backgroundMusic.stop();
                }
                backgroundMusic.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            backgroundMusic = null;
        }
    }

    public void playGameOverSound() {
        stopBackgroundMusic();
        try {
            if (gameOverPlayer != null) {
                gameOverPlayer.release();
            }
            gameOverPlayer = MediaPlayer.create(context, R.raw.alphix_game_over_417465);
            if (gameOverPlayer != null) {
                gameOverPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
