package com.njm.nnc.models;

import java.util.List;

public class N2RobotModel {

    private final List<Double> _data;

    public N2RobotModel(List<Double> data) { _data = data; }

    public List<Double> getData() {
        return _data;
    }
}
