package com.hori.packingcmd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.util.Log;

/**
 * Command:    head(0x53) + index(0x11 0x01) + data_size + data + crc16 + end(0x54)
 * encode:     contain(index, data_size, data)
 *             0x52 -- 0x52 0x00
 *             0x53 -- 0x52 0x01
 *             0x54 -- 0x52 0x02
 */
public class ComUtils {
	
	private final static boolean D  = true;
	private final static String TAG = "PackingCmd";
	
	/**	The minimum size of Command	*/
	public static final int CMD_MIN_SIZE = 10;
	/**	The maximum size of Command allow send	*/
	public final static int CMD_MAX_SIZE = 2048;
	
    /**	The Size(byte) Of Head	*/
	public final static int HD_SIZE = 1;
	/**	The Size(byte) Of Index	*/
	public final static int ID_SIZE = 2;
	/**	The Size(byte) Of Data Size	*/
	public final static int DS_SIZE = 2;
	/**	The Size(byte) Of Check	*/
	public final static int CK_SIZE = 2;
	/**	The Size(byte) Of End	*/
	public final static int ED_SIZE = 1;
	
	/**	TimeStamp Size	*/
	public final static int TS_SIZE = 4;
	
	/**	The Head Code	*/
	public final static byte HEAD_CODE = 0x54;
	/**	The End Code	*/
	public final static byte END_CODE = 0x53;
	
	/**
	 * Index Code Array.
	 */
	public final static byte[][] INDEX_ARRAY = 
		{{0x11, 0x02}, {0x11, 0x03}, {0x11, 0x04}, {0x11, 0x05}, {0x11, 0x06},
		 {0x11, 0x07}, {0x11, 0x08}, {0x11, 0x09}, {0x11, 0x0A}, {0x11, 0x0B},
		 {0x11, 0x0C}, {0x11, 0x0D}, {0x11, 0x0E}, {0x11, 0x0F}, {0x11, 0x10},
		 {0x12, 0x01}, {0x12, 0x02}, {0x12, 0x10}, {0x12, 0x11}, {0x12, 0x12},
		 {0x12, 0x13}, {0x12, 0x14}, {0x12, 0x15}, {0x12, 0x16}, {0x12, 0x17},
		 {0x12, 0x18}, {0x12, 0x19}, {0x12, 0x1A}, {0x12, 0x1B}, {0x12, 0x1C},
		 {0x12, 0x20}, {0x12, 0x21}, {0x12, 0x1B}, {0x12, 0x1C}, {0x12, 0x1D},
		 {0x12, 0x1E}, {0x12, 0x1F}, {0x12, 0x22}, {0x21, 0x02}, {0x22, 0x02},
		 {0x22, 0x03}};
	
	
	public static class ConnectThread extends Thread {
		private final static String TAG = "ConnectThread";
		
		private int                 mPort;
		
		private SBBStateChange      mStateChange;
		
		private boolean				mIsRun;
		
		private ServerSocket		mServerSocket;
		
		public ConnectThread() {
			mPort        = 6666;
			mIsRun       = false;
			mStateChange = null;
		}
		
		public ConnectThread(int port) {
			mPort        = port;
			mIsRun       = false;
			mStateChange = null;
		}
		
		public void setOnStateChange(SBBStateChange change) {
			mStateChange = change;
		}
		
		@Override
		public void run() {
			if (!initialize()) {
				if (D) Log.i(TAG, "Init Fail!");
				if (null != mStateChange) mStateChange.onInitFail();
				cancel();
				return;
			}
			
			if (D) Log.i(TAG, "Start accept");
			while (mIsRun) {
				// clear();
				Socket tmpSocket;
				try {
//					if (null != mStateChange) mStateChange.onWaiting();
					tmpSocket = mServerSocket.accept();
				} catch (IOException e) {
					e.printStackTrace();
					if (D) Log.i(TAG, "accept Fail!");
					if (null != mStateChange) mStateChange.onConnectFail();
					continue;
				}
				
				if (null != mStateChange)
					if (null != mStateChange) mStateChange.onConnected(tmpSocket);
				if (D) Log.i(TAG, "accept Success!");

			}
		}

		public void cancel() {
			mIsRun = false;

			if (mServerSocket != null) {
				try {
					mServerSocket.close();
	    		} catch (IOException e) {
	    			e.printStackTrace();
	    		} finally {
	    			mServerSocket = null;
	    		}
			}

			if (D) Log.i(TAG, "ConnectThread Cancel");
		}
		
		private boolean initialize() {
			try {
				mServerSocket = new ServerSocket(mPort);
			} catch (IOException e) {
				e.printStackTrace();
				if (D) Log.i(TAG, "ServerSocket Fail!");
				mIsRun = false;
				return false;
			}
			
			mIsRun = true;
			return true;
		}
		
		public interface SBBStateChange {
			/**
			 * When the Thread are initialized unsuccessfully,
			 * the method will be call. 
			 */
			public void onInitFail();
			
			/**
			 * When we are waiting the other device,
			 * the method will be call. 
			 */
			public void onWaiting();
			
			/**
			 * When we connect the other device unsuccessfully,
			 * the method will be call. 
			 */
			public void onConnectFail();
			
			/**
			 * When we connect the other device successfully,
			 * the method will be call. 
			 * @param s The socket of the connecting client 
			 */
			public void onConnected(Socket s);
		}
		
	}
	
	public static abstract class SBBConnectedThread extends Thread {
		private final static String TAG = "ConnectedThread";

		private Socket              mClientSocket;
		
		private OutputStream        mOutputStream;
		private InputStream         mInputStream;
		
		private boolean             mIsRun;
		
		private byte[]              mBuffer;

		public SBBConnectedThread(Socket s) {
			mClientSocket = s;
			
			try {
				mOutputStream = mClientSocket.getOutputStream();
				mInputStream  = mClientSocket.getInputStream();
			} catch (IOException e) {
				e.printStackTrace();
				mIsRun = false;
				if (D) Log.i(TAG, "Get OutputStream/InputStream Fail!");
			}
			mIsRun = true;
			if (D) Log.i(TAG, "Got OutputStream And InputStream");
		}
		
		@Override
		public void run() {
			int BUF_SIZE = 1024;
			int bottom   = 0;
			int top      = 0;
			mBuffer     = new byte[BUF_SIZE];

			while (mIsRun) {
				try {
					top += mInputStream.read(mBuffer, top, BUF_SIZE - top);
				} catch (IOException e) {
					e.printStackTrace();
					if (D) Log.i(TAG, "Link Disconnect!");
					onDisCounnect();
					break;
				}
				
				bottom += parseMessage(mBuffer, bottom, top - bottom);
				if (top == BUF_SIZE) {
					System.arraycopy(mBuffer, bottom, mBuffer, 0, top - bottom);
				}
			}
		}

		public void write(byte[] cmd) {
			if (null == mOutputStream) {
				return;
			}
			try {
				mOutputStream.write(cmd);
				mOutputStream.flush();
			} catch (IOException e) {
				onDisCounnect();
				e.printStackTrace();
				cancel();
			}
		}
		
		public void write(byte[] cmd, int offset, int count) {
			if (null == mOutputStream) {
				return;
			}
			try {
				mOutputStream.write(cmd, offset, count);
				mOutputStream.flush();
			} catch (Exception e) {
				onDisCounnect();
				e.printStackTrace();
				cancel();
			}
		}
		
