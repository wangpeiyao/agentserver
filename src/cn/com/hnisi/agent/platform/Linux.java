package cn.com.hnisi.agent.platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.SocketChannel;
import java.util.List;

import org.apache.log4j.Logger;

import cn.com.hnisi.agent.handler.AgentHandler;
import cn.com.hnisi.agent.interfaces.IPlatform;
import cn.com.hnisi.agent.middleware.WebLogicTool;
import cn.com.hnisi.agent.middleware.WebSphereTool;
import cn.com.hnisi.config.AgentConfig;
import cn.com.hnisi.model.ClusterServicesModel;
import cn.com.hnisi.model.ResultModel;
import cn.com.hnisi.model.ServerModel;
import cn.com.hnisi.type.PlatformType;
import cn.com.hnisi.type.Status;
import cn.com.hnisi.util.Tool;

public class Linux implements IPlatform {
	static Logger log = Logger.getLogger(Linux.class);
	AgentHandler responseHandler = null;

	public Linux(AgentHandler responseHandler) {
		this.responseHandler = responseHandler;
	}

	/**
	 * 备份应用
	 * 
	 * @param server
	 * @param socketChannel
	 */
	public void backUp(ServerModel server, SocketChannel socketChannel) {
		// TODO 备份
		ResultModel result = new ResultModel();
		Process p = null;
		ProcessBuilder pb = null;
		long startTime, endTime;
		try {

			File appPath = new File(server.getAppPath());
			if (!appPath.exists()) {
				result.setCode(-1);
				result.setMsg("备份目标“" + appPath.getPath() + "” 不存在，备份失败");
				responseHandler.sendMsgCompleted(result, socketChannel);
				return;
			}

			result.setCode(0);
			result.setMsg("正在对备份目标“" + appPath.getPath() + "” 进行备份，请稍候...");
			responseHandler.sendMsgRun(result, socketChannel);

			startTime = System.currentTimeMillis();
			// 备份后的文件路径和文件名
			String backFile = "";
			if (server.getBackUpPath() != null
					&& server.getBackUpPath().trim().length() > 0) {
				File backPath = new File(server.getBackUpPath());
				// 如果目录不存在则创建
				if (!backPath.exists()) {
					backPath.mkdirs();
				}
				backFile = Tool.formatPath(server.getBackUpPath()) + "/"
						+ appPath.getName() + Tool.getNowDateSSS()
						+ server.getDomainPort() + ".bak";
			} else {
				backFile = appPath.getPath() + Tool.getNowDateSSS()
						+ server.getDomainPort() + ".bak";
			}

			String[] commandList = { "/bin/sh", "-c",
					"tar -cf " + backFile + " " + appPath.getPath() };
			log.info("Back up command: " + commandList[0] + " "
					+ commandList[1] + " " + commandList[2]);
			pb = new ProcessBuilder(commandList);
			pb.redirectErrorStream(true);
			p = pb.start();
			p.getOutputStream().close();
			// 获取程序“退出”结果，0为已正常执行。
			int exitCode = p.waitFor();
			if (exitCode == 0) {
				endTime = System.currentTimeMillis();
				result.setCode(0);
				result.setMsg("备份成功，备份文件存放在服务器:“" + backFile + "”, 耗时: "
						+ Tool.millisecondFormat(endTime - startTime));
			} else {
				result.setCode(-1);
				result.setMsg("备份失败，操作已取消或发生异常退出");
			}
		} catch (Exception ex) {
			result.setCode(-1);
			result.setMsg("备份发生异常，原因: " + ex.getMessage());
			ex.printStackTrace();
			log.info(result.getMsg());
		}
		responseHandler.sendMsgCompleted(result, socketChannel);
	}

