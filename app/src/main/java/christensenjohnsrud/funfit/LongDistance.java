package christensenjohnsrud.funfit;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;

public class LongDistance extends Activity implements LocationListener, View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status>, com.yahoo.mobile.client.android.util.rangeseekbar.RangeSeekBar.OnRangeSeekBarChangeListener{

    LocationManager locationManager;
    TextView tvCurrentSpeed, tvTimer, tvCurrentActivity;
    Button btnTimer, btnFinish;
    CheckBox cbSpeedBoundaries;
    private float speedThresholdLower, speedThresholdUpper;
    private double km_h = 3.6, mph = 2.2369;
    private Timer totalTimer, walkingTimer, runningTimer;

    private boolean timerRunningOn = false, timerWalkingOn = false, timerOn = false;
    private int GPS_request_intensity = 5000;

    private ArrayList<Float> results;
    private Boolean speedBoundariesEnabled;

    private ToneGenerator tone;
    public static ArrayList<DataPoint[]> resultList;
    public static ArrayList<Integer> resultKeys;
    public int resultCounter = 0;

    //Google API
    private long startTimeGoogle;
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

    // SeekBar
    private RangeSeekBar<Integer> rangeSeekBar;
    LinearLayout seekBarLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_long_distance);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        tvCurrentSpeed = (TextView) findViewById(R.id.textView_current_speed);
        tvTimer = (TextView) findViewById(R.id.timer_long_distance_total);

        speedThresholdLower = /*Load a value from database*/ 6.0f;
        speedThresholdUpper = /*Load a value from database*/ 9.0f;

        btnTimer = (Button) findViewById(R.id.button_timer_long_distance);
        btnFinish = (Button) findViewById(R.id.button_timer_stop);

        btnTimer.setOnClickListener(this);
        btnFinish.setOnClickListener(this);

        tone = new ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME);
        results = new ArrayList<Float>();
        resultList = new ArrayList<DataPoint[]>();
        resultKeys = new ArrayList<Integer>();




        tvCurrentActivity = (TextView) findViewById(R.id.text_view_current_activity);

        totalTimer = new Timer(this, R.id.timer_long_distance_total);
        runningTimer = new Timer(this, R.id.timer_long_distance_running);
        walkingTimer = new Timer(this, R.id.timer_long_distance_walking);

        // GOOGLE API
        // Get a receiver for broadcasts from ActivityDetectionIntentService.
        startTimeGoogle = SystemClock.uptimeMillis();
        mBroadcastReceiver = new ActivityDetectionBroadcastReceiver();


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

        // Kick off the request to build GoogleApiClient.
        buildGoogleApiClient();

        // RangeSeekBar
        rangeSeekBar = new RangeSeekBar<Integer>(this);
        // Set the range
        rangeSeekBar.setRangeValues(0, 30);
        rangeSeekBar.setSelectedMinValue(6);
        rangeSeekBar.setSelectedMaxValue(9);
        rangeSeekBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener<Integer>() {
            @Override
            public void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar, Integer minValue, Integer maxValue) {
                speedThresholdLower = minValue;
                speedThresholdUpper = maxValue;
            }
        });
        // Add to layout
        seekBarLayout = (LinearLayout) findViewById(R.id.seekbar_placeholder);
        seekBarLayout.addView(rangeSeekBar);

        // Checkbox to enable speed threshold
        cbSpeedBoundaries = (CheckBox) findViewById(R.id.checkBox_speed_boundaries);
        cbSpeedBoundaries.setChecked(true);
        setSpeedBoundariesEnabled(true);
        cbSpeedBoundaries.setOnClickListener(this);


    }

    @Override
    public void onLocationChanged(Location location) {
        location.getLatitude();
        Log.i("onLocationChanged", "called");
        double speed = location.getSpeed()*km_h; //Convert from m/s to km/h
        String speedHolder = "Current speed: " + speed;
        if(speed < speedThresholdLower){
            if(speedBoundariesEnabled){
                tone.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 1000);
                speedHolder += " TOO SLOW";
            }
        } else if (speed > speedThresholdUpper) {
            if (speedBoundariesEnabled) {
                tone.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 100);
                speedHolder += " TOO FAST";
            }
        }
        tvCurrentSpeed.setText(speedHolder);
        results.add((float) speed);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.i("onStatusChanged", "called");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.i("onProviderEnabled", "called");
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.i("onProviderDisabled", "called");

    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.button_timer_long_distance){
            if(timerOn){ //Wants to pause timer
                timerOn = false;
                totalTimer.pause();
                runningTimer.pause();
                walkingTimer.pause();
                LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
                btnTimer.setText("Start");
                locationManager.removeUpdates(this);
            }
            else{
                timerOn = true;
                totalTimer.start();
                LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, new IntentFilter(Constants.BROADCAST_ACTION));
                btnTimer.setText("Pause");

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                if ( !locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
                    buildAlertMessageNoGps();
                }
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_request_intensity, 0, this);
            }
        }
        else if(v.getId() == R.id.button_timer_stop){
            totalTimer.finish();
            runningTimer.finish();
            walkingTimer.finish();
            timerWalkingOn = false;
            timerRunningOn = false;
            timerOn = false;
            btnTimer.setText("Start");
            if(results.isEmpty()){
                //Adding test data
                results.add(4.3f);
                results.add(5.3f);
                results.add(4.3f);
                results.add(3.0f);
                results.add(4.0f);
                results.add(6.0f);
                results.add(6.0f);
                results.add(6.0f);
                results.add(5.3f);
            }
            DataPoint[] convertedResults = convertToDataPointArray(results);
            if(resultList.equals(null)){
                resultList = new ArrayList<DataPoint[]>();
            }
            resultList.add(convertedResults);
            resultKeys.add(resultCounter);
            resultCounter++;
            results.clear();
            locationManager.removeUpdates(this);
        } else if (v.getId() == R.id.checkBox_speed_boundaries){
            if(cbSpeedBoundaries.isChecked()){
                setSpeedBoundariesEnabled(true);
            } else{
                setSpeedBoundariesEnabled(false);
            }
        }
    }

    public void setSpeedBoundariesEnabled(boolean boo){
        speedBoundariesEnabled = boo;
        rangeSeekBar.setEnabled(boo);
        if (boo) {
            rangeSeekBar.setAlpha(1.0f);
        }
        else{
            rangeSeekBar.setAlpha(0.5f);
        }
    }

    public DataPoint[] convertToDataPointArray(ArrayList<Float> result){
        DataPoint[] convertedResults = new DataPoint[result.size()];
        for(int i = 0; i < result.size(); i++){
            convertedResults[i] = new DataPoint(i, result.get(i));
        }
        return convertedResults;
    }


    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
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

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i("Interval", "Connected to GoogleApiClient");
        requestActivityUpdatesHandler();
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
    public void requestActivityUpdatesHandler() {
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
    public void removeActivityUpdatesHandler() {
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
     *
     * @param status The Status returned through a PendingIntent when requestActivityUpdates()
     *               or removeActivityUpdates() are called.
     */
    public void onResult(Status status) {
        if (status.isSuccess()) {
            // Toggle the status of activity updates requested, and save in shared preferences.
            boolean requestingUpdates = !getUpdatesRequestedState();
            setUpdatesRequestedState(requestingUpdates);
        } else {
            Log.e("Interval", "Error adding or removing activity detection: " + status.getStatusMessage());
        }
    }

    /**
     * Gets a PendingIntent to be sent for each activity detection.
     */
    private PendingIntent getActivityDetectionPendingIntent() {
        Intent intent = new Intent(this, DetectedActivitiesIntentService.class);
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
        tvCurrentActivity.setText(holder);
        int currentType0 = detectedActivities.get(0).getType();

        if (currentType0 == DetectedActivity.RUNNING ||
                (currentType0 == DetectedActivity.ON_FOOT &&  detectedActivities.get(1).getType() == DetectedActivity.RUNNING)) {
            if(!timerRunningOn){
                runningTimer.start();
                timerRunningOn = true;
            }
            if(timerWalkingOn){
                walkingTimer.pause();
                timerWalkingOn = false;
            }
        }
        else if (currentType0 == DetectedActivity.WALKING ||
                (currentType0 == DetectedActivity.ON_FOOT &&  detectedActivities.get(1).getType() == DetectedActivity.WALKING)) {
            if(timerRunningOn){
                runningTimer.pause();
                timerRunningOn = false;
            }
            if(!timerWalkingOn){
                walkingTimer.start();
            }
            timerWalkingOn = true;
        }
        else if (timerRunningOn && timerWalkingOn){
            runningTimer.pause();
            walkingTimer.pause();
            timerRunningOn = false;
            timerWalkingOn = false;
        }
        else if (timerRunningOn){
            runningTimer.pause();
            timerRunningOn = false;
        }
        else if (timerWalkingOn){
            walkingTimer.pause();
            timerWalkingOn = false;
        }
    }

    @Override
    public void onRangeSeekBarValuesChanged(com.yahoo.mobile.client.android.util.rangeseekbar.RangeSeekBar bar, Object minValue, Object maxValue) {

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
