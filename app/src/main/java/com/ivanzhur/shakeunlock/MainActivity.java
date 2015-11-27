package com.ivanzhur.shakeunlock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    SensorManager sensorManager;
    Sensor accelerometer;
    TextView accTextView;
    long lastTime = 0;
    boolean logging = false;
    List<Double> vals;

    static final String NAME_PREFERENCES = "com.ivanzhur.shakeunlock.sharedpreferences";
    static SharedPreferences preferences;
    static SharedPreferences.Editor editor;
    static final String DEFAULT[] = {"GRAPH_DEFAULT_1", "GRAPH_DEFAULT_2", "GRAPH_DEFAULT_3"};

    final String TAG = "WAKE_TEST";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        accTextView = (TextView)findViewById(R.id.accTextView);

        setSupportActionBar(toolbar);

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);

        preferences = getSharedPreferences(NAME_PREFERENCES, Context.MODE_PRIVATE);
        editor = preferences.edit();

        Button fab = (Button) findViewById(R.id.addButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), AddActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();
        /*IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        BroadcastReceiver receiver = new ScreenOnReceiver();
        registerReceiver(receiver, filter);
        Log.i(TAG, "ScreenOnReceiver registered");*/
        Intent service = new Intent(this, UpdateService.class);
        startService(service);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTime < 100) return;
        lastTime = currentTime;

        Sensor sensor = event.sensor;

        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            double sum = Math.sqrt(x*x+y*y+z*z);
            sum = Math.round(sum*100)/100.0;
            accTextView.setText(x+"\n"+y+"\n"+z+"\n"+sum);

            if (vals!=null) vals.add(sum);
            if (logging) Log.v("SENSOR_VALUES", "x: " + x + " y: " + y + " z: " + z + "   sum: " + sum);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        /*if (id == R.id.action_settings) {
            return true;
        }*/

        return super.onOptionsItemSelected(item);
    }
}
