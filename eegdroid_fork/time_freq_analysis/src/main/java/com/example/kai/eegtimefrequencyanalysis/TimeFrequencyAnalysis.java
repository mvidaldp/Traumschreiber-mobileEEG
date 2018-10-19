package com.example.kai.eegtimefrequencyanalysis;

import android.graphics.Bitmap;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.Toast;

import org.apache.commons.math3.complex.Complex;
import com.example.kai.eegtimefrequencyanalysis.utilities.CustomFFT;
import com.example.kai.eegtimefrequencyanalysis.utilities.FFTWWrapper;
import com.example.kai.eegtimefrequencyanalysis.utilities.Utilities;
import com.example.kai.eegtimefrequencyanalysis.utilities.ZoomableImageView;
import com.opencsv.CSVReader;

import java.io.File;
import java.io.InputStreamReader;

public class TimeFrequencyAnalysis extends AppCompatActivity {

    //ZoomableImageView bmpView;
    private Spinner widthSpinner;
    private Spinner channelSpinner;
    private Spinner overlapSpinner;
    private ImageView bmpView;
    private static final String[] channels = {"1", "2", "3", "4", "5", "6","7", "8"};
    private static final String[] widths = {"1", "2", "3", "4", "5"};
    private static final String[] overlaps = {"0", "1/4", "1/3", "1/2"};
    private static int FS = 225;
    public static int WIDTH = 2*FS;
    public static int CHANNEL = 0;
    public static int OVERLAP = WIDTH/2;
    public double[][] eegData;
    public static int MAXHERTZ = 60;
    public static String WINDOW = "hanning";
    private static int MAX_CHART_HEIGHT;
    private static int MAX_CHART_WIDTH;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_frequency_analysis);
        bmpView = findViewById(R.id.spectogram);
        //bmpView = (ZoomableImageView)findViewById(R.id.spectogram);

        // GENERATE DUMMY DATA
        //double[] sinesData = generateDummyData();

        //LOAD REAL DATA
        eegData = loadEEGFromResourceCSV("eeg_data.csv");

        final LinearLayout parent = (LinearLayout) bmpView.getParent();
        parent.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                parent.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int padding = 20*2;
                MAX_CHART_HEIGHT = parent.getHeight()-padding - 200;
                MAX_CHART_WIDTH = parent.getWidth()-padding - 150;//height is ready

                Bitmap spectogram = Utilities.getSpectrogramBitmap(eegData[CHANNEL], FS, WIDTH, OVERLAP,
                        WINDOW, true, MAXHERTZ, true);

                int width = spectogram.getWidth();
                int height = spectogram.getHeight();
                float wScale = (float) MAX_CHART_WIDTH/width;
                float hScale = (float) MAX_CHART_HEIGHT/2/height;
                hScale = Math.min(wScale,hScale);
                spectogram = Bitmap.createScaledBitmap(spectogram, (int) (wScale*width), (int) (hScale*height), false);

                // get real spectogram
                Bitmap chart = Utilities.addAxisAndLabels(spectogram, MAXHERTZ, eegData[CHANNEL].length/FS);

                bmpView.setImageBitmap(chart);
            }
        });

        setupOverlapSpinner();
        setupWidthSpinner();
        setupChannelSpinner();
    }

    private double[] generateDummyData(){
        int[] freqs = {10, 20, 30, 40, 50};
        int fs = 256; //225
        int secs = 100; // 5
        double[] sinesData = Utilities.genSinWaves(freqs, fs, secs);
        return sinesData;
    }

    private void generateSpectogram(){
        Bitmap spectogram = Utilities.getSpectrogramBitmap(eegData[CHANNEL], FS, WIDTH, OVERLAP,
                WINDOW, true, MAXHERTZ, true);

        int width = spectogram.getWidth();
        int height = spectogram.getHeight();
        float wScale = (float) MAX_CHART_WIDTH/width;
        float hScale = (float) MAX_CHART_HEIGHT/2/height;
        hScale = Math.min(wScale,hScale);
        spectogram = Bitmap.createScaledBitmap(spectogram, (int) (wScale*width), (int) (hScale*height), false);

        // get real spectogram
        Bitmap chart = Utilities.addAxisAndLabels(spectogram, MAXHERTZ, eegData[CHANNEL].length/FS);

        bmpView.setImageBitmap(chart);
    }

    private void setupOverlapSpinner(){
        overlapSpinner = (Spinner) findViewById(R.id.overlap_dropdown);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, overlaps);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        overlapSpinner.setAdapter(adapter);

        overlapSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                int newOVERLAP = OVERLAP;
                switch((String) parent.getItemAtPosition(position)){
                    case "1/2":
                        newOVERLAP = WIDTH/2;
                        break;
                    case "1/3":
                        newOVERLAP = WIDTH/3;
                        break;
                    case "1/4":
                        newOVERLAP = WIDTH/4;
                        break;
                    case "0":
                        newOVERLAP = 0;
                        break;
                }
                if(newOVERLAP!=OVERLAP){
                    OVERLAP = newOVERLAP;
                    generateSpectogram();
                    Log.v("overlap", (String) parent.getItemAtPosition(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });

        int spinnerPosition = adapter.getPosition("1/2");
        overlapSpinner.setSelection(spinnerPosition);
    }

    private void setupWidthSpinner(){
        widthSpinner = (Spinner) findViewById(R.id.width_dropdown);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, widths);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        widthSpinner.setAdapter(adapter);

        widthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {

                int newWIDTH = Integer.parseInt((String) parent.getItemAtPosition(position)) * FS;
                if(WIDTH!=newWIDTH){
                    double overlap = (double)OVERLAP/WIDTH;
                    WIDTH = newWIDTH;
                    OVERLAP = (int)(WIDTH*overlap);
                    generateSpectogram();
                    Log.v("width", (String) parent.getItemAtPosition(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });

        int spinnerPosition = adapter.getPosition("2");
        widthSpinner.setSelection(spinnerPosition);
    }

    private void setupChannelSpinner(){
        channelSpinner = (Spinner) findViewById(R.id.channel_dropdown);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, channels);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        channelSpinner.setAdapter(adapter);

        channelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                int newChannel = Integer.parseInt((String) parent.getItemAtPosition(position)) - 1;
                if(CHANNEL!=newChannel){
                    CHANNEL = newChannel;
                    generateSpectogram();
                    Log.v("channel", (String) parent.getItemAtPosition(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });

        int spinnerPosition = adapter.getPosition("1");
        channelSpinner.setSelection(spinnerPosition);
    }

    public double[][] loadEEGFromResourceCSV(String filename){
        double[][] eegData = new double[0][0];
        try{
            InputStreamReader is = new InputStreamReader(getAssets()
                    .open(filename));
            File csvfile = new File(Environment.getExternalStorageDirectory() + "/csvfile.csv");
            CSVReader reader = new CSVReader(is);
            String [] nextLine;
            nextLine = reader.readNext();
            int numChannels = Integer.parseInt(nextLine[0]);
            int numData = Integer.parseInt(nextLine[1]);
            eegData = new double[numChannels][numData];
            int i=0;
            while ((nextLine = reader.readNext()) != null) {
                // nextLine[] is an array of values from the line
                for (int j=0; j<nextLine.length; j++){
                    eegData[j][i] = Double.parseDouble(nextLine[j]);
                };
                i+=1;
            }
        }catch(Exception e){
            e.printStackTrace();
            Toast.makeText(this, "The specified file was not found", Toast.LENGTH_SHORT).show();
        }
        return eegData;
    }
}
