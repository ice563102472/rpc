package com.linda.framework.rpc.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.linda.framework.rpc.RemoteCall;
import com.linda.framework.rpc.RemoteExecutor;
import com.linda.framework.rpc.RpcServiceBean;
import com.linda.framework.rpc.exception.RpcException;
import com.linda.framework.rpc.exception.RpcExceptionHandler;
import com.linda.framework.rpc.exception.SimpleRpcExceptionHandler;
import com.linda.framework.rpc.utils.RpcUtils;

public class SimpleServerRemoteExecutor implements RemoteExecutor, RpcServicesHolder {

    protected ConcurrentHashMap<String, RpcServiceBean> exeCache = new ConcurrentHashMap<String, RpcServiceBean>();

    private RpcExceptionHandler exceptionHandler;

    public SimpleServerRemoteExecutor() {
        exceptionHandler = new SimpleRpcExceptionHandler();
    }

    @Override
    public void oneway(RemoteCall call) {
        RpcUtils.invokeMethod(this.findService(call), call.getMethod(), call.getArgs(), exceptionHandler);
    }

    @Override
    public Object invoke(RemoteCall call) {
        return RpcUtils.invokeMethod(this.findService(call), call.getMethod(), call.getArgs(), exceptionHandler);
    }

    public void registerRemote(Class<?> clazz, Object ifaceImpl) {
        this.registerRemote(clazz, ifaceImpl, null);
    }

    /**
     * 注册服务
     * @param clazz     class，一般是接口
     * @param ifaceImpl 接口实现类
     * @param version   版本号
     */
    public void registerRemote(Class<?> clazz, Object ifaceImpl, String version) {
        Object service = exeCache.get(clazz.getName());
        if (service != null && service != ifaceImpl) {
            throw new RpcException("can't register service " + clazz.getName() + " again");
        }
        if (ifaceImpl == service || ifaceImpl == null) {
            return;
        }
        if (version == null) {
            version = RpcUtils.DEFAULT_VERSION;
        }
        exeCache.put(this.genExeKey(clazz.getName(), version), new RpcServiceBean(clazz, ifaceImpl, version));
    }

    /**
     * 通过服务接口名称和版本号来为cache生成key值，
     * 后续如果业务需要，可能粒度很细，可以修改该函数
     * @param service   服务名称
     * @param version   服务版本号
     * @return
     */
    private String genExeKey(String service, String version) {
        if (version != null) {
            return service + "_" + version;
        }
        return service;
    }

    private Object findService(RemoteCall call) {
        String exeKey = this.genExeKey(call.getService(), call.getVersion());
        RpcServiceBean object = exeCache.get(exeKey);
        if (object == null || object.getBean() == null) {
            throw new RpcException("service " + call.getService() + " version:" + call.getVersion() + " not exist");
        }
        return object.getBean();
    }

    @Override
    public void startService() {

    }

    @Override
    public void stopService() {

    }

    public RpcExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    public void setExceptionHandler(RpcExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    public List<RpcServiceBean> getRpcServices() {
        ArrayList<RpcServiceBean> list = new ArrayList<RpcServiceBean>();
        list.addAll(exeCache.values());
        return list;
    }
}
