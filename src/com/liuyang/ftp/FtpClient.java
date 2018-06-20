package com.liuyang.ftp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Ftp Client Manager
 * 
 * <li>2018/5/12 created by liuyang.</li>
 * <li>2018/5/24 modify {@code CloseSream} to {@code CloseStream}.</li>
 * <li>2018/5/24 add {@code lines} method.</li>
 * @see FTP-RFC959
 * @author liuyang
 * @version 1.0.1
 *
 */
public class FtpClient {
	public static final int DEFAULT_PORT = 21;
	private Logger logger = Logger.getLogger(FtpClient.class);
	
	private Socket client = null;
    private Socket passiveClient = null; // 被动模式客户端
    private Socket activeClient = null; // 主动模式客户端
    private ServerSocket activeServer = null; // 主动模式服务端
    
    private int timeoutMills = 3000;
    private int activePort = 39881;
	
	private InputStream  in = null;
	private OutputStream out = null;
	
	private BufferedReader reader = null;
	private BufferedWriter writer = null;

	private boolean isPassiveModeEnable = true; // 默认打开被动模式, false表示主动模式
	private String encode = "UTF8"; // 默认使用UTF8编码
	private boolean isReceivingMode = false; // 判断是否在使用主/被动模式接收数据
	private boolean isSendingMode = false; // 判断是否在使用主/被动模式发送数据
	
	private String host = null;
	private int    port = 21;
	private String username = null;
	private String password = null;
	
	private int    rtt = 1; // TCP建链轮循时间, 也是等待时间, 毫秒
	private int    retryLimit = 3; // TCP轮循尝试次数

	public FtpClient() {
		
	}
	
	@Override
	protected void finalize() {
		// 关闭连接
		close();
		// 释放内存资源
		client = null;
		passiveClient = null;
		activeServer = null;
		activeClient = null;
		activePort = 0;
		timeoutMills = 0;
		in = null;
		out = null;
		reader = null;
		writer = null;
		isPassiveModeEnable = false;
		encode = null;
		host  = null;
		port = 0;
		username = null;
		password = null;
		rtt = 0;
		retryLimit = 0;
	}
	
	/**
	 * 
	 * @throws FtpClientException
	 */
	private void connectionCheck() throws FtpClientException {
		if (client == null) throw new NullPointerException();
		if (client.isConnected() == false) throw new FtpClientException("connection not initial.");
		if (client.isClosed() == true) throw new FtpClientException("not connect.");
	}
	
	/**
	 * if true means passive mode not opend.
	 * @return
	 */
	private boolean passiveModeCheck() {
		if (client == null) throw new NullPointerException();
		if (client.isConnected() && !client.isClosed() && passiveClient == null) return true;
		//System.err.println("passiveClient.isConnected()=" + passiveClient.isConnected());
		//System.err.println("passiveClient.isClosed()=" + passiveClient.isClosed());
		if (client.isConnected() && !client.isClosed() && !passiveClient.isConnected()) return true;
		if (client.isConnected() && !client.isClosed() && passiveClient.isClosed()) return true;
		return false;
	}
	
	/**
	 * if true means active mode not opend.
	 * @return
	 */
	private boolean activeModeCheck() {
		if (client == null) throw new NullPointerException();
		if (client.isConnected() && !client.isClosed() && activeServer == null) return true;
		if (client.isConnected() && !client.isClosed() && activeServer.isClosed()) return true;
		return false;
	}
	
	private synchronized void closePassiveMode() throws FtpClientException {
		try {
			if (!passiveModeCheck()) passiveClient.close();
		} catch (IOException e) {
			throw new FtpClientException("close activeServer fial." + e.getMessage());
		} finally {
			passiveClient = null;
		}
	}
	
	private synchronized void closeActiveMode() throws FtpClientException {
		try {
			if (!activeModeCheck()) activeServer.close();
		} catch (IOException e) {
			throw new FtpClientException("close passiveClient fial." + e.getMessage());
		} finally {
			activeServer = null;
		}
	}
	
