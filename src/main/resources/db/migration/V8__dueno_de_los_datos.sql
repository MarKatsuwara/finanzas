-- Cada cosa pasa a tener DUEÑO: el usuario que la creó (como poner tu nombre en tu táper).
--
-- La columna usuario_id admite NULL a propósito: los datos que ya había antes de los usuarios
-- se quedan "sin dueño" hasta que entra el primer usuario, que se los queda (lo hace la API).

-- 1. Movimientos
ALTER TABLE movimientos ADD COLUMN usuario_id BIGINT;
ALTER TABLE movimientos
    ADD CONSTRAINT fk_movimientos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id);
CREATE INDEX idx_movimientos_usuario ON movimientos (usuario_id);

-- 2. Pagos recurrentes
ALTER TABLE pagos_recurrentes ADD COLUMN usuario_id BIGINT;
ALTER TABLE pagos_recurrentes
    ADD CONSTRAINT fk_pagos_recurrentes_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id);
CREATE INDEX idx_pagos_recurrentes_usuario ON pagos_recurrentes (usuario_id);

-- 3. Saldos del banco
ALTER TABLE saldos_banco ADD COLUMN usuario_id BIGINT;
ALTER TABLE saldos_banco
    ADD CONSTRAINT fk_saldos_banco_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id);
CREATE INDEX idx_saldos_banco_usuario ON saldos_banco (usuario_id);

-- 4. Categorías
ALTER TABLE categorias ADD COLUMN usuario_id BIGINT;
ALTER TABLE categorias
    ADD CONSTRAINT fk_categorias_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id);
CREATE INDEX idx_categorias_usuario ON categorias (usuario_id);

-- El nombre de una categoría ya no es único en TODA la tabla, sino para cada usuario
-- (tú y otra persona podéis tener los dos "Gimnasio").
-- La regla UNIQUE de la V2 se creó sin nombre, así que usamos el mismo truco que en la V5:
-- columna nueva, copiar, borrar la vieja (y con ella su regla) y renombrar.
ALTER TABLE categorias ADD COLUMN nombre_nuevo VARCHAR(50);

UPDATE categorias SET nombre_nuevo = nombre;

ALTER TABLE categorias DROP COLUMN nombre;

ALTER TABLE categorias RENAME COLUMN nombre_nuevo TO nombre;

ALTER TABLE categorias ALTER COLUMN nombre SET NOT NULL;

ALTER TABLE categorias
    ADD CONSTRAINT uq_categorias_usuario_nombre UNIQUE (usuario_id, nombre);
