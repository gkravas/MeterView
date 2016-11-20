package com.gkravas.meterviewexample;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.gkravas.meterview.MeterView;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;

public class MainActivity extends AppCompatActivity {

    private MeterView meterView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        meterView = (MeterView) findViewById(R.id.meter_view);
    }

    @Override
    protected void onResume() {
        super.onResume();

        final float min = 0;
        final float max = 54;
        final Random random = new Random();

        Observable.interval(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Func1<Long, Float>() {
                    @Override
                    public Float call(Long aLong) {
                        return min + (random.nextFloat() * (max - min));
                    }
                })
                .subscribe(new Action1<Float>() {
                    @Override
                    public void call(Float value) {
                        meterView.setValue(value);
                    }
                });
    }
}
