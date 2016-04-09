package com.hori.voicerecord;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import com.hori.voicerecord.StaticData;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class Sender {
	final static private String TAG = "Sender";	
	private static final int NOTHING	= 0x01;
	private static final int CONNECTING = 0x02;
	private static final int CONNECTED	= 0x03;
	
	private Socket mSocket = null;
	
	private ConnectThread mConnectThread = null;
	private ConnectedThread mConnectedThread = null;
	
	private int mState = NOTHING;
	
	// UI�̴߳������
	private Handler mHandler = null;

	/**
	 * ���캯��,����UI�̴߳���Handler
	 * @param handler
	 */
    Sender(Handler handler) {
    	mHandler = handler;
    }
    
    public boolean isLink() {
		return (mState == CONNECTED);
	}
    
    /**
     * ���ӷ������ӿ�
     */
	public synchronized void ConnectServer(String IP, int Port) {
		if (mState == CONNECTED) {
			SendMsgToUI(StaticData.SHOW_TOAST, "Connected");
			return;
		} else if (mState == CONNECTING) {
			SendMsgToUI(StaticData.SHOW_TOAST, "Connecting...");
			return;
		} else {
			mConnectThread = new ConnectThread(mSocket, IP, Port);
			mConnectThread.start();
			
			SendMsgToUI(StaticData.SHOW_TOAST, "Please Wait a Moment...");
		}
    }
    
	public void CloseClient() {
		if(mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}
		
		if(mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		
		mState = NOTHING;
	}
	
    public synchronized void SendFile(String FilePath) throws IOException {
    	Log.i(TAG, "SendFile:	" + FilePath);
    	File file = new File(FilePath);
    	int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        FileInputStream fis = null;
        
        // write("i am is a picture".getBytes());
        

		fis = new FileInputStream(file);
		
		int read = 0;
		while ((read = fis.read(buffer, 0, bufferSize)) != -1)
			mConnectedThread.write(buffer, 0, read);
		
		fis.close();
		fis = null;
        
    }
    
    public void write(byte[] out) {
        
        if (mState != CONNECTED) {
        	return;
        }
    	
        /*// Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);*/
        
        mConnectedThread.write(out);
    }
    
    
    
    private class ConnectedThread extends Thread {
    	private final static String TAG = "ConnectedThread";
    	
		private final Socket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
		
    	ConnectedThread(Socket socket) {
    		mmSocket = socket;
    		InputStream tmpIn = null;
            OutputStream tmpOut = null;
            
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
    	}

		@Override
		public void run() {
			super.run();
            Log.i(TAG, "BEGIN ConnectedThread");
            
            int BufSize = 128;
            int recvSize = -1;
    		byte[] Buffer = new byte[BufSize];

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                	recvSize = mmInStream.read(Buffer);
                    if (recvSize > 0) {
                    	// TODO
                    	byte[] temp = new byte[recvSize];
                    	System.arraycopy(Buffer, 0, temp, 0, recvSize);
                    	SendMsgToUI(StaticData.WORDS, temp);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
                
                // Log.d(TAG, "" + Buffer);
            }
		}
		private void write(byte[] buffer, int start, int size) {
			try {
                mmOutStream.write(buffer, start, size);
                mmOutStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
                connectionLost();
            }
			
			Log.i(TAG, "write finished");
		}
		
		private void write(byte[] buffer) {
			try {
                mmOutStream.write(buffer);
                mmOutStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
                connectionLost();
            }
			
			Log.i(TAG, "write finished");
		}
		
		public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
            
            mState = NOTHING;
        }

		private void connectionLost() {
			SendMsgToUI(StaticData.CONNECT_LOST, null);
			mState = NOTHING;
		}
    	
    	
    }

    
	
    private class ConnectThread extends Thread {
    	private Socket mmSocket = null;
    	private SocketAddress mmRemoteAddr = null;
    	private int mmTimeout = 2000;	// 2s ��ʱ
    	
    	ConnectThread(Socket socket, String IP, int Port) {
    		mmSocket = socket;
    		mmRemoteAddr = new InetSocketAddress(IP, Port);
    	}
    	
		@Override
		public void run() {
			super.run();
			
			if (mState != NOTHING) {
				return;
			}

			mState = CONNECTING;
			Socket mmTempSocket = new Socket();
			try {
				// mTempSocket = new Socket(mmIP, mmPort);
				mmTempSocket.connect(mmRemoteAddr, mmTimeout);
			} catch (Exception e) {
				e.printStackTrace();
				SendMsgToUI(StaticData.CONNECT_FAIL, null);
				return;
			}
			SendMsgToUI(StaticData.CONNECT_SUCCESS, null);
			
			mmSocket = mmTempSocket;
			
			synchronized (this) {
            	mConnectThread = null;
            }
			
			if(mConnectedThread != null) {
				mConnectedThread.cancel();
				mConnectedThread = null;
			}
			
			mState = CONNECTED;
			mConnectedThread = new ConnectedThread(mmSocket);
            mConnectedThread.start();
			return;
		}
		
		public void cancel() {
            try {
            	mmSocket.close();
            	mmSocket = null;
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
		
    }
	
	/**
	 * ת��2���Ƶ�10����
	 * @param	byteArray	��ת����byte����,������������Ϊ��λ����λ;final����;
	 * @return	���Ӧ��ʮ������; -1��ʾbyteArray�д���
	 */
    private int convertToDeci(final byte[] byteArray) {
		if((byteArray.length <= 0) || (byteArray.length > 4)) {
			Log.e(TAG, "convertToDeci:	byte���鳤�ȴ���");
			return -1;
		}	// �ݴ�
				
		
		// false->0; true->1
		boolean[] everyBite = new boolean[byteArray.length * 8];	

		byte mask = 0x01;	// ����
		for (int arrOffset = 0; arrOffset < byteArray.length; ++arrOffset) {
			int biteOffset = byteArray.length - arrOffset - 1;
			mask = 0x01;	// ��λ����
			for (int eleOffset = 0; eleOffset < 8; ++eleOffset) {
				everyBite[eleOffset + biteOffset * 8] =  (mask == (byteArray[arrOffset] & mask));
				mask <<= 1;	// ����һλ
			}
		}
		
		// ת��Ϊʮ����
		double length = 0;
		for (int i = 0; i < byteArray.length * 8; ++i) {
			if (everyBite[i]) {
				length += Math.pow(2, i);
			}
		}
		
		if (length > Integer.MAX_VALUE) {
			Log.e(TAG, "convertToDeci:	������Χ���");
			return -1;
		}
		
		return (int) length;
	}
	
	
	/**
	 * ת��10���Ƶ�16����
	 * @param 	deciNum		��ת����ʮ������;final����;
	 * @param	byteSize	���byte���鹲byteSize�ֽ�
	 * @return	����ת�����16���Ƶ�byte����
	 */
	private byte[] convertToHex(final long deciNum, int byteSize) {
		if (byteSize <= 0) {
			Log.e(TAG, "convertToHex:	��������ֽ���Ϊ��");
			return null;
		}
		
		double integer = deciNum;
		double maxNum = Math.pow(2, byteSize * 8);
		// 65535 = 0xFF 0xFF; 65536 * 1byte = 64K
		if ((integer < 0) || (integer >= maxNum)) {
			Log.e(TAG, "convertToHex:	��ת��������Χ����");
			return null;
		}
		
		byte[] hexInteger = new byte[byteSize];
		int offset = 0;
		double base = maxNum / 2;
		byte mask = 0x01;	// 0000 0001b
		
		for (int i = 0; i < byteSize * 8; ++i) {
			if (integer >= base) {
				integer -= base;
				mask = 0x01;	// 0000 0001b
				offset = (int) (i / 8);
				mask <<= (7 - (i % 8));	// ����7 - i(0~7)λ,��λ����
				
				hexInteger[offset] = (byte) (hexInteger[offset] | mask);

				if (0 == integer) {
					break;
				}
			}
			
			base /= 2;
		}
		return hexInteger;
	}
	
    /**
     * ���ڲ�������Ϣ�ӿں���
     * @param what	Message.what
     * @param obj	Message.obj
     */
    private void SendMsgToUI(int what, Object obj) {
    	Message msg = new Message();
    	msg.what = what;
    	msg.obj = obj;
    	mHandler.sendMessage(msg);
    }
	
    
}
