package com.group.unkown.positioningminiproject.ui;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.group.unkown.positioningminiproject.R;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import Model.BuildingModel;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;

    private List<ScanResult> mDevices;
    private List<BluetoothGatt> mConnections;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    private static final long SCAN_PERIOD = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDevices = new ArrayList<>();
        mConnections = new ArrayList<>();
        mHandler = new Handler();

        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        scanDevices(true);

        InputStream ins = getResources().openRawResource(getResources().getIdentifier("beacons", "raw", getPackageName()));
        BuildingModel model = new BuildingModel(ins);

        ((TextView)findViewById(R.id.tv_text)).setText((model.getBeacon("f7826da6-4fa2-4e98-8024-bc5b71e0893e").getRoomName()));
    }

    private void scanDevices(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.getBluetoothLeScanner().startScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
        }
    }

    private ScanCallback mLeScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            synchronized (MainActivity.this) {
                mDevices.add(result);
            }
            result.getDevice().connectGatt(MainActivity.this, true, mBluetoothGattCallback);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                synchronized (MainActivity.this) {
                    mDevices.add(result);
                }
                result.getDevice().connectGatt(MainActivity.this, true, mBluetoothGattCallback);
            }
        }

    };

    private BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    synchronized (MainActivity.this) {
                        mConnections.add(gatt);
                    }
                    break;

                case BluetoothProfile.STATE_DISCONNECTED:
                    synchronized (MainActivity.this) {
                        mConnections.remove(gatt);
                        mDevices.remove(gatt.getDevice());
                    }
                    break;
            }
        }
    };

}
