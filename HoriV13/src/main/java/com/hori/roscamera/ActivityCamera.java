package com.hori.roscamera;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.apache.http.conn.util.InetAddressUtils;

import com.hori.app4ros.R;
import com.hori.roscamera.CameraLisenter;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

public class ActivityCamera extends Activity {

	private final static String TAG = "CameraActivity";
	
	private View           mButtons;
	private Button         mExit;
	private Button         mConnect;
	private Button         mSwitch;
	private Button         mAddr;
	private SurfaceView    mSurfaceView;
	private CameraLisenter mCallback;
	
    private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			/*switch (msg.what)
			{
			case 0:
				showToast("Waiting...");
				break;
			case 1: // Link Successful
				showToast("Connected!");
				if (null != mLinkItem) {
					mLinkItem.setIcon(R.drawable.ic_bt_connected);
				} else {
					mIsLink = true;
				}
				break;
			case 2:
				if (null != mLinkItem) {
					mLinkItem.setIcon(R.drawable.ic_bt_available);
				} else {
					mIsLink = false;
				}
				break;
			}*/
		}
    	
    };

	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); 
        
        mCallback = new CameraLisenter(mHandler);
        
        mButtons     = findViewById(R.id.ll_buttons);
        mExit        = (Button) findViewById(R.id.btn_exit);
        mConnect     = (Button) findViewById(R.id.btn_connect);
        mSwitch      = (Button) findViewById(R.id.btn_switch);
        mAddr        = (Button) findViewById(R.id.btn_addr);
        mSurfaceView = (SurfaceView) findViewById(R.id.playwnd);
        mSurfaceView.setOnClickListener(mOnClickListener);
        mSurfaceView.getHolder().addCallback(mCallback);
        
        mExit.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mCallback.cancel();
				ActivityCamera.this.finish();
			}
        });
        
        mSwitch.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mCallback.switchCamera();
			}
        	
        });
        
        String ip = getLocalHostIp();
        if (null != ip) {
        	mAddr.setText(ip + ": 6666");
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
	protected void onDestroy() {
		super.onDestroy();
	}

	private void showToast(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}
	
	public String getLocalHostIp() {
        String ipaddress = null;
        try {
            Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces();
            while (en.hasMoreElements())
            {
                NetworkInterface nif = en.nextElement();
                Enumeration<InetAddress> inet = nif.getInetAddresses();
                while (inet.hasMoreElements())
                {
                    InetAddress ip = inet.nextElement();
                    if (!ip.isLoopbackAddress()
                            && InetAddressUtils.isIPv4Address(ip
                                    .getHostAddress()))
                    {
                        return ipaddress = ip.getHostAddress();
                    }
                }

            }
        }
        catch (SocketException e)
        {
            Log.e("feige", "Error");
            e.printStackTrace();
        }
        return ipaddress;
    }
	
	private OnClickListener mOnClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (View.VISIBLE == mButtons.getVisibility())
				mButtons.setVisibility(View.GONE);
			else
				mButtons.setVisibility(View.VISIBLE);
		}
    };
    
    
}
