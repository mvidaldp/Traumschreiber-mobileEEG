package de.uos.ikw.traumschreiber;

import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.DecimalFormat;

public class MyXAxisValueFormatter extends ValueFormatter {
    private DecimalFormat mFormat;

    public MyXAxisValueFormatter() {
        // format values to 1 decimal digit
        mFormat = new DecimalFormat("###,###,##0.0");
    }

    @Override
    public String getFormattedValue(float value) {
        // "value" represents the position of the label on the axis (x or y)
        return mFormat.format(value / 1000);  //
    }

//    /** this is only needed if numbers are returned, else return 0 */
//    @Override
//    public int getDecimalDigits() { return 1; }
}