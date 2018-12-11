package cn.com.hnisi.security;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import cn.com.hnisi.config.AgentConfig;

/**
 * 设置Agent密码
 * @author FengGeGe
 *
 */
public class AgentPassword {
	private static Properties props=new Properties();
	
	public static String getPassword(){
		try {
			if(!AgentConfig.BOOT_FILE.exists()){
				AgentConfig.BOOT_FILE.createNewFile();
			}
			InputStream in = new BufferedInputStream (new FileInputStream(AgentConfig.BOOT_FILE));
			props.load(in);
			return props.getProperty("pwd");
		} catch (Exception e) {
			return null;
		}
		
	}
	/**
	 * 修改Agent密码
	 * @param password
	 * 密码
	 * @param remark
	 * 备注修改
	 * @return
	 */
	public static boolean changePassword(String password,String remark){
		OutputStream fos;
		try {
			if(!AgentConfig.BOOT_FILE.exists()){
				AgentConfig.BOOT_FILE.createNewFile();
			}
			fos = new FileOutputStream(AgentConfig.BOOT_FILE);
			props.setProperty("pwd", password);   
			props.store(fos, "Modified by "+remark);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}   
		return true;
	}
}
