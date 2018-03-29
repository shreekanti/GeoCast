package jeeryweb.geocast.Activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;

import android.provider.*;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import android.content.Context;
import android.location.Location;
import android.os.Handler;

import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import jeeryweb.geocast.Dialogs.MessageInputDialog;
import jeeryweb.geocast.FirebaseServices.FirebaseRegistrationIntentService;
import jeeryweb.geocast.Services.LocationUpdaterService;
import jeeryweb.geocast.Utility.FileHelper;
import jeeryweb.geocast.Utility.Network;


/**
 * This class 1.sends the FCM token to the server if running --for the first time-- in a separate thread
 *            2.uploads the last known location in a separate thread
 *            3.sends the message if send button clicked on a separate thread
 *
 *            4.Running a FirebaseRegisterIntentService for getting the token for the first time
 *            5.starting the LocationUpdater Service
 *            6.Starting the MessageRecieverService
 *
 */

import jeeryweb.geocast.R;
import jeeryweb.geocast.Utility.SharedPrefHandler;

public class Home extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,OnMapReadyCallback {

//Attributes***************************************************************

    //TAG for Logging
    private final String updateLoc = "https://jeeryweb.000webhostapp.com/ProjectLoc/updateLoc.php";
    private final String sendMsg = "https://jeeryweb.000webhostapp.com/ProjectLoc/uploadMsg.php";
    private final String updateFcm = "https://jeeryweb.000webhostapp.com/ProjectLoc/updateFcm.php";
    private final String lgot = "https://jeeryweb.000webhostapp.com/ProjectLoc/logout.php";
    private final String pullMsg = "https://jeeryweb.000webhostapp.com/ProjectLoc/pullMsg.php";
    public static boolean locChanged=false;


    private BroadcastReceiver receiver;



    //objects
    SharedPrefHandler sharedPrefHandler;
    Handler handler;
    NavigationView navigationView;
    Context con;
    Activity activity;



    //globally required for location purpose
    private int mode;
    private UiSettings mUiSettings;
    private GoogleMap mMap;
    static Location locationObj;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    /**
     * Provides access to the Location Settings API.
     */
    private SettingsClient mSettingsClient;
    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    private LocationSettingsRequest mLocationSettingsRequest;

    /**
     * Constant used in the location settings dialog.
     */
    private static final int REQUEST_CHECK_SETTINGS = 0x1;
    private FusedLocationProviderClient mFusedLocationClient;
    LocationUpdaterService locationUpdaterService;




    Network network;
    FileHelper fileHelper;



    //Variables
    private final String TAG = getClass().getSimpleName();
    //to check if my activity is the current activity
    public boolean isInFront;
    //to check if the app is run for the first time
    private boolean firstTime;
    static String username, password;
    String fcmToken;
    String msg, result;




    View mapView;
    FloatingActionMenu sendMessageFab;



    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        con = this;
        activity= this;


