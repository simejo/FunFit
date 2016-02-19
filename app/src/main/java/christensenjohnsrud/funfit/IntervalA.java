package christensenjohnsrud.funfit;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;


import java.util.ArrayList;

public class IntervalA extends AppCompatActivity implements SensorEventListener, View.OnClickListener {

    private String className = "IntervalA.java"; //To debug


    //BUGGING
    private float accel_threshold = 6f;
    private long startTimeGoogle;
    private Button plus, minus;
    private EditText accel_tv;

    // SENSOR
    private SensorManager sensorManager;
    private Sensor sensor;
    //TODO: Find appropriate default values
    private Float startThreshold = 14f;
    private Float stopThreshold = 11f;
    private float[] gravity = new float[3];

    // TIMER
    private Button startButton;
    private Button pauseButton;
    private TextView timerValue, currentActivity;
    private long startTime = 0L;
    private Timer timer;
    private Handler handlerCheck, handlerRunPause;

    // CONNECT TIMER AND ACCELERATION
    private boolean timerRunning = false;
    private boolean blockedCheck = false; // Activation/deactivation of timer for a given time
    private boolean blockedRunPause = false;

    private int startTimerCountDown= 15;
    private ListView resultsList;

    // RESULTS
    public ArrayList<IntervalItem> currentResults;
    private int intervalItemId;
    private ArrayAdapterItem adapter;
    private float maxX = 0;
    private float maxY = 0;
    private float maxZ = 0;

    //TESTNG THINGS
    /*The idea is to collect the last seconds of acceleration data, in order to check for how long
    * time the user may have been running but the google api did not detect it*/
    Integer[] accelDataList;
    int accelDataListLength;
    int accel_counter = 0;


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
                //handlerCheck.removeCallbacks(updateTimeTask);
                //handlerCheck.postDelayed(updateTimeTask, 10); //The runnable is started every 10ms
                String currentIntervalDuration = timerValue.getText().toString();
                currentResults.add(new IntervalItem(intervalItemId, null, currentIntervalDuration));
            }
        });

        pauseButton = (Button) findViewById(R.id.pause_button);
        pauseButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                //handlerCheck.removeCallbacks(updateTimeTask);
                blockedCheck = true;
            }
        });


        plus = (Button) findViewById(R.id.button_plus2);
        minus = (Button) findViewById(R.id.button_minus2);
        accel_tv = (EditText) findViewById(R.id.textView_speed_threshold2);

        plus.setOnClickListener(this);
        minus.setOnClickListener(this);
        accelDataList = new Integer[]{0,0,0,0,0};
        accelDataListLength = accelDataList.length;
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        // http://developer.android.com/guide/topics/sensors/sensors_motion.html
        startThreshold = accel_threshold;
        stopThreshold = accel_threshold;

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
        else if(!blockedCheck) {
            // Updating accelDataList
            int acc = getMax((int)axisX, (int)axisY, (int)axisZ);
            accelDataList[accel_counter] = acc;
            accel_counter += 1;
            accel_counter = accel_counter % accelDataListLength;
            // Testing
            String holder = "";
            for (Integer x : accelDataList){
                holder += x + ", ";
            }
            Log.i(className + " ACCEL DATA", holder);
            running();

            blockedCheck =true;
            blockTimerCheck();


            // Start drag / done with pause
            if (!blockedRunPause && !timerRunning && running()) {
                Log.i(className, "*accelerometer* x=" + Math.round(axisX) + " y=" + Math.round(axisY) + " z=" + Math.round(axisZ));
                if (intervalItemId != 0) {
                    String currentIntervalDuration = timerValue.getText().toString();
                    currentResults.add(new IntervalItem(intervalItemId, IntervalItem.Type.PAUSE, currentIntervalDuration));
                }

                timer.resetTimer();
                timerValue.setText(timer.getCurrentTime());


                timerRunning = true;
                blockedRunPause = true;
                blockTimerRunPause();
                intervalItemId ++;
                adapter.notifyDataSetChanged();
            }
            // Start pause/ done with drag
            else if (timerRunning && !running()){
                String currentIntervalDuration = timerValue.getText().toString();
                currentResults.add(new IntervalItem(intervalItemId, IntervalItem.Type.RUN, currentIntervalDuration));

                timer.resetTimer();
                timerValue.setText(timer.getCurrentTime());

                timerRunning = false;
                blockedRunPause = true;
                blockTimerRunPause();
                intervalItemId ++;
                adapter.notifyDataSetChanged();
            }

        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void blockTimerCheck(){
        handlerCheck = null;
        handlerCheck = new Handler();
        handlerCheck.postDelayed(new Runnable() {
            @Override
            public void run() {
                blockedCheck = false;
            }
        }, 1000);

    }
    private void blockTimerRunPause(){
        handlerRunPause = null;
        handlerRunPause = new Handler();
        handlerRunPause.postDelayed(new Runnable() {
            @Override
            public void run() {
                blockedRunPause = false;
            }
        }, 6000);

    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.button_plus2){
                accel_threshold++;
                accel_tv.setText(accel_threshold + "");
        }
        else if(v.getId() == R.id.button_minus2){
                accel_threshold--;
                accel_tv.setText(accel_threshold + "");
        }
    }

    public int getMax(int x, int y, int z){
        return Math.max(x, Math.max(y,z));
    }

    public boolean running(){
        int holder = 0;
        for (int data : accelDataList){
            holder+= data;
        }
        float avg = holder/accelDataListLength;
        Log.i(className + " AVG: ", avg + "");
        if (avg > accel_threshold){
            return true;
        }

        return false;
    }



}
