package ch.pgras.kafkalearning.streams.avro.demo;

import ch.pgras.kafkalearning.streams.demo.avro.CreateEvent;
import ch.pgras.kafkalearning.streams.demo.avro.DeleteEvent;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class TestEventProducer {

    public static final String createEventTopic = "create-event-avro";

    private static final Logger logger = LoggerFactory.getLogger(TestEventProducer.class);

    public static void main(String[] args) {

        logger.info("Starting TestEventProducer...");

        Properties properties = new Properties();
        // normal producer
        properties.setProperty("bootstrap.servers", "127.0.0.1:9092");
        properties.setProperty("acks", "all");
        properties.setProperty("retries", "10");
        // avro part
        properties.setProperty("key.serializer", StringSerializer.class.getName());
        properties.setProperty("value.serializer", KafkaAvroSerializer.class.getName());
        properties.setProperty("schema.registry.url", "http://127.0.0.1:8081");

        Producer<String, SpecificRecord> producer = new KafkaProducer<>(properties);

        // add a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("stopping application...");
            logger.info("closing producer...");
            producer.flush();
            producer.close();
            logger.info("done!");
        }));

       for (int i=0; i<100; i++) {
            CreateEvent event = createCreateEvent();
            ProducerRecord<String, SpecificRecord> producerRecord = new ProducerRecord<>(
                    createEventTopic, event.getActionType(), event
            );
            logger.debug("Event to be sent: " + event);
            producer.send(producerRecord, (RecordMetadata metadata, Exception exception) -> {
                if (exception != null) {
                    exception.printStackTrace();
                } else {
                    // logger.debug(metadata.toString());
                }
            });
        }
    }

    static CreateEvent createCreateEvent() {
        return CreateEvent.newBuilder()
                .setActionType("CreateEvent")
                .setPayload("the payload of my event...")
                .build();
    }

    static DeleteEvent createDeleteEvent() {
        return DeleteEvent.newBuilder()
                .setActionType("DeleteEvent")
                .setPayload("the payload of my event...")
                .build();
    }
}
