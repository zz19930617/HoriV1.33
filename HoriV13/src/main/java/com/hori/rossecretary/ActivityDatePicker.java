package com.hori.rossecretary;

import java.util.Date;

import com.hori.app4ros.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TimePicker;

public class ActivityDatePicker extends Activity {

	private Date       mDate;
	
	private DatePicker mDatePicker;
	private TimePicker mTimePicker;
	
	private Button     mCancel;
	private Button     mOk;
	
	private OnClick    mOnClick;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_date);
        
        mDate       = new Date(
        		getIntent().getLongExtra(EventItem.DATETIME, new Date().getTime()));
        
        mDatePicker = (DatePicker) findViewById(R.id.dp_date);
        mTimePicker = (TimePicker) findViewById(R.id.tp_time);
        mTimePicker.setIs24HourView(true);
        
        mDatePicker.updateDate(mDate.getYear() + 1900,
        		mDate.getMonth(), mDate.getDate());
        mTimePicker.setCurrentHour(mDate.getHours());
        mTimePicker.setCurrentMinute(mDate.getMinutes());
        
        mCancel = (Button) findViewById(R.id.btn_date_cancel);
        mOk     = (Button) findViewById(R.id.btn_date_ok);
        mOnClick = new OnClick();
        
        mCancel.setOnClickListener(mOnClick);
        mOk.setOnClickListener(mOnClick);
    }
    
    private class OnClick implements OnClickListener {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.btn_date_cancel:
				ActivityDatePicker.this.setResult(RESULT_CANCELED);
				ActivityDatePicker.this.finish();
				break;
			case R.id.btn_date_ok:
				mDate.setYear(mDatePicker.getYear() - 1900);
				mDate.setMonth(mDatePicker.getMonth());
				mDate.setDate(mDatePicker.getDayOfMonth());
				mDate.setHours(mTimePicker.getCurrentHour());
				mDate.setMinutes(mTimePicker.getCurrentMinute());
				
				Intent intent = new Intent();
				intent.putExtra(EventItem.DATETIME, mDate.getTime());
				ActivityDatePicker.this.setResult(RESULT_OK, intent);
				ActivityDatePicker.this.finish();
				break;
			default:
				break;
			}
		}
    	
    }
}
