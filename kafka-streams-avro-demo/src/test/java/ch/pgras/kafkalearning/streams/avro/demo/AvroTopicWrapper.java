package ch.pgras.kafkalearning.streams.avro.demo;

import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.ProducerRecord;

public class AvroTopicWrapper<K,V extends SpecificRecord> {

    String topicName;

    public ProducerRecord<K, SpecificRecord> getProducerRecord(V record){
        return null;
    }
}
