package cn.com.hnisi.model;

/**
 * 集群服务启动状态类
 * @author WenZhiFeng
 * 2018年1月30日
 */
public class ClusterServicesStatus {
	
	public ClusterServicesStatus(){		
	}
	//服务名
	private String name;
	//状态值
	private int code;
	//返回信息
	private String msg;
	//是否已结束
	private boolean isFinish=false;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * -1表示失败，0表示正常
	 * @return
	 */
	public int getCode() {
		return code;
	}
	public void setCode(int code) {
		this.code = code;
	}
	
	/**
	 * 返回处理信息
	 * @return
	 */
	public String getMsg() {
		return msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
	
	/**
	 * 如果为true表示，启动已结束
	 * @param isFinish
	 */
	public boolean isFinish() {
		return isFinish;
	}
	
	/**
	 * 如果应用启动已完成，则设为true.
	 * @param isFinish
	 */
	public void setFinish(boolean isFinish) {
		this.isFinish = isFinish;
	}
}
