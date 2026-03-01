package com.alpian.paymentws.util;


import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public abstract class LockManager<T,C> {
	private final Logger LOG = LoggerFactory.getLogger(LockManager.class);
	
	private int expireTimeout = 60000;
	
	@Autowired
    private RedisTemplate<String, Object> redisTemplateForObject;
	
	public T withLock(C context, Supplier<T> action) {
	    try {
	        boolean isFirst = lock(context);
	        if (!isFirst) {
	            return wait(context);
	        }
	        return action.get();
	    } finally {
	        unlock(context);
	    }
	}
	
	private T wait(C context) {
		LOG.info("A process is already performing an action for the context {}. Checking every {} ms, maxwait={} ms",
				context, getNextAttemptWait(), getMaxWait());
        
		T result = null;
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < getMaxWait()) {
		    try {
		        Thread.sleep(getNextAttemptWait());
		    } catch (InterruptedException e) {
		    	LOG.error("Couldn't sleep while waiting for expected action", e);
		        Thread.currentThread().interrupt();
		    }
	
		    result = checkIfCreated(context);
		    if (result != null) {
		        return result;
		    }
		}
		
		LOG.info("Reached maximum wait time {} for the context {}",
				getMaxWait(), context);
			  
		return result;
	}
	
	/**
     * Checks if it is possible to execute an action for the given context input. Returns true if the action
     * can be performed, false if another process is already doing it
     */
	protected boolean lock(C context) {
        String key = getKey(context);
        Long newVal = redisTemplateForObject.opsForValue().increment(key, 1);

        boolean ok = newVal == 1;
        if (ok) {
            redisTemplateForObject.expire(key, expireTimeout, TimeUnit.MILLISECONDS);
            LOG.info("Acquired action lock for the following key: {}, value: {}", key, newVal);
        }
        return ok;
    }

    /**
     * Marks the execution of an action as over (whether it worked or not) for the given context input
     */
    protected void unlock(C context) {
        String key = getKey(context);
        redisTemplateForObject.delete(key);
        LOG.info("Released lock for the following context: {}", context);
    }
    
    protected abstract String getKey(C context);
    
    protected abstract T checkIfCreated(C context);
    
	protected long getMaxWait() {
		return 10000l;
	}
	
	protected long getNextAttemptWait() {
		return 1000l;
	}
}
