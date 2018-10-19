package com.example.android.bluetoothlegatt;

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
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.text.InputType;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineData;

import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import static org.nd4j.linalg.indexing.NDArrayIndex.interval;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Date;
import java.util.UUID;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileWriter;
import java.math.RoundingMode;

import com.afollestad.materialdialogs.MaterialDialog;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState;
    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private LineChart mChart1;
    private LineChart mChart2;
    private LineChart mChart3;
    private LineChart mChart4;
    private LineChart mChart5;
    private LineChart mChart6;
    private LineChart mChart7;
    private LineChart mChart8;

    private Button btn_record;
    private boolean recording = false;
    private INDArray main_data;
    private String start_time;
    private String end_time;
    private long start_watch;
    private String recording_time;
    private String session_label;

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
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                if(recording) {
                    storeData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                }
            }
        }
    };

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
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField.setText(R.string.no_data);
    }

    private View.OnClickListener btnRecordOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            btnRecordButtonClicked();
            if(recording) {
                askForLabel();
            }
        }
    };

    private void askForLabel() {
        new MaterialDialog.Builder(this)
                .title("Please, enter the session label")
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input("E.g. walking, eating, sleeping, etc.",
                        "", new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog dialog, CharSequence input) {
                        session_label = input.toString();
                    }
                }).show();
    }

    private void btnRecordButtonClicked() {
        if(recording) {
            recording = false;
            end_time = new SimpleDateFormat("HH:mm:ss").format(new Date());
            long stop_watch = System.currentTimeMillis();
            recording_time = Long.toString(stop_watch - start_watch);
            Toast.makeText(this, "Saving EEG session into a file...",
                    Toast.LENGTH_LONG).show();
            if(session_label == null) {
                saveSession();
            }
            else {
                saveSession(session_label);
                session_label = null;
            }
            Toast.makeText(this, "Your EEG session was successfully stored",
                    Toast.LENGTH_SHORT).show();
            btn_record.setText("Record");
        }
        else {
            main_data = Nd4j.zeros(1, 8);
            start_time = new SimpleDateFormat("HH:mm:ss").format(new Date());
            start_watch = System.currentTimeMillis();
            recording = true;
            btn_record.setText("Stop and Store Data");
        }
    }

    private void storeData(String data) {
        // Conversion formula: V_in = X*1.65V/(1000 * GAIN * 2048)
        // Assuming GAIN = 64
        final float numerator = 1650000;
        final float denominator = 1000 * 64 * 2048;
        final String[] parts = data.split(" ");
        final List<Float> data_microV = new ArrayList<>();

        for(int i = 0; i < 8; i++) {
            data_microV.add((Float.parseFloat(parts[i]) * numerator) / denominator);
        }
        float[] f_microV = new float[data_microV.size()];
        int i = 0;
        for (Float f : data_microV) {
            f_microV[i++] = (f != null ? f : Float.NaN); // Or whatever default you want
        }
        INDArray curr_data = Nd4j.create(f_microV);
        main_data = Nd4j.vstack(main_data, curr_data);
    }

    private void saveSession() {
        saveSession("Default");
    }

    private void saveSession(String tag) {
        String top_header = "Session ID,Session Tag,Date,Shape (rows x columns)," +
                "Duration (ms),Starting Time,Ending Time,Resolution (ms),Unit Measure";
        String dp_header = "S1, S2, S3, S4, S5, S6, S7, S8,";
        main_data = main_data.get(interval(1, main_data.rows()));  // Remove the first row (zeros)
        UUID id = UUID.randomUUID();
        String date = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
        Character delimiter = ',';
        Character break_line = '\n';
        Float current;
        DecimalFormat df = new DecimalFormat("#.###");
        df.setRoundingMode(RoundingMode.HALF_EVEN);
        try {
            File formatted = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS),
                    date.replace('/', '-') + '_' + id + ".csv");
            // if file doesn't exists, then create it
            if (!formatted.exists()) {
                formatted.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(formatted);
            String rows = String.valueOf(main_data.rows());  // Also INDArray.shape()[0]
            String cols = String.valueOf(main_data.columns());  // Also INDArray.shape()[1]
            fileWriter.append(top_header);
            fileWriter.append(break_line);
            fileWriter.append(id.toString());
            fileWriter.append(delimiter);
            fileWriter.append(tag);
            fileWriter.append(delimiter);
            fileWriter.append(date);
            fileWriter.append(delimiter);
            fileWriter.append(rows + " x " + cols);
            fileWriter.append(delimiter);
            fileWriter.append(recording_time);
            fileWriter.append(delimiter);
            fileWriter.append(start_time);
            fileWriter.append(delimiter);
            fileWriter.append(end_time);
            fileWriter.append(delimiter);
            String resolution = String.valueOf(Float.parseFloat(recording_time) /
                    Float.parseFloat(rows));
            fileWriter.append(resolution);
            fileWriter.append(delimiter);
            fileWriter.append("µV");
            fileWriter.append(delimiter);
            fileWriter.append(break_line);
            fileWriter.append(dp_header);
            fileWriter.append(break_line);
            for (int i = 0; i < main_data.rows(); i++) {
                for (int j = 0; j < main_data.columns(); j++) {
                    current = main_data.getFloat(i, j);
                    fileWriter.append(df.format(current));
                    fileWriter.append(delimiter);
                }
                fileWriter.append(break_line);
            }
            fileWriter.flush();
            fileWriter.close();
        } catch (Exception e) {
            Log.e(TAG, String.format("Error storing the data into a CSV file: " + e));
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        btn_record = (Button) findViewById(R.id.btn_record);
        btn_record.setOnClickListener(btnRecordOnClickListener);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mGattServicesList.setOnChildClickListener(servicesListClickListner);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);