		public void cancel() {
			mIsRun = false;

			if (mOutputStream != null) {
				try {
					mOutputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					mOutputStream = null;
				}
			}
			if (mInputStream != null) {
				try {
					mInputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					mInputStream = null;
				}
			}
			if (mClientSocket != null) {
				try {
					mClientSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					mClientSocket = null;
				}
			}
		}

		public abstract int parseMessage(byte[] buffer, int start, int size);
		public abstract void onDisCounnect();
	}
	
	/**
	 * We have packaged the queue for message what received from devices.
	 * And you can use some interface, for example {@link SBBQueue#addElement(byte[])}
	 * , {@link SBBQueue#getBytes(int)} etc.
	 * @author UESTC-PRMI-Burial
	 * @date 2014-12-15
	 */
	public final static class SBBQueue {
		
		private final static String TAG = "SBBQueue";
		/** The lock for read and write operation */
		private ReadWriteLock mRWLock;
		/** When the data is full, the lock is locked */
		private Lock mRangeLock;
		private Condition mRangeCondition;
		/** What the buffer is used as a temporary place for message is a queue.	*/
		private byte[] mQueue;
		/** The index of the top of {@link BleQueue#mQueue}.	*/
		private int mTop;
		/** The index of the bottom of {@link BleQueue#mQueue}.	*/
		private int mBottom;
		/** The length of {@link BleQueue#mQueue}.	*/
		public int length;
		/** We have been recording the size of queue which has used.	*/
		public int size;
		
		/**
		 * The constructor initalize this queue with the special queue length.
		 * @param queueLen The length of this queue.Fails if queueLen is equal
		 * or lesser than zero.
		 */
		public SBBQueue(int queueLen) {
			if (queueLen <= 0) {
				Log.w(TAG, "The length of queue is wrong!");
				length = 0;
				size = 0;
				return;
			}
			length = queueLen;
			mQueue = new byte[length];
			mTop = 0;
			mBottom = 0;
			size = 0;
			mRWLock = new ReentrantReadWriteLock();
			mRangeLock = new ReentrantLock();
			mRangeCondition = mRangeLock.newCondition();
		}

		/**
		 * Adds the specified data at the end of this queue.
		 * @param data The data to add.
		 */
		public void addElement(byte[] data) {
			if (null == mQueue) {
				return;
			}
			if ((null == data) || (0 == data.length) || (data.length > length)) {
				if (D) Log.i(TAG, "The element is null");
				return;
			}
			
			while (true) {
				if ((data.length + size) > length) {
					mRangeLock.lock();
					try {
						if (D)	Log.i(TAG, "Try Range wait");
						mRangeCondition.await();
						if (D)	Log.i(TAG, "Range Signal");
					} catch (Exception e) {
						e.printStackTrace();
						if (D)	Log.i(TAG, "Range await() is fails.");
					}
					mRangeLock.unlock();
				} else {
					break;
				}
			}
			
			mRWLock.writeLock().lock();	// Write Lock
			
			if ((mBottom + data.length) <= length) {
				System.arraycopy(data, 0, mQueue, mBottom, data.length);
				mBottom += data.length;
			} else {
				int remainder = length - mBottom;
				System.arraycopy(data, 0, mQueue, mBottom, remainder);
				mBottom = data.length - remainder;
				System.arraycopy(data, remainder, mQueue, 0, mBottom);
			}
			mBottom %= length;
			// Now we are calculating the size of data
			size += data.length;
			if (size > length) {	// The maximum of size is equal to the length
				size = length;
				mTop = mBottom;
			}
			
			mRWLock.writeLock().unlock();	// UnLock
			if (D) Log.i(TAG, "Add data successful");
			
		}
		
		/**
		 * Removes the specified length of data at the front of this queue.
		 * @param dataLen The length of data to remove, If the dataLen is greater than
		 * {@link BleQueue#size}, we will {@link BleQueue#clear()}
		 */
		public void removeElement(int dataLen) {
			if (null == mQueue) {
				return;
			}
			if (dataLen <= 0) {
				if (D) Log.i(TAG, "The dataLen is wrong!");
				return;
			}
			if (dataLen >= size) {
				clear();
			} else {
				size -= dataLen;
			}
			
			mTop += dataLen;
			mTop %= length;	// The maximum of mTop is equal to the length
			
			mRangeLock.lock();
			mRangeCondition.signalAll();	// UnLock
			if (D)	Log.i(TAG, "Range signalAll");
			mRangeLock.unlock();

		}
		
		/**
		 * Returns the element at the specified index in this queque.
		 * @param index The index of the element to return.
		 * @return The element at the specified index, Or 0x00 if the index is out of range.
		 */
		public byte getByte(int index) {
			byte element = 0x00;
			if (null == mQueue) {
				return element;
			}
			mRWLock.readLock().lock();	// Read Lock
			
			if (index < size) {
				int position = ((mTop + index) % length);
				element = mQueue[position];
			}
			
			mRWLock.readLock().unlock();	// UnLock
			return element;
		}
		
		/**
		 * Returns the array at the specified length in this queque.
		 * @param arrayLen The length of the array to return.
		 * @return The array at the specified length.But if the arrayLen is greater than
		 * {@link BleQueue#size}, we will return all of the data in this queue,if there
		 * is no data, we will return null.
		 */
		public byte[] getBytes(int arrayLen) {
			byte[] array = null;
			if ((null == mQueue) || (0 == size)) {
				return array;
			}
			
			mRWLock.readLock().lock();	// Read Lock
			
			if (arrayLen >= size) {
				array = new byte[size];
				if ((mTop + size) <= length) {
					System.arraycopy(mQueue, mTop, array, 0, size);
				} else {
					int remainder = length - mTop;
					System.arraycopy(mQueue, mTop, array, 0, remainder);
					System.arraycopy(mQueue, 0, array, remainder, size - remainder);
				}
			} else {
				array = new byte[arrayLen];
				if ((arrayLen + mTop) <= length) {
					System.arraycopy(mQueue, mTop, array, 0, arrayLen);
				} else {
					int remainder = arrayLen + mTop - length;
					System.arraycopy(mQueue, mTop, array, 0, length - mTop);
					System.arraycopy(mQueue, 0, array, length - mTop, remainder);
				}
			}
			
			mRWLock.readLock().unlock();	// UnLock
			
			return array;
		}
		
		/**
		 * Returns the array included all of the data in this queque.
		 * @return We will return all of the data in this queue,if there
		 * is no data, we will return null.
		 */
		public byte[] getAllBytes() {
			byte[] array = null;
			if ((null == mQueue) || (0 == size)) {
				return array;
			}
			
			mRWLock.readLock().lock();	// Read Lock
			
			array = new byte[size];
			if ((mTop + size) <= length) {
				System.arraycopy(mQueue, mTop, array, 0, size);
			} else {
				int remainder = length - mTop;
				System.arraycopy(mQueue, mTop, array, 0, remainder);
				System.arraycopy(mQueue, 0, array, remainder, size - remainder);
			}
			
			mRWLock.readLock().unlock();	// UnLock
			
			return array;
		}
		
