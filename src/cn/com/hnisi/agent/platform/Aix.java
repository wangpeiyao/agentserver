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
import cn.com.hnisi.util.AixCheck;
import cn.com.hnisi.util.Tool;

public class Aix implements IPlatform {
	static Logger log = Logger.getLogger(Aix.class);
	AgentHandler responseHandler = null;

	public Aix(AgentHandler responseHandler) {
		this.responseHandler = responseHandler;
	}
	//记录处理时间
	long startTime, endTime;

	/**
	 * tar 备份应用
	 * 
	 * @param commandList
	 * @param server
	 * @param socketChannel
	 * @return
	 */
	public void backUp(ServerModel server, SocketChannel socketChannel) {
		// TODO 备份应用
		ResultModel result = new ResultModel();
		Process p = null;
		ProcessBuilder pb = null;

		try {
			File appPath = new File(server.getAppPath());
			if (!appPath.exists()) {
				result.setMsg("备份目标“" + appPath.getPath() + "” 不存在，备份失败");
				responseHandler.sendMsgCompleted(result, socketChannel);
				return;
			}

			result.setCode(0);
			result.setMsg("正在对备份目标“" + appPath.getPath() + "” 进行备份，请稍候...");
			responseHandler.sendMsgRun(result, socketChannel);

			startTime = System.currentTimeMillis();
			String backFile = "";
			// 备份后的文件路径和文件名
			if (server.getBackUpPath() != null
					&& server.getBackUpPath().trim().length() > 0) {
				File backPath = new File(server.getBackUpPath());
				// 如果目录不存在则创建
				if (!backPath.exists()) {
					backPath.mkdirs();
				}
				backFile = backPath.getPath() + "/" + appPath.getName()
						+ Tool.getNowDateSSS() + server.getDomainPort()
						+ ".bak";
			} else {
				backFile = server.getAppPath() + Tool.getNowDateSSS()
						+ server.getDomainPort() + ".bak";
			}
			String[] commandList = { "/bin/sh", "-c",
					"tar -cf " + backFile + " " + server.getAppPath() };
			pb = new ProcessBuilder(commandList);
			pb.redirectErrorStream(true);
			p = pb.start();
			p.getOutputStream().close();
			// 获取程序“退出”结果，0为已正常执行。
			int exitCode = p.waitFor();
			if (exitCode == 0) {
				endTime = System.currentTimeMillis();
				result.setCode(0);
				result.setMsg("备份成功，备份文件存放在服务器: “" + backFile + "”, 耗时: "
						+ Tool.millisecondFormat(endTime - startTime));
			} else {
				result.setCode(-1);
				result.setMsg("备份失败，操作已取消或发生异常退出");
			}
		} catch (Exception ex) {
			result.setCode(-1);
			result.setMsg("备份发生异常，原因: " + ex.getMessage());
			log.info(result.getMsg());
		}
		responseHandler.sendMsgCompleted(result, socketChannel);
	}

	/**
	 * 备份应用
	 * 
	 * @param commandList
	 * @param server
	 * @param socketChannel
	 * @return
	 */
	public void backUpByCopy(ServerModel server, SocketChannel socketChannel) {
		// TODO 备份应用
		ResultModel result = new ResultModel();
		Process p = null;
		ProcessBuilder pb = null;
		long startTime, endTime;
		try {
			File appPath = new File(server.getAppPath());
			if (!appPath.exists()) {
				result.setCode(-1);
				result.setMsg("备份失败，备份目标“" + appPath.getPath() + "” 不存在");
				responseHandler.sendMsgCompleted(result, socketChannel);
				return;
			}
			result.setCode(0);
			result.setMsg("正在对备份目标“" + appPath.getPath() + "” 进行备份，请稍候...");
			responseHandler.sendMsgRun(result, socketChannel);

			startTime = System.currentTimeMillis();
			String backFile = "";

			// 备份后的文件路径和文件名
			if (server.getBackUpPath() != null
					&& server.getBackUpPath().trim().length() > 0) {
				File backPath = new File(server.getBackUpPath());
				// 如果目录不存在则创建
				if (!backPath.exists()) {
					backPath.mkdirs();
				}
				backFile = backPath.getPath() + "/" + appPath.getName()
						+ Tool.getNowDateSSS() + server.getDomainPort();
			} else {
				backFile = server.getAppPath() + Tool.getNowDateSSS()
						+ server.getDomainPort();
			}
			/*
			 * cp 命令参数： -r 递归处理，将指定目录下的所有文件与子目录一并处理； -f
			 * 强行复制文件或目录，不论目标文件或目录是否已存在；
			 */
			String[] commandList = { "/bin/sh", "-c",
					"cp -rf \"" + server.getAppPath() + "\" \"" + backFile +"\""};
			result.setCode(0);
			result.setMsg("正在使用“复制文件夹”方式备份，时间可能较长，请耐心等待...");
			responseHandler.sendMsgRun(result, socketChannel);
			pb = new ProcessBuilder(commandList);
			pb.redirectErrorStream(true);
			p = pb.start();
			p.getOutputStream().close();
			// 获取程序“退出”结果，0为已正常执行。
			int exitCode = p.waitFor();
			if (exitCode == 0) {
				endTime = System.currentTimeMillis();
				result.setCode(0);
				result.setMsg("备份成功，备份文件存放在服务器: “" + backFile + "”, 耗时: "
						+ Tool.millisecondFormat(endTime - startTime));
			} else {
				result.setCode(-1);
				result.setMsg("备份失败，操作已取消或发生异常退出");
			}
		} catch (Exception ex) {
			result.setCode(-1);
			result.setMsg("备份发生异常，原因: " + ex.getMessage());
			log.info(result.getMsg());
		}
		responseHandler.sendMsgCompleted(result, socketChannel);
	}

