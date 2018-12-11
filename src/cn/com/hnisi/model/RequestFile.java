package cn.com.hnisi.model;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;

public class RequestFile implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/** 文件名长度 */
	private int nameLength;
	/** 文件名 */
	private byte[] fileName;
	/** 文件长度 */
	private long contentLength;
	/** 文件内容 */
	private byte[] contents;

	public RequestFile() {
	}

	public RequestFile(int nameLength, byte[] fileName, int contentLength,
			byte[] contents) {
		this.nameLength = nameLength;
		this.fileName = fileName;
		this.contentLength = contentLength;
		this.contents = contents;
	}

	public int getNameLength() {
		return nameLength;
	}

	public void setNameLength(int nameLength) {
		this.nameLength = nameLength;
	}

	public byte[] getFileName() {
		return fileName;
	}

	public void setFileName(byte[] fileName) {
		this.fileName = fileName;
	}

	public long getContentLength() {
		return contentLength;
	}

	public void setContentLength(long contentLength) {
		this.contentLength = contentLength;
	}

	public byte[] getContents() {
		return contents;
	}

	public void setContents(byte[] contents) {
		this.contents = contents;
	}

	@Override
	public String toString() {
		try {
			return "[ nameLength : " + nameLength + " ,fileName : "
					+ new String(fileName,"UTF-8") + " ,contentLength : "
					+ contentLength + " ,contents : " + contentLength + "]";
		} catch (UnsupportedEncodingException e) {
			return e.getMessage();
		}
	}

}