	/**
	 * close connection.
	 */
	public synchronized void close() {
		try {
			connectionCheck();
			client.close();
		} catch (FtpClientException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			client = null;
		}
		
	}
	
	/**
	 * Thread sleep
	 * @param millis
	 */
	private void sleep(long millis) {
		try {
			Thread.currentThread();
			Thread.sleep(millis); 
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			
		}
	}
	
	/**
	 * Create a connction, use specify host and port to connect ftp server.
	 * @param host
	 * @param port
	 * @return
	 * @throws FtpClientException
	 */
	public boolean connect(String host, int port) throws FtpClientException {
		long start = System.nanoTime(), end = System.nanoTime();
		try {
			if(client != null) close(); // 需要关闭之前的连接
			client = new Socket();
			client.connect(new InetSocketAddress(host, port), timeoutMills);
			end = System.nanoTime();
		} catch (IOException e) {
			client = null;
			throw new FtpClientException("connect fial." + e.getMessage());
		}
		try {
			in  = client.getInputStream();
			reader = new BufferedReader(new InputStreamReader(in));
		} catch (IOException e) {
			throw new FtpClientException("get inputStream fial." + e.getMessage());
		}
		try {
			out = client.getOutputStream();
			writer = new BufferedWriter(new OutputStreamWriter(out));
		} catch (IOException e) {
			throw new FtpClientException("get outputStream fial." + e.getMessage());
		}
		// 获取tcp建链时延（即服务器传递数据的时延会略大于该时延，因此获取数据的间隔时间也要略大于该时延）
		this.rtt = (int) ((end - start) / 1000000 * 2);
		this.host = host;
	    this.port = port;
		response("CONNECT", true, false, "220");
		opts(encode, true);
		return true;
	}
	
	/**
	 * Create a connction, use specify host to connect ftp server.
	 * @param host
	 * @return
	 * @throws FtpClientException
	 */
	public boolean connect(String host) throws FtpClientException {
		return connect(host, DEFAULT_PORT);
	}
	
	/**
	 * Create a connction, use specify host and port to connect ftp server. <br>
	 * If connect succful, then try use the specify username and passwrod login ftp server.
	 * @param host
	 * @param port
	 * @param username
	 * @param password
	 * @return
	 * @throws FtpClientException
	 */
	public boolean connect(String host, int port, String username, String password) throws FtpClientException {
		connect(host, port);
		user(username);
		pass(password);
		this.username = username;
		this.password = password;
		return true;
	}
	
	/**
	 * Ftp file outputstream mode.
	 * <li>APPEND</li>
	 * <li>OVERWRITE</li>
	 * <li>UNIQUEU</li>
	 * @author liuyang
	 * @version 1.0.1
	 */
	public enum Mode {
		/**追加。如果存在则在文件末尾追加。*/
		APPEND,
		/**覆盖。如果存在，则覆盖原文件。*/
		OVERWRITE,
		/**唯一命名。即：如果存在同名文件则重命名后储存。*/
		UNIQUEU;
	}
	
	/**
	 * Tests whether the file or directory denoted by this abstract pathname exists.
	 * @param path
	 * @return
	 * @throws FtpClientException
	 */
	public synchronized boolean exists(String path) throws FtpClientException {
		if (request("STAT", path)) return 
				response("STAT", true, true)
				    .messages().stream()
				    .filter(e -> !e.message.startsWith("213"))
				    .count() > 0;
		return false;
	}
	
	public synchronized InputStream getActiveInputStream() throws FtpClientException {
		if (activeModeCheck()) 
			throw new FtpClientException("get active mode inputstream fial. because active mode not initial.");
		try {
			return activeClient.getInputStream();
		} catch (IOException e) {
			throw new FtpClientException("get active mode inputstream fial." + e.getMessage());
		}
	}
	
	public synchronized OutputStream getActiveOutputStream() throws FtpClientException {
		if (activeModeCheck()) 
			throw new FtpClientException("get active mode outputstream fial. because active mode not initial.");
		try {
			return activeClient.getOutputStream();
		} catch (IOException e) {
			throw new FtpClientException("get active mode outputstream fial." + e.getMessage());
		}
	}
	
