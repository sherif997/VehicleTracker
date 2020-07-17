package com.example.sherif.testproject1;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v4.app.NotificationCompat;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private FirebaseAuth mAuth;
    private GoogleMap mMap;
    Button menuButton;
    Button historyButton;
    TextView infoView;
   // ArrayList<String> vehicleText = new ArrayList<String>();
    SharedPreferences sharedPrefs;
    ImageButton refreshButton;
    private LocationManager manager;
    SupportMapFragment mapFragment;
   // private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mAuth = FirebaseAuth.getInstance();
       // String longitude = "", latitude = " ";
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        //userDatePreference(dateSelection);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        menuActions();
        refreshAction();
        viewHistory();
        manageRefresh();


        }



    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker in London, UK.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Intent authenticate = new Intent(MapsActivity.this, authActivity.class);
            //Log.d(TAG, "onAuthStateChanged:signed_out");
            startActivity(authenticate);
            finish();
        }


    }

    /*@Override
    public void onBackPressed() {    //ONLY USED FOR TESTING AUTHENTICATION ON MOBILE DEVICE
        super.onBackPressed();
        FirebaseAuth.getInstance().signOut();
    }*/

    @Override
    public void onStop(){
        super.onStop();//Push notifications displaying location even when user exits application
        String userSelection=sharedPrefs.getString("notification_frequency","0");
        System.out.println("NOTIFICATION string: !!!!"+ userSelection);
        try {
            int notificationRate = Integer.parseInt(userSelection);
                System.out.println("NOTIFICATION RATE: !!!!" + notificationRate);
                final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
                ses.scheduleWithFixedDelay(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("notifying!!!");
                        pushNotifications();
                    }
                }, 0, notificationRate, TimeUnit.SECONDS);

        }
        catch(Exception e){
            //do nothing
        }
    }



    public void pushNotifications(){
        Boolean sound = sharedPrefs.getBoolean("notification_sound", false);
        Boolean vibrate=sharedPrefs.getBoolean("notification_vibrate",false);
        Intent intent = new Intent(this, MapsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);


        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.applaunch)
                .setContentTitle("Vehicle location")
                .setContentText("Vehicle found at: "+getVehicleAddress()+". Tap for more information")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setStyle(new NotificationCompat.BigTextStyle());


        if(sound)
            mBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        if(vibrate)
            mBuilder.setVibrate(new long[] { 1000, 1000});

        mBuilder.setAutoCancel(true);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        notificationManager.notify(1, mBuilder.build());//Takes unique number id and the notification builder along with chosen settings







    }

    //NOTE: FOR ALL THESE SMS MESSAGES, REPLACE WITH ARDUINO PHONE NUMBER
    public ArrayList<String[]> readSMS() {
        /*This method aims to obtain longitude and latitude information from text
    https://stackoverflow.com/questions/10870230/read-all-sms-from-a-particular-sender*/
        String[] locationData = new String[2];
        ArrayList<String[]> locations = new ArrayList<String[]>();
        //editText4 = (EditText) findViewById(R.id.editText4);
        infoView = (TextView) findViewById(R.id.infoView);
        StringBuilder smsBuilder = new StringBuilder();
        final String SMS_URI_INBOX = "content://sms/inbox";
        final String SMS_URI_ALL = "content://sms/";
        if (ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED) {
            try {
                Uri uri = Uri.parse(SMS_URI_INBOX);
                String[] projection = new String[]{"_id", "address", "person", "body", "date", "type"};
                Cursor cur = getContentResolver().query(uri, projection, "address='+447387835492'", null, "date desc");
                String newestSMS = "";
                String latitude = "", longitude = "";
                String latitudeString = "", longitudeString = "";
                if (cur.moveToFirst()) {
                    int index_Address = cur.getColumnIndex("address");
                    int index_Person = cur.getColumnIndex("person");
                    int index_Body = cur.getColumnIndex("body");
                    int index_Date = cur.getColumnIndex("date");
                    int index_Type = cur.getColumnIndex("type");
                    do {
                        String strAddress = cur.getString(index_Address);
                        int intPerson = cur.getInt(index_Person);
                        String strbody = cur.getString(index_Body);
                        long longDate = cur.getLong(index_Date);
                        int int_Type = cur.getInt(index_Type);
                        String[] smsSplit = strbody.split("\\r?\\n");
                        System.out.println(smsSplit[0]+"!!!"+smsSplit[1]);
                        latitudeString = smsSplit[1];
                        longitudeString = smsSplit[2];

                        String[] latArray = latitudeString.split(":");
                        String[] longitArray = longitudeString.split(":");
                        smsBuilder.append("[ ");
                        latitude = latArray[1].replaceAll("\\s+", "");
                        longitude = longitArray[1].replaceAll("\\s+", "");
                        System.out.println("!!!" + longitude + "    " + latitude + "!!!!");
                        locationData[0] = longitude;
                        locationData[1] = latitude;
                        locations.add(locationData);
                        smsBuilder.append(latitude);
                        smsBuilder.append("|" + longitude);
                        smsBuilder.append(" " + longDate + ", ");

                        smsBuilder.append(" ]\n\n");
                        newestSMS = smsBuilder.toString();
                        break;
                    }
                    while (cur.moveToNext());

                    if (!cur.isClosed()) {
                        cur.close();
                        cur = null;
                    }
                } else {
                    smsBuilder.append("no result!");
                } // end if
                //editText4.setText(smsBuilder.toString().replaceAll("[^\\d.]", ""));
                //editText4.setText(newestSMS);
                System.out.println("SMS: " + smsBuilder.toString());
            } catch (SQLiteException ex) {
                Log.d("SQLiteException", ex.getMessage());
            }
        }
        return locations;
    }
    public String getVehicleAddress(){
        ArrayList<String[]>carLocations=readSMS();
        double carLat = Double.parseDouble(carLocations.get(0)[1]);
        double carLongit = Double.parseDouble(carLocations.get(0)[0]);
        Geocoder geo = new Geocoder(getApplicationContext());
        String carResult="";
        try {
            List<Address> carAddresses = geo.getFromLocation(carLat, carLongit, 1);
            carResult = carAddresses.get(0).getAddressLine(0) + " " + carAddresses.get(0).getLocality() + " " + carAddresses.get(0).getCountryCode();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return carResult;
    }

    public String getTime(){

        String dateString="";
        final String SMS_URI_INBOX = "content://sms/inbox";
        final String SMS_URI_ALL = "content://sms/";
        if (ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED) {
            try {
                Uri uri = Uri.parse(SMS_URI_INBOX);
                String[] projection = new String[]{"_id", "address", "person", "body", "date", "type"};
                Cursor cur = getContentResolver().query(uri, projection, "address='+447387835492'", null, "date desc");
                String newestSMS = "";
                if (cur.moveToFirst()) {
                    int index_Address = cur.getColumnIndex("address");
                    int index_Person = cur.getColumnIndex("person");
                    int index_Body = cur.getColumnIndex("body");
                    int index_Date = cur.getColumnIndex("date");
                    int index_Type = cur.getColumnIndex("type");
                    do {
                        String strAddress = cur.getString(index_Address);
                        int intPerson = cur.getInt(index_Person);
                        String strbody = cur.getString(index_Body);
                        long longDate = cur.getLong(index_Date);
                        int int_Type = cur.getInt(index_Type);
                        Date date = new Date(longDate);
                        SimpleDateFormat locatedDate = new SimpleDateFormat(userDatePreference()+userTimePreference());
                        //userTimePreference(userSelection);
                        dateString="Located on: "+locatedDate.format(date);

                        break;
                    }
                    while (cur.moveToNext());

                    if (!cur.isClosed()) {
                        cur.close();
                        cur = null;
                    }
                } else {
                    //Do nothing
                } // end if
            } catch (SQLiteException ex) {
                Log.d("SQLiteException", ex.getMessage());
            }
        }
        return dateString;
    }


    public String userDatePreference(){
        //Synchronize preferences with main activity
        String dateSelection=sharedPrefs.getString("date_config","0");
      //  String timeSelection=sharedPrefs.getString("time_congfig","");
        String dateFormat="EEEE, MMMM dd, yyyy ";
        System.out.println("FORMATTING DATE: "+dateSelection+" !!!!");
        if (dateSelection.equals("noWeekday")){
            System.out.println("no weekday!!!!");
            dateFormat="MMMM dd, yyyy ";
        }
        else if(dateSelection.equals("shorthand")){
            //System.out.println("shorthand!!!!");
            dateFormat="dd.MM.yy ";
            return dateFormat;
        }

        return dateFormat;
    }
    public String userTimePreference(){
        String timeSelection=sharedPrefs.getString("time_config","0");
        System.out.println("FORMATTING TIME: "+timeSelection+" !!!!");
        String timeFormat="HH:mm:ss";
        if(timeSelection.equals("12")){
            System.out.println("12 hour!!!!");
            timeFormat="hh:mm:ss aa";
            return timeFormat;
        }
        else{
            return timeFormat;
        }


    }


    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        final ArrayList<String[]> carLocations = readSMS();
        for (int i = 0; i < carLocations.size(); i++)
            System.out.println("CAR LOCATION: " + carLocations.get(i)[1] + "   " + carLocations.get(i)[0]);
        // Add a marker in London and move the camera
        manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        if (manager.isProviderEnabled(manager.NETWORK_PROVIDER)) {
            manager.requestLocationUpdates(manager.NETWORK_PROVIDER, 1, 5, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    double lat = location.getLatitude();
                    double longit = location.getLongitude();
                    double carLat = Double.parseDouble(carLocations.get(0)[1]);
                    double carLongit = Double.parseDouble(carLocations.get(0)[0]);
                    Location carLocation = new Location(carLocations.get(0)[1] + "," + carLocations.get(0)[0]);
                    LatLng loc = new LatLng(lat, longit);
                    LatLng carLoc = new LatLng(carLat, carLongit);
                    Geocoder geo = new Geocoder(getApplicationContext());
                    MarkerOptions marker = new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.male));
                    MarkerOptions carMarker = new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.car));

                    try {
                        List<Address> addresses = geo.getFromLocation(lat, longit, 1);
                        //List<Address> carAddresses = geo.getFromLocation(carLat, carLongit, 1);
                        String userSelection = sharedPrefs.getString("marker_information", "0");
                        String result = addresses.get(0).getAddressLine(0) + " " + addresses.get(0).getLocality() + " " + addresses.get(0).getCountryCode();
                        //String carResult = carAddresses.get(0).getAddressLine(0) + " " + carAddresses.get(0).getLocality() + " " + carAddresses.get(0).getCountryCode();
                        infoView.setMovementMethod(new ScrollingMovementMethod());
                        infoView.setText("Vehicle location: "+getVehicleAddress());

                        mMap.addMarker(marker.position(loc).title(result));
                        if(userSelection.equals("km")||userSelection.equals("miles"))
                            mMap.addMarker(carMarker.position(carLoc).title("Distance to vehicle: "+vehicleDistance(loc,carLoc)));
                        else
                            mMap.addMarker(carMarker.position(carLoc).title(getTime()));
                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        builder.include(marker.getPosition());
                        builder.include(carMarker.getPosition());
                        LatLngBounds bounds = builder.build();
                        int padding = 200; // offset from edges of the map in pixels, in order to center the markers and put both of them in view
                        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                        mMap.moveCamera(cu);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            });
        } else if (manager.isProviderEnabled(manager.GPS_PROVIDER)) {

            manager.requestLocationUpdates(manager.GPS_PROVIDER, 0, 0, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    double lat = location.getLatitude();
                    double longit = location.getLongitude();
                    double carLat = Double.parseDouble(carLocations.get(0)[1]);
                    double carLongit = Double.parseDouble(carLocations.get(0)[0]);
                    LatLng loc = new LatLng(lat, longit);
                    LatLng carLoc = new LatLng(carLat, carLongit);
                    Geocoder geo = new Geocoder(getApplicationContext());

                    try {
                        List<Address> addresses = geo.getFromLocation(lat, longit, 1);
                        List<Address> carAddresses = geo.getFromLocation(carLat, carLongit, 1);
                        mMap.addMarker(new MarkerOptions().position(loc).title("Marker in London"));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 15.2f));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            });
        }

    }

    public void menuActions() {

        menuButton = (Button) findViewById(R.id.menuButton);
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent menu = new Intent(MapsActivity.this, SettingsActivity.class);
                startActivity(menu);
            }
        });

    }

    public void refreshAction() {
        refreshButton = (ImageButton) findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                startActivity(getIntent());

            }
        });

    }


    public void viewHistory() {
        historyButton = (Button) findViewById(R.id.viewSMS);
        historyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent locations = new Intent(MapsActivity.this, locationList.class);
                startActivity(locations);

            }
        });

    }





    public void manageRefresh() {//This is to control the refresh rate of the application
        String refreshRate = sharedPrefs.getString("refresh_frequency", "0");
        //System.out.println("ListPreference value:|| " + refreshRate+" !!!!");
       // refreshRate=sharedPrefs.getString("sync_frequency","0");
        final int refreshTimer = Integer.parseInt(refreshRate);

        if (refreshTimer != 0) {

            Thread t = new Thread(){
            @Override
            public void run () {
                try {
                    System.out.println("Should refresh !!!" +refreshTimer);
                    Thread.sleep(refreshTimer);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("Should refresh !!!" +refreshTimer);
                            finish();
                            startActivity(getIntent());
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
         };
         t.start();
           /*Timer timer = new Timer();
            TimerTask refresher = new TimerTask() {
                @Override
                public void run() {
                    handler.post(new Runnable() {
                        @SuppressWarnings("unchecked")
                        public void run() {
                            try {
                                  System.out.println("This is where the page would have refreshed!!!");
                                  finish();
                                  startActivity(getIntent());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            };
            timer.schedule(refresher,0,refreshTimer);*/




        }
    }

    public String vehicleDistance(LatLng location, LatLng carLocation){
        String userSelection = sharedPrefs.getString("marker_information", "0");
        Location userLoc=new Location("");
        Location carLoc=new Location("");
        userLoc.setLatitude(location.latitude);
        userLoc.setLongitude(location.longitude);
        carLoc.setLatitude(carLocation.latitude);
        carLoc.setLongitude(carLocation.longitude);
        double distance=userLoc.distanceTo(carLoc);
        System.out.println(distance);
        if (userSelection.equals("km")){
            double km=distance/1000;
            //Link below used to help with rounding values
            //https://www.mkyong.com/java/java-display-double-in-2-decimal-points/
            String formattedValue=String.format("%.2f",km)+" km";
            return formattedValue;
        }
        else{
            double miles=distance*0.000621371; //From google metre to miles converter
            String formattedValue=String.format("%.2f",miles)+" mi";
            return formattedValue;
        }


    }
}
   /*   final Runnable refresher = new Runnable() {
                public void run() {
                    System.out.println("SCREEN WOULD REFRESH IF IT COULD!!!!!" + " " + refreshTimer);
                    finish();
                    startActivity(getIntent());
                  //  android.support.v4.app.FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                  //  ft.detach(mapFragment).attach(supportMap).commit();
                  //  ft.replace(R.id.map, supportMap.instantiate(MapsActivity.this, supportMap.getClass().getName()));
                }
            };
            final ScheduledFuture<?> smsHandle = scheduler.scheduleAtFixedRate(refresher, 10, refreshTimer, SECONDS);
            scheduler.schedule(new Runnable() {
                public void run() {
                    smsHandle.cancel(true);
                }
            }, refreshTimer * 60, SECONDS);
        }*/
