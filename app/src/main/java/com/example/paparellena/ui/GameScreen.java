package com.example.paparellena.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.paparellena.R;
import com.example.paparellena.game.GameManager;
import com.example.paparellena.game.Player;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class GameScreen extends AppCompatActivity implements GameManager.GameEventListener {
    private TextView tvStatus;
    private TextView tvTimer;
    private MaterialButton btnPassPotato;
    private GameManager gameManager;
    private Handler updateHandler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        tvStatus = findViewById(R.id.tv_game_status);
        tvTimer = findViewById(R.id.tv_timer); // Asegúrate de que este ID exista en activity_game.xml
        btnPassPotato = findViewById(R.id.btn_request_potato);

        gameManager = GameManager.getInstance();
        gameManager.setListener(this);

        btnPassPotato.setOnClickListener(v -> {
            if (gameManager.canPassPotato()) {
                gameManager.passPotatoToNext();
            } else if (gameManager.getLocalPlayer() != null && gameManager.getLocalPlayer().isHasPotato()) {
                Toast.makeText(this, "¡Espera! Debes tener la papa al menos 2.5s", Toast.LENGTH_SHORT).show();
            }
        });

        startUpdateTimer();
    }

    private void startUpdateTimer() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateUI();
                updateHandler.postDelayed(this, 100);
            }
        };
        updateHandler.post(updateRunnable);
    }

    private void updateUI() {
        Player localPlayer = gameManager.getLocalPlayer();
        if (localPlayer == null) return;
        
        if (localPlayer.isHasPotato()) {
            long remaining = gameManager.getRemainingTurnTime();
            tvStatus.setText("¡TIENES LA PAPA!");
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            
            if (gameManager.canPassPotato()) {
                btnPassPotato.setText("¡PASAR PAPA!");
                btnPassPotato.setEnabled(true);
            } else {
                btnPassPotato.setText("ESPERA...");
                btnPassPotato.setEnabled(false);
            }
            
            double seconds = remaining / 1000.0;
            String statusText = String.format(Locale.getDefault(), "¡TIENES LA PAPA!\nExplosión en: %.1fs", seconds);
            tvStatus.setText(statusText);
        } else {
            tvStatus.setText("Esperando la papa...");
            tvStatus.setTextColor(getResources().getColor(android.R.color.black));
            btnPassPotato.setText("NO TIENES LA PAPA");
            btnPassPotato.setEnabled(false);
        }
    }

    @Override
    public void onPlayerJoined(Player player) {}

    @Override
    public void onGameStarted(String starterId) {
        runOnUiThread(() -> Toast.makeText(this, "¡Juego Iniciado!", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onPotatoReceived() {
        runOnUiThread(() -> {
            Toast.makeText(this, "¡RECIBISTE LA PAPA!", Toast.LENGTH_SHORT).show();
            updateUI();
        });
    }

    @Override
    public void onPotatoPassed() {
        runOnUiThread(() -> {
            Toast.makeText(this, "Papa pasada", Toast.LENGTH_SHORT).show();
            updateUI();
        });
    }

    @Override
    public void onGameOver(String loserId) {
        runOnUiThread(() -> {
            Player localPlayer = gameManager.getLocalPlayer();
            String message = (localPlayer != null && loserId.equals(localPlayer.getId())) ? "¡BOOM! PERDISTE" : "¡Alguien explotó!";
            tvStatus.setText(message);
            btnPassPotato.setEnabled(false);
            Toast.makeText(this, "Fin del juego: " + loserId, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onTick(int seconds) {
        runOnUiThread(() -> {
            if (tvTimer != null) {
                tvTimer.setText(String.format(Locale.getDefault(), "Tiempo: %ds", seconds));
            }
        });
    }

    @Override
    public void onHoldTimeUpdate(long elapsedMs) {
        runOnUiThread(this::updateUI);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        updateHandler.removeCallbacks(updateRunnable);
        gameManager.setListener(null);
    }
}
