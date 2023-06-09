package com.soa.awa_v2;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ConnectedThread extends Thread
{
    //Crea la clase que permite crear el evento de conexion
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private final String default_error="-1";

    Handler bluetoothIn;
    private int handlerState = 0;

    public ConnectedThread(BluetoothSocket socket, Handler bluetoothIn, int handlerState)
    {
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        this.bluetoothIn=bluetoothIn;
        this.handlerState=handlerState;
        try
        {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        }
        catch (IOException e)
        {

        }
        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    public void run()
    {
        byte[] byte_in = new byte[50];
        // Se mantiene en modo escucha para determinar el ingreso de datos
        while (true)
        {
            try
            {
                mmInStream.read(byte_in);
                String cadena = new String(byte_in, 0, 50);
                bluetoothIn.obtainMessage(handlerState, cadena).sendToTarget();
            }
            catch (IOException e)
            {
                break;
            }
        }
    }

    public void write(String input)
    {
        try
        {
            mmOutStream.write(input.getBytes());
        }
        catch (IOException e)
        {
            //si no es posible enviar datos se cierra la conexión
            try
            {
                mmOutStream.close();
            }
            catch (IOException ex)
            {
                throw new RuntimeException(ex);
            }
        }
    }

    public String read()
    {
        byte[] buffer = new byte[1024];
        try
        {
            int byteReads = mmInStream.read(buffer);
            return new String(buffer, 0, byteReads);
        }
        catch (IOException e)
        {
            //si no es posible enviar datos se cierra la conexión
            try
            {
                mmInStream.close();
            }
            catch (IOException ex)
            {
                throw new RuntimeException(ex);
            }
        }
        return default_error;
    }
}
