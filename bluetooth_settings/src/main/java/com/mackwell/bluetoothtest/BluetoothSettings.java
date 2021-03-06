package com.mackwell.bluetoothtest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.larswerkman.holocolorpicker.ColorPicker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class BluetoothSettings extends Activity implements BluetoothLongConnection.OnReceiveListener {

    @Override
    public void receive(List<Integer> rxBuffer) {
        System.out.println("rxBuffer size: " + rxBuffer.size());

        //popup dialog window
        TestDialogFragment testDialogFragment = new TestDialogFragment();
        testDialogFragment.show(getFragmentManager(), "testDialog");

        endTime = System.nanoTime();
        final DecimalFormat df = new DecimalFormat("#.00");
        final double timeElapsed = (endTime - startTime) / 1e9;

        this.rxBuffer.addAll(rxBuffer);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                textView.setText(BluetoothSettings.this.rxBuffer.size() + "/" + bytes_to_receive + " in " + df.format(timeElapsed) + "secs");
            }
        });

        if (rxBuffer.get(1) == 0xAD && (rxBuffer.get(2) == 0x29 || rxBuffer.get(2) == 0xA1)) {
            this.rxBuffer.clear();
        }

    }

    static final int REQUEST_ENABLE_BT = 100;
    static final String MY_UUID = "e296b5c6-959d-11e4-b100-123b93f75cba";
    static final String TAG = "Bluetooth Test";
    private static final long SCAN_PERIOD = 10000;

    private BluetoothAdapter mBluetoothAdapter;
    private boolean leScanning;
    private Handler mHandler;

    private SimpleAdapter mSimpleAdapter;
    private List<BluetoothDevice> bluetoothDeviceList;
    private List<Map<String, String>> mDataList;
    private List<String> dataList;
    private Object[] pairedDevices;
    private ArrayAdapter<String> mArrayAdapter;
    private List<BluetoothDevice> deviceList;

    private SeekBar mSeekBar;
    private ListView mListView;
    private Button connectButton;
    private Button onButton;
    private Button sendButton;
    private Button searchLeButton;
    private EditText editText;
    private ColorPicker colorPicker;
    private TextView textView;
    private MenuItem connectMenuItem;


    private ConnectThread connectThread;
    private ConnectedThread dataThread;
    private BluetoothLongConnection longConnection;
    private BluetoothGatt bluetoothGatt;

    private int index;
    private List<Integer> rxBuffer;
    private int bytes_to_receive = 0;
    private boolean flag = false;
    private long startTime = 0L;
    private long endTime = 0L;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                deviceList.add(device);

            } else if (BluetoothDevice.ACTION_UUID.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Parcelable[] uuids = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
                if (device.getUuids() != null) {
                    Log.d("TAG", "UUID found: " + uuids[0].toString());
                    if (connectThread != null) connectThread = null;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            connectMenuItem.setEnabled(true);
                        }
                    });

                } else {
                    device.fetchUuidsWithSdp();
                }
            }
        }
    };

    private BluetoothGattCallback bleGattCallback = new BluetoothGattCallback(){
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            bluetoothGatt.discoverServices();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d(TAG,characteristic.getValue().toString());
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            List<BluetoothGattService> services = gatt.getServices();
            for(BluetoothGattService service: services){
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
            }
//            bluetoothGatt.writeCharacteristic();
//            bluetoothGatt.


        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.d(TAG,characteristic.getValue().toString());
        }


    };

    private ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            BluetoothDevice device = result.getDevice();
            if(!bluetoothDeviceList.contains(device)) {
                bluetoothDeviceList.add(device);
                updateMDataList();
                mSimpleAdapter.notifyDataSetChanged();
            }

            int type = device.getType();
        }
    };


    private void updateMDataList(){
        if(mDataList==null){
            mDataList = new ArrayList<Map<String, String>>();
        }
        else   mDataList.clear();

        Map map = null;

        if(bluetoothDeviceList.size()>0){
            for(int i=0;i<bluetoothDeviceList.size();i++){
                map = new HashMap<String,String>();
                BluetoothDevice device = bluetoothDeviceList.get(i);
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

        rxBuffer = new ArrayList<>();
        mHandler = new Handler();

        bluetoothDeviceList = new ArrayList<BluetoothDevice>();

        mSeekBar = (SeekBar) findViewById(R.id.seekBar);
//        mSeekBar.setMax(255);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int value = seekBar.getProgress();
                char[] buffer = new char[10];
                buffer[0] = 0x02;
                buffer[1] = 0xA1;
                buffer[2] = 0x66;
                buffer[3] = (char)(value/2);

                if(longConnection!=null) longConnection.write(buffer);

            }
        });
        onButton = (Button) findViewById(R.id.buttonRed);
        onButton.setTextColor(Color.TRANSPARENT);
        textView = (TextView) findViewById(R.id.textView);
        connectButton = (Button) findViewById(R.id.button3);
        sendButton = (Button) findViewById(R.id.buttonRed);
        searchLeButton = (Button) findViewById(R.id.searchLE);
        editText = (EditText) findViewById(R.id.editText);
        colorPicker = (ColorPicker) findViewById(R.id.picker);
        colorPicker.setOnColorSelectedListener(new ColorPicker.OnColorSelectedListener() {
            @Override
            public void onColorSelected(int i) {
                colorPicker.setOldCenterColor(i);
                int red =  Color.red(colorPicker.getColor());
                int green =  Color.green(colorPicker.getColor());
                int blue =  Color.blue(colorPicker.getColor());

                Log.d(TAG,"R: " + Color.red(colorPicker.getColor()) + " G: " + Color.green(colorPicker.getColor()) + " B: " + Color.blue(colorPicker.getColor()));

                char[] buffer = new char[10];
                buffer[0] = 0x02;
                buffer[1] = 0xA1;
                buffer[2] = 0x65;
                buffer[3] = (char) (red/2);
                buffer[4] = (char) (green/2);
                buffer[5] = (char) (blue/2);

                if(longConnection!=null) longConnection.write(buffer);

            }
        });



        /*colorPicker.setOnColorChangedListener(new ColorPicker.OnColorChangedListener() {
            @Override
            public void onColorChanged(int i) {


                int red =  Color.red(colorPicker.getColor());
                int green =  Color.green(colorPicker.getColor());
                int blue =  Color.blue(colorPicker.getColor());

                Log.d(TAG,"R: " + Color.red(colorPicker.getColor()) + " G: " + Color.green(colorPicker.getColor()) + " B: " + Color.blue(colorPicker.getColor()));


                byte[] buffer = new byte[10];
                buffer[0] = 0;
                buffer[1] = (byte) (red/2);
                buffer[2] = (byte) (green/2);
                buffer[3] = (byte) (blue/2);

                dataThread.write(buffer);
            }
        });*/
        //register receiver
        IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_UUID);
        IntentFilter filter3 = new IntentFilter(BluetoothLeService.ACTION_GATT_CONNECTED);
        IntentFilter filter4 = new IntentFilter(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        IntentFilter filter5 = new IntentFilter(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        IntentFilter filter6 = new IntentFilter(BluetoothLeService.ACTION_DATA_AVAILABLE);

        registerReceiver(mReceiver, filter1);
        registerReceiver(mReceiver,filter2);
        registerReceiver(mReceiver,filter3);
        registerReceiver(mReceiver,filter4);
        registerReceiver(mReceiver,filter5);
        registerReceiver(mReceiver,filter6);


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
                //Intent intent = new Intent(BluetoothSettings.this,DeviceControlActivity.class);
                //intent.putExtra("device",bluetoothDeviceList.get(index));
                //startActivity(intent);
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
        connectMenuItem = menu.findItem(R.id.action_connect);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_list_device:
                searchPaired();
                 return true;
            case R.id.action_connect:
                connect();
                return true;
            case R.id.action_ft:
                ft();
                return true;
            case R.id.action_st:
                st();
                return true;
            case R.id.action_panel_init:
                getInit();
                return true;
            default: return super.onOptionsItemSelected(item);
        }


    }

    public void searchPaired(){

        pairedDevices =  mBluetoothAdapter.getBondedDevices().toArray();
// If there are paired devices
        Intent i = new Intent();


        if (pairedDevices.length > 0) {
            for(Object device: pairedDevices)
            {
                if(!bluetoothDeviceList.contains(device)){
                    bluetoothDeviceList.add((BluetoothDevice) device);
                    i.putExtra("s",(BluetoothDevice) device);
                }

            }
            updateMDataList();
            mSimpleAdapter.notifyDataSetChanged();

        }


    }

    public void discovery(View view){
        mBluetoothAdapter.startDiscovery();



    }

    public void searchLE(View view)
    {
        Button button = (Button) view;
        if("Start Search LE".equals(button.getText()))
        {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    leScanning = false;
                    mBluetoothAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
                    searchLeButton.setText("Start Search LE");
                }
            },SCAN_PERIOD);

            leScanning = true;
            mBluetoothAdapter.getBluetoothLeScanner().startScan(mLeScanCallback);
            searchLeButton.setText("Stop Search LE");

        }
        else{
            leScanning = false;
            mBluetoothAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
            searchLeButton.setText("Stop Search LE");
        }
    }




    public void connect(){
        BluetoothDevice device =  bluetoothDeviceList.get(index);

        if (device.getType()==BluetoothDevice.DEVICE_TYPE_CLASSIC && connectThread == null) {
            connectThread = new ConnectThread(device);
           // Log.d(TAG,Thread.currentThread().toString());

        }

        if(connectMenuItem.getTitle().equals("Connect")){
            switch(device.getType())
            {
                case BluetoothDevice.DEVICE_TYPE_LE:
                    bluetoothGatt = device.connectGatt(this, false, bleGattCallback);
                    break;
                case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                    if(connectThread!=null) connectThread.start();
                    break;
                default: break;
            }
            connectMenuItem.setEnabled(false);
        } else {
            if(connectThread!=null)connectThread.cancel();
            connectThread = null;
            //Log.d(TAG, Thread.currentThread().toString());

        }

    }


    public void send(View view){
        Log.d(TAG,editText.getText().toString());

        if(dataThread!=null){
            char[] buf = editText.getText().toString().toCharArray();
            if(dataThread!=null) dataThread.write(buf);
        }
        editText.setText("");
    }

    public void byteTest(View view)
    {
        startTime = System.nanoTime();
        Log.d(TAG,"FT");
        bytes_to_receive = 39;
        char command[] = {0x02,0xA1,0x54,0x00,0x78,0x7E,0x5A,0xA5,0x0D,0x0A};
        if(longConnection!=null) longConnection.write(command);

    }


    public  void button(View view){
        Button button = (Button) view;
        int position = 0;
        switch(button.getId()){
            case R.id.buttonRed: position = 1; break;
            case R.id.buttonGreen: position = 2; break;
            case R.id.buttonBlue: position = 3; break;

        }
        char[] buffer = null;

        if ("On".equals(button.getText())) {
             buffer = new char[]{0x02,0xA1,0x63,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
            button.setText("Off");

            button.setBackgroundResource(R.drawable.off);
        }else{
            buffer = new char[]{0x02,0xA1,0x64,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
            button.setText("On");
            button.setBackgroundResource(R.drawable.on);
        }

     if(longConnection!=null) longConnection.write(buffer);

 }

    public void ft()
    {
        startTime = System.nanoTime();
        Log.d(TAG,"FT");
        bytes_to_receive = 39;
        char command[] = {0x02,0xA1,0x60,0x00,0x78,0x7E,0x5A,0xA5,0x0D,0x0A};
        if(longConnection!=null) longConnection.write(command);

    }

    public void st()
    {
        startTime = System.nanoTime();
        Log.d(TAG,"ST");
        bytes_to_receive = 39;
        char command[] = {0x02,0xA1,0x62,0x00,0x79,0x1E,0x5A,0xA5,0x0D,0x0A};
        if(longConnection!=null) longConnection.write(command);


    }

    public void getInit()
    {
        startTime = System.nanoTime();
        bytes_to_receive = 16784;
        Log.d(TAG,"GetPanelInformation");
        char command[] = {0x02,0xA0,0x21,0x68,0x18,0x5A,0xA5,0x0D,0x0A};
        if(longConnection!=null) longConnection.write(command);

    }



    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice  mDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                ParcelUuid[] uuidArray = device.getUuids();
                if(uuidArray!=null) {
                    UUID deviceUUID = uuidArray[0].getUuid();
                    tmp = device.createRfcommSocketToServiceRecord(deviceUUID);

                }else{
                    Log.d(TAG,"Searching UUID");
                    mBluetoothAdapter.cancelDiscovery();
                    device.fetchUuidsWithSdp();
                }

            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            Log.d(TAG,currentThread().toString());

            if (mmSocket!=null) {
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
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(BluetoothSettings.this, "Connection + Failed, please try again", Toast.LENGTH_SHORT).show();
                                connectMenuItem.setEnabled(true);
                                connectThread = null;
                            }
                        });
                        mmSocket.close();

                    } catch (IOException closeException) { }
                    return;
                }

                if(mmSocket.isConnected()){
                    Log.d(TAG, "bt device connected: " + mmSocket.isConnected());
                    longConnection = new BluetoothLongConnection(mmSocket,BluetoothSettings.this);
                    longConnection.start();
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            connectMenuItem.setTitle("Disconnect");
                            connectMenuItem.setEnabled(true);

                            //disabled for LMC5 07/05/2015
                            colorPicker.setVisibility(View.VISIBLE);
                            onButton.setVisibility(View.VISIBLE);
                            mSeekBar.setVisibility(View.VISIBLE);
                        }
                    });
                }
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
                        connectMenuItem.setTitle("Connect");
                        colorPicker.setVisibility(View.INVISIBLE);
                        onButton.setVisibility(View.INVISIBLE);
                        mSeekBar.setVisibility(View.INVISIBLE);
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
            System.out.println("bluetooth thread starts listening");
            //byte[] buffer = new byte[1024];  // buffer store for the stream
            ArrayList<Integer> buffer = new ArrayList<Integer>();
            int len= -1;
            int bytes_received = 0;
            ArrayList<Byte> buf = new ArrayList<Byte>(100);
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    int data;
                    while(mmInStream.available()>0)
                    {
                        data = mmInStream.read();
                        buffer.add(data);
                        bytes_received++;
                        //System.out.print(data + " ");
                        //TimeUnit.MILLISECONDS.sleep(10);
                    }

                    //System.out.flush();

                    /*while((len = mmInStream.read(buffer)) != -1){
                        //Log.d(TAG, "Received bytes: "  + len);
                        bytes_received += len;

//                        System.out.print(mmInStream.read() + " ");

                    }*/

                    final int finalBytes_received = bytes_received;  //final copy of bytes_received

                    // Send the obtained bytes to the UI activity
                    //mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            textView.setText(finalBytes_received + "/16784");
                        }
                    });


                    buffer.clear();
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
}
