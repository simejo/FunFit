package christensenjohnsrud.funfit;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.util.Log;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by siljechristensen on 06/03/16.
 */
public class RandomWorkout extends Activity implements SensorEventListener {


    private String className = "RandomWorkout";
    private static int SHAKE_THRESHOLD = 16;

    // NB! Remember to update both type and types
    private String[] core = {"Planken i 1 min","Saks i 1 min","60 sit ups"};
    private String[] booty = {"20 Lunges","20 Squat"};
    private String[] arms = {"Weight","Lift"};
    private String[] back = {"rygghev","mark"};
    private String[][] types = {core,booty, arms, back}; // TYPES
    private String[] np_types = {"core","booty", "arms", "back"};

    private int chosenType;


    private NumberPicker type;
    private TextView exercise_tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_random_workout);


        chosenType = 1;

        type = (NumberPicker) findViewById(R.id.type_picker);
        type.setMinValue(0); //from array first value
        //Specify the maximum value/number of NumberPicker
        type.setMaxValue(types.length - 1); //to array last value
        type.setWrapSelectorWheel(true);
        type.setDisplayedValues(np_types);


        exercise_tv = (TextView) findViewById(R.id.text_view_exercise);

        type.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                // TODO Auto-generated method stub
                Log.i("main", newVal +"");
                switch (newVal) {
                    case 1: exercise_tv.setText(getRandomWorkout(core));
                        chosenType = 1;
                        break;
                    case 2: exercise_tv.setText(getRandomWorkout(booty));
                        chosenType = 2;
                        break;
                    case 3: exercise_tv.setText(getRandomWorkout(arms));
                        chosenType = 3;
                        break;
                    case 4: exercise_tv.setText(getRandomWorkout(back));
                        chosenType = 4;
                        break;
                }
            }
        });

    }

    private String getRandomWorkout(String[] type){
        Random generator = new Random();
        int i = generator.nextInt(type.length);
        return type[i];
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        // Axis of the rotation sample, not normalized yet.
        float axisX = event.values[0];
        float axisY = event.values[1];
        float axisZ = event.values[2];
        if (axisX >= SHAKE_THRESHOLD || axisY >= SHAKE_THRESHOLD || axisZ >= SHAKE_THRESHOLD){
            getRandomWorkout(types[chosenType]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
