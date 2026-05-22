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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.eia_app.R;
import com.example.eia_app.adapters.WifiAdapter;
import com.example.eia_app.models.WifiNetwork;
import com.example.eia_app.services.UsbSerialService;

import java.util.ArrayList;
import java.util.List;

public class ScanFragment extends Fragment implements UsbSerialService.ConnectionCallback, WifiAdapter.OnNetworkClickListener {

    private static final String TAG = "ScanFragment";

    private UsbSerialService usbService;
    private boolean isBound = false;

    private WifiAdapter adapter;
    private final List<WifiNetwork> wifiList = new ArrayList<>();

    public ScanFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Konfiguracja RecyclerView
        RecyclerView rvWifiList = view.findViewById(R.id.rvWifiList);
        rvWifiList.setLayoutManager(new LinearLayoutManager(getContext()));

        //dodanie dzialania przycisku i przekirowania do configu
        adapter = new WifiAdapter(wifiList, network ->{
            Bundle bundle = new Bundle();
            bundle.putString("selected_ssid",network.getSsid());

            Navigation.findNavController(view).navigate(
                    R.id.action_scanFragment_to_configFragment,
                    bundle
            );
        });
        rvWifiList.setAdapter(adapter);

        // Powrót do poprzedniego ekranu
        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            Navigation.findNavController(view).navigate(R.id.action_scanFragment_to_connectionFragment);
        });

        // Przycisk odświeżania sieci
        view.findViewById(R.id.btnRefresh).setOnClickListener(v -> startScanning());
    }

    private void startScanning() {
        if (isBound && usbService != null && usbService.isConnected()) {
            wifiList.clear();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            // Wysyłamy komendę skanowania do ESP
            usbService.sendCommand("scan\n");
        } else if (isBound && usbService != null) {
            // Jeśli niepołączone, spróbuj zainicjować
            usbService.initUSB();
        } else {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Urządzenie nie jest gotowe", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        adapter = null;
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            UsbSerialService.UsbBinder binder = (UsbSerialService.UsbBinder) service;
            usbService = binder.getService();
            isBound = true;

            // Rejestrujemy ten fragment jako odbiorcę wiadomości z serwisu
            usbService.setConnectionCallback(ScanFragment.this);

            // Automatyczne skanowanie przy wejściu (jeśli połączono, wyśle 'scan', jeśli nie, zrobi 'initUSB')
            startScanning();
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
        // Bindujemy serwis w onStart
        Intent intent = new Intent(getContext(), UsbSerialService.class);
        requireActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        // Odpinamy serwis w onStop, żeby nie generować wycieków pamięci
        if (isBound) {
            if (usbService != null) {
                usbService.setConnectionCallback(null); // Czyszczenie referencji
            }
            requireActivity().unbindService(serviceConnection);
            isBound = false;
        }
    }

    @Override
    public void onDataReceived(String line) {
        // Wszystko co przychodzi z serwisu, odbieramy tutaj.
        // Musi się to wykonać na głównym wątku (UI)
        if (getActivity() == null || !isAdded()) return;

        getActivity().runOnUiThread(() -> {
            if (adapter == null) return;
            Log.d(TAG, "Odebrano z ESP: " + line);

            // Założenie przetwarzania danych: format "SSID,RSSI" (np: "Home_WiFi,-72")
            // Dostosuj ten warunek pod to, jak dokładnie pluje Twoje ESP.
            if (line.contains(",")) {
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    String ssid = parts[0].trim();
                    String rssi = parts[1].trim();

                    // Zapobieganie dublowaniu na liście podczas jednego skanowania
                    boolean exists = false;
                    for (WifiNetwork net : wifiList) {
                        if (net.getSsid().equals(ssid)) {
                            exists = true;
                            break;
                        }
                    }

                    if (!exists) {
                        try {
                            int rssiVal = Integer.parseInt(rssi);
                            wifiList.add(new WifiNetwork(ssid, rssiVal, true));
                            adapter.notifyItemInserted(wifiList.size() - 1);
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Błąd parsowania RSSI: " + rssi);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onConnectionSuccess() {
        if (getActivity() == null || !isAdded()) return;
        getActivity().runOnUiThread(this::startScanning);
    }

    @Override
    public void onConnectionError(String message) {
        if (getActivity() == null || !isAdded()) return;
        getActivity().runOnUiThread(() ->
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public void onPermissionRequested() {
        Log.d(TAG, "Poproszono o uprawnienia USB...");
    }

    @Override
    public void onNetworkClick(WifiNetwork network) {
        // Po kliknięciu w sieć przechodzimy do konfiguracji
        Bundle bundle = new Bundle();
        bundle.putString("ssid", network.getSsid());
        Navigation.findNavController(requireView()).navigate(R.id.action_scanFragment_to_configFragment, bundle);
    }
}