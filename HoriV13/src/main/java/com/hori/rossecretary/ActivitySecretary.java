package com.hori.rossecretary;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.hori.app4ros.R;
import com.hori.packingcmd.ComUtils;
import com.hori.packingcmd.ComUtils.ConnectThread;
import com.hori.packingcmd.ComUtils.SBBConnectedThread;
import com.hori.packingcmd.ComUtils.ConnectThread.SBBStateChange;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ActivitySecretary extends Activity {

	private final static int     EVENT_ADD  = 1;
	private final static int     EVENT_EDIT = 2;
	
	private int            mLongClickItem = -1;
	
	private View                 mNoEvent;
	
	private ListView             mEventListView;
	private ArrayList<EventItem> mEventList;
	private EventItemAdapter     mEventAdapter;
	private EventOnClickListener mItemListener;
	
	private MenuItem             mWiFi;
	private MenuItem             mSync;
	
	private SharedPreferences    mSp;
	private Editor               mEditor;
	
	private ConnectThread        mConnectThread;
	private ConnectedThread      mConnectedThread;
	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secretary);        
       
        mSp = this.getSharedPreferences(
				EventItem.FILE_NAME, Context.MODE_PRIVATE);
        mEditor = mSp.edit();
        
        mNoEvent   = findViewById(R.id.ll_noevent);

        mEventList = readEventsFromLocal();
        
        mLongClickItem = -1;
        mEventAdapter  = new EventItemAdapter();
        mItemListener  = new EventOnClickListener();
        mEventListView = (ListView) findViewById(R.id.lv_event);
        mEventListView.setAdapter(mEventAdapter);
        mEventListView.setOnItemClickListener(mItemListener);
        mEventListView.setOnItemLongClickListener(mItemListener);
        mNoEvent.setOnClickListener(mItemListener);
        
        mConnectedThread = null;
        mConnectThread   = new ConnectThread();
        mConnectThread.setOnStateChange(new SBBStateChange() {
			@Override
			public void onInitFail() {
				Looper.prepare(); 
				showToast("No~Init Fail!");
				Looper.loop(); 
			}
			@Override
			public void onWaiting() {
//				Looper.prepare();
				showToast("Waiting...");
//				Looper.loop();
			}
			@Override
			public void onConnectFail() {
				Looper.prepare(); 
				showToast("Connect Fail");
				Looper.loop(); 
			}
			@Override
			public void onConnected(Socket s) {
//				Looper.prepare();
//				showToast("Yes! Connected");
//				mWiFi.setEnabled(false);
//				Looper.loop();
				mConnectedThread = new ConnectedThread(s);
			}
        });
        mConnectThread.start();       
    }
	
    
    
    @Override
	protected void onDestroy() {
    	if (null != mConnectThread) {
    		mConnectThread.cancel();
    		mConnectThread = null;
    	}
    	if (null != mConnectedThread) {
    		mConnectedThread.cancel();
    		mConnectedThread = null;
    	}
    	
    	writeEventsToLocal();
    	if (mEditor != null) {
    		mEditor = null;
    	}
    	if (null != mSp) {
    		mSp = null;
    	}
    	
		super.onDestroy();
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_secretary, menu);
        mWiFi = menu.findItem(R.id.secreary_link);
        mSync = menu.findItem(R.id.secreary_sync);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
        case R.id.secreary_exit:
        	finish();
        	break;
        case R.id.secreary_sync:
        	item.setEnabled(false);
        	new Timer().schedule(new TimerTask() {
				@Override
				public void run() {
					ActivitySecretary.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
				        	if (syncEvent()) {
				        		showToast("Sync Successful");
				        	}
				        	else {
				        		showToast("Sync UnSuccessful");
				        	}
				        	mSync.setEnabled(true);
						}
					});
				}
        	}, 3000);
        	break;
       	default:
       		break;
        }
        return super.onOptionsItemSelected(item);
    }
    
    public void writeEventsToLocal() {
    	// send Event to remote
    	syncEvent();
    	
    	if ((null == mEditor) || (null == mSp)
    			|| (null == mEventList)) {
    		return;
    	}
    	// Clear the old data
    	mEditor.clear().commit();
    	
    	mEditor.putInt(EventItem.EVENT_NUM, mEventList.size());
    	mEditor.commit();
    	
    	int       count = 0;
    	EventItem minItem;
    	EventItem item;
    	while (!mEventList.isEmpty()) {
    		int index = 0;
    		minItem = mEventList.get(index);
    		for (; index < mEventList.size(); ++index) {
    			item = mEventList.get(index);
    			if (minItem.date.getTime() > item.date.getTime()) {
    				minItem = item;
    			}
    		}
    		
    		minItem.toLocal(mEditor, count++);
    		mEventList.remove(minItem);
    	}
    }
    
    public ArrayList<EventItem> readEventsFromLocal() {
    	ArrayList<EventItem> list = new ArrayList<EventItem>();
    	int count = mSp.getInt(EventItem.EVENT_NUM, 0);
    	for (int i = 0; i < count; ++i) {
    		list.add(new EventItem(mSp, i));
    	}
    
    	// send Event to remote
//    	syncEvent();
    	
    	return list;
    }
    //读取本地.txt文件后发送到机器人端后，机器人返回确认消息作为波尔返回值
    public boolean syncEvent() {
    	Log.i("D", "1");
    	if ((null != mConnectedThread) && (null != mEventList)) {
    		Log.i("D", "2");
	    	for (EventItem i : mEventList) {
	    		Log.i("D", "3");
	    		if (!i.sync) {
	    			Log.i("D", "4");
	    			mConnectedThread.write(
	    					ComUtils.packagingCommand(0, i.getBytes()));
	    		}
	    	}
	    	return true;
    	} else {
    		return false;
    	}
    }
    
    public boolean syncEvent(EventItem i) {
    	if ((null != mConnectedThread) && (null != mEventList)
    			&& (!i.sync)) {
			mConnectedThread.write(
					ComUtils.packagingCommand(0, i.getBytes()));
			return true;
    	} else {
    		return false;
    	}
    }
    
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	EventItem item;
    	if (EVENT_ADD == requestCode) {
			switch (resultCode) {
			case RESULT_OK:
				item = new EventItem(data);
				mEventList.add(item);
				mEventAdapter.notifyDataSetChanged();
				break;
			case RESULT_CANCELED:
				break;
			default:
				showToast("Something was wrong!");
				break;
			}
		} else if (EVENT_EDIT == requestCode) {
			switch (resultCode) {
			case RESULT_OK:
				item = new EventItem(data);
				int index = data.getIntExtra(EventItem.INDEX, -1);
				if (-1 == index) {
					showToast("Something was wrong!");
				} else {
					mEventList.set(index, item);
					mEventAdapter.notifyDataSetChanged();
				}
				break;
			case RESULT_CANCELED:
				break;
			default:
				showToast("Something was wrong!");
				break;
			}
		} else {
			;
		}
    	super.onActivityResult(requestCode, resultCode, data);
	}
    
    private void showToast(String msg) {
    	Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

	private class EventOnClickListener implements OnItemClickListener,
						OnClickListener, OnItemLongClickListener {

    	private Intent intent;
    	
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			EventItem event = mEventList.get(position);
			intent = new Intent(ActivitySecretary.this, ActivityEventInfo.class);
			event.toIntent(intent);
			intent.putExtra(EventItem.INDEX, position);
			ActivitySecretary.this.startActivityForResult(intent, EVENT_EDIT);
		}

		@Override
		public void onClick(View v) {
			intent = new Intent(ActivitySecretary.this, ActivityEventInfo.class);
			ActivitySecretary.this.startActivityForResult(intent, EVENT_ADD);
		}

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id) {
			mLongClickItem = position;
			new AlertDialog.Builder(ActivitySecretary.this) 
			.setTitle("Delete")
			.setMessage("Delete?")
			.setPositiveButton("Yes", new  DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (mLongClickItem != -1) {
						mEventList.remove(mLongClickItem);
						mEventAdapter.notifyDataSetChanged();
					} else {
						showToast("Something was wrong!");
					}
					mLongClickItem = -1;
				}
			})
			.setNegativeButton("No", null)
			.show();
			return true;
		}
    	
    }
    
    private class EventItemAdapter extends BaseAdapter {
    	
        private LayoutInflater mmInflater;

        
        public EventItemAdapter() {
        	mmInflater = LayoutInflater.from(ActivitySecretary.this);
        }
        
    	@Override
    	public int getCount() {
    		return mEventList.size();
    		
    	}

    	@Override
    	public Object getItem(int position) {
    		return mEventList.get(position);
    	}

    	@Override
    	public long getItemId(int position) {
    		return position;
    	}

    	@Override
    	public View getView(int position, View convertView, ViewGroup parent) {
    		convertView = mmInflater.inflate(R.layout.event_item, null);
    		convertView.setBackgroundResource(R.drawable.event_bg);
    		
    		ImageView turn     = (ImageView) convertView.findViewById(R.id.iv_turn);
    		ImageView owner    = (ImageView) convertView.findViewById(R.id.iv_owner);
    		TextView textView  = (TextView) convertView.findViewById(R.id.tv_item);
    		
    		EventItem item;
    		item = mEventList.get(position);
    		
    		textView.setText(item.toString());
    		turn.setOnClickListener(new OnClickTurn(position));
    		
    		if (item.turn) {
    			turn.setImageResource(R.drawable.event_on);
    		} else {
    			turn.setImageResource(R.drawable.event_off);
    		}
    		
    		if (item.owner) {
    			owner.setImageResource(R.drawable.event_me);
    		} else {
    			owner.setImageResource(R.drawable.event_other);
    		}

            return convertView;
    	}
    	
    }
	
	private class OnClickTurn implements OnClickListener {

		private int mmPosition;
		
		OnClickTurn(int posi) {
			mmPosition = posi;
		}
		
		@Override
		public void onClick(View v) {
			boolean turn = mEventList.get(mmPosition).turn;
			mEventList.get(mmPosition).turn = !turn;
			mEventAdapter.notifyDataSetChanged();
		}
	}
	
	private class ConnectedThread extends SBBConnectedThread {

		public ConnectedThread(Socket s) {
			
			super(s);
		}

		@Override
		public int parseMessage(byte[] buffer, int start, int size) {
			showToast("Recv");
			return size;
		}

		@Override
		public void onDisCounnect() {
			showToast("Disconnect");
		}
		
	}

}
