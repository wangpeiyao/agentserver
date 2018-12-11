package cn.com.hnisi.agent.middleware;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import cn.com.hnisi.model.ServerModel;
import cn.com.hnisi.util.XmlUtil;

public class WebSphereTool {

	private static void checkPath(ServerModel server) throws Exception {
		File appSrvPath = new File(server.getDomainPath());
		if (!appSrvPath.exists()) {
			throw new Exception("服务器中 “" + appSrvPath.getPath()
					+ "”不是有效的概要文件(Profile)路径，请检查配置");
		}

	}

	/**
	 * 判断webshpere是否已启动
	 * 
	 * @param server
	 * @return
	 * @throws Exception
	 */
	public static boolean isRunning(ServerModel server) throws Exception {
		checkPath(server);
		File serverStatusFile = new File(server.getDomainPath().replace("\\",
				"/")
				+ "/bin/serverStatus.bat");
		if (!serverStatusFile.exists()) {
			throw new Exception("未找到启动文件 “" + serverStatusFile.getPath()
					+ "” ，请检查websphere配置");
		}
		String[] commandList = new String[3];
		;
		if (server.getUsername() != null
				&& server.getUsername().trim().length() > 0) {
			commandList[0] = "cmd.exe";
			commandList[1] = "/c";
			commandList[2] = "\"" + serverStatusFile.getPath()
					+ "\" server1 -username " + server.getUsername()
					+ " -password " + server.getPassword();
		} else {
			commandList[0] = "cmd.exe";
			commandList[1] = "/c";
			commandList[2] = "\"" + serverStatusFile.getPath() + "\" server1";
		}

		Process p = null;
		ProcessBuilder pb = null;
		BufferedReader in;

		pb = new ProcessBuilder(commandList);
		pb.redirectErrorStream(true);
		p = pb.start();
		p.getOutputStream().close();
		in = new BufferedReader(
				new InputStreamReader(p.getInputStream(), "GBK"));
		String console = "";
		while ((console = in.readLine()) != null) {
			if (console.toLowerCase().contains("is started")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 校验端口是否为websphere端口
	 * 
	 * @param server
	 * @throws Exception
	 */
	public static boolean isServerPort(ServerModel server) throws Exception {
		checkPath(server);
		// 例：G:\Program Files
		// (x86)\IBM\WebSphere\AppServer\profiles\AppSrv01\config\cells
		File cellsPath = new File(server.getDomainPath().replace("\\", "/")
				+ "/config/cells");
		if (!cellsPath.exists()) {
			throw new Exception("服务器不存在目录  “" + cellsPath.getPath()
					+ "” ，请检查配置");
		}

		for (String nodes : cellsPath.list()) {
			// 例：G:\Program Files
			// (x86)\IBM\WebSphere\AppServer\profiles\AppSrv01\config\cells\多个cell
			File cellPath = new File(cellsPath.getPath() + "/" + nodes
					+ "/nodes");
			if (!cellPath.exists()) {
				throw new Exception("服务器不存在目录  “" + cellPath.getPath()
						+ "” ，请检查配置");
			}

			for (String node : cellPath.list()) {
				// 例：G:\Program Files
				// (x86)\IBM\WebSphere\AppServer\profiles\AppSrv01\config\cells\FengGeGe-PCNode01Cell\nodes\多个node
				File nodePath = new File(cellPath.getPath() + "/" + node);
				if (!nodePath.exists()) {
					throw new Exception("服务器不存在目录  “" + nodePath.getPath()
							+ "” ，请检查配置");
				}
				// G:\Program Files
				// (x86)\IBM\WebSphere\AppServer\profiles\AppSrv01\config\cells\FengGeGe-PCNode01Cell\nodes\FengGeGe-PCNode01\serverindex.xml
				File serverIndexXml = new File(nodePath.getPath()
						+ "/serverindex.xml");
				if (!serverIndexXml.exists()) {
					throw new Exception("服务器不存在文件  “"
							+ serverIndexXml.getPath() + "” ，请检查配置");
				}
				String port = XmlUtil
						.getNodeText(
								serverIndexXml,
								"ServerIndex/serverEntries/specialEndpoints[@endPointName=\"WC_defaulthost\"]/endPoint/@port");
				if (port.equals(server.getDomainPort())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 获取websphere应用的进程ID
	 * @param server
	 * @return
	 * @throws Exception
	 */
	public static String getServerPid(ServerModel server) throws Exception {
		checkPath(server);
		File logPath = new File(server.getDomainPath() + "/logs/"
				+ server.getServerName());
		if (!logPath.exists()) {
			throw new Exception("服务器不存在目录  “" + logPath.getPath() + "” ，请检查配置");
		}
		File serverPid = new File(logPath + "/" + server.getServerName()
				+ ".pid");
		if (!serverPid.exists()) {
			throw new Exception("服务器不存在文件  “" + serverPid.getPath()
					+ "” ，请检查配置");
		}
		BufferedReader read = null;
		try {
			read = new BufferedReader(new InputStreamReader(
					new FileInputStream(serverPid)));
			if (read != null) {
				return read.readLine();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			if(read!=null){
				read.close();
			}
		}
		return null;
	}
	
}