		/**
		 * If you want to know whether is empty about this queue, you can call this funcion.
		 * @return True if this queue is empty, Or false if not;
		 */
		public boolean isEmpty() {
			return (size == 0);
		}
		
		/**
		 * Removes all elements from this queue, leaving it empty.
		 */
		public void clear() {
			if (mQueue != null) {
				mQueue = null;
				mQueue = new byte[length];
			}
			size = 0;
			mTop = 0;
			mBottom = 0;
		}
		
		/**
		 * Destroys this queue.
		 */
		public void destroy() {
			if (mQueue != null) {
				mQueue = null;
			}
			length = 0;
			size = 0;
			mTop = 0;
			mBottom = 0;
		}
	
	}
	
	/**
	 * We have packaged the command what send to device.
	 * @author UESTC-PRMI-Burial
	 * @date 2014-12-19
	 */
	public static class SBBCommand {
		/** The whole command bytes after decode */
		private byte[] cmdBytes = null;
		/** The Head Code	*/
		public byte head = 0x00;
		/** The Index Of Command	*/
		public int index = -1;
		/** The Data	*/
		public byte[] data = null;
		/** The End Code	*/
		public byte end = 0x00;
		/** The accuracy of the message	*/
		public boolean isRight = false;
		
		public static int reSendCount = 0;
		
		/**
		 * The Constructor for SBBCommand
		 * @param command The whole command. You should to be careful that
		 * the command must be the whole command.Or Something will be wrong. 
		 */
		public SBBCommand(byte[] command) {
			int offset = 0;
			byte[] temp = null;
			
			cmdBytes = command;
			
			command = decoding(command);

			// head
			head = command[offset++];
			// Index
			index = -1;
			temp = new byte[ID_SIZE];
			System.arraycopy(command, offset, temp, 0, ID_SIZE);
			for (int i = 0; i < INDEX_ARRAY.length; ++i) {
				if ((INDEX_ARRAY[i][0] == temp[0])
						&& (INDEX_ARRAY[i][1] == temp[1])) {
					index = i;
					break;
				}
			}
			offset += ID_SIZE;
			// data length
			temp = new byte[DS_SIZE];
			System.arraycopy(command, offset, temp, 0, DS_SIZE);
			int dataLength = Hex2Deci(temp, false);
			offset += DS_SIZE;
			// data
			if ((dataLength < 0) || (dataLength > CMD_MAX_SIZE)) {
				dataLength = -1;
				data = null;
			} else if (dataLength == 0){
				data = null;
			} else {
				data = new byte[dataLength];
				System.arraycopy(command, offset, data, 0, dataLength);
				offset += dataLength;
			}

			// crc
			temp = getCrc16(command, HD_SIZE,
					command.length - 1);
			
			// is right
			isRight = ((null != temp) && (temp[0] == command[offset++]) && (temp[1] == command[offset++])
					&& (index != -1) && (dataLength != -1));
			
			// end
			end = command[offset];
		}
		
		/**
		 * The copy constructor for SBBCommand
		 * @param cmd The SBBCommand instance to copy
		 */
		public SBBCommand(SBBCommand cmd) {
			if (cmd == null) {
				Log.w(TAG, "The command is null!");
				return;
			}
			cmdBytes = cmd.cmdBytes;
			head = cmd.head;	// head
			index = cmd.index;
			if ((null == cmd.data) || (0 == cmd.data.length)) {
				data = null;
			} else {
				data = new byte[cmd.data.length];
				System.arraycopy(cmd.data, 0, data, 0, data.length);
			}
			end = cmd.end;
			isRight = cmd.isRight;
		}

		/**
		 * Return the bytes about command
		 * @return Return the array of bytes
		 */
		public byte[] getBytes() {
			++reSendCount;
			return cmdBytes;
		}
		
		@Override
		public String toString() {
			final StringBuilder stringBuilder = new StringBuilder(cmdBytes.length);
            for(byte byteChar : cmdBytes)
                stringBuilder.append(String.format("%02X ", byteChar));
            
			return stringBuilder.toString();
		}
		
	}
	
	/**
	 * We have packaged the message what received from device.And
	 * @author UESTC-PRMI-Burial
	 * @date 2014-12-16
	 */
	public static class SBBMessage {
		/** The whole message bytes */
		public byte[] msgBytes = null;
		/** The Head Code	*/
		public byte head = 0x00;
		/** The Index Of Command	*/
		public int index = -1;
		/** The Data	*/
		public byte[] data = null;
		/** The End Code	*/
		public byte end = 0x00;
		/** The accuracy of the message	*/
		public boolean isRight = false;
		
		
		/**
		 * The Constructor for SBBMessage
		 * @param message The whole message. You should to be careful that
		 * the message must be the whole message.Or Something will be wrong. 
		 */
		public SBBMessage(byte[] message) {
			msgBytes = message;
			
			int offset = 0;
			byte[] temp = null;
			// head
			head = message[offset++];
			// Index
			index = -1;
			temp = new byte[ID_SIZE];
			System.arraycopy(message, offset, temp, 0, ID_SIZE);
			for (int i = 0; i < INDEX_ARRAY.length; ++i) {
				if ((INDEX_ARRAY[i][0] == temp[0])
						&& (INDEX_ARRAY[i][1] == temp[1])) {
					index = i;
					break;
				}
			}
			offset += ID_SIZE;
			// data size
			temp = new byte[DS_SIZE];
			System.arraycopy(message, offset, temp, 0, DS_SIZE);
			int dataLength = Hex2Deci(temp, false);
			offset += DS_SIZE;
			// data
			if ((dataLength < 0) || (dataLength > CMD_MAX_SIZE)) {
				dataLength = -1;
				data = null;
			} else if (dataLength == 0){
				data = null;
			} else {
				data = new byte[dataLength];
				System.arraycopy(message, offset, data, 0, dataLength);
				offset += dataLength;
			}

			// crc
			temp = getCrc16(message, HD_SIZE,
					message.length - 1 - CK_SIZE - ED_SIZE);
			
			// is right
			isRight = ((null != temp) && (temp[0] == message[offset++]) && (temp[1] == message[offset++])
					&& (index != -1) && (dataLength != -1));
			
			// end
			end = message[offset];
		}
		
		/**
		 * The copy constructor for SBBMessage
		 * @param msg The SBBMessage's instance to copy
		 */
		public SBBMessage(SBBMessage msg) {
			if (msg == null) {
				Log.w(TAG, "The message is null!");
			}
			msgBytes = msg.msgBytes;
			// head
			head = msg.head;
			// index
			index = msg.index;  
			// data
			if ((null == msg.data) || (0 == msg.data.length)) {
				data = null;
			} else {
				data = new byte[msg.data.length];
				System.arraycopy(msg.data, 0, data, 0, data.length);
			}
			end = msg.end;
			isRight = msg.isRight;
		}

		@Override
		public String toString() {
			final StringBuilder stringBuilder = new StringBuilder(msgBytes.length);
            for(byte byteChar : msgBytes)
                stringBuilder.append(String.format("%02X ", byteChar));
            String strMessage = stringBuilder.toString();
			return strMessage;
		}
		
	}
	
