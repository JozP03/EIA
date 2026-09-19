package com.eia.app.fragments;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.eia.app.R;
import com.eia.app.models.Device;
import com.eia.app.services.UsbSerialService;
import com.eia.app.viewModels.DashboardViewModel;

public class DeviceSetupFragment extends Fragment implements UsbSerialService.ConnectionCallback {

    private static final String TAG = "DeviceSetupFragment";
    private UsbSerialService usbService;
    private boolean isBound = false;
    private String deviceId;
    private String ssid;
    private View loadingOverlay;
    private DashboardViewModel viewModel;

    private EditText etDeviceName, etMqttHost, etMqttPort, etMqttUser, etMqttPassword;

    public DeviceSetupFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            deviceId = getArguments().getString("deviceId");
            ssid = getArguments().getString("ssid");
        }
        viewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_device_setup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etDeviceName = view.findViewById(R.id.etDeviceName);
        etMqttHost = view.findViewById(R.id.etMqttHost);
        etMqttPort = view.findViewById(R.id.etMqttPort);
        etMqttUser = view.findViewById(R.id.etMqttUser);
        etMqttPassword = view.findViewById(R.id.etMqttPassword);
        loadingOverlay = view.findViewById(R.id.loadingOverlay);

        if (ssid != null) {
            etDeviceName.setText(getString(R.string.gateway_default_name, ssid));
        } else if (deviceId != null) {
            etDeviceName.setText(getString(R.string.gateway_default_name, deviceId));
        }

        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            Navigation.findNavController(view).popBackStack();
        });

        view.findViewById(R.id.btnSubmit).setOnClickListener(v -> {
            submitData();
        });
    }

    private void submitData() {
        String name = etDeviceName.getText().toString().trim();
        String host = etMqttHost.getText().toString().trim();
        String port = etMqttPort.getText().toString().trim();
        String user = etMqttUser.getText().toString().trim();
        String pass = etMqttPassword.getText().toString().trim();

        if (name.isEmpty() || host.isEmpty() || port.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.toast_fields_required), Toast.LENGTH_SHORT).show();
            return;
        }

        // Format: MQTT:server;port;user;pass\n
        String command = "MQTT:" + host + ";" + port + ";" + user + ";" + pass + "\n";

        if (isBound && usbService != null && usbService.isConnected()) {
            loadingOverlay.setVisibility(View.VISIBLE);
            usbService.sendCommand(command);
        } else {
            Toast.makeText(getContext(), getString(R.string.toast_usb_error), Toast.LENGTH_SHORT).show();
        }
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            UsbSerialService.UsbBinder binder = (UsbSerialService.UsbBinder) service;
            usbService = binder.getService();
            isBound = true;
            usbService.setConnectionCallback(DeviceSetupFragment.this);
            
            if (!usbService.isConnected()) {
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

            if (line.startsWith("STATUS:MQTT_CONFIG_SAVED")) {
                loadingOverlay.setVisibility(View.GONE);
                
                // Zapisujemy urządzenie z nową nazwą
                Device newDevice = new Device(deviceId, etDeviceName.getText().toString().trim());
                viewModel.saveDevice(newDevice);

                Toast.makeText(getContext(), getString(R.string.toast_config_saved), Toast.LENGTH_SHORT).show();

                // Zamykamy port i zatrzymujemy serwis
                if (usbService != null) {
                    usbService.closePort();
                }
                Intent stopIntent = new Intent(getContext(), UsbSerialService.class);
                requireActivity().stopService(stopIntent);

                // Przejście do dashboardu
                Navigation.findNavController(requireView()).navigate(R.id.action_deviceSetupFragment_to_dashboardFragment);
                
            } else if (line.startsWith("STATUS:ERROR")) {
                loadingOverlay.setVisibility(View.GONE);
                Toast.makeText(getContext(), getString(R.string.toast_error_device_save), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onConnectionSuccess() {
        Log.d(TAG, "USB połączone w DeviceSetupFragment");
    }

    @Override
    public void onConnectionError(String message) {
        if (getActivity() == null || !isAdded()) return;
        getActivity().runOnUiThread(() -> Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onPermissionRequested() {
        Log.d(TAG, "Uprawnienia USB wymagane w DeviceSetupFragment");
    }
}
