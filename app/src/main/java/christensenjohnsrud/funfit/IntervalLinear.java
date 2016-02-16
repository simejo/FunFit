package christensenjohnsrud.funfit;

import android.content.Context;
import android.content.DialogInterface;
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

public class IntervalLinear extends AppCompatActivity implements SensorEventListener{

    private String className = "Interval.java"; //To debug


    //BUGGING
    private float accel_threshold = 10.0f;

    // SENSOR
    private SensorManager sensorManager;
    private Sensor sensor;
    //TODO: Find appropriate default values
    private Float startThreshold = 8f;
    private Float stopThreshold = -8f;
    private float[] gravity = new float[3];

    // TIMER
    private Button startButton;
    private Button pauseButton;
    private TextView timerValue;
    private long startTime = 0L;
    private Handler handler = new Handler();


    // CONNECT TIMER AND ACCELERATION
    private boolean timerOn = false;
    private boolean blocked = false; // Block activation/deactivation of timer for a given time
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
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);

        // TIMER
        timerValue = (TextView) findViewById(R.id.timer_value);

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
                handler.removeCallbacks(updateTimeTask);
                handler.postDelayed(updateTimeTask, 10); //The runnable is started every 10ms
            }
        });

        pauseButton = (Button) findViewById(R.id.pause_button);
        pauseButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                handler.removeCallbacks(updateTimeTask);

            }
        });
    }

    // Timer
    private Runnable updateTimeTask = new Runnable() {
        public void run() {
            final long start = startTime;
            long millis = SystemClock.uptimeMillis() - start;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds     = seconds % 60;

            //TODO: ADD TIDELER
            if (seconds < 10) {
                timerValue.setText("" + minutes + ":0" + seconds);
            } else {
                timerValue.setText("" + minutes + ":" + seconds);
            }
            handler.postAtTime(this,
                    start + (((minutes * 60) + seconds + 1) * 1000));
        }
    };


    @Override
    public void onSensorChanged(SensorEvent event) {

        float axisX = Math.abs(event.values[0]);
        float axisY = Math.abs(event.values[1]);
        float axisZ = Math.abs(event.values[2]);

        if (axisX > maxX) maxX = axisX;
        if (axisY > maxY) maxY = axisY;
        if (axisZ > maxZ) maxZ = axisZ;

        if (startTimerCountDown > 0){
            startTimerCountDown--;
        }
        else{
            if ((axisX >= accel_threshold || axisY >= accel_threshold || axisZ >= accel_threshold) && !blocked){
                Log.i(className, "*accelerometer* x=" + Math.round(axisX) + " y=" + Math.round(axisY) + " z=" + Math.round(axisZ));

                if (timerOn){
                    String currentIntervalDuration = timerValue.getText().toString();
                    currentResults.add(new IntervalItem(intervalItemId, IntervalItem.Type.RUN, currentIntervalDuration, maxX, maxY, maxZ));
                    maxX = 0;
                    maxY = 0;
                    maxZ = 0;

                    startTime = SystemClock.uptimeMillis();

                    handler.removeCallbacks(updateTimeTask);
                    handler.postDelayed(updateTimeTask, 10); //The runnable is started every 10ms

                    timerOn = false;
                    blocked = true;
                    blockTimer();
                }
                else{
                    String currentIntervalDuration = timerValue.getText().toString();
                    currentResults.add(new IntervalItem(intervalItemId, IntervalItem.Type.PAUSE, currentIntervalDuration, maxX, maxY, maxZ));
                    maxX = 0;
                    maxY = 0;
                    maxZ = 0;

                    startTime = SystemClock.uptimeMillis();
                    handler.removeCallbacks(updateTimeTask);
                    handler.postDelayed(updateTimeTask, 10); //The runnable is started every 10ms

                    timerOn = true;
                    blocked = true;
                    blockTimer();
                }
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
        }, 2000); // Milliseconds before it will check whether the user's acceleration is more than the threshold
    }

}
