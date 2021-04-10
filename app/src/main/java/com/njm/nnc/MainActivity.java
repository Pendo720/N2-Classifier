package com.njm.nnc;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.njm.nnc.models.KmsClustering;
import com.njm.nnc.models.N2Model;
import com.njm.nnc.models.N2RobotModel;
import com.njm.nnc.models.N2Trainer;
import com.njm.nnc.ui.N2ModelView;
import com.njm.nnc.ui.N2RobotDrawable;
import com.njm.nnc.ui.N2RobotModelView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.stream.IntStream;

public class MainActivity extends AppCompatActivity {
    public final static String TRAINING_COMPLETE = "COM.NJM.NNC.TRAINING_COMPLETE";
    public final static String TRAINING_STATUS = "COM.NJM.NNC.TRAINING_STATUS";

    private SensorManager mSensorMan;
    private ActionBar mActionBar;
    private SensorEventListener mListener;

    private N2Model mN2Model;
    private N2Trainer<Integer> mTrainer;
    private N2ModelView mModelView;
    private N2RobotModelView mBotView;
    private N2State mN2State;
    final int FEATURES = 5;

    private TextView mError, mAvError;
    private LinearLayout mCtrlButtons;
    private RelativeLayout mCtrlsCV;
    private N2RobotDrawable mDrawable;
    private ProgressBar mProgress;

    final List<Integer> _topology = Arrays.asList(FEATURES, FEATURES);
    final int iterations = (int) Math.pow(2, FEATURES);

