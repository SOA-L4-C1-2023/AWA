package com.example.androidapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.Manifest;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();

    private BluetoothAdapter mBluetoothAdapter;

    public static final int MULTIPLE_PERMISSIONS = 3; //10 original, code you want.

    //se crea un array de String con los permisos a solicitar en tiempo de ejecucion
    //Esto se debe realizar a partir de Android 6.0, ya que con verdiones anteriores
    //con solo solicitarlos en el Manifest es suficiente
    String[] permissions = new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT};
            //Manifest.permission.ACCESS_COARSE_LOCATION};
    //Manifest.permission.ACCESS_COARSE_LOCATION,
    //Manifest.permission.WRITE_EXTERNAL_STORAGE,
    //Manifest.permission.READ_PHONE_STATE,
    //Manifest.permission.READ_EXTERNAL_STORAGE};
    private TextView txtResultadoEstado;
    private Button btnConectar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtResultadoEstado = (TextView) findViewById(R.id.txtResultadoEstado);
        btnConectar = (Button) findViewById(R.id.btnConectar);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothAdapter.enable();
        if (checkPermissions()) {
            enableComponent();
        }

    }

    protected void enableComponent() {
        //Primero veo si soporta BT
        if (mBluetoothAdapter == null) {
            showUnsupported(); //Aca no soporta el celu el BT
        } else {
            if (mBluetoothAdapter.isEnabled()) {
                showSupported(); //Si lo soporta...
                btnConectar.setOnClickListener(btnConectarListener);
            } else {
                showUnsupported();
            }
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(mReceiver, filter);
        }
    }

    private void showSupported() {
        txtResultadoEstado.setText("BT soportado");
        btnConectar.setEnabled(true);
    }

    private void showUnsupported() {
        txtResultadoEstado.setText("BT no soportado");
        btnConectar.setEnabled(false);
    }

    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();

        //Se chequea si la version de Android es menor a la 7
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

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
        public void onReceive(Context context, Intent intent) {

            //Atraves del Intent obtengo el evento de Bluethoot que informo el broadcast del SO
            String action = intent.getAction();

            //Si cambio de estado el Bluethoot(Activado/desactivado)
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                //Obtengo el parametro, aplicando un Bundle, que me indica el estado del Bluethoot
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                //Si esta activado
                if (state == BluetoothAdapter.STATE_ON) {
                    showToast("Activar");

                    showEnabled();
                }
            }
            //Si se inicio la busqueda de dispositivos bluethoot
            else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                //Creo la lista donde voy a mostrar los dispositivos encontrados
                mDeviceList = new ArrayList<BluetoothDevice>();

                //muestro el cuadro de dialogo de busqueda
                //mProgressDlg.show();
            }
            //Si finalizo la busqueda de dispositivos bluethoot
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //se cierra el cuadro de dialogo de busqueda
                // mProgressDlg.dismiss();

                //se inicia el activity DeviceListActivity pasandole como parametros, por intent,
                //el listado de dispositovos encontrados
                //Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);

                //newIntent.putParcelableArrayListExtra("device.list", mDeviceList);

                //startActivity(newIntent);
            }
            //si se encontro un dispositivo bluethoot
            else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //Se lo agregan sus datos a una lista de dispositivos encontrados
                //BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                //mDeviceList.add(device);
                //showToast("Dispositivo Encontrado:" + device.getName());
            }
        }
    };

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private View.OnClickListener btnConectarListener = new View.OnClickListener() {

        @SuppressLint("MissingPermission")
        @Override
        public void onClick(View v) {
            //mBluetoothAdapter.enable();
            if (mBluetoothAdapter.isEnabled()) {
                showToast("Deshabilitado");
                mBluetoothAdapter.disable();

            }else{
                Intent intent = new Intent(mBluetoothAdapter.ACTION_REQUEST_ENABLE);
                showToast("OK");
                startActivityForResult(intent, 1000);
            }
        }
    };

}