//        TextView tv = (TextView) findViewById(R.id.data_value);
//        String eeg_data = new String((String) tv.getText());
////        Log.d(TAG, String.format("Values: " + eeg_data));
        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        OnChartValueSelectedListener ol = new OnChartValueSelectedListener(){

            @Override
            public void onValueSelected(Entry entry, Highlight h) {
                //entry.getData() returns null here
            }

            @Override
            public void onNothingSelected() {

            }
        };

        mChart1 = (LineChart) findViewById(R.id.chart1);
        mChart1.setOnChartValueSelectedListener(ol);

        mChart2 = (LineChart) findViewById(R.id.chart2);
        mChart2.setOnChartValueSelectedListener(ol);

        mChart3 = (LineChart) findViewById(R.id.chart3);
        mChart3.setOnChartValueSelectedListener(ol);

        mChart4 = (LineChart) findViewById(R.id.chart4);
        mChart4.setOnChartValueSelectedListener(ol);

        mChart5 = (LineChart) findViewById(R.id.chart5);
        mChart5.setOnChartValueSelectedListener(ol);

        mChart6 = (LineChart) findViewById(R.id.chart6);
        mChart6.setOnChartValueSelectedListener(ol);

        mChart7 = (LineChart) findViewById(R.id.chart7);
        mChart7.setOnChartValueSelectedListener(ol);

        mChart8 = (LineChart) findViewById(R.id.chart8);
        mChart8.setOnChartValueSelectedListener(ol);

        // enable description text
        mChart1.getDescription().setEnabled(false);
        mChart2.getDescription().setEnabled(false);
        mChart3.getDescription().setEnabled(false);
        mChart4.getDescription().setEnabled(false);
        mChart5.getDescription().setEnabled(false);
        mChart6.getDescription().setEnabled(false);
        mChart7.getDescription().setEnabled(false);
        mChart8.getDescription().setEnabled(false);

        // enable touch gestures
        mChart1.setTouchEnabled(true);
        mChart2.setTouchEnabled(true);
        mChart3.setTouchEnabled(true);
        mChart4.setTouchEnabled(true);
        mChart5.setTouchEnabled(true);
        mChart6.setTouchEnabled(true);
        mChart7.setTouchEnabled(true);
        mChart8.setTouchEnabled(true);

        // enable scaling and dragging
        mChart1.setDragEnabled(true);
        mChart1.setScaleEnabled(true);
        mChart1.setDrawGridBackground(false);

        mChart2.setDragEnabled(true);
        mChart2.setScaleEnabled(true);
        mChart2.setDrawGridBackground(false);

        mChart3.setDragEnabled(true);
        mChart3.setScaleEnabled(true);
        mChart3.setDrawGridBackground(false);

        mChart4.setDragEnabled(true);
        mChart4.setScaleEnabled(true);
        mChart4.setDrawGridBackground(false);

        mChart5.setDragEnabled(true);
        mChart5.setScaleEnabled(true);
        mChart5.setDrawGridBackground(false);

        mChart6.setDragEnabled(true);
        mChart6.setScaleEnabled(true);
        mChart6.setDrawGridBackground(false);

        mChart7.setDragEnabled(true);
        mChart7.setScaleEnabled(true);
        mChart7.setDrawGridBackground(false);

        mChart8.setDragEnabled(true);
        mChart8.setScaleEnabled(true);
        mChart8.setDrawGridBackground(false);

        // if disabled, scaling can be done on x- and y-axis separately
        mChart1.setPinchZoom(true);
        mChart2.setPinchZoom(true);
        mChart3.setPinchZoom(true);
        mChart4.setPinchZoom(true);
        mChart5.setPinchZoom(true);
        mChart6.setPinchZoom(true);
        mChart7.setPinchZoom(true);
        mChart8.setPinchZoom(true);

        // set an alternative background color
        mChart1.setBackgroundColor(Color.LTGRAY);
        mChart2.setBackgroundColor(Color.LTGRAY);
        mChart3.setBackgroundColor(Color.LTGRAY);
        mChart4.setBackgroundColor(Color.LTGRAY);
        mChart5.setBackgroundColor(Color.LTGRAY);
        mChart6.setBackgroundColor(Color.LTGRAY);
        mChart7.setBackgroundColor(Color.LTGRAY);
        mChart8.setBackgroundColor(Color.LTGRAY);

        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);

        // add empty data
        mChart1.setData(data);
        mChart2.setData(data);
        mChart3.setData(data);
        mChart4.setData(data);
        mChart5.setData(data);
        mChart6.setData(data);
        mChart7.setData(data);
        mChart8.setData(data);

        // get the legend (only possible after setting data)
        Legend l1 = mChart1.getLegend();
        Legend l2 = mChart2.getLegend();
        Legend l3 = mChart3.getLegend();
        Legend l4 = mChart4.getLegend();
        Legend l5 = mChart5.getLegend();
        Legend l6 = mChart6.getLegend();
        Legend l7 = mChart7.getLegend();
        Legend l8 = mChart8.getLegend();

        Typeface mTfLight = Typeface.createFromAsset(getAssets(), "OpenSans-Light.ttf");

        // modify the legend ...
        l1.setForm(Legend.LegendForm.LINE);
        l1.setTypeface(mTfLight);
        l1.setTextColor(Color.WHITE);

        l2.setForm(Legend.LegendForm.LINE);
        l2.setTypeface(mTfLight);
        l2.setTextColor(Color.WHITE);

        l3.setForm(Legend.LegendForm.LINE);
        l3.setTypeface(mTfLight);
        l3.setTextColor(Color.WHITE);

        l4.setForm(Legend.LegendForm.LINE);
        l4.setTypeface(mTfLight);
        l4.setTextColor(Color.WHITE);

        l5.setForm(Legend.LegendForm.LINE);
        l5.setTypeface(mTfLight);
        l5.setTextColor(Color.WHITE);

        l6.setForm(Legend.LegendForm.LINE);
        l6.setTypeface(mTfLight);
        l6.setTextColor(Color.WHITE);

        l7.setForm(Legend.LegendForm.LINE);
        l7.setTypeface(mTfLight);
        l7.setTextColor(Color.WHITE);

        l8.setForm(Legend.LegendForm.LINE);
        l8.setTypeface(mTfLight);
        l8.setTextColor(Color.WHITE);

        XAxis xl1 = mChart1.getXAxis();
        xl1.setTypeface(mTfLight);
        xl1.setTextColor(Color.WHITE);
        xl1.setDrawGridLines(false);
        xl1.setAvoidFirstLastClipping(true);
        xl1.setEnabled(false);

        XAxis xl2 = mChart2.getXAxis();
        xl2.setTypeface(mTfLight);
        xl2.setTextColor(Color.WHITE);
        xl2.setDrawGridLines(false);
        xl2.setAvoidFirstLastClipping(true);
        xl2.setEnabled(false);

        XAxis xl3 = mChart3.getXAxis();
        xl3.setTypeface(mTfLight);
        xl3.setTextColor(Color.WHITE);
        xl3.setDrawGridLines(false);
        xl3.setAvoidFirstLastClipping(true);
        xl3.setEnabled(false);

        XAxis xl4 = mChart4.getXAxis();
        xl4.setTypeface(mTfLight);
        xl4.setTextColor(Color.WHITE);
        xl4.setDrawGridLines(false);
        xl4.setAvoidFirstLastClipping(true);
        xl4.setEnabled(false);

        XAxis xl5 = mChart5.getXAxis();
        xl5.setTypeface(mTfLight);
        xl5.setTextColor(Color.WHITE);
        xl5.setDrawGridLines(false);
        xl5.setAvoidFirstLastClipping(true);
        xl5.setEnabled(false);

        XAxis xl6 = mChart6.getXAxis();
        xl6.setTypeface(mTfLight);
        xl6.setTextColor(Color.WHITE);
        xl6.setDrawGridLines(false);
        xl6.setAvoidFirstLastClipping(true);
        xl6.setEnabled(false);

        XAxis xl7 = mChart7.getXAxis();
        xl7.setTypeface(mTfLight);
        xl7.setTextColor(Color.WHITE);
        xl7.setDrawGridLines(false);
        xl7.setAvoidFirstLastClipping(true);
        xl7.setEnabled(false);

        XAxis xl8 = mChart8.getXAxis();
        xl8.setTypeface(mTfLight);
        xl8.setTextColor(Color.WHITE);
        xl8.setDrawGridLines(false);
        xl8.setAvoidFirstLastClipping(true);
        xl8.setEnabled(false);

        YAxis leftAxis1 = mChart1.getAxisLeft();
        leftAxis1.setTypeface(mTfLight);
        leftAxis1.setTextColor(Color.WHITE);
        leftAxis1.setAxisMaximum(35f);
        leftAxis1.setAxisMinimum(-35f);
        leftAxis1.setLabelCount(3, true);
        leftAxis1.setDrawGridLines(true);

        YAxis leftAxis2 = mChart2.getAxisLeft();
        leftAxis2.setTypeface(mTfLight);
        leftAxis2.setTextColor(Color.WHITE);
        leftAxis2.setAxisMaximum(35f);
        leftAxis2.setAxisMinimum(-35f);
        leftAxis2.setLabelCount(3, true);
        leftAxis2.setDrawGridLines(true);

        YAxis leftAxis3 = mChart3.getAxisLeft();
        leftAxis3.setTypeface(mTfLight);
        leftAxis3.setTextColor(Color.WHITE);
        leftAxis3.setAxisMaximum(35f);
        leftAxis3.setAxisMinimum(-35f);
        leftAxis3.setLabelCount(3, true);
        leftAxis3.setDrawGridLines(true);

        YAxis leftAxis4 = mChart4.getAxisLeft();
        leftAxis4.setTypeface(mTfLight);
        leftAxis4.setTextColor(Color.WHITE);
        leftAxis4.setAxisMaximum(35f);
        leftAxis4.setAxisMinimum(-35f);
        leftAxis4.setLabelCount(3, true);
        leftAxis4.setDrawGridLines(true);

        YAxis leftAxis5 = mChart5.getAxisLeft();
        leftAxis5.setTypeface(mTfLight);
        leftAxis5.setTextColor(Color.WHITE);
        leftAxis5.setAxisMaximum(35f);
        leftAxis5.setAxisMinimum(-35f);
        leftAxis5.setLabelCount(3, true);
        leftAxis5.setDrawGridLines(true);

        YAxis leftAxis6 = mChart6.getAxisLeft();
        leftAxis6.setTypeface(mTfLight);
        leftAxis6.setTextColor(Color.WHITE);
        leftAxis6.setAxisMaximum(35f);
        leftAxis6.setAxisMinimum(-35f);
        leftAxis6.setLabelCount(3, true);
        leftAxis6.setDrawGridLines(true);

        YAxis leftAxis7 = mChart7.getAxisLeft();
        leftAxis7.setTypeface(mTfLight);
        leftAxis7.setTextColor(Color.WHITE);
        leftAxis7.setAxisMaximum(35f);
        leftAxis7.setAxisMinimum(-35f);
        leftAxis7.setLabelCount(3, true);
        leftAxis7.setDrawGridLines(true);

        YAxis leftAxis8 = mChart8.getAxisLeft();
        leftAxis8.setTypeface(mTfLight);
        leftAxis8.setTextColor(Color.WHITE);
        leftAxis8.setAxisMaximum(35f);
        leftAxis8.setAxisMinimum(-35f);
        leftAxis8.setLabelCount(3, true);
        //leftAxis8.setGranularityEnabled(true);
        leftAxis8.setDrawGridLines(true);

        YAxis rightAxis1 = mChart1.getAxisRight();
        rightAxis1.setEnabled(false);

        YAxis rightAxis2 = mChart2.getAxisRight();
        rightAxis2.setEnabled(false);

        YAxis rightAxis3 = mChart3.getAxisRight();
        rightAxis3.setEnabled(false);

        YAxis rightAxis4 = mChart4.getAxisRight();
        rightAxis4.setEnabled(false);

        YAxis rightAxis5 = mChart5.getAxisRight();
        rightAxis5.setEnabled(false);

        YAxis rightAxis6 = mChart6.getAxisRight();
        rightAxis6.setEnabled(false);

        YAxis rightAxis7 = mChart7.getAxisRight();
        rightAxis7.setEnabled(false);

        YAxis rightAxis8 = mChart8.getAxisRight();
        rightAxis8.setEnabled(false);


    }

    ArrayList<Entry> lineEntries = new ArrayList<Entry>();
    int count = 0;

    public void addEntry(float f) {

        lineEntries.add(new Entry(count,f));

        count = count+1;

        LineDataSet set1 = createSet(lineEntries);
        //new LineDataSet(lineEntries,"legend");

        LineData data1 = new LineData(set1);

        data1.notifyDataChanged();
        mChart1.setData(data1);

        mChart1.notifyDataSetChanged();

        // limit the number of visible entries
        mChart1.setVisibleXRangeMaximum(20);

        // move to the latest entry
        mChart1.moveViewToX(data1.getEntryCount());
        
    }

    ArrayList<Entry> lineEntries2 = new ArrayList<Entry>();
    int count2 = 0;

    public void addEntry2(float f) {

        lineEntries2.add(new Entry(count2,f));

        count2 = count2+1;

        LineDataSet set2 = createSet2(lineEntries2);
        //new LineDataSet(lineEntries2,"legend");

        LineData data2 = new LineData(set2);

        data2.notifyDataChanged();
        mChart2.setData(data2);

        mChart2.notifyDataSetChanged();

        // limit the number of visible entries
        mChart2.setVisibleXRangeMaximum(20);

        // move to the latest entry
        mChart2.moveViewToX(data2.getEntryCount());

    }

    ArrayList<Entry> lineEntries3 = new ArrayList<Entry>();
    int count3 = 0;

    public void addEntry3(float f) {

        lineEntries3.add(new Entry(count3,f));

        count3 = count3+1;

        LineDataSet set3 = createSet3(lineEntries3);

        LineData data3 = new LineData(set3);

        data3.notifyDataChanged();
        mChart3.setData(data3);

        mChart3.notifyDataSetChanged();

        // limit the number of visible entries
        mChart3.setVisibleXRangeMaximum(20);

        // move to the latest entry
        mChart3.moveViewToX(data3.getEntryCount());
    }

    ArrayList<Entry> lineEntries4 = new ArrayList<Entry>();
    int count4 = 0;

    public void addEntry4(float f) {

        lineEntries4.add(new Entry(count4,f));

        count4 = count4+1;

        LineDataSet set4 = createSet4(lineEntries4);

        LineData data4 = new LineData(set4);

        data4.notifyDataChanged();
        mChart4.setData(data4);

        mChart4.notifyDataSetChanged();

        // limit the number of visible entries
        mChart4.setVisibleXRangeMaximum(20);

        // move to the latest entry
        mChart4.moveViewToX(data4.getEntryCount());
    }

    ArrayList<Entry> lineEntries5 = new ArrayList<Entry>();
    int count5 = 0;

    public void addEntry5(float f) {

        lineEntries5.add(new Entry(count5,f));

        count5 = count5+1;

        LineDataSet set5 = createSet5(lineEntries5);

        LineData data5 = new LineData(set5);

        data5.notifyDataChanged();
        mChart5.setData(data5);

        mChart5.notifyDataSetChanged();

        // limit the number of visible entries
        mChart5.setVisibleXRangeMaximum(20);

        // move to the latest entry
        mChart5.moveViewToX(data5.getEntryCount());
    }

    ArrayList<Entry> lineEntries6 = new ArrayList<Entry>();
    int count6 = 0;

    public void addEntry6(float f) {

        lineEntries6.add(new Entry(count6,f));

        count6 = count6+1;

        LineDataSet set6 = createSet6(lineEntries6);

        LineData data6 = new LineData(set6);

        data6.notifyDataChanged();
        mChart6.setData(data6);

        mChart6.notifyDataSetChanged();

        // limit the number of visible entries
        mChart6.setVisibleXRangeMaximum(20);

        // move to the latest entry
        mChart6.moveViewToX(data6.getEntryCount());
    }

    ArrayList<Entry> lineEntries7 = new ArrayList<Entry>();
    int count7 = 0;

    public void addEntry7(float f) {

        lineEntries7.add(new Entry(count7,f));

        count7 = count7+1;

        LineDataSet set7 = createSet7(lineEntries7);

        LineData data7 = new LineData(set7);

        data7.notifyDataChanged();
        mChart7.setData(data7);

        mChart7.notifyDataSetChanged();

        // limit the number of visible entries
        mChart7.setVisibleXRangeMaximum(20);

        // move to the latest entry
        mChart7.moveViewToX(data7.getEntryCount());
    }

    ArrayList<Entry> lineEntries8 = new ArrayList<Entry>();
    int count8 = 0;

    public void addEntry8(float f) {

        lineEntries8.add(new Entry(count8,f));

        count8 = count8+1;

        LineDataSet set8 = createSet8(lineEntries8);

        LineData data8 = new LineData(set8);

        data8.notifyDataChanged();
        mChart8.setData(data8);

        mChart8.notifyDataSetChanged();

        // limit the number of visible entries
        mChart8.setVisibleXRangeMaximum(20);

        // move to the latest entry
        mChart8.moveViewToX(data8.getEntryCount());
    }

    private LineDataSet createSet(ArrayList<Entry> le) {

        LineDataSet set1 = new LineDataSet(le, "S1");
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setColor(Color.rgb(255,165,0));  // Orange color
        set1.setCircleColor(Color.WHITE);
        set1.setLineWidth(1f);
        set1.setCircleRadius(1f);
        set1.setFillAlpha(65);
        set1.setFillColor(ColorTemplate.getHoloBlue());
        //set1.setHighLightColor(Color.rgb(244, 117, 117));
        set1.setValueTextColor(Color.WHITE);
        //set1.setValueTextSize(0.1f);
        //set1.setDrawValues(false);
        return set1;
    }

    private LineDataSet createSet2(ArrayList<Entry> le) {

        LineDataSet set2 = new LineDataSet(le, "S2");
        set2.setAxisDependency(YAxis.AxisDependency.LEFT);
        set2.setColor(Color.GREEN);
        set2.setCircleColor(Color.WHITE);
        set2.setLineWidth(1f);
        set2.setCircleRadius(1f);
        set2.setFillAlpha(65);
        //set.setHighLightColor(Color.rgb(44, 117, 117));
        set2.setValueTextColor(Color.WHITE);
        //set2.setValueTextSize(0.1f);
        //set2.setDrawValues(false);
        return set2;
    }

    private LineDataSet createSet3(ArrayList<Entry> le) {

        LineDataSet set3 = new LineDataSet(le, "S3");
        set3.setAxisDependency(YAxis.AxisDependency.LEFT);
        set3.setColor(Color.CYAN);
        set3.setCircleColor(Color.WHITE);
        set3.setLineWidth(1f);
        set3.setCircleRadius(1f);
        set3.setFillAlpha(65);
        //set.setHighLightColor(Color.rgb(44, 117, 117));
        set3.setValueTextColor(Color.WHITE);
        //set3.setValueTextSize(0.1f);
        //set3.setDrawValues(false);
        return set3;
    }

    private LineDataSet createSet4(ArrayList<Entry> le) {

        LineDataSet set4 = new LineDataSet(le, "S4");
        set4.setAxisDependency(YAxis.AxisDependency.LEFT);
        set4.setColor(Color.MAGENTA);
        set4.setCircleColor(Color.WHITE);
        set4.setLineWidth(1f);
        set4.setCircleRadius(1f);
        set4.setFillAlpha(65);
        //set.setHighLightColor(Color.rgb(44, 117, 117));
        set4.setValueTextColor(Color.WHITE);
        //set4.setValueTextSize(0.1f);
        //set4.setDrawValues(false);
        return set4;
    }

    private LineDataSet createSet5(ArrayList<Entry> le) {

        LineDataSet set5 = new LineDataSet(le, "S5");
        set5.setAxisDependency(YAxis.AxisDependency.LEFT);
        set5.setColor(Color.rgb(139,69,19));  // Brown color
        set5.setCircleColor(Color.WHITE);
        set5.setLineWidth(1f);
        set5.setCircleRadius(1f);
        set5.setFillAlpha(65);
        //set.setHighLightColor(Color.rgb(44, 117, 117));
        set5.setValueTextColor(Color.WHITE);
        //set5.setValueTextSize(0.1f);
        //set5.setDrawValues(false);
        return set5;
    }

    private LineDataSet createSet6(ArrayList<Entry> le) {

        LineDataSet set6 = new LineDataSet(le, "S6");
        set6.setAxisDependency(YAxis.AxisDependency.LEFT);
        set6.setColor(Color.BLACK);
        set6.setCircleColor(Color.WHITE);
        set6.setLineWidth(1f);
        set6.setCircleRadius(1f);
        set6.setFillAlpha(65);
        //set.setHighLightColor(Color.rgb(44, 117, 117));
        set6.setValueTextColor(Color.WHITE);
        //set6.setValueTextSize(0.1f);
        //set6.setDrawValues(false);
        return set6;
    }

    private LineDataSet createSet7(ArrayList<Entry> le) {

        LineDataSet set7 = new LineDataSet(le, "S7");
        set7.setAxisDependency(YAxis.AxisDependency.LEFT);
        set7.setColor(Color.YELLOW);
        set7.setCircleColor(Color.WHITE);
        set7.setLineWidth(1f);
        set7.setCircleRadius(1f);
        set7.setFillAlpha(65);
        //set.setHighLightColor(Color.rgb(44, 117, 117));
        set7.setValueTextColor(Color.WHITE);
        //set7.setValueTextSize(0.1f);
        //set7.setDrawValues(false);
        return set7;
    }

    private LineDataSet createSet8(ArrayList<Entry> le) {

        LineDataSet set8 = new LineDataSet(le, "S8");
        set8.setAxisDependency(YAxis.AxisDependency.LEFT);
        set8.setColor(Color.RED);
        set8.setCircleColor(Color.WHITE);
        set8.setLineWidth(1f);
        set8.setCircleRadius(1f);
        set8.setFillAlpha(65);
        //set.setHighLightColor(Color.rgb(44, 117, 117));
        set8.setValueTextColor(Color.WHITE);
        //set8.setValueTextSize(0.1f);
        //set8.setDrawValues(false);
        return set8;
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
        switch(item.getItemId()) {
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

    private int cnt = 0;
    private void displayData(String data) {
        cnt += 1;
        // Conversion formula: V_in = X*1.65V/(1000 * GAIN * 2048)
        // Assuming GAIN = 64
        if (cnt % 10 == 0) {
            final float numerator = 1650000;
            final float denominator = 1000 * 64 * 2048;
            final String[] parts = data.split(" ");
            final List<Float> data_raw = new ArrayList<>();



            addEntry((Float.parseFloat(parts[0]) * numerator ) / denominator);
            addEntry2((Float.parseFloat(parts[1]) * numerator ) / denominator);
            addEntry3((Float.parseFloat(parts[2]) * numerator ) / denominator);
            addEntry4((Float.parseFloat(parts[3]) * numerator ) / denominator);
            addEntry5((Float.parseFloat(parts[4]) * numerator ) / denominator);
            addEntry6((Float.parseFloat(parts[5]) * numerator ) / denominator);
            addEntry7((Float.parseFloat(parts[6]) * numerator ) / denominator);
            addEntry8((Float.parseFloat(parts[7]) * numerator ) / denominator);
        }


//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                int count = 1;
//                for (String part : parts) {
//                    float f = Float.parseFloat(part);
//                    f = (f * numerator) / denominator;
//                    data_raw.add(f);
//                    switch (count) {
//                        case 1:
//                            addEntry(f);
//                            break;
//                        case 2:
//                            addEntry2(f);
//                            break;
//                        case 3:
//                            addEntry3(f);
//                            break;
//                        case 4:
//                            addEntry4(f);
//                            break;
//                        case 5:
//                            addEntry5(f);
//                            break;
//                        case 6:
//                            addEntry6(f);
//                            break;
//                        case 7:
//                            addEntry7(f);
//                            break;
//                        case 8:
//                            addEntry8(f);
//                            break;
//                    }
//                    count++;
//                }
//            }
//        }).start();

        if (data != null) {
            // data format example: +01012 -00234 +01374 -01516 +01656 +01747 +00131 -00351
            mDataField.setText(data); // print the n-dimensional array after the data
        }
    }

//    private INDArray dataToMicroVolts(INDArray data) {
//        // Conversion formula: V_in = X*1.65V/(1000 * GAIN * 2048)
//        // Assuming GAIN = 64
//        float denominator = 1000 * 64 * 2048;
//        INDArray result = (data.mul(1650000)).div(denominator);
//        return result;
//    }
    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            if(uuid.equals("a22686cb-9268-bd91-dd4f-b52d03d85593")) {
                currentServiceData.put(LIST_NAME, "EEG Data Service");
                currentServiceData.put(LIST_UUID, uuid);
                gattServiceData.add(currentServiceData);

                ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                        new ArrayList<HashMap<String, String>>();
                List<BluetoothGattCharacteristic> gattCharacteristics =
                        gattService.getCharacteristics();
                ArrayList<BluetoothGattCharacteristic> charas =
                        new ArrayList<BluetoothGattCharacteristic>();

                // Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    charas.add(gattCharacteristic);

                    HashMap<String, String> currentCharaData = new HashMap<String, String>();
                    uuid = gattCharacteristic.getUuid().toString();
                    currentCharaData.put(
                            LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                    // If not really needed since the filtered service has only one characteristic
                    if(uuid.equals("faa7b588-19e5-f590-0545-c99f193c5c3e")){
                        currentCharaData.put(LIST_NAME, "EEG Data Values");
                        currentCharaData.put(LIST_UUID, uuid);
                        gattCharacteristicGroupData.add(currentCharaData);
                    }
                }
                mGattCharacteristics.add(charas);
                gattCharacteristicData.add(gattCharacteristicGroupData);
            }
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
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
