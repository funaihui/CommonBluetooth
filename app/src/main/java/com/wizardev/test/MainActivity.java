package com.wizardev.test;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT=1111;
    private ListView mLvConnectedDevices,mLvSearchDevices;
    private ArrayAdapter<String> mConnectedAdapter;
    private ArrayAdapter<String> mSearchAdapter;
    private Button btnStartSearch,btnOpenSearch,btnSendMessage;
    private BluetoothService mBluetoothService;
    private List<BluetoothDevice> mBluetoothDevices = new ArrayList<>();
    private static final String TAG = "BluetoothChatService";
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:

                            Log.d(TAG, "STATE_CONNECTED: ");
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            Log.d(TAG, "STATE_CONNECTING: ");

                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            Log.d(TAG, "STATE_NONE: ");

                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    Log.d(TAG, "writeMessage: "+writeMessage);

                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Toast.makeText(MainActivity.this,readMessage,Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    msg.getData().getString(Constants.DEVICE_NAME);
                    Log.d(TAG, "handleMessage: "+msg.getData().getString(Constants.DEVICE_NAME));
                    break;
                case Constants.MESSAGE_TOAST:

                        Toast.makeText(MainActivity.this, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();

                    break;
            }

        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
        openBlueTooth();
        registerReceiver(mReceiver, filter);
        mBluetoothService = new BluetoothService(this,mHandler);
        mBluetoothService.start();
    }

    private void initView() {
        mLvConnectedDevices = findViewById(R.id.lvConnectedDevices);
        mLvSearchDevices = findViewById(R.id.lvSearchDevices);
        btnStartSearch = findViewById(R.id.btnStartSearch);
        btnOpenSearch = findViewById(R.id.btnOpenSearch);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        btnStartSearch.setOnClickListener(this);
        btnOpenSearch.setOnClickListener(this);
        btnSendMessage.setOnClickListener(this);
        mConnectedAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        mSearchAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        mLvConnectedDevices.setAdapter(mConnectedAdapter);
        mLvSearchDevices.setAdapter(mSearchAdapter);

        mLvSearchDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                BluetoothDevice bluetoothDevice = mBluetoothDevices.get(i);
                if (bluetoothDevice != null) {
                    Toast.makeText(MainActivity.this,"开始连接",Toast.LENGTH_SHORT).show();
                    mBluetoothService.connect(bluetoothDevice);

                }
            }
        });

        mLvConnectedDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                BluetoothDevice bluetoothDevice = mBluetoothDevices.get(i);
                if (bluetoothDevice != null) {
                    Toast.makeText(MainActivity.this,"开始连接",Toast.LENGTH_SHORT).show();
                    mBluetoothService.connect(bluetoothDevice);

                }
            }
        });
    }

    private void initData() {

    }

    private void openBlueTooth() {
        if (haveBluetooth()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                checkAlreadyConnect();
            }
        }
    }

    private boolean haveBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this,"当前设备不支持蓝牙！",Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this,"成功打开蓝牙！",Toast.LENGTH_SHORT).show();
                checkAlreadyConnect();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this,"打开蓝牙失败！",Toast.LENGTH_SHORT).show();

            }
        }
    }

    private void checkAlreadyConnect() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                mConnectedAdapter.add(device.getName() + "\n" + device.getAddress());
                mBluetoothDevices.add(device);

            }
        }
        mConnectedAdapter.notifyDataSetChanged();
    }


    // 新建一个 BroadcastReceiver来接收ACTION_FOUND广播
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // 发现设备
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //获得 BluetoothDevice
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mBluetoothDevices.add(device);
                //向mArrayAdapter中添加设备信息
                mSearchAdapter.add(device.getName() + "\n" + device.getAddress());
                mSearchAdapter.notifyDataSetChanged();
            }
        }
    };
    //设置IntentFilter
    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        if (mBluetoothService != null) {
            mBluetoothService.stop();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnStartSearch:
                if (mBluetoothAdapter != null) {
                    if (mBluetoothDevices != null) {
                        mBluetoothDevices.clear();
                    }
                    if (mSearchAdapter != null) {
                        mSearchAdapter.clear();
                    }

                    boolean b = mBluetoothAdapter.startDiscovery();
                    if (b) {
                        Toast.makeText(this,"开始扫描设备",Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case R.id.btnOpenSearch:
                openSearch();
                break;
                case R.id.btnSendMessage:
                sendMessage("hello");
                break;
        }

    }

    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mBluetoothService.getState() != mBluetoothService.STATE_CONNECTED) {
            Toast.makeText(this,"没有连接！", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mBluetoothService.write(send);

            // Reset out string buffer to zero and clear the edit text field
//            mOutStringBuffer.setLength(0);
//            mOutEditText.setText(mOutStringBuffer);
        }
    }

    private void openSearch() {
        Intent discoverableIntent = new
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
    }
}
