package christensenjohnsrud.funfit;

import android.content.Context;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.jjoe64.graphview.series.DataPoint;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Created by simen on 3/6/16.
 */
public class Database {

    public Database(){
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public void writeToSDFile(String filename, DataPoint[] data){

        // Find the root of the external storage.
        // See http://developer.android.com/guide/topics/data/data-  storage.html#filesExternal

        File dir = new File (Environment.getExternalStorageDirectory().getAbsolutePath() + "/funfit");
        dir.mkdirs();
        File file = new File(dir, filename);

        try {
            FileOutputStream f = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(f);

            pw.println("new" + data.length);
            for(DataPoint dp : data){
                pw.println(dp.getX() + ":" + dp.getY());
            }
            pw.flush();
            pw.close();
            f.close();
            Log.d("Database", "Data saved");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<String> readFileKeys(String filename){
        final File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/funfit", filename);

        ArrayList<String> result = new ArrayList<String>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            int counter = 1;
            String[] holder = br.readLine().split("new");
            result.add(holder[0] + counter);
            while ((line = br.readLine()) != null) {
                if(line.startsWith("new")){
                    holder = line.split("new");
                    counter++;
                    result.add(holder[0] + counter);
                }
            }
            br.close();
            Log.d("Database", "Data read");
            return result;
        }
        catch (IOException e) {
            //You'll need to add proper error handling here
        }
        return null;
    }


    public ArrayList<DataPoint[]> readFile(String filename){
        final File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/funfit", filename);

        ArrayList<DataPoint[]> result = new ArrayList<DataPoint[]>();
        DataPoint[] data;

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            String[] holder = br.readLine().split("new");
            data = new DataPoint[Integer.parseInt(holder[1])];
            int counter = 0;
            while ((line = br.readLine()) != null) {
                if(line.startsWith("new")){
                    result.add(data);
                    holder = line.split("new");
                    data = new DataPoint[Integer.parseInt(holder[1])];
                }
                holder = line.split(":");
                data[counter] = new DataPoint(Double.parseDouble(holder[0]),Double.parseDouble(holder[1]));
                counter++;
            }
            result.add(data);
            br.close();
            Log.d("Database", "Data read");
            return result;
        }
        catch (IOException e) {
            //You'll need to add proper error handling here
        }
        return null;
    }
}
