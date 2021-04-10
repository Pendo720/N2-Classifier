package com.njm.nnc.models;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.njm.nnc.N2Utils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static java.lang.Math.tanh;

public class N2Neuron {

    private final List<N2Connection> mWeights;
    final int mLayer;
    final int mId;
    private Double mActivation, mGradient;
    static double mEta = 0.345, mAlpha = 0.5;

    N2Neuron(int layer, int id){
        mLayer = layer;
        mId = id;
        mGradient = 0.;
        mWeights = new ArrayList<>();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    N2Neuron(int outputs, int layer, int id){
        mId = id;
        mLayer = layer;
        mWeights = new ArrayList<>();
        mGradient = 0.;
        IntStream.range(0, outputs)
                .forEach(i->mWeights.add(new N2Connection(N2Utils.GetRandom(),0.)));
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    void feedForward(final N2Layer prev){
        AtomicReference<Double> acc = new AtomicReference<>(0.0);
        IntStream.range(0, prev.size())
                .forEach(i-> acc.updateAndGet(v -> v + prev.get(i).getOutput() * prev.get(i).mWeights.get(mId).mWeight));
        mActivation = activationFunction(acc.get());
    }

    void setActivationValue(double val){ mActivation = val; }

    double getOutput() { return mActivation; }

    double activationFunction(double x){ return tanh(x); }

    double activationDerivativeFunction(double x){ return (1.0 - x * x); }

    void calculateOutputGradient(double val){ mGradient = ((val - mActivation) * activationDerivativeFunction(mActivation)); }

    @RequiresApi(api = Build.VERSION_CODES.N)
    double sumDOW(final N2Layer next){
        final double[] acc = {0.0};
        IntStream.range(0, next.size()-1).forEach(i-> acc[0] += mWeights.get(i).mWeight * next.get(i).mGradient);
        return acc[0];
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    void updateInputWeights(N2Layer previous){

        IntStream.range(0, previous.size())
            .forEach(i->{
                N2Neuron cur = previous.get(i);
                double oldDel = cur.mWeights.get(mId).mDeltaWeight;
                double newDel = mEta * cur.getOutput()*mGradient +mAlpha * oldDel;
                cur.mWeights.get(mId).mDeltaWeight = newDel;
                cur.mWeights.get(mId).mWeight += newDel;
            });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    void calculateHiddenGradients(N2Layer next){ mGradient = sumDOW(next) * activationDerivativeFunction(mActivation); }

    public List<N2Connection> getWeights() { return mWeights; }

    public void setGradient(Double gradient) { mGradient = gradient; }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @NotNull
    @Override
    public String toString(){
        StringBuilder toReturn = new StringBuilder("\n");
        toReturn.append(String.format(Locale.UK, "%d | %d | % 12.11f | % 12.11f | %d |", mLayer, mId, mActivation, mGradient, mWeights.size()));
        mWeights.forEach(c->toReturn.append(c.export()));
        return toReturn.toString();
    }
}
