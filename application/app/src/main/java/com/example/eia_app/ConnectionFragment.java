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

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.navigation.Navigation;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ConnectionFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ConnectionFragment extends Fragment {

    private static final String ACTION_USB_PERMISSION = "com.example.eia_app.USB_PERMISSION";
    private UsbSerialPort usbPort;
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_USB_PERMISSION.equals(intent.getAction())) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            initUSB();
                        }
                    }
                }
            }
        }
    };

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ConnectionFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ConnectionFragment newInstance(String param1, String param2) {
        ConnectionFragment fragment = new ConnectionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_connection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // connect button
        view.findViewById(R.id.btnConnect).setOnClickListener(v -> {
            initUSB();
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            requireContext().unregisterReceiver(usbReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver not registered
        }
        if (usbPort != null) {
            try {
                usbPort.close();
            } catch (IOException ignored) {}
        }
    }

    // todo: zrobic to
    private void initUSB() {
        UsbManager manager = (UsbManager) requireContext().getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);

        if (availableDrivers.isEmpty()) {
            // brak urzadzen
            Toast.makeText(requireContext(), "Brak podłączonych urządzeń USB", Toast.LENGTH_SHORT).show();
            return;
        }

        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDevice device = driver.getDevice();

        if (!manager.hasPermission(device)) {
            //zapytanie dla uzytkownika
            Intent intent = new Intent(ACTION_USB_PERMISSION);
            intent.setPackage(requireContext().getPackageName());
            PendingIntent permissionIntent = PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_MUTABLE);

            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            ContextCompat.registerReceiver(requireContext(), usbReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);

            manager.requestPermission(device, permissionIntent);
            return;
        }

        try {
            UsbDeviceConnection connection = manager.openDevice(device);
            if (connection == null) {
                // Nie udało się otworzyć połączenia mimo uprawnień
                return;
            }

            usbPort = driver.getPorts().get(0);
            usbPort.open(connection);
            usbPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

            // przejscie do next ekranu
            //todo: do zmiany to zapewnie
            requireActivity().runOnUiThread(() -> {
                if (getView() != null) {
                    Navigation.findNavController(getView()).navigate(R.id.action_connectionFragment_to_scanFragment);
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            // Obsługa błędów wejścia/wyjścia
            if (usbPort != null) {
                try {
                    usbPort.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}