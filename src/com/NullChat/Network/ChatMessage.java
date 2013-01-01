package com.NullChat.Network;

public interface ChatMessage {
	public static final String CHARSET = "utf-8";
	public static final int MAX_POST_LENGTH = 255;

	public byte[] getBytes();

	public String getText();
}
