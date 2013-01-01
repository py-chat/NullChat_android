package com.NullChat.Network;
 

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.NullChat.Activities.ChatActivity;

import android.util.Log;

 
public class NullChatClient implements Runnable {
	
	public final String TAG="NullChatClient";
    public static final int STATE_NONE = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_HANDSHAKING = 2;
    public static final int STATE_CONNECTED = 3;
    public static final int STATE_DISCONNECTED = 4;
    
    
    
    private final static String[] HANDSHAKE_TAG = { "<handshake version=4 token=", "/>" };
    private String token ="0b2203acf7f9d9d754fadbaee34ef42b";
    
    private int state = STATE_NONE;
    private int port = 1984;
    private String chatIP;
	private URL tokenPage;
	private Socket s;
	private ChatUI UI;
	private boolean useTokenPage = false;
	
	public static boolean IS_CAPTCHA_ENTERED = false;
	
	public NullChatClient(ChatUI ui, String chatIP, int port, String tokenPage)
			throws MalformedURLException {
		this.UI = ui;
		this.chatIP = chatIP;
		this.port = port;
		useTokenPage = true;
		this.tokenPage = new URL(tokenPage);
	}

	public NullChatClient(ChatUI ui, String chatIP, int port) {
		this.UI = ui;
		this.chatIP = chatIP;
		this.port = port;
		useTokenPage = false;
	}

	public int getState() {
		return state;
	}

	@Override
	public void run() {
		try {
			Log.i(TAG, "run()");
			state = STATE_CONNECTING;
			if (connect()) {
				state = STATE_CONNECTED;
				Log.i(TAG, "run(): receive()");
				receive();
			}
		} catch (Exception ex) {
			Log.e(TAG, "run():", ex);
		}
	}

	public boolean connect() {
		try {
			UI.message(new ComplexMessage(ComplexMessage.DEFAULT,
					"Connecting..."));
			IS_CAPTCHA_ENTERED = false;
			s = new Socket(chatIP, port);
			UI.message(new ComplexMessage(ComplexMessage.DEFAULT,
					"Handshaking..."));
			if (useTokenPage) {
				String t = getToken();
				token = t != null ? t : token;
			}
			String handshake = HANDSHAKE_TAG[0] + token + HANDSHAKE_TAG[1];
			send(handshake);
			IS_CAPTCHA_ENTERED = false;
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private String getToken() {
        InputStream input = null;
        byte[] bytes = null;
        int n = - 1;

        try {
            input = tokenPage.openStream();
            byte[] buffer = new byte[4096];
            ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);

			while ((n = input.read(buffer)) != -1) {
				if (n > 0)
					baos.write(buffer, 0, n);
			}
			bytes = baos.toByteArray();
		} catch (IOException ex) {
			Log.e(TAG, "getToken()", ex);
			return null;
		}

        String r = new String(bytes, 0, bytes.length);
        int tStart = r.indexOf("token=", 0) + 6;
        int tEnd = tStart + 32;
        String token = r.substring(tStart, tEnd).trim();
        return token;
    }

    void receive() {
        while (state == STATE_CONNECTED) {
            try {
                ComplexMessage m = new ComplexMessage(s.getInputStream());
                if(m.getType() == ComplexMessage.CLEAR){
                	NullChatClient.IS_CAPTCHA_ENTERED = true;
                }
                UI.message(m);
            } catch (Exception ex) {
            	disconnect();
            	break;
            }
        }
    }

	public void disconnect() {
		if (state != STATE_DISCONNECTED) {
			try {
				state = STATE_DISCONNECTED;
				s.close();
			} catch (Exception ex) {
				Logger.getLogger(NullChatClient.class.getName()).log(
						Level.SEVERE, null, ex);
			}
			UI.clientDisconnected();
		}
	}
    
      public void send(String toSend) {
        try {
            s.getOutputStream().write(toSend.getBytes());
        } catch (IOException ex) {
            Logger.getLogger(NullChatClient.class.getName()).log(Level.SEVERE, null, ex);
            disconnect();
        }
    }
    
    public void send(ChatMessage mes) {
        try {
            s.getOutputStream().write(mes.getBytes());
        } catch (IOException ex) {
            Logger.getLogger(NullChatClient.class.getName()).log(Level.SEVERE, null, ex);
            disconnect();
        }
    }
}
