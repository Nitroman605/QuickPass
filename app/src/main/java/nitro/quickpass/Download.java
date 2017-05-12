package nitro.quickpass;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

public class Download extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
        setupUI(findViewById(R.id.activity_download));
        /*
        To check if we have permission to write to storage or not
        If we already have permission nothing happens ( checkPermission() will return true.
        If we do not have permission the "requestPermission()" will be called which it will
        request permission from user with a pop up .
         */
        if (Build.VERSION.SDK_INT >= 23)//This feature is only supported in android api 23 and above
        {
            if (checkPermission())
            {
                System.out.println("Premession is granted !");
            } else {
                requestPermission(); // Code for permission
            }
        }
        else
        {
            //This deivce api is lower than 23 which mean declaring permission in the
            //Android Manifest is enough .
        }

    }




    /*
        This function will be called when the Download Button is clicked
        and Here is what happens :
        1-We take the entered passcode by findViewById .
        2-We call an Async task called "GetSessionIDAsyncTask()" which will connect
        to the server for creating the session and will return the session ID .
        Please note this is an Async Task and it's a must for this kind of tasks
        which is works in the background in another thread so the UI does not freaze.

        3-if the session ID (res for response) is "1" that means the passcode is wrong
        therefor a toast will pop up saying that .

        4-if it's not then we proceed to download the file using the DownloadManager lib

     */
    public void download(View v) throws Exception{
        EditText passcode = (EditText)findViewById(R.id.passCode);
        String res = new GetSessionIDAsyncTask().execute(passcode.getText().toString()).get();


        if(res.equals("1")){
            Toast toast = Toast.makeText(this,"Your Passcode does not exist",Toast.LENGTH_SHORT);
            toast.show();
        }
        else {
            String url = "https://quickpass.azurewebsites.net/Quick_Pass/FileServlet/" + res;
            String nameOfFile = new GetFileNameAsyncTask().execute(url).get();
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setDescription("Quick Pass Downloading now");
            request.setTitle(nameOfFile);
            // in order for this if to run, you must use the android 3.2 to compile your app
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            }
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, nameOfFile);

            // get download service and enqueue file
            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            manager.enqueue(request);
            Toast toast = Toast.makeText(this, nameOfFile+" , Has been downloaded", Toast.LENGTH_SHORT);
            toast.show();
        }
    }


    /*
        This to remove the softkeyboard whenever you click
        done on the softkeybaord or you click outside.
     */
    public  void hideSoftKeyboard() {
        InputMethodManager inputMethodManager =
                (InputMethodManager) this.getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(
                this.getCurrentFocus().getWindowToken(), 0);
        EditText passcode = (EditText)findViewById(R.id.passCode);
        passcode.clearFocus();
    }

    /*
        This will run at the start to assign the hideSoftKeyboard to the layout
     */
    public void setupUI(View view) {

        // Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new android.view.View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    hideSoftKeyboard();
                    return false;
                }
            });
        }
        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupUI(innerView);
            }
        }
    }

    /*
        explained above .
     */
    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    private void requestPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(this, "Write External Storage permission allows us to Download from Quick Pass Server , Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e("value", "Permission Granted, Now you can use local drive .");
                } else {
                    Log.e("value", "Permission Denied, You cannot use local drive .");
                }
                break;
        }
    }

}