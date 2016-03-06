package christensenjohnsrud.funfit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity implements View.OnClickListener{

    private Button btn_interval, btn_long_distance, btn_height_measurement, btn_random_workout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_interval = (Button) findViewById(R.id.button_interval);
        btn_long_distance = (Button) findViewById(R.id.button_long_distance);
        btn_height_measurement = (Button) findViewById(R.id.button_height_measurement);
        btn_random_workout = (Button) findViewById(R.id.button_random);

        btn_interval.setOnClickListener(this);
        btn_long_distance.setOnClickListener(this);
        btn_height_measurement.setOnClickListener(this);
        btn_random_workout.setOnClickListener(this);


    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.button_interval){
            startActivity(new Intent(this, IntervalA.class));

        }
        else if(v.getId() == R.id.button_long_distance){
            startActivity(new Intent(this, LongDistanceTabHolder.class));
        }
        else if(v.getId() == R.id.button_height_measurement){
            startActivity(new Intent(this, HeightMeasurement.class));
        }
        else if(v.getId() == R.id.button_random){
            startActivity(new Intent(this, RandomWorkout.class));
        }
    }
}