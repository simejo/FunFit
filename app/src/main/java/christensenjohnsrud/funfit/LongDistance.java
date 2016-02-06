package christensenjohnsrud.funfit;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.RequiresPermission;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

public class LongDistance extends AppCompatActivity implements LocationListener, View.OnClickListener, TextWatcher{

    LocationManager locationManager;
    TextView tv_current_speed, tv_speed_threshold;
    Button btn_plus, btn_minus;
    private float speed_threshold;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_long_distance);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
        tv_current_speed = (TextView) findViewById(R.id.textView_current_speed);

        speed_threshold = /*Load a value from database*/ 8.0f;

        btn_minus = (Button) findViewById(R.id.button_minus);
        btn_plus = (Button) findViewById(R.id.button_plus);

        btn_minus.setOnClickListener(this);
        btn_plus.setOnClickListener(this);

        tv_speed_threshold = (TextView) findViewById(R.id.textView_speed_threshold);
        tv_speed_threshold.addTextChangedListener(this);
        updateThresholdText();


    }

    @Override
    public void onLocationChanged(Location location) {
        location.getLatitude();
        tv_current_speed.setText("Current speed: " + location.getSpeed() + "GPS accuracy" + location.getAccuracy());
        if(location.getSpeed() < speed_threshold){
            tv_current_speed.setText("Current speed: " + location.getSpeed() + "GPS accuracy" + location.getAccuracy() + "TOO SLOW");
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.button_minus){
            if(speed_threshold > 0.1){
                speed_threshold -= 0.1;
            }
            else{
                speed_threshold = 0;
            }
            updateThresholdText();
        }
        else if(v.getId() == R.id.button_plus){
            speed_threshold += 0.1;
            updateThresholdText();
        }
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
            speed_threshold = 0;
        }
        else{
            speed_threshold = Float.parseFloat(s.toString());
        }
        //updateThresholdText();
    }

    public void updateThresholdText(){
        tv_speed_threshold.setText(String.format("%.1f", speed_threshold));
        //tv_speed_threshold.setText(speed_threshold + "");
    }
}
