
package jp.co.c_lis.adktest;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Menu;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

public class MainActivity extends Activity implements OnCheckedChangeListener {
    private static final String LOG_TAG = "AdkTest";

    private static final String ACTION_USB_PERMISSION = "jp.co.c_lis.adktest.action.USB_PERMISSION";

    private UsbManager mUsbManager;
    private PendingIntent mPermissionIntent;
    private boolean mPermissionRequestPending;

    private UsbAccessory mAccessory;

    private ToggleButton mLed = null;
    private TextView mLabel = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLed = (ToggleButton) findViewById(R.id.toggle_led);
        mLed.setEnabled(false);
        mLed.setOnCheckedChangeListener(this);

        mLabel = (TextView) findViewById(R.id.tv_label);

        mUsbManager = UsbManager.getInstance(this);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
                ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(mUsbReceiver, filter);

        // 現在は非推奨。Fragment#setRetainInstance(boolean)を使う
        Object obj = getLastNonConfigurationInstance();
        if (obj != null) {
            mAccessory = (UsbAccessory) obj;
            openAccessory(mAccessory);
        }
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbAccessory accessory = UsbManager.getAccessory(intent);
                    if (intent.getBooleanExtra(
                            UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        openAccessory(accessory);
                    } else {
                        Log.d(LOG_TAG, "permission denied for accessory "
                                + accessory);
                    }
                    mPermissionRequestPending = false;
                }
            } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                UsbAccessory accessory = UsbManager.getAccessory(intent);
                if (accessory != null && accessory.equals(mAccessory)) {
                    closeAccessory();
                }
            }
        }
    };

    private ParcelFileDescriptor mFileDescriptor;
    private FileInputStream mInputStream;
    private FileOutputStream mOutputStream;

    private void openAccessory(UsbAccessory accessory) {
        mFileDescriptor = mUsbManager.openAccessory(accessory);
        if (mFileDescriptor != null) {
            mAccessory = accessory;
            FileDescriptor fd = mFileDescriptor.getFileDescriptor();
            mInputStream = new FileInputStream(fd);
            mOutputStream = new FileOutputStream(fd);
            Thread thread = new Thread(mDataReceiveRunnable);
            thread.start();
            Log.d(LOG_TAG, "accessory opened");
            enableControls(true);
        } else {
            Log.d(LOG_TAG, "accessory open fail");
        }
    }

    private void closeAccessory() {
        enableControls(false);

        try {
            if (mFileDescriptor != null) {
                mFileDescriptor.close();
            }
        } catch (IOException e) {
        } finally {
            mInputStream = null;
            mOutputStream = null;
            mFileDescriptor = null;
            mAccessory = null;
        }
        Log.d(LOG_TAG, "accessory closed");
    }

    protected void enableControls(boolean enable) {
        mLed.setEnabled(enable);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mInputStream != null && mOutputStream != null) {
            return;
        }

        UsbAccessory[] accessories = mUsbManager.getAccessoryList();
        UsbAccessory accessory = (accessories == null ? null : accessories[0]);
        if (accessory != null) {
            if (mUsbManager.hasPermission(accessory)) {
                openAccessory(accessory);
            } else {
                synchronized (mUsbReceiver) {
                    if (!mPermissionRequestPending) {
                        mUsbManager.requestPermission(accessory, mPermissionIntent);
                        mPermissionRequestPending = true;
                    }
                }
            }
        } else {
            Log.d(LOG_TAG, "mAccessory is null");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        closeAccessory();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mUsbReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    private int mCdsValue = 0;
    
    private final Runnable mDataReceiveRunnable = new Runnable() {
        public void run() {
            int len = 0;
            byte[] buff = new byte[1];

            try {
                while (len >= 0) {
                    len = mInputStream.read(buff);
                    mCdsValue = ((int) buff[0] & 0xff);
                    mHandler.sendEmptyMessage(HANDLE_RECEIVE_MESSAGE);

                    synchronized (this) {
                        try {
                            wait(1000 / 8);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "IOException", e);
            }
        }
    };

    private static final int HANDLE_RECEIVE_MESSAGE = 0x01;
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLE_RECEIVE_MESSAGE:
                    mLabel.setText("明るさ: " + mCdsValue);
                    break;
            }
        }
    };

    private void setLed(boolean on) {
        try {
            mOutputStream.write(on ? 0x1 : 0x0);
            mOutputStream.flush();
        } catch (IOException e) {
            Log.e(LOG_TAG, "IOException", e);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.toggle_led:
                setLed(isChecked);
                break;
        }
    }

}
