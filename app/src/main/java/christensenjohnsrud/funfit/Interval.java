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
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;
import java.util.ArrayList;

public class Interval extends AppCompatActivity implements SensorEventListener, View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    private String className = "Interval.java"; //To debug

    //Google API
    private GoogleApiClient client;
    private Context context;
    /**
     * A receiver for DetectedActivity objects broadcast by the
     * {@code ActivityDetectionIntentService}.
     */
    protected ActivityDetectionBroadcastReceiver mBroadcastReceiver;

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

    //private ListView mDetectedActivitiesListView;

    /**
     * The DetectedActivities that we track in this sample. We use this for initializing the
     * {@code DetectedActivitiesAdapter}. We also use this for persisting state in
     * {@code onSaveInstanceState()} and restoring it in {@code onCreate()}. This ensures that each
     * activity is displayed with the correct confidence level upon orientation changes.
     */
    private ArrayList<DetectedActivity> mDetectedActivities;

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
    private TextView timerValue, currentActivity;
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


        // GOOGLE API ------------------------------------------------------------------------------------

        // Get a receiver for broadcasts from ActivityDetectionIntentService.
        mBroadcastReceiver = new ActivityDetectionBroadcastReceiver();

        //finding current activity text field
        currentActivity = (TextView) findViewById(R.id.current_activity);

        // Enable either the Request Updates button or the Remove Updates button depending on
        // whether activity updates have been requested.
        //setButtonsEnabledState();

        // Reuse the value of mDetectedActivities from the bundle if possible. This maintains state
        // across device orientation changes. If mDetectedActivities is not stored in the bundle,
        // populate it with DetectedActivity objects whose confidence is set to 0. Doing this
        // ensures that the bar graphs for only only the most recently detected activities are
        // filled in.
        if (savedInstanceState != null && savedInstanceState.containsKey(
                Constants.DETECTED_ACTIVITIES)) {
            mDetectedActivities = (ArrayList<DetectedActivity>) savedInstanceState.getSerializable(Constants.DETECTED_ACTIVITIES);
        } else {
            mDetectedActivities = new ArrayList<DetectedActivity>();

            // Set the confidence level of each monitored activity to zero.
            for (int i = 0; i < Constants.MONITORED_ACTIVITIES.length; i++) {
                mDetectedActivities.add(new DetectedActivity(Constants.MONITORED_ACTIVITIES[i], 0));
            }
        }
        currentActivity.setText(mDetectedActivities.get(0).toString() + " " + mDetectedActivities.get(1).toString());
        
        // Kick off the request to build GoogleApiClient.
        buildGoogleApiClient();

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


        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, "Not connected",
                    Toast.LENGTH_SHORT).show();
            return;
        }

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
        }, 1000);
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


    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * ActivityRecognition API.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(ActivityRecognition.API)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();

    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register the broadcast receiver that informs this activity of the DetectedActivity
        // object broadcast sent by the intent service.
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
                new IntentFilter(Constants.BROADCAST_ACTION));
    }

    @Override
    protected void onPause() {
        // Unregister the broadcast receiver that was registered during onResume().
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i("Interval", "Connected to GoogleApiClient");
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                mGoogleApiClient,
                Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
                getActivityDetectionPendingIntent()
        ).setResultCallback(this);
        currentActivity.setText("OnConnected ");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i("Interval", "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i("Interval", "Connection suspended");
        mGoogleApiClient.connect();
    }

    /**
     * Registers for activity recognition updates using
     * {@link com.google.android.gms.location.ActivityRecognitionApi#requestActivityUpdates} which
     * returns a {@link com.google.android.gms.common.api.PendingResult}. Since this activity
     * implements the PendingResult interface, the activity itself receives the callback, and the
     * code within {@code onResult} executes. Note: once {@code requestActivityUpdates()} completes
     * successfully, the {@code DetectedActivitiesIntentService} starts receiving callbacks when
     * activities are detected.
     */
    public void requestActivityUpdatesButtonHandler(View view) {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, "Not connected",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                mGoogleApiClient,
                Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
                getActivityDetectionPendingIntent()
        ).setResultCallback(this);
        currentActivity.setText("requestAUBH " + mDetectedActivities.get(0).toString() + " " + mDetectedActivities.get(1).toString());
    }

    /**
     * Removes activity recognition updates using
     * {@link com.google.android.gms.location.ActivityRecognitionApi#removeActivityUpdates} which
     * returns a {@link com.google.android.gms.common.api.PendingResult}. Since this activity
     * implements the PendingResult interface, the activity itself receives the callback, and the
     * code within {@code onResult} executes. Note: once {@code removeActivityUpdates()} completes
     * successfully, the {@code DetectedActivitiesIntentService} stops receiving callbacks about
     * detected activities.
     */
    public void removeActivityUpdatesButtonHandler(View view) {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        // Remove all activity updates for the PendingIntent that was used to request activity
        // updates.
        ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(
                mGoogleApiClient,
                getActivityDetectionPendingIntent()
        ).setResultCallback(this);
    }

    /**
     * Runs when the result of calling requestActivityUpdates() and removeActivityUpdates() becomes
     * available. Either method can complete successfully or with an error.
     *
     * @param status The Status returned through a PendingIntent when requestActivityUpdates()
     *               or removeActivityUpdates() are called.
     */
    public void onResult(Status status) {
        if (status.isSuccess()) {
            // Toggle the status of activity updates requested, and save in shared preferences.
            boolean requestingUpdates = !getUpdatesRequestedState();
            setUpdatesRequestedState(requestingUpdates);
            currentActivity.setText("On result " + mDetectedActivities.get(0).toString() + " " + mDetectedActivities.get(1).toString());


            // Update the UI. Requesting activity updates enables the Remove Activity Updates
            // button, and removing activity updates enables the Add Activity Updates button.
            //setButtonsEnabledState();

        } else {
            Log.e("Interval", "Error adding or removing activity detection: " + status.getStatusMessage());
        }
    }

    /**
     * Gets a PendingIntent to be sent for each activity detection.
     */
    private PendingIntent getActivityDetectionPendingIntent() {
        Intent intent = new Intent(this, DetectedActivitiesIntentService.class);
        Log.i(className, "getActivityDetectionPendingIntent");
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // requestActivityUpdates() and removeActivityUpdates().
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Retrieves a SharedPreference object used to store or read values in this app. If a
     * preferences file passed as the first argument to {@link #getSharedPreferences}
     * does not exist, it is created when {@link SharedPreferences.Editor} is used to commit
     * data.
     */
    private SharedPreferences getSharedPreferencesInstance() {
        return getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
    }

    /**
     * Retrieves the boolean from SharedPreferences that tracks whether we are requesting activity
     * updates.
     */
    private boolean getUpdatesRequestedState() {
        Log.i(className, "getUpdatesRequestedState");
        return getSharedPreferencesInstance()
                .getBoolean(Constants.ACTIVITY_UPDATES_REQUESTED_KEY, false);
    }

    /**
     * Sets the boolean in SharedPreferences that tracks whether we are requesting activity
     * updates.
     */
    private void setUpdatesRequestedState(boolean requestingUpdates) {
        getSharedPreferencesInstance()
                .edit()
                .putBoolean(Constants.ACTIVITY_UPDATES_REQUESTED_KEY, requestingUpdates)
                .commit();
    }

    /**
     * Stores the list of detected activities in the Bundle.
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putSerializable(Constants.DETECTED_ACTIVITIES, mDetectedActivities);
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Processes the list of freshly detected activities. Asks the adapter to update its list of
     * DetectedActivities with new {@code DetectedActivity} objects reflecting the latest detected
     * activities.
     */
    protected void updateDetectedActivitiesList(ArrayList<DetectedActivity> detectedActivities) {
        String holder = "";
        for(DetectedActivity da: detectedActivities){
            holder += da.toString() + ", ";
        }
        currentActivity.setText("Update: " + holder);
        Log.i(className, "updateDetectedActivitiesList");
    }

    /**
     * Receiver for intents sent by DetectedActivitiesIntentService via a sendBroadcast().
     * Receives a list of one or more DetectedActivity objects associated with the current state of
     * the device.
     */
    public class ActivityDetectionBroadcastReceiver extends BroadcastReceiver {
        protected static final String TAG = "activity-detection";

        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<DetectedActivity> updatedActivities =
                    intent.getParcelableArrayListExtra(Constants.ACTIVITY_EXTRA);
            updateDetectedActivitiesList(updatedActivities);
            Log.i(TAG, "ActivityDetectionBroadcastReceiver");

        }
    }
}
