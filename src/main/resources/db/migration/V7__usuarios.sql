-- Los usuarios de la app. Se entra con la cuenta de Google, así que NO guardamos contraseñas:
-- solo el identificador que nos da Google (google_id) y los datos para mostrar en pantalla.

CREATE TABLE usuarios (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    google_id VARCHAR(255)             NOT NULL,  -- el "DNI" de la persona en Google (nunca cambia)
    email     VARCHAR(255)             NOT NULL,
    nombre    VARCHAR(255)             NOT NULL,
    foto      VARCHAR(1000),                      -- la dirección de su foto de Google (puede no tener)
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_usuarios_google_id UNIQUE (google_id)  -- una cuenta de Google = un solo usuario
);