	public synchronized InputStream getPassiveInputStream() throws FtpClientException {
		if (passiveModeCheck()) 
			throw new FtpClientException("get passive mode inputstream fial. because passive mode not initial.");
		try {
			return passiveClient.getInputStream();
		} catch (IOException e) {
			throw new FtpClientException("get passive mode inputstream fial." + e.getMessage());
		}
	}
	
	public synchronized OutputStream getPassiveOutputStream() throws FtpClientException {
		if (passiveModeCheck()) throw new FtpClientException("get passive mode outputstream fial. because passive mode not initial.");
		try {
			return passiveClient.getOutputStream();
		} catch (IOException e) {
			throw new FtpClientException("get passive mode outputstream fial." + e.getMessage());
		}
	}
	
	/**
	 * it will be get the file list from path. you can use specify the regular expression to match.
	 * the result not contains <code>". .."</code>
	 * @param path
	 * @param pattern the regular expression. see the {@link Pattern}.
	 * @return
	 * @throws FtpClientException
	 */
	public synchronized List<FtpFile> getFileList(String path, String pattern) throws FtpClientException {
		List<FtpFile> ftpFlieList = new ArrayList<FtpFile>();
		// 打开被动模式
		if (isPassiveModeEnable) {
			if (passiveModeCheck()) pasv();
		} else {
			if (activeModeCheck()) port(activePort);
		}
		// 如果处于接收模式，则抛出异常
        if (isReceivingMode) throw new FtpClientException("in receiving mode.");
		// 发送命令&& 首次获取响应
		if (list(path).status() == true) {
            BufferedReader br;
			try {
				Pattern p = Pattern.compile(pattern == null ? "([\\S\\s]+)" : pattern);
				String line;
				br = new BufferedReader(new InputStreamReader(
						isPassiveModeEnable ? getPassiveInputStream() : getActiveInputStream()));
				// 开始从服务器读取消息
				isReceivingMode = true;
				while((line = br.readLine()) != null) {
					FtpFile ftpFile = new FtpFile(path, line);
					if (p.matcher(ftpFile.getFileName()).matches() == true)
					    ftpFlieList.add(ftpFile);
					ftpFile = null;
				}
				p = null;
				line = null;
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				isReceivingMode = false;
				br = null;
			}
			response("LIST"); // 再次获取响应
		}
		return ftpFlieList;
	}
	
	/**
	 * it will be get the file list from path.
	 * the result not contains <code>". .."</code>
	 * @param path
	 * @return
	 * @throws FtpClientException
	 */
	public synchronized List<FtpFile> getFileList(String path) throws FtpClientException {
		return getFileList(path, null);
	}
	
	/**
	 * Get remote file data from ftp server and write the remote file data to the specify local file.<br>
	 * Must enable passive mode at first.
	 * @param remotePath
	 * @param localPath
	 * @param mode see {@link Mode}. Topic: the UNIQUEU mode unuse.
	 * @return
	 * @throws FtpClientException
	 */
	public synchronized long copyRemoteFileToLocal(String remotePath, String localPath, Mode mode) throws FtpClientException {
		long length = 0;
		File dstFile = new File(localPath);
		if (!dstFile.getParentFile().exists()) dstFile.mkdirs();
		// 先确保本地文件可以进行操作
		FileOutputStream fos;
		try {
			switch (mode) {
			case APPEND:
				fos = new FileOutputStream(dstFile, true);
				break;
			case OVERWRITE:
			default :
				fos = new FileOutputStream(dstFile, false);
				break;
			}
		} catch (IOException e) {
			throw new FtpClientException(e.getMessage());
		}
        // 打开远程文件流读取数据并写到本地文件
		InputStream fin;
		try {
			fin = openStream(remotePath);
			byte[] buff = new byte[4096];
			int len = 0;
			// 开始从服务器读取消息
			while((len = fin.read(buff, 0, buff.length)) != -1) {
				fos.write(buff, 0, len);
				length += len;
			}
			fos.close();
			buff = null;
			len = 0;
		} catch (IOException e) {
			e.printStackTrace();
			throw new FtpClientException(e.getMessage());
		} finally {
			closeStream();
			fin = null;
			fos = null;
		}
		return length;
	}
	
