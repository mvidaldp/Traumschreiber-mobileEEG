package com.example.kai.eegtimefrequencyanalysis.utilities;

import org.apache.commons.math3.complex.Complex;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.fftw3;

import static org.bytedeco.javacpp.fftw3.FFTW_ESTIMATE;
import static org.bytedeco.javacpp.fftw3.FFTW_FORWARD;
import static org.bytedeco.javacpp.fftw3.fftw_destroy_plan;
import static org.bytedeco.javacpp.fftw3.fftw_execute;
import static org.bytedeco.javacpp.fftw3.fftw_plan;
import static org.bytedeco.javacpp.fftw3.fftw_plan_dft_1d;

public class FFTWWrapper {
    public static int NUM_POINTS = 1024;
    private static final int REAL = 0;
    private static final int IMAG = 1;
    private static boolean FFTW_LOADED=false;

    public static Complex[] fftw(double[] x){
        if(!(FFTW_LOADED)){
            Loader.load(fftw3.class);
            FFTW_LOADED = true;
        }
        int n = x.length;
        if(n!=NUM_POINTS){
            NUM_POINTS = n;
        }

        DoublePointer signal = new DoublePointer(2 * NUM_POINTS);
        DoublePointer result = new DoublePointer(2 * NUM_POINTS);

        fftw_plan plan = fftw_plan_dft_1d(NUM_POINTS, signal, result,
                FFTW_FORWARD, (int)FFTW_ESTIMATE);

        updateSignal(x, signal);
        fftw_execute(plan);
        fftw_destroy_plan(plan);

        return convertResult(result);
    }

    private static void updateSignal(double[] x, DoublePointer signal) {
        /* Generate two sine waves of different frequencies and amplitudes. */

        double[] s = new double[(int)signal.capacity()];
        for (int i = 0; i < NUM_POINTS; i++) {
            s[2 * i + REAL] = x[i];
            s[2 * i + IMAG] = 0;
        }
        signal.put(s);
    }

    private static Complex [] convertResult(DoublePointer result) {
        double[] r = new double[(int)result.capacity()];
        result.get(r);
        Complex[] res = new Complex[NUM_POINTS];
        for (int i = 0; i < NUM_POINTS; i++) {
            res[i] = new Complex(r[2 * i + REAL], r[2 * i + IMAG]);
        }
        return res;
    }
}
