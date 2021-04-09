package com.njm.nnc;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.Arrays;
import java.util.Optional;

public enum N2State {
    None("None"), Training("Training"), Validating("Validating"), Operating("Online");
    final String mLabel;
    N2State(String sLabel){
        mLabel = sLabel;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static N2State get(int ordinal) {
        Optional<N2State> value = Arrays.stream(values()).filter(v->v.ordinal() == ordinal).findAny();
        return value.orElse(None);
    }

    public String getLabel() { return mLabel; }
}
