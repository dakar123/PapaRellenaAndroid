package com.example.paparellena.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.paparellena.R;
import com.example.paparellena.game.GameManager;
import com.example.paparellena.game.Player;
import com.google.android.material.button.MaterialButton;

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
        tvTimer = new TextView(this); // Temporal, deberías tenerlo en el XML
        btnPassPotato = findViewById(R.id.btn_request_potato); // Usando el ID existente para pasar la papa

        gameManager = GameManager.getInstance();
        gameManager.setListener(this);

        btnPassPotato.setOnClickListener(v -> {
            if (gameManager.canPassPotato()) {
                gameManager.passPotatoToNext();
            } else if (gameManager.getLocalPlayer().isHasPotato()) {
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
        if (gameManager.getLocalPlayer().isHasPotato()) {
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
            
            // Mostrar cuenta regresiva de 5s
            double seconds = remaining / 1000.0;
            tvStatus.append(String.format("\nExplosión en: %.1fs", seconds));
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
            String message = loserId.equals(gameManager.getLocalPlayer().getId()) ? "¡BOOM! PERDISTE" : "¡Alguien explotó!";
            tvStatus.setText(message);
            btnPassPotato.setEnabled(false);
            Toast.makeText(this, "Fin del juego: " + loserId, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onTick(int seconds) {}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        updateHandler.removeCallbacks(updateRunnable);
    }
}
