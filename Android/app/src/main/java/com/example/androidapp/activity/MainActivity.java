package com.example.androidapp.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.Manifest;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.androidapp.R;
import com.example.androidapp.adapter.DeviceRVA;
import com.example.androidapp.model.Device;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;

    public static final int MULTIPLE_PERMISSIONS = 10;

    //se crea un array de String con los permisos a solicitar en tiempo de ejecucion
    //Esto se debe realizar a partir de Android 6.0, ya que con verdiones anteriores
    //con solo solicitarlos en el Manifest es suficiente
    String[] permissions = new String[]
    {
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private Button btnToggleBluetooth;
    private Button btnScan;
    private RecyclerView rvDevices;
    List<Device> devices = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rvDevices = (RecyclerView) findViewById(R.id.rvDevices);
        btnToggleBluetooth = (Button) findViewById(R.id.btnToggleBluetooth);
        btnScan = (Button) findViewById(R.id.btnScan);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (checkPermissions()) {
            enableComponent();

            DeviceRVA adapter = new DeviceRVA(devices, getApplication());
            rvDevices.setAdapter(adapter);
            rvDevices.setLayoutManager(new LinearLayoutManager(this));
            RecyclerView.ItemAnimator itemAnimator = new DefaultItemAnimator();
            itemAnimator.setAddDuration(500);
            itemAnimator.setRemoveDuration(500);
            rvDevices.setItemAnimator(itemAnimator);
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    protected void enableComponent() {
        //Primero veo si soporta BT
        if (mBluetoothAdapter == null) {
            showUnsupported();
        } else {
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(mReceiver, filter);
            checkEnabled();
        }
    }

    private void showUnsupported() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_no_bluetooth, null);
        builder.setView(dialogView);

        Button btnClose = dialogView.findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> {
            finish(); // Cierra la aplicación
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showDisabled() {
        btnScan.setEnabled(false);
        btnScan.setVisibility(View.INVISIBLE);
        rvDevices.setEnabled(false);
        rvDevices.setVisibility(View.INVISIBLE);
        btnToggleBluetooth.setText(R.string.activar_bt);
    }

    private void showEnabled() {
        btnScan.setEnabled(true);
        btnScan.setVisibility(View.VISIBLE);
        rvDevices.setEnabled(true);
        rvDevices.setVisibility(View.VISIBLE);
        btnToggleBluetooth.setText(R.string.desactivar_bt);
    }

    private void checkEnabled() {
        if (mBluetoothAdapter.isEnabled()) {
            showEnabled();
            btnToggleBluetooth.setOnClickListener(btnToggleBTListener);
            btnScan.setOnClickListener(btnScanListener);
        } else {
            showDisabled();
        }
    }

    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();

        //Se chequea si la version de Android es menor a la 7

        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        public void onReceive(Context context, Intent intent) {
            showToast("BroadcastReceiver");
            //Atraves del Intent obtengo el evento de Bluethoot que informo el broadcast del SO
            String action = intent.getAction();

            //Si cambio de estado el Bluethoot(Activado/desactivado)
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                //Obtengo el parametro, aplicando un Bundle, que me indica el estado del Bluetooth
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                checkEnabled();
                //Si esta activado
                if (state == BluetoothAdapter.STATE_ON) {
                    showEnabled();
                    showToast("Activado");
                } else if (state == BluetoothAdapter.STATE_OFF) {
                    showDisabled();
                    showToast("Desactivado");
                }
            }
            //Si se inicio la busqueda de dispositivos bluethoot
            else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                btnScan.setEnabled(false);
                showToast("Iniciando Busqueda");
            }
            //Si finalizo la busqueda de dispositivos bluethoot
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                btnScan.setEnabled(true);
                showToast("Busqueda Finalizada");
            }
            //si se encontro un dispositivo bluethoot
            else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                devices.add(0, new Device(device.getName(), device.getAddress(), device));
                rvDevices.getAdapter().notifyDataSetChanged();
                showToast("Dispositivo Encontrado:" + device.getName());
            }
        }
    };

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    @SuppressLint("MissingPermission")
    public void toggleBluetooth() {
        Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
        startActivity(intent);
    }

    private final View.OnClickListener btnToggleBTListener = new View.OnClickListener() {
        @SuppressLint("MissingPermission")
        @Override
        public void onClick(View v) {
            toggleBluetooth();
        }
    };

    private final View.OnClickListener btnScanListener = new View.OnClickListener() {
        @SuppressLint("MissingPermission")
        @Override
        public void onClick(View v) {
            toggleBluetooth();
        }
    };

}



