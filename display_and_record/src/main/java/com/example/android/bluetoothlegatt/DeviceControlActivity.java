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
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import androidx.annotation.Nullable;
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
    final Handler handler = new Handler();
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    double freq;
    String key = "";
    float res_time;
    float res_freq;
    List<Float> dp_received = new ArrayList<>();
    Timer timer;
    Timer timer2;
    TimerTask timerTask;
    TimerTask timerTask2;
    TimerTask timerTask3;
    List<ImmutablePair<Integer, Integer>> keys = new ArrayList<>();
    List<ImmutablePair<String, Double>> notes = new ArrayList<>();
    List<ImmutablePair<String, Double>> stimuli_data = new ArrayList<>();
    List<Float> s_times = new ArrayList<>();
    MediaPlayer mMediaPlayer = new MediaPlayer();
    // constants
    int NKEYS = 12;
    int MINOCTAVE = 4;
    int MAXOCTAVE = 6;
    int STIMULUS_START = 4000;
    int STIMULUS_LENGTH = 1000;
    int PERIOD = 3000;  // milliseconds
    int SILENCE_START = 5500;
    int TUNING = 440;
    float DATAPOINT_TIME = 4.5f;
    int DPS_AVG_CNT = 20;
    String TONES_PATH = Environment.getExternalStorageDirectory().getPath() + "/Tones/";
    ArrayList<Entry> lineEntries1 = new ArrayList<Entry>();
    int cnt = 0;
    ArrayList<Entry> lineEntries2 = new ArrayList<Entry>();
    ArrayList<Entry> lineEntries3 = new ArrayList<Entry>();
    ArrayList<Entry> lineEntries4 = new ArrayList<Entry>();
    ArrayList<Entry> lineEntries5 = new ArrayList<Entry>();
    ArrayList<Entry> lineEntries6 = new ArrayList<Entry>();
    ArrayList<Entry> lineEntries7 = new ArrayList<Entry>();
    ArrayList<Entry> lineEntries8 = new ArrayList<Entry>();
    int ch1_color;
    int ch2_color;
    int ch3_color;
    int ch4_color;
    int ch5_color;
    int ch6_color;
    int ch7_color;
    int ch8_color;
    boolean show_ch1 = true;
    boolean show_ch2 = true;
    boolean show_ch3 = true;
    boolean show_ch4 = true;
    boolean show_ch5 = true;
    boolean show_ch6 = true;
    boolean show_ch7 = true;
    boolean show_ch8 = true;
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
    private TextView mDataResolution;
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            } else mBluetoothLeService.connect(mDeviceAddress);
            // Automatically connects to the device upon successful start-up initialization.
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic mWriteCharacteristic;
    private LineChart mChart;
    private Button btn_record;
    private Switch switch_plots;
    private View layout_plots;
    private Spinner gain_spinner;
    private boolean recording = false;
    private boolean plotting = false;
    private boolean playing = false;
    private List<float[]> main_data;
    private float data_cnt = 0;
    private long start_data = 0;
    private long last_data = 0;
    private String start_time;
    private String end_time;
    private long start_watch;
    private String recording_time;
    private String session_label;
    private long start_timestamp;
    private long end_timestamp;
    private String selected_gain = "1";  // default gain
    private View.OnClickListener btnRecordOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!recording) {
                notes.clear();
                keys.clear();
                s_times.clear();
                keys.addAll(generateKeys());
                notes.addAll(keysToNotes(keys));
                askForLabel();
            } else endTrial();
        }
    };
    private CompoundButton.OnCheckedChangeListener switchPlotsOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (!isChecked) {
                layout_plots.setVisibility(ViewStub.GONE);
                plotting = false;
            } else {
                layout_plots.setVisibility(ViewStub.VISIBLE);
                plotting = true;
            }
        }
    };
    private List<List<Float>> accumulated = new ArrayList<>();
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
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                if (mNotifyCharacteristic == null) {
                    readGattCharacteristic(mBluetoothLeService.getSupportedGattServices());
                }
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                data_cnt++;
                switch_plots.setEnabled(true);
                List<Float> microV = transData(intent.getIntArrayExtra(BluetoothLeService.EXTRA_DATA));
                displayData(microV);
                if (plotting) plotData(microV);
                if (recording) storeData(microV);
                if (start_data == 0) start_data = System.currentTimeMillis();
                last_data = System.currentTimeMillis();
                res_time = (last_data - start_data) / data_cnt;
                res_freq = (1 / res_time) * 1000;
                String hertz = String.valueOf((int) res_freq) + "Hz";
                String resolution = String.format("%.2f", res_time) + "ms - ";
                String content = resolution + hertz;
                mDataResolution.setText(content);
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
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    private List<ImmutablePair<Integer, Integer>> generateKeys() {
        List<ImmutablePair<Integer, Integer>> new_keys = new ArrayList<>();
        for (int o = MINOCTAVE; o <= MAXOCTAVE; o++) {
            for (int k = 0; k < NKEYS; k++) {
                ImmutablePair<Integer, Integer> pair = new ImmutablePair<>(k, o);
                new_keys.add(pair);
            }
        }
        // randomize list
        Collections.shuffle(new_keys);
        return new_keys;
    }

    private List<ImmutablePair<String, Double>> keysToNotes(List<ImmutablePair<Integer, Integer>> keysList) {
        char letter = ' ';
        char accidental = ' ';
        String note;
        List<ImmutablePair<String, Double>> notesFreqs = new ArrayList<>();
        for (ImmutablePair<Integer, Integer> pair : keysList) {
            int key = pair.getKey();  // e.g note from 0 to 11
            int octave = pair.getValue(); // e.g octave from 2 to 5
            accidental = ' ';
            switch (key) {
                case 0:
                    letter = 'C';
                    break;
                case 1:
                    letter = 'C';
                    accidental = '#';
                    break;
                case 2:
                    letter = 'D';
                    break;
                case 3:
                    letter = 'D';
                    accidental = '#';
                    break;
                case 4:
                    letter = 'E';
                    break;
                case 5:
                    letter = 'F';
                    break;
                case 6:
                    letter = 'F';
                    accidental = '#';
                    break;
                case 7:
                    letter = 'G';
                    break;
                case 8:
                    letter = 'G';
                    accidental = '#';
                    break;
                case 9:
                    letter = 'A';
                    break;
                case 10:
                    letter = 'A';
                    accidental = '#';
                    break;
                case 11:
                    letter = 'B';
                    break;
            }
            if (accidental != ' ') note = letter + "" + accidental + "" + octave;  // e.g. C#5
            else note = letter + "" + octave; // e.g. C5
            NoteToFrequency mNotetoFreq = new NoteToFrequency(letter, accidental, octave, TUNING);
            double frequency = mNotetoFreq.frequency;
            ImmutablePair<String, Double> noteFreqPair = new ImmutablePair<>(note, frequency);
            notesFreqs.add(noteFreqPair);
        }
        return notesFreqs;
    }

    public void startTimer() {
        //set a new Timer
        timer = new Timer();
        //initialize the TimerTask's job
        initializeTimerTask();
        // schedule the timer, the stimulus presence will repeat every 3 seconds
        timer.schedule(timerTask, STIMULUS_START, PERIOD);
        // the silence (stop MediaPlayer) starts after 5.5 seconds, repeat every 3 seconds
        // done this way to avoid "click" sounds at the end of the presentation
        timer.schedule(timerTask2, SILENCE_START, PERIOD);
    }

    public void initializeTimerTask2() {
        timerTask3 = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        playing = false;
                        timer2.cancel();
                        timer2.purge();
                    }
                });
            }
        };
    }

    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        if (!notes.isEmpty()) {
                            mMediaPlayer = new MediaPlayer();
                            ImmutablePair<String, Double> note = notes.get(0);
                            key = note.getKey();  // same as .getLeft()
                            freq = note.getValue();
                            String solfa = "";
                            switch (key.charAt(0)) {
                                case 'A':
                                    solfa = "La";
                                    break;
                                case 'B':
                                    solfa = "Si";
                                    break;
                                case 'C':
                                    solfa = "Do";
                                    break;
                                case 'D':
                                    solfa = "Re";
                                    break;
                                case 'E':
                                    solfa = "Mi";
                                    break;
                                case 'F':
                                    solfa = "Fa";
                                    break;
                                case 'G':
                                    solfa = "Sol";
                                    break;
                            }
                            try {
                                mMediaPlayer.setDataSource(TONES_PATH + key + ".wav");
                                mMediaPlayer.prepare();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            float s_appearance = System.currentTimeMillis() - start_watch;
                            s_times.add(s_appearance);
                            String key_n_freq = key + " = " + freq + "Hz";
                            XAxis bottom = mChart.getXAxis();
                            LimitLine ll_start = new LimitLine(s_appearance, key_n_freq);
                            LimitLine ll_stop = new LimitLine(s_appearance + 1, "Silence");
                            ll_start.setLineColor(Color.GREEN);
                            ll_start.setTextColor(Color.GREEN);
                            ll_stop.setLineColor(Color.RED);
                            ll_stop.setTextColor(Color.RED);
                            bottom.addLimitLine(ll_start);
                            bottom.addLimitLine(ll_stop);
                            mChart.notifyDataSetChanged();
                            mMediaPlayer.start();
                            playing = true;
                            initializeTimerTask2();
                            timer2 = new Timer();
                            timer2.schedule(timerTask3, STIMULUS_LENGTH);  // run only once
                            notes.remove(0);
                            Toast.makeText(
                                    getApplicationContext(),
                                    solfa + "/" + key_n_freq,
                                    Toast.LENGTH_LONG
                            ).show();
                        } else {
                            timer.cancel();
                            endTrial();
                        }
                    }
                });
            }
        };
        timerTask2 = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        mMediaPlayer.release();
                        mMediaPlayer = null;
                    }
                });
            }
        };
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

    private void startTrial() {
        cnt = 0;
        main_data = new ArrayList<>();
        start_time = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
        start_watch = System.currentTimeMillis();
        start_timestamp = new Timestamp(start_watch).getTime();
        recording = true;
        btn_record.setText("Stop and Store Data");
    }

    private void endTrial() {
        timer.cancel();
        recording = false;
        end_time = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
        long stop_watch = System.currentTimeMillis();
        end_timestamp = new Timestamp(stop_watch).getTime();
        recording_time = Long.toString(stop_watch - start_watch);
        btn_record.setText("Saving EEG session...");
        if (session_label == null) saveSession();
        else saveSession(session_label);
        session_label = null;
        Toast.makeText(
                getApplicationContext(),
                "Your EEG session was successfully stored",
                Toast.LENGTH_LONG
        ).show();
        btn_record.setText("Record");
    }

    private void askForLabel() {
        new MaterialDialog.Builder(this)
                .title("Please, enter the session label")
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input("E.g. walking, eating, sleeping, etc.",
                        "", new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(MaterialDialog dialog, CharSequence input) {
                                session_label = input.toString();
                                // Use a new tread as this can take a while
                                // onResume we start our timer so it can start when the app comes from the background
                                startTrial();
                                startTimer();
                            }
                        }).show();
    }

    private List<Float> transData(int[] data) {
        // Assuming GAIN = 64
        // Conversion formula: V_in = X*1.65V/(1000 * GAIN * 2048)
        float gain = Float.parseFloat(selected_gain);
        float numerator = 1650;
        float denominator =  gain * 2048;
        List<Float> data_trans = new ArrayList<>();
        for (int datapoint : data) {
            float dp = datapoint;
            data_trans.add((dp * numerator) / denominator);
        }
        return data_trans;
    }

    private void plotData(List<Float> current) {
        // test 0 -> plot all values (this library and/or the smartphone can't handle it)
//        addEntries(current);
//         test 1 -> plot only every certain counter
        if (cnt % DPS_AVG_CNT == 0) addEntries(current);
        cnt++;
        // test 2 -> plot the average of a certain number
//        if (accumulated.size() < DPS_AVG_CNT) accumulated.add(current);
//        else {
//            List<Float> data_microV = new ArrayList<>();
//            for (int j = 0; j < current.size(); j++) {
//                float sum = 0;
//                for (int i = 0; i < accumulated.size(); i++) sum += accumulated.get(i).get(j);
//                float avg = sum / accumulated.size();
//                data_microV.add(avg);
//            }
//            addEntries(data_microV);
//            accumulated = new ArrayList<>();
//            accumulated.add(current);
//        }
    }

    private void displayData(List<Float> data_microV) {
        if (data_microV != null) {
            // data format example: +01012 -00234 +01374 -01516 +01656 +01747 +00131 -00351
            String trans = "";
            List<String> values = new ArrayList<String>();
            for (Float value : data_microV) {
                if (value >= 0) {
                    trans += "+";
                    trans += String.format("%5.2f", value);
                } else trans += String.format("%5.2f", value);
                values.add(trans);
                trans = "";
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

    private void storeData(List<Float> data_microV) {
        float[] f_microV = new float[data_microV.size()];
        float curr_received = System.currentTimeMillis() - start_watch;
        dp_received.add(curr_received);
        int i = 0;
        for (Float f : data_microV) {
            f_microV[i++] = (f != null ? f : Float.NaN); // Or whatever default you want
        }
        main_data.add(f_microV);
        ImmutablePair<String, Double> stimuli = new ImmutablePair<>(key, freq);
        if (!playing) stimuli = new ImmutablePair<>("silence", 0.0);
        stimuli_data.add(stimuli);
    }

    private void saveSession() {
        saveSession("Default");
    }

    private void saveSession(String tag) {
        String top_header = "Session ID,Session Tag,Date,Shape (rows x columns)," +
                "Duration (ms),Starting Time,Ending Time,Resolution (ms),Resolution (Hz)," +
                "Unit Measure,Starting Timestamp,Ending Timestamp";
        String dp_header = "Time,Ch-1,Ch-2,Ch-3,Ch-4,Ch-5,Ch-6,Ch-7,Ch-8,Key,Freq";
        UUID id = UUID.randomUUID();
        String date = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss").format(new Date());
        Character delimiter = ',';
        Character break_line = '\n';
        Float current;
        try {
            File formatted = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS),
                    date + ".csv");
            // if file doesn't exists, then create it
            if (!formatted.exists()) formatted.createNewFile();
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
            fileWriter.append(rows + "x" + cols);
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
            fileWriter.append("Stimuli appearance");
            fileWriter.append(delimiter);
            for (float time : s_times) {
                fileWriter.append(String.valueOf(time));
                fileWriter.append(delimiter);
            }
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
                fileWriter.append(stimuli_data.get(i).getLeft());
                fileWriter.append(delimiter);
                fileWriter.append(stimuli_data.get(i).getRight().toString());
                fileWriter.append(delimiter);
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
        ch1_color = ContextCompat.getColor(getApplicationContext(), R.color.aqua);
        ch2_color = ContextCompat.getColor(getApplicationContext(), R.color.fuchsia);
        ch3_color = ContextCompat.getColor(getApplicationContext(), R.color.green);
        ch4_color = ContextCompat.getColor(getApplicationContext(), android.R.color.holo_purple);
        ch5_color = ContextCompat.getColor(getApplicationContext(), R.color.orange);
        ch6_color = ContextCompat.getColor(getApplicationContext(), R.color.red);
        ch7_color = ContextCompat.getColor(getApplicationContext(), R.color.yellow);
        ch8_color = ContextCompat.getColor(getApplicationContext(), R.color.black);
        btn_record = (Button) findViewById(R.id.btn_record);
        switch_plots = (Switch) findViewById(R.id.switch_plots);
        gain_spinner = (Spinner) findViewById(R.id.gain_spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.gains, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        gain_spinner.setAdapter(adapter);
        gain_spinner.setSelection(1);
        gain_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                float max = 1700f;
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
        btn_record.setOnClickListener(btnRecordOnClickListener);
        switch_plots.setOnCheckedChangeListener(switchPlotsOnCheckedChangeListener);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mCh1 = (TextView) findViewById(R.id.ch1);
        mCh2 = (TextView) findViewById(R.id.ch2);
        mCh3 = (TextView) findViewById(R.id.ch3);
        mCh4 = (TextView) findViewById(R.id.ch4);
        mCh5 = (TextView) findViewById(R.id.ch5);
        mCh6 = (TextView) findViewById(R.id.ch6);
        mCh7 = (TextView) findViewById(R.id.ch7);
        mCh8 = (TextView) findViewById(R.id.ch8);
        mCh1.setTextColor(ch1_color);
        mCh2.setTextColor(ch2_color);
        mCh3.setTextColor(ch3_color);
        mCh4.setTextColor(ch4_color);
        mCh5.setTextColor(ch5_color);
        mCh6.setTextColor(ch6_color);
        mCh7.setTextColor(ch7_color);
        mCh8.setTextColor(ch8_color);
        chckbx_ch1 = (CheckBox) findViewById(R.id.checkBox_ch1);
        chckbx_ch2 = (CheckBox) findViewById(R.id.checkBox_ch2);
        chckbx_ch3 = (CheckBox) findViewById(R.id.checkBox_ch3);
        chckbx_ch4 = (CheckBox) findViewById(R.id.checkBox_ch4);
        chckbx_ch5 = (CheckBox) findViewById(R.id.checkBox_ch5);
        chckbx_ch6 = (CheckBox) findViewById(R.id.checkBox_ch6);
        chckbx_ch7 = (CheckBox) findViewById(R.id.checkBox_ch7);
        chckbx_ch8 = (CheckBox) findViewById(R.id.checkBox_ch8);
        chckbx_ch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) show_ch1 = true;
                else show_ch1 = false;
            }
        });
        chckbx_ch2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) show_ch2 = true;
                else show_ch2 = false;
            }
        });
        chckbx_ch3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) show_ch3 = true;
                else show_ch3 = false;
            }
        });
        chckbx_ch4.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) show_ch4 = true;
                else show_ch4 = false;
            }
        });
        chckbx_ch5.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) show_ch5 = true;
                else show_ch5 = false;
            }
        });
        chckbx_ch6.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) show_ch6 = true;
                else show_ch6 = false;
            }
        });
        chckbx_ch7.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) show_ch7 = true;
                else show_ch7 = false;
            }
        });
        chckbx_ch8.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) show_ch8 = true;
                else show_ch8 = false;
            }
        });
        mDataResolution = (TextView) findViewById(R.id.resolution_value);
        getActionBar().setDisplayHomeAsUpEnabled(false);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        setChart();
    }

    void setChart() {
        OnChartValueSelectedListener ol = new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry entry, Highlight h) {
                //entry.getData() returns null here
            }
            @Override
            public void onNothingSelected() {

            }
        };
        mChart = (LineChart) findViewById(R.id.layout_chart);
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
        // TODO: Make stimuli start and stop vertical lines appear in real-time
        // TODO: Block the last checkbox to prevent empty plot and app forceclose
        // disable the y right axis
        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);
        // set the x bottom axis
        XAxis bottomAxis = mChart.getXAxis();
        bottomAxis.setLabelCount(5, false);
        bottomAxis.setPosition(XAxis.XAxisPosition.TOP);
        bottomAxis.setGridColor(Color.WHITE);
        bottomAxis.setTextColor(Color.GRAY);
    }

    public void addEntries(List<Float> f) {
        List<ILineDataSet> datasets = new ArrayList<>(); // for adding multiple plots
        float x = cnt * DATAPOINT_TIME;
        if (show_ch1) {
            lineEntries1.add(new Entry(x, f.get(0)));
            LineDataSet set1 = createSet1(lineEntries1);
            datasets.add(set1);
        }
        if (show_ch2) {
            lineEntries2.add(new Entry(x, f.get(1)));
            LineDataSet set2 = createSet2(lineEntries2);
            datasets.add(set2);
        }
        if (show_ch3) {
            lineEntries3.add(new Entry(x, f.get(2)));
            LineDataSet set3 = createSet3(lineEntries3);
            datasets.add(set3);
        }
        if (show_ch4) {
            lineEntries4.add(new Entry(x, f.get(3)));
            LineDataSet set4 = createSet4(lineEntries4);
            datasets.add(set4);
        }
        if (show_ch5) {
            lineEntries5.add(new Entry(x, f.get(4)));
            LineDataSet set5 = createSet5(lineEntries5);
            datasets.add(set5);
        }
        if (show_ch6) {
            lineEntries6.add(new Entry(x, f.get(5)));
            LineDataSet set6 = createSet6(lineEntries6);
            datasets.add(set6);
        }
        if (show_ch7) {
            lineEntries7.add(new Entry(x, f.get(6)));
            LineDataSet set7 = createSet7(lineEntries7);
            datasets.add(set7);
        }
        if (show_ch8) {
            lineEntries8.add(new Entry(x, f.get(7)));
            LineDataSet set8 = createSet8(lineEntries8);
            datasets.add(set8);
        }
        LineData linedata = new LineData(datasets);
        linedata.notifyDataChanged();
        mChart.setData(linedata);
        mChart.notifyDataSetChanged();
        // limit the number of visible entries
        mChart.setVisibleXRangeMaximum(500);  // in this case you always see 1000ms
        // move to the latest entry
        mChart.moveViewToX(x);
    }


    private LineDataSet createSet1(ArrayList<Entry> le) {
        LineDataSet set1 = new LineDataSet(le, "Ch-1");
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setColor(ch1_color);
        set1.setCircleColor(Color.WHITE);
        set1.setLineWidth(1f);
        set1.setCircleRadius(1f);
        set1.setFillAlpha(65);
        set1.setFillColor(ColorTemplate.getHoloBlue());
        set1.setValueTextColor(ch1_color);
        return set1;
    }

    private LineDataSet createSet2(ArrayList<Entry> le) {
        LineDataSet set2 = new LineDataSet(le, "Ch-2");
        set2.setAxisDependency(YAxis.AxisDependency.LEFT);
        set2.setColor(ch2_color);
        set2.setCircleColor(Color.WHITE);
        set2.setLineWidth(1f);
        set2.setCircleRadius(1f);
        set2.setFillAlpha(65);
        set2.setValueTextColor(ch2_color);
        return set2;
    }

    private LineDataSet createSet3(ArrayList<Entry> le) {
        LineDataSet set3 = new LineDataSet(le, "Ch-3");
        set3.setAxisDependency(YAxis.AxisDependency.LEFT);
        set3.setColor(ch3_color);
        set3.setCircleColor(Color.WHITE);
        set3.setLineWidth(1f);
        set3.setCircleRadius(1f);
        set3.setFillAlpha(65);
        set3.setValueTextColor(ch3_color);
        return set3;
    }

    private LineDataSet createSet4(ArrayList<Entry> le) {
        LineDataSet set4 = new LineDataSet(le, "Ch-4");
        set4.setAxisDependency(YAxis.AxisDependency.LEFT);
        set4.setColor(ch4_color);
        set4.setCircleColor(Color.WHITE);
        set4.setLineWidth(1f);
        set4.setCircleRadius(1f);
        set4.setFillAlpha(65);
        set4.setValueTextColor(ch4_color);
        return set4;
    }

    private LineDataSet createSet5(ArrayList<Entry> le) {
        LineDataSet set5 = new LineDataSet(le, "Ch-5");
        set5.setAxisDependency(YAxis.AxisDependency.LEFT);
        set5.setColor(ch5_color);
        set5.setCircleColor(Color.WHITE);
        set5.setLineWidth(1f);
        set5.setCircleRadius(1f);
        set5.setFillAlpha(65);
        set5.setValueTextColor(ch5_color);
        return set5;
    }

    private LineDataSet createSet6(ArrayList<Entry> le) {
        LineDataSet set6 = new LineDataSet(le, "Ch-6");
        set6.setAxisDependency(YAxis.AxisDependency.LEFT);
        set6.setColor(ch6_color);
        set6.setCircleColor(Color.WHITE);
        set6.setLineWidth(1f);
        set6.setCircleRadius(1f);
        set6.setFillAlpha(65);
        set6.setValueTextColor(ch6_color);
        return set6;
    }

    private LineDataSet createSet7(ArrayList<Entry> le) {
        LineDataSet set7 = new LineDataSet(le, "Ch-7");
        set7.setAxisDependency(YAxis.AxisDependency.LEFT);
        set7.setColor(ch7_color);
        set7.setCircleColor(Color.WHITE);
        set7.setLineWidth(1f);
        set7.setCircleRadius(1f);
        set7.setFillAlpha(65);
        set7.setValueTextColor(ch7_color);
        return set7;
    }

    private LineDataSet createSet8(ArrayList<Entry> le) {
        LineDataSet set8 = new LineDataSet(le, "Ch-8");
        set8.setAxisDependency(YAxis.AxisDependency.LEFT);
        set8.setColor(ch8_color);
        set8.setCircleColor(Color.WHITE);
        set8.setLineWidth(1f);
        set8.setCircleRadius(1f);
        set8.setFillAlpha(65);
        set8.setValueTextColor(ch8_color);
        return set8;
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
        String uuid = null;
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();
            if (uuid.equals("a22686cb-9268-bd91-dd4f-b52d03d85593")) {

            } else {
                List<BluetoothGattCharacteristic> gattCharacteristics =
                        gattService.getCharacteristics();
                // Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    // uuid -> "faa7b588-19e5-f590-0545-c99f193c5c3e"
                    // start reading the EEG data received from this gatt characteristic
                    final int charaProp = gattCharacteristic.getProperties();
                    if (((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) |
                            (charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0) {
                        // writing characteristic functions
                        mWriteCharacteristic = gattCharacteristic;
                        final byte[] stored = mWriteCharacteristic.getValue();
                        if (stored != null) {
                            Toast.makeText(
                                    getApplicationContext(),
                                    "CHARACTERISTIC VALUE = " + stored.toString(),
                                    Toast.LENGTH_LONG
                            ).show();
                        } else {
                            // gain-> {0.5:0b111, 1:0b000, 2:0b001, 4:0b010, 8:0b011, 16:0b100, 32:0b101,64:0b110}
                            final byte[] newValue = new byte[6];
                            newValue[4] = 0b110;
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
                            mWriteCharacteristic.setValue(newValue);
                            mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
                            mBluetoothLeService.disconnect();
                            mBluetoothLeService.connect(mDeviceAddress);
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
        String uuid = null;
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
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
                    mBluetoothLeService.disconnect();
                    mBluetoothLeService.connect(mDeviceAddress);
                }
            }
        }
    }
}
