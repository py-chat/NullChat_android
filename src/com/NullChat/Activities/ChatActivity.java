package com.NullChat.Activities;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;

import com.NullChat.R;
import com.NullChat.Network.NullChatClient;
import com.NullChat.Service.ChatService;

public class ChatActivity extends Activity {
	public static String TAG = "ChatActivity";

	private Button buttonSend;
	private EditText editTextOut;
	private WebView webView;

	private boolean connected = false;

	private String radio = "--";
	private String online = "--";

	private String radBTitle;
	private String conBTitle;
	private String onlBTitle;

	private StringBuilder html;
	private String css;

	private Messenger mService = null;
	private boolean mIsBound;
	private final Messenger mMessenger = new Messenger(new IncomingHandler());

	private boolean fullscreen = false;
	private boolean atBottom = true;
	private boolean useTokenPage = true;
	private String serverAddr;
	private int port;
	private String tokenPage;
	private String nick;
	private boolean is_use_nick=false;
	
	

	public final String DEFAULT_SERVER = "d.py-chat.tk";
	public final int DEFAULT_PORT = 1984;
	public final String DEFAULT_TOKEN_PAGE = "http://py-chat.tk";
	public final String DEFAULT_NICK = "**<Аноним>** ";

	class IncomingHandler extends Handler {
		Bundle b = null;
		String data = null;

		@Override
		public void handleMessage(Message msg) {
			Log.i(TAG, "IncomingHandler.handleMessage()");
			switch (msg.what) {
			case ChatService.MSG_POST:
				b = msg.getData();
				data = b.getString(new Integer(ChatService.MSG_POST).toString());
				Log.v(TAG, "handleMessage(): post=" + data);
				addPost(data);
				break;
			case ChatService.MSG_ONLINE:
				b = msg.getData();
				data = b.getString(new Integer(ChatService.MSG_ONLINE)
						.toString());
				online = data;
				Log.v(TAG, "handleMessage(): online=" + online);
				break;
			case ChatService.MSG_RADIO:
				b = msg.getData();
				data = b.getString(new Integer(ChatService.MSG_RADIO)
						.toString());
				radio = data;
				Log.v(TAG, "handleMessage(): radio=" + radio);
				break;
			case ChatService.MSG_IMAGE:
				b = msg.getData();
				data = b.getString(new Integer(ChatService.MSG_IMAGE)
						.toString());
				addImage(data);
				break;
			case ChatService.MSG_DISCONNECT:
				Log.i(TAG, "IncomingHandler.handleMessage(): connected=false");
				connected = false;
				setTitles();
				addPost("Disconnected.");
				break;
			case ChatService.MSG_CONNECT:
				NullChatClient.IS_CAPTCHA_ENTERED = true;
				Log.i(TAG, "IncomingHandler.handleMessage(): connected=true");
				connected = true;
				setTitles();
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	public ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = new Messenger(service);
			Log.i(TAG, "Atached to service");
			try {
				Message msg = Message.obtain(null,
						ChatService.MSG_REGISTER_CHAT_ACT);
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected - process crashed.
			mService = null;
			Log.i("onServiceDisconnected()", "Detached to service");
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
				
		editTextOut = (EditText) findViewById(R.id.edit_text_out);
		
		buttonSend = (Button) findViewById(R.id.button_send);
		
		buttonSend.setOnClickListener(btnSendListener);
		
		webView = (WebView) findViewById(R.id.webView);
		webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setWebViewClient(webViewClient);

		radBTitle = getResources().getString(R.string.radio_button);
		onlBTitle = getResources().getString(R.string.online_button);
		conBTitle = getResources().getString(R.string.connect_button);
		// restoreMe(savedInstanceState);
		
//TODO fullscreen
		
	/*	if (fullscreen) {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}*/
		
		loadPreferences();
		
		html = new StringBuilder();
		html.append(css);
		
		startService();
		doBindService();
		setTitles();
	}

	private OnClickListener btnSendListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			sendPost();
		}
	};

