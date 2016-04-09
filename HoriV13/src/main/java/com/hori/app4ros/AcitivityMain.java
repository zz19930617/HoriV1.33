package com.hori.app4ros;

import com.hori.app4ros.R;
import com.hori.roscamera.ActivityCamera;
import com.hori.rossecretary.ActivitySecretary;
import com.hori.voicerecord.ActivityVoiceMain;
import com.hori.voicerecord.IpSettingActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;


public class AcitivityMain extends Activity {

	private onClickButton clickListener;
	
	private Button mRosCamera;
	private Button mRosCalendar;
	private Button mRosVoice;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		
        clickListener = new onClickButton();
        
        mRosCamera = (Button) findViewById(R.id.btn_roscamera);
        mRosCamera.setOnClickListener(clickListener);
        
        mRosCalendar = (Button) findViewById(R.id.btn_roscalendar);
        mRosCalendar.setOnClickListener(clickListener);
        
        mRosVoice = (Button) findViewById(R.id.btn_rosvoice);
        mRosVoice.setOnClickListener(clickListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
    	Intent intent ;
        int id = item.getItemId();
        switch (id){
        case R.id.action_Settings:
        	intent = new Intent(AcitivityMain.this, IpSettingActivity.class);
        	AcitivityMain.this.startActivity(intent);
			break;
        case R.id.secreary_exit:
        	finish();        
        	break;
        default:
        	break;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private class onClickButton implements OnClickListener
    {
    	private Intent intent;
		@Override
		public void onClick(View v) {
			switch (v.getId())
			{
			case R.id.btn_roscamera:
				intent = new Intent(AcitivityMain.this, ActivityCamera.class);
				AcitivityMain.this.startActivity(intent);
				break;
			case R.id.btn_roscalendar:
				intent = new Intent(AcitivityMain.this, ActivitySecretary.class);
				AcitivityMain.this.startActivity(intent);
				break;
			case R.id.btn_rosvoice:
				intent = new Intent(AcitivityMain.this, ActivityVoiceMain.class);
				AcitivityMain.this.startActivity(intent);
				break;	
			default:
				break;
			}
		}
    }
}
