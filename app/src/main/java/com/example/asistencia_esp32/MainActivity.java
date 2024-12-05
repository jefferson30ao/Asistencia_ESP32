package com.example.asistencia_esp32;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private TextView txtEstado;
    private EditText editCodigo;
    private Button btnEnviar;

    private static final String ESP32_IP = "http://192.168.4.1"; // Dirección IP del ESP32
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtEstado = findViewById(R.id.txtEstado);
        editCodigo = findViewById(R.id.editCodigo);
        btnEnviar = findViewById(R.id.btnEnviar);

        // Inicializar la cola de peticiones HTTP
        requestQueue = Volley.newRequestQueue(this);

        // Obtener la dirección MAC del dispositivo
        String macAddress = getMacAddress();

        if (macAddress != null && !macAddress.isEmpty()) {
            // Enviar la dirección MAC al ESP32
            verificarMacEnESP32(macAddress);
        } else {
            txtEstado.setText("Estado: No se pudo obtener la MAC");
            Toast.makeText(this, "Error al obtener la dirección MAC", Toast.LENGTH_SHORT).show();
        }

        // Configurar botón para enviar código de estudiante
        btnEnviar.setOnClickListener(v -> {
            /*String codigo = editCodigo.getText().toString();
            if (!codigo.isEmpty() && codigo.length() == 8 && codigo.matches("\\d+")) {
                enviarCodigoESP32(macAddress, codigo);
            } else {
                Toast.makeText(MainActivity.this, "Ingrese un código válido de 8 dígitos", Toast.LENGTH_SHORT).show();
            }*/
            try {
                // Dirección IP del ESP32 (reemplázala con la IP de tu ESP32)
                String esp32Ip = "192.168.4.1"; // La dirección IP del Access Point del ESP32
                String urlString = "http://" + esp32Ip + "/DireccionMac"; // Ruta configurada en el ESP32

                // Crear la URL
                URL url = new URL(urlString);

                // Abrir conexión HTTP
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET"); // Establecer el método GET

                // Obtener el código de respuesta
                int responseCode = connection.getResponseCode();
                System.out.println("Response Code: " + responseCode);

                // Si la respuesta es exitosa (código 200)
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Leer la respuesta del servidor
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuffer response = new StringBuffer();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    // Imprimir la respuesta del servidor
                    System.out.println("Respuesta del servidor: " + response.toString());
                } else {
                    System.out.println("Error en la solicitud. Código de respuesta: " + responseCode);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private String getMacAddress() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return wifiInfo.getMacAddress();
    }

    private void verificarMacEnESP32(String macAddress) {
        String url = ESP32_IP + "/attendance?mac=" + macAddress;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    // Procesar respuesta del ESP32
                    if (response.contains("Asistencia ya registrada")) {
                        txtEstado.setText("Estado: Asistencia ya registrada");
                        editCodigo.setEnabled(false); // Deshabilitar el ingreso del código
                        btnEnviar.setEnabled(false);
                    } else {
                        txtEstado.setText("Estado: Ingrese su código de estudiante");
                        editCodigo.setEnabled(true); // Habilitar el ingreso del código
                        btnEnviar.setEnabled(true);
                    }
                },
                error -> {
                    txtEstado.setText("Estado: Error al conectar con el ESP32");
                    Toast.makeText(MainActivity.this, "Error al verificar la dirección MAC", Toast.LENGTH_SHORT).show();
                });

        requestQueue.add(stringRequest);
    }

    private void enviarCodigoESP32(String macAddress, String codigo) {
        String url = ESP32_IP + "/attendance?mac=" + macAddress + "&code=" + codigo;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    // Procesar respuesta del ESP32
                    if (response.contains("Asistencia registrada")) {
                        txtEstado.setText("Estado: Asistencia registrada");
                        Toast.makeText(MainActivity.this, "Asistencia registrada correctamente", Toast.LENGTH_SHORT).show();
                    } else {
                        txtEstado.setText("Estado: Error al registrar asistencia");
                    }
                },
                error -> {
                    txtEstado.setText("Estado: Error al conectar con el ESP32");
                    Toast.makeText(MainActivity.this, "Error al registrar el código", Toast.LENGTH_SHORT).show();
                });

        requestQueue.add(stringRequest);
    }
}