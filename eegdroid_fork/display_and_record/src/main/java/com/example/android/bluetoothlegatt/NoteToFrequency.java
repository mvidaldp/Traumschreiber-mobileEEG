package com.example.android.bluetoothlegatt;

import java.util.List;

import static java.util.Arrays.asList;


public class NoteToFrequency {

    List<Character> RIGHT_SCORES = asList('A', 'B', 'C', 'D', 'E', 'F', 'G');
    List<Character> WRONG_FLAT = asList('C', 'F');
    List<Character> WRONG_SHARP = asList('B', 'E');
    int DEFAULT_TUNING = 440;
    double DEFAULT_DURATION = 2.0;
    char note;
    char accidental;
    int octave;
    int duration;
    int tuning;
    double frequency;

    public NoteToFrequency(char note, char accidental, int octave, int tuning) {
        this.note = note;
        this.accidental = accidental;
        this.octave = octave;
        this.tuning = tuning;
        this.frequency = calculateFrequency();
    }

    public double calculateFrequency() {
        /*
         * The basic formula for the frequencies of the notes of the equal
         * tempered scale is given by fn = f0 * (a)n where f0 = the frequency
         * of one fixed note which must be defined. A common choice is setting
         * the A above middle C (A4) at f0 = 440 Hz.
         * n = the number of half steps away from the fixed note you are. If
         * you are at a higher note, n is positive. If you are on a lower
         * note, n is negative.
         * fn = the frequency of the note n half steps away.
         * a = (2)1/12 = the twelth root of 2 = the number which when
         * multiplied by itself 12 times equals 2 = 1.059463094359...
         * https://pages.mtu.edu/~suits/NoteFreqCalcs.html
         * https://pages.mtu.edu/~suits/notefreqs.html
         */
        int steps = 0;
        int diff = this.octave - 4;

        if (this.octave > 4) {
            diff = 12 * (diff - 1);
            switch (this.note) {
                case 'C':
                    steps = 3 + diff;
                    break;
                case 'D':
                    steps = 5 + diff;
                    break;
                case 'E':
                    steps = 7 + diff;
                    break;
                case 'F':
                    steps = 8 + diff;
                    break;
                case 'G':
                    steps = 10 + diff;
                    break;
                case 'A':
                    steps = 12 + diff;
                    break;
                case 'B':
                    steps = 14 + diff;
                    break;
            }

        } else {
            diff = 12 * diff;
            switch (this.note) {
                case 'C':
                    steps = -9 + diff;
                    break;
                case 'D':
                    steps = -7 + diff;
                    break;
                case 'E':
                    steps = -5 + diff;
                    break;
                case 'F':
                    steps = -4 + diff;
                    break;
                case 'G':
                    steps = -2 + diff;
                    break;
                case 'A':
                    steps = diff; // 0 + diff
                    break;
                case 'B':
                    steps = 2 + diff;
                    break;
            }
        }

        if (this.accidental == '#')
            steps += 1;
        else if (this.accidental == 'b')
            steps -= 1;

        double a = Math.pow(2 , (1 / 12.0)); // a = 1.0594630943592953
        double f = this.tuning * Math.pow(a, steps);
        double roundF = Math.round(f * 100.0) / 100.0;
        return roundF;
    }
}
