package com.njm.nnc.ui;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.njm.nnc.models.N2Trainer;

public class N2ModelView extends ViewModel {

    private final MutableLiveData<N2Trainer> mNetTrainer;

    public N2ModelView() {
        mNetTrainer = new MutableLiveData<>();
    }

    public MutableLiveData<N2Trainer> getModel() {
        return mNetTrainer;
    }
}