	/**
	 * 
	 * @param localFile
	 * @param remotePath
	 * @param mode see {@link Mode}
	 * @return
	 * @throws FtpClientException
	 */
	@SuppressWarnings("resource")
	public synchronized long copyLocalFileToRemote(String localFile, String remotePath, Mode mode) throws FtpClientException {
		long length = 0;
		File srcFile = new File(localFile);
		if (!srcFile.getParentFile().exists()) srcFile.getParentFile().mkdirs();
		// 先确保本地文件可以进行操作
		FileInputStream fin;
		try {
			fin = new FileInputStream(localFile);
		} catch (IOException e) {
			throw new FtpClientException(e.getMessage());
		}
        // 打开远程文件流读取数据并写到本地文件
		OutputStream fos;
		try {
			fos = createStream(remotePath + "/" + srcFile.getName(), mode);
			byte[] buff = new byte[4096];
			int len = 0;
			// 开始从服务器读取消息
			while((len = fin.read(buff, 0, buff.length)) != -1) {
				fos.write(buff, 0, len);
				length += len;
			}
			fos.close();
			buff = null;
			len = 0;
		} catch (IOException e) {
			e.printStackTrace();
			throw new FtpClientException(e.getMessage());
		} finally {
			closeStream();
			fin = null;
			fos = null;
		}
		return length;
	}
	
	/**
	 * Create the remote file outputstream. <br>
	 * At the end, must use <code>closeStream()</code> to close outputstream.
	 * <li>2018/6/20 optimize by liuyang. append NULL check. </li>
	 * @param remotePath
	 * @param mode see {@link Mode}
	 * @return
	 * @throws FtpClientException
	 */
	public synchronized OutputStream createStream(String remotePath, Mode mode) throws FtpClientException {
		//logger.debug("createStream >> isSendingMode:" + isSendingMode + " isReceivingMode:" + isReceivingMode);
		// 打开主/被动模式
		if (isPassiveModeEnable) {
			pasv();
		} else {
			port(activePort);
		}
		// 如果处于发送模式，则抛出异常
        if (isSendingMode) throw new FtpClientException("in sending mode.");
        FtpResponse response = null;
        OutputStream retval = null;
		// 发送命令 && 首次获取响应
        switch (mode) {
        case APPEND:
        	response = appe(remotePath); break;
        case OVERWRITE:
        	response = stor(remotePath); break;
        case UNIQUEU:
        	response = stou(remotePath); break;
        }
        if (!response.status()) throw new FtpClientException("response failure. " + response);
        // 打开输出流
        retval = isPassiveModeEnable ? getPassiveOutputStream() : getActiveOutputStream();
        if (retval == null) throw new FtpClientException("can not create outputstream. " + response);
        isSendingMode = true;
        response = null;
		return retval;
	}
	
	/**
	 * Open the remote file inputstream. <br>
	 * At the end, must use <code>closeStream()</code> to close inputstream.
	 * <li>2018/6/20 optimize by liuyang. append NULL check. </li>
	 * @param remotePath
	 * @return
	 * @throws FtpClientException
	 */
	public synchronized InputStream openStream(String remotePath) throws FtpClientException {
		//logger.debug("openStream >> isSendingMode:" + isSendingMode + " isReceivingMode:" + isReceivingMode);
		// 打开主/被动模式
		if (isPassiveModeEnable) {
			pasv();
		} else {
			port(activePort);
		}
		// 如果处于接收模式，则抛出异常
        if (isReceivingMode) throw new FtpClientException("in receiving mode.");
        // 发送命令 && 首次获取响应
        FtpResponse response = retr(remotePath);
        if (!response.status()) throw new FtpClientException("response failure. because " + response);
        // 打开输入流
        InputStream retval = isPassiveModeEnable ? getPassiveInputStream() : getActiveInputStream();
        if (retval == null) throw new FtpClientException("can not open inputstream. " + response);
        isReceivingMode = true;
		response = null;
		return retval;
	}
	
