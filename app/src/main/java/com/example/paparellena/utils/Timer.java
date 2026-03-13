package com.example.paparellena.utils;

import android.os.Handler;
import android.os.Looper;

public class Timer {
    public interface TimerCallback {
        void onTick(int remainingSeconds);
        void onFinish();
    }

    private Handler handler;
    private Runnable runnable;
    private int remainingTime;
    private TimerCallback callback;

    public Timer(int seconds, TimerCallback callback) {
        this.remainingTime = seconds;
        this.callback = callback;
        this.handler = new Handler(Looper.getMainLooper());
    }

    public void start() {
        runnable = new Runnable() {
            @Override
            public void run() {
                if (remainingTime > 0) {
                    callback.onTick(remainingTime);
                    remainingTime--;
                    handler.postDelayed(this, 1000);
                } else {
                    callback.onFinish();
                }
            }
        };
        handler.post(runnable);
    }

    public void stop() {
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }
}
