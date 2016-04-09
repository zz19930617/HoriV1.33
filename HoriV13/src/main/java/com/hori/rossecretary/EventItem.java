package com.hori.rossecretary;


import java.util.Date;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class EventItem {

	public final static String FILE_NAME = "EventItems.xml";
	public final static String EVENT_NUM = "event_num";
	
	/* true -- on, false -- off */
	public boolean turn;
	/* true -- me, false -- other */
	public boolean owner;
	/* true -- yes, false -- no */
	public boolean sync;
	/* The title of the event */
	public String  title;
	/* The content of the event */
	public String  content;
	/* The date of the event */
	public Date    date;
	/* The warning time for the event */
	public Date    time;

	public final static String TURN    = "Turn";
	public final static String OWNER   = "Owner";
	public final static String SYNC    = "Sync";
	public final static String TITLE   = "Title";
	public final static String CONTENT = "Content";
	public final static String INDEX   = "Index";
	
	public final static String DATE    = "Date";
	public final static String TIME    = "Time";
	
	public final static String DATETIME= "SetDate";
	
	
	EventItem() {
		turn    = true;
		owner   = true;
		sync    = false;
		title   = null;
		content = null;
		date    = new Date();
		time    = date;
	}
	
	EventItem(boolean t, boolean o, boolean s, String ti, String c, Date d, Date tim) {
		turn    = t;
		owner   = o;
		sync    = s;
		title   = ti;
		content = c;
		date    = d;
		time    = tim;
	}
	
	EventItem(Intent i) {
		turn    = i.getBooleanExtra(TURN, true);
		owner   = i.getBooleanExtra(OWNER, true);
		sync   = i.getBooleanExtra(SYNC, false);
		title   = i.getStringExtra(TITLE);
		content = i.getStringExtra(CONTENT);
		long defaultValue = new Date().getTime();
		date    = new Date(i.getLongExtra(DATE, defaultValue));
		time    = new Date(i.getLongExtra(TIME, defaultValue));
	}
	
	EventItem(SharedPreferences sp, int index) {
		turn    = sp.getBoolean(TURN + index, turn);
		owner   = sp.getBoolean(OWNER + index, owner);
		sync    = sp.getBoolean(SYNC + index, sync);
		title   = sp.getString(TITLE + index, null);
		content = sp.getString(CONTENT + index, null);
		long defaultValue = new Date().getTime();
		date    = new Date(sp.getLong(DATE + index, defaultValue));
		time    = new Date(sp.getLong(TIME + index, defaultValue));
	}
	
	public void toIntent(Intent i) {
		i.putExtra(TURN, turn);
		i.putExtra(OWNER, owner);
		i.putExtra(SYNC, sync);
		i.putExtra(TITLE, title);
		i.putExtra(CONTENT, content);
		i.putExtra(DATE, date.getTime());
		i.putExtra(TIME, time.getTime());
	}
	
	public void toLocal(Editor editor, int index) {
		editor.putBoolean(TURN + index, turn);
		editor.putBoolean(OWNER + index, owner);
		editor.putBoolean(SYNC + index, sync);
		editor.putString(TITLE + index, title);
		editor.putString(CONTENT + index, content);
		editor.putLong(DATE + index, date.getTime());
		editor.putLong(TIME + index, time.getTime());
		
		editor.commit();
	}
	
	public void fromLocal(SharedPreferences sp, int index) {
		turn    = sp.getBoolean(TURN + index, turn);
		owner   = sp.getBoolean(OWNER + index, owner);
		sync    = sp.getBoolean(SYNC + index, sync);
		title   = sp.getString(TITLE + index, title);
		content = sp.getString(CONTENT + index, content);
		date    = new Date(sp.getLong(DATE + index, date.getTime()));
		time    = new Date(sp.getLong(TIME + index, time.getTime()));
	}
	
	/**
	 * turn         (1 byte: false -- 0x00, true -- !0x00)
	 * owner        (1 byte: false -- 0x00, true -- !0x00)
	 * sync         (1 byte: false -- 0x00, true -- !0x00)
	 * date         (8 byte: Date.getTime())
	 * time         (8 byte: Date.getTime())
	 * title_size   (4 byte: x)
	 * title        (x byte: String.getBytes())
	 * content_size (4 byte: y)
	 * content      (y byte: String.getBytes())
	 * @return
	 */
	public byte[] getBytes() {
		byte[] bytes = null;
		if (null == title) {
			return bytes;
		}
		
		byte[] titleBytes   = title.getBytes();
		byte[] contentBytes = content.getBytes();
		int bytesSize       = 27 + titleBytes.length + contentBytes.length;
		bytes = new byte[bytesSize];
		int offset = 0;
		// turn
		if (!turn) {
			bytes[offset++] = 0x00;
		} else {
			bytes[offset++] = 0x01;
		}
		// owner
		if (!owner) {
			bytes[offset++] = 0x00;
		} else {
			bytes[offset++] = 0x01;
		}
		// owner
		if (!sync) {
			bytes[offset++] = 0x00;
		} else {
			bytes[offset++] = 0x01;
		}
		// date
		long tmpLong = date.getTime();
		for (int i = 7; i >= 0; --i) {
			bytes[offset + i] = (byte) tmpLong;
			tmpLong >>= 8;
		}
		offset += 8;
		// time
		tmpLong = time.getTime();
		for (int i = 7; i >= 0; --i) {
			bytes[offset + i] = (byte) tmpLong;
			tmpLong >>= 8;
		}
		offset += 8;
		// title_size
		int tmpInt = titleBytes.length;
		for (int i = 3; i >= 0; --i) {
			bytes[offset + i] = (byte) tmpInt;
			tmpInt >>= 8;
		}
		offset += 4;
		// title
		System.arraycopy(titleBytes, 0, bytes, offset, titleBytes.length);
		offset += titleBytes.length;
		// content_size
		tmpInt = contentBytes.length;
		for (int i = 3; i >= 0; --i) {
			bytes[offset + i] = (byte) tmpInt;
			tmpInt >>= 8;
		}
		offset += 4;
		// content
		System.arraycopy(contentBytes, 0, bytes, offset, contentBytes.length);
		offset += contentBytes.length;
		return bytes;
	}
	
	@Override
	public String toString() {
		String str = title;
		str += "\n";
		str += date.toLocaleString();
		str += ("\n" + content);
		return str;
	}
	
	
}
