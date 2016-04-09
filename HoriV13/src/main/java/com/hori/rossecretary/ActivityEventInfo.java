package com.hori.rossecretary;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import com.hori.app4ros.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityEventInfo extends Activity {
	
	public final static int SET_DATE = 1;
	public final static int SET_TIME = 2;

	private int       mIndex;
	private EventItem mEvent;
	
	private EditText  mTitleEdit;
	private EditText  mContentEdit;
	private TextView  mDateEdit;
	private ImageView mDateView;
	private TextView  mTimeEdit;
	private ImageView mTimeView;
	
	
	/*private Spinner              mSpinner;
	private ArrayAdapter<String> mSpinnerAdapter;
	private ArrayList<String>    mSelectionList;*/
	
	private Button    mCancel;
	private Button    mOk;
	
	private ButtonOnClick mOnClick;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_info);

        mTitleEdit   = (EditText) findViewById(R.id.ev_event_title);
        mContentEdit = (EditText) findViewById(R.id.ev_event_content);
        
        mDateEdit    = (TextView) findViewById(R.id.event_date);
        mDateView    = (ImageView) findViewById(R.id.iv_calendar);
        mTimeEdit    = (TextView) findViewById(R.id.event_warning_time);
        mTimeView    = (ImageView) findViewById(R.id.iv_warning_calendar);
        mOnClick  = new ButtonOnClick();
        
        mDateEdit.setOnClickListener(mOnClick);
        mDateView.setOnClickListener(mOnClick);
        mTimeEdit.setOnClickListener(mOnClick);
        mTimeView.setOnClickListener(mOnClick);

        mCancel   = (Button) findViewById(R.id.btn_addevent_cancel);
        mOk       = (Button) findViewById(R.id.btn_addevent_ok);
        mCancel.setOnClickListener(mOnClick);
        mOk.setOnClickListener(mOnClick);

        mIndex  = -1;
        mEvent  = new EventItem(getIntent());
        if (null == mEvent.title) {
        	// Add
        	setTitle("Add Event");
        	mEvent.date = new Date();
        	mDateEdit.setText(mEvent.date.toLocaleString());
        	mTimeEdit.setText(mEvent.time.toLocaleString());
        } else {
        	// Edit
        	setTitle("Edit Event");
        	mIndex  = getIntent().getIntExtra(EventItem.INDEX, -1);
        	mTitleEdit.setText(mEvent.title);
        	mDateEdit.setText(mEvent.date.toLocaleString());
        	mTimeEdit.setText(mEvent.time.toLocaleString());
        	mContentEdit.setText(mEvent.content);
        }
        
        /*mSelectionList = new ArrayList<String>();
        mSelectionList.add("Type0");
        mSelectionList.add("Type1");
        mSelectionList.add("Type2");
        mSelectionList.add("Type3");
        
        mSpinner        = (Spinner) findViewById(R.id.sn_type);
        mSpinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mSelectionList);  
        mSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);  
        mSpinner.setAdapter(mSpinnerAdapter);  
        mSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				
			}
        });*/
    }

    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (SET_DATE == requestCode) {
			switch (resultCode) {
			case RESULT_OK:
				mEvent.date = new Date(data.getLongExtra(EventItem.DATETIME, mEvent.date.getTime()));
				mDateEdit.setText(mEvent.date.toLocaleString());
				break;
			case RESULT_CANCELED:
				break;
			default:
				break;
			}
		} else if (SET_TIME == requestCode) {
			switch (resultCode) {
			case RESULT_OK:
				mEvent.time = new Date(data.getLongExtra(EventItem.DATETIME, mEvent.time.getTime()));
				mTimeEdit.setText(mEvent.time.toLocaleString());
				break;
			case RESULT_CANCELED:
				break;
			default:
				break;
			}
		} else {
			;
		}
		
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void saveEventInfo(Intent i) {
		mEvent.title   = mTitleEdit.getText().toString();
		mEvent.content = mContentEdit.getText().toString();
		mEvent.toIntent(i);
	}

	private class ButtonOnClick implements OnClickListener {

		Intent intent;
		
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.btn_addevent_ok:
				intent = new Intent();
				saveEventInfo(intent);
				intent.putExtra(EventItem.INDEX, mIndex);
				ActivityEventInfo.this.setResult(RESULT_OK, intent);
				
				ActivityEventInfo.this.finish();
				break;
			case R.id.btn_addevent_cancel:
				ActivityEventInfo.this.setResult(RESULT_CANCELED);
				ActivityEventInfo.this.finish();
				break;
			case R.id.iv_calendar:
			case R.id.event_date:
				intent = new Intent(ActivityEventInfo.this, ActivityDatePicker.class);
				intent.putExtra(EventItem.DATETIME, mEvent.date.getTime());
				ActivityEventInfo.this.startActivityForResult(intent, SET_DATE);
				break;
			case R.id.iv_warning_calendar:
			case R.id.event_warning_time:
				intent = new Intent(ActivityEventInfo.this, ActivityDatePicker.class);
				intent.putExtra(EventItem.DATETIME, mEvent.time.getTime());
				ActivityEventInfo.this.startActivityForResult(intent, SET_TIME);
				break;
			default:
				break;
			}
		}
    }
}