	/**
	 * Close the stream which created by active mode or passive mode.
	 * @throws FtpClientException
	 */
	public synchronized void closeStream() throws FtpClientException {
		//logger.debug("closeStream >> isSendingMode:" + isSendingMode + " isReceivingMode:" + isReceivingMode);
		if (isReceivingMode == true || isSendingMode == true) {
			
			isReceivingMode = false;
			isSendingMode = false;
			if (isPassiveModeEnable) {
				closePassiveMode();
			} else {
				closeActiveMode();
			}
			// 再次获取响应, 但不指定响应类型。
			response(""); 
		}
	}
	
	/**
	 * Get local address which use to connected.
	 * @return
	 * @throws FtpClientException
	 */
	public String getLocalAddress() throws FtpClientException {
		connectionCheck();
		return client.getLocalSocketAddress().toString();
	}
	
    /**
     * Open the remote file as line stream. <br>
     * At the end, must use <code>closeStream()</code> to close stream.
     * @param remotePath
     * @return
     * @throws FtpClientException
     */
	public synchronized Stream<String> lines(String remotePath) throws FtpClientException {
		return new BufferedReader(new InputStreamReader(openStream(remotePath))).lines();
	}
	
	/**
	 * Send a request to server.
	 * @param method
	 * @param parameters
	 * @return
	 * @throws FtpClientException
	 */
	public synchronized boolean request(String method, String... parameters) throws FtpClientException {
		connectionCheck();
		logger.debug("host=%s request: method=%s parameter=%s", host, method, String.join(",", parameters));
		try {
			out.write(String.format("%s %s\r\n", method, String.join(" ", parameters)).getBytes());
			out.flush();
		} catch (IOException e) {
			throw new FtpClientException("send request fial. " + e.getMessage());
		}
		return true;
	}
	
	/**
	 * Receive response messages from server.
	 * @param method it must be same as the <b>Request Method</b>
	 * @param multi if true, means there is more than one message will be to read.
	 * @param check if true, means will check the first and the last message that is contains response code.
	 * @param responsecode for each <b>Request Method</b>, there is more than one corresponding response code.
	 * @return get the message from server
	 * @throws FtpClientException
	 */
	public synchronized FtpResponse response(String method, boolean multi, boolean check, String... responsecode) 
			throws FtpClientException 
	{
		connectionCheck();
		logger.debug("host=%s response: method=%s code=%s", host, method, String.join(",", responsecode));
		FtpResponse response = new FtpResponse(method);
		boolean bContinue = true;
		try {
			int count = 0;
			while (bContinue) {
				final String str = reader.readLine();
				response.parse(str);
				// 如果指定非多条响应则退出循环不再检测; 否则就会重复检测是否可以获取到下一条消息。
				if (!multi) break; 
				// 判断是否需要检测首行和尾行响应码。一般用于消息体, 如HELP, STAT等等.
				if (!check) {
					// 如果未获取到服务器写入状态, 则让线程体眠rtt时间, 等待服务器写入数据, 该时间可以根据服务器时延来决定
					ready: for(int i = 0; i < retryLimit; i++) {
						if (bContinue = reader.ready()) break ready;
						sleep(rtt);
					}
				} else {
					count += Arrays.stream(responsecode).filter(e -> str.startsWith(e)).count();
					// 检测到两个响应码后则会停止读取
					if (count > 1) bContinue = false;
				}
			}
			//System.out.println(response.status());
			//if (!response.status()) 
			//	throw new FtpClientException("response failure after send " + method + ". message: " + response.messages());
		} catch (IOException e) {
			throw new FtpClientException("recieve response failure after send " + method + ". case IOException. " + e.getMessage());
		} finally {
			if (Logger.enableDebug) response.print();
		}
		return response;
	}
	
