package cn.com.hnisi.agent.interfaces;

import java.nio.channels.SocketChannel;

import cn.com.hnisi.model.ServerModel;

public interface IPlatform {
	/**
	 * 压缩备案
	 * @param server
	 * @param socketChannel
	 */
	public void backUp(ServerModel server, SocketChannel socketChannel);
	/**
	 * 复制备案
	 * @param server
	 * @param socketChannel
	 */
	public void backUpByCopy(ServerModel server, SocketChannel socketChannel);
	/**
	 * 启动WebSphere
	 * @param server
	 * @param socketChannel
	 */
	public void startWebSphere(ServerModel server, SocketChannel socketChannel);
	/**
	 * 停止WebSphere
	 * @param server
	 * @param socketChannel
	 */
	public void stopWebSphere(ServerModel server, SocketChannel socketChannel);
	/**
	 * 启动WebLogic
	 * @param server
	 * @param socketChannel
	 */
	public void startWebLogic(ServerModel server, SocketChannel socketChannel);
	/**
	 * 停止WebLogic
	 * @param server
	 * @param socketChannel
	 */
	public void stopWebLogic(ServerModel server, SocketChannel socketChannel);
}
