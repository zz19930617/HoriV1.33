package com.hori.voicerecord;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hori.app4ros.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class IpSettingActivity extends Activity {
	private Button okbtn=null; 
	private EditText ipedit=null;
	private Toast imageToast=null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.ip_setting);
	
	okbtn=(Button)findViewById(R.id.ip_setting_ok);
	ipedit=(EditText)findViewById(R.id.edit_text);
	initView();  
	
	okbtn.setOnClickListener(new OnClickListener() {
    
   		@Override
   		public void onClick(View v) {
   			imageToast=new Toast(IpSettingActivity.this);
   	      //定义一个ImageView对象
   	      ImageView imageView=new ImageView(IpSettingActivity.this);
   	      //为ImageView对象设置上去一张图片
   	      imageView.setImageResource(R.drawable.ip_warning);
   	      //将ImageView对象绑定到Toast对象imageToasr上面去
   	      imageToast.setView(imageView);
   	      //设置Toast对象显示的时间长短
   	      imageToast.setGravity(Gravity.TOP , 0, 350);
   	      imageToast.setDuration(Toast.LENGTH_SHORT);
   	      
   			//get the string from edittext then return to last activity
   			String ip = ipedit.getText().toString();  
   			//decide if ip is valid ip address
   			Pattern pattern = Pattern.compile("\\b((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\b");
   			Matcher matcher = pattern.matcher(ip); //ä»¥éªŒè¯?127.400.600.2ä¸ºä¾‹
   			if(!matcher.matches()){
   				imageToast.show();
   			}
   			else{
//   				//return ip data to MainActivity
//   				Intent intent = new Intent();
//   				intent.putExtra("data_return",ip );
//   				setResult(RESULT_OK,intent);
   			
   				SharedPreferences localip = getSharedPreferences("local_ip", 0);  
   				localip.edit().putString("IPaddr", ip).commit(); 
   				finish(); 	
   			}
   		}
      });	
	}
	
	@Override
	public void onBackPressed(){
		AlertDialog.Builder dialog =new AlertDialog.Builder(IpSettingActivity.this);
		dialog.setTitle("Leaving Ip setting unsaved!");
		dialog.setMessage("Are you sure to leave?");
		dialog.setCancelable(false);
		dialog.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {		
				// TODO Auto-generated method stub
				
			}
		});
		dialog.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {				
				// TODO Auto-generated method stub
				finish();
			}
		});
		dialog.show();	
		
	}
	private void initView() {  
		SharedPreferences localip = getSharedPreferences("local_ip", 0);  
		String init_ip = localip.getString("IPaddr", ""); 
		ipedit.setText(init_ip);
	}
}