	/**
	 * Receive response messages from server.
	 * @param method it must be same as the <b>Request Method</b>
	 * @param multi if true means there is more than one message to read.
	 * @param check if true means it will check the first and the last message contains transcode.
	 * @return get the message from server
	 * @throws FtpClientException
	 */
	public FtpResponse response(String method, boolean multi, boolean check) throws FtpClientException {
		return response(method, multi, check, ResponseCode.code(method));
	}
	
	/**
	 * Receive <b>one</b> response messages from server.
	 * @param method it must be same as the <b>Request Method</b>
	 * @return get the message from server
	 * @throws FtpClientException
	 */
	public FtpResponse response(String method) throws FtpClientException {
		return response(method, false, false, ResponseCode.code(method));
	}
	
	/**
	 * System privilege account.
	 * @param account
	 * @return
	 * @throws FtpClientException
	 * @deprecated <code>ACCT</code> not implemented.
	 */
	@Deprecated
	public synchronized FtpResponse acct(String account) throws FtpClientException {
		if (request("ACCT", account)) return response("ACCT");
		return null;
	}
	
	/**
	 * 
	 * @param path
	 * @return
	 * @throws FtpClientException
	 */
	public synchronized FtpResponse appe(String path) throws FtpClientException {
		if (request("APPE", path)) return response("APPE");
		return null;
	}
	
	/**
	 * Change work directory.
	 * @param path
	 * @return
	 * @throws FtpClientException
	 */
	public synchronized FtpResponse cwd(String path) throws FtpClientException {
		if (request("CWD", path)) return response("CWD");
		return null;
	}
	
	/**
	 * Delete remote file.
	 * @param path
	 * @return
	 * @throws FtpClientException
	 */
	public synchronized FtpResponse dele(String path) throws FtpClientException {
		if (request("DELE", path)) return response("DELE");
		return null;
	}
	
	/**
	 * Get help message from server.
	 * can use <code>FtpResponse.messages.stream().filter(e -> !e.startsWith("214"))</code> to get content.<br>
	 * @return
	 * @throws FtpClientException
	 */
	public synchronized FtpResponse help() throws FtpClientException {
		if (request("HELP")) return response("HELP", true, true);
		return null;
	}
	
	/**
	 * It will be use to get the file list from remote path. 
	 * But it must open passive/port mode at first,
	 * then you can use <code>getPassiveInputStream()</code>/<code>getActiveInputStream()</code>
	 * to read message from ftp server.<br>
	 * Must use <code>response("LIST")</code> to end. 
	 * The results not contains <code>". .."</code>
	 * @param path
	 * @return
	 * @throws FtpClientException
	 */
	public synchronized FtpResponse list(String path) throws FtpClientException {
		if (request("LIST", path)) return response("LIST"); 
		return null;
	}
	
	/**
	 * Specify transmission mode.
     * @param mode One of the following ASCII character value:
     * 	 <li>S -- Stream Mode （流模式）</li>
	 *   <li>B -- Block Mode （块模式）</li>
	 *   <li>C -- Compress Mode（压缩模式）</li>
	 * @return 
	 * @throws FtpClientException
	 */
	public synchronized FtpResponse mode(String mode) throws FtpClientException {
		if (request("MODE", mode)) return response("MODE"); 
		return null;
	}
	
	/**
	 * Not any operation to do.
	 * @return
	 * @throws FtpClientException
	 */
	public synchronized FtpResponse noop() throws FtpClientException {
		if (request("NOOP")) return response("NOOP"); 
		return null;
	}
	
	/**
	 * Set operation.
	 * @param opeartion
	 * @param flag
	 * @return
	 * @throws FtpClientException
	 */
	public synchronized FtpResponse opts(String opeartion, boolean flag) throws FtpClientException {
		if (request("OPTS", opeartion + " " + (flag ? "ON" : "OFF"))) 
			return response("OPTS");
		return null;
	}