	/**
	 * Getting the command after encoding
	 * @param indexOfOrder The index of {@link ComUtils#INDEX_ARRAY}
	 * @param data The DATA
	 * @return Return the command which include the spacial command index,
	 * data, Or null if something is wrong.
	 *  
	 */
	public static byte[] packagingCommand(int indexOfOrder, byte[] data) {
		byte[] command = null;
		// Checking the input parameter
		if ((indexOfOrder < 0) || (indexOfOrder >= INDEX_ARRAY.length)) {
			Log.w(TAG, "Something is wrong with the input parameter!");
			return command;		// return null.
		}
		
		int offset = 0;
		// Getting The Command Length
		int tempCmdLen = CMD_MIN_SIZE - HD_SIZE - ED_SIZE;
		if (null != data) {
			tempCmdLen += data.length;
		}
		
		// Initialize the temporary command array with the special length
		byte[] tempCmd = new byte[tempCmdLen];
		// Index Of Command
		System.arraycopy(INDEX_ARRAY[indexOfOrder], 0,
				tempCmd, offset, ID_SIZE);
		offset += ID_SIZE;
		// Data Length And DATA
		if (null != data) {
			System.arraycopy(Deci2Hex(data.length, DS_SIZE), 0,
					tempCmd, offset, DS_SIZE);
			offset += DS_SIZE;
			
			System.arraycopy(data, 0, tempCmd, offset, data.length);
			offset += data.length;
		} else {	// dataLen == 0 And data == null
			tempCmd[offset++] = 0x00;
			tempCmd[offset++] = 0x00;
		}
		// Check
		byte[] crc = getCrc16(tempCmd, offset);
		System.arraycopy(crc, 0, tempCmd, offset, CK_SIZE);
		offset += CK_SIZE;
		// Encode the command
		tempCmd = encoding(tempCmd);
		if (null == tempCmd) {
			Log.w(TAG, "Something is wrong with the process of encode.");
			return command;
		}
		// The length of the command which has encoded.
		tempCmdLen = tempCmd.length;
		
		int cmdLength = tempCmdLen + HD_SIZE + ED_SIZE;
		command = new byte[cmdLength];
		// Head Code And End Code
		command[0] = HEAD_CODE;
		command[cmdLength - 1] = END_CODE;
		// Copying the command Body ignore the head and end code.
		System.arraycopy(tempCmd, 0, command, HD_SIZE, tempCmdLen);

		return command;
	}
	
	/**
	 * Getting the command after encoding
	 * @param indexOfOrder The index of {@link ComUtils#INDEX_ARRAY}
	 * @param data The DATA
	 * @param dataLen The length of the DATA
	 * @return Return the command which include the spacial command index,
	 * data, Or null if something is wrong.
	 *  
	 */
	public static byte[] packagingCommand(int indexOfOrder, byte[] data, int dataLen) {
		byte[] command = null;
		// Checking the input parameter
		if ((indexOfOrder < 0) || (indexOfOrder >= INDEX_ARRAY.length)) {
			Log.w(TAG, "Something is wrong with the input parameter!");
			return command;		// return null.
		}
		if ((data == null) && (dataLen != 0)) {
			Log.w(TAG, "Something is wrong with the input parameter!");
			return command;		// return null.
		}
		if ((data != null) && ((dataLen > data.length) || (dataLen < 0))) {
			Log.w(TAG, "Something is wrong with the input parameter!");
			return command;		// return null.
		}
		
		int offset = 0;
		// Getting The Command Length
		int tempCmdLen = 
				dataLen + CMD_MIN_SIZE - HD_SIZE - ED_SIZE;
		
		// Initialize the temporary command array with the special length
		byte[] tempCmd = new byte[tempCmdLen];
		// Index Of Command
		System.arraycopy(INDEX_ARRAY[indexOfOrder], 0,
				tempCmd, offset, ID_SIZE);
		offset += ID_SIZE;
		// Data Length And DATA
		if (null != data) {
			System.arraycopy(Deci2Hex(dataLen, DS_SIZE), 0,
					tempCmd, offset, DS_SIZE);
			offset += DS_SIZE;
			
			System.arraycopy(data, 0, tempCmd, offset, data.length);
			offset += data.length;
		} else {	// dataLen == 0 And data == null
			tempCmd[offset++] = 0x00;
			tempCmd[offset++] = 0x00;
		}
		// Check
		byte[] crc = getCrc16(tempCmd, offset);
		System.arraycopy(crc, 0, tempCmd, offset, CK_SIZE);
		offset += CK_SIZE;
		// Encode the command
		tempCmd = encoding(tempCmd);
		if (null == tempCmd) {
			Log.w(TAG, "Something is wrong with the process of encode.");
			return command;
		}
		// The length of the command which has encoded.
		tempCmdLen = tempCmd.length;
		
		int cmdLength = tempCmdLen + HD_SIZE + ED_SIZE;
		command = new byte[cmdLength];
		// Head Code And End Code
		command[0] = HEAD_CODE;
		command[cmdLength - 1] = END_CODE;
		// Copying the command Body ignore the head and end code.
		System.arraycopy(tempCmd, 0, command, HD_SIZE, tempCmdLen);

		return command;
	}
	
	/**
	 * Traverse command for encode.As follow:
	 * 0X54 -> 0X52 0X00;
	 * 0X53 -> 0X52 0X01;
	 * 0X52 -> 0X52 0X02;
	 * 
	 * @param command The command to encode.
	 * @return The command what Complete encode. Or return null 
	 * if the command is null or the length of command is zero.
	 */
	public static byte[] encoding(byte[] command) {
		byte[] retCmd = null;
		
		if ((command == null) || (command.length == 0)
				|| (command.length > CMD_MAX_SIZE)) {
			Log.i(TAG, "The command for encode is null");
			return retCmd;
		}
		
		byte[] tempCmd = new byte[CMD_MAX_SIZE];
		int tempLength = 0;
		// Traverse the command for encode
		for (byte everyByte : command) {
			switch(everyByte) {
			case 0x52:
				tempCmd[tempLength++] = 0x52;
				tempCmd[tempLength++] = 0x02;
				break;
			case 0x53:
				tempCmd[tempLength++] = 0x52;
				tempCmd[tempLength++] = 0x01;
				break;
			case 0x54:
				tempCmd[tempLength++] = 0x52;
				tempCmd[tempLength++] = 0x00;
				break;
			default:
				tempCmd[tempLength++] = everyByte;
				break;
			}
		}
		
		retCmd = new byte[tempLength];
		System.arraycopy(tempCmd, 0, retCmd, 0, tempLength);
		
		return retCmd;
	}
	
