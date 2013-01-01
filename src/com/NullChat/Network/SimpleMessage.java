package com.NullChat.Network;

import java.io.UnsupportedEncodingException;

public class SimpleMessage implements ChatMessage {
	private String text;
	private byte[] rawText;

	public SimpleMessage(String text) throws UnsupportedEncodingException {
		makeMessage(text);
	}

	private void makeMessage(String text) throws UnsupportedEncodingException {
		if (text.length() > MAX_POST_LENGTH - 1)
			text = text.substring(0, MAX_POST_LENGTH - 1);
		this.text = text;
		rawText = text.getBytes(CHARSET);
	}

	@Override
	public byte[] getBytes() {
		return rawText;
	}

	public String getText() {
		return text;
	}
}