	/**
	 * 备份应用
	 * 
	 * @param server
	 * @param socketChannel
	 */
	public void backUpByCopy(ServerModel server, SocketChannel socketChannel) {
		// TODO 备份
		ResultModel result = new ResultModel();

		Process p = null;
		ProcessBuilder pb = null;
		BufferedReader in;
		long startTime, endTime;
		try {

			File appPath = new File(server.getAppPath());
			if (!appPath.exists()) {
				result.setCode(-1);
				result.setMsg("备份目标“" + appPath.getPath() + "” 不存在，备份失败");
				responseHandler.sendMsgCompleted(result, socketChannel);
				return;
			}
			result.setCode(0);
			result.setMsg("正在对备份目标“" + appPath.getPath() + "” 进行备份，请稍候...");
			responseHandler.sendMsgRun(result, socketChannel);

			startTime = System.currentTimeMillis();
			// 备份后的文件路径和文件名
			String newFile = "";
			if (server.getBackUpPath() != null
					&& server.getBackUpPath().trim().length() > 0) {
				File backPath = new File(server.getBackUpPath());
				// 如果目录不存在则创建
				if (!backPath.exists()) {
					backPath.mkdirs();
				}
				newFile = Tool.formatPath(server.getBackUpPath()) + "/"
						+ appPath.getName() + Tool.getNowDateSSS()
						+ server.getDomainPort();
			} else {
				newFile = appPath.getPath() + Tool.getNowDateSSS()
						+ server.getDomainPort();
			}
			String[] commandList = { "/bin/sh", "-c",
					"cp -r -v \"" + appPath.getPath() + "\" \"" + newFile +"\"" };

			pb = new ProcessBuilder(commandList);
			pb.redirectErrorStream(true);
			p = pb.start();
			p.getOutputStream().close();
			in = new BufferedReader(new InputStreamReader(p.getInputStream(),
					AgentConfig.getEncoding()));
			String console = "";
			while ((console = in.readLine()) != null) {
				result.setMsg(new String(console.getBytes(AgentConfig
						.getEncoding()), AgentConfig.getEncoding()));
				responseHandler.sendMsgRun(result, socketChannel);
			}
			endTime = System.currentTimeMillis();
			result.setCode(0);
			result.setMsg("备份成功，备份文件存放在服务器: “" + newFile + "”, 耗时: "
					+ Tool.millisecondFormat(endTime - startTime));
		} catch (Exception ex) {
			result.setCode(-1);
			result.setMsg("备份发生异常，原因: " + ex.getMessage());
			log.info(result.getMsg());
			ex.printStackTrace();
		}
		responseHandler.sendMsgCompleted(result, socketChannel);
	}

