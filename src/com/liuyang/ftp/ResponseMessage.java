package com.liuyang.ftp;

public class ResponseMessage {
	protected String message;
	protected String code;
	protected String word;
	
	public ResponseMessage(String msg) {
		message = msg;
		code = msg.substring(0, 3);
		word = msg.substring(3);
	}
	
	@Override
	protected void finalize() {
		message = null;
		code = null;
		word = null;
	}
	
	@Override
	public String toString() {
		return String.format("{code: \"%s\", word: \"%s\", message: \"%s\"", code, word, message);
	}
}
