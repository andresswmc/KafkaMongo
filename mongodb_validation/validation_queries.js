### MongoDB - Validación

1.  **Conectarse a MongoDB**:
    Usa 'mongosh' o MongoDB Compass para conectarte a tu instancia de MongoDB.
    '''bash
    mongosh mongodb://localhost:27017/miBaseDeDatosAnalitica
    '''

2.  **Ejecutar Consultas de Validación**:
    Puedes usar las consultas en 'mongodb_validation/validation_queries.js'.

    Contenido de 'mongodb_validation/validation_queries.js':
    '''javascript
    // Conectado a la base de datos: miBaseDeDatosAnalitica

    // Verificar todos los documentos en la colección clientes_transformados
    db.clientes_transformados.find().pretty();

    // Buscar un cliente específico por email
    // db.clientes_transformados.find({ email: "juan.perez@example.com" }).pretty();

    // Contar documentos
    // db.clientes_transformados.countDocuments();
    '''
    Verifica que los datos de 'clientes' aparezcan en la colección 'clientes_transformados' y que el campo 'nombre' esté en mayúsculas.

## Pruebas Adicionales

* **Nuevas Inserciones en PostgreSQL**: Inserta nuevas filas en la tabla 'clientes' de PostgreSQL y observa cómo aparecen en MongoDB después de unos segundos.
    '''sql
    INSERT INTO clientes (nombre, email) VALUES ('laura gomez', 'laura.gomez@example.com');
    '''
* **Actualizaciones en PostgreSQL**: Actualiza una fila existente.
    '''sql
    UPDATE clientes SET email = 'juan.p.revisado@example.com' WHERE nombre = 'juan perez';
    '''
    *Nota: La lógica actual de la ruta Camel usa 'operation=insert'. Para manejar actualizaciones de forma idempotente (actualizar si existe, insertar si no), podrías necesitar cambiar la operación a 'save' (que hace upsert basado en '_id')
     o implementar una lógica de búsqueda y actualización/inserción más explícita basada en 'id_postgres'.*
