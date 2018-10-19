package de.uni_osnabrueck.traumschreiber.epilepsy.eegdroid;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ConnectToDeviceFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ConnectToDeviceFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ConnectToDeviceFragment extends Fragment {
    private final static String TAG = ConnectToDeviceFragment.class.getSimpleName();


    // For now, we do not need any fragment initialization parameters
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    //private static final String ARG_PARAM1 = "param1";
    //private static final String ARG_PARAM2 = "param2";

    //private String mParam1;
    //private String mParam2;

    private static final boolean FILTER_FOR_TRAUMSCHREIBER = true;

    private static final long SCAN_DURATION = 1000;
    public static LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    private OnFragmentInteractionListener mListener;

    public ConnectToDeviceFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ConnectToDeviceFragment.
     */
    //public static ConnectToDeviceFragment newInstance(String param1, String param2) {
    public static ConnectToDeviceFragment newInstance() {
        ConnectToDeviceFragment fragment = new ConnectToDeviceFragment();
        Bundle args = new Bundle();
        //args.putString(ARG_PARAM1, param1);
        //args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }


    @Override
    public void onResume() {
        super.onResume();
        // check if necessary permissions for Ble are granted
        ((MainActivity) getActivity()).getPermission();
        // in case of disconnect refresh/clear the BLE Device list to not shit legacy devices
        if (!MainActivity.mDeviceConnected) {
            mLeDeviceListAdapter.clear();
        }
    }

    /**
     * Triggers device scanning and stops it after a pre defined period
     *
     * @param enable or disable scanning
     */
    private void scanLeDevice(final boolean enable) {
        Activity activity = getActivity();
        if (enable) {
            // Stops scanning after the set duration
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    // display search search animation
                    Activity activity = getActivity();
                    if (activity != null) {
                        //((MainActivity) getActivity()).toggleProgressBar(0);
                        Toast.makeText(getActivity(), "Stopping scan", Toast.LENGTH_SHORT).show();
                    }


                }
            }, SCAN_DURATION);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            // hide search search animation


            if (activity != null) {
                //((MainActivity) getActivity()).toggleProgressBar(1);
                Toast.makeText(getContext(), "Starting scan", Toast.LENGTH_SHORT).show();
            }


        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mHandler = new Handler();
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        // Initializes the Bluetooth adapter
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "onCreateView: mBluetoothAdapter null. No idea why");
        }

        // hook up UI elements and adapter for later inflation
        View rootView = inflater.inflate(R.layout.fragment_connect_to_device, container, false);
        ListView listView = (ListView) rootView.findViewById(R.id.deviceList);
        listView.setAdapter(mLeDeviceListAdapter);

        /**
         * handles the on click event on an item(BLE) device inside the ListView (displayed devices)
         */
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // get the device at from the clicked position
                final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
                if (device == null) return;

                String deviceAddress = device.getAddress();

                Toast.makeText(getContext(), "Connecting to device with address " + deviceAddress, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onItemClick: Connecting to device with address " + deviceAddress);


                // The MainActivity should then attempt connecting to the selected Device
                ((MainActivity) getActivity()).connect(deviceAddress);

                // after connecting to a device stop the scanning
                if (mScanning) {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mScanning = false;
                }
            }

        });

        // Inflate the layout for this fragment
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // hook up the connection button and define on click behavior
        // former imageButton
        @SuppressWarnings("ConstantConditions") ImageButton bluetoothButton = (ImageButton) getView().findViewById(R.id.bluetoothButton);


        if (bluetoothButton != null) {
            bluetoothButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mLeDeviceListAdapter.clear();

                    // TODO: If the user presses the button although we are already connected, first disconnect.
                    // Even better: Do not conduct a new search or disconnect but just tell the user to disconnect himself before searching

                    // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
                    // prompt the user to do so else the app will not be able to discover BLE devices
                    if (!mBluetoothAdapter.isEnabled()) {
                        Toast.makeText(getContext(), "Enable bluetooth and try again", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // On Android 6.0 or higher Ensures Location is enabled on the device.
                    // If Location is not currently enabled prompt the user to do so else the app
                    // will not be able to discover BLE devices
                    if (!((MainActivity) getActivity()).checkLocationPermission()) {
                        Toast.makeText(getContext(), "Enable location and try again", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // if everything is in place/enabled start scanning for devices
                    if (!mScanning) {
                        scanLeDevice(true);
                    }

                }
            });
        }

    }


    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLeDeviceListAdapter.addDevice(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    @Override
    public void onPause() {
        super.onPause();
        scanLeDevice(false);

        if (!MainActivity.mDeviceConnected) {
            mLeDeviceListAdapter.clear();
        }
    }

    // helper holding class
    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    // Adapter for holding devices found through scanning.
    public class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = LayoutInflater.from(getContext());
        }

        /**
         * adds a found BLE device to the device list
         *
         * @param device BLE device that will be added
         */
        void addDevice(BluetoothDevice device) {

            // make sure the device is a real Traumschreiber, if wanted
            if (FILTER_FOR_TRAUMSCHREIBER && TraumschreiberToolbox.isTraumschreiber(device)) {
                // Add a Traumschreiber to the list
                if (!mLeDevices.contains(device)) {
                    mLeDevices.add(device);
                }
            } else {
                // Later, this option might be commented to only show Traumschreiber
                //Toast.makeText(getContext(), "Some non-Traumschreiber device has been found or you don't filter", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "addDevice: Some non-Traumschreiber device has been found or you don't filter");
                if (!mLeDevices.contains(device)) {
                    mLeDevices.add(device);
                }
            }

            // set UI text because a new BLE device has been found and added
            TextView textView = (TextView) getActivity().findViewById(R.id.bluetoothButtonTextView);
            textView.setText(R.string.select_your_traumschreiber);
        }

        BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
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
            // prepares and arranges the list view
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }

        // clear the UI up after disconnects or data changes
        void clear() {
            mLeDevices.clear();
            mLeDeviceListAdapter.notifyDataSetChanged();
        }
    }

}
