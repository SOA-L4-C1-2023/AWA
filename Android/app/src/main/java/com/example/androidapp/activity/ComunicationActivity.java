package com.example.androidapp.activity;

import androidx.appcompat.app.AppCompatActivity;
import static android.content.Intent.getIntent;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import android.os.Bundle;
//Hilo Principal
public class ComunicationActivity extends AppCompatActivity
{
    //Handler
    Handler bluetoothIN;
    final int handlerState = 0; //used to identify handler message

    //Recursos del BT
    private BluetoothAdapter btAdapter;
    private BluetoothSocket btSocket;
    private StringBuilder recDataString = new StringBuilder();

    //Recursos para manejo de thread
    private ConnectedThread mConnectedThread;

    // SPP UUID service  - Funciona en la mayoria de los dispositivos
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // String for MAC address del Hc05
    private static String address = null;
    private Message msg;
    private Button btnConectar;

    //Eventos Propios de la aplicación.
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_comunication);
        //obtengo el adaptador del bluethoot
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        //defino el Handler de comunicacion entre el hilo Principal y el secundario.
        //El hilo secundario va a mostrar informacion al layout atraves utilizando indeirectamente a este handler
        bluetoothIN = MainThreadMsgHandler();
        //Bindeo el boton conectar acá
        //btnConectar=(Button)findViewById(R.id.btnConectar);
        btnConectar.setOnClickListener(btnConectarListener);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onResume()
    {
        super.onResume();
        //Obtengo el parametro, aplicando un Bundle, que me indica la Mac Adress del HC05
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        address = "8c:f1:12:42:65:55"; //extras.getString("DireccionBT"); //->Aca meter la MAC del HC05
        BluetoothDevice device = btAdapter.getRemoteDevice(address);
        try
        {
            btSocket = createBTSocket(device);
        }
        catch (IOException e)
        {
            showToast("La creacion del Socket fallo");
        }
        //Suponiendo que sale todo bien por acá
        try
        {
            btSocket.connect();
        }
        catch (IOException e)
        {
            try
            {
                btSocket.close();
            }
            catch (IOException e2)
            {
                //Deal with this...
            }
        }
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();
        //I send a character when resuming.beginning transmission to check device is connected
        //If it is not an exception will be thrown in the write method and finish() will be called
        mConnectedThread.write("x");
    }

    @Override
    public void onPause()
    {
        super.onPause();
        try
        {
            btSocket.close(); //Siempre cerrar cuando esta en pausa
        }
        catch(IOException e)
        {
            //Do something to catch this...
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        try
        {
            btSocket.close(); //Siempre cerrar cuando esta en pausa
        }
        catch(IOException e)
        {
            //Do something to catch this...
        }
    }
    //---------------------Recursos privados---------------------------------
    private void showToast(String message)
    {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("MissingPermission")
    private BluetoothSocket createBTSocket(BluetoothDevice device) throws IOException
    {
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    private Handler MainThreadMsgHandler()
    {
        return new Handler()
        {
            public void handleMessage(android.os.Message msg) //Y de donde salio esto???
            {
                //si se recibio un msj del hilo secundario
                if (msg.what == handlerState)
                {
                    //voy concatenando el msj
                    String readMessage = (String) msg.obj;
                    recDataString.append(readMessage);
                    int endOfLineIndex = recDataString.indexOf("\r\n");

                    //cuando recibo toda una linea la muestro en el layout
                    if (endOfLineIndex > 0)
                    {
                        String dataInPrint = recDataString.substring(0, endOfLineIndex);
                        //txtPotenciometro.setText(dataInPrint); //Reemplazar por lo que corresponde?

                        recDataString.delete(0, recDataString.length());
                    }
                }
            }
        };
    }
    private View.OnClickListener btnConectarListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            //mConnectedThread.write("a");
            showToast("LED Blanco encendido");
        }
    };

    //Hilo Secundario del Thread
    private class ConnectedThread extends Thread
    {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //Constructor de la clase del hilo secundario
        public ConnectedThread(BluetoothSocket socket)
        {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try
            {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            }
            catch (IOException e)
            {

            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        //metodo run del hilo, que va a entrar en una espera activa para recibir los msjs del HC05
        public void run()
        {
            byte[] buffer = new byte[256];
            int bytes;

            //el hilo secundario se queda esperando mensajes del HC05
            while (true)
            {
                try
                {
                    //se leen los datos del Bluethoot
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);

                    //se muestran en el layout de la activity, utilizando el handler del hilo
                    // principal antes mencionado
                    bluetoothIN.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                }
                catch (IOException e)
                {
                    break;
                }
            }
        }
    //write method
    public void write(String input)
    {
        byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
        try
        {
            mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
        }
        catch (IOException e)
        {
            //if you cannot write, close the application
            showToast("La conexion fallo");
            finish();
        }
    }
    }
}