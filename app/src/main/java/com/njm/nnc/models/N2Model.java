package com.njm.nnc.models;

import android.os.Build;

import androidx.annotation.RequiresApi;
import com.njm.nnc.BuildConfig;
import com.njm.nnc.N2Utils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Math.sqrt;

public class N2Model {
    private double mAverageSmoothingFactor = 100.0;
    private List<N2Layer> mLayers;
    private double mError, mAverageError;
    private List<Integer> mTopology;
    final String mPath;

    @RequiresApi(api = Build.VERSION_CODES.N)
    public N2Model(String filePath, String fileName) {
        mPath = filePath ;
        load(mPath + "/" + fileName);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public N2Model(List<Integer> topology, String filePath) {
        mTopology = topology;
        mPath = filePath;
        mLayers = new ArrayList<>();
        IntStream.range(0, mTopology.size())
                .forEach(i->{
                    mLayers.add(new N2Layer(i));
                    int outputs = i == mTopology.size()-1?0:mTopology.get(i+1);
                    for(int n = 0; n<=mTopology.get(i); ++n){
                        mLayers.get(mLayers.size()-1).add(new N2Neuron(outputs, i,  n));
                    }

                    N2Layer netLayer =  mLayers.get(mLayers.size()-1);
                    netLayer.get(netLayer.size()-1).setActivationValue(1.0);
                });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    void feedForward(final List<Double> inputs) {

        if (BuildConfig.DEBUG && !(inputs.size() == mLayers.get(0).size() - 1)) {
            throw new AssertionError("Assertion failed");
        }
        IntStream.range(0, inputs.size())
                .forEach(i -> mLayers.get(0).get(i).setActivationValue(inputs.get(i)));
        // forward prop
        IntStream.range(1, mLayers.size())
                .forEach(i -> {
                    N2Layer prevLayer = mLayers.get(i - 1);
                    IntStream.range(0, mLayers.get(i).size() - 1)
                            .forEach(n -> mLayers.get(i).get(n).feedForward(prevLayer));
                });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    void backPropagate(final List<Double> targets) {
        // calculate net error
        N2Layer outputLayer = mLayers.get(mLayers.size()-1);
        mError = 0.0;
        IntStream.range(0, outputLayer.size()-1)
                .forEach(i-> mError += Math.pow(targets.get(i) - outputLayer.get(i).getOutput(), 2));

        mError /= outputLayer.size()-1;
        mError = sqrt(mError);
        mAverageError = ((mAverageError * mAverageSmoothingFactor + mError)/(mAverageSmoothingFactor + 1.0));

        IntStream.range(0, outputLayer.size()-1)
                .forEach(i-> outputLayer.get(i).calculateOutputGradient(targets.get(i)));

        //calculate gradients on hidden layer
        for(int lay=mLayers.size()-2; lay>0; --lay) {
            N2Layer hiddenLayer = mLayers.get(lay);
            N2Layer nextLayer = mLayers.get(lay+1);
            for(int n=0; n<hiddenLayer.size(); ++n) {
                hiddenLayer.get(n).calculateHiddenGradients(nextLayer);
            }
        }

        // for all layers from output to first hidden layer, updated connection weights
        for( int lay=mLayers.size()-1; lay>0; --lay) {
            N2Layer cur = mLayers.get(lay);
            N2Layer prev = mLayers.get(lay-1);
            for(int n=0; n<cur.size()-1; ++n) {
                cur.get(n).updateInputWeights(prev);
            }
        }
    }

    void getResults(List<Double> results) {
        results.clear();
        for(int n=0; n<mLayers.get(mLayers.size()-1).size()-1; ++n){
            results.add(mLayers.get(mLayers.size()-1).get(n).getOutput());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    void exportNetwork(final String modelFile) {

        try {
            File fw = new File(modelFile);
            FileOutputStream handle = new FileOutputStream(fw);
            if(mTopology != null) {
                final String[] sLine = {"topology: "};
                IntStream.range(0, mTopology.size())
                        .forEach(i -> sLine[0] += String.format(Locale.UK, "%d ", mTopology.get(i)));

                sLine[0] += String.format(Locale.UK, "\n%4.3f | % 4.3f | % 12.11f | %3.1f | % 12.11f", N2Neuron.mEta, N2Neuron.mAlpha, mAverageError, mAverageSmoothingFactor, mError);
                IntStream.range(0, mLayers.size())
                        .forEach(i -> sLine[0] += mLayers.get(i).export());
                handle.write(sLine[0].getBytes());
            }
            handle.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void load(final String modelFile) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(modelFile)));
            String sLine = reader.readLine();
            if(sLine != null && sLine.contains("topology:")) {
                sLine = sLine.substring(sLine.indexOf(':') + 1);
                mTopology = Stream.of(sLine.split(" "))
                        .filter(s->!s.isEmpty())
                        .map(Integer::valueOf).collect(Collectors.toList());

                mLayers = new ArrayList<>();
                sLine = reader.readLine();
                List<Double> parameters = Stream.of(sLine.split("\\s\\|"))
                                            .filter(s->!s.isEmpty() && !s.contains("|"))
                                            .map(Double::valueOf).collect(Collectors.toList());

                N2Neuron.mEta = parameters.get(0);
                N2Neuron.mAlpha = parameters.get(1);
                mAverageError = parameters.get(2);
                mAverageSmoothingFactor = parameters.get(3);
                mError = parameters.get(4);

                reader.lines()
                    .forEach(l -> {
                        if(!l.isEmpty()) {
                            List<Double> params =
                            Stream.of(l.split("\\s"))
                                    .filter(s -> !s.isEmpty() && !s.contains("|"))
                                    .map(Double::valueOf).collect(Collectors.toList());

                            int layer = params.get(0).intValue();
                            N2Layer current  = this.mLayers.stream().filter(i->i.getLayerId() == layer).findAny().orElse(null);
                            if(current != null){
                                this.mLayers.get(layer).load(params);
                            }else {
                                N2Layer nl = new N2Layer(layer);
                                this.mLayers.add(nl.load(params));
                            }
                        }
                    });
            }
            reader.close();
//            Uncomment the following line verify correct loading by capturing the network state
//            exportNetwork(mPath + "/new_snapshot.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    public List<Double> check(Integer input) {
        List<Double> results = new ArrayList<>();
        feedForward(N2Utils.int2BitDoubles(input, mTopology.get(0)));
        getResults(results);
        return results;
    }

    public List<Integer> getTopology() { return mTopology; }

    public Double getError() { return mError; }

    public Double getAverageError() { return mAverageError; }

}