    final BroadcastReceiver mMsgReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String sAction = intent.getAction();
            switch (sAction){
                case TRAINING_STATUS:
                    mModelView.getModel().postValue(mTrainer);
                break;
                case TRAINING_COMPLETE:
                    if(mN2State != N2State.Operating) {
                        mTrainer.exportModel();
                        mModelView.getModel().postValue(mTrainer);
                        mTrainer.setState(N2State.Operating);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            mN2State = mTrainer.getState();
                            showLayout();
                        }
                    }
                    break;
            }
        }
    };

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        mActionBar = getSupportActionBar();
        mActionBar.setHomeAsUpIndicator(ContextCompat.getDrawable(this, R.mipmap.ic_launcher_round));
        mSensorMan = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            init();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void init(){

        Context mContext = MainActivity.this;
        int ordinal = getApplicationContext().getSharedPreferences(getApplication().getPackageName(), MODE_PRIVATE).getInt(N2State.class.getName(), N2State.None.ordinal());
        mN2State = N2State.get(ordinal);

        List<Double> dataPoints = new ArrayList<>();
        IntStream.range(0, 101)
                .filter(x->x<100).forEach(i->dataPoints.add(N2Utils.GetRandom()*100));

        new KmsClustering<>(dataPoints, Arrays.asList(N2Utils.GetRandom()*100, N2Utils.GetRandom()*100, N2Utils.GetRandom()*100));

        mBotView = new ViewModelProvider(this).get(N2RobotModelView.class);
        mModelView = new ViewModelProvider(this).get(N2ModelView.class);
        mN2Model = new N2Model(_topology, mContext.getFilesDir().getPath());

        mCtrlsCV = findViewById(R.id.cv_id);
        mCtrlButtons = findViewById(R.id.drivers);
        mCtrlButtons.setVisibility(View.VISIBLE);

        mError = mCtrlButtons.findViewById(R.id.txtError);
        mAvError = mCtrlButtons.findViewById(R.id.txtAvError);

        mProgress = findViewById(R.id.progressBar);
        mDrawable = new N2RobotDrawable();
        mError.setVisibility(View.VISIBLE);
        mAvError.setVisibility(View.VISIBLE);;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onResume() {
        super.onResume();
        List<Integer> data = new ArrayList<>();
        IntStream.range(0, iterations).forEach(data::add);
        String sFile = N2Trainer.savedModelFilename(_topology, getString(R.string.n2_model));
        List<String> allFiles = Arrays.asList(Objects.requireNonNull(getFilesDir().list()));

        boolean hasSavedModel = (allFiles.stream().filter(s -> s.equals(sFile)).count() >= 1);
        mN2State = !hasSavedModel ? N2State.None : mN2State;

        if( mN2State == N2State.None | mN2State == N2State.Training){
            Double mThreshold = 0.99;
            mTrainer = new N2Trainer<>(MainActivity.this, mN2Model, data, mThreshold, true);
            new Thread(() -> mTrainer.train()).start();
        }else {
            if(mN2State == N2State.Operating) {
                mTrainer = new N2Trainer<>(MainActivity.this, getFilesDir().getPath(), "/" + sFile, true);
            }
        }

        mTrainer.setState(mN2State);
        mModelView.getModel().observe(this, mTrainer -> {
            if (mTrainer.getState() == N2State.Training || mTrainer.getState() == N2State.None) {
                List<Double> errorData = mTrainer.convergenceErrors();
                if(!errorData.isEmpty()) {
                    Double error = errorData.get(0), avError = errorData.get(1);
                    mError.setText(String.format(Locale.UK, "Error: %2.2E", error));
                    mAvError.setText(String.format(Locale.UK, "Average: %2.2E", avError));
                }
                mActionBar.setSubtitle(mN2State.getLabel() + String.format(Locale.UK, "... (Iteration : %03d)",mTrainer.getIterationCount()));
            }
        });

        mModelView.getModel().postValue(mTrainer);

        N2RobotModel mN2RobotModel1 = new N2RobotModel(N2Utils.int2BitDoubles(0, _topology.get(0)));
        mBotView.getModel().observe(this, mN2RobotModel ->{
            if(mTrainer.getState() == N2State.Operating) {
                mDrawable.update(mN2RobotModel.getData());
            }
        });

        mBotView.getModel().postValue(mN2RobotModel1);

        IntentFilter mMsgFilter = new IntentFilter(TRAINING_COMPLETE);
        mMsgFilter.addAction(TRAINING_STATUS);
        registerReceiver(mMsgReceiver, mMsgFilter);

        mListener = new SensorEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onSensorChanged(SensorEvent event) {
                if(event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                    Random random = new Random();
                    if(mTrainer.getState() == N2State.Operating) {
                        int v = (int) (Math.abs(random.nextInt()) % Math.pow(2,_topology.get(0)));
                        List<Double> d = N2Utils.int2BitDoubles(v, _topology.get(0));
                        d.add((double) v);
                        mDrawable.update(d);
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        mSensorMan.registerListener(mListener, mSensorMan.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_NORMAL);
        showLayout();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mMsgReceiver);
        mSensorMan.unregisterListener(mListener);
        SharedPreferences.Editor editor = getApplicationContext().getSharedPreferences(getApplication().getPackageName(), MODE_PRIVATE).edit();

        editor.putInt(N2State.class.getName(), mN2State.ordinal());
        editor.apply();
    }

    private void trainingLayout(){
        mCtrlsCV.setVisibility(View.VISIBLE);
        mProgress.setVisibility(View.VISIBLE);
        mProgress.setIndeterminate(true);
        mError.setVisibility(View.VISIBLE);
        mAvError.setVisibility(View.VISIBLE);
        mCtrlButtons.setVisibility(View.VISIBLE);
    }

    private void deploymentLayout(){

        mCtrlsCV.setVisibility(View.VISIBLE);
        mCtrlsCV.findViewById(R.id.robot_view).setBackground(mDrawable);
        mProgress.setVisibility(View.GONE);
        mError.setVisibility(View.GONE);
        mAvError.setVisibility(View.GONE);
        mCtrlButtons.setVisibility(View.GONE);

        Toast.makeText(this, "Activate proximity sensor...", Toast.LENGTH_LONG).show();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void showLayout(){
        if(mN2State == N2State.None) { mN2State = N2State.Training; }
        mActionBar.setSubtitle(String.format(Locale.UK, "%s", mN2State.getLabel()));

        if(mN2State == N2State.Training){
            trainingLayout();
        }

        if(mN2State == N2State.Operating){
            deploymentLayout();
        }
    }
}