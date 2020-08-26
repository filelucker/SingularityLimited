package com.example.singularitylimited;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.example.singularitylimited.adapter.CustomAdapter;
import com.example.singularitylimited.model.DataModel;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<DataModel> dataModels;
    ListView listView;
    private static CustomAdapter adapter;
    ProgressDialog progressDialog;
    private static final int REQUEST_LOCATION = 1;
    LocationManager locationManager;
    String stringName, stringUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            OnGPS();
        } else {
            getLocation();
        }
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("please wait.");
        progressDialog.setCancelable(false);
        listView = (ListView) findViewById(R.id.list);


        dataModels = new ArrayList<>();

        adapter = new CustomAdapter(dataModels, getApplicationContext());

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                DataModel dataModel = dataModels.get(position);

                final Dialog dialog = new Dialog(MainActivity.this);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.dialog);
                final EditText editTextName = dialog.findViewById(R.id.name);
                final EditText editTextUId = dialog.findViewById(R.id.id);

                Button dialogBtn = (Button) dialog.findViewById(R.id.button);
                dialogBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (editTextName.getText().toString().trim().equalsIgnoreCase("") || editTextUId.getText().toString().trim().equalsIgnoreCase("")) {
                            Toast.makeText(MainActivity.this, "Must enter all field", Toast.LENGTH_SHORT).show();
                        } else {
                            stringName = editTextName.getText().toString().trim();
                            stringUserId = editTextUId.getText().toString().trim();
                            SubmitAttendance submitAttendance = new SubmitAttendance();
                            submitAttendance.execute();
                        }
                        dialog.dismiss();
                    }
                });

                dialog.show();

            }
        });

        GetStoreAsync getStoreAsync = new GetStoreAsync();
        getStoreAsync.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        //if (id == R.id.action_settings) {
        //    return true;
        //}true

        return super.onOptionsItemSelected(item);
    }

    public class GetStoreAsync extends AsyncTask {

        OkHttpClient client = new OkHttpClient();
        Response response = null;

        @Override
        protected void onPreExecute() {
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(Object o) {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            if (response.code() == 200) {
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
            }
        }


        @Override
        protected Object doInBackground(Object[] objects) {


            Request request = new Request.Builder()
                    .url("http://128.199.215.102:4040/api/stores")
                    .method("GET", null)
                    .build();
            try {
                response = client.newCall(request).execute();
                if (response.code() == 200) {
                    // get JSONObject from JSON file
                    JSONObject obj = new JSONObject(response.body().string());
                    // fetch JSONArray named data
                    JSONArray dataArray = obj.getJSONArray("data");
                    // implement for loop for getting data list data
                    for (int i = 0; i < dataArray.length(); i++) {
                        // create a JSONObject for fetching single user data
                        JSONObject storeDetail = dataArray.getJSONObject(i);
                        // fetch email and name and store it in arraylist
                        String name = storeDetail.getString("name");
                        String address = storeDetail.getString("address");
                        dataModels.add(new DataModel(name, address));
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private String getLocation() {
        double lat = 0.0, longi = 0.0;
        if (ActivityCompat.checkSelfPermission(
                MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (locationGPS != null) {
                lat = locationGPS.getLatitude();
                longi = locationGPS.getLongitude();
            } else {
                Toast.makeText(this, "Unable to find location.", Toast.LENGTH_SHORT).show();
            }
        }
        return String.valueOf(lat) + "&" + String.valueOf(longi);
    }

    private void OnGPS() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Enable GPS").setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Generate a random string.
     */
    static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    static SecureRandom rnd = new SecureRandom();

    String randomString(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++)
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        return sb.toString();
    }

    public class SubmitAttendance extends AsyncTask {

        OkHttpClient client = new OkHttpClient();
        Response response = null;

        @Override
        protected void onPreExecute() {
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(Object o) {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            if (response.code() == 200) {
                try {
                    JSONObject obj = new JSONObject(response.body().string());
                    if (obj.getString("app_message").equalsIgnoreCase("Success")) {
                        Toast.makeText(MainActivity.this, "Attendance sent successful", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Toast.makeText(MainActivity.this, "Attendance sent successful", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
            }
        }


        @Override
        protected Object doInBackground(Object[] objects) {

            try {
                String location = getLocation();
                String[] sendlocation = location.split("&");
                MediaType mediaType = MediaType.parse("text/plain");
                RequestBody body = RequestBody.create(mediaType, "");
                Request request = new Request.Builder()
                        .url("http://128.199.215.102:4040/api/attendance?name=" + stringName + "&uid=" + stringUserId + "&latitude=" + sendlocation[0] + "&longitude=" + sendlocation[1] + "&request_id=" + randomString(5))
                        .method("POST", body)
                        .build();
                response = client.newCall(request).execute();

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}