package com.liuyang.ftp;

import java.util.ArrayList;
import java.util.List;

public class FtpResponse {
	private String method;
	private List<ResponseMessage> messages;
	private int count = 0;
	
	public FtpResponse(String method) {
		this.method = method;
		this.messages = new ArrayList<ResponseMessage>();
		
	}
	
	@Override
	protected void finalize() {
		messages.clear();
		method = null;
		messages = null;
	}
	
	public void parse(String msg) {
		ResponseMessage message = new ResponseMessage(msg);
		messages.add(message);
		count += ResponseCode.status(method, message.code) ? 1 : -1;
		message = null;
	}
	
	public boolean status() {
		return count > 0;
	}
	
	public List<ResponseMessage> messages() {
		return messages;
	}
	
	public void print() {
		messages.forEach(m -> {
			System.out.println("  >> " + m.message);
		});
	}
}