	/**
	 * Use password to login ftp server.
	 * @param password
	 * @return
	 * @throws FtpClientException
	 */
	public synchronized FtpResponse pass(String password) throws FtpClientException {
		if (request("PASS", password)) return response("PASS");
		return null;
	}
	
	/**
	 * Open and enter passive mode. use ipv4 protocl.
	 * @return
	 * @throws FtpClientException
	 */
	public synchronized FtpResponse pasv() throws FtpClientException {
		FtpResponse response = null;
		// 强制关闭之前打开的主/被动模式的流操作对象。
		closeStream();
		// 发送命令 获取首次响应
		if (request("PASV") && (response = response("PASV")).status()) {
			String ipInfo = response.messages().get(0).message;
			//System.out.println(response.messages);
			String[] passiveinfo = ipInfo.substring(ipInfo.indexOf("(") + 1, ipInfo.indexOf(")")).split(",");
			//String[] passiveinfo = Pattern.compile("\\((\\w+)\\)").matcher(ipInfo).group(1).split(","); //ipInfo.substring(ipInfo.indexOf("(") + 1, ipInfo.indexOf(")")).split(",");
			String passiveHost = passiveinfo[0] + "." + passiveinfo[1] + "."+ passiveinfo[2] + "."+ passiveinfo[3];
			int passivePort = Integer.valueOf(passiveinfo[4]) * 256 + Integer.valueOf(passiveinfo[5]);
			try {
				//System.err.println("try open passive mode.");
				passiveClient = new Socket();
				passiveClient.connect(new InetSocketAddress(passiveHost, passivePort), 3000);
				//System.err.println("open passive mode succful. " + passiveClient.isConnected());
			} catch (IOException e) {
				throw new FtpClientException("enter passive mode fial." + e.getMessage());
			} finally {
				ipInfo = null;
				passiveinfo = null;
				passiveHost = null;
				passivePort = 0;
			}
			return response;
		} else {
			throw new FtpClientException("enter passive mode fial. " + response.messages());
		}
	}
	
	/**
	 * Open active mode. use ipv4 protocol.
	 * @param port
	 * @return
	 * @throws FtpClientException
	 */
	public synchronized FtpResponse port(int port) throws FtpClientException {
		FtpResponse response = null;
		String activePort = getLocalAddress()
				.replaceAll("/",  "")
				.replace(".", ",") + "," + (port - port % 256) / 256 + "," + port % 256;
		// 强制关闭之前打开的主/被动模式的流操作对象。
		closeStream();
		// 发送命令 获取首次响应
		if (request("PORT", activePort) && (response = response("PORT")).status()) {
			try {
				activeServer = new ServerSocket(port);
				for(int i = 0; i < retryLimit; i++) {
					activeClient = activeServer.accept();
					if (activeClient.getRemoteSocketAddress().toString().equals(host)) {
						break;
					}
					activeClient = null;
				}
			} catch (IOException e) {
				throw new FtpClientException("enter passive mode fial." + e.getMessage());
			} finally {
				activePort = null;
			}
			return response;
		} else {
			throw new FtpClientException("enter passive mode fial. " + response.messages());
		}
	}
	
	public synchronized FtpResponse pwd() throws FtpClientException {
		if (request("PWD")) return response("PWD");
		return null;
	}
	
	/**
	 * quit and logout
	 * @return
	 * @throws FtpClientException
	 */
	public synchronized FtpResponse quit() throws FtpClientException {
		if (request("QUIT")) return response("QUIT");
		return null;
	}
	
	/**
	 * Copy file from ftp server. but it must open passive/port mode at first.<br>
	 * then you can use <code>getPassiveInputStream()</code>/<code>getActiveInputStream()</code>
	 * to read data from ftp server.<br>
	 * @param path
	 * @return
	 * @throws FtpClientException
	 */
	public synchronized FtpResponse retr(String path) throws FtpClientException {
		if (request("RETR")) return response("RETR");
		return null;
	}
	
