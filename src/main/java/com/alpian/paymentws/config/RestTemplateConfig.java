package com.alpian.paymentws.config;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

import redis.clients.jedis.JedisPoolConfig;

import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

@Configuration
public class RestTemplateConfig {
	private final Logger LOG = LoggerFactory.getLogger(RestTemplateConfig.class);
	
    @Value("${spring.redis.sentinel.enabled}")
    private Boolean sentinelEnabled;

    @Value("${spring.redis.host}")
    private String redisHostName;

    @Value("${spring.redis.port}")
    private int redisPort;

    @Value("${spring.redis.sentinel.master}")
    private String redisMasterName;

    @Value("${spring.redis.sentinel.nodes}")
    private String sentinelNodes;

    @Value("${redis.maxTotal}")
    private int maxTotal;

    @Value("${redis.minIdle}")
    private int minIdle;

    @Value("${redis.maxIdle}")
    private int maxIdle;

	@Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
	
	@Bean(name="defaultRedisConnectionFactory")
    public JedisConnectionFactory redisConnectionFactory() {

        // Config
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(maxTotal);
        jedisPoolConfig.setMinIdle(minIdle);
        jedisPoolConfig.setMaxIdle(maxIdle);
        jedisPoolConfig.setTestOnBorrow(true);
        jedisPoolConfig.setTestWhileIdle(false);
        jedisPoolConfig.setTestOnCreate(false);
        jedisPoolConfig.setTestOnReturn(false);

        // Sentinel configuration
        RedisSentinelConfiguration sentinelConfiguration = null;
        JedisConnectionFactory connectionFactory = null;

        if (sentinelEnabled) {
            Set<String> sentinelNodesSet = StringUtils.commaDelimitedListToSet(sentinelNodes);
            // Sentinel configuration
            sentinelConfiguration = new RedisSentinelConfiguration(redisMasterName, sentinelNodesSet);
            connectionFactory = new JedisConnectionFactory(sentinelConfiguration, jedisPoolConfig);
            LOG.info("Multihosting configuration: hosts={}, masterName={}", sentinelNodes, redisMasterName);
        } else {
            connectionFactory = new JedisConnectionFactory(jedisPoolConfig);
            connectionFactory.setHostName(redisHostName);
            connectionFactory.setPort(redisPort);
            LOG.info("Single host configuration: host={}, port={}", redisHostName, redisPort);
        }
        connectionFactory.setUsePool(true);
        connectionFactory.setPoolConfig(jedisPoolConfig);

        // This seems to be connection timeout in millis (what a mess, java doc useless, bad naming)
        connectionFactory.setTimeout(5000);

        // Over
        return connectionFactory;
    }
}