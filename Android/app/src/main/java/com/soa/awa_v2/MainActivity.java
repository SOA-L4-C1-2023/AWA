package com.soa.awa_v2;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener
{
    //-------------------------------------------
    Handler bluetoothIn;
    final int handlerState = 0;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder DataStringIN = new StringBuilder();
    private ConnectedThread MyConexionBT;
    // Identificador unico de servicio - SPP UUID
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // String para la direccion MAC
    private static String address = null;
    //-------------------------------------------
    //Declaracion de botones
    Button btnLed, btnNivelDeAgua;
    TextView txtAgua;
    //------------------------------------------
    //Declaracion de sensores
    private SensorManager sensorManager;
    private Sensor accelerometer;
    //------------------------------------------
    String nivelDeAgua = "";
    private final String request_led="A";
    private final String request_water_level="B";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothIn = new Handler()
        {
            public void handleMessage(android.os.Message msg)
            {
                txtAgua = findViewById(R.id.txtAgua);
                if (msg.what == handlerState)
                {
                    nivelDeAgua = nivelDeAgua + msg.obj.toString();
                    if (nivelDeAgua.contains("\r\n"))
                    {
                        txtAgua.setText(nivelDeAgua);
                        nivelDeAgua = "";
                    }
                }
            }
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        VerificarEstadoBT();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        led();
        nivelDeAgua();


    }

    //Zona del BT
    @SuppressLint("MissingPermission")
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException
    {
        //creates secure outgoing connecetion with BT device using UUID
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    public void onResume()
    {
        super.onResume();

        sensorManager.registerListener((SensorEventListener) this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        Intent intent = getIntent();
        address = intent.getStringExtra(DispositivosVinculados.EXTRA_DEVICE_ADDRESS);
        //Setea la direccion MAC
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try
        {
            btSocket = createBluetoothSocket(device);
        }
        catch (IOException e)
        {
            Toast.makeText(getBaseContext(), "La creacción del Socket fallo", Toast.LENGTH_LONG).show();
        }
        // Establece la conexión con el socket Bluetooth.
        try
        {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
            {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                btSocket.connect();
            }
        }
        catch (IOException e)
        {
            try
            {
                btSocket.close();
            }
            catch (IOException e2)
            {
                new RuntimeException(e2);
            }
        }
        MyConexionBT = new ConnectedThread(btSocket,bluetoothIn,handlerState);
        MyConexionBT.start();
        Toast.makeText(this, address, Toast.LENGTH_SHORT).show();
    }



    private void led()
    {
        btnLed = findViewById(R.id.btnLed);
        btnLed.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //Enviamos la letra para prender led al arduino
                MyConexionBT.write(request_led);
            }
        }
        );
    }

    private void nivelDeAgua()
    {
        btnNivelDeAgua = findViewById(R.id.btnNivelDeAgua);
        btnNivelDeAgua.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                MyConexionBT.write(request_water_level);
            }
        }
        );
    }

    @Override
    public void onPause()
    {
        super.onPause();
        try
        { // Cuando se sale de la aplicación esta parte permite que no se deje abierto el socket
            btSocket.close();
        }
        catch (IOException e2)
        {
            new RuntimeException(e2);
        }
    }

    //Comprueba que el dispositivo Bluetooth
    //está disponible y solicita que se active si está desactivado
    private void VerificarEstadoBT()
    {
        if (btAdapter == null)
        {
            Toast.makeText(getBaseContext(), "El dispositivo no soporta bluetooth", Toast.LENGTH_LONG).show();
        }
        else
        {
            if (!btAdapter.isEnabled())
            {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    startActivityForResult(enableBtIntent, 1);
                }
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
        {
            // Obtener los valores del acelerómetro
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            double magnitude = Math.sqrt(x * x + y * y + z * z);
            if (magnitude > 15)
            {
                Toast.makeText(this, "¡Shake detectado!", Toast.LENGTH_SHORT).show();
                MyConexionBT.write(request_led);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }
    //Zona del BT
}