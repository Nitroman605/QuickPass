package nitro.quickpass;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;



public class Upload extends Activity {
    Uri receivedUri = null;
    Intent receivedIntent = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload);


        receivedIntent = getIntent();
        receivedUri = receivedIntent.getParcelableExtra(Intent.EXTRA_STREAM);

        Toast toast = Toast.makeText(this,receivedUri.toString(),Toast.LENGTH_SHORT);
        toast.show();
    }




}
