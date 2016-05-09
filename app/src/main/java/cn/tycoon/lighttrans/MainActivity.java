package cn.tycoon.lighttrans;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import cn.tycoon.lighttrans.fileManager.AbstractFilePickerFragment;
import cn.tycoon.lighttrans.fileManager.FilePickerActivity;
import cn.tycoon.lighttrans.utils.ImageUtils;


public class MainActivity extends Activity implements View.OnClickListener {
    private static final int CODE_SD = 0;
    private ImageView mPhoto;
    private Button mBtnReceiver;
    private Button mBtnSender;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_user_profile);
        mPhoto = (ImageView) findViewById(R.id.ivUserProfilePhoto);
        mBtnReceiver = (Button) findViewById(R.id.btn_receive);
        mBtnSender = (Button) findViewById(R.id.btn_send);
        mBtnReceiver.setOnClickListener(this);
        mBtnSender.setOnClickListener(this);
        initProfile(this);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    private void initProfile(Context c) {
        Drawable temp = c.getResources().getDrawable(R.drawable.ic_cake);
        Bitmap afterTrans = ImageUtils.drawableToBitmap(temp);
        Bitmap finalBitmap = ImageUtils.transform(afterTrans);
        Drawable finalDrawable = ImageUtils.bitmapToDrawable(finalBitmap);
        mPhoto.setBackground(finalDrawable);
        afterTrans.recycle();
        //finalBitmap.recycle();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_receive:
                Intent i = new Intent(MainActivity.this,
                        FilePickerActivity.class);
                i.setAction(Intent.ACTION_GET_CONTENT);

                i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, true);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
                i.putExtra(FilePickerActivity.EXTRA_MODE, AbstractFilePickerFragment.MODE_FILE);
                startActivityForResult(i, CODE_SD);
                break;
            case R.id.btn_send:
                Intent j = new Intent(MainActivity.this,
                        FilePickerActivity.class);
                j.setAction(Intent.ACTION_GET_CONTENT);

                j.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, true);
                j.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
                j.putExtra(FilePickerActivity.EXTRA_MODE, AbstractFilePickerFragment.MODE_FILE_AND_DIR);
                startActivityForResult(j, CODE_SD);
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://cn.tycoon.lighttrans/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://cn.tycoon.lighttrans/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }
}