	/**
	 * 启动WebSphere
	 * 
	 * @param server
	 * @param socketChannel
	 */
	public void startWebSphere(ServerModel server, SocketChannel socketChannel) {
		ResultModel result = new ResultModel();
		Process p = null;
		ProcessBuilder pb = null;
		long start, end;
		try {

			File appSrvPath = new File(server.getDomainPath());
			if (!appSrvPath.exists()) {
				result.setCode(-1);
				result.setMsg("服务器中 “" + appSrvPath.getPath()
						+ "”不是有效的概要文件(Profile)路径，请检查配置");
				responseHandler.sendMsgCompleted(result, socketChannel);
				return;
			}

			File startServerFile = new File(appSrvPath.getPath().replace("\\",
					"/")
					+ "/bin/startServer.sh");
			if (!startServerFile.exists()) {
				result.setCode(-1);
				result.setMsg("未找到启动文件 “" + startServerFile.getPath()
						+ "” ，请检查websphere配置");
				responseHandler.sendMsgCompleted(result, socketChannel);
				return;
			}

			if (Tool.processIsRunning(PlatformType.LINUX,
					WebSphereTool.getServerPid(server))) {
				result.setCode(-1);
				result.setMsg("经检测,WebSphere服务器进程已在运行中,进程ID="
						+ WebSphereTool.getServerPid(server) + ",请确认.");
				responseHandler.sendMsgCompleted(result, socketChannel);
				return;
			}
			String[] commandList = {
					"/bin/sh",
					"-c",
					"nohup " + startServerFile.getPath() + " server1 > "
							+ appSrvPath.getPath() + "/logs/"
							+ server.getServerName() + "/nohup.out 2>&1 &" };
			pb = new ProcessBuilder(commandList);
			pb.redirectErrorStream(true);
			p = pb.start();
			start = System.currentTimeMillis();
			// 如果把输出流输出到nohup.server1文件中，则从文件中获取启动信息
			// 等待上一命令
			if (p.waitFor() == 0) {
				String[] tailCommandList = {
						"/bin/sh",
						"-c",
						"tail -f " + appSrvPath.getPath() + "/logs/"
								+ server.getServerName() + "/nohup.out" };

				pb = new ProcessBuilder(tailCommandList);
				pb.redirectErrorStream(true);
				p = pb.start();
			}
			BufferedReader in = new BufferedReader(new InputStreamReader(
					p.getInputStream(), AgentConfig.getEncoding()));
			String console = "";
			int timer = AgentConfig.getTimeOut();

			result.setCode(0);
			result.setMsg("WebSphere服务器启动中...");
			responseHandler.sendMsgRun(result, socketChannel);

			while ((console = in.readLine()) != null) {
				result.setCode(0);
				result.setMsg(console);
				if (!responseHandler.sendMsgRun(result, socketChannel)) {
					// 如果信息发送失败，则停止
					return;
				}
				if (console.toLowerCase().contains("进程标识为")
						|| console.toLowerCase().contains("process id is")) {
					result.setCode(0);
					result.setMsg("WebSphere应用已启动成功");
					responseHandler.sendMsgCompleted(result, socketChannel);
					return;
				} else if (console.toLowerCase().contains("already be running")
						|| console.toLowerCase().contains("已经有一个服务器的实例在运行")) {
					result.setCode(-1);
					result.setMsg("WebSphere应用已经有一个服务器的实例在运行");
					responseHandler.sendMsgCompleted(result, socketChannel);
					return;
				}
				end = System.currentTimeMillis();
				if (timer - (end - start) <= 0) {
					result.setCode(-1);
					result.setMsg("WebSphere加载部署时间超过系统指定时间，已断开连接，请检查应用是否正常");
					responseHandler.sendMsgCompleted(result, socketChannel);
					break;
				}
			}
		} catch (Exception ex) {
			result.setCode(-1);
			result.setMsg("启动WebSphere出错，原因：" + ex.getMessage());
			responseHandler.sendMsgCompleted(result, socketChannel);
		} finally {
			if (p != null) {
				Tool.killProcess(p);
				p.destroy();
			}
		}
	}

	/**
	 * 停止WebSphere
	 * 
	 * @param server
	 * @param socketChannel
	 */
	public void stopWebSphere(ServerModel server, SocketChannel socketChannel) {
		ResultModel result = new ResultModel();

		try {
			// 首先，校验端口号是否为WebSphere的http端口
			if (!WebSphereTool.isServerPort(server)) {
				result.setCode(-1);
				result.setMsg("停止失败，端口号 “" + server.getDomainPort()
						+ "” 并不是WebSphere应用的端口!");
				responseHandler.sendMsgCompleted(result, socketChannel);
				return;
			}
			// 根据端口查找PID
			String pid = WebSphereTool.getServerPid(server);
			// 判断进程是否运行中
			if (Tool.processIsRunning(PlatformType.LINUX, pid)) {
				String msg = Tool.KillProcessByPidLinux(String.valueOf(pid));
				result.setCode(0);
				result.setMsg("WebSphere应用服务已停止成功. "
						+ (msg != null ? "处理信息:" + msg : ""));
				responseHandler.sendMsgCompleted(result, socketChannel);
				return;
			} else {
				result.setCode(-1);
				result.setMsg("根据应用端口号 “" + server.getDomainPort()
						+ "” 未找到WebSphere应用服务进程");
				responseHandler.sendMsgCompleted(result, socketChannel);
				return;
			}
		} catch (Exception ex) {
			result.setCode(-1);
			result.setMsg("停止WebSphere服务出错，原因：" + ex.getMessage());
			responseHandler.sendMsgCompleted(result, socketChannel);
		}
	}

