package ch.pgras.kafkalearning.streams.avro.demo;

import ch.pgras.kafkalearning.streams.demo.avro.CreatedEvent;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class EventHandlingService {

    private static final Logger logger = LoggerFactory.getLogger(EventHandlingService.class);

    public static final String BOOTSTRAP_SERVERS = "localhost:9092";
    public static final String SCHEMA_REGISTRY_URL = "http://localhost:8081";
    public static final String STATE_DIR = "./cp/kafka-streams-state";
    public static final String CREATED_EVENT_TOPIC = "created-event-avro";

    public static void main(String[] args) {

        final KafkaStreams streams = buildEventHandler(BOOTSTRAP_SERVERS, SCHEMA_REGISTRY_URL, STATE_DIR);

        // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

        streams.start();
    }

    static KafkaStreams buildEventHandler(final String bootstrapServers,
                                          final String schemaRegistryUrl,
                                          final String stateDir) {
        final Properties streamsConfiguration = new Properties();
        // Give the Streams application a unique name.  The name must be unique in the Kafka cluster
        // against which the application is run.
        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "event-handling-service");
        streamsConfiguration.put(StreamsConfig.CLIENT_ID_CONFIG, "event-handling-service-client");
        // Where to find Kafka broker(s).
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // Where to find the Confluent schema registry instance(s)
        streamsConfiguration.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        // Specify default (de)serializers for record keys and for record values.
        streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class);
        streamsConfiguration.put(StreamsConfig.STATE_DIR_CONFIG, stateDir);
        streamsConfiguration.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Records should be flushed every 10 seconds. This is less than the default
        // in order to keep this example interactive.
        // streamsConfiguration.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 10 * 1000);

        // fixme: this is to test something (default value is 600 * 1000)
        streamsConfiguration.put(StreamsConfig.STATE_CLEANUP_DELAY_MS_CONFIG, 10 * 1000);

        final Topology topology = new Topology();
        topology.addSource("textInTopicSource", CREATED_EVENT_TOPIC);
        topology.addProcessor("SystemOutProcessor", SystemOutPrintProcessor::new, "textInTopicSource");
        topology.addSink("textOutTopicSink", "textOutTopic", new StringSerializer(), new StringSerializer(), "SystemOutProcessor");


        return new KafkaStreams(topology, streamsConfiguration);
    }


    private static class SystemOutPrintProcessor implements Processor<String, CreatedEvent> {

        ProcessorContext context = null;

        @Override
        public void init(ProcessorContext processorContext) {
            context = processorContext;
        }

        @Override
        public void process(String key, CreatedEvent value) {

            System.out.printf("Processing: Key='%s', Value='%s'.%n", key, value);
            context.forward(key, value.getPayload());
            if (value.getPayload().equals("exception")) {
                throw new RuntimeException("this is a test Exception.");
            }
            context.commit();

        }

        @Override
        public void close() {
        }
    }


    public static <T> T convertInstanceOfObject(Object o, Class<T> clazz) {
        try {
            return clazz.cast(o);
        } catch (ClassCastException e) {
            return null;
        }
    }

}
