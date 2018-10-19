package de.uni_osnabrueck.traumschreiber.epilepsy.eegdroid;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements ConnectToDeviceFragment.OnFragmentInteractionListener, ShowStatisticsFragment.OnFragmentInteractionListener {
    private final static String TAG = MainActivity.class.getSimpleName();


    private BluetoothAdapter mBluetoothAdapter;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static final int REQUEST_ACCESS_COARSE_LOCATION = 1;

    // Stops scanning after x/1000 seconds.
    private static final long SCAN_PERIOD = 10000;

    private static final int COLOR_CONNECTED = Color.parseColor("#00FF00");
    private static final int COLOR_DISCONNECTED = Color.parseColor("#000000");
    private static final int COLOR_RECORDING = Color.parseColor("#FF8000");

    public BluetoothGattCharacteristic mDataGattCharacteristic;

    //Setting up all TextViews and Buttons we might need
    private TextView mDeviceIDTextView;
    private TextView mChannelsNumTextView;
    private TextView mConnectionStatusTextView;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));


//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Disconnecting from Traumschreiber Device", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//                if (mBluetoothLeService != null) {
//                    mBluetoothLeService.disconnect();
//                }
//            }
//
//        });
        final Intent intent = getIntent();

        // Sets up UI references.
//        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
//        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
//        mGattServicesList.setOnChildClickListener(servicesListClickListner);
//        mConnectionState = (TextView) findViewById(R.id.connection_state);
//        mDataField = (TextView) findViewById(R.id.data_value);
//        TextView tv = (TextView) findViewById(R.id.data_value);
//        String eeg_data = new String((String) mSectionsPagerAdapter.getItem(2).);
////        Log.d(TAG, String.format("Values: " + eeg_data));
//        getActionBar().setTitle(mDeviceName);
//        getActionBar().setDisplayHomeAsUpEnabled(true);

        if (!this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            // TODO: If BLE is not supported, we should do graceful error handling
            return;
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            // TODO: Graceful error handling again
            return;
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    //TODO: Read the Toast
    public void getPermission() {
        Toast.makeText(this, "It is not yet implemented to ask for permission. Do so ASAP. Apps may crash otherwise. Use Somnium as orientation", Toast.LENGTH_SHORT).show();
    }

    /**
     * Checks if Location Services are enabled on the users device if on Android 6.0 or higher
     * On Android < 6.0 return true
     *
     * @return boolean
     */
    public boolean checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            LocationManager locationManager;
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } else return true;
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            textView.setText(getString(R.string.section_format, getArguments().getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            if (position == 0) {
                return ConnectToDeviceFragment.newInstance();
            }

            if (position == 1) {
                return ShowStatisticsFragment.newInstance("Test 1", "param 2");
            }


            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }
    }


    //https://stackoverflow.com/questions/24777985/how-to-implement-onfragmentinteractionlistener#27666001
    @Override
    public void onFragmentInteraction(Uri uri){
        //you can leave it empty
    }



    public void connect(String deviceAddress) {

        Log.d(TAG, "connect: Attempting to connect to " + deviceAddress);

        mDeviceAddress = deviceAddress;

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        //ComponentName comp = startService(gattServiceIntent);


        boolean ret = bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

//        Intent gattServiceIntent = new Intent(this, BLEConnectionService.class);
//        startService(new Intent(this, BLEConnectionService.class));
//        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        Log.d(TAG, "connect: Returned from bindService: " + ret);
    }

    public void updateAvailableServices() {
        List<BluetoothGattService> gattServices = mBluetoothLeService.getSupportedGattServices();
        Log.d(TAG, "updateAvailableServices: found services: " + gattServices.toString());


        // To find the Service which provides us with EEG_DATA, loop through all available services
        for (BluetoothGattService gattService : gattServices) {
            //String service_uuid = gattService.getUuid().toString();
            UUID service_uuid = gattService.getUuid();
            Log.d(TAG, "updateAvailableServices: "+service_uuid);

            // If the Service ID is the one of the BIOSIGNALS_SERVICE_UUID, we want to be notified on updates
            if (service_uuid.equals(TraumschreiberToolbox.BIOSIGNALS_SERVICE_UUID)) {
                Log.d(TAG, "updateAvailableServices: This one is the BIOSIGNALS_SERVICE: " + service_uuid.toString());
                List<BluetoothGattCharacteristic> gattCharacteristics =
                        gattService.getCharacteristics();

                UUID chararcteristic_uuid;
                // Loops through available Characteristics of the service
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    chararcteristic_uuid = gattCharacteristic.getUuid();

                    // If the characteristics is the one of your
                    if(chararcteristic_uuid.equals(TraumschreiberToolbox.BIOSIGNALS_UUID)){
                        Log.d(TAG, "updateAvailableServices: Found Traumschreiber EEG SIGNAL Characteristic: " + chararcteristic_uuid.toString());

                        // We want to be notified by the BluetoothLeService on updates of the characteristic. I.e., every time a new signal is measured
                        mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
                    }
                }

            }


        }
    }


    //Will hold the attributes of a connected Traumschreiber, when done
    public static boolean mDeviceConnected = false;
    public static boolean mDeviceRecording = false;
    public static String mDeviceAddress;


    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private BluetoothGattCharacteristic mNotifyCharacteristic;


    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: Received something");

            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mDeviceConnected = true;
                updateStatisticsFragment();
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mDeviceConnected = false;
                invalidateOptionsMenu();
                updateStatisticsFragment();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                updateAvailableServices();
                // Show all the supported services and characteristics on the user interface.