	/**
	 * 启动WebLogic
	 * 
	 * @param server
	 * @param socketChannel
	 */
	public void startWebLogic(ServerModel server, SocketChannel socketChannel) {
		// TODO 启动应用
		ResultModel result = new ResultModel();
		long start, end;
		Process p = null;
		ProcessBuilder pb = null;
		BufferedReader in = null;
		//检查配置
		if (!WebLogicTool.isDomainPath(server.getDomainPath())) {
			result.setCodeMsg(Status.FAILED,"服务器中“" + server.getDomainPath()
					+ "”不是有效的Domain路径，请检查配置");
			responseHandler.sendMsgCompleted(result, socketChannel);
			return;
		}
		//非集群机器需要检查应用是否已在运行中，集群机器不用检查
		String pid = Tool.findPidByPortLinux(server.getDomainPort());
		if (pid != null && 
				server.getServerType().equals("0")&&server.getServerType().equals("1")) {
			result.setCodeMsg(Status.FAILED,"应用端口" + server.getServerIp() + ":"
					+ server.getDomainPort() + " 程序已在运行状态中.");
			result.getMsg();
			responseHandler.sendMsgCompleted(result, socketChannel);
			return;
		}

		try {

			// 只有应用类型为“普通应用=0” 或“ 总控制端应用=1”时才启动主应用。“集群机器=2”不启动主应用，只启动集群服务。
			if (server.getServerType().equals("0")|| server.getServerType().equals("1")) {
				// 检查启动文件
				String startFile = null;
				File file = new File(server.getDomainPath()
						+ "/startWebLogic.sh");
				if (file.exists()) {
					startFile = file.getPath();
				} else {
					file = new File(server.getDomainPath()
							+ "/bin/startWebLogic.sh");
					if (file.exists()) {
						startFile = file.getPath();
					}
				}
				if (startFile == null) {
					result.setCodeMsg(Status.FAILED,"启动失败，未找到启动文件“startWebLogic.sh”");
					responseHandler.sendMsgCompleted(result, socketChannel);
					return;
				}
				//拼装启动命令
				String[] commandList = {
						"/bin/sh",
						"-c",
						"nohup " + startFile + " >" + server.getDomainPath()
								+ "/nohup.out 2>&1 &" };
				pb = new ProcessBuilder(commandList);
				pb.redirectErrorStream(true);
				p = pb.start();
				// 如果把输出流输出到nohup.out文件中，再从文件中获取启动信息
				start = System.currentTimeMillis();
				if (p.waitFor() == 0) {
					//拼装tail命令
					String[] tailCommandList = { "/bin/sh", "-c",
							"tail -f " + server.getDomainPath() + "/nohup.out" };

					pb = new ProcessBuilder(tailCommandList);
					pb.redirectErrorStream(true);
					p = pb.start();

				}
				//获取启动日志输入流
				in = new BufferedReader(new InputStreamReader(
						p.getInputStream(), AgentConfig.getEncoding()));
				String console = "";
				int timer = AgentConfig.getTimeOut();
				while ((console = in.readLine()) != null) {
					result.setMsg(console);
					if (!responseHandler.sendMsgRun(result, socketChannel)) {
						// 如果信息发送失败，则停止
						return;
					}
					if (console.toLowerCase().contains("server started in running")) {
						result.setCodeMsg(Status.SUCCEED,"WebLogic应用已启动成功");
						responseHandler.sendMsgRun(result, socketChannel);
						break;//启动成功后，判断是否有配置集群
					} else if (console.toLowerCase().contains("unable to get file lock")
							|| console.toLowerCase().contains("could not get the server file lock.")) {
						result.setCodeMsg(Status.FAILED,"WebLogic应用文件被锁定，请检查后台进程是否正在运行中");
						responseHandler.sendMsgCompleted(result, socketChannel);
						return;
					} else if (console.toLowerCase().contains(
							"force_shutting_down")) {
						result.setCodeMsg(Status.FAILED,"WebLogic加载时发生异常，应用启动失败");
						responseHandler.sendMsgRun(result, socketChannel);
						Tool.killProcess(p);
						return;
					} else if (console.toLowerCase().contains(
							"could not create pool connection")) {
						result.setCodeMsg(Status.FAILED,"WebLogic创建数据库连接池失败，请检查网络和配置！");
						responseHandler.sendMsgRun(result, socketChannel);
						return;
					} else {
						end = System.currentTimeMillis();
						result.setCodeMsg(Status.SUCCEED,"WebLogic正在加载部署项目("
								+ (int) ((end - start) / 1000) + "/"
								+ (AgentConfig.getTimeOut() / 1000) + "秒)");
						if (!responseHandler.sendMsgRun(result, socketChannel)) {
							// 如果信息发送失败，则停止
							return;
						}
						Thread.sleep(100);
					}
					if (timer - (end - start) <= 0) {
						result.setCodeMsg(Status.FAILED,"WebLogic加载部署时间超过系统指定时间，已断开连接，请检查应用是否正常");
						responseHandler.sendMsgCompleted(result, socketChannel);
						return;
					}
				}// 读取启动日志输入流while-结束
			}// 判断是否为普通应用和集群总控制，只有这两种情况才启动主应用
			
			//启动集群服务
			WebLogicTool.startWebLogicClusterServices(responseHandler,server, socketChannel);		
			//最后通知客户端已经完成所有操作
			result.setCodeMsg(Status.SUCCEED, "启动完毕");
			responseHandler.sendMsgCompleted(result, socketChannel);

		} catch (Exception e) {
			result.setCodeMsg(Status.FAILED,"Agent发生异常，原因: " + e.getMessage());
			log.info(result.getMsg());
			responseHandler.sendMsgCompleted(result, socketChannel);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
			if (p != null) {
				Tool.killProcess(p);
				p.destroy();
			}
		}
	}

	

