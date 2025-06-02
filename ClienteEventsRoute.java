package com.example.pipeline;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.mongodb.client.MongoClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Date;

@ApplicationScoped
public class ClienteEventsRoute extends RouteBuilder {

    @ConfigProperty(name = "kafka.topic.name")
    String kafkaTopicName;

    @ConfigProperty(name = "mongo.collection.name")
    String mongoCollectionName;

    @ConfigProperty(name = "quarkus.mongodb.database")
    String mongoDatabase;

    @ConfigProperty(name = "quarkus.kafka.bootstrap.servers")
    String kafkaBootstrapServers;

    @ConfigProperty(name = "kafka.group.id")
    String kafkaGroupId;

    // Inyecta el cliente MongoDB gestionado por Quarkus
    // El nombre del bean por defecto es "defaultMongoClient" si solo hay una configuración
    @Inject
    MongoClient mongoClient; // Camel usará este cliente si se especifica el bean con el mismo nombre que el componente mongo


    @Override
    public void configure() throws Exception {
        Jsonb jsonb = JsonbBuilder.create();

        // Definición de la URI de Kafka usando las propiedades de configuración
        String kafkaUri = String.format("kafka:%s?brokers=%s&groupId=%s",
                                        kafkaTopicName, kafkaBootstrapServers, kafkaGroupId);

        // Definición de la URI de MongoDB usando las propiedades de configuración
        // El nombre "mongoClientBean" puede ser el nombre del bean inyectado por Quarkus
        // o un nombre que registres. Por defecto Quarkus registra "defaultMongoClient".
        String mongoUri = String.format("mongodb:defaultMongoClient?database=%s&collection=%s&operation=insert",
                                        mongoDatabase, mongoCollectionName);

        from(kafkaUri)
            .routeId("kafkaToMongoRoute")
            .log(LoggingLevel.INFO, "Mensaje recibido de Kafka: ${body}")
            .process(new Processor() {
                @Override
                public void process(Exchange exchange) throws Exception {
                    String kafkaMessage = exchange.getIn().getBody(String.class);
                    if (kafkaMessage == null || kafkaMessage.trim().isEmpty()) {
                        log.warn("Mensaje de Kafka vacío o nulo, omitiendo.");
                        exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE);
                        return;
                    }

                    JsonObject fullMessage = jsonb.fromJson(kafkaMessage, JsonObject.class);

                    // Debezium < 1.6 usaba 'payload' directamente, luego se estandarizó 'before' y 'after' dentro de 'payload'
                    // Si value.converter.schemas.enable=false, el payload es el JSON directo
                    // Si es true, el payload está dentro de un objeto "payload"
                    JsonObject payload = fullMessage; // Asumiendo schemas.enable=false
                    if (fullMessage.containsKey("payload") && fullMessage.get("payload").getValueType() == JsonValue.ValueType.OBJECT) {
                         payload = fullMessage.getJsonObject("payload");
                    }


                    // 'op' indica la operación: c=create, u=update, d=delete, r=read (snapshot)
                    String operation = payload.getString("op", "");
                    JsonObject data = null;

                    if ("d".equals(operation)) {
                        log.info("Evento de eliminación detectado para: " + (payload.containsKey("before") ? payload.getJsonObject("before").toString() : "ID no disponible"));
                        // Aquí podrías implementar la lógica para eliminar el documento de MongoDB
                        // Por ahora, solo lo registramos y detenemos el procesamiento de este mensaje.
                        exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE);
                        return;
                    }

                    if (payload.containsKey("after") && !payload.isNull("after")) {
                        data = payload.getJsonObject("after");
                    } else if ("r".equals(operation) || "c".equals(operation) || "u".equals(operation)) {
                        // Para snapshot (r), create (c) o update (u) sin 'after' (raro, pero posible si el mensaje es solo el payload)
                        // o si la estructura es directamente el payload (configuraciones más antiguas o específicas de Debezium)
                        data = payload; // Asumimos que el payload es el objeto de datos directamente en estos casos
                                       // Esto puede necesitar ajuste según la estructura exacta del mensaje de Debezium
                    }


                    if (data == null) {
                        log.warn("No se pudieron extraer los datos ('after' o payload directo) del mensaje de Debezium: " + kafkaMessage);
                        exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE);
                        return;
                    }

                    Map<String, Object> mongoDocument = new HashMap<>();
                    // Mapear campos de 'data' a 'mongoDocument'
                    if (data.containsKey("id") && !data.isNull("id")) {
                         mongoDocument.put("id_postgres", data.getInt("id"));
                    }

                    if (data.containsKey("nombre") && !data.isNull("nombre")) {
                        String nombreOriginal = data.getString("nombre");
                        mongoDocument.put("nombre", nombreOriginal.toUpperCase()); // Transformación
                    }

                    if (data.containsKey("email") && !data.isNull("email")) {
                        mongoDocument.put("email", data.getString("email"));
                    }

                    // Manejo de fecha_creacion
                    // Debezium para TIMESTAMP WITHOUT TIME ZONE en PostgreSQL puede enviar microsegundos desde epoch
                    if (data.containsKey("fecha_creacion") && !data.isNull("fecha_creacion")) {
                        // Verificar si es un número (epoch) o un string (ISO)
                        JsonValue fechaCreacionValue = data.get("fecha_creacion");
                        if (fechaCreacionValue.getValueType() == JsonValue.ValueType.NUMBER) {
                            long microseconds = data.getJsonNumber("fecha_creacion").longValue();
                            mongoDocument.put("fecha_creacion", new Date(microseconds / 1000)); // Convertir micro a mili
                        } else if (fechaCreacionValue.getValueType() == JsonValue.ValueType.STRING) {
                            // Si Debezium lo envía como String, intentar parsearlo.
                            // Esto depende de la configuración del conector (time.precision.mode)
                            // Por ejemplo, io.debezium.time.ZonedTimestamp podría ser una opción.
                            // Para simplificar, asumimos que si es String, ya está en un formato que Mongo puede entender
                            // o necesitarías un SimpleDateFormat aquí.
                            // Por ahora, si es string, lo pasamos tal cual o lo convertimos si conocemos el formato.
                            // Este ejemplo asume que si es número es epoch en microsegundos.
                            log.warn("fecha_creacion es un String: " + data.getString("fecha_creacion") + ". Se requiere parseo específico o ajuste en el conector Debezium.");
                            mongoDocument.put("fecha_creacion_str", data.getString("fecha_creacion"));
                        }
                    } else {
                        // Fallback o manejar como nulo si es apropiado
                        // mongoDocument.put("fecha_creacion", null);
                    }

                    if (mongoDocument.isEmpty() || !mongoDocument.containsKey("id_postgres")) {
                         log.warn("Documento transformado está vacío o no contiene id_postgres, omitiendo inserción: " + data.toString());
                         exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE);
                         return;
                    }

                    exchange.getIn().setBody(mongoDocument);
                }
            })
            .filter(simple("${exchangeProperty.ROUTE_STOP} == null")) // Solo continuar si no se marcó para detener
            .log(LoggingLevel.INFO, "Documento transformado para MongoDB: ${body}")
            .to(mongoUri)
            .log(LoggingLevel.INFO, "Datos procesados para MongoDB colección: " + mongoCollectionName);
    }
}