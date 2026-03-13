<h1>ESTRURTURA</h1>
<ul>
  <li>app
    <ul>
      <li>ui
        <ul>
          <li>MenuScreen.kt</li>
          <li>LobbyScreen.kt</li>
          <li>GameScreen.kt</li>
        </ul>
      </li>
      <li>network
        <ul>
          <li>Server.kt</li>
          <li>Client.kt</li>
        </ul>
      </li>
      <li>game
        <ul>
          <li>GameManager.kt</li>
          <li>Player.kt</li>
        </ul>
      </li>
      <li>utils
        <ul>
          <li>SoundManager.kt</li>
          <li>Timer.kt</li>
        </ul>
      </li>
    </ul>
  </li>
</ul>
2
Interfaz gráfica moderna

Para que la app se vea bonita y fluida usa:

Jetpack Compose

Ventajas:

Interfaces modernas

Animaciones fáciles

Código más limpio

Mejor experiencia visual

3
Comunicación entre celulares (muy importante)

Para juegos en misma red Wi-Fi, la mejor opción es:

✔ Sockets TCP
con RETROFIT

4 Temporizador del juego
aleatorio


5 Sonido y alerta cuando alguien pierde

Clases útiles:

MediaPlayer

Vibrator

Cuando el tiempo llegue a 0:

✔ sonar alarma
✔ pantalla roja
✔ mensaje "Perdiste"
