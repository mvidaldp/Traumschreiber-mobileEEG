package de.uos.ikw.traumschreiber;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.io.File;
import java.io.FileWriter;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private final static String TAG = DeviceControlActivity.class.getSimpleName();
    private final Handler handler = new Handler();
    private final List<Float> dp_received = new ArrayList<>();
    private final List<List<Float>> accumulated = new ArrayList<>();
    // constants
    private final int CONNECT_DELAY = 2000;
    private final float DATAPOINT_TIME = 4.5f;
    private final int PLOT_MEMO = 3000;  // max time range in ms (x value) to store on plot
    private final int MAX_VISIBLE = 500;  // see 500ms at the time on the plot
    private final ArrayList<Entry> lineEntries1 = new ArrayList<>();
    private final ArrayList<Entry> lineEntries2 = new ArrayList<>();
    private final ArrayList<Entry> lineEntries3 = new ArrayList<>();
    private final ArrayList<Entry> lineEntries4 = new ArrayList<>();
    private final ArrayList<Entry> lineEntries5 = new ArrayList<>();
    private final ArrayList<Entry> lineEntries6 = new ArrayList<>();
    private final ArrayList<Entry> lineEntries7 = new ArrayList<>();
    private final ArrayList<Entry> lineEntries8 = new ArrayList<>();
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // hack for ensuring a successful connection (with a defined delay)
            handler.postDelayed(() -> mBluetoothLeService.connect(mDeviceAddress), CONNECT_DELAY);
        }

        // Automatically connects to the device upon successful start-up initialization.
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };
    private float res_time;
    private float res_freq;
    private int cnt = 0;
    private int ch1_color;
    private int ch2_color;
    private int ch3_color;
    private int ch4_color;
    private int ch5_color;
    private int ch6_color;
    private int ch7_color;
    private int ch8_color;
    private boolean show_ch1 = true;
    private boolean show_ch2 = true;
    private boolean show_ch3 = true;
    private boolean show_ch4 = true;
    private boolean show_ch5 = true;
    private boolean show_ch6 = true;
    private boolean show_ch7 = true;
    private boolean show_ch8 = true;
    private int enabledCheckboxes = 8;
    private TextView mConnectionState;
    private TextView mCh1;
    private TextView mCh2;
    private TextView mCh3;
    private TextView mCh4;
    private TextView mCh5;
    private TextView mCh6;
    private TextView mCh7;
    private TextView mCh8;
    private CheckBox chckbx_ch1;
    private CheckBox chckbx_ch2;
    private CheckBox chckbx_ch3;
    private CheckBox chckbx_ch4;
    private CheckBox chckbx_ch5;
    private CheckBox chckbx_ch6;
    private CheckBox chckbx_ch7;
    private CheckBox chckbx_ch8;
    private TextView mXAxis;
    private TextView mDataResolution;
    private Spinner gain_spinner;
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private int ACCUM_PLOT = 30;
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private LineChart mChart;
    private Button btn_record;
    private Switch switch_plots;
    private View layout_plots;
    private boolean recording = false;
    private boolean plotting = false;
    private List<float[]> main_data;
    private float data_cnt = 0;
    private long start_data = 0;
    private String start_time;
    private String end_time;
    private long start_watch;
    private String recording_time;
    private String session_label;
    private long start_timestamp;
    private long end_timestamp;
    private final View.OnClickListener btnRecordOnClickListener = v -> {
        if (!recording) askForLabel();
        else endTrial();
    };
    private String selected_gain;
    private Thread thread;
    private long plotting_start;
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
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
                switch_plots.setEnabled(false);
                btn_record.setEnabled(false);
                gain_spinner.setEnabled(false);
                disableCheckboxes();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                if (mNotifyCharacteristic == null) {
                    readGattCharacteristic(mBluetoothLeService.getSupportedGattServices());
                }
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                data_cnt++;
                long last_data = System.currentTimeMillis();
                switch_plots.setEnabled(true);
                btn_record.setEnabled(true);
                gain_spinner.setEnabled(true);
                enableCheckboxes();
                List<Float> microV = transData(intent.getIntArrayExtra(BluetoothLeService.EXTRA_DATA));
                displayData(microV);
                if (plotting) {
                    long plotting_elapsed = last_data - plotting_start;
                    if (plotting_elapsed > ACCUM_PLOT) {
                        addEntries(accumulated);
                        accumulated.clear();
                        plotting_start = System.currentTimeMillis();
                    } else accumulated.add(microV);
                }
                if (recording) storeData(microV);
                if (start_data == 0) start_data = System.currentTimeMillis();
                res_time = (last_data - start_data) / data_cnt;
                res_freq = (1 / res_time) * 1000;
                String hertz = String.valueOf((int) res_freq) + "Hz";
                @SuppressLint("DefaultLocale") String resolution = String.format("%.2f", res_time) + "ms - ";
                String content = resolution + hertz;
                mDataResolution.setText(content);
            }
        }
    };
    private final CompoundButton.OnCheckedChangeListener switchPlotsOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (!isChecked) {
                layout_plots.setVisibility(ViewStub.GONE);
                mXAxis.setVisibility(ViewStub.GONE);
                plotting = false;
            } else {
                layout_plots.setVisibility(ViewStub.VISIBLE);
                mXAxis.setVisibility(ViewStub.VISIBLE);
                plotting = true;
                plotting_start = System.currentTimeMillis();
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);
        ch1_color = ContextCompat.getColor(getApplicationContext(), R.color.aqua);
        ch2_color = ContextCompat.getColor(getApplicationContext(), R.color.fuchsia);
        ch3_color = ContextCompat.getColor(getApplicationContext(), R.color.green);
        ch4_color = ContextCompat.getColor(getApplicationContext(), android.R.color.holo_purple);
        ch5_color = ContextCompat.getColor(getApplicationContext(), R.color.orange);
        ch6_color = ContextCompat.getColor(getApplicationContext(), R.color.red);
        ch7_color = ContextCompat.getColor(getApplicationContext(), R.color.yellow);
        ch8_color = ContextCompat.getColor(getApplicationContext(), R.color.black);
        btn_record = findViewById(R.id.btn_record);
        switch_plots = findViewById(R.id.switch_plots);
        gain_spinner = findViewById(R.id.gain_spinner);
        gain_spinner.setSelection(1);
        gain_spinner.setEnabled(false);
        selected_gain = gain_spinner.getSelectedItem().toString();
        gain_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                float max;
                switch (position) {
                    case 0:
                        selected_gain = "0.5";
                        max = 2100f;
                        break;
                    case 1:
                        selected_gain = "1";
                        max = 1700f;
                        break;
                    case 2:
                        selected_gain = "2";
                        max = 850f;
                        break;
                    case 3:
                        selected_gain = "4";
                        max = 425f;
                        break;
                    case 4:
                        selected_gain = "8";
                        max = 210f;
                        break;
                    case 5:
                        selected_gain = "16";
                        max = 110f;
                        break;
                    case 6:
                        selected_gain = "32";
                        max = 60f;
                        break;
                    case 7:
                        selected_gain = "64";
                        max = 30f;
                        break;
                    default:
                        selected_gain = "1";
                        max = 2100f;
                }
                if (mBluetoothLeService != null) {
                    writeGattCharacteristic(mBluetoothLeService.getSupportedGattServices());
                }
                YAxis leftAxis = mChart.getAxisLeft();
                leftAxis.setAxisMaximum(max);
                leftAxis.setAxisMinimum(-max);
                leftAxis.setLabelCount(13, false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // sometimes you need nothing here
            }
        });
        layout_plots = findViewById(R.id.linearLayout_chart);
        layout_plots.setVisibility(ViewStub.GONE);
        mXAxis = findViewById(R.id.XAxis_title);
        mXAxis.setVisibility(ViewStub.GONE);
        btn_record.setOnClickListener(btnRecordOnClickListener);
        switch_plots.setOnCheckedChangeListener(switchPlotsOnCheckedChangeListener);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mConnectionState = findViewById(R.id.connection_state);
        mCh1 = findViewById(R.id.ch1);
        mCh2 = findViewById(R.id.ch2);
        mCh3 = findViewById(R.id.ch3);
        mCh4 = findViewById(R.id.ch4);
        mCh5 = findViewById(R.id.ch5);
        mCh6 = findViewById(R.id.ch6);
        mCh7 = findViewById(R.id.ch7);
        mCh8 = findViewById(R.id.ch8);
        mCh1.setTextColor(ch1_color);
        mCh2.setTextColor(ch2_color);
        mCh3.setTextColor(ch3_color);
        mCh4.setTextColor(ch4_color);
        mCh5.setTextColor(ch5_color);
        mCh6.setTextColor(ch6_color);
        mCh7.setTextColor(ch7_color);
        mCh8.setTextColor(ch8_color);
        chckbx_ch1 = findViewById(R.id.checkBox_ch1);
        chckbx_ch2 = findViewById(R.id.checkBox_ch2);
        chckbx_ch3 = findViewById(R.id.checkBox_ch3);
        chckbx_ch4 = findViewById(R.id.checkBox_ch4);
        chckbx_ch5 = findViewById(R.id.checkBox_ch5);
        chckbx_ch6 = findViewById(R.id.checkBox_ch6);
        chckbx_ch7 = findViewById(R.id.checkBox_ch7);
        chckbx_ch8 = findViewById(R.id.checkBox_ch8);
        chckbx_ch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                show_ch1 = isChecked;
                if (!isChecked) enabledCheckboxes--;
                else enabledCheckboxes++;
                if (enabledCheckboxes == 0) {
                    chckbx_ch1.setChecked(true);
                    show_ch1 = true;
                    enabledCheckboxes++;
                }
            }
        });
        chckbx_ch2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                show_ch2 = isChecked;
                if (!isChecked) enabledCheckboxes--;
                else enabledCheckboxes++;
                if (enabledCheckboxes == 0) {
                    chckbx_ch2.setChecked(true);
                    show_ch2 = true;
                    enabledCheckboxes++;
                }
            }
        });
        chckbx_ch3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                show_ch3 = isChecked;
                if (!isChecked) enabledCheckboxes--;
                else enabledCheckboxes++;
                if (enabledCheckboxes == 0) {
                    chckbx_ch3.setChecked(true);
                    show_ch3 = true;
                    enabledCheckboxes++;
                }
            }
        });
        chckbx_ch4.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                show_ch4 = isChecked;
                if (!isChecked) enabledCheckboxes--;
                else enabledCheckboxes++;
                if (enabledCheckboxes == 0) {
                    chckbx_ch4.setChecked(true);
                    show_ch4 = true;
                    enabledCheckboxes++;
                }
            }
        });
        chckbx_ch5.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                show_ch5 = isChecked;
                if (!isChecked) enabledCheckboxes--;
                else enabledCheckboxes++;
                if (enabledCheckboxes == 0) {
                    chckbx_ch5.setChecked(true);
                    show_ch5 = true;
                    enabledCheckboxes++;
                }
            }
        });
        chckbx_ch6.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                show_ch6 = isChecked;
                if (!isChecked) enabledCheckboxes--;
                else enabledCheckboxes++;
                if (enabledCheckboxes == 0) {
                    chckbx_ch6.setChecked(true);
                    show_ch6 = true;
                    enabledCheckboxes++;
                }
            }
        });
        chckbx_ch7.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                show_ch7 = isChecked;
                if (!isChecked) enabledCheckboxes--;
                else enabledCheckboxes++;
                if (enabledCheckboxes == 0) {
                    chckbx_ch7.setChecked(true);
                    show_ch7 = true;
                    enabledCheckboxes++;
                }
            }
        });
        chckbx_ch8.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                show_ch8 = isChecked;
                if (!isChecked) enabledCheckboxes--;
                else enabledCheckboxes++;
                if (enabledCheckboxes == 0) {
                    chckbx_ch8.setChecked(true);
                    show_ch8 = true;
                    enabledCheckboxes++;
                }
            }
        });
        mDataResolution = findViewById(R.id.resolution_value);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        setChart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            clearUI();
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
        mBluetoothLeService.disconnect();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void writeGattCharacteristic(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid;
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();
            if (!uuid.equals("a22686cb-9268-bd91-dd4f-b52d03d85593")) {
                List<BluetoothGattCharacteristic> gattCharacteristics =
                        gattService.getCharacteristics();
                // Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    // uuid -> "faa7b588-19e5-f590-0545-c99f193c5c3e"
                    // start reading the EEG data received from this gatt characteristic
                    final int charaProp = gattCharacteristic.getProperties();
                    if (((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) |
                            (charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0) {
                        // gain-> {0.5:0b111, 1:0b000, 2:0b001, 4:0b010, 8:0b011, 16:0b100, 32:0b101, 64:0b110}
                        final byte[] newValue = new byte[6];
                        switch (selected_gain) {
                            case "0.5":
                                newValue[4] = 0b111;
                                break;
                            case "1":
                                newValue[4] = 0b000;
                                break;
                            case "2":
                                newValue[4] = 0b001;
                                break;
                            case "4":
                                newValue[4] = 0b010;
                                break;
                            case "8":
                                newValue[4] = 0b011;
                                break;
                            case "16":
                                newValue[4] = 0b100;
                                break;
                            case "32":
                                newValue[4] = 0b101;
                                break;
                            case "64":
                                newValue[4] = 0b110;
                                break;
                        }
                        gattCharacteristic.setValue(newValue);
                        mBluetoothLeService.writeCharacteristic(gattCharacteristic);
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = gattCharacteristic;
                            mBluetoothLeService.setCharacteristicNotification(
                                    gattCharacteristic, true);
                        }
                    }
                }
            }
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void readGattCharacteristic(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid;
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();
            if (uuid.equals("a22686cb-9268-bd91-dd4f-b52d03d85593")) {
                List<BluetoothGattCharacteristic> gattCharacteristics =
                        gattService.getCharacteristics();
                // Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    // uuid -> "faa7b588-19e5-f590-0545-c99f193c5c3e"
                    // start reading the EEG data received from this gatt characteristic
                    final int charaProp = gattCharacteristic.getProperties();
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                        // If there is an active notification on a characteristic, clear
                        // it first so it doesn't update the data field on the user interface.
                        if (mNotifyCharacteristic != null) {
                            mBluetoothLeService.setCharacteristicNotification(
                                    mNotifyCharacteristic, false);
                            mNotifyCharacteristic = null;
                        }
                        mBluetoothLeService.readCharacteristic(gattCharacteristic);
                    }
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                        mNotifyCharacteristic = gattCharacteristic;
                        mBluetoothLeService.setCharacteristicNotification(
                                gattCharacteristic, true);
                    }
                    mBluetoothLeService.connect(mDeviceAddress);
                }
            }
        }
    }

    private void clearUI() {
        mCh1.setText("");
        mCh2.setText("");
        mCh3.setText("");
        mCh4.setText("");
        mCh5.setText("");
        mCh6.setText("");
        mCh7.setText("");
        mCh8.setText("");
        mDataResolution.setText(R.string.no_data);
        data_cnt = 0;
        start_data = 0;
    }

    private void enableCheckboxes() {
        chckbx_ch1.setEnabled(true);
        chckbx_ch2.setEnabled(true);
        chckbx_ch3.setEnabled(true);
        chckbx_ch4.setEnabled(true);
        chckbx_ch5.setEnabled(true);
        chckbx_ch6.setEnabled(true);
        chckbx_ch7.setEnabled(true);
        chckbx_ch8.setEnabled(true);
    }

    private void disableCheckboxes() {
        chckbx_ch1.setEnabled(false);
        chckbx_ch2.setEnabled(false);
        chckbx_ch3.setEnabled(false);
        chckbx_ch4.setEnabled(false);
        chckbx_ch5.setEnabled(false);
        chckbx_ch6.setEnabled(false);
        chckbx_ch7.setEnabled(false);
        chckbx_ch8.setEnabled(false);
    }

    private List<Float> transData(int[] data) {
        // Assuming GAIN = 64
        // Conversion formula: V_in = X*1.65V/(1000 * GAIN * 2048)
        float gain = Float.parseFloat(selected_gain);
        float numerator = 1650;
        float denominator = gain * 2048;
        List<Float> data_trans = new ArrayList<>();
        for (int datapoint : data)
            data_trans.add((datapoint * numerator) / denominator);
        return data_trans;
    }

    @SuppressLint("DefaultLocale")
    private void displayData(List<Float> data_microV) {
        if (data_microV != null) {
            // data format example: +01012 -00234 +01374 -01516 +01656 +01747 +00131 -00351
            StringBuilder trans = new StringBuilder();
            List<String> values = new ArrayList<>();
            for (Float value : data_microV) {
                if (value >= 0) {
                    trans.append("+");
                    trans.append(String.format("%5.2f", value));
                } else trans.append(String.format("%5.2f", value));
                values.add(trans.toString());
                trans = new StringBuilder();
            }
            mCh1.setText(values.get(0));
            mCh2.setText(values.get(1));
            mCh3.setText(values.get(2));
            mCh4.setText(values.get(3));
            mCh5.setText(values.get(4));
            mCh6.setText(values.get(5));
            mCh7.setText(values.get(6));
            mCh8.setText(values.get(7));
        }
    }

    private void setChart() {
        OnChartValueSelectedListener ol = new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry entry, Highlight h) {
                //entry.getData() returns null here
            }

            @Override
            public void onNothingSelected() {

            }
        };
        mChart = findViewById(R.id.layout_chart);
        mChart.setOnChartValueSelectedListener(ol);
        // enable description text
        mChart.getDescription().setEnabled(false);
        // enable touch gestures
        mChart.setTouchEnabled(true);
        // enable scaling and dragging
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(true);
        // if disabled, scaling can be done on x- and y-axis separately
        mChart.setPinchZoom(true);
        // set an alternative background color
        LineData data = new LineData();
        data.setValueTextColor(Color.BLACK);
        // add empty data
        mChart.setData(data);
        // get the legend (only possible after setting data)
        Legend l1 = mChart.getLegend();
        // modify the legend ...
        l1.setForm(Legend.LegendForm.LINE);
        l1.setTextColor(Color.BLACK);
        // set the y left axis
        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setTextColor(Color.GRAY);
        leftAxis.setAxisMaximum(30f);
        leftAxis.setAxisMinimum(-30f);
        leftAxis.setLabelCount(13, true); // from -35 to 35, a label each 5 microV
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.WHITE);
        // disable the y right axis
        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);
        // set the x bottom axis
        XAxis bottomAxis = mChart.getXAxis();
        bottomAxis.setLabelCount(5, true);
        bottomAxis.setValueFormatter(new MyXAxisValueFormatter());
        bottomAxis.setPosition(XAxis.XAxisPosition.TOP);
        bottomAxis.setGridColor(Color.WHITE);
        bottomAxis.setTextColor(Color.GRAY);
    }

    private LineDataSet createSet1(ArrayList<Entry> le, boolean show) {
        LineDataSet set1 = new LineDataSet(le, "Ch-1");
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setColor(ch1_color);
        set1.setDrawCircles(false);
        set1.setLineWidth(1f);
        set1.setValueTextColor(ch1_color);
        set1.setVisible(show);
        return set1;
    }

    private LineDataSet createSet2(ArrayList<Entry> le, boolean show) {
        LineDataSet set2 = new LineDataSet(le, "Ch-2");
        set2.setAxisDependency(YAxis.AxisDependency.LEFT);
        set2.setColor(ch2_color);
        set2.setDrawCircles(false);
        set2.setLineWidth(1f);
        set2.setValueTextColor(ch2_color);
        set2.setVisible(show);
        return set2;
    }

    private LineDataSet createSet3(ArrayList<Entry> le, boolean show) {
        LineDataSet set3 = new LineDataSet(le, "Ch-3");
        set3.setAxisDependency(YAxis.AxisDependency.LEFT);
        set3.setColor(ch3_color);
        set3.setDrawCircles(false);
        set3.setLineWidth(1f);
        set3.setValueTextColor(ch3_color);
        set3.setVisible(show);
        return set3;
    }

    private LineDataSet createSet4(ArrayList<Entry> le, boolean show) {
        LineDataSet set4 = new LineDataSet(le, "Ch-4");
        set4.setAxisDependency(YAxis.AxisDependency.LEFT);
        set4.setColor(ch4_color);
        set4.setDrawCircles(false);
        set4.setLineWidth(1f);
        set4.setValueTextColor(ch4_color);
        set4.setVisible(show);
        return set4;
    }

    private LineDataSet createSet5(ArrayList<Entry> le, boolean show) {
        LineDataSet set5 = new LineDataSet(le, "Ch-5");
        set5.setAxisDependency(YAxis.AxisDependency.LEFT);
        set5.setColor(ch5_color);
        set5.setDrawCircles(false);
        set5.setLineWidth(1f);
        set5.setValueTextColor(ch5_color);
        set5.setVisible(show);
        return set5;
    }

    private LineDataSet createSet6(ArrayList<Entry> le, boolean show) {
        LineDataSet set6 = new LineDataSet(le, "Ch-6");
        set6.setAxisDependency(YAxis.AxisDependency.LEFT);
        set6.setColor(ch6_color);
        set6.setDrawCircles(false);
        set6.setLineWidth(1f);
        set6.setValueTextColor(ch6_color);
        set6.setVisible(show);
        return set6;
    }

    private LineDataSet createSet7(ArrayList<Entry> le, boolean show) {
        LineDataSet set7 = new LineDataSet(le, "Ch-7");
        set7.setAxisDependency(YAxis.AxisDependency.LEFT);
        set7.setColor(ch7_color);
        set7.setDrawCircles(false);
        set7.setLineWidth(1f);
        set7.setValueTextColor(ch7_color);
        set7.setVisible(show);
        return set7;
    }

    private LineDataSet createSet8(ArrayList<Entry> le, boolean show) {
        LineDataSet set8 = new LineDataSet(le, "Ch-8");
        set8.setAxisDependency(YAxis.AxisDependency.LEFT);
        set8.setColor(ch8_color);
        set8.setDrawCircles(false);
        set8.setLineWidth(1f);
        set8.setValueTextColor(ch8_color);
        set8.setVisible(show);
        return set8;
    }

    private void addEntries(final List<List<Float>> e_list) {
        final List<ILineDataSet> datasets = new ArrayList<>();  // for adding multiple plots
        float x = 0;
        for (List<Float> f : e_list) {
            cnt++;
            x = cnt * DATAPOINT_TIME;
            lineEntries1.add(new Entry(x, f.get(0)));
            lineEntries2.add(new Entry(x, f.get(1)));
            lineEntries3.add(new Entry(x, f.get(2)));
            lineEntries4.add(new Entry(x, f.get(3)));
            lineEntries5.add(new Entry(x, f.get(4)));
            lineEntries6.add(new Entry(x, f.get(5)));
            lineEntries7.add(new Entry(x, f.get(6)));
            lineEntries8.add(new Entry(x, f.get(7)));
        }
        final float f_x = x;
        if (thread != null) thread.interrupt();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                LineDataSet set1 = createSet1(lineEntries1, show_ch1);
                datasets.add(set1);
                LineDataSet set2 = createSet2(lineEntries2, show_ch2);
                datasets.add(set2);
                LineDataSet set3 = createSet3(lineEntries3, show_ch3);
                datasets.add(set3);
                LineDataSet set4 = createSet4(lineEntries4, show_ch4);
                datasets.add(set4);
                LineDataSet set5 = createSet5(lineEntries5, show_ch5);
                datasets.add(set5);
                LineDataSet set6 = createSet6(lineEntries6, show_ch6);
                datasets.add(set6);
                LineDataSet set7 = createSet7(lineEntries7, show_ch7);
                datasets.add(set7);
                LineDataSet set8 = createSet8(lineEntries8, show_ch8);
                datasets.add(set8);
                LineData linedata = new LineData(datasets);
                linedata.notifyDataChanged();
                mChart.setData(linedata);
                mChart.notifyDataSetChanged();
                // limit the number of visible entries
                mChart.setVisibleXRangeMaximum(MAX_VISIBLE);
                // move to the latest entry
                mChart.moveViewToX(f_x);
            }
        };
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(runnable);
            }
        });
        thread.start();
        if (x > PLOT_MEMO) {
            for (int j = 0; j < e_list.size(); j++) {
                for (int i = 0; i < mChart.getData().getDataSetCount(); i++) {
                    mChart.getData().getDataSetByIndex(i).removeFirst();
                }
            }
        }
    }

    @SuppressLint({"SimpleDateFormat", "SetTextI18n"})
    private void startTrial() {
        cnt = 0;
        main_data = new ArrayList<>();
        start_time = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
        start_timestamp = new Timestamp(start_watch).getTime();
        recording = true;
        btn_record.setText("Stop and Store Data");
    }

    private void askForLabel() {
        // set the custom layout
        final View inputLayout = getLayoutInflater().inflate(R.layout.prompt, null);

        // set an EditText view to get user input
        final EditText inputText = inputLayout.findViewById(R.id.editText);

        // create an alert builder
        new AlertDialog.Builder(this, R.style.prompt)
            .setView(inputLayout)
            .setCancelable(false)
            .setTitle("Please, enter the session label")
            .setMessage("E.g. walking, eating, sleeping, etc.")
            .setPositiveButton("Save", (dialog, id) -> {
                session_label = inputText.getText().toString();
                startTrial();
            }).create().show();
    }

    @SuppressLint("SimpleDateFormat")
    private void endTrial() {
        recording = false;
        end_time = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
        long stop_watch = System.currentTimeMillis();
        end_timestamp = new Timestamp(stop_watch).getTime();
        recording_time = Long.toString(stop_watch - start_watch);
        btn_record.setText(R.string.save_label);
        if (session_label == null) saveSession();
        else saveSession(session_label);
        session_label = null;
        Toast.makeText(
                getApplicationContext(),
                "Your EEG session was successfully stored",
                Toast.LENGTH_LONG
        ).show();
        btn_record.setText(R.string.record_label);
    }

    private void storeData(List<Float> data_microV) {
        if (dp_received.size() == 0) start_watch = System.currentTimeMillis();
        float[] f_microV = new float[data_microV.size()];
        float curr_received = System.currentTimeMillis() - start_watch;
        dp_received.add(curr_received);
        int i = 0;
        for (Float f : data_microV)
            f_microV[i++] = (f != null ? f : Float.NaN); // Or whatever default you want
        main_data.add(f_microV);
    }

    private void saveSession() {
        saveSession("default");
    }

    private void saveSession(final String tag) {
        final String top_header = "Session ID,Session Tag,Date,Shape (rows x columns)," +
                "Duration (ms),Starting Time,Ending Time,Resolution (ms),Resolution (Hz)," +
                "Unit Measure,Starting Timestamp,Ending Timestamp";
        final String dp_header = "Time,Ch-1,Ch-2,Ch-3,Ch-4,Ch-5,Ch-6,Ch-7,Ch-8";
        final UUID id = UUID.randomUUID();
        @SuppressLint("SimpleDateFormat") final String date = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss").format(new Date());
        final char delimiter = ',';
        final char break_line = '\n';
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File formatted = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(),
                            date + "_" + tag + ".csv");
                    // if file doesn't exists, then create it
                    if (!formatted.exists()) //noinspection ResultOfMethodCallIgnored
                        formatted.createNewFile();
                    FileWriter fileWriter = new FileWriter(formatted);
                    int rows = main_data.size();
                    int cols = main_data.get(0).length;
                    fileWriter.append(top_header);
                    fileWriter.append(break_line);
                    fileWriter.append(id.toString());
                    fileWriter.append(delimiter);
                    fileWriter.append(tag);
                    fileWriter.append(delimiter);
                    fileWriter.append(date);
                    fileWriter.append(delimiter);
                    fileWriter.append(String.valueOf(rows)).append("x").append(String.valueOf(cols));
                    fileWriter.append(delimiter);
                    fileWriter.append(recording_time);
                    fileWriter.append(delimiter);
                    fileWriter.append(start_time);
                    fileWriter.append(delimiter);
                    fileWriter.append(end_time);
                    fileWriter.append(delimiter);
                    fileWriter.append(String.valueOf(res_time));
                    fileWriter.append(delimiter);
                    fileWriter.append(String.valueOf(res_freq));
                    fileWriter.append(delimiter);
                    fileWriter.append("µV");
                    fileWriter.append(delimiter);
                    fileWriter.append(Long.toString(start_timestamp));
                    fileWriter.append(delimiter);
                    fileWriter.append(Long.toString(end_timestamp));
                    fileWriter.append(delimiter);
                    fileWriter.append(break_line);
                    fileWriter.append(dp_header);
                    fileWriter.append(break_line);
                    for (int i = 0; i < rows; i++) {
                        fileWriter.append(String.valueOf(dp_received.get(i)));
                        fileWriter.append(delimiter);
                        for (int j = 0; j < cols; j++) {
                            fileWriter.append(String.valueOf(main_data.get(i)[j]));
                            fileWriter.append(delimiter);
                        }
                        fileWriter.append(break_line);
                    }
                    fileWriter.flush();
                    fileWriter.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error storing the data into a CSV file: " + e);
                }
            }
        }).start();
    }
}
