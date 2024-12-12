package com.example.asistencia_esp32;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.Manifest;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_WIFI_STATE}, 1);
            }
        }

        // Inicializar requestQueue
        requestQueue = Volley.newRequestQueue(this);

        // Obtener el Android ID
        String androidId = getAndroidId();
        if (androidId != null && !androidId.isEmpty()) {
            verificarAndroidIdEnESP32(androidId);
        } else {
            txtEstado.setText("Estado: No se pudo obtener el Android ID");
            Toast.makeText(this, "Error al obtener el Android ID", Toast.LENGTH_SHORT).show();
        }

        // Configurar botón para enviar código de estudiante
        btnEnviar.setOnClickListener(v -> {
            String codigo = editCodigo.getText().toString();
            if ((!codigo.isEmpty()) && codigo.length() == 8 && codigo.matches("\\d+")) {
                enviarCodigoESP32(androidId, codigo);
            } else {
                Toast.makeText(MainActivity.this, "Ingrese un código válido de 8 dígitos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getAndroidId() {
        try {
            // Obtener el Android ID (un identificador único por dispositivo)
            return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // En caso de error, retornar null
    }

    private void verificarAndroidIdEnESP32(String androidId) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yy", Locale.getDefault());
        String hoy = dateFormat.format(new Date());
        String url = ESP32_IP + "/android_id?id="+androidId+"&fecha="+hoy;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    if (response.contains("Android ID encontrado en Firebase")) {
                        txtEstado.setText("Estado: Asistencia ya registrada");
                        editCodigo.setEnabled(false);
                        btnEnviar.setEnabled(false);
                    } else if(response.contains("Android ID no encontrado en Firebase")){
                        txtEstado.setText("Estado: Ingrese su código de estudiante");
                        editCodigo.setEnabled(true);
                        btnEnviar.setEnabled(true);
                    }
                },
                error -> {
                    // Agregar detalles del error para depuración
                    txtEstado.setText("Estado: Error al conectar con el ESP32");
                    Log.e("verificarAndroidIdEnESP32", "Error: " + error.getMessage());
                    editCodigo.setText(error.getMessage());
                });

        requestQueue.add(stringRequest);
    }

    private void enviarCodigoESP32(String androidId, String codigo) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yy", Locale.getDefault());
        String hoy = dateFormat.format(new Date());
        String url = ESP32_IP + "/code?android_id="+androidId+"&code="+codigo+"&fecha="+hoy;
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
                    } else {
                        txtEstado.setText("Estado: Desconocido");
                    }
                },
                error -> {
                    txtEstado.setText("Estado: Error al conectar con el ESP32");
                    Toast.makeText(MainActivity.this, "Error al registrar el código", Toast.LENGTH_SHORT).show();
                });

        requestQueue.add(stringRequest);
    }
}