package com.example.androidapp.model;

import android.bluetooth.BluetoothDevice;

public class Device {
    public String title;
    public String address;
    public BluetoothDevice device;
    public Device(String title, String address, BluetoothDevice device) {
        this.title = title;
        this.address = address;
        this.device = device;
    }

}
