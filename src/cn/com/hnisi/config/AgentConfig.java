package cn.com.hnisi.config;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import cn.com.hnisi.util.Tool;
import cn.com.hnisi.util.XmlUtil;

public class AgentConfig {
	static Logger log = Logger.getLogger(AgentConfig.class);
	public static String AppPath = System.getProperty("user.dir");
	/**
	 * 配置文件
	 */
	public static File CONFIG_FILE = new File(AppPath + "/config/config.xml");
	/**
	 * 密码文件
	 */
	public static File BOOT_FILE = new File(AppPath + "/config/boot.properties");

	/**
	 * 获取线程池大小
	 * 
	 * @return
	 */
	public static int getThreadPool() {
		String timeout = XmlUtil.getNodeText(CONFIG_FILE,
				"/root/agent/thread-pool");
		try {
			return Integer.parseInt(timeout);
		} catch (Exception e) {
			return 10;
		}
	}

	/**
	 * 获取编码方式，默认为UTF-8
	 * 
	 * @return
	 */
	public static String getEncoding() {
		String encoding = XmlUtil.getNodeText(CONFIG_FILE,
				"/root/agent/encoding");
		if (encoding != null) {
			try {
				"".getBytes(encoding);
				return encoding;
			} catch (UnsupportedEncodingException e) {
				return "UTF-8";
			}
		} else {
			return "UTF-8";
		}
	}

	/**
	 * 获取会话超时时间,最长时间为1小时
	 * 
	 * @return
	 */
	public static int getTimeOut() {
		String timeout = XmlUtil
				.getNodeText(CONFIG_FILE, "/root/agent/timeout");
		try {
			if (Integer.parseInt(timeout) >= 3600000) {
				// 最大60分钟（1小时）
				return 3600000;
			} else if (Integer.parseInt(timeout) <= 30000) {
				// 最小半分钟
				return 30000;
			} else {
				return Integer.parseInt(timeout);
			}
		} catch (Exception e) {
			return 60000;
		}
	}

	/**
	 * 获取配置文件节点内容，未找到返回NULL
	 * 
	 * @param nodeExpression
	 * @return
	 */
	public static String getNodeTextContent(String nodeExpression) {
		return XmlUtil.getNodeText(CONFIG_FILE, nodeExpression);
	}

	/**
	 * 获取IP
	 * 
	 * @return
	 */
	public static String getIp() {
		String ip = XmlUtil.getNodeText(CONFIG_FILE, "/root/agent/ip");
		if (ip == null || ip.equals("")) {
			ip=Tool.getIp();
			if(ip==null){
				 ip="127.0.0.1";
			}
		}else{
			if(ip.equals("127.0.0.1")||ip.toLowerCase().equals("localhost")){
				ip=Tool.getIp();
			}		
		}
		return ip;
	}

	/**
	 * 获取监听端口
	 * 
	 * @return
	 */
	public static String getPort() {
		return XmlUtil.getNodeText(CONFIG_FILE, "/root/agent/port");
	}

	/**
	 * 获取允许删除文件目录列表，为空-NULL时是不能删除服务器文件
	 * @return List
	 */
	public static List<String> getPermissionDeletePath(){
		String paths=XmlUtil.getNodeText(CONFIG_FILE, "/root/agent/permission-delete-path");
		if(paths!=null){
			String[] ps=paths.split(";");
			if(ps!=null && ps.length>0){
				final List<String> pathList=new ArrayList<String>();
				for(String p:ps){
					
					File verifyPath=new File(p);
					if(verifyPath.exists()){
						pathList.add(p);
					}
				}
				//只有文件目录存在有效数大于0时，才返回实例列表
				if(pathList.size()>0){
					return pathList;
				}
			}
		}
		return null;
	}
	
	public static void main(String[] args){
		System.out.println(getPermissionDeletePath());
	}
}
