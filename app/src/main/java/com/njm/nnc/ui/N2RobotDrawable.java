package com.njm.nnc.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.graphics.ColorUtils;

import com.njm.nnc.N2Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Math.cos;
import static java.lang.Math.min;
import static java.lang.Math.sin;

public class N2RobotDrawable extends Drawable {
    private Paint mPaint, mTxtPaint;
    private final List<Double> _data;
    private boolean mIsCorrect;
    private Double mInput;
    private static final int[] LinkColours = new int[]{Color.BLACK, Color.LTGRAY, Color.GRAY,
            Color.BLUE, Color.CYAN, Color.MAGENTA,
            Color.RED, Color.GREEN};
    public N2RobotDrawable(){
        _data = new ArrayList<>();
        init();
    }

    protected void init() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaint.setColor(Color.BLUE);
        mTxtPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTxtPaint.setTextAlign(Paint.Align.CENTER);
        mTxtPaint.setTextSize(48);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void draw(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT);
        if(!_data.isEmpty()){
            drawRobot(canvas);
        }
    }

    @Override
    public void setAlpha(int i) {

    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void drawRobot(Canvas canvas){

        int w = getBounds().width(), h = getBounds().height(), cx = w/2, cy = h/2, baseWidth=30;
        int dim = min(cx, cy), margin = 20, margin_3 = 3*margin, half_margin = margin/2;
        final int linksCount = _data.size();
        final int linkThickness = 60;

        mPaint.setColor(Color.LTGRAY);
        mPaint.setStrokeWidth(25);
        canvas.drawText(String.format("%02d: %0" + linksCount +"d", mInput.intValue(), Integer.valueOf(Integer.toBinaryString(mInput.intValue()))), cx-margin_3, margin_3, mTxtPaint);
//        reference line
        mPaint.setStrokeWidth(2*margin);
        canvas.drawLine(margin, h, 2*dim-margin, h, mPaint);
        mPaint.setStrokeWidth(80);

        canvas.drawCircle(dim, h, margin_3/2, mPaint);
        final Integer[] counter = {1};
        canvas.translate(dim, h);

        if(mIsCorrect) {
            Float[] xy = {0f, 0f};
            double angle = Math.PI/(linksCount);
            final int linkLength = 2*(dim-baseWidth)/linksCount;

            final double[] ang = new double[1];
            _data.forEach( d -> {
                int c = counter[0];
                mPaint.setStrokeWidth(1.75f*linkThickness/c);
                ang[0] = angle*(d == 1.0?2.25:1);
                ang[0] *= ( c==1?c:(c-1));

                float fx = (float) (xy[0]+linkLength*cos(ang[0]));
                float fy = xy[1] - (float) (linkLength*sin(ang[0]));

                if(c == 1) {
                    canvas.drawLine(0, (1 - c) * linkLength, fx, fy, mPaint);
                }
                else{
                    mPaint.setColor(ColorUtils.blendARGB(Color.LTGRAY, LinkColours[c%LinkColours.length], 0.1f*c));
                    canvas.drawCircle(xy[0], xy[1], half_margin, mPaint);
                    canvas.drawLine(xy[0], xy[1], fx, fy, mPaint);
                }
                xy[0] = fx;
                xy[1] = fy;
                counter[0] += 1;
            });

            mPaint.setColor(Color.RED);
            canvas.drawCircle(xy[0], xy[1], half_margin, mPaint);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void update(List<Double> data) {
        if(!data.isEmpty()) {
            Log.d(getClass().getSimpleName(), "update: " + data);
            mInput = data.get(data.size() - 1);
            _data.clear();
            _data.addAll(data.stream().filter(v->v != mInput).collect(Collectors.toList()));
            int intValue = mInput.intValue();
            List<Double> exploded = N2Utils.int2BitDoubles(intValue, data.size() - 1);
            mIsCorrect = _data.toString().equals(exploded.toString());
            invalidateSelf();
        }
    }
}
