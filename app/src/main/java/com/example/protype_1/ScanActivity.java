package com.example.protype_1;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.protype_1.old.ConnectActivity;

import java.util.ArrayList;


@TargetApi(23)
public class ScanActivity extends AppCompatActivity{//AppCompatActivity {
    Button b1,b2,b3,b4;
    private BluetoothAdapter BA;
    private ArrayList<BluetoothDevice>pairedDevices;
    ListView lv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        initViews();


    }

    /**
     * Initializes all elements on the paired devices page
     */
    private void initViews(){
        BA = BluetoothAdapter.getDefaultAdapter();

        pairedDevices = new ArrayList<BluetoothDevice>();
        lv = (ListView)findViewById(R.id.list);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int itemPosition = position;
                BluetoothDevice dev = pairedDevices.get(position);
                Toast.makeText(getApplicationContext(), "Connecting to "+ dev.getAddress(),Toast.LENGTH_LONG).show();
                startConnect(itemPosition);
            }
        });
        list();

    }

    /**
     *
     * @param menu
     * @return
     *
     * Creates the menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scan_menu, menu);
        //menu.findItem(R.id.menu_stop).setVisible(true);
        menu.findItem(R.id.menu_refresh).setVisible(true);
        //menu.findItem(R.id.menu_refresh).setActionView(
        //R.layout.actionbar_indeterminate_progress);
        return true;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                pairedDevices.clear();
                list();
                break;
            default:
                finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     *
     * @param position
     *
     * Go to next activity (Analysis Page) and sends the MAC address of
     * the selected paired device
     */
    protected void startConnect(int position) {
        final BluetoothDevice device = pairedDevices.get(position);
        if (device == null) return;
        final Intent intent = new Intent(this, ConnectActivity.class);
        intent.putExtra(ConnectActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(ConnectActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        startActivity(intent);
    }


    /**
     * Display all bluetooth paired devices
     */
    public void list(){
        pairedDevices.addAll(BA.getBondedDevices());
        ArrayList list = new ArrayList();
        for(BluetoothDevice bt : pairedDevices) list.add(bt.getName());
        Toast.makeText(getApplicationContext(), "Showing Paired Devices",Toast.LENGTH_SHORT).show();
        final ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1, list);
        lv.setAdapter(adapter);
    }

}
