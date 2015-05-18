package com.jsvgoncalves.speedlimitadas;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.Image;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.jsvgoncalves.speedlimitadas.utils.JSONHelper;
import com.jsvgoncalves.speedlimitadas.utils.NetworkUtils;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;


public class MainStatusActivity extends ActionBarActivity {

    private String currentSpeedLimit = "";
    private CommunicationTask comm;

    /**
     * Broadcast receiver for connectivity status update.
     */
    BroadcastReceiver networkStateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            Log.w("adas", "Network Type Changed");
            updateNetworkStatus();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_status);

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkStateReceiver, filter);

        // Start the socket communication
        comm = new CommunicationTask();
        comm.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_status, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Sends the user back to the launcher homescreen
     * without closing the app.
     */
    @Override
    public void onBackPressed() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }

    public void updateNetworkStatus() {
        // Get the network status
        boolean networkStatus = NetworkUtils.checkNetwork(getApplicationContext());
        // Update TextViews
        TextView tNetStatus = (TextView) findViewById(R.id.tv_networkStatus2);
        if(networkStatus) {
            tNetStatus.setText("Connected");
            tNetStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tNetStatus.setText("Not Connected");
            tNetStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        unregisterReceiver(networkStateReceiver);
        comm.onPostExecute("onDestroy");
    }

    /**
     * Called by the network async task when a comm is received.
     * @param str The JSON string with all the data.
     */
    public void positionUpdated(String str) {
        // Create the JSON object and parse the string.
        JSONObject json = JSONHelper.string2JSON(str);
        Log.w("adas", "You I got it");
        Log.w("adas", str);
        // Get values from JSON
        String speedLimit = JSONHelper.getValue(json, "speedLimit");
        String avgLaneSpeed = JSONHelper.getValue(json, "avgLaneSpeed");
        String currentSpeed = JSONHelper.getValue(json, "currentSpeed");
        Log.w("adas", "It is : " + speedLimit);
        Log.w("adas", "It is : " + avgLaneSpeed);
        Log.w("adas", "It is : " + currentSpeed);

        if(!speedLimit.equals(currentSpeedLimit)) {
            updateSpeedLimit(speedLimit);
        }

        TextView tAvgLaneSpeed = (TextView) findViewById(R.id.tv_avglanespeed);
        tAvgLaneSpeed.setText(avgLaneSpeed + " km/h");
        Log.w("adas", "Updated avg lane speed to " + avgLaneSpeed);
        TextView tCurrentSpeed = (TextView) findViewById(R.id.tv_currentSpeed);
        tCurrentSpeed.setText(currentSpeed + " km/h");

        // TODO: Set the other parameters such as speed limit mipmap.
        // setSpeedLimit(speedLimit);
    }

    private void updateSpeedLimit(String speedLimit) {
        TextView tSpeedLimit = (TextView) findViewById(R.id.tv_currentSpeedLimit);
        tSpeedLimit.setText(speedLimit + " km/h");

        ImageView iSpeedLimit = (ImageView) findViewById(R.id.iv_speedLimit);
        if(speedLimit.equals("50")) {
            iSpeedLimit.setImageResource(R.mipmap.speed50);
        } else if(speedLimit.equals("90")) {
            iSpeedLimit.setImageResource(R.mipmap.speed90);
        } else if(speedLimit.equals("100")) {
            iSpeedLimit.setImageResource(R.mipmap.speed100);
        } else if(speedLimit.equals("120")) {
            iSpeedLimit.setImageResource(R.mipmap.speed120);
        } else {
            iSpeedLimit.setImageResource(R.mipmap.speed50);
        }

    }

    /**
     * AssyncTask that handles the socket communication.
     * Calls positionUpdated() when a new string of data is received.
     * @author Joao
     *
     */
    class CommunicationTask extends AsyncTask<String, String, String> {
        ServerSocket socket;

        /**
         * Main loop.
         */
        protected String doInBackground(String... urls) {
            try {
                socket = new ServerSocket(5173);
                Socket clientSocket = socket.accept();
                //PrintWriter out =
                //        new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    publishProgress(inputLine);
                    Log.w("adas", inputLine);
                    // out.println("#Received: " + inputLine + " - Nexus 7");
                }
                return "na";
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * Used to update the position instead of the progress.
         */
        protected void onProgressUpdate(String... msg) {
            Log.w("adas", "Sending Position update to the app");
            positionUpdated(msg[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            try {
                socket.close();
                resetCommunication();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void resetCommunication() {
        Log.w("adas", "I should be resetting the communication");
        new CommunicationTask().execute();
    }
}
