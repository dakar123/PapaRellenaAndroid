package com.example.paparellena.network;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {
    // Ejemplo de uso de Retrofit para verificar conexión o estado
    @GET("status")
    Call<String> getStatus();
}
