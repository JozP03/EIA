package com.example.eia_app.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;

public class UsbSerialService extends Service {
    private static final String TAG = "UsbSerialService";
    private static final String ACTION_USB_PERMISSION = "com.example.eia_app.USB_PERMISSION";

    private final IBinder binder = new UsbBinder();
    private UsbSerialPort usbPort;
    private UsbManager usbManager;
    private ConnectionCallback connectionCallback;

    public interface ConnectionCallback {
        void onConnectionSuccess();
        void onConnectionError(String message);
        void onPermissionRequested();
    }

    public class UsbBinder extends Binder {
        public UsbSerialService getService() {
            return UsbSerialService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setConnectionCallback(ConnectionCallback callback) {
        this.connectionCallback = callback;
    }

    public void initUSB() {
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);

        if (availableDrivers.isEmpty()) {
            if (connectionCallback != null) connectionCallback.onConnectionError("Brak podłączonych urządzeń USB");
            return;
        }

        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDevice device = driver.getDevice();

        if (!usbManager.hasPermission(device)) {
            Intent intent = new Intent(ACTION_USB_PERMISSION);
            intent.setPackage(getPackageName());
            PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_MUTABLE);

            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            ContextCompat.registerReceiver(this, usbReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);

            usbManager.requestPermission(device, permissionIntent);
            if (connectionCallback != null) connectionCallback.onPermissionRequested();
            return;
        }

        connectToDevice(driver);
    }

    private void connectToDevice(UsbSerialDriver driver) {
        try {
            UsbDeviceConnection connection = usbManager.openDevice(driver.getDevice());
            if (connection == null) {
                if (connectionCallback != null) connectionCallback.onConnectionError("Nie udało się otworzyć połączenia");
                return;
            }

            usbPort = driver.getPorts().get(0);
            usbPort.open(connection);
            usbPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

            Log.d(TAG, "USB Connected successfully");
            if (connectionCallback != null) connectionCallback.onConnectionSuccess();

        } catch (IOException e) {
            Log.e(TAG, "Error connecting to USB", e);
            if (connectionCallback != null) connectionCallback.onConnectionError("Błąd: " + e.getMessage());
            closePort();
        }
    }

    public void closePort() {
        if (usbPort != null) {
            try {
                usbPort.close();
            } catch (IOException ignored) {}
            usbPort = null;
        }
    }

    public boolean isConnected() {
        return usbPort != null && usbPort.isOpen();
    }

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_USB_PERMISSION.equals(intent.getAction())) {
                synchronized (this) {
                    unregisterReceiver(this);
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            initUSB();
                        }
                    } else {
                        if (connectionCallback != null) connectionCallback.onConnectionError("Odmowa uprawnień USB");
                    }
                }
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        closePort();
    }
}