	/**
	 * Decode message after received from device ignore the head and end code.
	 * 0X52 0X00 -> 0X54;
	 * 0X52 0X01 -> 0X53;
	 * 0X52 0X02 -> 0X52;
	 * 
	 * @param cmdPackage The message received from BLE
	 * @return The message what Complete decode. 
	 */
	public static byte[] decoding(final byte[] message) {
		byte[] retMsg = null;
		int msgOffset = 0;
		int retOffset = 0;
		
		if ((message == null) || (message.length == 0)
				|| (message.length >= CMD_MAX_SIZE)) {
			Log.w(TAG, "Something is wrong with the message");
			return retMsg;
		}

		if ((message[msgOffset] != HEAD_CODE) ||
				(message[message.length - 1] != END_CODE)) {
			Log.w(TAG, "The head code Or the end code is wrong");
			return null;
		}
		
		byte[] tempMsg = new byte[CMD_MAX_SIZE];

		for (; msgOffset < message.length; ++msgOffset) {
			if (message[msgOffset] == 0x52) {
				if (msgOffset == (message.length - 1)) {
					Log.w(TAG, "Something is wrong with the command");
					break;
				}
					
				switch(message[++msgOffset])
				{
				case 0x00:
					tempMsg[retOffset++] = 0x54;
					break;
				case 0x01:
					tempMsg[retOffset++] = 0x53;
					break;
				case 0x02:
					tempMsg[retOffset++] = 0x52;
					break;
				default:
					Log.w(TAG, "Something is wrong with the message");
					return null;
				}
			} else {
				tempMsg[retOffset++] = message[msgOffset];
			}
		}
			
		retMsg = new byte[retOffset];

			System.arraycopy(tempMsg, 0, retMsg, 0, retOffset);
		
		return retMsg;
	}
	
	/**
	 * Return the Cyclic Redundancy Check corresponding with data
	 * 
	 * @param data The command what ready to check
	 * @param dataLen The length of the data what need to calculate.
	 * @return Cyclic Redundancy Check
	 * 
	 */
	public static byte[] getCrc16(byte[] data, int dataLen) {
		byte[] byteCrc = null;
		if ((null == data) || (dataLen > data.length)
				|| (dataLen < 0)) {
			return byteCrc;
		}
		
	    /** CRC Redundancy Table	*/
		short[] crc_ta={
			(short) 0x0000, (short) 0x1021, (short) 0x2042, (short) 0x3063, (short) 0x4084, (short) 0x50a5, (short) 0x60c6, (short) 0x70e7,
			(short) 0x8108, (short) 0x9129, (short) 0xa14a, (short) 0xb16b, (short) 0xc18c, (short) 0xd1ad, (short) 0xe1ce, (short) 0xf1ef,
			(short) 0x1231, (short) 0x0210, (short) 0x3273, (short) 0x2252, (short) 0x52b5, (short) 0x4294, (short) 0x72f7, (short) 0x62d6,
			(short) 0x9339, (short) 0x8318, (short) 0xb37b, (short) 0xa35a, (short) 0xd3bd, (short) 0xc39c, (short) 0xf3ff, (short) 0xe3de,
			(short) 0x2462, (short) 0x3443, (short) 0x0420, (short) 0x1401, (short) 0x64e6, (short) 0x74c7, (short) 0x44a4, (short) 0x5485,
			(short) 0xa56a, (short) 0xb54b, (short) 0x8528, (short) 0x9509, (short) 0xe5ee, (short) 0xf5cf, (short) 0xc5ac, (short) 0xd58d,
			(short) 0x3653, (short) 0x2672, (short) 0x1611, (short) 0x0630, (short) 0x76d7, (short) 0x66f6, (short) 0x5695, (short) 0x46b4,
			(short) 0xb75b, (short) 0xa77a, (short) 0x9719, (short) 0x8738, (short) 0xf7df, (short) 0xe7fe, (short) 0xd79d, (short) 0xc7bc,
			(short) 0x48c4, (short) 0x58e5, (short) 0x6886, (short) 0x78a7, (short) 0x0840, (short) 0x1861, (short) 0x2802, (short) 0x3823,
			(short) 0xc9cc, (short) 0xd9ed, (short) 0xe98e, (short) 0xf9af, (short) 0x8948, (short) 0x9969, (short) 0xa90a, (short) 0xb92b,
			(short) 0x5af5, (short) 0x4ad4, (short) 0x7ab7, (short) 0x6a96, (short) 0x1a71, (short) 0x0a50, (short) 0x3a33, (short) 0x2a12,
			(short) 0xdbfd, (short) 0xcbdc, (short) 0xfbbf, (short) 0xeb9e, (short) 0x9b79, (short) 0x8b58, (short) 0xbb3b, (short) 0xab1a,
			(short) 0x6ca6, (short) 0x7c87, (short) 0x4ce4, (short) 0x5cc5, (short) 0x2c22, (short) 0x3c03, (short) 0x0c60, (short) 0x1c41,
			(short) 0xedae, (short) 0xfd8f, (short) 0xcdec, (short) 0xddcd, (short) 0xad2a, (short) 0xbd0b, (short) 0x8d68, (short) 0x9d49,
			(short) 0x7e97, (short) 0x6eb6, (short) 0x5ed5, (short) 0x4ef4, (short) 0x3e13, (short) 0x2e32, (short) 0x1e51, (short) 0x0e70,
			(short) 0xff9f, (short) 0xefbe, (short) 0xdfdd, (short) 0xcffc, (short) 0xbf1b, (short) 0xaf3a, (short) 0x9f59, (short) 0x8f78,
			(short) 0x9188, (short) 0x81a9, (short) 0xb1ca, (short) 0xa1eb, (short) 0xd10c, (short) 0xc12d, (short) 0xf14e, (short) 0xe16f,
			(short) 0x1080, (short) 0x00a1, (short) 0x30c2, (short) 0x20e3, (short) 0x5004, (short) 0x4025, (short) 0x7046, (short) 0x6067,
			(short) 0x83b9, (short) 0x9398, (short) 0xa3fb, (short) 0xb3da, (short) 0xc33d, (short) 0xd31c, (short) 0xe37f, (short) 0xf35e,
			(short) 0x02b1, (short) 0x1290, (short) 0x22f3, (short) 0x32d2, (short) 0x4235, (short) 0x5214, (short) 0x6277, (short) 0x7256,
			(short) 0xb5ea, (short) 0xa5cb, (short) 0x95a8, (short) 0x8589, (short) 0xf56e, (short) 0xe54f, (short) 0xd52c, (short) 0xc50d,
			(short) 0x34e2, (short) 0x24c3, (short) 0x14a0, (short) 0x0481, (short) 0x7466, (short) 0x6447, (short) 0x5424, (short) 0x4405,
			(short) 0xa7db, (short) 0xb7fa, (short) 0x8799, (short) 0x97b8, (short) 0xe75f, (short) 0xf77e, (short) 0xc71d, (short) 0xd73c,
			(short) 0x26d3, (short) 0x36f2, (short) 0x0691, (short) 0x16b0, (short) 0x6657, (short) 0x7676, (short) 0x4615, (short) 0x5634,
			(short) 0xd94c, (short) 0xc96d, (short) 0xf90e, (short) 0xe92f, (short) 0x99c8, (short) 0x89e9, (short) 0xb98a, (short) 0xa9ab,
			(short) 0x5844, (short) 0x4865, (short) 0x7806, (short) 0x6827, (short) 0x18c0, (short) 0x08e1, (short) 0x3882, (short) 0x28a3,
			(short) 0xcb7d, (short) 0xdb5c, (short) 0xeb3f, (short) 0xfb1e, (short) 0x8bf9, (short) 0x9bd8, (short) 0xabbb, (short) 0xbb9a,
			(short) 0x4a75, (short) 0x5a54, (short) 0x6a37, (short) 0x7a16, (short) 0x0af1, (short) 0x1ad0, (short) 0x2ab3, (short) 0x3a92,
			(short) 0xfd2e, (short) 0xed0f, (short) 0xdd6c, (short) 0xcd4d, (short) 0xbdaa, (short) 0xad8b, (short) 0x9de8, (short) 0x8dc9,
			(short) 0x7c26, (short) 0x6c07, (short) 0x5c64, (short) 0x4c45, (short) 0x3ca2, (short) 0x2c83, (short) 0x1ce0, (short) 0x0cc1,
			(short) 0xef1f, (short) 0xff3e, (short) 0xcf5d, (short) 0xdf7c, (short) 0xaf9b, (short) 0xbfba, (short) 0x8fd9, (short) 0x9ff8,
			(short) 0x6e17, (short) 0x7e36, (short) 0x4e55, (short) 0x5e74, (short) 0x2e93, (short) 0x3eb2, (short) 0x0ed1, (short) 0x1ef0
		 };
		
		short  crc = 0x0000;
		byte da = 0x00;
		int index = 0;
		for (int i = 0; i < dataLen; ++i) {
			da = (byte) (crc >> 8);
		    crc <<= 8;
		    index = (da ^ data[i]);
		    if (index < 0) {
		    	index += 256;
		    }
		    crc ^= crc_ta[index]; 
		}
		
		byteCrc = new byte[] {0x00, 0x00};
		byteCrc[0] = (byte) (crc >> 8);
		byteCrc[1] = (byte) crc; 

		return byteCrc;
	}
	