//                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.d(TAG, "onReceive: Received data from a service");
//                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
//                if(recording) {
//                    storeData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
//                }
                updateStatisticsFragment();
            }
        }
    };

//    private void updateStatisticsFragment() {
//        Log.d(TAG, String.format("updateStatisticsFragment: Attempt to update the Statistics fragment with following data: " +
//                "ID: %1$s, status: %2$s", mDeviceAddress, mDeviceConnected));
//        Toast.makeText(getBaseContext(), String.format("updateStatisticsFragment: Attempt to update the Statistics fragment with following data: " +
//                "ID: %1$s, status: %2$s", mDeviceAddress, mDeviceConnected), Toast.LENGTH_SHORT).show();
//    }


    private void updateStatisticsFragment() {
        Log.d(TAG, String.format("updateStatisticsFragment: Attempt to update the Statistics fragment with following data: " +
                "ID: %1$s, status: %2$s", mDeviceAddress, mDeviceConnected));
        Toast.makeText(getBaseContext(), String.format("updateStatisticsFragment: Attempt to update the Statistics fragment with following data: " +
                "ID: %1$s, status: %2$s", mDeviceAddress, mDeviceConnected), Toast.LENGTH_SHORT).show();

        //Initialise all the ViewElements needed
        //for the fragment_show_statistics
        mConnectionStatusTextView = findViewById(R.id.connectionStatusContent);
        mDeviceIDTextView = findViewById(R.id.deviceIdContent);
        mChannelsNumTextView = findViewById(R.id.numDataChannelsContent);


        if (mDeviceConnected) {
            mDeviceIDTextView.setText(mDeviceAddress);
            if (mDeviceRecording) {
                mConnectionStatusTextView.setText(R.string.recording);
                mConnectionStatusTextView.setTextColor(COLOR_RECORDING);
            } else {
                mConnectionStatusTextView.setText(R.string.connected);
                mConnectionStatusTextView.setTextColor(COLOR_CONNECTED);
            }
        } else {
            mDeviceIDTextView.setText(R.string.no_device_connected);
            mConnectionStatusTextView.setText(R.string.disconnected);
            mConnectionStatusTextView.setTextColor(COLOR_DISCONNECTED);
        }
    }

    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeService.setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            mBluetoothLeService.readCharacteristic(characteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(
                                    characteristic, true);
                        }
                        return true;
                    }
                    return false;
                }
            };


    private void clearUI() {
//        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
//        mDataField.setText(R.string.no_data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }


    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

}
