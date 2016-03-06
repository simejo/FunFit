package christensenjohnsrud.funfit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;

public class LongDistanceData extends Activity implements AdapterView.OnItemClickListener {

    private ListView lv_data;
    private ArrayAdapterLongDistance adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_long_distance_data);

        lv_data = (ListView) findViewById(R.id.listViewLongDistanceData);
        lv_data.setOnItemClickListener(this);
        adapter = new ArrayAdapterLongDistance(this, R.layout.list_view_row_item, LongDistance.resultKeys);
        lv_data.setAdapter(adapter);
    }

    public void buildGraphAlert(DataPoint[] result){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Graph").setNegativeButton("Close", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });
        // Create the AlertDialog object and return it
        builder.setView(buildGraphView(result));
        builder.create();
        builder.show();
    }

    public GraphView buildGraphView(DataPoint[] result){
        GraphView graph = new GraphView(this);
        LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(result);
        graph.addSeries(series);
        return graph;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d("Position", position + "");
        buildGraphAlert(LongDistance.resultList.get(position));
    }
}
