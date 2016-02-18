package christensenjohnsrud.funfit;

import android.app.PendingIntent;
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
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import java.util.ArrayList;

public class IntervalA extends AppCompatActivity implements SensorEventListener {

    private String className = "IntervalA.java"; //To debug


    //BUGGING
    private float accel_threshold = 10.0f;
    private long startTimeGoogle;

    // SENSOR
    private SensorManager sensorManager;
    private Sensor sensor;
    //TODO: Find appropriate default values
    private Float startThreshold = 14f;
    private Float stopThreshold = 9f;
    private float[] gravity = new float[3];

    // TIMER
    private Button startButton;
    private Button pauseButton;
    private TextView timerValue, currentActivity;
    private long startTime = 0L;
    private Timer timer;

    // CONNECT TIMER AND ACCELERATION
    private boolean timerRunning = false;
    private boolean blocked = false; // Activation/deactivation of timer for a given time
    private int startTimerCountDown= 15;
    private ListView resultsList;

    // RESULTS
    public ArrayList<IntervalItem> currentResults;
    private int intervalItemId;
    private ArrayAdapterItem adapter;
    private float maxX = 0;
    private float maxY = 0;
    private float maxZ = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interval);

        //  ACCELEROMETER
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);

        // TIMER
        timerValue = (TextView) findViewById(R.id.timer_value);
        timer = new Timer(IntervalA.this, R.id.timer_value);


        // RESULTS
        currentResults = new ArrayList<IntervalItem>();
        intervalItemId = 0;
        resultsList = (ListView) findViewById(R.id.list_view_interval_tracker);
        adapter = new ArrayAdapterItem(this, R.layout.list_view_row_item, currentResults);
        // create a new ListView, set the adapter and item click listener
        resultsList.setAdapter(adapter);

        // HELP BUTTONS
        startButton = (Button) findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                startTime = SystemClock.uptimeMillis();
                Log.i(className, "starttime " + startTime);
                //handler.removeCallbacks(updateTimeTask);
                //handler.postDelayed(updateTimeTask, 10); //The runnable is started every 10ms
                String currentIntervalDuration = timerValue.getText().toString();
                currentResults.add(new IntervalItem(intervalItemId, null, currentIntervalDuration, 0, 0, 0));
            }
        });

        pauseButton = (Button) findViewById(R.id.pause_button);
        pauseButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                //handler.removeCallbacks(updateTimeTask);

            }
        });



    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        // http://developer.android.com/guide/topics/sensors/sensors_motion.html

        final float alpha = 0.8f;

        // Isolate the force of gravity with the low-pass filter.
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        // Remove the gravity contribution with the high-pass filter.


        //Log.i(className, "x: " + (int) (event.values[0] - gravity[0]) + " y: " + (int) (event.values[1] - gravity[1]) + " z: " + (int) (event.values[2] - gravity[2]));
        float axisX = Math.abs(event.values[0] - gravity[0]);
        float axisY = Math.abs(event.values[1] - gravity[1]);
        float axisZ = Math.abs(event.values[2] - gravity[2]);

        if (axisX > maxX) maxX = axisX;
        if (axisY > maxY) maxY = axisY;
        if (axisZ > maxZ) maxZ = axisZ;

        if (startTimerCountDown > 0) {
            startTimerCountDown--;
            Log.i(className, "" + startTimerCountDown);
        }
        else {
            if ((axisX >= startThreshold || axisY >= startThreshold || axisZ >= startThreshold) && !blocked && !timerRunning) {
                Log.i(className, "*accelerometer* x=" + Math.round(axisX) + " y=" + Math.round(axisY) + " z=" + Math.round(axisZ));
                if (intervalItemId != 0) {
                    String currentIntervalDuration = timerValue.getText().toString();
                    currentResults.add(new IntervalItem(intervalItemId, IntervalItem.Type.PAUSE, currentIntervalDuration, maxX, maxY, maxZ));
                }

                maxX = 0;
                maxY = 0;
                maxZ = 0;

                timer.setStartTime(SystemClock.uptimeMillis());
                timer.removeHandlerCallback();
                timerValue.setText(timer.getCurrentTime());

                timer.postDelayed();


                timerRunning = true;
                blocked = true;
                blockTimer();
                intervalItemId ++;
                adapter.notifyDataSetChanged();
            }
            else if ((axisX >= stopThreshold || axisY >= stopThreshold || axisZ >= stopThreshold) && !blocked && timerRunning){
                String currentIntervalDuration = timerValue.getText().toString();
                currentResults.add(new IntervalItem(intervalItemId, IntervalItem.Type.RUN, currentIntervalDuration, maxX, maxY, maxZ));
                maxX = 0;
                maxY = 0;
                maxZ = 0;

                timer.setStartTime(SystemClock.uptimeMillis());
                timer.removeHandlerCallback();
                timerValue.setText(timer.getCurrentTime());

                timer.postDelayed();


                timerRunning = false;
                blocked = true;
                blockTimer();
                intervalItemId ++;
                adapter.notifyDataSetChanged();
            }
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void blockTimer(){
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                blocked = false;
            }
        }, 4000);

    }



}
