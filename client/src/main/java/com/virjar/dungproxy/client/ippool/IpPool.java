package com.virjar.dungproxy.client.ippool;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.virjar.dungproxy.client.ippool.config.Context;
import com.virjar.dungproxy.client.ippool.config.ObjectFactory;
import com.virjar.dungproxy.client.ippool.exception.PoolDestroyException;
import com.virjar.dungproxy.client.ippool.strategy.ResourceFacade;
import com.virjar.dungproxy.client.util.CommonUtil;
import com.virjar.dungproxy.client.model.AvProxy;

/**
 * Description: IpListPool
 *
 * @author lingtong.fu
 * @version 2016-09-03 01:13
 */
public class IpPool {

    private Logger logger = LoggerFactory.getLogger(IpPool.class);

    private Map<String, DomainPool> pool = Maps.newConcurrentMap();

    private volatile boolean isRunning = false;

    private FeedBackThread feedBackThread;
    private FreshResourceThread freshResourceThread;

    private IpPool() {
        init();
    }

    private void init() {
        isRunning = true;
        unSerialize();
        feedBackThread = new FeedBackThread();
        freshResourceThread = new FreshResourceThread();
        feedBackThread.start();
        freshResourceThread.start();
    }

    private static IpPool instance = new IpPool();

    public static IpPool getInstance() {
        return instance;
    }

    public void destroy() {
        Context.getInstance().getAvProxyDumper().serializeProxy(getPoolInfo());
        isRunning = false;
        feedBackThread.interrupt();
        freshResourceThread.interrupt();
    }

    public void unSerialize() {
        Map<String, List<AvProxy>> stringListMap = Context.getInstance().getAvProxyDumper().unSerializeProxy();
        if (stringListMap == null) {
            return;
        }
        String importer = Context.getInstance().getResourceFacade();
        for (Map.Entry<String, List<AvProxy>> entry : stringListMap.entrySet()) {
            if (pool.containsKey(entry.getKey())) {
                pool.get(entry.getKey()).addAvailable(entry.getValue());
            } else {
                pool.put(entry.getKey(), new DomainPool(entry.getKey(),
                        ObjectFactory.<ResourceFacade> newInstance(importer), entry.getValue()));
            }
        }
    }

    public AvProxy bind(String host, String url, Object userID) {
        if (!isRunning) {
            throw new PoolDestroyException();
        }
        if (!pool.containsKey(host)) {
            synchronized (this) {
                if (!pool.containsKey(host)) {
                    String importer = Context.getInstance().getResourceFacade();
                    pool.put(host, new DomainPool(host, ObjectFactory.<ResourceFacade> newInstance(importer)));
                }
            }
        }
        return pool.get(host).bind(url, userID);
    }

    public Map<String, List<AvProxy>> getPoolInfo() {
        return Maps.transformValues(pool, new Function<DomainPool, List<AvProxy>>() {
            @Override
            public List<AvProxy> apply(DomainPool domainPool) {// copy 一份新数据出去,数据结构会给外部使用,随意暴露可能会导致数据错误
                return Lists.transform(domainPool.availableProxy(), new Function<AvProxy, AvProxy>() {
                    @Override
                    public AvProxy apply(AvProxy input) {
                        return input.copy();
                    }
                });

            }
        });
    }

    // 向服务器反馈不可用IP
    private class FeedBackThread extends Thread {
        @Override
        public void run() {
            while (isRunning) {
                CommonUtil.sleep(Context.getInstance().getFeedBackDuration());
                Context.getInstance().getAvProxyDumper().serializeProxy(getPoolInfo());
                for (DomainPool domainPool : pool.values()) {
                    try {
                        domainPool.feedBack();
                    } catch (Exception e) {
                        logger.error("ip feedBack error for domain:{}", domainPool.getDomain(), e);
                    }
                }
            }
        }
    }

    // 检查IP池资源是否足够,不够则启动线程下载资源
    private class FreshResourceThread extends Thread {
        @Override
        public void run() {
            while (isRunning) {
                for (DomainPool domainPool : pool.values()) {
                    try {
                        if (domainPool.needFresh()) {
                            domainPool.fresh();
                        }
                    } catch (Exception e) {
                        logger.error("error when fresh ip pool for domain:{}", domainPool.getDomain(), e);
                    }
                }
                CommonUtil.sleep(4000);
            }
        }
    }
}