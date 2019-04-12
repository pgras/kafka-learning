package ch.pgras.kafkalearning.streams.avro.demo;

import ch.pgras.kafkalearning.streams.demo.avro.CreatedEvent;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class TestEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TestEventConsumer.class);

    public static void main(String[] args) {
        Properties properties = new Properties();
        // normal consumer
        properties.setProperty("bootstrap.servers", "127.0.0.1:9092");
        properties.put("group.id", "event-consumer-1");
        properties.put("auto.commit.enable", "false");
        properties.put("auto.offset.reset", "earliest");

        // avro part (deserializer)
        properties.setProperty("key.deserializer", StringDeserializer.class.getName());
        properties.setProperty("value.deserializer", KafkaAvroDeserializer.class.getName());
        properties.setProperty("schema.registry.url", "http://127.0.0.1:8081");
        properties.setProperty("specific.avro.reader", "true");

        KafkaConsumer<String, CreatedEvent> kafkaConsumer = new KafkaConsumer<>(properties);
        String topic = TestEventProducer.CREATED_EVENT_TOPIC;
        kafkaConsumer.subscribe(Collections.singleton(topic));

        logger.info("Waiting for data...");

        // add a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("stopping application...");
            logger.info("closing consumer...");
            // do not commit here !!!
            kafkaConsumer.close();
            logger.info("done!");
        }));

        while (true) {

            ConsumerRecords<String, CreatedEvent> records = kafkaConsumer.poll(Duration.ofMillis(1000));

            for (ConsumerRecord<String, CreatedEvent> record : records) {
                SpecificRecord action = record.value();
                logger.info(action.toString());
            }

            kafkaConsumer.commitSync();
        }
    }
}
