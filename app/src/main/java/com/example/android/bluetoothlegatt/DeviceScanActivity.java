package com.example.android.bluetoothlegatt;

import android.Manifest;
import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends ListActivity {
    private final static String TAG = DeviceScanActivity.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH = 1;
    private static final int REQUEST_BLUETOOTH_ADMIN = 1;
    private static final int REQUEST_ACCESS_COARSE_LOCATION = 1;
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 1;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            Log.i("onServicesDiscovered", services.toString());
            gatt.readCharacteristic(services.get(0).getCharacteristics().get
                    (0));
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
            Log.i("onCharacteristicRead", characteristic.toString());
            gatt.disconnect();
        }
    };
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mLEScanner;
    private boolean mScanning;
    private Handler mHandler;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;
    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            BluetoothDevice btDevice = result.getDevice();
            connectToDevice(btDevice);
            mLeDeviceListAdapter.addDevice(btDevice);
            mLeDeviceListAdapter.notifyDataSetChanged();
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    protected void onResume() {
        super.onResume();
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            if (Build.VERSION.SDK_INT >= 21) {
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                        .build();
                filters = new ArrayList<>();
            }
            // Initializes list view adapter.
            mLeDeviceListAdapter = new LeDeviceListAdapter();
            setListAdapter(mLeDeviceListAdapter);
            scanLeDevice(true);
        }
        if (ContextCompat.checkSelfPermission(DeviceScanActivity.this,
                Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(DeviceScanActivity.this,
                    Manifest.permission.BLUETOOTH)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(DeviceScanActivity.this,
                        new String[]{Manifest.permission.BLUETOOTH},
                        REQUEST_BLUETOOTH);
                // REQUEST_BLUETOOTH is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
        if (ContextCompat.checkSelfPermission(DeviceScanActivity.this,
                Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(DeviceScanActivity.this,
                    Manifest.permission.BLUETOOTH_ADMIN)) {
            } else {
                ActivityCompat.requestPermissions(DeviceScanActivity.this,
                        new String[]{Manifest.permission.BLUETOOTH_ADMIN},
                        REQUEST_BLUETOOTH_ADMIN);
            }
        }
        if (ContextCompat.checkSelfPermission(DeviceScanActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(DeviceScanActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            } else {
                ActivityCompat.requestPermissions(DeviceScanActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        }
        if (ContextCompat.checkSelfPermission(DeviceScanActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(DeviceScanActivity.this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
            } else {
                ActivityCompat.requestPermissions(DeviceScanActivity.this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_ACCESS_COARSE_LOCATION);
            }
        }
        if (ContextCompat.checkSelfPermission(DeviceScanActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(DeviceScanActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
            } else {
                ActivityCompat.requestPermissions(DeviceScanActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_READ_EXTERNAL_STORAGE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            // Bluetooth not enabled.
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
            mLeDeviceListAdapter.clear();
        }
    }

    @Override
    protected void onDestroy() {
        if (mGatt == null) {
            return;
        }
        mGatt.close();
        mGatt = null;
        super.onDestroy();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
        final Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning) {
            mLEScanner.stopScan(mScanCallback);
            mScanning = false;
        }
        startActivity(intent);  // start activity DeviceControlActivity
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mLEScanner.stopScan(mScanCallback);
                    mScanning = false;
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);
            mLEScanner.startScan(filters, settings, mScanCallback);
            mScanning = true;
        } else {
            mLEScanner.stopScan(mScanCallback);
            mScanning = false;
        }
        invalidateOptionsMenu();
    }

    private void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            mGatt = device.connectGatt(this, false, gattCallback);
            scanLeDevice(false); // will stop after first device detection
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private final ArrayList<BluetoothDevice> mLeDevices;

        LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<>();
        }

        void addDevice(BluetoothDevice device) {
            if (TraumschreiberService.isTraumschreiberAddress(device.getAddress())) {
                Log.d(TAG, "addDevice: Found a Traumschreiber with ID " + device.getAddress());
                if (!mLeDevices.contains(device)) {
                    mLeDevices.add(device);
                    scanLeDevice(false);
                }
            } else {
                Log.d(TAG, "addDevice: Found a device which is not a Traumschreiber with ID " + device.getAddress());
            }
        }

        BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = View.inflate(getApplicationContext(), R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = view.findViewById(R.id.device_address);
                viewHolder.deviceName = view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0) {
                viewHolder.deviceName.setText(deviceName);
            } else viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }
}