package com.zervice.common.jmx;

import lombok.extern.log4j.Log4j2;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
public class JmxMetrics {
    private static final String DOMAIN_NAME = "Promptai";
    //Mbean is used to show the metric in jmx of the application
    private static final MBeanServer MBEAN_SERVER = ManagementFactory.getPlatformMBeanServer();//MBeanServerFactory.createMBeanServer(DOMAIN_NAME);
    //For Mbean didn't provide the lookup object function, we have to store one mbean refer here.
    private static final ConcurrentHashMap<String, Object> mbeanMap = new ConcurrentHashMap<>();

    private static Object register(Object mbeanObject, String name) throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {
        String objectName = DOMAIN_NAME + ":" + name;
        Object lastMbean = mbeanMap.putIfAbsent(name, mbeanObject);
        if (lastMbean == null) {
            MBEAN_SERVER.registerMBean(mbeanObject, new ObjectName(objectName));
            return mbeanObject;
        }
        else {
            LOG.warn("Duplicate object name, maybe caused by getMbean at the same time: " + objectName);
            return lastMbean;
        }
    }

    private synchronized static void unregister(String name) throws MalformedObjectNameException, MBeanRegistrationException {
        String objectName = DOMAIN_NAME + ":" + name;
        Object removedObject = mbeanMap.remove(name);
        if (removedObject != null) {
            try {
                MBEAN_SERVER.unregisterMBean(new ObjectName(objectName));
            } catch (InstanceNotFoundException e) {
                LOG.warn("Cannot unregister the mbean, it means the our map is not the same with mbeanServer. Need Investigate: " + objectName);
            }
        }
    }

    public static <T> T getMbean(String name, Class<T> mbean) {
        if (mbeanMap.containsKey(name)) {
            return (T) mbeanMap.get(name);
        }
        try {
            T mbeanObject = mbean.newInstance();
            return (T) register(mbeanObject, name);
        } catch (Exception e) {
            LOG.error("Cannot create new instance for the mbean, return a null object: " + name, e);
            return null;
        }
    }
}
