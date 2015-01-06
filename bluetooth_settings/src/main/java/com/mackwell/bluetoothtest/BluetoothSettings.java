package com.mackwell.bluetoothtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class BluetoothSettings extends ActionBarActivity {

    static final int REQUEST_ENABLE_BT = 100;
    static final String MY_UUID = "e296b5c6-959d-11e4-b100-123b93f75cba";
    static final String TAG = "Bluetooth Test";

    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;

    private SimpleAdapter mSimpleAdapter;
    private List<Map<String,String>> mDataList;
    private List<String> dataList;
    private Object[] pairedDevices;
    private ArrayAdapter<String> mArrayAdapter;
    private List<BluetoothDevice> deviceList;

    private ListView mListView;
    private Button connectButton;
    private Button sendButton;
    private EditText editText;

    private ConnectThread connectThread;
    private ConnectedThread dataThread;

    private int index;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                deviceList.add(device);

            }
        }
    };

    private void updateMDataList(){
        if(mDataList==null){
            mDataList = new ArrayList<Map<String, String>>();
        }else   mDataList.clear();

        Map map = null;

        if(pairedDevices.length>0){
            for(int i=0;i<pairedDevices.length;i++){
                map = new HashMap<String,String>();
                BluetoothDevice device = (BluetoothDevice) pairedDevices[i];
                map.put("name",device.getName());
                map.put("address",device.getAddress());
                mDataList.add(map);
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_settings);

        mHandler = new Handler();

        connectButton = (Button) findViewById(R.id.button3);
        sendButton = (Button) findViewById(R.id.button4);
        editText = (EditText) findViewById(R.id.editText);

        //register receiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        pairedDevices =  new Object[]{};
        updateMDataList();


        dataList = new ArrayList<>();
        dataList.add("test1");
        dataList.add("test2");

        mListView = (ListView) findViewById(R.id.listView);
        mArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_activated_1,dataList);
        mSimpleAdapter = new SimpleAdapter(this, mDataList,android.R.layout.simple_list_item_activated_2,new String[]{"name","address"},new int[]{android.R.id.text1,android.R.id.text2});

        mListView.setAdapter(mSimpleAdapter);
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                System.out.println(position);
                index = position;
            }
        });

        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth

        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_bluetooth_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void searchPaired(View view){

        pairedDevices =  mBluetoothAdapter.getBondedDevices().toArray();
// If there are paired devices
        if (pairedDevices.length > 0) {
            updateMDataList();
            mSimpleAdapter.notifyDataSetChanged();

        }

    }

    public void discovery(View view){
        mBluetoothAdapter.startDiscovery();

    }

    public void connect(View view){
        BluetoothDevice device = (BluetoothDevice) pairedDevices[index];
        if (connectThread == null) {
            connectThread = new ConnectThread(device);
            Log.d(TAG,Thread.currentThread().toString());

        }

        if(connectButton.getText().equals("Connect")){
            connectThread.start();
            connectButton.setEnabled(false);
        } else {
            connectThread.cancel();
            connectThread = null;
            Log.d(TAG,Thread.currentThread().toString());

        }

    }


    public void send(View view){
        Log.d(TAG,editText.getText().toString());

        if(dataThread!=null){
            dataThread.write(editText.getText().toString());
        }
        editText.setText("");
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(device.getUuids()[0].getUuid());

            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            Log.d(TAG,currentThread().toString());

            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                if (!mmSocket.isConnected()) {
                    mmSocket.connect();
                } else {
                    mmSocket.close();
                }

            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();

                } catch (IOException closeException) { }
                return;
            }

            if(mmSocket.isConnected()){
                Log.d(TAG, "bt device connected: " + mmSocket.isConnected());
                dataThread = new ConnectedThread(mmSocket);

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        connectButton.setText("Disconnect");
                        connectButton.setEnabled(true);

                    }
                });
            }

            // Do work to manage the connection (in a separate connectThread)
            //manageConnectedSocket(mmSocket);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                Log.d(TAG,currentThread().toString());
                mmSocket.close();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        connectButton.setText("Connect");
                    }
                });
            } catch (IOException e) { }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    //mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String message) {
            byte[] bytes = null;
            try {
                bytes = message.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}