	/**
	 * 停止WebLogic
	 * 
	 * @param server
	 * @param socketChannel
	 */
	public void stopWebLogic(ServerModel server, SocketChannel socketChannel) {
		// TODO 停止应用
		ResultModel result = new ResultModel();
		try {

			// 先停止集群服务
			List<ClusterServicesModel> cmsList = server.getClusterServers();
			if (cmsList != null) {
				for (ClusterServicesModel csm : cmsList) {
					WebLogicTool.stopWebLogicClusterServices(responseHandler,server.getDomainPath(), csm,
							socketChannel);
					Thread.sleep(500);
				}
			}
			// 普通应用 或者 总控制端，才停止主应用。集群机器一开始不启动，所以不用停止。
			if (server.getServerType().equals("0")
					|| server.getServerType().equals("1")) {
				// 检查端口
				if (!WebLogicTool.getListenPort(server.getDomainPath()).equals(
						server.getDomainPort())
						&& !WebLogicTool.getSSLListenPort(server.getDomainPath())
								.equals(server.getDomainPort())) {
					result.setCodeMsg(Status.FAILED,"端口号  <" + server.getDomainPort() + "” 不是“"
							+ server.getDomainPath() + "> 应用的端口，停止失败.");
					responseHandler.sendMsgCompleted(result, socketChannel);
					return;
				}

				// 先根据端口号获取进程ID，（并非所有进程都能被检测到，所有非本用户的进程信息将不会显示，如果想看到所有信息，则必须切换到
				// root
				// 用户）
				String pid = Tool.findPidByPortLinux(server.getDomainPort());
				if (pid == null) {
					result.setCodeMsg(Status.SUCCEED,"应用端口 <" + server.getServerIp() + ":"
							+ server.getDomainPort() + "> 已停止（或未启动）.");
					responseHandler.sendMsgRun(result, socketChannel);
				} else {
					String s = Tool.KillProcessByPidLinux(pid);
					if (s == null || s.equals("")) {
						result.setCodeMsg(Status.FAILED,"应用程序 <" + server.getServerIp() + ":"
								+ server.getDomainPort() + "> 已停止.");
						responseHandler.sendMsgRun(result, socketChannel);
					} else {
						result.setCodeMsg(Status.FAILED,"停止应用失败，原因:" + s);
						responseHandler.sendMsgRun(result, socketChannel);
					}
				}
				result.setCodeMsg(Status.SUCCEED, "停止完毕");
				responseHandler.sendMsgCompleted(result, socketChannel);
			}
		} catch (Exception e) {
			result.setCode(-1);
			result.setMsg("停止WebLogic应用发生异常，原因: " + e.getMessage());
			log.info(result.getMsg());
			responseHandler.sendMsgCompleted(result, socketChannel);
		}
	
	}
}
