package com.example.androidapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.Manifest;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();

    private BluetoothAdapter mBluetoothAdapter;

    public static final int MULTIPLE_PERMISSIONS = 10; // code you want.

    //se crea un array de String con los permisos a solicitar en tiempo de ejecucion
    //Esto se debe realizar a partir de Android 6.0, ya que con verdiones anteriores
    //con solo solicitarlos en el Manifest es suficiente
    String[] permissions= new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(checkPermissions()){
            enableComponent();
        }

    }

    protected void enableComponent() {
        //Primero veo si soporta BT
        if(mBluetoothAdapter == null){
            //showUnsupported(); -->Esto es un metodo de nosotros
        }else {
            //Hacer algo
        }
    }

    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();

        //Se chequea si la version de Android es menor a la 7
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        for (String p:permissions) {
            result = ContextCompat.checkSelfPermission(this,p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),MULTIPLE_PERMISSIONS );
            return false;
        }
        return true;
    }
}

