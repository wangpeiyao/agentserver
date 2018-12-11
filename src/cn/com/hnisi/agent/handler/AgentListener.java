package cn.com.hnisi.agent.handler;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import cn.com.hnisi.config.AgentConfig;

/**
 * Agent启动类
 * 
 * @author FengGeGe
 * 
 */
public class AgentListener {
	static Logger log = Logger.getLogger(AgentListener.class);
	static String PRINT_DECOLLATOR = "*";// 分割符
	static int PRINT_LENGTH = 70;// 打印系统信息长度
	AgentHandler handler = null;
	ServerSocketChannel serverSocketChannel = null;
	Selector selector = null;

	

	/**
	 * Agent服务器
	 * 
	 * @param port
	 *            监听端口
	 */
	public AgentListener() {
		try {
			printCopyright();
			selector = Selector.open();
			// 打开通道
			serverSocketChannel = ServerSocketChannel.open();
			String ip = AgentConfig.getIp();
			if (ip == null) {
				log.error("启动Agent服务失败，原因: 未找到配置参数[服务器地址]");
				return;
			}
			String port = AgentConfig.getPort();

			if (port == null) {
				log.error("启动Agent服务失败，原因: 未找到配置参数[Agent监听端口]");
				return;
			}
			SocketAddress endpoint = new InetSocketAddress(ip,
					Integer.parseInt(port));
			serverSocketChannel.configureBlocking(false);
			// 绑定到本地端口
			serverSocketChannel.socket().bind(endpoint);
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
			log.info("正在监听 " + ip+":"+port );
			// 创建固定线程池。
			ExecutorService threadPool = Executors
					.newFixedThreadPool(AgentConfig.getThreadPool());
			while (true) {
				if (selector.select() > 0) {
					Iterator<SelectionKey> it = selector.selectedKeys()
							.iterator();
					while (it.hasNext()) {
						SelectionKey key = null;
						try {
							key = it.next();
							it.remove();
							if (!key.isValid()) {
								break;
							}
							if (key.isAcceptable()) {
								ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key
										.channel();

								handler = new AgentHandler(
										serverSocketChannel);
								// 把线程任务放入线程池
								threadPool.execute(handler);
							}
						} catch (Exception e) {
							log.error("监听中发生异常: " + e.getMessage());
						}
					}
				}/*-判断selector.select()>0-*/
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * 打印程序信息
	 */
	private static void printCopyright() {
		printLines();
		printLines();
		printInfo("程序发布管理工具-Agent代理服务器", 2, 2);
		printLines();
		printInfo("广州华资软件有限公司", 1, 0);
		printInfo("www.sinobest.cn", 1, 0);
		printInfo("Version 2017", 1, 1);
		printInfo("Copyright (C) 2017 All Rights Reserved.", 0, 1);
		printLines();
	}

	/**
	 * 在内容前面和后面换行
	 * 
	 * @param content
	 *            内容
	 * @param frontLine
	 *            前面换行数
	 * @param backLine
	 *            后面换行数
	 */
	private static void printInfo(String content, int frontLine, int backLine) {
		for (int i = 0; i < frontLine; i++) {
			printInfo("  ");
		}
		printInfo(content);
		for (int i = 0; i < backLine; i++) {
			printInfo("  ");
		}
	}

	/**
	 * 打印系统相关信息
	 * 
	 * @param content
	 *            要显示的内容
	 * @return
	 */
	private static void printInfo(String content) {
		String fillChar = "";
		char[] ca = content.toCharArray();
		int len = 0;// 计算内容的实际长度
		for (char c : ca) {
			if (c >= 19968 && c <= 171941) {// 汉字范围 \u4e00-\u9fa5 (中文)
				len += 2;// 中文计算为2个字符长度
			} else {
				len += 1;
			}
		}
		if (len >= PRINT_LENGTH) {
			System.out.println(content);
			return;
		}
		// 避免出现奇数，导致不能对齐
		if (len % 2 != 0 && PRINT_LENGTH % 2 == 0) {
			// 内容长度为奇数时，在内容后面加一个长度为1的空格
			content += " ";
		}
		for (int i = 0; i < (PRINT_LENGTH - len - 2) / 2; i++) {
			fillChar += " ";// 除去要显示的内容长度后，计算需要填充两边的长度，并且用空格字符填充
		}
		System.out.println(PRINT_DECOLLATOR + fillChar + content + fillChar
				+ PRINT_DECOLLATOR);
	}

	/**
	 * 用分割符PRINT_DECOLLATOR，打印一行长度为PRINT_LENGTH的线条
	 * 
	 * @return
	 */
	private static void printLines() {
		String str = "";
		for (int i = 0; i < PRINT_LENGTH; i++) {
			str += PRINT_DECOLLATOR;
		}
		System.out.println(str);
	}
}
