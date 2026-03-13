package com.example.paparellena.utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import com.example.paparellena.R;

public class SoundManager {
    private Context context;
    private MediaPlayer backgroundMusic;
    private MediaPlayer gameOverPlayer;
    private ToneGenerator toneGenerator;

    public SoundManager(Context context) {
        this.context = context;
        try {
            this.toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playBackgroundMusic() {
        try {
            // Intentar cargar música de fondo si el recurso existe
            int resId = context.getResources().getIdentifier("musicafondo", "raw", context.getPackageName());
            if (resId != 0) {
                if (backgroundMusic == null) {
                    backgroundMusic = MediaPlayer.create(context, resId);
                    if (backgroundMusic != null) {
                        backgroundMusic.setLooping(true);
                        backgroundMusic.setVolume(0.3f, 0.3f);
                    }
                }
                if (backgroundMusic != null && !backgroundMusic.isPlaying()) {
                    backgroundMusic.start();
                }
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

    // "Crea" el sonido de pasar la papa usando ToneGenerator
    public void playPassPotatoSound() {
        if (toneGenerator != null) {
            // Un tono corto y ascendente para indicar el pase
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150);
        }
    }

    // "Crea" el sonido de tick usando ToneGenerator
    public void playTickSound() {
        if (toneGenerator != null) {
            // Un tono muy corto y seco para el segundero
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 50);
        }
    }

    public void playGameOverSound() {
        stopBackgroundMusic();
        try {
            int resId = context.getResources().getIdentifier("gameover", "raw", context.getPackageName());
            if (resId != 0) {
                if (gameOverPlayer != null) {
                    gameOverPlayer.release();
                }
                gameOverPlayer = MediaPlayer.create(context, resId);
                if (gameOverPlayer != null) {
                    gameOverPlayer.start();
                }
            } else {
                // Fallback si no hay audio: un tono largo y grave
                if (toneGenerator != null) {
                    toneGenerator.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 1000);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void release() {
        stopBackgroundMusic();
        if (gameOverPlayer != null) {
            gameOverPlayer.release();
        }
        if (toneGenerator != null) {
            toneGenerator.release();
        }
    }
}
