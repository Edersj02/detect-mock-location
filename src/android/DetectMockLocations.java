package cordova.detect.mock.location;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationListener;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.os.Bundle;
import android.provider.Settings;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import android.location.Location;

import cordova.detect.mock.location.PermissionHelper;

/**
 * This class echoes a string called from JavaScript.
 */
public class DetectMockLocations extends CordovaPlugin implements GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = "DetectMockLocations";

    public static final int START_REQ_CODE = 0;
    public static final int PERMISSION_DENIED_ERROR = 20;
    public static final long UPDATE_INTERVAL = 1000;
    public static final long UPDATE_FASTEST_INTERVAL = UPDATE_INTERVAL / 2;
    protected final static String[] permissions = { Manifest.permission.ACCESS_FINE_LOCATION };

    private Activity activity;
    private Context context;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private CallbackContext callbackContext;

    private boolean sw = false;
    private Dialog dialog;

    String title;
    String msg;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.activity = this.cordova.getActivity();
        this.context = activity.getApplicationContext();
        this.title = args.getString(0);
        this.msg = args.getString(1);
        if (action.equals("initCheckMockLocation")) {
            initCheckMockLocation();
            return true;
        }
        return false;
    }

    private void initCheckMockLocation(){
        if (hasPermisssion()) {
            attachRecorder();
        } else {
            PermissionHelper.requestPermissions(this, START_REQ_CODE, permissions);
        }
    }

    public boolean hasPermisssion() {
        for(String p : permissions)
        {
            if(!PermissionHelper.hasPermission(this, p))
            {
                return false;
            }
        }
        return true;
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                Log.d(TAG, "Permission Denied!");
                PluginResult result = new PluginResult(PluginResult.Status.ERROR, PERMISSION_DENIED_ERROR);
                result.setKeepCallback(true);
                //this.callbackContext.sendPluginResult(result);
                return;
            }
        }
        switch (requestCode) {
            case START_REQ_CODE:
                attachRecorder();
                break;
        }
    }

    private void attachRecorder() {
        if (mGoogleApiClient == null) {
            buildGoogleApiClient();
        } else {
            mGoogleApiClient.connect();
        }
    }

    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient =  new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest()
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(UPDATE_FASTEST_INTERVAL)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    /*
    * GoogleApiClient.ConnectionCallbacks
    * */
    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "- CONNECTED TO GOOGLE PLAY SERVICES API!!!!!!!!!!");
        createLocationRequest();
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }
    /*
    * GoogleApiClient.OnConnectionFailedListener
    * */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(
                context,
                "Code connection error:" + connectionResult.getErrorCode(),
                Toast.LENGTH_LONG)
                .show();
    }

    /*
    * LocationListener
    * */
    @Override
    public void onLocationChanged(Location location) {
        Log.i("MOCK_LOCATION", "onHandleIntent " + location.getLatitude() + ", " + location.getLongitude());
        if (enabledMockLocation(location)){
            Log.d("MOCK_LOCATION", "MOCK LOCATION IS ENABLED");
            if (!sw) {
                this.dialog = showAlert();
                this.dialog.show();
                sw = true;
            }
        } else {
            Log.d("MOCK_LOCATION", "MOCK LOCATION IS NOT ENABLED");
        }
    }

    /**
     * enabledMockLocation
     * @return true if mock location is enabled, false if is not enabled
     */
    private boolean enabledMockLocation(Location location) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Log.d("MOCK_LOCATION", Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION));
            return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION)
                    .equals("1");
        } else {
            return location != null && location.isFromMockProvider();
        }
    }

    private Dialog showAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.activity);
        builder.setTitle(this.title)
                .setCancelable(false)
                .setMessage(this.msg)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        System.exit(0);
                    }
                });
        return builder.create();
    }
    
}
