package nitro.quickpass;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import net.gotev.uploadservice.MultipartUploadRequest;
import net.gotev.uploadservice.UploadNotificationConfig;
import net.gotev.uploadservice.UploadService;
import net.gotev.uploadservice.okhttp.OkHttpStack;

import java.io.File;


import okhttp3.OkHttpClient;

/*
    This activity only starts when user select our app in the send/share screen for a file.
 */
public class Upload extends Activity {
    private static final int PERMISSION_REQUEST_CODE = 2;
    Intent receivedIntent;
    File f;
    Uri uri;
    EditText passcode;
    EditText email;
    CheckBox emailSend;
    Button upload;
    Cursor returnCursor;
    boolean isImage = false;
    int nameIndex;
    int sizeIndex;
    int pathIndex;

    String imageName;
    long imageSize;
    String imagePath;

    TextView fileName;
    TextView fileSize;
    TextView filePath;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload);

        /*
        To check if we have permission to read from storage or not
        If we already have permission nothing happens ( checkPermission() will return true.
        If we do not have permission the "requestPermission()" will be called which it will
        request permission from user with a pop up .
         */
        if (Build.VERSION.SDK_INT >= 23)
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

            // Code for Below 23 API Oriented Device
            // Do next code
        }
        //We get the Text Elements ( view) that is on the screen
        //so later we can write into it the file information
        fileName = (TextView)findViewById(R.id.fileName);
        fileSize = (TextView)findViewById(R.id.fileSize);
        filePath = (TextView)findViewById(R.id.filePath);
        //Get the file URL in the local system , this is passed by the OS
        uri = ShareCompat.IntentReader.from(this).getStream();
        if(getIntent().getType().toString().indexOf("image/") ==0){
            returnCursor =
                    getContentResolver().query(uri, null, null, null, null);
            nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
            pathIndex = returnCursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            returnCursor.moveToFirst();

            imageName =returnCursor.getString(nameIndex);
            imageSize = returnCursor.getLong(sizeIndex);
            imagePath= returnCursor.getString(pathIndex);

            returnCursor.close();

            fileName.setText(imageName);
            fileSize.setText(String.format("%.02f",1.0*imageSize/1024.0/1024.0)+" Mb");
            filePath.setText(imagePath);

            isImage = true;
        }
        else{
            /*
                * Get the column indexes of the data in the Cursor,
                * move to the first row in the Cursor, get the data,
                * and display it.
             */


            //Create object file and it points to the file selected by the user.
            f = new File(uri.getPath());

            //Writing the file information in the screen.
            fileName.setText(f.getName());
            fileSize.setText(String.format("%.02f",1.0*f.length()/1024.0/1024.0)+" Mb");
            filePath.setText(f.getPath());
        }



        //Geting these elements
        passcode = (EditText)findViewById(R.id.passCode2);
        email = (EditText)findViewById(R.id.email);
        emailSend = (CheckBox)findViewById(R.id.sendEmail);
        upload = (Button)findViewById(R.id.upload);

        /*
            This is the Upload Lib we gonna use to upload the file
         */
        UploadService.NAMESPACE = BuildConfig.APPLICATION_ID;
        OkHttpClient client = new OkHttpClient(); // create your own OkHttp client
        UploadService.HTTP_STACK = new OkHttpStack(client); // make the library use your own OkHttp client
    }

    /*
        this is called when the Check button is clicked
        Checks if the passcode is not used and unlocks the upload button
     */
    public void verify(View v) throws Exception {
        String code = passcode.getText().toString();
        Toast toast;

        String response = new VerifyCodeAsyncTask().execute(code).get();


        if(response.equals("1")){

            upload.setEnabled(true);
            toast = Toast.makeText(this,"Your Passcode is Good !",Toast.LENGTH_SHORT);
            toast.show();
        }
        else{
            upload.setEnabled(false);
            toast = Toast.makeText(this,"Your Passcode is already Used !",Toast.LENGTH_LONG);
            toast.show();
        }
    }
    /*
        if the checkbox send email is clicked on
        show or hide the email EditText ( Textbox ) .
     */
    public void toggleEmail(View v){
        if(emailSend.isChecked()){
            email.setVisibility(EditText.VISIBLE);
        }
        else{
            email.setVisibility(EditText.INVISIBLE);
        }
    }
    /*
        This uploads the file .it's called by the upload button.
     */
    public void upload(View v){
        try {
            if(isImage){
                String uploadId =
                        new MultipartUploadRequest(this.getApplicationContext(), "https://quickpass.azurewebsites.net/Quick_Pass/NewFileUpload")
                                .addFileToUpload(imagePath, "filename")
                                .setNotificationConfig(new UploadNotificationConfig())
                                .setMaxRetries(2)
                                .addHeader("code", passcode.getText().toString())
                                .addHeader("size", (imageSize/1024) + "")
                                .addHeader("email", returnEmail())
                                .startUpload();
                upload.setVisibility(Button.INVISIBLE);
                passcode.setEnabled(false);
                email.setEnabled(false);
                Toast.makeText(this, "Your upload has been started , you can close this screen", Toast.LENGTH_LONG).show();
            }
            else {


                String uploadId =
                        new MultipartUploadRequest(this.getApplicationContext(), "https://quickpass.azurewebsites.net/Quick_Pass/NewFileUpload")
                                .addFileToUpload(f.getPath(), "filename")
                                .setNotificationConfig(new UploadNotificationConfig())
                                .setMaxRetries(2)
                                .addHeader("code", passcode.getText().toString())
                                .addHeader("size", (f.length() / 1024) + "")
                                .addHeader("email", returnEmail())
                                .startUpload();
                upload.setVisibility(Button.INVISIBLE);
                passcode.setEnabled(false);
                email.setEnabled(false);
                Toast.makeText(this, "Your upload has been started , you can close this screen", Toast.LENGTH_LONG).show();
            }
        } catch (Exception exc) {
            Log.e("AndroidUploadService", exc.getMessage(), exc);
        }
    }
    /*
        Return the entered email or null if the send email checkbox is not checked
     */
    public String returnEmail(){
        if(emailSend.isChecked() && !email.getText().toString().isEmpty()){
            return email.getText().toString();
        }
        else{
            return "1";
        }
    }

    private void requestPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Toast.makeText(this, "Read Your Files so you can upload to quick pass server .", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e("value", "Permission Granted, Now you can Read from local drive .");
                } else {
                    Log.e("value", "Permission Denied, You cannot read from local drive .");
                }
                break;
        }
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

}
