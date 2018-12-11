package cn.com.hnisi.agent.middleware.cluster;

import cn.com.hnisi.model.ClusterServicesModel;
import cn.com.hnisi.model.ClusterServicesStatus;
import cn.com.hnisi.model.ServerModel;

/**
 * 集群服务抽象类
 * @author WenZhiFeng
 * 2018年2月7日
 */
public abstract class WebLogicCluster extends Thread implements ICluster{
	/**
	 * 集群服务对象
	 */
	public ClusterServicesModel csm=null;
	/**
	 * 应用对象
	 */
	public ServerModel server=null;
	/**
	 * 集群服务状态对象
	 */
	public ClusterServicesStatus csmStatus = new ClusterServicesStatus();

	public WebLogicCluster(ServerModel server,ClusterServicesModel csm){
		this.server = server;
		this.csm = csm;
	}

	public ClusterServicesStatus getStatus() {
		return this.csmStatus;
	}
	public ClusterServicesModel getClusterServicesModel() {
		return this.csm;
	}
	
}
