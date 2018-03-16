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
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.group.unkown.positioningminiproject.R;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import Model.BuildingModel;

public class BluetoothActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;

    private List<ScanResult> mDevices;
    private List<BluetoothGatt> mConnections;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    private static final long SCAN_PERIOD = 100000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

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

        InputStream ins = getResources().openRawResource(getResources().getIdentifier("beacons", "raw", getPackageName()));
        BuildingModel model = new BuildingModel(ins);

        ((TextView)findViewById(R.id.tv_text)).setText((model.getBeacon("f7826da6-4fa2-4e98-8024-bc5b71e0893e").getRoomName()));

        scanDevices(true);
    }

    private void scanDevices(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
                    appendToTextView("Stopped Scan");
                }
            }, SCAN_PERIOD);

            appendToTextView("Started Scan");
            mScanning = true;
            mBluetoothAdapter.getBluetoothLeScanner().startScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
        }
    }

    public void startScan(View view) {
        ((TextView)findViewById(R.id.tv_text)).setText("");
        scanDevices(true);
    }

    private ScanCallback mLeScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (!mDevices.contains(result)) {
                synchronized (BluetoothActivity.this) {
                    mDevices.add(result);
                    /*ParcelUuid[] uuids = result.getDevice().getUuids();
                    StringBuilder sb = new StringBuilder();
                    sb.append(result.getDevice().toString()).append(" ");
                    sb.append(result.getRssi()).append(" ");
                    if (uuids != null) {
                        for (ParcelUuid uuid : uuids) {
                            sb.append(uuid.getUuid()).append(" ");
                        }
                    }
                    appendToTextView(sb.toString());*/
                }
                result.getDevice().connectGatt(BluetoothActivity.this, true, mBluetoothGattCallback);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                if (!mDevices.contains(result)) {
                    synchronized (BluetoothActivity.this) {
                        mDevices.add(result);
                        appendToTextView(result.getDevice().toString());
                    }
                    result.getDevice().connectGatt(BluetoothActivity.this, true, mBluetoothGattCallback);
                }
            }
        }

    };

    private BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    if (!mConnections.contains(gatt)) {
                        synchronized (BluetoothActivity.this) {
                            mConnections.add(gatt);
                        }
                        StringBuilder sb = new StringBuilder();
                        sb.append("Connected ").append(gatt.getDevice());
                        appendToTextView(sb.toString());
                    }
                    break;

                case BluetoothProfile.STATE_DISCONNECTED:
                    if (mConnections.contains(gatt)) {
                        synchronized (BluetoothActivity.this) {
                            mConnections.remove(gatt);
                            //mDevices.remove(gatt.getDevice());
                        }
                    }
                    break;
            }
        }
    };

    private void appendToTextView(String text) {
        TextView tv = findViewById(R.id.tv_text);
        tv.append(String.format("\n\n" + text));
    }

}
