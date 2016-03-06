package christensenjohnsrud.funfit;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;

import java.util.ArrayList;

public class LongDistanceData extends Activity {

    private ListView lv_data;
    private ArrayAdapterLongDistance adapter;
    public static ArrayList<Float> results;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_long_distance_data);

        results = new ArrayList<Float>();
        results.add(2.2f);
        results.add(2.5f);
        lv_data = (ListView) findViewById(R.id.listViewLongDistanceData);
        adapter = new ArrayAdapterLongDistance(this, R.layout.list_view_row_item, results);
        lv_data.setAdapter(adapter);
    }
}
