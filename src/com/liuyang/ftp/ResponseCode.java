package com.liuyang.ftp;

import java.util.Arrays;

public enum ResponseCode {
	/**数据连接已打开，传输启动。*/
	APPE_125("APPE", "125", "125 %s", true),
	LIST_150("LIST", "150", "150 Here comes the directory listing.", true),
	_150_1("RETR", "150", "Opening BINARY mode data connection for %s.", true),
	_150_2("STOU", "150", "FILE: %s", true),
	/**文件状态没问题，准备打开数据进行连接。*/
	APPE_150("APPE", "150", "FILE: %s", true),
	/**NOOP (no operation) 命令执行成功。*/
	NOOP_200("NOOP", "200", "200 NOOP ok.", true),
	_200_0("OPTS", "200", "200 Always in UTF8 mode.", true),
	TYPE_200("TYPE", "200", "200 Switching to %s mode.", true),
	_213_0("STAT", "213", "213-Status follows", true),
	_213_1("STAT", "213", "213 End of status", true),
	_214_0("HELP", "214", "214-The following commands are recognized.", true),
	_214_1("HELP", "214", "214 Help OK", true),
	_215("SYST", "215", "215 %s Type: %s", true),
	_220("CONN", "220", "", true),
	_221("QUIT", "221 Goodbye", "226 Directory send OK.", true),
	_226_0("LIST", "226", "226 Directory send OK.", true),
	_226_1("RETR", "226", "226 Transfer complete.", true),
	/**关闭数据连接，请求的文件操作已成功。*/
	APPE_226("APPE", "226", "226 Transfer complete.", true),
	_227("PASV", "227 Entering Passive Mode ", "", true),
	_230_0("PASS", "230", "230 Login successful.", true),
	_230_1("PASS", "230", "230 Already logged in.", true),
	_250_0("CWD", "250", "250 Directory successfully changed.", true),
	_250_1("DELE", "250", "250 Delete operation successful.", true),
	APPE_250("APPE", "250", "250 %s.", true),
	_257_0("PWD", "257", "257 %s", true),
	_257_1("XMKD", "257", "257 %s created", true),
	/**提示输入指定用户的密码登陆。*/
	USER_331("USER", "331", "331 Please specify the password", true),
	_332("", "332", "", true),
	_421_0("LIST", "421", "421 Timeout.", false),
	APPE_421("APPE", "421", "421 %s", false),
	/**服务不可用，关闭控制连接。*/
	NOOP_421("NOOP", "421", "421 %s", false),
	_425_0("LIST", "425", "425 Use PORT or PASV first.", false),
	_425_1("APPE", "425", "425 Use PORT or PASV first.", false),
	/**请求的文件操作无法执行，文件不可用（例如文件正忙）。*/
	APPE_450("APPE", "450", "450 %s", false),
	/**请求的操作被中止，处理中发生本地错误。*/
	APPE_451("APPE", "451", "451 %s", false),
	/**未实现的TYPE命令*/
	TYPE_500("TYPE", "500", "Unrecognised TYPE command.", false),
	/**PASS 拒绝登陆。*/
	PASS_530("PASS", "530", "530 Login incorrect.", false),
	DELE_550("DELE", "550", "550 Delete operation failed.", false),
	/**RETR 不能打开文件。*/
	RETR_550("RETR", "550", "550 Failed to open file.", false),
	/**XMKD 创建目录操作失败。*/
	XMKD_550("XMKD", "550", "550 Create directory operation failed.", false);

	private String method, code, desc;
	private boolean status;
	
	private ResponseCode(String method, String transcode, String descritpion, boolean status) {
		this.method = method;
		this.code = transcode;
		this.desc = descritpion;
		this.status = status;
	}
	
	public String description() {
		return desc;
	}
	
	public String method() {
		return method;
	}
	
	public String code() {
		return code;
	}
	
	public boolean status() {
		return status;
	}
	
	public static boolean containsMethod(String method) {
		return  Arrays.stream(values()).filter(e -> e.method().equals(method)).count() > 0;
	}
	
	/**
	 * Get method response codes.
	 * @param method
	 * @return
	 */
	public static String[] code(String method) {
		return (method == null ? Arrays.stream(values()) : Arrays.stream(values()).filter(e -> e.method().equals(method)))
				.map(e -> e.code())
				.distinct()
				.toArray(n -> new String[n]);
				
	}
	
	/**
	 * Get respose code status.
	 * @param code
	 * @return
	 */
	public static boolean status(String code) {
		return Arrays.stream(ResponseCode.values())
				.filter(e -> e.code().equals(code))
				.mapToInt(e -> e.status() ? 1 : -1)
				.sum() >= 0;
	}
	
	/**
	 * Get respose code status of method.
	 * @param code
	 * @return
	 */
	public static boolean status(String method, String code) {
		return Arrays.stream(ResponseCode.values())
				.filter(e -> e.method().equals(method) && e.code().equals(code))
				.mapToInt(e -> e.status() ? 1 : -1)
				.sum() >= 0;
	}
}
