package com.example.sherif.testproject1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.R.attr.format;

/**
 * Created by SHERIF on 07/03/2018.
 */

//This page aims to display all previous locations of the vehicle along with the date.
public class locationList extends AppCompatActivity {
    TextView smsBody;
    Button setDate;
    Button backButton;
    Button resetButton;
    String selectedDate;
    EditText dateText;
    SharedPreferences sharedPrefs;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.location_list);
        smsBody=(TextView)findViewById(R.id.textView2);
        setDate=(Button)findViewById(R.id.dateSet);
        sharedPrefs= PreferenceManager.getDefaultSharedPreferences(this);
       // selectedDate="";
       // dateSet();
        loadSMS(selectedDate);
        backAction();

    }
    public void loadSMS(String selectedDate){ //This method is used to display previous locations which were sent in earlier text messages
        StringBuilder smsBuilder = new StringBuilder();
        final String SMS_URI_INBOX = "content://sms/inbox";
        final String SMS_URI_ALL = "content://sms/";

        try {
            Uri uri = Uri.parse(SMS_URI_INBOX);
            String[] projection = new String[]{"_id", "address", "person", "body", "date", "type"};
            Cursor cur = getContentResolver().query(uri, projection, "address='+447387835492'", null, "date desc");


            //This will need to link to a dropdownlist where the user determines how early they
            // to iew their history
            String latitude = "",longitude="";
            Geocoder geocoder=new Geocoder(getApplicationContext());
            List<Address> addresses=null;


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
                    //CODE BELOW FOR DISPLAYING ADDRESS
                    String[] smsSplit = strbody.split("\\r?\\n");

                    String latitudeString = smsSplit[1];
                    String longitudeString = smsSplit[2];
                    System.out.println("LOCATION DISPLAY: "+latitudeString+" || "+longitudeString);
                    String[] latArray = latitudeString.split(":");
                    String[] longitArray = longitudeString.split(":");
                    latitude = latArray[1].replaceAll("\\s+", "");
                    longitude = longitArray[1].replaceAll("\\s+", "");
                    double lat=Double.parseDouble(latitude);
                    double longit=Double.parseDouble(longitude);

                    try {
                        addresses=geocoder.getFromLocation(lat,longit,1);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //if things mess up, comment out the code above
                    long longDate = cur.getLong(index_Date);
                    long earliestDate = 1515001880553L;
                    int int_Type = cur.getInt(index_Type);
                    Date date = new Date(longDate);
                    Date newDate=new Date(0);
                    Date earliest=new Date(earliestDate);
// the format of your date

                    SimpleDateFormat sendDate = new SimpleDateFormat(userDatePreference()+userTimePreference());//Convert date to visual format
                    SimpleDateFormat chosenDate = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss ");
                    String finalDate = sendDate.format(date);

                    selectedDate=dateSet();
                    if(selectedDate != null && !selectedDate.isEmpty()){
                            System.out.println("CURRENT SELECTED DATE IS: "+selectedDate+"!!!");

                        newDate = chosenDate.parse(selectedDate);
                        if(newDate.after(date)) {//Filtering happens here by comparing date entered by user.
                            smsBody.setText("   ");
                            break;
                        }
                        }
                    if(date.after(earliest)){
                        String addressLine=addresses.get(0).getAddressLine(0);
                        String locality=addresses.get(0).getLocality();
                        String countryCode=addresses.get(0).getCountryCode();
                        smsBuilder.append(addressLine+" "+locality+" "+countryCode);
                        smsBuilder.append(" \nDate located: "+finalDate );
                        smsBuilder.append("\n\n");
                        smsBuilder.append("------------------------------------------------");
                        smsBuilder.append("\n\n");
                    }

                } while (cur.moveToNext());

                if (!cur.isClosed()) {
                    cur.close();
                    cur = null;
                }
            } else {
                smsBuilder.append("no result!");
            } // end if
            smsBody.setText(smsBuilder.toString());
            System.out.println("SMS: "+smsBuilder.toString());
        }
        catch (SQLiteException ex) {
            Log.d("SQLiteException", ex.getMessage());
        }/* catch (ParseException e) {
            e.printStackTrace();
        }*/ catch (ParseException e) {
            e.printStackTrace();
        }
        resetButton=(Button)findViewById(R.id.resetButton);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //finish();
                startActivity(getIntent());
            }
        });

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
            System.out.println("shorthand!!!!");
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
    public String dateSet(){
        final Date[] selectDate = new Date[1];
        final String chosenDate;
        dateText=(EditText)findViewById(R.id.dateText);
        final String newDate="";
        setDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){


                    // loadSMS(selectDate[0]);
                    System.out.println(dateText.getText());
                    selectedDate=dateText.getText().toString()+" 00:00:00 ";
                    loadSMS(selectedDate);
                    dateText.setText("");

            }});

        System.out.println("THE NEW DATE IS:"+selectedDate);
        DateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss ");

        System.out.println("DATE IS PRINTING:"+selectedDate);
        return selectedDate;
    }
    public void backAction(){
        backButton=(Button)findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }





    }

