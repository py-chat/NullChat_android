package com.NullChat.Service;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.ArrayBlockingQueue;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.NullChat.Activities.ChatActivity;
import com.NullChat.Network.ChatUI;
import com.NullChat.Network.ComplexMessage;
import com.NullChat.Network.SimpleMessage;
import com.NullChat.Network.NullChatClient;

public class ChatService extends Service implements ChatUI {

	public final static int TO_SAVE = 10;
	private ArrayBlockingQueue<String> lastPosts = new ArrayBlockingQueue<String>(
			TO_SAVE);

	private String lastOnline;
	private String lastRadio;

	private static boolean isRunning = false;
	public static final String TAG = "ChatService";
	public static final int MSG_REGISTER_CHAT_ACT = 1;
	public static final int MSG_UNREGISTER_CHAT_ACT = 2;

	public static final int MSG_CONNECT = 3;
	public static final int MSG_DISCONNECT = 4;

	public static final int MSG_SERVER_ADDRESS = 5;
	public static final int MSG_SERVER_PORT = 6;
	public static final int MSG_TOKEN_PAGE = 7;

	public static final int MSG_POST = 8;
	public static final int MSG_RADIO = 9;
	public static final int MSG_ONLINE = 10;
	public static final int MSG_IMAGE = 11;

	private static String ip;
	private static int port;
	private static String tokenPage;

	private Messenger chatMes = null;

	private static NullChatClient chat = null;
	final Messenger mMessenger = new Messenger(new IncomingHandler());

	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Bundle b = null;
			switch (msg.what) {
			case MSG_REGISTER_CHAT_ACT:
				chatMes = msg.replyTo;
				if (isConnected()) {
					sendMessageToAct("", MSG_CONNECT);
					sendLastMessages();
				}
				Log.i(TAG, "handleMessage(): MSG_REGISTER_CHAT_ACT");
				break;

			case MSG_POST:
				b = msg.getData();
				String post = b.getString(new Integer(MSG_POST).toString());
				Log.i(TAG, "handleMessage(): MSG_POST, post=" + post);
				sendMessageToChatCl(post);
				break;

			case MSG_CONNECT:
				NullChatClient.IS_CAPTCHA_ENTERED = true;
				b = msg.getData();
				String addr = b.getString(new Integer(MSG_SERVER_ADDRESS)
						.toString());
				ip = addr != null ? addr : ip;

				int p = b.getInt(new Integer(MSG_SERVER_PORT).toString());
				port = p != 0 ? p : port;

				String tPage = b.getString(new Integer(MSG_TOKEN_PAGE)
						.toString());
				// tokenPage = tPage != null ? tPage : tokenPage;
				tokenPage = tPage;

				Log.i(TAG, "handleMessage(): MSG_CONNECT, ip=" + addr
						+ " port=" + p);
				startChatCl();
				break;

			case MSG_DISCONNECT:
				Log.i(TAG, "handleMessage(): MSG_DISCONNECT");
				disconnectChatCl();
				break;

			case MSG_UNREGISTER_CHAT_ACT:
				Log.i(TAG, "handleMessage(): MSG_UNREGISTER_CHAT_ACT");
				chatMes = null;
				break;

			default:
				Log.i(TAG, "handleMessage(): default");
				super.handleMessage(msg);
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.i(TAG, "onBind()");
		return mMessenger.getBinder();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "onCreate()");
		isRunning = true;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (chat != null) {
			disconnectChatCl();
		}
		Log.i(TAG, "Service Stopped.");
		isRunning = false;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "Received start id " + startId + ": " + intent);
		return START_STICKY; // run until explicitly stopped.
	}

	public static boolean isRunning() {
		return isRunning;
	}

	private void sendLastMessages() {
		StringBuilder sb = new StringBuilder();
		for (String s : lastPosts) {
			sb.append(s);
			sb.append("<br>");
		}
		sendMessageToAct(sb.toString(), MSG_POST);
		sendMessageToAct(lastOnline, MSG_ONLINE);
		sendMessageToAct(lastRadio, MSG_RADIO);
	}

	private void savePost(String message) {
		if (lastPosts.size() == TO_SAVE) {
			lastPosts.poll();
		}
		lastPosts.add(message);
	}

	public static boolean isConnected() {
		if (chat == null)
			return false;
		else
			return chat.getState() == NullChatClient.STATE_CONNECTED
					|| chat.getState() == NullChatClient.STATE_CONNECTING;
	}

	private void disconnectChatCl() {
		if (chat != null || chat.getState() != NullChatClient.STATE_NONE) {
			chat.disconnect();
		}
	}

	public void clientDisconnected(){
		lastPosts = new ArrayBlockingQueue<String>(TO_SAVE);
		sendMessageToAct("", MSG_DISCONNECT);		
	}
	
	
	public void message(ComplexMessage m) {
		Log.v(TAG, "message(): reply=" + m.getText() + " type=" + m.getType());
		switch (m.getType()) {
		default:
		case ComplexMessage.POST:
			sendMessageToAct(m.getText(), MSG_POST);
			savePost(m.getText());
			break;
		case ComplexMessage.IMAGE:
			sendMessageToAct(m.getText(), MSG_IMAGE);
			break;
		case ComplexMessage.ONLINE:
			sendMessageToAct(m.getText(), MSG_ONLINE);
			lastOnline = m.getText();
			break;
		case ComplexMessage.RADIO:
			sendMessageToAct(m.getText(), MSG_RADIO);
			lastRadio = m.getText();
			break;
		}
	}

	private void sendMessageToAct(String toSend, int type) {
		Message msg = null;
		msg = Message.obtain(null, type);
		
		if (toSend!=null) {
			Bundle b = new Bundle();
			b.putString(new Integer(type).toString(), toSend);
			msg.setData(b);
		}
		try {
			chatMes.send(msg);
		} catch (RemoteException e) {
			Log.e(TAG, "sendMessageToAct()", e);
		}
	}

	private void sendMessageToChatCl(String toSend) {
		if (chat != null)
			try {
				chat.send(new SimpleMessage(toSend));
				Log.i(TAG, "sendMessageToChatCl() " + toSend);
			} catch (UnsupportedEncodingException e) {
				Log.e(TAG, "sendMessageToChatCl():", e);
			}
	}

	private void startChatCl() {
		try {
			if (tokenPage == null)
				chat = new NullChatClient(this, ip, port);
			else
				chat = new NullChatClient(this, ip, port, tokenPage);
			Thread t = new Thread(chat);
			t.start();
			sendMessageToAct("", MSG_CONNECT);
			//chatMes.send(new Message().obtain(null, MSG_CONNECT));
			Log.v(TAG, "startChatCl(): ChatClient thread started");
		} catch (Exception e) {
			chat = null;
			Log.e(TAG, "Exception in startChatCl()", e);
		}
	}
}