        receiver = new BroadcastReceiver() {
            final Snackbar snackbar=Snackbar.make(findViewById(R.id.home_content_parent), "No Internet Connection", Snackbar.LENGTH_INDEFINITE);
            @Override
            public void onReceive(Context context, Intent arg1) {
                //do something based on the intent's action
                boolean isConnected = arg1.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                if(isConnected){
                    Log.e("broadcast reciever","no network");
                    snackbar.show();
                }
                else{
                    Log.e("broadcast reciever","network aagaya");
                    if(snackbar!=null) {
                        snackbar.dismiss();
                        Log.e("broadcast reciever","snackbar  mila");
                    }
                }
            }
        };
        registerReceiver(receiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));




        com.github.clans.fab.FloatingActionButton emergencyMsgFab, customMsgFab;

        sendMessageFab = (FloatingActionMenu) findViewById(R.id.sendmsg_floating_menu);
        emergencyMsgFab = (com.github.clans.fab.FloatingActionButton) findViewById(R.id.emergencymsgfab);
        customMsgFab = (com.github.clans.fab.FloatingActionButton) findViewById(R.id.custommsgfab);


        emergencyMsgFab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //TODO something when floating action menu first item clicked
                //send in built message directly
                msg = "Please help me please";

                //do this on a new thread
                new Thread(new Runnable() {
                    public void run() {                                                 //THREAD 4.............
                        // a potentially  time consuming task
                        if (locationObj != null) {
                            network = new Network(sendMsg, username, password, msg, Double.toString(locationObj.getLatitude()), Double.toString(locationObj.getLongitude()), "ksdhfj", null, null, null, null);
                            result = network.DoWork();
                            if (result != null) {
                                Log.e(TAG, "send message main thread" + result);
                                //pass this result to UI thread by writing a message to the UI's handler
                                Message m = Message.obtain();
                                Bundle bundle = new Bundle();
                                bundle.putString("result", result);
                                m.setData(bundle);
                                handler.sendMessage(m);
                            }
                        }
                    }
                }).start();
                sendMessageFab.close(true);

            }
        });
        customMsgFab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //TODO something when floating action menu second item clicked
                MessageInputDialog messageInputDialog = new MessageInputDialog();
                messageInputDialog.passFloatMenu(sendMessageFab);
                messageInputDialog.show(getFragmentManager(), "customMsg");

            }
        });
        sendMessageFab.setClosedOnTouchOutside(true);





        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.home_nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.home_nav_home);

        //getting navbar header items- image , name ,welcome
        View homeNavHeader = navigationView.getHeaderView(0);
        LinearLayout navHeaderLayout = (LinearLayout) homeNavHeader.findViewById(R.id.home_nav_layout);

        //setting up objects ................................................................................
        sharedPrefHandler = new SharedPrefHandler(this);


        //logged in first time or not --required for fcm token sending........................................
        Bundle bundle = getIntent().getExtras();
        if (bundle != null)
            firstTime = bundle.getBoolean("firstTime");


        //Retrieving username and password ..................................................................
        HashMap<String, String> user = sharedPrefHandler.getUserDetails();
        username = user.get("name");
        password = user.get("pass");
        Log.e(TAG + " retrieved session:", username + "  " + password);


        //if first time run firebase service and send token to server if not dont run.................................
        if (firstTime) {
            Log.e(TAG, "first time yes");
            //create chat history saving file
            fileHelper = new FileHelper();
            fileHelper.createFile(this);
            //noone should interrupt while registering so doing it in a service
            if (checkPlayServices()) {
                Intent intent = new Intent(this, FirebaseRegistrationIntentService.class);
                startService(intent);
            }
            fcmToken = sharedPrefHandler.getFcmToken();
            if (username != null && password != null & fcmToken != null) {
                new Thread(new Runnable() {
                    public void run() {                                                 //THREAD 1.................
                        // a potentially  time consuming task
                        network = new Network(updateFcm, username, password, "dummy", "00.00", "00.00", fcmToken, null, null, null, null);
                        result = network.DoWork();
                        if (result != null) {
                            Log.e(TAG, " Fcm " + result);
                        }
                    }
                }).start();
            }
        } else {
            Log.e(TAG, "first time no");
            fcmToken = sharedPrefHandler.getFcmToken();
            Log.e(TAG + " retrieved token:", fcmToken);
        }


        //getting widgets........................................................................................


        //set username in navigation drawer
        TextView navUsername = (TextView) homeNavHeader.findViewById(R.id.home_nav_username);
        navUsername.setText(username.toUpperCase());


        //setting up the map fragment view ...........................................................................
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapView = mapFragment.getView();
        mapFragment.getMapAsync(this);



        //try to get current location
        //check settings first
        try {
            mode= android.provider.Settings.Secure.getInt(this.getContentResolver(), android.provider.Settings.Secure.LOCATION_MODE);
            Log.e("settings mode::::=",String.valueOf(mode));
        } catch (Exception e) {
            Log.e("settings mode=","exception setting not found");
            e.printStackTrace();
        }
        if(mode==0)
            showSettingsAlert(0); //ask to enable gps
        if(mode!=0 && mode!=3)
            showSettingsAlert(1); //ask to set to high accuracy mode
        if(mode==3)
        {
            //all ok try to get location
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

            //mFusedLocationClient=== OPtion 1:: get last known location
            tryToGetLastLocation();
            //mFusedLocationClient=== OPtion 2:: request location ourself
            if(locationObj==null)
                tryToRequestLocationOurself();

            //lets assume we successfully got the location
            if(locationObj!=null) {
                homeLocationSuccessDoWork();
                tryToRequestLocationOurself(); //to get periodic update on location change
            }

        }


        mapView.post(new Runnable() {
            @Override
            public void run() {

                if (mMap != null && locationObj!=null && locChanged ==true) {
                    LatLng myloc = new LatLng(locationObj.getLatitude(), locationObj.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(myloc).title("My Current Location"));
                    LatLng coordinate = new LatLng(locationObj.getLatitude(), locationObj.getLongitude()); //Store these lat lng values somewhere. These should be constant.
                    CameraUpdate cam = CameraUpdateFactory.newLatLngZoom(coordinate, 13);
                    mMap.animateCamera(cam);
                    mMap.setMyLocationEnabled(true);
                    Log.e("maps", "ui thread update");
                    locChanged = false;
                }
                mapView.postDelayed(this,10000);
            }
        });


        //setting listeners on buttons.............................................................................

        //setting listner on nav header layout to go to MyProfile Activity
        navHeaderLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //go to MyProfile Activity
                Intent i = new Intent(con, MyProfile.class);
                con.startActivity(i);
            }
        });

        //done-------------------------------------------------------------------------------------

        Log.e("Main2Activity Threads=", " " + Thread.activeCount());

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                result = msg.getData().get("result").toString();
                Toast.makeText(con, "Message Sent Successfully", Toast.LENGTH_LONG).show();
            }
        };
    }


    //when this activity restarts or resumes check if the location service is running or not
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    void homeLocationSuccessDoWork() {

        //Got Location
        Log.e("mFusedLocationClient", "getLastLocationSuccess");
        Log.e("mFusedLocationClient", "getLastLocationObjnotnull");
        //set the marker in the map only if the map is already ready
        if (mMap != null && locationObj !=null) {
            LatLng myloc = new LatLng(locationObj.getLatitude(), locationObj.getLongitude());
            mMap.addMarker(new MarkerOptions().position(myloc).title("My Current Location"));
            LatLng coordinate = new LatLng(locationObj.getLatitude(), locationObj.getLongitude()); //Store these lat lng values somewhere. These should be constant.
            CameraUpdate cam = CameraUpdateFactory.newLatLngZoom(coordinate, 13);
            mMap.animateCamera(cam);
            mMap.setMyLocationEnabled(true);
            Log.e("maps", "home location success do work");
        }
        // Logic to handle location object
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
        String format = simpleDateFormat.format(new Date());
        //Log.v(TAG + "timestamp", format);Log.e(TAG + " lattitude", Double.toString(location.getLatitude()));Log.e(TAG + " longitude", Double.toString(location.getLongitude()));
        msg = "dummy";
        //update database with this location
        //do this on a new thread
        new Thread(new Runnable() {
            public void run() {                                                     //THREAD 2............
                // a potentially  time consuming task
                network = new Network(updateLoc, username, password, msg, Double.toString(locationObj.getLatitude()), Double.toString(locationObj.getLongitude()), "kjdfjk", null, null, null, null);
                result = network.DoWork();
                if (result != null) {
                    Log.e(TAG, "toast main thread" + result);
                    if (result.contains("some keyword")) { //we are not sending this thats why some keyword
                        //pass this result to UI thread by writing a message to the UI's handler
                        Message m = Message.obtain();
                        Bundle bundle = new Bundle();
                        bundle.putString("result", result);
                        m.setData(bundle);
                        handler.sendMessage(m);
                    }
                }
            }
        }).start();

        //start the services------------only if location is known
        //location updater service
        if (!isMyServiceRunning(LocationUpdaterService.class)) {
            Intent intent1 = new Intent(con, LocationUpdaterService.class);
            startService(intent1);
        }
    }

    private void tryToGetLastLocation()
    {
        mFusedLocationClient.getLastLocation().addOnSuccessListener(activity, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                locationObj=location;

            }
        });
    }

    private void tryToRequestLocationOurself()
    {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //create locationCallback
        locationCallback = new LocationCallback() {
            public void OnLocationResult(LocationResult locationResult) {

                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    Log.e("on location change"," main ui thread location change callback");
                    // Update UI with location data
                    locationObj = location;
                    locChanged= true;
                }

            }
        };

        mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

    }




    /**
     * Function to show settings alert dialog
     * On pressing Settings button will lauch Settings Options
     * */

    public void showSettingsAlert(int mode){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(con);

        // Setting Dialog Title
        alertDialog.setTitle("GPS settings");

        // Setting Dialog Message
        if(mode == 0)
            alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");
        else if(mode ==1)
            alertDialog.setMessage("GPS is not set to High Accuracy Mode. Do you want to go to settings menu?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(intent,103);
            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }

    private  void updateLocationInUI()
    {
        //mFusedLocationClient=== OPtion 1:: get last known location
        tryToGetLastLocation();
        //mFusedLocationClient=== OPtion 2:: request location ourself
        if(locationObj==null)
            tryToRequestLocationOurself();

        //lets assume we successfully got the location
        if(locationObj!=null){
            homeLocationSuccessDoWork();
            tryToRequestLocationOurself(); //to get periodic update on location change
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check for the integer request code originally supplied to startResolutionForResult().
        try {
            int mode= android.provider.Settings.Secure.getInt(this.getContentResolver(), Settings.Secure.LOCATION_MODE);
            Log.e("settings mode now:::=",String.valueOf(mode));
            if(mode!=3)
                finish();
            else
                {
                    Log.e("okokok","everything ok");
                    updateLocationInUI();
                }

        } catch (Exception e) {
            Log.e("settings mode=","exception setting not found");
            e.printStackTrace();
        }
        Log.e("on activity result rec=", String.valueOf(requestCode));

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("MissingPermission")
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //changing the position of my location button
        View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
        // position on right bottom
        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        rlp.setMargins(16, 200, 16, 16);

        mMap.setMyLocationEnabled(true);

        mUiSettings = mMap.getUiSettings();
        mUiSettings.setCompassEnabled(true);
        //mUiSettings.setZoomControlsEnabled(true);
        mUiSettings.setAllGesturesEnabled(true);
        Log.e("maps", "onMapReady Callback");

    }



    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, 1)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }



