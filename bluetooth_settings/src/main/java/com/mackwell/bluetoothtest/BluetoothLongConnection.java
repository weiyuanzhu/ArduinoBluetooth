package com.mackwell.bluetoothtest;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Weiyuan on 30/01/2015.
 */
public class BluetoothLongConnection  extends Thread implements Serializable{

    public static final int UART_STOP_BIT_H = 0x5A;
    public static final int UART_STOP_BIT_L = 0xA5;
    public static final int UART_NEW_LINE_H = 0x0D;
    public static final int UART_NEW_LINE_L = 0x0A;

    public interface OnReceiveListener {
        public void receive(List<Integer> rxBuffer);
    }

    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private OnReceiveListener mListener;

    public BluetoothLongConnection(BluetoothSocket socket,OnReceiveListener listener) {
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        mListener = listener;

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    public void run() {
        System.out.println("bluetooth thread starts listening");
        ArrayList<Integer> rxBuffer = new ArrayList<Integer>();
        int bytes_received = 0;

        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                int data = 0;
                while (mmInStream.available() > 0) {
                    data = mmInStream.read();
                    rxBuffer.add(data);
                    bytes_received++;

                    if(!rxBuffer.isEmpty() && (data == UART_NEW_LINE_L) &&
                            rxBuffer.get(rxBuffer.size() - 2).equals(UART_NEW_LINE_H) &&
                            rxBuffer.get(rxBuffer.size() - 3).equals(UART_STOP_BIT_L) &&
                            rxBuffer.get(rxBuffer.size() - 4).equals(UART_STOP_BIT_H))   // check finished bit; to be changed
                    {
                        mListener.receive(rxBuffer);
                        rxBuffer.clear();
                    }
                    //System.out.print(data + " ");
                    //TimeUnit.MILLISECONDS.sleep(10);
                }


                //pass rxBuffer to listener when it reaches stop bytes


            } catch (IOException e) {
                break;
            }
        }

    }

    /* Call this from the main activity to send data to the remote device */
    public void write(char[] command) {
        //byte[] bytes = null;
            /*try {
                bytes = message.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }*/

        byte[] buffer = new byte[command.length];

        for(int i=0;i<command.length;i++)
        {
            buffer[i] = (byte) command[i];
        }

        try {
            mmOutStream.write(buffer);
        } catch (IOException e) {

        }
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}
