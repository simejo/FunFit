package christensenjohnsrud.funfit;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private Button btn_interval, btn_long_distance, btn_height_measurement, btn_height_measurement_camera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_interval = (Button) findViewById(R.id.button_interval);
        btn_long_distance = (Button) findViewById(R.id.button_long_distance);
        btn_height_measurement = (Button) findViewById(R.id.button_height_measurement);
        btn_height_measurement_camera = (Button) findViewById(R.id.button_height_measurement_camera);

        btn_interval.setOnClickListener(this);
        btn_long_distance.setOnClickListener(this);
        btn_height_measurement.setOnClickListener(this);
        btn_height_measurement_camera.setOnClickListener(this);


    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.button_interval){
            startActivity(new Intent(this, IntervalA.class));

        }
        else if(v.getId() == R.id.button_long_distance){
            startActivity(new Intent(this, LongDistance.class));
        }
        else if(v.getId() == R.id.button_height_measurement){
            startActivity(new Intent(this, HeightMeasurement.class));
        }
        else if(v.getId() == R.id.button_height_measurement_camera){
            startActivity(new Intent(this, HeightMeasurementCamera.class));
        }
    }
}