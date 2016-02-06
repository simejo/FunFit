package christensenjohnsrud.funfit;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class Interval extends AppCompatActivity implements SensorEventListener{

    private String className = "Interval.java"; //To debug

    //SESNOR
    private SensorManager sensorManager;
    private Sensor sensor;
    //TODO: Find appropriate default values
    private Float startThreshold = 7f;
    private Float stopThreshold = -7f;
    private float[] gravity = new float[3];

    //TODO: TIMER
    private Button start_button;
    private Button pause_button;
    private TextView timer_value;
    private long startTime = 0L;
    private Handler handler = new Handler();
    long timeInMilliseconds = 0L;
    long timeSwapBuff = 0L;
    long updatedTime = 0L;

    //TODO: Connect acceleration to timer



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interval);

        //  Setting up accelerometer sensor
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);

        // Setting up timer
        timer_value = (TextView) findViewById(R.id.timer_value);
        start_button = (Button) findViewById(R.id.start_button);
        start_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                //TODO: Start run timer, stop pause timer - ellernosaant
                startTime = SystemClock.uptimeMillis();
                Log.i(className, "starttime " + startTime);
                handler.removeCallbacks(updateTimeTask);
                handler.postDelayed(updateTimeTask, 10);
            }
        });
        pause_button = (Button) findViewById(R.id.pause_button);
        pause_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                //TODO: Stop run timer, start pause timer - ellernosaant
                timeSwapBuff += timeInMilliseconds;
                handler.removeCallbacks(updateTimeTask);

            }
        });
    }

    private Runnable updateTimeTask = new Runnable() {
        public void run() {
            final long start = startTime;
            long millis = SystemClock.uptimeMillis() - start;
            Log.i(className, "millis " + millis);

            int seconds = (int) (millis / 1000);
            Log.i(className, "seconds " + seconds);

            int minutes = seconds / 60;
            seconds     = seconds % 60;


            if (seconds < 10) {
                timer_value.setText("" + minutes + ":0" + seconds);
            } else {
                timer_value.setText("" + minutes + ":" + seconds);
            }

            handler.postAtTime(this,
                    start + (((minutes * 60) + seconds + 1) * 1000));
        }
    };


    @Override
    public void onSensorChanged(SensorEvent event) {

        // In this example, alpha is calculated as t / (t + dT),
        // where t is the low-pass filter's time-constant and
        // dT is the event delivery rate.
        final float alpha = 0.8f;

        // Isolate the force of gravity with the low-pass filter.
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        // Remove the gravity contribution with the high-pass filter.
        float axisX = event.values[0] - gravity[0];
        float axisY = event.values[1] - gravity[1];
        float axisZ = event.values[2] - gravity[2];

        if (axisX >= startThreshold || axisY >= startThreshold || axisZ >= startThreshold){
            //TODO: do something
            Log.i(className, "x=" + Math.round(axisX) + " y=" + Math.round(axisY) + " z=" + Math.round(axisZ));
        }
        else if (axisX <= stopThreshold || axisY <= stopThreshold || axisZ <= stopThreshold){
            //TODO: do something
            Log.i(className, "x=" + Math.round(axisX) + " y=" + Math.round(axisY) + " z=" + Math.round(axisZ));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}
