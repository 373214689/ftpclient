package com.liuyang.ftp;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * FTP文件
 * @author liuyang
 *
 */
public class FtpFile {
	
    private String fileName = null;
    private long   fileSize = 0;
    private String filePath = null;
    private String infoType = null;
    private String groupId  = null;
    private String ownerId  = null;
    private String fileAttr = null;
    private String fileDate = null;
    private Date   fileModifyDate = null;
    private String fileLinkInfo = null;
    private String hostName = null;
    private int    port = 0;
    private String username = null;
    private String password = null;
    private boolean isDirectory = false;
    private Map<String, Object> properties = new HashMap<String, Object>();
    public FtpFile (String hostName, int port, String username, String password, String pathName, String fileInfo) throws IOException {
        if(parse(hostName, port, username, password, pathName, fileInfo)==true) {
            
        } else {
            throw new IOException("invaild file link info");
        }
    }
    public FtpFile (String pathName, String fileInfo) throws IOException {
        if(parse(null, 0, null, null, pathName, fileInfo)==true) {
            
        } else {
            throw new IOException("invaild file link info");
        }
    }
    
    public FtpFile () {
        
    }
    
    /**
     * 清除数据，回收内存。
     */
    @Override
    protected void finalize() {
    	this.clear();
    	this.fileName = null;
    	this.fileSize = 0;
    	this.filePath = null;
    	this.infoType = null;
    	this.groupId  = null;
    	this.ownerId  = null;
    	this.fileAttr = null;
    	this.fileDate = null;
    	this.fileLinkInfo = null;
        this.hostName = null;
        this.port     = 0;
        this.username = null;
        this.password = null;
        this.isDirectory = false;
        this.properties = null;
    }
    
    private Date parse(String date) {
    	String[] months = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
    	String[] values = date.split(" ");
    	int month = 0, day = 0, hour = 0, minute = 0;
    	//Date parseDate = new Date();
    	Calendar calendar = Calendar.getInstance();
    	for(String name : months) {
    		if (name.equals(values[0])) break;
    		month++;
    	}
    	day = Integer.parseInt(values[1]);
    	try {
        	hour = Integer.parseInt(values[2].split(":")[0]);
        	minute = Integer.parseInt(values[2].split(":")[1]);
    	} catch (ArrayIndexOutOfBoundsException e) {
    		//System.out.println("parser data error:" + e.getMessage() + "  " + date);
    	}
    	
    	TimeZone timezone = calendar.getTimeZone();
    	timezone.setRawOffset(0);
    	calendar.set(Calendar.MONTH, month);
    	calendar.set(Calendar.DAY_OF_MONTH, day);
    	//hour + timezone.getRawOffset() / 3600000
    	calendar.set(Calendar.HOUR_OF_DAY, hour);
    	calendar.set(Calendar.MINUTE, minute);
    	calendar.set(Calendar.SECOND, 0);
    	calendar.setTimeZone(timezone);
    	//parseDate.setMonth(month);
    	//parseDate.setDate(day);
    	//parseDate.setHours(hour + 8);
    	//parseDate.setMinutes(minute);
    	//parseDate.setSeconds(0);
		return calendar.getTime();
		//return parseDate;
    	
    }
    /**
     * 解析文件信息文本行
     * @param hostName
     * @param port
     * @param username
     * @param password
     * @param pathName
     * @param fileInfo
     * @return
     */
    private boolean parse(String hostName, int port, String username, String password, String pathName, String fileInfo) {
        List<String> fileLink = Stream.of(fileInfo.split(" "))
        		.filter(str -> !str.isEmpty())
        		.collect(Collectors.toList());
        boolean bFileInfoCheck = fileLink.size() == 9;
        this.fileLinkInfo = fileInfo;
        this.fileName     = fileLink.get(8);
        this.filePath     = (pathName == null && pathName == fileName) ? "" : pathName;
        this.fileSize     = Long.parseLong(fileLink.get(4));
        this.infoType     = fileLink.get(1);
        this.groupId      = fileLink.get(2);
        this.ownerId      = fileLink.get(3);
        this.fileAttr     = fileLink.get(0);
        this.fileDate     = String.format("%s %s %s", fileLink.get(5), fileLink.get(6), fileLink.get(7));
        this.fileModifyDate = parse(fileDate);
        /*try {
			this.fileModifyDate = formatter.parse(fileDate);
		} catch (ParseException e) {
			this.fileModifyDate = null;
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
        
        this.isDirectory  = fileLink.get(1).startsWith("d");
        this.hostName     = hostName;
        this.port         = port;
        this.username     = username;
        this.password     = password;
        fileLink.clear();
        fileLink = null;
        return bFileInfoCheck;
    }
    
    public String setFileName(String fileName) {
    	String old_fileName = this.fileName;
    	this.fileName = fileName;
    	return old_fileName;
    }
    
    public long setFileSize(long fileSize) {
    	long old_fileSize = this.fileSize;
    	this.fileSize = fileSize;
    	return old_fileSize;
    }
    
    public String setFilePath(String filePath) {
    	String old_filePath = this.filePath;
    	this.filePath = filePath;
    	return old_filePath;
    }
    
    public String setLinkInfo(String linkInfo) {
    	String old_linkInfo = this.fileLinkInfo;
    	this.fileLinkInfo = linkInfo;
    	return old_linkInfo;
    }
    
    public String getFileName() {
        return fileName;
    }
    public String getFilePath() {
        return (".".equals(fileName) || "..".equals(fileName)) ? String.format("%s%s", fileName, filePath) : String.format("%s/%s", "/".equals(filePath) ? "" : filePath, fileName);
    }
    public long getFileSize() {
        return fileSize;
    }
    public String getLinkInfo() {
        return fileLinkInfo;
    }
    public Date getFileDate() {
    	return this.fileModifyDate;
    }
    public String getHostName() {
    	return hostName;
    }
    public int getPort() {
    	return port;
    }
    public String getUserName() {
    	return username;
    }
    public String getPassword() {
    	return password;
    }
    
    public Object setProperty(String name, Object value) {
    	return this.properties.put(name, value);
    }
    
    public Object getProperty(String name) {
    	return this.properties.get(name);
    }
    
    public String getPropertyString(String name) {
    	return String.valueOf(this.properties.get(name));
    }
    
    public int getPropertyInt(String name) {
    	return Integer.parseInt(this.properties.get(name).toString());
    }
    
    public long getPropertyLong(String name) {
    	return Long.parseLong(this.properties.get(name).toString());
    }
    
    public Set<String> getPropertyKeySet() {
    	return this.properties.keySet();
    }
    
    public Collection<Object> getPropertyValues() {
    	return this.properties.values();
    }
    
    public boolean isDirectory() {
    	return this.isDirectory;
    }
    
	public void clear() {

        this.properties.clear();
	}
	
    public String toString() {
        return String.format(
                "[attr=%s, infoType=%s, group=%s, owner=%s, filesize=%s, date=%s, filename=%s, filepath=%s, linkinfo=%s, properties=%s]",
                fileAttr,
                infoType,
                groupId,
                ownerId,
                fileSize,
                this.fileModifyDate,
                fileName,
                this.getFilePath(),
                fileLinkInfo,
                properties
        );
    }

}
