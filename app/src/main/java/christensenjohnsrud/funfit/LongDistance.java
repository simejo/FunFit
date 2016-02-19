package christensenjohnsrud.funfit;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class LongDistance extends AppCompatActivity implements LocationListener, View.OnClickListener{

    LocationManager locationManager;
    TextView tvCurrentSpeed, tvSpeedThresholdLower, tvSpeedThresholdUpper, tvAccuracy, tvTimer;
    Button btnPlusLower, btnMinusLower, btnPlusUpper, btnMinusUpper, btnTimer;
    private float speedThresholdLower, speedThresholdUpper;
    private double km_h = 3.6, mph = 2.2369;
    private Timer timer;
    private boolean timerOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_long_distance);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i("INSIDE IF", "_----------__--_-_-_");
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        if ( !locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            buildAlertMessageNoGps();
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        tvCurrentSpeed = (TextView) findViewById(R.id.textView_current_speed);
        tvAccuracy = (TextView) findViewById(R.id.textView_accuracy);
        tvTimer = (TextView) findViewById(R.id.timer_long_distance);

        speedThresholdLower = /*Load a value from database*/ 6.0f;
        speedThresholdUpper = /*Load a value from database*/ 9.0f;

        btnMinusLower = (Button) findViewById(R.id.button_minus_lower);
        btnPlusLower = (Button) findViewById(R.id.button_plus_lower);
        btnMinusUpper = (Button) findViewById(R.id.button_minus_upper);
        btnPlusUpper = (Button) findViewById(R.id.button_plus_upper);
        btnTimer = (Button) findViewById(R.id.button_timer_long_distance);

        btnMinusLower.setOnClickListener(this);
        btnPlusLower.setOnClickListener(this);
        btnMinusUpper.setOnClickListener(this);
        btnPlusUpper.setOnClickListener(this);
        btnTimer.setOnClickListener(this);

        tvSpeedThresholdLower = (TextView) findViewById(R.id.textView_speed_threshold_lower);
        tvSpeedThresholdLower.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().equals("")){
                    speedThresholdLower = 0;
                }
                else {
                    String num = s.toString().replace(',','.');
                    speedThresholdLower = Float.valueOf(num);
                }
            }
        });
        tvSpeedThresholdUpper = (TextView) findViewById(R.id.textView_speed_threshold_upper);
        tvSpeedThresholdUpper.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().equals("")){
                    speedThresholdUpper = 0;
                }
                else {
                    String num = s.toString().replace(',','.');
                    speedThresholdUpper = Float.valueOf(num);
                }
            }
        });
        updateThresholdText();

        timer = new Timer(this, R.id.timer_long_distance);


    }

    @Override
    public void onLocationChanged(Location location) {
        location.getLatitude();
        Log.i("onLocationChanged", "called");
        double speed = location.getSpeed()*km_h; //Convert from m/s to km/h
        tvAccuracy.setText("Accuracy: " + location.getAccuracy());
        if(speed < speedThresholdLower){
            tvCurrentSpeed.setText("Current speed: " + speed + "TOO SLOW");
        } else if (speed > speedThresholdUpper){
            tvCurrentSpeed.setText("Current speed: " + speed + "TOO FAST");
        } else{
            tvCurrentSpeed.setText("Current speed: " + speed);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        tvAccuracy.setText("Accuracy: onStatusChange");
        Log.i("onStatusChanged", "called");
    }

    @Override
    public void onProviderEnabled(String provider) {
        tvAccuracy.setText("Accuracy: onProviderEnabled");
        Log.i("onProviderEnabled", "called");
    }

    @Override
    public void onProviderDisabled(String provider) {
        tvAccuracy.setText("Accuracy: onProviderDisabled");
        Log.i("onProviderDisabled", "called");

    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.button_minus_lower){
            if(speedThresholdLower > 0.1){
                speedThresholdLower -= 0.1;
            }
            else{
                speedThresholdLower = 0;
            }
        }
        else if(v.getId() == R.id.button_plus_lower){
            if(speedThresholdLower < speedThresholdUpper){
                speedThresholdLower += 0.1;
            }
        }

        else if(v.getId() == R.id.button_minus_upper){
            if(speedThresholdUpper > 0.1){
                if(speedThresholdLower < speedThresholdUpper){
                    speedThresholdUpper -= 0.1;
                }
            }
            else{
                speedThresholdUpper = 0;
            }
        }
        else if(v.getId() == R.id.button_plus_upper){
            speedThresholdUpper += 0.1;
        }
        else if(v.getId() == R.id.button_timer_long_distance){
            if(timerOn){
                timerOn = false;
                timer.setStartTime(SystemClock.uptimeMillis());
                timer.removeHandlerCallback();
                tvTimer.setText(timer.getCurrentTime());
                timer.postDelayed();
                btnTimer.setText("Start");
            }
            else{
                timerOn = true;

                timer.setStartTime(SystemClock.uptimeMillis());
                timer.removeHandlerCallback();
                tvTimer.setText(timer.getCurrentTime());

                timer.postDelayed();
                btnTimer.setText("Pause");
            }
        }

        updateThresholdText();
    }

    public void updateThresholdText(){
        tvSpeedThresholdLower.setText(String.format("%.1f", speedThresholdLower));
        tvSpeedThresholdUpper.setText(String.format("%.1f", speedThresholdUpper));
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
}
