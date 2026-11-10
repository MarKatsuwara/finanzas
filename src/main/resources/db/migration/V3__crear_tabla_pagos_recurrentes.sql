-- Pagos que se repiten: alquiler, Netflix, nómina...
CREATE TABLE pagos_recurrentes (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    descripcion  VARCHAR(255)   NOT NULL,
    tipo         VARCHAR(10)    NOT NULL CHECK (tipo IN ('INGRESO', 'GASTO')),
    importe      NUMERIC(12, 2) NOT NULL CHECK (importe > 0),
    frecuencia   VARCHAR(10)    NOT NULL CHECK (frecuencia IN ('SEMANAL', 'MENSUAL', 'ANUAL')),
    fecha_inicio DATE           NOT NULL, -- el primer día que toca pagar (o cobrar)
    activo       BOOLEAN        NOT NULL DEFAULT TRUE, -- false = dado de baja, ya no cuenta
    categoria_id BIGINT -- opcional, igual que en los movimientos
);

ALTER TABLE pagos_recurrentes
    ADD CONSTRAINT fk_pagos_recurrentes_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id);

CREATE INDEX idx_pagos_recurrentes_categoria ON pagos_recurrentes (categoria_id);
