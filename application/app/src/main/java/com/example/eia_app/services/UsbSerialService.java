package com.example.eia_app.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class UsbSerialService extends Service {
    public UsbSerialService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}