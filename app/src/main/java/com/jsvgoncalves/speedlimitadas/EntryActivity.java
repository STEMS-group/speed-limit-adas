package com.jsvgoncalves.speedlimitadas;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.jsvgoncalves.speedlimitadas.utils.NetworkUtils;


public class EntryActivity extends ActionBarActivity {

    /**
     * Broadcast receiver for connectivity status update.
     */
    BroadcastReceiver networkStateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            Log.w("adas", "Network Type Changed");
            updateStatusBar();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);

        // Check things and then start main
        updateStatusBar();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkStateReceiver, filter);
    }

    /**
     * Check the network status and updates the sidebar.
     * @return boolean with the network status
     */
    private boolean updateStatusBar() {

        // Get the network status
        boolean networkStatus = NetworkUtils.checkNetwork(getApplicationContext());

        // Update TextViews
        TextView tNetStatus = (TextView) findViewById(R.id.networkStatusTextView);
        TextView tIP = (TextView) findViewById(R.id.ipTextView);
        if(networkStatus) {
            tIP.setText(NetworkUtils.getIPAddress(true));
            tNetStatus.setText("Connected");
            tNetStatus.setTextColor(getResources().getColor(R.color.opaque_green));
        } else {
            tIP.setText(R.string.tmpip);
            tNetStatus.setText("Not Connected");
            tNetStatus.setTextColor(getResources().getColor(R.color.opaque_red));
        }

        return networkStatus;
    }

    public void startClicked(View view) {
        // Kabloey
        if(updateStatusBar()) {
            startMainActivity();
        } else {
            Toast.makeText(this, getString(R.string.nonetwork), Toast.LENGTH_LONG).show();
        }
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainStatusActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_entry, menu);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        unregisterReceiver(networkStateReceiver);
    }
}
