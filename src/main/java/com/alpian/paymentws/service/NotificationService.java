package com.alpian.paymentws.service;

import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.stereotype.Service;

/**
 * Notification service that will send messages to Kafka broker
 */
@Service
public class NotificationService {
	private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);

	public <V> void notify(String topicName, String key, V value) {
		try {
			LOG.info("Sending notification for topicName {}, key {}, value {}", topicName, key, value);
			
			Properties props = new Properties();
	        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
	        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
	        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.springframework.kafka.support.serializer.JsonSerializer");
	
	        try (KafkaProducer<String, V> producer = new KafkaProducer<>(props)) {
	        	ProducerRecord<String, V> record =
	                    new ProducerRecord<>(topicName, key, value);
	
	            producer.send(record, (metadata, exception) -> {
	                if (exception == null) {
	                	LOG.info("Notification sent to partition {}, offset {}",
	                        metadata.partition(), metadata.offset());
	                } else {
	                	LOG.error(String.format("Error sending notification to %s for key %s", topicName, key), exception);
	                }
	            });
	        }
		} catch (Exception e) {
			// We should not propagate notification problems assuring previous changes
			LOG.error("Failed to publish notification to {} for key {}", topicName, key, e);
        }
	}
}
