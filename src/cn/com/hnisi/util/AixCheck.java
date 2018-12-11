package cn.com.hnisi.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

import cn.com.hnisi.config.AgentConfig;

public class AixCheck {
	static Logger log = Logger.getLogger(AixCheck.class);
	/**
	 * 判断能否使用rmsock命令
	 * rmsock命令主要是用来通过port端口号查找对应的pid，因为AIX系统下普通用户不能使用，所以需要检查。
	 * @return
	 * 可以调用则返回True，否则返回False
	 */
	public static boolean canUseRmsock(){
		Process p = null;
		ProcessBuilder pb = null;
		try {
			String[] cmd = { 
					"/bin/sh", "-c", 
					"rmsock 1 tcpcb" };
			pb = new ProcessBuilder(cmd);
			pb.redirectErrorStream(true);
			p = pb.start();
			p.getOutputStream().close();
			String console = "";
			BufferedReader in = new BufferedReader(new InputStreamReader(
					p.getInputStream(), AgentConfig.getEncoding()));
			while ((console = in.readLine()) != null) {
				if(console.toLowerCase().contains("error in opening")){
						//没有管理员权限
					    log.info("No administrator privileges");
						return false;
				}
			}
			
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	/**
	 * 判断端口是否已经被使用
	 * @param port
	 * @return
	 * 已经被使用则返回True，没有被使用则返回False
	 */
	public static boolean checkPortStatus(String port){
		Process p = null;
		ProcessBuilder pb = null;
		try {
			String[] cmd = { 
					"/bin/sh", "-c", 
					"netstat -Aon|grep ."+port };
			pb = new ProcessBuilder(cmd);
			pb.redirectErrorStream(true);
			p = pb.start();
			p.getOutputStream().close();
			String console = "";
			BufferedReader in = new BufferedReader(new InputStreamReader(
					p.getInputStream(), AgentConfig.getEncoding()));
			/* 查询结果:
			 *  f1000e00338a03b8 tcp        0      0  127.0.0.1.6666        *.*                   LISTEN
			 *  f1000e00275b73b8 tcp        0      0  128.110.9.70.6666     *.*                   LISTEN
			 *  f1000e00a0097bb8 tcp4       0      0  128.110.9.70.6666     20.120.104.207.45558  TIME_WAIT
			 *  f1000e0002526bb8 tcp4       0      0  128.110.9.70.6666     20.120.104.204.45616  TIME_WAIT
			 */
			while ((console = in.readLine()) != null) {
				if(console.toUpperCase().contains("LISTEN")){
					    return true;
				}
			}
		} catch (Exception e) {
			return false;
		}
		return false;
	}
}
