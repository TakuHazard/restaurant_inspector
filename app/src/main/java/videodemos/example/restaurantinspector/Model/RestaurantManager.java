package videodemos.example.restaurantinspector.Model;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import videodemos.example.restaurantinspector.R;


public class RestaurantManager {
    private final int NUM_OF_FIRST_NONVIOLATION_COLS = 5;
    public static RestaurantManager instance;
    private List<Restaurant> restaurantList = new ArrayList<>();

    public static RestaurantManager getInstance(Context c) {
        if (instance == null) {
            instance = new RestaurantManager(c);
        }

        return instance;
    }

    public List<Restaurant> getRestaurantList() {
        return restaurantList;
    }

    public Restaurant getRestaurant(int index) {
        return restaurantList.get(index);
    }

    private RestaurantManager(Context c) {
        InputStream is = c.getResources().openRawResource(R.raw.restaurants);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, Charset.forName("UTF-8"))
        );

        String line = "";
        try {

            // Step over headers
            reader.readLine();
            while ((line = reader.readLine()) != null) {

                // Split by ','

                String[] tokens = line.split(",");


                // Read the data

                Restaurant restaurant = new Restaurant();
                restaurant.setTrackingNumber(tokens[0].replace("\"", ""));
                restaurant.setName(tokens[1].replace("\"", ""));
                restaurant.setPhysicalAddress(tokens[2].replace("\"", ""));
                restaurant.setPhysicalCity(tokens[3].replace("\"", ""));
                restaurant.setFactype(tokens[4].replace("\"", ""));

                if (tokens[5].length() > 0) {
                    restaurant.setLatitude(Double.parseDouble(tokens[5]));
                } else {
                    restaurant.setLatitude(0);
                }

                if (tokens[6].length() > 0) {
                    restaurant.setLongitude(Double.parseDouble(tokens[6]));
                } else {
                    restaurant.setLongitude(0);
                }

                restaurantList.add(restaurant);

                Log.d("MyActivity", "Just  created this right now" + restaurant);

            }
        } catch (IOException e) {
            Log.wtf("My Activity", "Error reading data file on line " + line, e);
            e.printStackTrace();
        }

        sortByRestaurantName();
    }

    public void InspectionReader(Context c) {
        InputStream is = c.getResources().openRawResource(R.raw.inspectionreports);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, Charset.forName("UTF-8"))
        );

        String line = "";
        try {


            // Step over headers
            reader.readLine();
            while ((line = reader.readLine()) != null) {
                Inspection inspection = new Inspection();



                //" SPLH-9NEUHG" ,20191001," Routine" ,0,1," Low" ," 209,Not Critical,Food not protected from contamination [s. 12(a)],Not Repeat"

                // Even newer splitting method
                String[] tokens = line.split("\"");
                String trackingNum = tokens[1];
                for(Restaurant r: restaurantList){
                    if(r.getTrackingNumber().equals(trackingNum)){
                        String dateToAdd = tokens[2];
                        dateToAdd = dateToAdd.substring(1,dateToAdd.length() - 1);
                        dateToAdd.replaceAll(",","");
                        inspection.setInspectionDate(dateToAdd);

                        String inspectionType = tokens[3];
                        inspection.setInspType(inspectionType);

                        String valuesForCritical = tokens[4];
                        valuesForCritical = valuesForCritical.replaceAll(",","");

                        char charNumNonCrit = valuesForCritical.charAt(0);
                        String numNonCrit = Character.toString(charNumNonCrit);

                        char charNumCrit = valuesForCritical.charAt(1);
                        String numCrit = Character.toString(charNumCrit);


                        inspection.setNumNonCritical(Integer.parseInt(numNonCrit));
                        inspection.setNumCritical(Integer.parseInt(numCrit));

                        String hazardRating = tokens[5];
                        inspection.setHazardRating(hazardRating);

                        if(Integer.parseInt(numNonCrit) + Integer.parseInt(numCrit) > 0){
                            String[] violations = tokens[7].split("\\|");

                            for(String violationPossibility: violations){
                                String violation = violationPossibility.substring(0, 3);
                               inspection.addViolation(Integer.parseInt(violation));
                            }

                        }

                        r.addInspection(inspection);

                    }
                }

            }

        } catch (Exception e) {
            Log.wtf("My Activity", "Error reading data file on line " + line, e);
            e.printStackTrace();
        }

    }

    private void sortByRestaurantName() {
        Comparator<Restaurant> comparatorName = new Comparator<Restaurant>() {
            @Override
            public int compare(Restaurant r1, Restaurant r2) {
                return r1.getName().compareTo(r2.getName());
            }
        };

        Collections.sort(restaurantList, comparatorName);
    }

    public void sortInspections() {
        for (Restaurant r : restaurantList) {
            r.sortByInspectionDate();
        }
    }
    
}
