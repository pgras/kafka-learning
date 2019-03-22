package ch.pgras.kafkalearning.basics.avro;

import ch.pgras.kafkalearning.basic.avro.Action;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class AvroProducer {

    private static Logger logger = LoggerFactory.getLogger(AvroProducer.class);

    public static void main(String[] args) {

        logger.info("Starting Avro Producer...");

        Properties properties = new Properties();
        // normal producer
        properties.setProperty("bootstrap.servers", "127.0.0.1:9092");
        properties.setProperty("acks", "all");
        properties.setProperty("retries", "10");
        // avro part
        properties.setProperty("key.serializer", StringSerializer.class.getName());
        properties.setProperty("value.serializer", KafkaAvroSerializer.class.getName());
        properties.setProperty("schema.registry.url", "http://127.0.0.1:8081");

        Producer<String, Action> producer = new KafkaProducer<String, Action>(properties);

        String topic = "action-avro";

        // add a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("stopping application...");
            logger.info("closing producer...");
            producer.flush();
            producer.close();
            logger.info("done!");
        }));

        while (true) {
            Action action = createAction();

            ProducerRecord<String, Action> producerRecord = new ProducerRecord<String, Action>(
                    topic, action.getType(), action
            );

            logger.debug("Action to be sent: " + action);
            producer.send(producerRecord, new Callback() {

                @Override
                public void onCompletion(RecordMetadata metadata, Exception exception) {
                    if (exception == null) {
                        // logger.debug(metadata.toString());
                    } else {
                        exception.printStackTrace();
                    }
                }
            });
        }



    }

    static Action createAction() {
        return Action.newBuilder()
                .setType("MyNiceActionType")
                .setPayload("\n\n\nthe payload of my action...\n\n\n")
                .build();
    }
}
