package com.njm.nnc.models;

import org.jetbrains.annotations.NotNull;
import java.util.Locale;

public class N2Connection {
    public double mWeight, mDeltaWeight;

    public N2Connection(double mWeight, double mDeltaWeight) {
        this.mWeight = mWeight;
        this.mDeltaWeight = mDeltaWeight;
    }

    @NotNull
    @Override
    public String toString() {
        return String.format(Locale.UK, " %12.11f | %12.11f |", mWeight, mDeltaWeight);
    }

    public String export(){
        return toString();
    }

}
