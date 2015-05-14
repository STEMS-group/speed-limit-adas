package com.jsvgoncalves.speedlimitadas;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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
        new CommunicationTask().execute();
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
    }

    /**
     * Called by the network async task when a comm is received.
     * @param str The JSON string with all the data.
     */
    public void positionUpdated(String str) {
        // Create the JSON object and parse the string.
        JSONObject json = JSONHelper.string2JSON(str);
        TextView tSpeedLimit = (TextView) findViewById(R.id.tv_currentSpeedLimit);
        tSpeedLimit.setText(str + " km/h");
        // Get latitude and longitude values.
        // String lat = JSONHelper.getValue(json, "latitude");
        // String lon = JSONHelper.getValue(json, "longitude");

        // Set the new device location.
        // setMockLocation(lat, lon);

        // TODO: Set the other parameters such as speed limit.
        // setSpeedLimit(speedLimit);
    }

    /**
     * AssyncTask that handles the socket communication.
     * Calls positionUpdated() when a new string of data is received.
     * @author João
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
                PrintWriter out =
                        new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    publishProgress(inputLine);
                    Log.w("adas", inputLine);
                    out.println("#Received: " + inputLine + " - Nexus 7");
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
        Log.v("adas", "I should be resetting the communication");
    }
}