	private void sendPost() {
		if (connected) {
			String toSend = editTextOut.getText().toString().trim();
			
			if(is_use_nick && NullChatClient.IS_CAPTCHA_ENTERED==true){
				toSend = nick + " " + toSend;
			}
			if (!toSend.isEmpty()) {
				sendMessageToService(toSend, ChatService.MSG_POST);
				editTextOut.setText(null);
			}
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			switch (keyCode) {
			case KeyEvent.KEYCODE_SEARCH:
			case KeyEvent.KEYCODE_ENTER:
				sendPost();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	private WebViewClient webViewClient = new WebViewClient() {
		@Override
		public void onPageFinished(WebView view, String url) {
			if (atBottom == true) {
				webView.loadUrl("javascript:window.scrollTo(0, document.body.scrollHeight);");
				}
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			if (url != null) {
				if (url.startsWith("event:insert,")) {
					String linkTo = " >>"
							+ url.replaceFirst("event:insert,", "") + " ";
					int pos = editTextOut.getSelectionStart();
					Editable text = editTextOut.getText().insert(pos, linkTo);
					editTextOut.setText(text);
					editTextOut.setSelection(pos + linkTo.length());
				} else
					try {
						view.getContext().startActivity(
								new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
					} catch (Exception e) {
						return false;
					}
				return true;
			} else {
				return false;
			}
		}
	};

	@Override
	public void onResume() {
		loadPreferences();
		super.onResume();
	}

	private void loadPreferences() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		useTokenPage = prefs.getBoolean("pr_use_token_page", true);
		serverAddr = prefs.getString("pr_server_addr", DEFAULT_SERVER);
		nick = prefs.getString("pr_nick", DEFAULT_NICK);
		is_use_nick = prefs.getBoolean("pr_is_use_nick", false);
		
		String p = prefs.getString("pr_server_port", null);
		try {
			port = Integer.valueOf(p);
		} catch (Exception e) {
			port = DEFAULT_PORT;
		}

		//fullscreen=prefs.getBoolean("pr_fullscreen", false);
		boolean hideSB = prefs.getBoolean("pr_hide_send_button", true);
				
		if (hideSB)
			buttonSend.setVisibility(View.GONE);
		else
			buttonSend.setVisibility(View.VISIBLE);

		tokenPage = prefs.getString("pr_token_page", DEFAULT_TOKEN_PAGE);
		css = "<style>"
				+ prefs.getString("pr_css",
						getResources().getString(R.string.pr_css)) + "</style>";
	}

	/*
	 * @Override protected void onSaveInstanceState(Bundle outState) {
	 * Log.i(TAG, "onSaveInstanceState()"); super.onSaveInstanceState(outState);
	 * outState.putString("html", html.toString()); }
	 * 
	 * private void restoreMe(Bundle state) { Log.i(TAG, "restoreMe()"); if
	 * (state!=null) Log.i(TAG, "restoreMe(): state!=null");
	 * html.append(state.getString("html")); }
	 */

	@Override
	public void onDestroy() {
		super.onDestroy();
		try {
			doUnbindService();
		} catch (Throwable t) {
			Log.e(TAG, "Failed to unbind from the service", t);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.icon_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.connect:
			connect();
			return true;
		case R.id.clear:
			clear();
			return true;
		case R.id.settings:
			Intent i = new Intent(this, Preferences.class);
			startActivity(i);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		Log.i(TAG, "onPrepareOptionsMenu(): online=" + online + " radio="
				+ radio);
		menu.findItem(R.id.online).setTitle(onlBTitle.concat(online));
		menu.findItem(R.id.radio).setTitle(radBTitle.concat(radio));
		menu.findItem(R.id.connect).setTitle(conBTitle);
		return super.onPrepareOptionsMenu(menu);
	}

	private void startService() {
		startService(new Intent(ChatActivity.this, ChatService.class));
	}

	private void doBindService() {
		try {
			bindService(new Intent(this, ChatService.class), mConnection,
					Context.BIND_AUTO_CREATE);
			mIsBound = true;
			Log.i(TAG, "Binded.");
		} catch (Exception e) {
			Log.e(TAG, "Binding exception.", e);
		}
	}

	private void doUnbindService() {
		if (mIsBound) {
			// If we have received the service, and hence registered with it,
			// then now is the time to unregister.
			if (mService != null) {
				try {
					Message msg = Message.obtain(null,
							ChatService.MSG_UNREGISTER_CHAT_ACT);
					msg.replyTo = mMessenger;
					mService.send(msg);
				} catch (RemoteException e) {
					// There is nothing special we need to do if the service has
					// crashed.
				}
			}
			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
			Log.i(TAG, "Unbinding");
		}
	}

	private void setTitles() {
		if (connected) {
			conBTitle = getResources().getString(R.string.disconnect_button);
		} else {
			online = "--";
			radio = "--";
			conBTitle = getResources().getString(R.string.connect_button);
		}
	}

	private void sendConnectMessage() {
		if (mIsBound) {
			if (mService != null) {
				try {
					Log.i(TAG, "sendConnectMessage(): ip=" + serverAddr
							+ " port=" + port + "tokenPage=" + tokenPage);
					Bundle b = new Bundle();
					b.putString(new Integer(ChatService.MSG_SERVER_ADDRESS)
							.toString(), serverAddr);
					b.putInt(
							new Integer(ChatService.MSG_SERVER_PORT).toString(),
							port);
					if (useTokenPage)
						b.putString(new Integer(ChatService.MSG_TOKEN_PAGE)
								.toString(), tokenPage);
					Message msg = Message.obtain(null, ChatService.MSG_CONNECT);
					msg.setData(b);
					msg.replyTo = mMessenger;
					mService.send(msg);
				} catch (RemoteException e) {
					Log.e(TAG, "sendConnectMessage()", e);
				}
			} else {
				Log.i(TAG, "sendConnectMessage(): mService is null");
			}
		}
	}

	private void sendDisconnectMessage() {
		Log.i(TAG,
				"sendMessageToService: mIsBound: "
						+ new Boolean(mIsBound).toString());
		if (mIsBound) {
			if (mService != null) {
				try {
					Message msg = Message.obtain(null,
							ChatService.MSG_DISCONNECT);
					msg.replyTo = mMessenger;
					mService.send(msg);
				} catch (RemoteException e) {
					Log.i(TAG, "sendDisconnectMessage():", e);
				}
			} else {
				Log.i(TAG, "sendDisconnectMessage(): mService is null");
			}
		}
	}

	private void sendMessageToService(String toSend, int type) {
		Log.i(TAG,
				"sendMessageToService: mIsBound: "
						+ new Boolean(mIsBound).toString());

		if (mIsBound) {
			if (mService != null) {
				try {
					Bundle b = new Bundle();
					b.putString(new Integer(type).toString(), toSend);
					Message msg = Message.obtain(null, type);
					msg.setData(b);
					msg.replyTo = mMessenger;
					mService.send(msg);
					Log.i(TAG, "sendMessageToService(): toSend=" + toSend);
				} catch (RemoteException e) {
				}
			} else {
				Log.i(TAG, "sendMessageToService(): mService is null, toSend="
						+ toSend);
			}
		}
	}

	private void addPost(String reply) {
		if ((webView.getContentHeight() * webView.getScale() - webView
				.getScrollY()) - webView.getHeight() < 10)
			atBottom = true;
		else
			atBottom = false;
		html.append(reply);
		html.append("<br>");
		webView.loadDataWithBaseURL(null, html.toString(), null, "utf-8", null);
	}

	private void addImage(String reply) {
		Log.i(TAG, "addImage(): reply=" + reply);
		html.append("<img src=" + reply + " /><br>");
		html.append("<br>");
		webView.loadDataWithBaseURL(null, html.toString(), null, "utf-8", null);
	}

	private void clear() {
		html = new StringBuilder();
		html.append(css);
		webView.loadDataWithBaseURL(null, html.toString(), null, "utf-8", null);
	}

	private void connect() {
		if (!connected) {
			clear();
			Log.i(TAG, "connect(): ip=" + serverAddr + " port=" + port
					+ " tokenPage=" + tokenPage);
			sendConnectMessage();
		} else {
			Log.v(TAG, "onClick: Stopping service.");
			sendDisconnectMessage();
		}
	}
}
