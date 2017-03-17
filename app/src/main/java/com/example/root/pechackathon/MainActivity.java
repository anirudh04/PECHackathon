package com.example.root.pechackathon;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.root.pechackathon.adapter.AppController;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SensorEventListener {


    private String TAG="MainActivity";
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 60;
    String url="http://172.31.68.109/api/v1/location";

    private double Sx,Sy,Sz,Ux=0,Uy=0,Uz=0,Vx,Vy,Vz;
    private String lat,lon;
    private double latitude,longitude;

    private double dLat,dLon,latO,lonO;



    private TextView x_direction,y_direction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        x_direction=(TextView) findViewById(R.id.textView1);
        y_direction=(TextView) findViewById(R.id.textView2);

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        //GettingBundle data from google sign in
        if(getIntent().getExtras()!=null) {
            Bundle args = getIntent().getExtras();
            lat= args.getString("lat");
            lon = args.getString("lon");

        }


        latitude= Double.parseDouble(lat);

        longitude=Double.parseDouble(lon);


    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        Sensor mySensor = sensorEvent.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;



                float speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;

                if (speed > SHAKE_THRESHOLD) {


                    Ux=Vx;
                    Uy=Vy;
                    Uz=Vz;

                    Sx=(double) (Ux*(diffTime/1000)+(0.5)*x*((double)Math.pow((diffTime/1000),2)));
                    Sy=(double) (Uy*(diffTime/1000)+(0.5)*y*((double)Math.pow((diffTime/1000),2)));
                    Sz=(double) (Uz*(diffTime/1000)+(0.5)*z*((double) Math.pow((diffTime/1000),2)));

                    Vx=Ux+x*diffTime;
                    Vy=Uy+y*diffTime;
                    Vz=Uy+z*diffTime;

                    x_direction.setText(Double.toString(Sx));
                    y_direction.setText(Double.toString(Sy));



                    dLat=(double)Sx/6378137;
                    dLon=(double)Sy/(6378137*Math.cos(Math.PI*(latitude/180)));

                    latO=(double)latitude+(dLat*(180/Math.PI));
                    lonO=(double)longitude+(dLon*(180/Math.PI));

                    Log.e(TAG, "x=" + Sx);
                    Log.e(TAG, "y=" + Sy);


                    Map<String, String> params = new HashMap<String, String>();
                    params.put("id","1");
                    params.put("x", Double.toString(latO));
                    params.put("y", Double.toString(lonO));

                    sendData(params);
                }

                last_x = x;
                last_y = y;
                last_z = z;
            }
        }
    }

    private void sendData(Map<String, String> params) {


        JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, new JSONObject(params),
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {


                        Log.d("Response: Description ", response.toString());

                        try {
                            String status = response.getString("status");
                            String message = response.getString("message");

                            Log.d(TAG, "onResponse: " + message);
                            if (status.equals("success")) {

                                Toast.makeText(MainActivity.this, "Location sent successfully", Toast.LENGTH_SHORT).show();

                            }
                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "Connection failed :/", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error

                        Log.d("Error.Response", error.toString());
                        Toast.makeText(MainActivity.this, "Connection failed :/", Toast.LENGTH_SHORT).show();
                    }
                });


        request.setRetryPolicy(new DefaultRetryPolicy(10000, 3, 2));
        AppController.getInstance().addToRequestQueue(request);



    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    protected void onPause() {
        super.onPause();
        senSensorManager.unregisterListener(this);
    }

    protected void onResume() {
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }


    }



