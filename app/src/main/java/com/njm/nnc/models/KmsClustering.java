package com.njm.nnc.models;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class KmsClustering<T extends Number> {
    private final List<T> mData;
    private final List<Cluster<T>> mClusters;
    private List<Double> mCentroids;

    @RequiresApi(api = Build.VERSION_CODES.N)
    public KmsClustering(List<T> data, List<T> centroids) {
        mData = data;
        mClusters = new ArrayList<>();
        centroids.forEach(c->mClusters.add(new Cluster<>(c)));
        initCentroids();
        iterations();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void initCentroids() {
        mCentroids = new ArrayList<>();
        mClusters.forEach(c->Log.d(getClass().getSimpleName(), "initCentroids: " + c.getCentroid()));
        mData.forEach(i->mClusters.stream().reduce((p, q)->p.distanceTo(i) < q.distanceTo(i)?p:q).ifPresent(c->c.add(i)));
        mClusters.forEach(c->mCentroids.add(c.getCentroid().doubleValue()));
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void iterations() {
        Log.d(getClass().getSimpleName(), "initCentroids: " + toString());
        boolean[] repeat = {true};
        List<Double> newCentroids = new ArrayList<>();
        mClusters.forEach(c->newCentroids.add(c.getElements().stream().collect(Collectors.averagingDouble(Number::doubleValue))));
        repeat[0] = !newCentroids.containsAll(mCentroids);
        if(repeat[0]){
            mCentroids = newCentroids;
            IntStream.range(0, newCentroids.size()).forEach(c->mClusters.get(c).setCentroid((T) newCentroids.get(c)));
            mData.forEach(i->mClusters.stream().reduce((p, q)->p.distanceTo(i) < q.distanceTo(i)?p:q).ifPresent(c->c.add(i)));
            iterations();
        }
    }

    @NotNull
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public String toString() {
        final String[] toReturn = {""};
        mClusters.forEach(c->toReturn[0] += c.toString());
        return toReturn[0];
    }
}
