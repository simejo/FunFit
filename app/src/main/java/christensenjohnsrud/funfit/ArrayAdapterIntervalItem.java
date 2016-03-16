package christensenjohnsrud.funfit;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by siljechristensen on 11/02/16.
 */
public class ArrayAdapterIntervalItem extends ArrayAdapter<IntervalItem>{
    Context mContext;
    int layoutResourceId;
    ArrayList<IntervalItem> data = null;

    public ArrayAdapterIntervalItem(Context mContext, int layoutResourceId, ArrayList<IntervalItem> data) {

        super(mContext, layoutResourceId, data);

        this.layoutResourceId = layoutResourceId;
        this.mContext = mContext;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView==null){
            // inflate the layout
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            convertView = inflater.inflate(layoutResourceId, parent, false);
        }

        // object item based on the position
        IntervalItem intervalItem = data.get(position);

        // get the TextView and then set the text (item name) and tag (item ID) values
        TextView textViewItem = (TextView) convertView.findViewById(R.id.textViewItem);
        textViewItem.setText(intervalItem.toString());
        textViewItem.setTag(intervalItem.itemId);

        return convertView;

    }

}
