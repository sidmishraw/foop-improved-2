/**
 * Project: Foops_1
 * Package: foop.utils
 * File: InstanceFactory.java
 * 
 * @author sidmishraw
 *         Last modified: Sep 28, 2017 8:49:04 PM
 */
package foop.utils;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author sidmishraw
 *
 *         Qualified Name: foop.utils.InstanceFactory
 *
 *         Provides utilities for generating singleton instances
 */
public class InstanceFactory {
    
    private static final Logger        logger      = LoggerFactory.getLogger(InstanceFactory.class);
    
    // just to make sure that the instance factory is thread-safe
    // I'm using ConcurrentHashMap instead of the HashMap
    private static Map<String, Object> instanceMap = new ConcurrentHashMap<>();
    
    /**
     * Provides the invoker with a singleton instance of the class requested
     * 
     * @param instanceClass
     *            The class of the singleton object
     * @return The singleton object
     */
    @SuppressWarnings("unchecked")
    public static final <T> T getInstance(Class<T> instanceClass) {
        
        Object value;
        
        T instance = Objects.isNull(value = instanceMap.get(instanceClass.getName())) ? null : (T) value;
        
        if (null == instance) {
            
            try {
                
                instance = instanceClass.newInstance();
                
                instanceMap.put(instanceClass.getName(), instance);
            } catch (InstantiationException | IllegalAccessException e) {
                
                logger.error(e.getMessage(), e);
            }
        }
        
        return instance;
    }
}
