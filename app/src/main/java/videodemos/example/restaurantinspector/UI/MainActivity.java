package videodemos.example.restaurantinspector.UI;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.Charset;

import videodemos.example.restaurantinspector.Model.DBAdapter;
import videodemos.example.restaurantinspector.Model.DataHandling.DateCalculations;
import videodemos.example.restaurantinspector.Model.DataHandling.Inspection;
import videodemos.example.restaurantinspector.Model.DataHandling.Restaurant;
import videodemos.example.restaurantinspector.Model.RestaurantManager;
import videodemos.example.restaurantinspector.Model.Network.HttpHandler;
import videodemos.example.restaurantinspector.R;

/**
 * Main Activity displays all the restaurants.
 */

public class MainActivity extends AppCompatActivity implements RestaurantsAdapter.OnRestaurantListener {

    private final String RESTAURANT_URL = "https://data.surrey.ca/api/3/action/package_show?id=restaurants";
    private final String INSPECTION_URL = "https://data.surrey.ca/api/3/action/package_show?id=fraser-health-restaurant-inspection-reports";
    private final int HOURS_FOR_UPDATE = 300000000;

    private RestaurantManager manager;
    private DBAdapter dbAdapter = new DBAdapter(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        // First check the dates. If there is a new update then proceed as follows


        // If there is no update then
        //
        // if the database is empty. If it empty then load from initial csv files
        // Databse is empty iff we have no restaurants.

        dbAdapter.open();
        Cursor cursor = dbAdapter.getAllRestaurantRows();
        if (cursor.moveToFirst())
        {

            // DO SOMETHING WITH CURSOR
            // so load from the database.

        } else
        {
            // I AM EMPTY
            // So we load from the csv file
        }


        //loadDB();
        Toast.makeText(this,"FINISHED", Toast.LENGTH_LONG).show();
        Log.d("ENDWHILECREATE", "onCreate continues..");

        printViolationDb();
        
        setupToolbar();
        setupRestaurantManager();

        setUpRestaurantsRecylerView();

        checkForNewServerData();



    }

    private void printViolationDb() {
        dbAdapter.open();

        Cursor cursor = dbAdapter.getAllViolationRows();

        if(cursor.moveToFirst()){
            do{

                Log.d("Violation:" ,cursor.getString(DBAdapter.COL_TRACKING_NUMBER_VIOLATION)
                        + cursor.getString(DBAdapter.COL_VIOLATION_DATE) + cursor.getString(DBAdapter.COL_VIOLATION_CODE));

            }while(cursor.moveToNext());
        }
        dbAdapter.close();
    }

    private void loadDBFromServer() {


        //dbAdapter = new DBAdapter(this);
        dbAdapter.open();
        loadRestaurantsFromServer();
        loadInspectionsFromServer();
        dbAdapter.close();
    }

    private void loadDB() {
        //dbAdapter = new DBAdapter(this);
        dbAdapter.open();
        readFromCSV();
        loadInspectionsCSVToDB();
        dbAdapter.close();
    }