	/**
	 * Return the Cyclic Redundancy Check corresponding with data
	 * 
	 * @param data The command what ready to check
	 * @param posStart The start position of the data what need to calculate.
	 * @param posEnd The end position of the data what need to calculate.
	 * @return Cyclic Redundancy Check
	 * 
	 */
	public static byte[] getCrc16(byte[] data, int posStart, int posEnd) {
		byte[] byteCrc = null;
		if ((null == data) || (posEnd >= data.length)
				|| (posEnd < 0) || (posStart < 0) || (posStart >= posEnd)) {
			return byteCrc;
		}
		
	    /** CRC Redundancy Table	*/
		short[] crc_ta={
			(short) 0x0000, (short) 0x1021, (short) 0x2042, (short) 0x3063, (short) 0x4084, (short) 0x50a5, (short) 0x60c6, (short) 0x70e7,
			(short) 0x8108, (short) 0x9129, (short) 0xa14a, (short) 0xb16b, (short) 0xc18c, (short) 0xd1ad, (short) 0xe1ce, (short) 0xf1ef,
			(short) 0x1231, (short) 0x0210, (short) 0x3273, (short) 0x2252, (short) 0x52b5, (short) 0x4294, (short) 0x72f7, (short) 0x62d6,
			(short) 0x9339, (short) 0x8318, (short) 0xb37b, (short) 0xa35a, (short) 0xd3bd, (short) 0xc39c, (short) 0xf3ff, (short) 0xe3de,
			(short) 0x2462, (short) 0x3443, (short) 0x0420, (short) 0x1401, (short) 0x64e6, (short) 0x74c7, (short) 0x44a4, (short) 0x5485,
			(short) 0xa56a, (short) 0xb54b, (short) 0x8528, (short) 0x9509, (short) 0xe5ee, (short) 0xf5cf, (short) 0xc5ac, (short) 0xd58d,
			(short) 0x3653, (short) 0x2672, (short) 0x1611, (short) 0x0630, (short) 0x76d7, (short) 0x66f6, (short) 0x5695, (short) 0x46b4,
			(short) 0xb75b, (short) 0xa77a, (short) 0x9719, (short) 0x8738, (short) 0xf7df, (short) 0xe7fe, (short) 0xd79d, (short) 0xc7bc,
			(short) 0x48c4, (short) 0x58e5, (short) 0x6886, (short) 0x78a7, (short) 0x0840, (short) 0x1861, (short) 0x2802, (short) 0x3823,
			(short) 0xc9cc, (short) 0xd9ed, (short) 0xe98e, (short) 0xf9af, (short) 0x8948, (short) 0x9969, (short) 0xa90a, (short) 0xb92b,
			(short) 0x5af5, (short) 0x4ad4, (short) 0x7ab7, (short) 0x6a96, (short) 0x1a71, (short) 0x0a50, (short) 0x3a33, (short) 0x2a12,
			(short) 0xdbfd, (short) 0xcbdc, (short) 0xfbbf, (short) 0xeb9e, (short) 0x9b79, (short) 0x8b58, (short) 0xbb3b, (short) 0xab1a,
			(short) 0x6ca6, (short) 0x7c87, (short) 0x4ce4, (short) 0x5cc5, (short) 0x2c22, (short) 0x3c03, (short) 0x0c60, (short) 0x1c41,
			(short) 0xedae, (short) 0xfd8f, (short) 0xcdec, (short) 0xddcd, (short) 0xad2a, (short) 0xbd0b, (short) 0x8d68, (short) 0x9d49,
			(short) 0x7e97, (short) 0x6eb6, (short) 0x5ed5, (short) 0x4ef4, (short) 0x3e13, (short) 0x2e32, (short) 0x1e51, (short) 0x0e70,
			(short) 0xff9f, (short) 0xefbe, (short) 0xdfdd, (short) 0xcffc, (short) 0xbf1b, (short) 0xaf3a, (short) 0x9f59, (short) 0x8f78,
			(short) 0x9188, (short) 0x81a9, (short) 0xb1ca, (short) 0xa1eb, (short) 0xd10c, (short) 0xc12d, (short) 0xf14e, (short) 0xe16f,
			(short) 0x1080, (short) 0x00a1, (short) 0x30c2, (short) 0x20e3, (short) 0x5004, (short) 0x4025, (short) 0x7046, (short) 0x6067,
			(short) 0x83b9, (short) 0x9398, (short) 0xa3fb, (short) 0xb3da, (short) 0xc33d, (short) 0xd31c, (short) 0xe37f, (short) 0xf35e,
			(short) 0x02b1, (short) 0x1290, (short) 0x22f3, (short) 0x32d2, (short) 0x4235, (short) 0x5214, (short) 0x6277, (short) 0x7256,
			(short) 0xb5ea, (short) 0xa5cb, (short) 0x95a8, (short) 0x8589, (short) 0xf56e, (short) 0xe54f, (short) 0xd52c, (short) 0xc50d,
			(short) 0x34e2, (short) 0x24c3, (short) 0x14a0, (short) 0x0481, (short) 0x7466, (short) 0x6447, (short) 0x5424, (short) 0x4405,
			(short) 0xa7db, (short) 0xb7fa, (short) 0x8799, (short) 0x97b8, (short) 0xe75f, (short) 0xf77e, (short) 0xc71d, (short) 0xd73c,
			(short) 0x26d3, (short) 0x36f2, (short) 0x0691, (short) 0x16b0, (short) 0x6657, (short) 0x7676, (short) 0x4615, (short) 0x5634,
			(short) 0xd94c, (short) 0xc96d, (short) 0xf90e, (short) 0xe92f, (short) 0x99c8, (short) 0x89e9, (short) 0xb98a, (short) 0xa9ab,
			(short) 0x5844, (short) 0x4865, (short) 0x7806, (short) 0x6827, (short) 0x18c0, (short) 0x08e1, (short) 0x3882, (short) 0x28a3,
			(short) 0xcb7d, (short) 0xdb5c, (short) 0xeb3f, (short) 0xfb1e, (short) 0x8bf9, (short) 0x9bd8, (short) 0xabbb, (short) 0xbb9a,
			(short) 0x4a75, (short) 0x5a54, (short) 0x6a37, (short) 0x7a16, (short) 0x0af1, (short) 0x1ad0, (short) 0x2ab3, (short) 0x3a92,
			(short) 0xfd2e, (short) 0xed0f, (short) 0xdd6c, (short) 0xcd4d, (short) 0xbdaa, (short) 0xad8b, (short) 0x9de8, (short) 0x8dc9,
			(short) 0x7c26, (short) 0x6c07, (short) 0x5c64, (short) 0x4c45, (short) 0x3ca2, (short) 0x2c83, (short) 0x1ce0, (short) 0x0cc1,
			(short) 0xef1f, (short) 0xff3e, (short) 0xcf5d, (short) 0xdf7c, (short) 0xaf9b, (short) 0xbfba, (short) 0x8fd9, (short) 0x9ff8,
			(short) 0x6e17, (short) 0x7e36, (short) 0x4e55, (short) 0x5e74, (short) 0x2e93, (short) 0x3eb2, (short) 0x0ed1, (short) 0x1ef0
		 };
		
		short  crc = 0x0000;
		byte da = 0x00;
		int index = 0;
		for (int i = posStart; i <= posEnd; ++i) {
			da = (byte) (crc >> 8);
		    crc <<= 8;
		    index = (da ^ data[i]);
		    if (index < 0) {
		    	index += 256;
		    }
		    crc ^= crc_ta[index]; 
		}
		
		byteCrc = new byte[] {0x00, 0x00};
		byteCrc[0] = (byte) (crc >> 8);
		byteCrc[1] = (byte) crc; 

		return byteCrc;
	}
	
