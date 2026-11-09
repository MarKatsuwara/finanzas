CREATE TABLE movimientos (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo        VARCHAR(10)    NOT NULL CHECK (tipo IN ('INGRESO', 'GASTO')),
    importe     NUMERIC(12, 2) NOT NULL CHECK (importe > 0),
    fecha       DATE           NOT NULL,
    descripcion VARCHAR(255)   NOT NULL
);
