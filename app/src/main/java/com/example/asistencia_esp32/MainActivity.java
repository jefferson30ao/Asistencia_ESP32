package com.example.asistencia_esp32;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import java.util.HashMap;
import java.util.Map;
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

        // Inicializar requestQueue
        requestQueue = Volley.newRequestQueue(this);

        // Obtener la dirección MAC del dispositivo
        String macAddress = getMacAddress();
        if (macAddress != null && !macAddress.isEmpty()) {
            verificarMacEnESP32(macAddress);
        } else {
            txtEstado.setText("Estado: No se pudo obtener la MAC");
            Toast.makeText(this, "Error al obtener la dirección MAC", Toast.LENGTH_SHORT).show();
        }

        // Configurar botón para enviar código de estudiante
        btnEnviar.setOnClickListener(v -> {
            String codigo = editCodigo.getText().toString();
            if ((!codigo.isEmpty()) && codigo.length() == 8 && codigo.matches("\\d+")) {
                enviarCodigoESP32(macAddress, codigo);
            } else {
                Toast.makeText(MainActivity.this, "Ingrese un código válido de 8 dígitos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getMacAddress() {
        WifiInfo wifiInfo = ((WifiManager) getSystemService(Context.WIFI_SERVICE)).getConnectionInfo();
            return wifiInfo.getMacAddress();
    }

    private void verificarMacEnESP32(String macAddress) {
        String url = "http://192.168.4.1/mac?id=" + macAddress;
        //Toast.makeText(MainActivity.this, url, Toast.LENGTH_SHORT).show();

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    if (response.contains("Mac encontrado en Firebase")) {
                        txtEstado.setText("Estado: Asistencia ya registrada");
                        editCodigo.setEnabled(false);
                        btnEnviar.setEnabled(false);
                    } else if(response.contains("Mac no encontrada")){
                        txtEstado.setText("Estado: Ingrese su código de estudiante");
                        editCodigo.setEnabled(true);
                        btnEnviar.setEnabled(true);
                    }
                },
                error -> {
                    // Agrega detalles del error para depuración
                    txtEstado.setText("Estado: Error al conectar con el ESP32");
                    Log.e("verificarMacEnESP32", "Error: " + error.getMessage());
                    editCodigo.setText(error.getMessage());
                });

        requestQueue.add(stringRequest);
    }


    private void enviarCodigoESP32(String macAddress, String codigo) {
        String url = "http://192.168.4.1/code";  // La ruta "/code" como se define en el ESP32

        Map<String, String> params = new HashMap<>();
        params.put("id", macAddress);  // Parámetro para la dirección MAC
        params.put("code", codigo);    // Parámetro para el código de estudiante

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    if (response.contains("Asistencia registrada exitosamente")) {
                        txtEstado.setText("Asistencia registrada exitosamente");
                        editCodigo.setEnabled(false);
                        btnEnviar.setEnabled(false);
                        Toast.makeText(MainActivity.this, "Asistencia registrada correctamente", Toast.LENGTH_SHORT).show();
                    } else if(response.contains("No perteneces a la clase")){
                        txtEstado.setText("Estado: No perteneces a la clase");
                        editCodigo.setEnabled(false);
                        btnEnviar.setEnabled(false);
                    }else{
                        txtEstado.setText("Estado: Desconocido");
                    }
                },
                error -> {
                    txtEstado.setText("Estado: Error al conectar con el ESP32");
                    Toast.makeText(MainActivity.this, "Error al registrar el código", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                return params;
            }
        };

        requestQueue.add(stringRequest);
    }
}