	/**
	 * Return the Cyclic Redundancy Check corresponding with data
	 * 
	 * @param data The command what ready to check
	 * @param posStart The start position of the data what need to calculate.
	 * @param posEnd The end position of the data what need to calculate.
	 * @return Cyclic Redundancy Check
	 * 
	 */
	public static byte getCrc8(byte[] data, int posStart, int posEnd) {
		byte crc8 = (byte) 0xFF;
		if ((null == data) || (posEnd >= data.length)
				|| (posEnd < 0) || (posStart < 0) || (posStart >= posEnd)) {
			return crc8;
		}
		
		byte[] crc_8 = { 
				(byte) 0x00,(byte) 0X5E,(byte) 0XBC,(byte) 0XE2,(byte) 0X61,(byte) 0X3F,(byte) 0XDD,(byte) 0X83,(byte) 0XC2,(byte) 0X9C,(byte) 0X7E,(byte) 0X20,(byte) 0XA3,(byte) 0XFD,(byte) 0X1F,(byte) 0X41,//0-15
				(byte) 0X9D,(byte) 0XC3,(byte) 0X21,(byte) 0X7F,(byte) 0XFC,(byte) 0XA2,(byte) 0X40,(byte) 0X1E,(byte) 0X5F,(byte) 0X01,(byte) 0XE3,(byte) 0XBD,(byte) 0X3E,(byte) 0X60,(byte) 0X82,(byte) 0XDC,//16-31
				(byte) 0X23,(byte) 0X7D,(byte) 0X9F,(byte) 0XC1,(byte) 0X42,(byte) 0X1C,(byte) 0XFE,(byte) 0XA0,(byte) 0XE1,(byte) 0XBF,(byte) 0X5D,(byte) 0X03,(byte) 0X80,(byte) 0XDE,(byte) 0X3C,(byte) 0X62,//32-47
				(byte) 0XBE,(byte) 0XE0,(byte) 0X02,(byte) 0X5C,(byte) 0XDF,(byte) 0X81,(byte) 0X63,(byte) 0X3D,(byte) 0X7C,(byte) 0X22,(byte) 0XC0,(byte) 0X9E,(byte) 0X1D,(byte) 0X43,(byte) 0XA1,(byte) 0XFF,//48-63
				(byte) 0X46,(byte) 0X18,(byte) 0XFA,(byte) 0XA4,(byte) 0X27,(byte) 0X79,(byte) 0X9B,(byte) 0XC5,(byte) 0X84,(byte) 0XDA,(byte) 0X38,(byte) 0X66,(byte) 0XE5,(byte) 0XBB,(byte) 0X59,(byte) 0X07,//64-79
				(byte) 0XDB,(byte) 0X85,(byte) 0X67,(byte) 0X39,(byte) 0XBA,(byte) 0XE4,(byte) 0X06,(byte) 0X58,(byte) 0X19,(byte) 0X47,(byte) 0XA5,(byte) 0XFB,(byte) 0X78,(byte) 0X26,(byte) 0XC4,(byte) 0X9A,//80-95
				(byte) 0X65,(byte) 0X3B,(byte) 0XD9,(byte) 0X87,(byte) 0X04,(byte) 0X5A,(byte) 0XB8,(byte) 0XE6,(byte) 0XA7,(byte) 0XF9,(byte) 0X1B,(byte) 0X45,(byte) 0XC6,(byte) 0X98,(byte) 0X7A,(byte) 0X24,//96-111
				(byte) 0XF8,(byte) 0XA6,(byte) 0X44,(byte) 0X1A,(byte) 0X99,(byte) 0XC7,(byte) 0X25,(byte) 0X7B,(byte) 0X3A,(byte) 0X64,(byte) 0X86,(byte) 0XD8,(byte) 0X5B,(byte) 0X05,(byte) 0XE7,(byte) 0XB9,//112-127
				(byte) 0X8C,(byte) 0XD2,(byte) 0X30,(byte) 0X6E,(byte) 0XED,(byte) 0XB3,(byte) 0X51,(byte) 0X0F,(byte) 0X4E,(byte) 0X10,(byte) 0XF2,(byte) 0XAC,(byte) 0X2F,(byte) 0X71,(byte) 0X93,(byte) 0XCD,//128-143
				(byte) 0X11,(byte) 0X4F,(byte) 0XAD,(byte) 0XF3,(byte) 0X70,(byte) 0X2E,(byte) 0XCC,(byte) 0X92,(byte) 0XD3,(byte) 0X8D,(byte) 0X6F,(byte) 0X31,(byte) 0XB2,(byte) 0XEC,(byte) 0X0E,(byte) 0X50,//144-159
				(byte) 0XAF,(byte) 0XF1,(byte) 0X13,(byte) 0X4D,(byte) 0XCE,(byte) 0X90,(byte) 0X72,(byte) 0X2C,(byte) 0X6D,(byte) 0X33,(byte) 0XD1,(byte) 0X8F,(byte) 0X0C,(byte) 0X52,(byte) 0XB0,(byte) 0XEE,//160-175
				(byte) 0X32,(byte) 0X6C,(byte) 0X8E,(byte) 0XD0,(byte) 0X53,(byte) 0X0D,(byte) 0XEF,(byte) 0XB1,(byte) 0XF0,(byte) 0XAE,(byte) 0X4C,(byte) 0X12,(byte) 0X91,(byte) 0XCF,(byte) 0X2D,(byte) 0X73,//176-191
				(byte) 0XCA,(byte) 0X94,(byte) 0X76,(byte) 0X28,(byte) 0XAB,(byte) 0XF5,(byte) 0X17,(byte) 0X49,(byte) 0X08,(byte) 0X56,(byte) 0XB4,(byte) 0XEA,(byte) 0X69,(byte) 0X37,(byte) 0XD5,(byte) 0X8B,//192-207
				(byte) 0X57,(byte) 0X09,(byte) 0XEB,(byte) 0XB5,(byte) 0X36,(byte) 0X68,(byte) 0X8A,(byte) 0XD4,(byte) 0X95,(byte) 0XCB,(byte) 0X29,(byte) 0X77,(byte) 0XF4,(byte) 0XAA,(byte) 0X48,(byte) 0X16,//208-223
				(byte) 0XE9,(byte) 0XB7,(byte) 0X55,(byte) 0X0B,(byte) 0X88,(byte) 0XD6,(byte) 0X34,(byte) 0X6A,(byte) 0X2B,(byte) 0X75,(byte) 0X97,(byte) 0XC9,(byte) 0X4A,(byte) 0X14,(byte) 0XF6,(byte) 0XA8,//224-239
				(byte) 0X74,(byte) 0X2A,(byte) 0XC8,(byte) 0X96,(byte) 0X15,(byte) 0X4B,(byte) 0XA9,(byte) 0XF7,(byte) 0XB6,(byte) 0XE8,(byte) 0X0A,(byte) 0X54,(byte) 0XD7,(byte) 0X89,(byte) 0X6B,(byte) 0X35//240-255    
			};

	    int index = 0;        // CRC8�??￠�?��?�ㄦ牸绱㈠紩
	    
	    // �?�涜CRC8浣嶆牎楠�
	    for (int i = posStart; i <= posEnd; ++i) {
	        index = crc8 ^ data[i];
	        if (index < 0) {
	        	index += 256;
	        }
	        crc8 = crc_8[index];
	    }
	    return (crc8);

	}
	
