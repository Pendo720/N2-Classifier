package com.njm.nnc.models;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.njm.nnc.N2Utils;
import com.njm.nnc.N2State;
import com.njm.nnc.R;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.njm.nnc.MainActivity.TRAINING_COMPLETE;
import static com.njm.nnc.MainActivity.TRAINING_STATUS;
import static java.util.stream.Collectors.toList;

public class N2Trainer <T> {
    private final N2Model mNetwork;
    private FileOutputStream mLogHandle;
    private final boolean mLoggingOn;
    private N2Data<T> mDataFactory;
    private Double mThreshold;
    private final Context mContext;
    private N2State mState;
    private Long mIterationCount;

    @RequiresApi(api = Build.VERSION_CODES.N)
    public N2Trainer(Context context, String filePath, String fileName, boolean loggingOn) {
        this.mContext = context;
        this.mNetwork = new N2Model(filePath, fileName);
        this.mLoggingOn = loggingOn;
        this.mIterationCount = 1L;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public N2Trainer(Context context, N2Model _network, List<T> items, Double threshold, final boolean logTraining) {
        this.mContext = context;
        this.mNetwork = _network;
        this.mLoggingOn = logTraining;
        this.mThreshold = threshold;
        int mInputs = this.mNetwork.getTopology().get(0);
        mDataFactory = new N2Data<>(mInputs, items);
        mState = N2State.None;
        this.mIterationCount = 1L;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void train() {

        mState = N2State.Training;
        if (mLoggingOn) {
            try {
                mLogHandle = new FileOutputStream(mNetwork.mPath + "/training.log");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        List<Double> results = new ArrayList<>();
        final boolean[] modelOptimised = {false};
        mDataFactory.duplicate(mDataFactory.train(), 1000)
            .forEach(i-> {
                if(!modelOptimised[0]) {
                    T code = (T) mDataFactory.nextCodedFeature();
                    List<Double> inputs = mDataFactory.getValues(), targets;
                    StringBuilder sLine = new StringBuilder();
                    targets = mDataFactory.getLabel((Integer) code);
                    mNetwork.feedForward(inputs);
                    mNetwork.getResults(results);
                    mNetwork.backPropagate(targets);
                    sLine.append("(")
                            .append(N2Utils.stringifyList(inputs))
                            .append(" ) => [")
                            .append(N2Utils.stringifyList(results)).append("] | [")
                            .append(String.format(Locale.UK, "%6.5f,% 6.5f]", mNetwork.getError(), mNetwork.getAverageError()));
                    logLine(sLine.toString());
                    this.mIterationCount += 1;
                    mContext.sendBroadcast(new Intent(TRAINING_STATUS));
                    if (this.mIterationCount % 10L == 0L) {
                        if (crossValidate()) {
                            modelOptimised[0] = tests();
                            if (modelOptimised[0]) {
                                Log.d("Trainer", "train: Broken out of the iterative looping after " + mIterationCount + " iterations");
                                Log.d("Trainer", "train: Average learning error: " + String.format(Locale.UK, "%.06f", mNetwork.getAverageError()));
                                endTraining();
                            }
                        }
                    }
                }
            });

        endTraining();
    }

    private void endTraining(){
        if(mLoggingOn) {
            try {
                mLogHandle.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mContext.sendBroadcast(new Intent(TRAINING_COMPLETE));
    }

    private void logLine(String sLine) {
        if(mLoggingOn){
            sLine += "\n";
            try {
                mLogHandle.write(sLine.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public boolean tests() {
        List<String> result = new ArrayList<>();
        final boolean[] passed = {true};
        mDataFactory.test().forEach(i->{
            List<Double> current = test((Integer) i);
            List<Double> exploded = N2Utils.int2BitDoubles((Integer)i, current.size());
            passed[0] &= current.toString().equals(exploded.toString());
            result.add(passed[0]?"Pass": "Fail");
        });

        Log.d("Trainer", "tests : " + (passed[0]?" PASSED ":" FAILED ") + result);
        return passed[0];
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public List<Double> test(int x) { return mNetwork.check(x).stream().map(d->(d >=mThreshold)?1.0:0.0).collect(toList()); }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public boolean crossValidate() {
        List<String> result = new ArrayList<>();
        final boolean[] passed = {true};
        mDataFactory.crossValidation().forEach(i->{
            List<Double> current = test((Integer) i);
            List<Double> exploded = N2Utils.int2BitDoubles((Integer)i, current.size());
            passed[0] &= current.toString().equals(exploded.toString());
            result.add(passed[0]?"Pass": "Fail");
        });

        Log.d("Trainer", "crossValidate : Iteration:" + mIterationCount.toString() + (passed[0]?" - PASSED ":" - FAILED ") + result);
        return passed[0];
    }

    public void exportModel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mNetwork.exportNetwork(mNetwork.mPath + "/" + savedModelFilename(mNetwork.getTopology(), mContext.getString(R.string.n2_model)));
        }
    }

    public List<Double> convergenceErrors(){ return Arrays.asList(mNetwork.getError(), mNetwork.getAverageError()); }

    public void setState(N2State state) { mState = state; }

    public N2State getState() { return mState; }

    public Long getIterationCount() { return mIterationCount; }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static String savedModelFilename(List<Integer> topology, String format){
        final String[] sModel = {format};
        topology.forEach(c-> sModel[0] +=String.format(Locale.UK,"_%02d", c));
        sModel[0] += ".txt";
        return sModel[0];
    }
}
