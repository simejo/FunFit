package christensenjohnsrud.funfit;

import android.content.BroadcastReceiver;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.DetectedActivityCreator;

import java.util.ArrayList;

public class Interval extends AppCompatActivity implements SensorEventListener, View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    private String className = "Interval.java"; //To debug

    //Google API
    private GoogleApiClient client;
    private Context context;

    //BUGGING
    private Button plus, minus;
    private EditText accel_tv;
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
        context = this;

        //Google API
        client = new GoogleApiClient.Builder(context)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        DetectedActivity activity = new DetectedActivity(0,60);

        Log.i("Activity", activity.toString());

        // FIND THRESHOLD HELPERS
        plus = (Button) findViewById(R.id.button_plus2);
        minus = (Button) findViewById(R.id.button_minus2);
        accel_tv = (EditText) findViewById(R.id.textView_speed_threshold2);

        plus.setOnClickListener(this);
        minus.setOnClickListener(this);

        //  ACCELEROMETER
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
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

        if (startTimerCountDown > 0){
            startTimerCountDown--;
        }

        else{
            startThreshold = accel_threshold;
            if ((axisX >= startThreshold || axisY >= startThreshold || axisZ >= startThreshold) && !blocked){
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
        }, 3000);
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


    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onResult(Status status) {

    }
}