	/**
	 * Returns byte array included the current timestamp in seconds since 
	 * January 1, 1970 00:00:00.0 UTC
	 * 
	 * @return Return null if fails, Or return byte array included the
	 * current timestamp.
	 */
	public static byte[] getTimeStamp() {
		byte[] retUTC = null;
		long millis = -1;
		
		millis = (System.currentTimeMillis() / 1000);
		if ((millis < 0) || (millis > Integer.MAX_VALUE)) {
			return retUTC;
		}
		
		int seconds = (int) millis;
		
		retUTC = Deci2Hex(seconds, TS_SIZE);
		
		return retUTC;
	}
	
	/** The Size(Bite) Of Each Byte	*/
	private final static int BITE_OF_BYTE = 8;
	
	
	/**
	 * Hexadecimal conversion to a Integer.
	 * @param singleByte The byte to convert.
	 * @param isSigned True if the integer is signed, Or false.
	 * @return Integer corresponding with byteArray.
	 */
	public static int Hex2Deci(final byte singleByte, boolean isSigned) {
		int integer = -1;
		integer = (int) singleByte;
		if (!isSigned && (integer < 0)) {
			integer += 256;
		}
		
		return integer;
	}
	
	/**
	 * Hexadecimal conversion to a Integer.
	 * @param bytes The bytes array to convert.
	 * @param isSigned True if the integer is signed, Or false.
	 * @return Integer corresponding with byteArray.
	 */
	public static int Hex2Deci(final byte[] bytes, boolean isSigned) {
		if((null == bytes) || (bytes.length <= 0) || (bytes.length > TS_SIZE)) {
			Log.e(TAG, "Hex2Deci:	The length of parameter out of range");
			return -1;
		}
		int integer = 0x00000000;
		int offset = 0;
		
		byte mask = (byte) 0x80;
		isSigned = (isSigned && (mask == (mask & bytes[offset])));
		
		byte temp = bytes[offset++];
		int iMask = 0x00000001;
		for (int i = 0; i < BITE_OF_BYTE; ++i) {
			integer <<= 1;
			if (mask == (mask & temp)) {
				integer |= iMask;
			}
			temp <<= 1;
		}
		while(offset < bytes.length) {
			temp = bytes[offset++];
			for (int i = 0; i < BITE_OF_BYTE; ++i) {
				integer <<= 1;
				if (mask == (mask & temp)) {
					integer |= iMask;
				}
				temp <<= 1;
			}
		}
		
		if (isSigned && (TS_SIZE != bytes.length)) {	// minus
			int[] carry = {1, 256, 65536, 16777216};
			integer -= carry[bytes.length];
		}

		return integer;
	}
	
	/**
	 * Integer conversion to a Hexadecimal.
	 * @param deciNum The Integer to convert.
	 * @param byteSize The length of byte array.
	 * @return The byte array corresponding with Integer
	 */
	public static byte[] Deci2Hex(final int deciNum, int byteSize) {
		switch (byteSize) {
		case 1:
			if ((deciNum < -128) || (deciNum > 127)) {
				Log.e(TAG, "convertToHex:	the size of byte array out of range");
				return null;
			}
			break;
		case 2:
			if ((deciNum < -32768) || (deciNum > 32767)) {
				Log.e(TAG, "convertToHex:	the size of byte array out of range");
				return null;
			}
			break;
		case 3:
			if ((deciNum < -8388608) || (deciNum > 8388607)) {
				Log.e(TAG, "convertToHex:	the size of byte array out of range");
				return null;
			}
			break;
		case 4:
			if ((deciNum < -2147483648) || (deciNum > 2147483647)) {
				Log.e(TAG, "convertToHex:	the size of byte array out of range");
				return null;
			}
			break;
		default:
			Log.e(TAG, "convertToHex:	the size of byte array out of range");
			return null;
		}
		
		byte[] hexInteger = new byte[byteSize];
		int tempInt = deciNum;
		int mask_4b = 0x80000000;
		byte mask = 0x01;
		
		tempInt <<= ((4 - byteSize) * BITE_OF_BYTE);
		for (int offset = 0; offset < byteSize; ++offset) {
			for (int i = 0; i < BITE_OF_BYTE; ++i) {
				hexInteger[offset] <<= 1;
				if (mask_4b == (mask_4b & tempInt)) {
					hexInteger[offset] |= mask;
				}
				tempInt <<= 1;
			}
		}
		
		return hexInteger;
	}
	
	/**
	 * Integer conversion to a Hexadecimal.
	 * @param deciNum The Integer to convert.
	 * @return The byte array corresponding with Integer.Equal to Deci2Hex(deciNum, 1)
	 * @see BleUtil#Deci2Hex(int, int)
	 */
	public static byte Deci2Hex(final int deciNum) {
		int byteSize = 1;
		byte hexInteger = (byte) 0xFF;
		
		if ((deciNum < -128) || (deciNum > 127)) {
			Log.e(TAG, "convertToHex:	the size of byte array out of range");
			return hexInteger;
		}
		
		int tempInt = deciNum;
		int mask_4b = 0x80000000;
		byte mask = 0x01;
		
		tempInt <<= ((4 - byteSize) * BITE_OF_BYTE);
		for (int i = 0; i < BITE_OF_BYTE; ++i) {
			hexInteger <<= 1;
			if (mask_4b == (mask_4b & tempInt)) {
				hexInteger |= mask;
			}
			tempInt <<= 1;
		}
		
		return hexInteger;
	}
}

