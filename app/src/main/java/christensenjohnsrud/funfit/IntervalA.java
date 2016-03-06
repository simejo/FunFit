package christensenjohnsrud.funfit;

import android.app.Activity;
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
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.TextView;


import java.util.ArrayList;

public class IntervalA extends AppCompatActivity implements SensorEventListener, View.OnClickListener, SeekBar.OnSeekBarChangeListener {


    private String className = "IntervalA.java"; //To debug


    // TESTING
    private float accel_threshold = 4f;
    private Button plusThreshold, minusThreshold, plusSR, minusSR;
    private EditText accel_tv;
    //private EditText sampleRate_tv;
    private TextView sampleRate_tv;
    private NumberPicker npSampleRate;
    private String[] sampleRateArray = {"100","200","500", "1000", "1500", "2000"};
    private int accelSampleRate;
    private SeekBar seekBarThreshold;

    // SENSOR
    private SensorManager sensorManager;
    private Sensor sensor;
    //TODO: Find appropriate default values

    private float[] gravity = new float[3];

    // TIMER
    private TextView timerValue;
    private long startTime = 0L;
    private Timer timer;
    private Handler handlerCheck, handlerRunPause;

    // CONNECT TIMER AND ACCELERATION
    private boolean timerRunning = false;       // Timer is for running or pause
    private boolean blockedCheck = false;       // Avoid to check max acceleration too often
    private boolean blockedRunPause = false;    // Avoid to start run/pause timer too often
    private int startTimerCountDown= 15;
    private ListView resultsList;

    // RESULTS
    public ArrayList<IntervalItem> currentResults;
    private int intervalItemId;
    private ArrayAdapterItem adapter;
    private float maxX = 0;
    private float maxY = 0;
    private float maxZ = 0;

    // TESTNG
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
        // Create a new ListView, set the adapter and item click listener
        resultsList.setAdapter(adapter);



        // Acceleration threshold
        seekBarThreshold = (SeekBar)findViewById(R.id.seekbar_threshold); // make seekbar object
        seekBarThreshold.setOnSeekBarChangeListener(this);

        // Acceleration sample rate
        sampleRate_tv = (TextView) findViewById(R.id.text_view_sample_rate);
        accelSampleRate = 500;
        npSampleRate = (NumberPicker) findViewById(R.id.np_sample_rate);
        npSampleRate.setMinValue(0); //from array first value
        //Specify the maximum value/number of NumberPicker
        npSampleRate.setMaxValue(sampleRateArray.length - 1); //to array last value
        npSampleRate.setWrapSelectorWheel(true);
        npSampleRate.setDisplayedValues(sampleRateArray);
        npSampleRate.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                // TODO Auto-generated method stub
                sampleRate_tv.setText("Acceleration sample rate is " + sampleRateArray[newVal] + " ms");
                accelSampleRate = (int) Integer.parseInt(sampleRateArray[newVal]);
            }
        });


        accelDataList = new Integer[]{0,0,0,0,0};
        accelDataListLength = accelDataList.length;
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
        float axisX = Math.abs(event.values[0] - gravity[0]);
        float axisY = Math.abs(event.values[1] - gravity[1]);
        float axisZ = Math.abs(event.values[2] - gravity[2]);

        if (axisX > maxX) maxX = axisX;
        if (axisY > maxY) maxY = axisY;
        if (axisZ > maxZ) maxZ = axisZ;

        // Need this because it takes several iterations to remove the gravity
        if (startTimerCountDown > 0) {
            startTimerCountDown--;
        }
        else if(!blockedCheck) {
            // Updating accelDataList
            int acc = getMax((int)axisX, (int)axisY, (int)axisZ);
            accelDataList[accel_counter] = acc;
            accel_counter += 1;
            accel_counter = accel_counter % accelDataListLength;

            // Test
            printAccelDataList();

            blockedCheck =true;
            blockTimerCheck();

            // Start drag / Done with pause
            if (!blockedRunPause && !timerRunning && running()) {
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
            // Start pause / Done with drag
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
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void blockTimerCheck(){
        handlerCheck = null;
        handlerCheck = new Handler();
        handlerCheck.postDelayed(new Runnable() {
            @Override
            public void run() {
                blockedCheck = false;
            }
        }, accelSampleRate); // Possibility to change this. {"100","200","500", "1000", "1500", "2000"} ms

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

    public void printAccelDataList(){
        String holder = "";
        for (Integer x : accelDataList){
            holder += x + ", ";
        }
        Log.i(className + " ACCEL DATA", holder);
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
