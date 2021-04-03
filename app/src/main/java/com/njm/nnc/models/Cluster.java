package com.njm.nnc.models;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Cluster<T extends Number> {
    private T mCentroid;
    private final List<T> mElements;

    public Cluster(T c) {
        mCentroid = c;
        mElements = new ArrayList<>();
    }

    public T getCentroid() { return mCentroid; }

    public void setCentroid(T centroid) {
        this.mCentroid = centroid;
        this.mElements.clear();
    }

    public List<T> getElements() { return mElements; }

    public void add(T t){ mElements.add(t); }

    public Double distanceTo(T i) { return Math.abs(this.mCentroid.doubleValue() - i.doubleValue()); }

    @NotNull
    @Override
    public String toString() { return "\n" + mCentroid + "@: " + mElements.size() + mElements; }
}
