package cn.tycoon.lighttrans;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import cn.tycoon.lighttrans.fileManager.AbstractFilePickerFragment;
import cn.tycoon.lighttrans.fileManager.FilePickerActivity;
import cn.tycoon.lighttrans.ui.activity.SearchDeviceActivity;
import cn.tycoon.lighttrans.utils.ImageUtils;


public class MainActivity extends Activity implements View.OnClickListener {
    private static final int CODE_SD = 0;
    private ImageView mPhoto;
    private Button mBtnReceiver;
    private Button mBtnSender;
    private TextView mTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_user_profile);
        mPhoto = (ImageView) findViewById(R.id.ivUserProfilePhoto);
        mBtnReceiver = (Button) findViewById(R.id.btn_receive);
        mBtnSender = (Button) findViewById(R.id.btn_send);
        mTextView = (TextView)findViewById(R.id.tips_tv);
        mBtnReceiver.setOnClickListener(this);
        mBtnSender.setOnClickListener(this);
        initProfile(this);
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
    }

    @Override
    public void onStop() {
        super.onStop();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if ((CODE_SD == requestCode ) &&
                resultCode == Activity.RESULT_OK) {
            if (data.getBooleanExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE,
                    false)) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    ClipData clip = data.getClipData();
                    StringBuilder sb = new StringBuilder();

                    if (clip != null) {
                        for (int i = 0; i < clip.getItemCount(); i++) {
                            sb.append(clip.getItemAt(i).getUri().toString());
                            sb.append("\n");
                        }
                    }

                    mTextView.setText(sb.toString());
                    gotoSearchActivity();

                } else {
                    ArrayList<String> paths = data.getStringArrayListExtra(
                            FilePickerActivity.EXTRA_PATHS);
                    StringBuilder sb = new StringBuilder();

                    if (paths != null) {
                        for (String path : paths) {
                            sb.append(path);
                            sb.append("\n");
                        }
                    }
                    mTextView.setText(sb.toString());
                }
            } else {
                mTextView.setText(data.getData().toString());
            }
        }
    }

    private void gotoSearchActivity(){
        Intent i = new Intent(MainActivity.this,
                SearchDeviceActivity.class);
        startActivity(i);

    }
}
