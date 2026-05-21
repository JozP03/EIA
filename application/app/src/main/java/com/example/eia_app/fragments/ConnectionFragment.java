package com.example.eia_app.fragments;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.eia_app.R;
import com.example.eia_app.services.UsbSerialService;

public class ConnectionFragment extends Fragment {

    private UsbSerialService usbService;
    private boolean isBound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            UsbSerialService.UsbBinder binder = (UsbSerialService.UsbBinder) service;
            usbService = binder.getService();
            isBound = true;
            setupCallback();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(requireContext(), UsbSerialService.class);
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (isBound) {
            requireContext().unbindService(serviceConnection);
            isBound = false;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_connection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnConnect).setOnClickListener(v -> {
            if (isBound && usbService != null) {
                usbService.initUSB();
            }
        });
    }

    private void setupCallback() {
        usbService.setConnectionCallback(new UsbSerialService.ConnectionCallback() {
            @Override
            public void onConnectionSuccess() {
                requireActivity().runOnUiThread(() -> {
                    if (getView() != null) {
                        Navigation.findNavController(getView()).navigate(R.id.action_connectionFragment_to_scanFragment);
                    }
                });
            }

            @Override
            public void onConnectionError(String message) {
                requireActivity().runOnUiThread(() -> 
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onPermissionRequested() {

            }
        });
    }
}
