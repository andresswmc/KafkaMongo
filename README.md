# Pipeline de Migración y Sincronización de Datos: PostgreSQL a MongoDB con Kafka


Este proyecto implementa un pipeline de eventos para migrar y mantener sincronizados los datos de clientes desde una base de datos PostgreSQL hacia MongoDB. Utiliza Kafka y Kafka Connect con Debezium para la Captura de Datos de Cambios (CDC), y una aplicación Java con Quarkus y Apache Camel para el procesamiento y la transformación de los mensajes.


## Caso de Uso


Una empresa quiere migrar los datos de sus clientes desde un sistema transaccional basado en PostgreSQL a una base de datos MongoDB para análisis. Para evitar interrupciones y mantener ambos sistemas sincronizados, desea hacerlo mediante un pipeline de eventos con Kafka.


## Requisitos Funcionales Cubiertos


1.  **PostgreSQL**: Creación de tabla 'clientes' e inserción de datos de prueba.

2.  **Kafka & Kafka Connect**: Uso de Debezium para capturar cambios en 'clientes' y publicarlos en un topic de Kafka.

3.  **Java (Quarkus + Apache Camel)**: Consumo de mensajes de Kafka, transformación (capitalizar nombre) e inserción en MongoDB.

4.  **MongoDB**: Validación de la llegada y transformación correcta de los datos.


# Data Migration and Synchronization Pipeline: PostgreSQL to MongoDB with Kafka

This project implements an event-driven pipeline to migrate and keep customer data synchronized from a PostgreSQL database to MongoDB. It uses Kafka and Kafka Connect with Debezium for Change Data Capture (CDC), and a Java application with Quarkus and Apache Camel for message processing and transformation.

## Use Case

A company wants to migrate its customer data from a PostgreSQL-based transactional system to a MongoDB database for analytics. To avoid outages and keep both systems synchronized, it wants to do this via an event pipeline with Kafka.

## Functional Requirements Covered

1.  **PostgreSQL**: Creation of table ‘customers’ and insertion of test data.
2.  **Kafka & Kafka Connect**: Using Debezium to capture changes in ‘customers’ and publish them to a Kafka topic.
3.  **Java (Quarkus + Apache Camel)**: Kafka message consumption, transformation (capitalize name) and insertion into MongoDB.
4.  **MongoDB**: Validation of the arrival and correct transformation of data.