    private void readFromCSV() {
        InputStream is = this.getResources().openRawResource(R.raw.restaurants);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, Charset.forName("UTF-8"))
        );

        String line = "";
        try {

            final int TRACKING_NUM_INDEX = 0;
            final int SET_NAME_INDEX = 1;
            final int SET_PHYSICAL_ADDRESS = 2;
            final int SET_PHYSICALCITY = 3;
            final int SET_FACT_TYPE = 4;
            final int SET_LATITUDE_TYPE = 5;
            final int SET_LONGITUDE= 6;

            // Step over headers
            reader.readLine();
            while ((line = reader.readLine()) != null) {

                // Split by ','

                String[] tokens = line.split(",");

                // Read the data
                String trackingNum = tokens[TRACKING_NUM_INDEX].
                        replace("\"", "");

                String name = tokens[SET_NAME_INDEX].
                        replace("\"", "");

                String address = tokens[SET_PHYSICAL_ADDRESS].
                        replace("\"", "");

                String city = tokens[SET_PHYSICALCITY].
                        replace("\"", "");

                String type = tokens[SET_FACT_TYPE].
                        replace("\"", "");

                double latitude = 0;
                if (tokens[SET_LATITUDE_TYPE].length() > 0) {
                    latitude = Double.parseDouble(tokens[SET_LATITUDE_TYPE]);
                }

                double longitude = 0;
                if (tokens[SET_LONGITUDE].length() > 0) {
                    longitude = Double.parseDouble(tokens[SET_LONGITUDE]);
                }

                dbAdapter.insertRow(trackingNum, name, address, city, type, latitude, longitude);

            }
        } catch (IOException e) {
            Log.wtf("My Activity", "Error reading data file on line " + line, e);
            e.printStackTrace();
        }
    }

    public void loadInspectionsCSVToDB() {

        InputStream is = getResources().openRawResource(R.raw.inspectionreports);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, Charset.forName("UTF-8"))
        );

        String line = "";
        try {

            final int TRACKING_NUM_INDEX = 1;
            final int DATE_INDEX = 2;
            final int INSPECTION_TYPE_INDEX = 3;
            final int CRITICAL_INDEX = 4;
            final int HAZARD_INDEX = 5;
            final int VIOLATIONS_LUMP_INDEX = 7;

            // Step over headers
            reader.readLine();
            while ((line = reader.readLine()) != null) {
                // Even newer splitting method
                String[] tokens = line.split("\"");
                String trackingNum = tokens[TRACKING_NUM_INDEX];

                String dateToAdd = tokens[DATE_INDEX];
                dateToAdd = dateToAdd.substring(1, dateToAdd.length() - 1);


                String inspectionType = tokens[INSPECTION_TYPE_INDEX];


                String valuesForCritical = tokens[CRITICAL_INDEX];
                valuesForCritical = valuesForCritical.replaceAll(",","");

                char charNumNonCrit = valuesForCritical.charAt(0);
                String numNonCrit = Character.toString(charNumNonCrit);

                char charNumCrit = valuesForCritical.charAt(1);
                String numCrit = Character.toString(charNumCrit);


                String hazardRating = tokens[HAZARD_INDEX];

                if(Integer.parseInt(numNonCrit) + Integer.parseInt(numCrit) > 0){
                    String[] violations = tokens[VIOLATIONS_LUMP_INDEX].split("\\|");

                    for(String violationPossibility: violations){
                        String violation = violationPossibility.substring(0, 3);
                        dbAdapter.insertViolationRow(trackingNum, dateToAdd, Integer.parseInt(violation));
                    }

                }

                dbAdapter.insertRow(trackingNum, hazardRating, dateToAdd, inspectionType, Integer.parseInt(numCrit), Integer.parseInt(numNonCrit));

            }

        } catch (Exception e) {
            Log.wtf("My Activity", "Error reading data file on line " + line, e);
            e.printStackTrace();
        }


    }

    public void loadInspectionsFromServer() {

        String line = "";
        try {

            HttpHandler httpHandler = new HttpHandler(INSPECTION_URL);
            httpHandler.getData();
            String body = httpHandler.getBody();


            //TRACKINGNUMBER,INSPECTIONDATE,INSPTYPE,NUMCRITICAL,NUMNONCRITICAL,VIOLLUMP,HAZARDRATING
            //SWOD-APSP3X,20190830,Routine,1,0,"205,Critical,Cold potentially hazardous food stored/displayed above 4 �C. [s. 14(2)],Not Repeat",Low
            //SWOD-APSP3X,20190305,Routine,0,2,"306,Not Critical,Food premises not maintained in a sanitary condition [s. 17(1)],Not Repeat|403,Not Critical,Employee lacks good personal hygiene, clean clothing and hair control [s. 21(1)],Not Repeat",Low
            //SWOD-APSP3X,20180625,Routine,2,3,"209,Not Critical,Food not protected from contamination [s. 12(a)],Not Repeat|303,Critical,Equipment/facilities/hot & cold water for sanitary maintenance not adequate [s. 17(3); s. 4(1)(f)],Not Repeat|308,Not Critical,Equipment/utensils/food contact surfaces are not in good working order [s. 16(b)],Not Repeat|311,Not Critical,Premises not maintained as per approved plans [s. 6(1)(b)],Not Repeat|402,Critical,Employee does not wash hands properly or at adequate frequency [s. 21(3)],Not Repeat",Moderate
            //SWOD-APSP3X,20180103,Routine,0,2,"209,Not Critical,Food not protected from contamination [s. 12(a)],Not Repeat|307,Not Critical,Equipment/utensils/food contact surfaces are not of suitable design/material [s. 16; s. 19],Not Repeat",Low
            //SWOD-APSP3X,20171011,Follow-Up,0,0,,Low

            Log.d("BODYY:", body);

            BufferedReader reader = new BufferedReader(new StringReader(body));

            // Step over headers
            reader.readLine();
            while ((line = reader.readLine()) != null) {
                Inspection inspection = new Inspection();

                // Even newer splitting method
                //NDAA-9DNQLJ,20180924,Follow-Up,0,0,,Low
                //HCAR-BKDQCL,20200127,Routine,1,0,"302,Critical,Equipment/utensils/food contact surfaces not properly washed and sanitized [s. 17(2)],Not Repeat",Low
                String [] tokens = line.split("\"");

                String [] tokensFirstHalf = tokens[0].split(",");

                if (tokensFirstHalf.length == 0){
                    continue;
                }
                String trackingNum = tokensFirstHalf[0];

                if(tokens.length > 1){

                    String datetoAdd = tokensFirstHalf[1];


                    String inspectionType = tokensFirstHalf[2];


                    int numOfCritical = Integer.parseInt(tokensFirstHalf[3]);


                    int numOfNonCritical = Integer.parseInt(tokensFirstHalf[4]);

                    if(numOfCritical + numOfNonCritical != 0){
                        String[] violations = tokens[1].split("\\|");
                        for(String violationPossibility: violations){
                            String violation = violationPossibility.substring(0, 3);
                            dbAdapter.insertViolationRow(trackingNum, datetoAdd, Integer.parseInt(violation));
                        }
                    }

                    String hazardLevel = tokens[2];
                    String hazardRating;
                    if(hazardLevel.length() > 1){
                        hazardRating = hazardLevel;
                    }
                    else{
                        hazardRating = "Low";
                    }

                    long rowId = dbAdapter.insertRow(trackingNum, hazardRating, datetoAdd, inspectionType, numOfCritical, numOfNonCritical);
                    //Log.d("Entry", "" + rowId);

                }
                else{

                    // NDAA-8RNNVR,20181017,Routine,0,0,,Low
                    String [] tokensSplit = tokens[0].split(",");
                     trackingNum = tokensSplit[0];


                     String dateToAdd = tokensSplit[1];


                     String inspectionType = tokensSplit[2];


                     int numOfNonCritical = Integer.parseInt(tokensSplit[3]);
                     int numOfCritical = Integer.parseInt(tokensSplit[4]);

                     String hazardRating = "Low";
                     if (tokensSplit.length == 7){
                         hazardRating = tokensSplit[6];
                     }

                    dbAdapter.insertRow(trackingNum, hazardRating, dateToAdd, inspectionType, numOfCritical, numOfNonCritical);

                }
                Log.d("Load", "Still going");
            }

        } catch (Exception e) {
            Log.wtf("My Activity", "Error reading data file on line " + line, e);
            e.printStackTrace();
        }


        Log.d("ENDWHILE", "ENDED");

    }

    public void loadRestaurantsFromServer() {

        String line = "";
        try {
            HttpHandler httpHandler = new HttpHandler(RESTAURANT_URL);
            httpHandler.getData();
            String body = httpHandler.getBody();

            BufferedReader reader = new BufferedReader(new StringReader(body));
            final int TRACKING_NUM_INDEX = 0;
            final int SET_NAME_INDEX = 1;
            final int SET_PHYSICAL_ADDRESS = 2;
            final int SET_PHYSICALCITY = 3;
            final int SET_FACT_TYPE = 4;
            final int SET_LATITUDE_TYPE = 5;
            final int SET_LONGITUDE= 6;

           // SWOD-AG5UGV,"Green Indian Cuisine, Pizza & Sweets",12565 88 Ave,Surrey,Restaurant,49.16401631,-122.8747815



            // Step over headers
            reader.readLine();
            while ((line = reader.readLine()) != null) {

                // Split by ','

                Restaurant restaurant = new Restaurant();

                String []tokensEdge = line.split("\"");
                if(tokensEdge.length > 1) {
                    String trackingNum = tokensEdge[0].replaceAll(",", "");

                    String name = tokensEdge[1];

                    String[] tokensEdgeRest = tokensEdge[2].split(",");

                    String address = tokensEdgeRest[1];

                    String city = tokensEdgeRest[2];

                    String type = tokensEdgeRest[3];

                    Double latitude = Double.parseDouble(tokensEdgeRest[4]);

                    Double longitude = Double.parseDouble(tokensEdgeRest[5]);

                    dbAdapter.insertRow(trackingNum, name, address, city, type, latitude, longitude);
                }
                else{
                    String[] tokens = line.split(",");

                    // Read the data

                    // We first try to see if our name has commas in it

                    String trackingNum = tokens[TRACKING_NUM_INDEX].
                            replace("\"", "");

                    String name = tokens[SET_NAME_INDEX].
                            replace("\"", "");

                    String address = tokens[SET_PHYSICAL_ADDRESS].
                            replace("\"", "");

                    String city = tokens[SET_PHYSICALCITY].
                            replace("\"", "");

                    String type = tokens[SET_FACT_TYPE].
                            replace("\"", "");

                    double latitude = 0;
                    if (tokens[SET_LATITUDE_TYPE].length() > 0) {
//                        Log.d("RestaurantManager",tokens[SET_LATITUDE_TYPE]);
//                        Log.d("RestaurantManager","Line is " + line);
                        latitude = Double.parseDouble(tokens[SET_LATITUDE_TYPE]);
                    }

                    double longitude = 0;
                    if (tokens[SET_LONGITUDE].length() > 0) {
                        longitude = Double.parseDouble(tokens[SET_LONGITUDE]);
                    }

                    dbAdapter.insertRow(trackingNum, name, address, city, type, latitude, longitude);
                }

            }
        } catch (Exception e) {
            Log.wtf("My Activity", "Error reading data file on line " + line, e);
            e.printStackTrace();
        }

    }


    private void checkForNewServerData() {
        Thread gettingDataThread = new Thread(new Runnable(){
            public void run(){
                try{
                    HttpHandler httpHandler = new HttpHandler(RESTAURANT_URL);
                    String latestDate = httpHandler.getCurrentDateFromServer();
                    if (isNewDataAvailable(latestDate)){
                        // ask user in dialog

                        // update database

                        Log.d("DATEWORKS", "Nice");
                    }else {
                        Log.d("DATEWORKS", "Nah");
                    }

                } catch(Exception e){
                    e.printStackTrace();
                }
            }
        });

        gettingDataThread.start();
    }

    public boolean isNewDataAvailable(String latestDate){
        DateCalculations dateCalculations = new DateCalculations();

        return dateCalculations.hoursInBetween(latestDate) >= HOURS_FOR_UPDATE;
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.restaurant_list_toolbar);
        setSupportActionBar(toolbar);

        ImageButton helpButton = findViewById(R.id.ib_restaurant_help_icon);
        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = InfoScreenActivity.makeLaunchIntent(MainActivity.this);
                startActivity(intent);
            }
        });
    }

    private void setupRestaurantManager(){
        manager = RestaurantManager.getInstance(this);
        manager.InspectionReader(this);
        manager.sortInspections();
    }

    private void setUpRestaurantsRecylerView() {
        RecyclerView restaurantsRecyclerView = findViewById(R.id.rv_restaurant_list);

        restaurantsRecyclerView.setHasFixedSize(true);
        restaurantsRecyclerView.setItemViewCacheSize(20);
        restaurantsRecyclerView.setDrawingCacheEnabled(true);
        restaurantsRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        restaurantsRecyclerView.setLayoutManager(layoutManager);

        RestaurantsAdapter restaurantsAdapter = new RestaurantsAdapter(manager.getRestaurantList(), this, this);
        restaurantsRecyclerView.setAdapter(restaurantsAdapter);
    }

    @Override
    public void onRestaurantClick(int position) {
            Intent intent = RestaurantReportActivity.makeIntent(this, position);
            startActivity(intent);
    }
}
