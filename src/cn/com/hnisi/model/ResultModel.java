package cn.com.hnisi.model;

import java.io.Serializable;

import org.apache.log4j.Logger;

import cn.com.hnisi.type.Status;

/**
 * 请求和返回：处理结果对象
 * 
 * @author FengGeGe
 * 
 */
public class ResultModel implements Serializable {
	static Logger log = Logger.getLogger(ResultModel.class);
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * -1表示失败，0表示成功
	 */
	private int code = 0;
	/**
	 * 返回信息
	 */
	private String msg = "";

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		if (code < -1) {
			code = -1;
		}
		if (code > 0) {
			code = 0;
		}
		this.code = code;
	}

	public String getMsg() {
		return msg;
	}

	/**
	 * 带符号"<..message..>"
	 * 
	 * @param message
	 */
	public void setMsg(String message) {
		if (message == null) {
			msg = "";
		}
		this.msg = "<" + message + ">";
	}

	public void setCodeMsg(int code, String message) {

		this.code = code;
		if (message == null) {
			msg = "";
		}
		this.msg = "<" + message + ">";

	}

	public void setCodeMsg(Status status, String message) {
		if (status != null) {
			this.code = status.getValue();
		} else {
			this.code = 99;//99表示异常
		}
		if (message == null) {
			msg = "";
		}
		this.msg = "<" + message + ">";

	}
}