	/**
	 * It will be get the file list from path and not use passive/active mode.
	 * can use <code>FtpResponse.messages.stream().filter(e -> !e.startsWith("213"))</code> to get content.<br>
	 * then use <code>FtpFile.parse()</code> to parse the message. <br>
	 * the results contains <code>". .."</code>
	 * @param path
	 * @return
	 * @throws FtpClientException
	 * @deprecated this functions only can read a few message (less than 1024 bytes) from server. 
	 *             you can use <code>LIST</code> command to get more.
	 */
	@Deprecated
	public synchronized FtpResponse stat(String path) throws FtpClientException {
		if (request("STAT", path)) return response("STAT", true, true);
		return null;
	}
	
	/**
	 * Get the system information of server.
	 * @return
	 * @throws FtpClientException
	 */
	public synchronized FtpResponse syst() throws FtpClientException {
		if (request("SYST")) return response("SYST");
		return null;
	}
	
	/**
	 * Create remote file on ftp server, <b> if the remote file exists, rename and create. </b> <br>
	 * But it must open passive/port mode at first, 
	 * then you can use <code>getPassiveOutputStream()</code>/<code>getActiveOutputStream()</code>
	 * to write data to ftp server.<br>
	 * @param remotepaht
	 * @param localpath
	 * @return
	 * @throws FtpClientException
	 */
	public synchronized FtpResponse stou(String remotepath) throws FtpClientException {
		if (request("STOU", remotepath)) return response("STOU");
		return null;
	}
	
	/**
	 * Create remote file on ftp server, <b> if the remote file exists, overwrite. </b> <br>
	 * But it must open passive/port mode at first, 
	 * then you can use <code>getPassiveOutputStream()</code>/<code>getActiveOutputStream()</code>
	 * to write data to ftp server.<br>
	 * @param remotepaht
	 * @param localpath
	 * @return
	 * @throws FtpClientException
	 */
	public synchronized FtpResponse stor(String remotepaht) throws FtpClientException {
		if (request("STOR", remotepaht)) return response("STOR");
		return null;
	}
	
	/**
	 * To determine the data transmission mode. 
	 * The most typical case is the use of TYPE command switching between in ASCII or binary mode.<br>
	 * Default types are ASCII Nonprint.
	 * If the <code>format</code> parameter changes, then only the first parameter changed, 
	 * the <code>format</code> will be immediately returned to the default Nonprint.
	 * @param format Assign the following code:
	 *        <li>A -- ASCII*, as ASCII mode, only used in transmitting text.</li>
	 *        <li>E -- EBCDIC*</li>
	 *        <li>I -- IMAGE, as BINARY mode, used in transmitting binary data (ex. image).</li>
	 *        <li>L <byte size>  -- Local byte size.</li>
	 *        <b>*</b> A and E type of the second parameter is used as follows one of the three:
	 *        <li>N -- Nonprint</li>
	 *        <li>T -- Telnet format effector</li>
	 *        <li>C -- Carriage Control (ASA)</li>
	 * @return
	 * @throws FtpClientException
	 */
	public synchronized FtpResponse type(String format) throws FtpClientException {
		if (request("TYPE", format)) return response("TYPE");
		return null;
	}
	
	/**
	 * set ftp user name to login
	 * @param username
	 * @return
	 * @throws FtpClientException
	 */
	public synchronized FtpResponse user(String username) throws FtpClientException {
		if (request("USER", username)) return response("USER");
		return null;
	}
	
	/**
	 * Make a directory on ftp server.
	 * @param path
	 * @return
	 * @throws FtpClientException
	 */
	public synchronized FtpResponse xmkd(String path) throws FtpClientException {
		if (request("XMKD", path)) return response("XMKD");
		return null;
	}
	
	/**
	 * Remove the directory with path.
	 * @param path
	 * @return
	 * @throws FtpClientException
	 */
	public synchronized FtpResponse xrmd(String path) throws FtpClientException {
		if (request("XRMD", path)) return response("XRMD");
		return null;
	}
	
	
	
}
