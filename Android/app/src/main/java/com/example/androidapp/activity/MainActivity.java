package com.example.androidapp.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.Manifest;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.androidapp.R;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;

    public static final int MULTIPLE_PERMISSIONS = 10;
    private AlertDialog enableBluetoothDialog;

    //se crea un array de String con los permisos a solicitar en tiempo de ejecucion
    //Esto se debe realizar a partir de Android 6.0, ya que con verdiones anteriores
    //con solo solicitarlos en el Manifest es suficiente
    String[] permissions = new String[]
    {
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN
    };

    private Button btnTogglePump;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        showToast("onCreate");
        checkBleutooth();
        //Parte del sensor
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor sensorShake = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        SensorEventListener sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if(sensorEvent!=null){
                    float x_accl = sensorEvent.values[0];
                    float y_accl = sensorEvent.values[1];
                    float z_accl = sensorEvent.values[2];

                    if(x_accl>2 || x_accl<-2 || y_accl>2 || y_accl<-2){ //|| z_accl>2 || z_accl<-2){
                        showToast("Shaking!");
                    }else {
                        showToast("Not Shaking!");
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };

        sensorManager.registerListener(sensorEventListener,sensorShake,SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onResume() {
        showToast("onResume");
        checkBleutooth();
        super.onResume();
    }

    @Override
    protected void onStart() {
        showToast("onStart");
        checkBleutooth();
        super.onStart();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        showToast("onWindowFocusChanged");
        checkBleutooth();
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @SuppressLint("MissingPermission")
    private void checkBleutooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (checkPermissions()) {
            if (mBluetoothAdapter == null) {
                showUnsupported();
            } else {
                if (mBluetoothAdapter.isEnabled()/* && mBluetoothAdapter.getName().startsWith("AWA_")*/) {
                    showToast("Bluetooth activado");
                    deviceConected();
                } else {
                    showToast("Bluetooth desactivado");
                    showNotConnectedOrNotEnabled();
                }
            }
        }
    }

    private void deviceConected() {
        btnTogglePump = findViewById(R.id.btnTogglePump);
        btnTogglePump.setOnClickListener(togglePumpListener);
        if (enableBluetoothDialog != null && enableBluetoothDialog.isShowing()) {
            enableBluetoothDialog.dismiss();
        }
    }

    private void showNotConnectedOrNotEnabled() {
        if (enableBluetoothDialog != null && enableBluetoothDialog.isShowing()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View noBluetoothView = getLayoutInflater().inflate(R.layout.activity_bluetooth, null);
        builder.setView(noBluetoothView);

        Button btnActivateBluetooth = noBluetoothView.findViewById(R.id.btnActivarBt);
        btnActivateBluetooth.setOnClickListener(activarBluetoothRequestListener);

        Button btnCloseApp = noBluetoothView.findViewById(R.id.btnCloseApp);
        btnCloseApp.setOnClickListener(cerrarAppListener);

        enableBluetoothDialog = builder.create();
        enableBluetoothDialog.show();
    }

    private void showUnsupported() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View noBluetoothView = getLayoutInflater().inflate(R.layout.dialog_no_bluetooth, null);
        builder.setView(noBluetoothView);

        Button btnClose = noBluetoothView.findViewById(R.id.btnClose);
        btnClose.setOnClickListener(cerrarAppListener);

        AlertDialog dialog = builder.create();
        dialog.show();

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

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private final View.OnClickListener activarBluetoothRequestListener = new View.OnClickListener() {
        @SuppressLint("MissingPermission")
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
        }
    };

    private final View.OnClickListener cerrarAppListener = new View.OnClickListener() {
        @SuppressLint("MissingPermission")
        @Override
        public void onClick(View v) {
            finish();
        }
    };

    private final View.OnClickListener togglePumpListener = new View.OnClickListener() {
        @SuppressLint("MissingPermission")
        @Override
        public void onClick(View v) {
            showToast("togglePumpListener");
        }
    };

}



