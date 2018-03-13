package com.group.unkown.positioningminiproject.ui;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.group.unkown.positioningminiproject.R;

import java.io.InputStream;

import Model.BuildingModel;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        InputStream ins = getResources().openRawResource(getResources().getIdentifier("beacons", "raw", getPackageName()));
        BuildingModel model = new BuildingModel(ins);

        ((TextView)findViewById(R.id.tv_text)).setText((model.getBeacon("f7826da6-4fa2-4e98-8024-bc5b71e0893e").getRoomName()));
    }
}
