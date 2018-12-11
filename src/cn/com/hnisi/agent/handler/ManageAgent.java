package cn.com.hnisi.agent.handler;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;

import cn.com.hnisi.config.AgentConfig;
import cn.com.hnisi.util.AixCheck;
import cn.com.hnisi.util.Tool;

public class ManageAgent {
	static Logger log = Logger.getLogger(ManageAgent.class);

	/**
	 * 初始化环境
	 */
	public void initEnvironment() {
		System.setProperty("file.encoding", AgentConfig.getEncoding());
		System.setProperty("line.separator", "\r\n");
		if (Tool.isWindows()) {
			// 获取程序启动路径
			String path = System.getProperty("user.dir");
			File file = new File(path);
			if (file.getName().equals("bin")) {
				path = file.getParent();
			}
			// 64位JVM只能加载 64位的DLL，32位JVM也能只加载对应的32位DLL
			if (System.getProperty("sun.arch.data.model").contains("64")) {
				path = path + "/lib/x64/WinCmd.dll";
			} else {
				path = path + "/lib/x32/WinCmd.dll";
			}
			File dllFile = new File(path);
			if (!dllFile.exists()) {
				log.error("提示：未找到程序所需DLL文件:" + path);
				return;
			}
			// 加载DLL
			System.load(path);
		}
	}

	public static void main(String[] args) {
		ManageAgent ma = new ManageAgent();
		ma.initEnvironment();
		if (args != null && args.length > 0) {
			if (args[0].toLowerCase().equals("start")) {
				log.info("Starting agent server");
				new AgentListener();//启动监听器
			} else if (args[0].toLowerCase().equals("stop")) {
				log.info("Stoping agent server");
				stop();
			}
		} else {
			log.info("Please choose args \"start\" or \"stop\"");
		}
	}

	/**
	 * 停止Agent
	 */
	public static void stop() {
		String port = AgentConfig.getPort();
		if (port == null) {
			log.error("停止Agent服务失败，原因: 未找到配置参数[Agent监听端口]");
			return;
		}

		String pid = null;
		if (Tool.isAix()) {
			//判断能否使用rmsock xxx tcpcb命令
			if (AixCheck.canUseRmsock()) {
				log.info("Is root,using rmsock method");
				pid = Tool.findPidByPortAix(port);
				AixKill(pid);
			} else {
				log.info("Not root,using grep method");
				// 因为Aix的普通用户不能直接通过rmsock、kdb获取端口号对应PID，只能通过使用ps |grep方式查找
				// 根据启动命令去查找:java -Dfile.encoding=UTF-8 -Xms256m -Xmx1024m -cp bin/Agent.jar cn.com.hnisi.agent.handler.ManageAgent start
				// ps -ef|grep cn.com.hnisi.agent.handler.ManageAgent
				List<String> pidList = Tool
						.findPidByCommandAix(ManageAgent.class.getName());
				for (String p : pidList) {
					AixKill(p);
				}
			}
		} else if (Tool.isLinux()) {
			pid = Tool.findPidByPortLinux(port);
			LinuxKill(pid);
		} else if (Tool.isWindows()) {
			pid = Tool.findPidByPortWindows(port);
			WindowsKill(pid);
		}
		log.info("Agent server has stopped");
	}

	public static void AixKill(String pid) {
		if (pid != null) {
			Tool.KillProcessByPidAix(pid);
		}
	}

	public static void WindowsKill(String pid) {
		if (pid != null) {
			Tool.killProcessByPidWindows(pid);
		}
	}

	public static void LinuxKill(String pid) {
		if (pid != null) {
			Tool.KillProcessByPidLinux(pid);
		}
	}
}
