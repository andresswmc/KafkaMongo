### Paso 1: PostgreSQL

1.  **Iniciar PostgreSQL**:
    Asegúrate de que tu servidor PostgreSQL esté en ejecución. Si usas Docker:
    ''bash
        docker run -d --name postgres-cdc \
        -p 5432:5432 \
        -e POSTGRES_USER=miusuario \
        -e POSTGRES_PASSWORD=micontraseña \
        -e POSTGRES_DB=mibd \
        postgres:13 -c wal_level=logical
    ''
    *Reemplaza 'miusuario', 'micontraseña' y 'mibd' según necesites.*

2.  **Crear Tabla y Datos de Prueba**:
    Conéctate a tu instancia de PostgreSQL (usando 'psql' o tu cliente SQL preferido) y ejecuta el script ubicado en 'postgres_setup/init_db.sql'.

    Contenido de 'postgres_setup/init_db.sql':
    """sql
    -- Conéctate a tu base de datos (ej. mibd) antes de ejecutar esto.

    DROP TABLE IF EXISTS clientes;

    CREATE TABLE clientes (
        id SERIAL PRIMARY KEY,
        nombre VARCHAR(255) NOT NULL,
        email VARCHAR(255) UNIQUE NOT NULL,
        fecha_creacion TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
    );

    -- Insertar datos de prueba
    INSERT INTO clientes (nombre, email) VALUES ('juan perez', 'juan.perez@example.com');
    INSERT INTO clientes (nombre, email) VALUES ('ana lopez', 'ana.lopez@example.com');
    INSERT INTO clientes (nombre, email) VALUES ('carlos sanchez', 'carlos.sanchez@example.com');

    -- Verificar
    SELECT * FROM clientes;
    """