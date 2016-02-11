package christensenjohnsrud.funfit;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class LongDistance extends AppCompatActivity implements LocationListener, View.OnClickListener, TextWatcher{

    LocationManager locationManager;
    TextView tvCurrentSpeed, tvSpeedThreshold;
    Button btnPlus, btnMinus;
    private float speedThreshold;
    private double km_h = 3.6, mph = 2.2369;

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

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        tvCurrentSpeed = (TextView) findViewById(R.id.textView_current_speed);

        speedThreshold = /*Load a value from database*/ 8.0f;

        btnMinus = (Button) findViewById(R.id.button_minus);
        btnPlus = (Button) findViewById(R.id.button_plus);

        btnMinus.setOnClickListener(this);
        btnPlus.setOnClickListener(this);

        tvSpeedThreshold = (TextView) findViewById(R.id.textView_speed_threshold);
        tvSpeedThreshold.addTextChangedListener(this);
        updateThresholdText();


    }

    @Override
    public void onLocationChanged(Location location) {
        location.getLatitude();
        Log.i("onLocationChanged", "called");
        double speed = location.getSpeed()*km_h; //Convert from m/s to km/h
        tvCurrentSpeed.setText("Current speed: " + speed + "GPS accuracy" + location.getAccuracy());
        if(speed < speedThreshold){
            tvCurrentSpeed.setText("Current speed: " + speed + "GPS accuracy" + location.getAccuracy() + "TOO SLOW");
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.i("onStatusChanged","called");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.i("onProviderEnabled","called");
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.i("onProviderDisabled","called");

    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.button_minus){
            if(speedThreshold > 0.1){
                speedThreshold -= 0.1;
            }
            else{
                speedThreshold = 0;
            }
        }
        else if(v.getId() == R.id.button_plus){
            speedThreshold += 0.1;
        }
        updateThresholdText();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        if(s.toString().equals("")){
            speedThreshold = 0;
        }
        else{
            String num = s.toString().replace(',','.');
            speedThreshold = Float.valueOf(num);
        }

    }

    public void updateThresholdText(){
        tvSpeedThreshold.setText(String.format("%.1f", speedThreshold));
    }
}
