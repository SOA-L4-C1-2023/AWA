package com.example.androidapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

public class ComunicationActivity {
    //Recursos varios

    //Handler
    Handler bluetoothIN;
    final int handlerState=0; //used to identify handler message

    //Variables propias
    private BluetoothAdapter btAdapter;
    private BluetoothSocket btSocket;
    private StringBuilder recDataString = new StringBuilder();

    //Recursos para el hilo
    //private ConnectedThread mConnectedThread;

}
