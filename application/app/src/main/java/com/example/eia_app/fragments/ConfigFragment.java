package com.example.eia_app.fragments;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.eia_app.R;
import com.example.eia_app.services.UsbSerialService;


public class ConfigFragment extends Fragment implements UsbSerialService.ConnectionCallback {

    private static final String TAG = "ConfigFragment";
    private UsbSerialService usbService;
    private boolean isBound = false;
    private String ssid;
    private boolean isStaticIp = true;
    private View loadingOverlay;

    public ConfigFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (getArguments() != null) {
            ssid = getArguments().getString("selected_ssid");
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_config, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvSelectedWifi = view.findViewById(R.id.tvSelectedWifiName);
        if(ssid != null) {
            tvSelectedWifi.setText(ssid);
        }

        EditText etPassword = view.findViewById(R.id.etPassword);
        EditText etIpAddress = view.findViewById(R.id.etStaticIpAddress);
        EditText etGateway = view.findViewById(R.id.etStaticGateway);
        EditText etSubnet = view.findViewById(R.id.etStaticNetmask);
        View layoutStaticFields = view.findViewById(R.id.layoutStaticFields);
        android.widget.Button btnStatic = view.findViewById(R.id.btnStaticIp);
        android.widget.Button btnDhcp = view.findViewById(R.id.btnDhcp);
        loadingOverlay = view.findViewById(R.id.loadingOverlay);

        btnStatic.setOnClickListener(v -> {
            isStaticIp = true;
            layoutStaticFields.setVisibility(View.VISIBLE);
            btnStatic.setBackgroundResource(R.drawable.bg_toggle_left);
            btnStatic.setTextColor(getResources().getColor(R.color.white_bg, null));
            btnDhcp.setBackgroundResource(R.drawable.bg_toggle_right);
            btnDhcp.setTextColor(getResources().getColor(R.color.text_muted, null));
        });

        btnDhcp.setOnClickListener(v -> {
            isStaticIp = false;
            layoutStaticFields.setVisibility(View.GONE);
            btnDhcp.setBackgroundResource(R.drawable.bg_toggle_right);
            btnDhcp.setTextColor(getResources().getColor(R.color.white_bg, null)); // Tu pewnie powinien być kolor aktywny, ale bg_toggle_right może go ustawiać
            // Naprawa kolorów dla DHCP (uproszczona)
            btnStatic.setBackgroundResource(R.drawable.bg_toggle_left);
            btnStatic.setTextColor(getResources().getColor(R.color.text_muted, null));
        });

        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            Navigation.findNavController(view).navigate(R.id.action_configFragment_to_scanFragment);
        });

        view.findViewById(R.id.btnSubmit).setOnClickListener(v -> {
            String password = etPassword.getText().toString().trim();
            
            if (password.isEmpty()) {
                Toast.makeText(getContext(), "Wprowadź hasło!", Toast.LENGTH_SHORT).show();
                return;
            }

            String command;
            if (isStaticIp) {
                String ip = etIpAddress.getText().toString().trim();
                String gateway = etGateway.getText().toString().trim();
                String subnet = etSubnet.getText().toString().trim();

                if (ip.isEmpty() || gateway.isEmpty() || subnet.isEmpty()) {
                    Toast.makeText(getContext(), "Uzupełnij parametry IP!", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Format: CONN_STATIC:SSID;Haslo;IP;Brama;Maska
                command = "CONN_STATIC:" + ssid + ";" + password + ";" + ip + ";" + gateway + ";" + subnet + "\n";
            } else {
                // Format dla DHCP (założenie)
                command = "CONN_DHCP:" + ssid + ";" + password + "\n";
            }

            if (isBound && usbService != null && usbService.isConnected()) {
                // Pokazujemy animację ładowania
                loadingOverlay.setVisibility(View.VISIBLE);

                // Wysyłamy komendę przez USB!
                usbService.sendCommand(command);
                Toast.makeText(getContext(), "Wysyłanie konfiguracji...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Urządzenie nie jest połączone!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            UsbSerialService.UsbBinder binder = (UsbSerialService.UsbBinder) service;
            usbService = binder.getService();
            isBound = true;

            // Rejestrujemy ten fragment, aby nasłuchiwać odpowiedzi z ESP (np. STATUS:OK)
            usbService.setConnectionCallback(ConfigFragment.this);

            // Jeśli wejdziemy tu, a urządzenie z jakiegoś powodu zgłasza brak połączenia, spróbujmy je zainicjować
            if (!usbService.isConnected()) {
                Log.d(TAG, "Urządzenie zgłasza brak połączenia w ConfigFragment, inicjalizacja...");
                usbService.initUSB();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            usbService = null;
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(getContext(), UsbSerialService.class);
        requireActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (isBound) {
            if (usbService != null) {
                usbService.setConnectionCallback(null);
            }
            requireActivity().unbindService(serviceConnection);
            isBound = false;
        }
    }

    @Override
    public void onDataReceived(String line) {
        if (getActivity() == null || !isAdded()) return;

        getActivity().runOnUiThread(() -> {
            Log.d(TAG, "Odpowiedź z ESP: " + line);

            // zapisanie flagi do pliku
            android.content.SharedPreferences prefs = requireActivity().getSharedPreferences("EIA_PREFS", android.content.Context.MODE_PRIVATE);
            prefs.edit().putBoolean("is_configured", true).apply();

            // Ukrywamy animację przy dowolnej odpowiedzi
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(View.GONE);
            }

            if (line.startsWith("STATUS:OK")) {
                // Zamykamy port i zatrzymujemy serwis USB przed przejściem do dashboardu
                if (usbService != null) {
                    usbService.closePort();
                }
                Intent stopIntent = new Intent(getContext(), UsbSerialService.class);
                requireActivity().stopService(stopIntent);

                // Przejście do dashboardu
                if (getView() != null) {
                    Navigation.findNavController(getView()).navigate(R.id.action_configFragment_to_dashboardFragment);
                }
            } else if (line.startsWith("STATUS:ERROR_TIMEOUT")) {
                Toast.makeText(getContext(), "Błąd: Przekroczono czas połączenia", Toast.LENGTH_LONG).show();
            } else if (line.startsWith("STATUS:ERROR_FORMAT")) {
                Toast.makeText(getContext(), "Błąd: Niepoprawny format danych", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void onConnectionSuccess() {}

    @Override
    public void onConnectionError(String message) {
        if (getActivity() == null || !isAdded()) return;
        getActivity().runOnUiThread(() -> Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onPermissionRequested() {}
}