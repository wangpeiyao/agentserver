package cn.com.hnisi.agent.middleware.cluster;

import cn.com.hnisi.model.ClusterServicesModel;
import cn.com.hnisi.model.ClusterServicesStatus;

/**
 * 集群服务接口
 * @author WenZhiFeng
 * 2018年2月7日
 */
public interface ICluster {
	/**
	 * 获取集群服务最终的状态
	 * @return
	 */
	public ClusterServicesStatus getStatus();
	
	/**
	 * 获取集群服务实例对象
	 * @return
	 */
	public ClusterServicesModel getClusterServicesModel();
}