	/**
	 * 启动weblogic
	 * 
	 * @param server
	 * @param socketChannel
	 * @throws IOException
	 */
	public void startWebLogic(ServerModel server, SocketChannel socketChannel) {
		// TODO 启动应用
		ResultModel result = new ResultModel();
		if (!WebLogicTool.isDomainPath(server.getDomainPath())) {
			result.setCodeMsg(Status.FAILED,"服务器中“" + server.getDomainPath()
					+ "”不是有效的Domain路径，请检查配置");
			responseHandler.sendMsgCompleted(result, socketChannel);
			return;
		}

		// 检查启动文件
		String startFile = null;
		File file = new File(server.getDomainPath() + "/startWebLogic.sh");
		if (file.exists()) {
			startFile = file.getPath();
		} else {
			file = new File(server.getDomainPath() + "/bin/startWebLogic.sh");
			if (file.exists()) {
				startFile = file.getPath();
			}
		}
		if (startFile == null) {
			result.setCodeMsg(Status.FAILED,"启动失败，未找到启动文件“startWebLogic.sh”");
			responseHandler.sendMsgCompleted(result, socketChannel);
			return;
		}
		// 判断当前用户能否使用RMSOCK命令
		if (AixCheck.canUseRmsock()) {
			// 管理员身份使用RMSOCK方式精确判断端口，程序是否已启动  (精确查询)
			String pid = Tool.findPidByPortAix(server.getDomainPort());
			if (pid != null) {
				result.setCodeMsg(Status.FAILED,"端口号" + server.getDomainPort() + " 程序已在运行状态中.");
				responseHandler.sendMsgCompleted(result, socketChannel);
				return;
			}
		} else {
			// 不具管理员权限，只能使用查找启动命令行方式，判断判断程序是否已在运行中。(模糊查询)
			List<String> pids = Tool
					.findPidByCommandAix(server.getDomainPath());
			if (pids != null && pids.size() > 0) {
				result.setCodeMsg(Status.FAILED,"应用程序  " + server.getDomainPath() + " 已在运行中.");
				responseHandler.sendMsgCompleted(result, socketChannel);
				return;
			}
		}
		Process p = null;
		ProcessBuilder pb = null;
		BufferedReader in = null;
		try {
			// 只有应用类型为“普通应用=0” 或“ 总控制端应用=1”时才启动主应用。“集群机器=2”不启动主应用，只启动集群服务。
			if (server.getServerType().equals("0")||server.getServerType().equals("1")) {
				String[] commandList = {
						"/bin/sh",
						"-c",
						"nohup " + startFile + " > " + server.getDomainPath()
								+ "/nohup.out 2>&1 &" };
				pb = new ProcessBuilder(commandList);
				pb.redirectErrorStream(true);
				p = pb.start();
				startTime = System.currentTimeMillis();
				// 等待上一命令
				if (p.waitFor() == 0) {
					String[] tailCommand = { "/bin/sh", "-c",
							"tail -f " + server.getDomainPath() + "/nohup.out" };
					pb = new ProcessBuilder(tailCommand);
					pb.redirectErrorStream(true);
					p = pb.start();
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
						if (console.toLowerCase().contains(
								"server started in running")) {
							result.setCodeMsg(Status.SUCCEED,"WebLogic应用已启动成功");
							responseHandler.sendMsgRun(result,
									socketChannel);
							break;
						} else if (console.toLowerCase().contains(
								"unable to get file lock")) {
							result.setCodeMsg(Status.FAILED,"WebLogic应用已在运行状态中");
							responseHandler.sendMsgCompleted(result,
									socketChannel);
							return;
						} else if (console.toLowerCase().contains(
								"force_shutting_down")) {
							result.setCodeMsg(Status.FAILED,"WebLogic加载时发生异常，应用启动失败");
							responseHandler.sendMsgRun(result, socketChannel);
							stopWebLogic(server, socketChannel);
							return;
						} else if (console.toLowerCase().contains(
								"could not create pool connection")) {
							result.setCodeMsg(Status.FAILED,"WebLogic创建数据库连接池失败，请检查网络和配置！");
							responseHandler.sendMsgRun(result, socketChannel);
							return;
						} else {
							endTime = System.currentTimeMillis();
							result.setCodeMsg(Status.SUCCEED,"WebLogic正在加载部署项目("
									+ (int) ((endTime - startTime) / 1000)
									+ "/" + (AgentConfig.getTimeOut() / 1000)
									+ "秒)");
							if (!responseHandler.sendMsgRun(result,
									socketChannel)) {
								// 如果信息发送失败，则停止
								return;
							}
							Thread.sleep(100);
						}
						if (timer - (endTime - startTime) <= 0) {
							result.setCodeMsg(Status.FAILED,"WebLogic加载应用时间超系统指定时间，请检查应用是否正常");
							responseHandler.sendMsgCompleted(result,
									socketChannel);
							break;
						}
					}// End-While
				}// 判断是否为普通应用和集群总控制，只有这两种情况才启动主应用
				//启动集群服务
				WebLogicTool.startWebLogicClusterServices(responseHandler,server, socketChannel);
				
				result.setCodeMsg(Status.SUCCEED, "启动完毕");
				responseHandler.sendMsgCompleted(result, socketChannel);
			} else {
				result.setCode(-1);
				result.setMsg("启动WebLogic应用失败");
				responseHandler.sendMsgCompleted(result, socketChannel);
			}
		} catch (Exception e) {
			result.setCode(-1);
			result.setMsg("Agent发生异常，原因: " + e.getMessage());
			log.info(result.getMsg());
			responseHandler.sendMsgCompleted(result, socketChannel);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (p != null) {
				Tool.killProcess(p);
				p.destroy();
			}
		}
	}


	/**
	 * 停止weblogic
	 * 
	 * @param server
	 * @param socketChannel
	 */
	public void stopWebLogic(ServerModel server, SocketChannel socketChannel) {
		// TODO 停止应用
		ResultModel result = new ResultModel();
		try {
			//先停止集群服务
			List<ClusterServicesModel> cmsList=server.getClusterServers();
			if(cmsList!=null){
				for(ClusterServicesModel csm:cmsList){
					WebLogicTool.stopWebLogicClusterServices(responseHandler,server.getDomainPath(),csm,socketChannel);
					Thread.sleep(1000);
				}
			}
			//普通应用 或者 总控制端，才停止主应用。集群机器一开始不启动，所以不用停止。
			if(server.getServerType().equals("0") || server.getServerType().equals("1")){				
				// 检查端口
				if (!WebLogicTool.getListenPort(server.getDomainPath()).equals(
						server.getDomainPort())
						&& !WebLogicTool.getSSLListenPort(server.getDomainPath())
								.equals(server.getDomainPort())) {
					result.setCode(-1);
					result.setMsg("停止失败，端口号 <" + server.getDomainPort() + "” 并不是“"
							+ server.getDomainPath() + "> 应用的端口.");
					responseHandler.sendMsgCompleted(result, socketChannel);
					return;
				}
	
				if (!AixCheck.checkPortStatus(server.getDomainPort())) {
					result.setCode(0);
					result.setMsg("应用端口  <" + server.getServerIp() + ":"
							+ server.getDomainPort() + "> 已停止（或未启动）.");
					responseHandler.sendMsgCompleted(result, socketChannel);
					return;
				}
				// 判断能否使用rmsock命令
				if (AixCheck.canUseRmsock()) {
					log.info("Using rmsock command");
					// 先根据端口号获取进程ID
					String pid = Tool.findPidByPortAix(server.getDomainPort());
					if (pid != null) {
						Tool.KillProcessByPidAix(pid);
						result.setCode(-1);
						result.setMsg("应用程序 <" + server.getServerIp() + ":"
								+ server.getDomainPort() + "> 已停止.");
					} else {
						result.setCode(0);
						result.setMsg("应用端口 <" + server.getServerIp() + ":"
								+ server.getDomainPort() + "> 已停止（或未启动）.");
					}
					responseHandler.sendMsgCompleted(result, socketChannel);
					return;
				} else {
					log.info("Using gred command");
					// 根据启动文件，获取启动程序PID （后续再根据此PID作为PPID去寻找属于的子进程）
					// 例：/weblogic/10.3.5/user_projects/domains/jmsb_6666
					List<String> ppids = Tool.findPidByCommandAix(server
							.getDomainPath());
	
					if (ppids == null || ppids.size() == 0) {
						result.setCode(0);
						result.setMsg("应用端口  <" + server.getServerIp() + ":"
								+ server.getDomainPort() + "> 已停止（或未启动）.");
						responseHandler.sendMsgCompleted(result, socketChannel);
						return;
					} else {
	
						for (String ppid : ppids) {
							// 根据PPID获取归属于它的所有子进程
							List<String> pids = Tool.findPidByPPIDAix(ppid);
							for (String pid : pids) {
								if (pid != null) {
									// 终止子进程
									Tool.KillProcessByPidAix(pid);
								}
							}
						}
						result.setCode(0);
						result.setMsg("应用程序 <" + server.getServerIp() + ":"
								+ server.getDomainPort() + "> 已停止.");
						responseHandler.sendMsgRun(result, socketChannel);
						return;
					}
				}
			}
		} catch (Exception e) {
			result.setCode(-1);
			result.setMsg("停止应用时发生异常，原因:" + e.getMessage());
			log.info(result.getMsg());
			responseHandler.sendMsgCompleted(result, socketChannel);
		}
	}

	/**
	 * 启动WebSphere
	 * 
	 * @param server
	 * @param socketChannel
	 * @throws IOException
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

			if (Tool.processIsRunning(PlatformType.AIX,
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
	 * 停止websphere
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
			if (Tool.processIsRunning(PlatformType.AIX, pid)) {
				String msg = Tool.KillProcessByPidAix(pid);
				result.setCode(0);
				result.setMsg("WebSphere应用服务已停止成功. "
						+ (msg != null ? "处理信息:" + msg : ""));
				responseHandler.sendMsgCompleted(result, socketChannel);
				return;
			} else {
				// 再用端口号和启动命令行复查进程是否存在
				if (AixCheck.canUseRmsock()) {
					log.info("Using rmsock command");
					pid = Tool.findPidByPortAix(server.getDomainPort());

					if (pid != null) {
						Tool.KillProcessByPidAix(pid);
						result.setCode(-1);
						result.setMsg("WebSphere应用程序" + server.getServerIp()
								+ ":" + server.getDomainPort() + "已停止.");
					} else {
						result.setCode(0);
						result.setMsg("WebSphere应用端口  “" + server.getServerIp()
								+ ":" + server.getDomainPort() + "” 已停止（或未启动）.");
					}
					responseHandler.sendMsgCompleted(result, socketChannel);
					return;
				} else {
					// 根据服务名去查
					List<String> ppids = Tool.findPidByCommandAix(server
							.getServerName());
					if (ppids == null || ppids.size() == 0) {
						result.setCode(0);
						result.setMsg("WebSphere应用端口  “" + server.getServerIp()
								+ ":" + server.getDomainPort() + "” 已停止（或未启动）");
						responseHandler.sendMsgCompleted(result, socketChannel);
					} else {
						boolean isServerPid = false;
						for (String ppid : ppids) {
							if (ppid.equals(WebSphereTool.getServerPid(server))) {
								log.info("Grep pid equals serverPid");
								// 根据PPID获取归属于它的所有子进程
								List<String> pids = Tool.findPidByPPIDAix(ppid);
								for (String p : pids) {
									if (p != null) {
										// 终止子进程
										Tool.KillProcessByPidAix(p);
									}
								}
								isServerPid = true;
								break;
							}
						}
						if (isServerPid) {
							result.setCode(0);
							result.setMsg("WebSphere应用程序"
									+ server.getServerIp() + ":"
									+ server.getDomainPort() + "已停止.");
							responseHandler.sendMsgCompleted(result,
									socketChannel);
						} else {
							result.setCode(-1);
							result.setMsg("搜索“" + server.getServerName()
									+ "”与配置文件“" + server.getServerName()
									+ ".pid”文件PID不一致");
							responseHandler.sendMsgCompleted(result,
									socketChannel);
						}
					}
				}
			}
		} catch (Exception ex) {
			result.setCode(-1);
			result.setMsg("停止WebSphere服务出错，原因：" + ex.getMessage());
			responseHandler.sendMsgCompleted(result, socketChannel);
		}
	}
}
