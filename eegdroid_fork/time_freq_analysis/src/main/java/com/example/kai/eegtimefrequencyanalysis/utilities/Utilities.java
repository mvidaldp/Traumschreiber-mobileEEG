package com.example.kai.eegtimefrequencyanalysis.utilities;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Environment;
import android.widget.Toast;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;
import com.opencsv.CSVReader;

import org.apache.commons.math3.complex.Complex;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class Utilities {

    public static Bitmap getSpectrogramBitmap(double []eegData, int fs, int width, int overlap,
                                              String window, boolean mean, double maxHertz,
                                              boolean fftw){
        int n = eegData.length;
        int windowStepSize = width-overlap;
        int windowSteps = (int)((n-width)/(windowStepSize));
        int frCount = (int) Math.ceil((double) width/2);
        double hertzPerVal = (double)fs/width;
        double yLim = (frCount-1) * hertzPerVal; // hertz
        if(maxHertz < yLim){
            frCount = (int) Math.ceil(maxHertz/hertzPerVal);
        }
        int minFr = (int) Math.ceil(1/hertzPerVal);
        double xLim = n/fs; //seconds
        double [][] spectogramData = new double [frCount][windowSteps+1];
        double [] fftData = new double[width];
        Complex [] fftRes = new Complex[width];

        for (int count=0, offset=0; count<=windowSteps; count++, offset+=windowStepSize) {
            for (int j=0; j<width; j++) {
                fftData[j] = eegData[offset + j];
            }
            switch(window){
                case "hanning":
                    applyHanningWindow(fftData);
                    break;
                case "hamming":
                    applyHammingWindow(fftData);
                    break;
                default:
                    break;
            }
            if(mean) subtractMean(fftData);
            if(fftw){
                fftRes = FFTWWrapper.fftw(fftData);
            } else {
                fftRes = CustomFFT.fft(fftData);
            }
            for (int i=0; i<width; i++){
                fftData[i] = fftRes[i].abs();
            }
            for (int i=minFr; i<frCount; i++){
                spectogramData[frCount-1-i][count] = fftData[i];
            }
        }
        return spectogramBitMap(spectogramData);
    }

    public static Bitmap getSpectrogramBitmap(double []eegData, int fs, int width, int overlap,
                                              String window, boolean mean, int maxHertz){
        return getSpectrogramBitmap(eegData, fs, width, overlap, window, mean, maxHertz, true);
    }

    public static Bitmap getSpectrogramBitmap(double []eegData, int fs, int width, int overlap,
                                              String window, boolean mean, boolean fftw){
        return getSpectrogramBitmap(eegData, fs, width, overlap, window, mean, 60, fftw);
    }

    public static Bitmap getSpectrogramBitmap(double []eegData, int fs, int width, int overlap,
                                              String window, boolean mean){
        return getSpectrogramBitmap(eegData, fs, width, overlap, window, mean, 60, true);
    }

    public static void applyHanningWindow(double[] x){
        int n = x.length-1;
        for (int i=0; i<x.length; i++){
            x[i] *= (0.5-0.5*Math.cos(2*i*Math.PI/n));
        }
    }

    public static void applyHammingWindow(double[] x){
        int n = x.length-1;
        double meanX = mean(x);
        for (int i=0; i<x.length; i++){
            x[i] *= (0.54-0.46*Math.cos(2*i*Math.PI/n));
        }
    }

    public static double mean(double[] x){
        double mean = 0;
        for (double i_x: x){
            mean += i_x;
        }
        mean /= x.length;
        return mean;
    }

    public static double max(double[][] x){
        double max = 0;
        for (double[] row: x){
            for (double ij_x: row){
                if(ij_x>max) max = ij_x;
            }
        }
        return max;
    }

    public static double min(double[][] x){
        double min = Double.POSITIVE_INFINITY;
        for (double[] row: x){
            for (double ij_x: row){
                if(ij_x<min) min = ij_x;
            }
        }
        return min;
    }

    public static void subtractMean(double[] x){
        double meanX = mean(x);
        for (int i=0; i<x.length; i++){
            x[i] -= meanX;
        }
    }

    public static Bitmap spectogramBitMap( double [][] spectogram){
        // You are using RGBA that's why Config is ARGB.8888
        int w = spectogram[0].length;
        int h = spectogram.length;
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        float[] hsv_color = {0,1,1};
        int[] vector = new int[w*h];
        double min = min(spectogram);
        double max = (max(spectogram)-min);
        double scaledSpec;
        for (int y=0; y<h; y++){
            for (int x=0; x<w; x++){
                scaledSpec = ((spectogram[y][x]-min)/max);
                hsv_color[0] = (float) (scaledSpec * 250);
                hsv_color[2] = (float) -Math.pow(1.6*(scaledSpec-0.5), 4)+1;
                vector[y*w+x] = Color.HSVToColor(hsv_color);
            }
        }
        // vector is your int[] of ARGB
        bitmap.copyPixelsFromBuffer(IntBuffer.wrap(vector));
        return bitmap;
    }

    public static double[] genSinWaves(int[]freqs, int fs, int sec){
        int N = sec*fs;
        double[] vals = new double[N];
        float y;
        for (int i=0; i<N; i++) {
            y = 0;
            for (int freq: freqs){
                y += (float) Math.sin(2*Math.PI*freq*i/fs);
            }
            vals[i] = y;
        }
        return vals;
    }

    public static LineDataSet genLineDataSet(double[] y){
        List<Entry> entries = new ArrayList<>();
        for (int i=0; i<y.length; i++) {
            entries.add(new Entry(i, (float) y[i]));
        }
        LineDataSet dataSet = new LineDataSet(entries, "Label"); // add entries to dataset
        dataSet.setDrawCircles(false);
        return dataSet;
    }

    public static LineDataSet genLineDataSet(double[] x, double[] y){
        List<Entry> entries = new ArrayList<>();
        for (int i=0; i<y.length; i++) {
            entries.add(new Entry((float) x[i], (float) y[i]));
        }
        LineDataSet dataSet = new LineDataSet(entries, "Label"); // add entries to dataset
        dataSet.setDrawCircles(false);
        return dataSet;
    }

    public static void fftShift(double []x){
        int step = x.length / 2;
        double rem;
        if(x.length%2==1) {
            int shift = (int) Math.ceil((double) x.length / 2);
            double set = x[shift];
            int idx = 0;
            for (int i = 0; i < x.length; i++) {
                rem = x[idx];
                x[idx] = set;
                idx = (idx + step) % x.length;
                set = rem;
            }
        } else {
            for (int idx=0, swapIdx; idx<step; idx++) {
                rem = x[idx];
                swapIdx = (idx+step)%x.length;
                x[idx] = x[swapIdx];
                x[swapIdx] = rem;
            }
        }
    }

    public static void fftShift(Complex []x){
        int step = x.length / 2;
        Complex rem;
        if(x.length%2==1) {
            int shift = (int) Math.ceil((double) x.length / 2);
            Complex set = x[shift];
            int idx = 0;
            for (int i = 0; i < x.length; i++) {
                rem = x[idx];
                x[idx] = set;
                idx = (idx + step) % x.length;
                set = rem;
            }
        } else {
            for (int idx=0, swapIdx; idx<step; idx++) {
                rem = x[idx];
                swapIdx = (idx+step)%x.length;
                x[idx] = x[swapIdx];
                x[swapIdx] = rem;
            }
        }
    }

    public static void testFFTs(){
        int[] freqs = {10};
        double[] sines = genSinWaves(freqs, 254, 1);
        Complex[] customFFTRes= CustomFFT.fft(sines);
        Complex[] FFTWRes= FFTWWrapper.fftw(sines);
        // compute magnitude
        for (int i = 0; i < 45; i++) {
            System.out.println("CUSTOM " + i + ":" + String.format("%4.3f" , customFFTRes[i].abs()));
            System.out.println("FFTW " + i + ":" + String.format("%4.3f" , FFTWRes[i].abs()));
        }
    }

    public static int[][] getTicks(int numTicks, int maxVal, int axisLength, int labelLength, int minSpace){
        int tickDif = maxVal/numTicks;
        tickDif -= tickDif%5;
        numTicks = maxVal/tickDif;
        numTicks = Math.min(numTicks, axisLength/(labelLength+minSpace));
        tickDif = maxVal/numTicks;
        tickDif -= tickDif%5;
        int [][] ticks = new int[numTicks+1][2];
        ticks[0][0] = 0;
        ticks[0][1] = 0;
        float pxPerVal = (float)axisLength/maxVal;
        for (int i=1; i<numTicks+1; i++){
            ticks[i][0] = (int)(pxPerVal * tickDif * i);;
            ticks[i][1] = i*tickDif;
        }
        return ticks;
    }

    public static Bitmap addAxisAndLabels(Bitmap bitmap, int maxY, int maxX) {

        final Paint paint = new Paint();
        paint.setStrokeWidth(5);
        int strokeWidth = 3;
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setTextSize(46);
        int [] sizes = getCurTextLengthInPixels(paint, Integer.toString(1));
        int labelHeight = sizes[1];
        paint.setTextSize(40);
        int absMax = Math.max(maxY,maxX);

        sizes = getCurTextLengthInPixels(paint, Integer.toString(absMax));
        int textLength = sizes[0];
        int textHeight = sizes[1];
        int maxLetters = Integer.toString(absMax).length();
        int letterLength = textLength/maxLetters;
        int space = 10;
        int tickSize = 20;

        int labelSizeLeft = strokeWidth+2*space+textLength+tickSize;
        int labelSizeTop = 2*space+labelHeight+textHeight;
        int labelSizeBottom = strokeWidth+tickSize+3*space+ textHeight + labelHeight;
        int labelSizeRight = textLength/2 + space;
        int bottom = labelSizeTop+bitmap.getHeight()+labelSizeBottom;
        int right = bitmap.getWidth()+labelSizeLeft;
        int heightOfX = bottom-labelSizeBottom;
        int widthOfY = labelSizeLeft-strokeWidth;


        Bitmap output = Bitmap.createBitmap(bitmap.getWidth()+labelSizeLeft + labelSizeRight,
                bitmap.getHeight()+labelSizeBottom+labelSizeTop, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        int desTicks = 5;
        int[][] yTicks = getTicks(desTicks, maxY, bitmap.getHeight(), textHeight, space);
        int[][] xTicks = getTicks(desTicks, maxX, bitmap.getWidth(), textLength, space);

        String yLabel = "Frequency in Hz";
        String xLabel = "Time in sec";

        // DRAW Y AXIS
        canvas.drawLine(widthOfY, labelSizeTop, widthOfY, bottom-labelSizeBottom+strokeWidth, paint);
        // DRAW X AXIS
        canvas.drawLine(widthOfY, heightOfX, right, heightOfX, paint);
        String tickLabel;
        for(int i=0; i < yTicks.length ; i++) {
            canvas.drawLine(widthOfY-tickSize, heightOfX-yTicks[i][0],widthOfY, heightOfX-yTicks[i][0], paint);
            tickLabel = Integer.toString(yTicks[i][1]);
            canvas.drawText(tickLabel, space+(maxLetters-tickLabel.length())*letterLength, heightOfX-yTicks[i][0]+textHeight/2, paint);
        }

        for(int i=0; i < xTicks.length ; i++) {
            canvas.drawLine(widthOfY+xTicks[i][0], heightOfX, widthOfY+xTicks[i][0], heightOfX+tickSize, paint);
            tickLabel = Integer.toString(xTicks[i][1]);
            canvas.drawText(tickLabel, widthOfY+xTicks[i][0]-tickLabel.length()*letterLength/2, heightOfX+tickSize+space+textHeight, paint);
        }
        paint.setTextSize(46);
        canvas.drawText(yLabel, 0, labelHeight + space, paint);
        canvas.drawText(xLabel, widthOfY+bitmap.getWidth()/2-yLabel.length()*letterLength/2, bottom-space, paint);

        //plot_array_list(canvas , data_2_plot , labels , "the title" , 0 );
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        final Rect rect2 = new Rect(widthOfY+strokeWidth, labelSizeTop+strokeWidth , output.getWidth()-labelSizeRight, heightOfX-strokeWidth);
        //canvas.drawRect( rect2, paint);
        canvas.drawBitmap(bitmap, null, rect2, paint);

        return output;
    }

    // need the width of the labels
    private static int[] getCurTextLengthInPixels(Paint this_paint, String this_text) {
        Paint.FontMetrics tp = this_paint.getFontMetrics();
        Rect rect = new Rect();
        this_paint.getTextBounds(this_text, 0, this_text.length(), rect);
        int [] sizes = {rect.width(), rect.height()};
        return sizes;
    }
}
