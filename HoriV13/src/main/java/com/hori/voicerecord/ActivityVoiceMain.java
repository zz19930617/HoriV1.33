package com.hori.voicerecord;

import java.io.File;
import java.io.IOException;

import com.hori.app4ros.AcitivityMain;
import com.hori.app4ros.R;
import com.hori.voicerecord.Sender;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityVoiceMain extends Activity {

   private ExtAudioRecorder myAudioRecorder=null;
   private String outputFile = null;
   private String IP ="192.168.1.102" ;
   private int Port=8000;  
   private Button settingbtn=null;
   private Button callbtn=null;
   private Sender send=null;
   private Handler handler =null;  
   private boolean isShortClick=false;
   private TextView text= null;
   private Toast imageToast=null;
   private Vibrator vibrator;
   
   @Override
   protected void onCreate(Bundle savedInstanceState) {
	   
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_voice);            
      callbtn = (Button)findViewById(R.id.voice_call);   
    //save user input IP into something like configuration file
      SharedPreferences local_IP = getSharedPreferences("local_ip", 0);  
     //local_IP.edit().putString("IPaddr", returnedData).commit(); 
	  IP = local_IP.getString("IPaddr", IP); 
	  //Toast.makeText(ActivityVoiceMain.this, IP, Toast.LENGTH_SHORT).show();
      outputFile = Environment.getExternalStorageDirectory()+File.separator+"audio/";      
      myAudioRecorder = ExtAudioRecorder.getInstanse();    
      vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
      //toast show pic
      imageToast=new Toast(ActivityVoiceMain.this);
      //定义一个ImageView对象
      ImageView imageView=new ImageView(ActivityVoiceMain.this);
      //为ImageView对象设置上去一张图片
      imageView.setImageResource(R.drawable.voice_warning1);
      //将ImageView对象绑定到Toast对象imageToasr上面去
      imageToast.setView(imageView);
      //设置Toast对象显示的时间长短
      imageToast.setGravity(Gravity.TOP , 0, 200);
      imageToast.setDuration(Toast.LENGTH_SHORT);
      
      
      
      
      callbtn.setOnLongClickListener(new OnLongClickListener() {

		@Override
		public boolean onLongClick(View v) {
			// TODO Auto-generated method stub
			isShortClick = false;
			callbtn.setText("Recording...");
			 long[] pattern = { 0, 50, 50, 50 };
			 vibrator.vibrate(pattern ,-1);
			//Toast.makeText(getApplicationContext(), "long",Toast.LENGTH_SHORT).show();
			return false;
		}
    	  
      });
      callbtn.setOnTouchListener(new OnTouchListener(){     	  
    	  @Override
          public boolean onTouch(View view, MotionEvent event) { 
    		 
    		  switch(event.getAction()){
    		 
    		  case MotionEvent.ACTION_DOWN:     
    			  isShortClick = true;
    			  
    				  try {
    					  myAudioRecorder.recordChat(outputFile, "xxx.wav");
    				  } catch (IllegalStateException e) {
    					  // TODO Auto-generated catch block
    					  e.printStackTrace();
    				  }    	
    			  
    		      // Toast.makeText(getApplicationContext(), "Recording started", Toast.LENGTH_SHORT).show();
    		  break; 
    		  
    		  case MotionEvent.ACTION_UP:   
    			  
    				  //Toast.makeText(getApplicationContext(), "up",Toast.LENGTH_SHORT).show();
    			   	  myAudioRecorder.stopRecord();    		      
    				  myAudioRecorder  = null;    		      
    				  //Toast.makeText(getApplicationContext(), "Audio recorded successfully",Toast.LENGTH_SHORT).show();
    				  if(!isShortClick)
					  {
    				 	if(!send.isLink())
    				 	{	      		
    				 		send.ConnectServer(IP, Port);
    				 		if(!send.isLink())
        				 	{	      			
    				 			//弹出dialoag
        				 	}
    				 	}
    				 	else 
    				 	{   	 
    				 		try {    				 			
    				 			send.SendFile(outputFile + "xxx.WAV");    						  
    				 		} catch (IOException e) {
    							  e.printStackTrace();
    				 		}
    				 	}
    				  } 
    				  else{
    					   imageToast.show();
    				  }
    				  callbtn.setText("Please press to record!");
    			  break; 
    		  default:
    			  break;
    		  }    		  
    		  return false;
    	  }
      });       
      
      send = new Sender(new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			switch (msg.what) {
			case 0x03:
				//Toast.makeText(ActivityVoiceMain.this, (String)msg.obj, Toast.LENGTH_SHORT).show();
				break;
			case 0x05:
				//Toast.makeText(ActivityVoiceMain.this, "" + msg.obj, Toast.LENGTH_SHORT).show();
				break;
			default:
				//Toast.makeText(ActivityVoiceMain.this, "" + msg.what, Toast.LENGTH_SHORT).show();
			}
			super.handleMessage(msg);			
		}
      } );      
      send.ConnectServer(IP, Port);    
   }
   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
       // Inflate the menu; this adds items to the action bar if it is present.
       getMenuInflater().inflate(R.menu.menu_voice, menu);
       return true;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
       // Handle action bar item clicks here. The action bar will
       // automatically handle clicks on the Home/Up button, so long
       // as you specify a parent activity in AndroidManifest.xml.   
       int id = item.getItemId();
       if (id == R.id.exitvoice) {
       		finish();       	
       		return true;
       }
       return super.onOptionsItemSelected(item);
   }
   
}
