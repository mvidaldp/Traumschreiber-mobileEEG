package com.example.kai.eegtimefrequencyanalysis.utilities;

import org.apache.commons.math3.complex.Complex;

public class CustomFFT {
    private static Complex[][] FFT_MAT;
    private static Complex[][] DFT_MAT;
    private static Complex[] FFT_FACTORS;
    private static final int FFT_THRES= 32;
    public static int FFT_SIZE = 1024;

    private static void initFFTMat(){
        FFT_MAT = new Complex[FFT_THRES][FFT_THRES];
        double img_exp_val;
        double real;
        double img;
        for (int i=0; i<FFT_THRES;i++) {
            for (int j = 0; j < FFT_THRES; j++) {
                img_exp_val = -2 * Math.PI * i * j / FFT_THRES;
                real = Math.cos(img_exp_val);
                img = Math.sin(img_exp_val);
                FFT_MAT[i][j] = new Complex(real, img);
            }
        }
    }

    private static void initFFTFactors(){
        FFT_FACTORS = new Complex[FFT_SIZE];
        double img_exp_val;
        double real;
        double img;
        for (int i=0; i<FFT_SIZE; i++) {
            img_exp_val = -2 * Math.PI * i / FFT_SIZE;
            real = Math.cos(img_exp_val);
            img = Math.sin(img_exp_val);
            FFT_FACTORS[i] = new Complex(real, img);
        }
    }

    private static void initDFTMat(int n){
        DFT_MAT = new Complex[n][n];
        double img_exp_val;
        double real;
        double img;
        for (int i=0; i<n;i++) {
            for (int j = 0; j < n; j++) {
                img_exp_val = -2 * Math.PI * i * j / n;
                real = Math.cos(img_exp_val);
                img = Math.sin(img_exp_val);
                DFT_MAT[i][j] = new Complex(real, img);
            }
        }
    }

    private static Complex[] fftRecursive(double []x){
        int n = x.length;
        if(n%2>0 || n<FFT_THRES) {
            return vectorizedDft(x);
        } else if (n==FFT_THRES){
            return dftThres(x);
        } else {
            int n_half = (int) (n/2);
            double[] even = new double[n_half];
            double[] odd = new double[n_half];
            for (int i=0, j=0; i<n_half; i++, j+=2){
                even[i] = x[j];
                odd[i] = x[j+1];
            }
            Complex[] fftEven = fftRecursive(even);
            Complex[] fftOdd = fftRecursive(odd);
            Complex[] fftRes = new Complex[n];
            int facSteps =  (int) (FFT_SIZE/n);
            int fs_half = (int) (FFT_SIZE/2);
            for (int i=0, fi=0; i<n_half; i++, fi+=facSteps){
                fftRes[i] = fftEven[i].add(FFT_FACTORS[fi].multiply(fftOdd[i]));
                fftRes[n_half+i] = fftEven[i].add(FFT_FACTORS[fs_half + fi].multiply(fftOdd[i]));
            }
            return fftRes;
        }
    }

    private static Complex[] dftThres(double []x){
        int n = x.length;
        assert n == FFT_THRES : "Wrong length in dftThres!";
        try {
            if(FFT_MAT.length!=n){
                initFFTMat();
            }
        } catch (NullPointerException e) {
            initFFTMat();
        }

        Complex[] x_dft = new Complex[FFT_THRES];
        Complex dft_val;
        Complex addend;

        for (int i=0; i<FFT_THRES;i++){
            dft_val = new Complex(0,0);
            for (int j=0; j<FFT_THRES;j++){
                addend = FFT_MAT[i][j];
                addend = addend.multiply(x[j]);
                dft_val = dft_val.add(addend);
            }
            x_dft[i] = dft_val;
        }
        return x_dft;
    }

    public static Complex[] vectorizedDft(double []x){
        int n = x.length;
        try {
            if(DFT_MAT.length!=n){
                initDFTMat(n);
            }
        } catch (NullPointerException e) {
            initDFTMat(n);
        }

        Complex[] x_dft = new Complex[n];
        Complex dft_val;
        Complex addend;

        for (int i=0; i<n;i++){
            dft_val = new Complex(0,0);
            for (int j=0; j<n;j++){
                addend = DFT_MAT[i][j];
                addend = addend.multiply(x[j]);
                dft_val = dft_val.add(addend);
            }
            x_dft[i] = dft_val;
        }
        return x_dft;
    }

    public static Complex[] dft(double []x){
        Complex[] x_dft = new Complex[x.length];
        Complex dft_val;
        Complex addend;
        double real;
        double img;
        double img_exp_val;
        for (int i=0; i<x.length;i++){
            dft_val = new Complex(0,0);
            for (int j=0; j<x.length;j++){
                img_exp_val = -2*Math.PI*i*j/x.length;
                real = Math.cos(img_exp_val);
                img = Math.sin(img_exp_val);
                addend = new Complex(real, img);
                addend = addend.multiply(x[j]);
                dft_val = dft_val.add(addend);
            }
            x_dft[i] = dft_val;
        }
        return x_dft;
    }

    public static Complex[] fft(double []x) {
        int n = x.length;
        if(n!=FFT_SIZE){
            FFT_SIZE = n;
            initFFTFactors();
        }
        return fftRecursive(x);
    }
}
