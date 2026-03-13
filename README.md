<h1>ESTRURTURA</h1>
<ul>
  <li>app
    <ul>
      <li>ui
        <ul>
          <li>MenuScreen.java</li>
          <li>LobbyScreen.java</li>
          <li>GameScreen.java</li>
        </ul>
      </li>
      <li>network
        <ul>
          <li>Server.java</li>
          <li>Client.java</li>
        </ul>
      </li>
      <li>game
        <ul>
          <li>GameManager.java</li>
          <li>Player.java</li>
        </ul>
      </li>
      <li>utils
        <ul>
          <li>SoundManager.java</li>
          <li>Timer.java</li>
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
