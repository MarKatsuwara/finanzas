-- Tabla de categorías (Comida, Ocio, Vivienda...)
CREATE TABLE categorias (
    id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE
);

-- Categorías que vienen de serie
INSERT INTO categorias (nombre) VALUES
    ('Comida'),
    ('Vivienda'),
    ('Transporte'),
    ('Ocio'),
    ('Suscripciones'),
    ('Nómina'),
    ('Otros');

-- Cada movimiento puede tener una categoría (opcional: por eso admite NULL)
ALTER TABLE movimientos ADD COLUMN categoria_id BIGINT;

-- Clave foránea: categoria_id solo puede apuntar a una categoría que exista
ALTER TABLE movimientos
    ADD CONSTRAINT fk_movimientos_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id);

-- Índice: acelera buscar los movimientos de una categoría
CREATE INDEX idx_movimientos_categoria ON movimientos (categoria_id);
