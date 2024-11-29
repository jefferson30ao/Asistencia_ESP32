package com.example.asistencia_esp32;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class MainActivity extends AppCompatActivity {

    private TextView txtEstado;
    private EditText editCodigo;
    private Button btnEnviar;

    private static final String ESP32_IP = "http://192.168.4.1"; // Cambiar por la IP del ESP32
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtEstado = findViewById(R.id.txtEstado);
        editCodigo = findViewById(R.id.editCodigo);
        btnEnviar = findViewById(R.id.btnEnviar);

        requestQueue = Volley.newRequestQueue(this);

        btnEnviar.setOnClickListener(v -> {
            String codigo = editCodigo.getText().toString();
            if (!codigo.isEmpty()) {
                enviarDatosESP32(codigo);
            } else {
                Toast.makeText(MainActivity.this, "Ingrese un código", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void enviarDatosESP32(String codigo) {
        // Crear la URL para la petición HTTP
        String url = ESP32_IP + "/registrar?codigo=" + codigo;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    // Respuesta exitosa del ESP32
                    txtEstado.setText("Estado: " + response);
                    Toast.makeText(MainActivity.this, "Datos enviados correctamente", Toast.LENGTH_SHORT).show();
                },
                error -> {
                    // Error al conectar con el ESP32
                    txtEstado.setText("Estado: Error de conexión");
                    Toast.makeText(MainActivity.this, "Error al enviar datos", Toast.LENGTH_SHORT).show();
                });

        // Agregar la solicitud a la cola de peticiones
        requestQueue.add(stringRequest);
    }
}





