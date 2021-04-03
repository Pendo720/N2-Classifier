package com.njm.nnc.ui;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.njm.nnc.models.N2RobotModel;

public class N2RobotModelView extends ViewModel {
    private final MutableLiveData<N2RobotModel> mBotState;
    public N2RobotModelView() {
        mBotState = new MutableLiveData<>();
    }
    public MutableLiveData<N2RobotModel> getModel() {
        return mBotState;
    }
}
