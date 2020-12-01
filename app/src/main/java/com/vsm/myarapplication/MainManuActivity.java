package com.vsm.myarapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.vsm.myarapplication.hand.HandActivity;

public class MainManuActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = MainManuActivity.class.getSimpleName();
    private Button buttonArWorld;
    private Button buttonSurfaceDetection;
    private boolean isFirstClick = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_manu);
        buttonSurfaceDetection = findViewById(R.id.buttonSurfaceDetection);
        buttonSurfaceDetection.setOnClickListener(this);
        buttonArWorld = findViewById(R.id.buttonArWorld);
        buttonArWorld.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonSurfaceDetection:
                startActivity(new Intent(MainManuActivity.this, MainActivity.class));
                break;
            case R.id.buttonArWorld:
                Intent intent = new Intent(this, HandActivity.class);
                startActivity(intent);
                break;
            default:
                Log.e(TAG, "onClick error!");
        }
    }
}