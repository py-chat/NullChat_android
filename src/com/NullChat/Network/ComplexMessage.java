package com.NullChat.Network;

import java.io.*;

public class ComplexMessage implements ChatMessage {
	public static final String TAG = "Message";

	public final static byte DEFAULT = 0x00;
	public final static byte POST = 0x01;
	public final static byte ONLINE = 0x02;
	public final static byte ERROR = 0x03;
	public final static byte RADIO = 0x04;
	public final static byte IMAGE = 0x05;
	public final static byte CLEAR = 0x07;
	private byte type = DEFAULT;
	private String message = null;
	private byte[] rawMessage;
	
	public ComplexMessage(InputStream is) throws Exception {
		readMessage(is);
	}

	public ComplexMessage(byte type, String message)
			throws UnsupportedEncodingException {
		this.type = type;
		this.message = message;
		this.rawMessage = makeMessage(message, type);
	}

	public byte getType() {
		return type;
	}

	public String getText() {
		return message;
	}

	public byte[] getBytes() {
		return rawMessage;
	}

	public void setType(byte type) {
		this.type = type;
	}

	private void saveMessage(byte length[], byte type, byte[] data)
			throws UnsupportedEncodingException {
		this.type = type;
		rawMessage = glueMessage(length, type, data);
		message = new String(data, CHARSET);
	}

	private void readMessage(InputStream is) throws Exception {
		byte size[] = new byte[2];
		byte cmd[] = new byte[1];

		if (is.read(size, 0, 2) == -1)
			throw new Exception();
		if (is.read(cmd, 0, 1) == -1)
			throw new Exception();

		int i_size = getUnsignedInt(size);
		byte data[] = readAll(is, i_size);

		saveMessage(size, cmd[0], data);
	}

	private static byte[] makeMessage(String text, byte type)
			throws UnsupportedEncodingException {
		if (type == POST && text.length() > MAX_POST_LENGTH - 1)
			text = text.substring(0, MAX_POST_LENGTH - 1);
		byte[] mes = text.getBytes(CHARSET);
		byte[] size = getUnsignedByte(mes.length);
		return glueMessage(size, type, mes);
	}

	private static byte[] glueMessage(byte[] length, byte type, byte[] data) {
		byte[] gluedMessage = new byte[data.length + 3];
		gluedMessage[0] = length[0];
		gluedMessage[1] = length[1];
		gluedMessage[2] = type;
		System.arraycopy(data, 0, gluedMessage, 3, data.length);
		return gluedMessage;
	}

	private static int getUnsignedInt(byte[] value) {
		int unsigned = ((int) value[0] & 0xff) << 8;
		unsigned += (int) value[1] & 0xff;
		return unsigned;
	}

	private static byte[] getUnsignedByte(int value) {
		int i = (((int) value & 0xff00) >> 8);
		int ii = (((int) value & 0xff));

		byte[] size = new byte[2];
		size[0] = (byte) i;
		size[1] = (byte) ii;
		return size;
	}

	private static byte[] readAll(InputStream is, int size) throws IOException {
		byte[] data = new byte[size];
		int i = is.read(data, 0, size);

		while (i < size) {
			int j = size - i;
			j = is.read(data, i, j);
			if (j == -1)
				throw new EOFException();
			i += j;
		}
		return data;
	}
}
