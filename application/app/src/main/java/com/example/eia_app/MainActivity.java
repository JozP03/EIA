package com.example.eia_app;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements SerialInputOutputManager.Listener {

    private static final String TAG = "USB_TEST";
    private static final String ACTION_USB_PERMISSION = "com.example.eia_app.USB_PERMISSION";

    private UsbSerialPort usbPort;
    private SerialInputOutputManager usbIoManager;
    
    private LinearLayout configLayout;
    private TextView statusText;
    private Spinner wifiSpinner;
    private EditText passwordInput;
    private Button connectButton;

    private final List<String> networksList = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.textView5);
        configLayout = findViewById(R.id.configLayout);
        wifiSpinner = findViewById(R.id.spinner);
        passwordInput = findViewById(R.id.editTextTextPassword);
        connectButton = findViewById(R.id.button);

        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, networksList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        wifiSpinner.setAdapter(spinnerAdapter);

        connectButton.setOnClickListener(v -> {
            if (usbPort == null) {
                initUsb();
            } else {
                sendWifiCredentials();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        ContextCompat.registerReceiver(this, usbReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(usbReceiver);
    }

    private void initUsb() {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);

        if (availableDrivers.isEmpty()) {
            Toast.makeText(this, "Nie znaleziono urządzenia USB", Toast.LENGTH_SHORT).show();
            return;
        }

        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDevice device = driver.getDevice();

        if (!manager.hasPermission(device)) {
            int flags = PendingIntent.FLAG_IMMUTABLE;
            PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), flags);
            manager.requestPermission(device, permissionIntent);
        } else {
            openSerialPort(driver);
        }
    }

    private void openSerialPort(UsbSerialDriver driver) {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());

        if (connection == null) {
            Log.e(TAG, "Nie udało się otworzyć połączenia (connection is null)");
            return;
        }

        usbPort = driver.getPorts().get(0);

        try {
            usbPort.open(connection);
            usbPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

            usbIoManager = new SerialInputOutputManager(usbPort, this);
            usbIoManager.start();

            Log.d(TAG, "Port szeregowy otwarty pomyślnie!");
            Toast.makeText(this, "Połączono z ESP32!", Toast.LENGTH_SHORT).show();

            showConfigUi();

            String command = "SCAN\n";
            usbPort.write(command.getBytes(), 1000);

        } catch (IOException e) {
            Log.e(TAG, "Błąd podczas otwierania portu: " + e.getMessage());
            closeUsb();
        }
    }

    private void closeUsb() {
        if (usbIoManager != null) {
            usbIoManager.stop();
            usbIoManager = null;
        }
        if (usbPort != null) {
            try {
                usbPort.close();
            } catch (IOException ignored) {}
            usbPort = null;
        }
    }

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            initUsb();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Odmowa uprawnień USB", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };

    private void showConfigUi() {
        runOnUiThread(() -> {
            statusText.setVisibility(View.GONE);
            configLayout.setVisibility(View.VISIBLE);
            connectButton.setText("Zapisz WiFi i Połącz");
        });
    }

    private void sendWifiCredentials() {
        if (usbPort == null) return;
        
        String ssid = wifiSpinner.getSelectedItem().toString();
        String pass = passwordInput.getText().toString();
        String command = "CONN:" + ssid + ";" + pass + "\n";

        try {
            usbPort.write(command.getBytes(), 1000);
            Toast.makeText(this, "Wysyłanie danych...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "Błąd wysyłania: " + e.getMessage());
            Toast.makeText(this, "Błąd wysyłania danych", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onNewData(byte[] data) {
        String received = new String(data).trim();
        Log.d(TAG, "Odebrano: " + received);

        runOnUiThread(() -> {
            if (received.startsWith("NETWORKS:")) {
                String rawList = received.substring(9);
                String[] networks = rawList.split(",");
                networksList.clear();
                networksList.addAll(Arrays.asList(networks));
                spinnerAdapter.notifyDataSetChanged();
            } else if (received.startsWith("STATUS:OK")) {
                Toast.makeText(this, "ESP Połączone z WiFi!", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onRunError(Exception e) {
        Log.e(TAG, "Błąd połączenia USB: " + e.getMessage());
        runOnUiThread(() -> {
            Toast.makeText(this, "Rozłączono urządzenie USB", Toast.LENGTH_SHORT).show();
            closeUsb();
            statusText.setVisibility(View.VISIBLE);
            configLayout.setVisibility(View.GONE);
            connectButton.setText("Connect");
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeUsb();
    }
}
