package com.njm.nnc.models;
import android.os.Build;

import androidx.annotation.RequiresApi;
import com.njm.nnc.N2Utils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class N2Data<T> {
    private final List<T> mSpace;
    private List<Double> mInputValues;
    private final List<List<Double>> mLookup;
    private List<T> mCrossValidation, mTrain, mTest;
    final int mBitCount;

    @RequiresApi(api = Build.VERSION_CODES.N)
    public N2Data(int bitCount, List<T> dataSamples) {
        this.mBitCount = bitCount;
        this.mSpace = new ArrayList<>();
        IntStream.range(0, (int)Math.pow(2, mBitCount)).forEach(x-> mSpace.add((T) Integer.valueOf(x)));
        mLookup = new ArrayList<>();
        mSpace.forEach(i->mLookup.add(N2Utils.int2BitDoubles((Integer) i, mBitCount)));
        splitData(dataSamples);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public T nextCodedFeature() {
        Collections.shuffle(mSpace);
        T toReturn = mSpace.get(0);
        mInputValues = mLookup.get((Integer) toReturn);
        return toReturn;
    }

    public List<Double> getValues() { return mInputValues; }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public List<Double> getLabel(int code) {
        mInputValues = mLookup.get(code%mLookup.size());
        return mInputValues;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void splitData(List<T> allData) {
        Collections.shuffle(allData);
        mTrain = allData.stream().limit((long) (0.6*allData.size())).collect(Collectors.toList());
        long whereFrom = mTrain.size();
        mTest = allData.stream().skip(whereFrom).limit((long) (0.2*allData.size())).collect(Collectors.toList());
        whereFrom += 0.2*allData.size();
        mCrossValidation = allData.stream().skip(whereFrom).limit((long) (0.2*allData.size())).collect(Collectors.toList());
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public List<T> duplicate(List<T> source, int howManyTimes) {
        List<T> toReturn = new ArrayList<>();
        IntStream.range(0, howManyTimes)
                .forEach(i->{
                    Collections.shuffle(source);
                    toReturn.addAll(source);
                });
        return toReturn;
    }

    public List<T> crossValidation() {
        Collections.shuffle(mCrossValidation);
        return mCrossValidation;
    }

    public List<T> train() {
        Collections.shuffle(mTrain);
        return mTrain;
    }

    public List<T> test() {
        Collections.shuffle(mTest);
        return mTest;
    }
}