package christensenjohnsrud.funfit;

import android.app.Activity;
import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;

public class LongDistanceTabHolder extends TabActivity {

    TabHost tabHost;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_long_distance_tab_holder);

        tabHost = getTabHost();

        Intent intentTrack = new Intent().setClass(this, LongDistance.class);
        TabHost.TabSpec trackTab = tabHost
                .newTabSpec("Track")
                .setIndicator("Track")
                .setContent(intentTrack);

        Intent intentData = new Intent().setClass(this, LongDistanceData.class);
        TabHost.TabSpec dataTab = tabHost
                .newTabSpec("Data")
                .setIndicator("Data")
                .setContent(intentData);

        tabHost.addTab(trackTab);
        tabHost.addTab(dataTab);

    }
}
