package com.njm.nnc.models;

import android.os.Build;
import androidx.annotation.RequiresApi;
import java.util.ArrayList;
import java.util.List;

public class N2Layer {
    private final int mLayerId;
    private final List<N2Neuron> mNetNeurons;
    static final int WEIGHTS_ARE_FROM = 5;
    public N2Layer(int id) {
        mLayerId = id;
        mNetNeurons = new ArrayList<>();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public String export() {
        final String[] toReturn = {""};
        mNetNeurons.forEach(n-> toReturn[0] += n.toString());
        return toReturn[0];
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public N2Layer load(List<Double> contents) {
        N2Neuron neuron = new N2Neuron(mLayerId, contents.get(1).intValue());
        neuron.setActivationValue(contents.get(2));
        neuron.setGradient(contents.get(3));
        int connections = contents.get(4).intValue();
        for (int i = 0; i < connections*2; ) {
            N2Connection con = new N2Connection(contents.get(WEIGHTS_ARE_FROM+i), contents.get(WEIGHTS_ARE_FROM + 1 + i));
            neuron.getWeights().add(con);
            i+=2;
        }
        mNetNeurons.add(neuron);
        return this;
    }

    public N2Neuron get(int index){
        return mNetNeurons.get(index);
    }

    public void add(N2Neuron n){
        mNetNeurons.add(n);
    }

    public int size() { return mNetNeurons.size(); }

    public int getLayerId() { return mLayerId; }
}