//States of a actvity and menu and navigation item selected methods*********************************
    @Override
    public void onResume() {
        super.onResume();
        navigationView.setCheckedItem(R.id.home_nav_home);
        isInFront = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        isInFront = false;
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
        else if(sendMessageFab.isOpened())
        {
            sendMessageFab.close(true);
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.home_action_logout) {
            //logout...................................................................................................
            Log.e("nav_logout= ","true");
            AlertDialog.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder = new AlertDialog.Builder(con, android.R.style.Theme_Material_Dialog_Alert);
            } else {
                builder = new AlertDialog.Builder(con);
            }
            builder.setTitle("Log out")
                    .setMessage("Are you sure you want to log out? This will delete all your messages from the device")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // continue with delete
                            sharedPrefHandler.logoutUser(con);
                            fileHelper =new FileHelper();
                            fileHelper.deleteFile(con);

                            //set loggedIn bit in database to Zero
                            //do this on a new thread
                            new Thread(new Runnable() {
                                public void run() {                                                     //THREAD 3............
                                    // a potentially  time consuming task
                                    network = new Network(lgot, username, password, "dummy", "00.00","00.00", "dummy",null,null,null,null);
                                    result = network.DoWork();
                                    if (result != null) {
                                        Log.e(TAG, "toast main thread" + result);
                                    }
                                }
                            }).start();
                            Toast.makeText(con, "Logged out Successfully", Toast.LENGTH_LONG).show();

                            // After logout redirect user to going Activity
                            Intent i = new Intent(con, Login.class);
                            con.startActivity(i);
                            finish();

                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            //logout done

            return true;
        }
        else if(id == R.id.home_action_downloadchat)
        {
            //download messages ........................................................................................
            fileHelper = new FileHelper();
            if (fileHelper.emptyFile(con)) {
                new Thread(new Runnable() {
                    public void run() {                                                     //THREAD 3............
                        // a potentially  time consuming task
                        network = new Network(pullMsg, username, password, "dummy", "00.00", "00.00", "dummy", null,null,null,null);
                        result = network.DoWork();
                        if (result != null) {
                            Log.e(TAG, "toast main thread=" + result);
                            //write the results in the file
                            //.................
                            fileHelper.writeFile(con, result);

                        }

                    }
                }).start();
                Toast.makeText(con, "All Messages downloaded", Toast.LENGTH_SHORT).show();
            }
            else
                Toast.makeText(con, "You already have messages", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.home_action_settings) {
            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.home_nav_home) {
            // Handle the camera action
        } else if (id == R.id.home_nav_inbox) {
            // Handle the inbox........................................................................................
            //go to messages/contacts activity
            Intent i = new Intent(con, Inbox.class);
            i.putExtra("sender","dummy");
            i.putExtra("msg","dummy");
            con.startActivity(i);

        } else if (id == R.id.home_nav_sent) {

        } else if (id == R.id.home_nav_tools) {

        }

        else if (id == R.id.home_nav_share) {

        } else if (id == R.id.home_nav_feedback) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}