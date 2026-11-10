-- Nuevas frecuencias: BIMESTRAL (cada 2 meses), TRIMESTRAL (cada 3) y SEMESTRAL (cada 6).
--
-- La regla (CHECK) que solo dejaba SEMANAL, MENSUAL o ANUAL se creó en la V3 sin nombre,
-- y cada base de datos le puso uno distinto. Para cambiarla igual en PostgreSQL y en H2:
--   1. creamos una columna nueva,
--   2. copiamos los datos,
--   3. borramos la vieja (y con ella su regla),
--   4. le damos a la nueva el nombre de siempre y una regla CON nombre.

ALTER TABLE pagos_recurrentes ADD COLUMN frecuencia_nueva VARCHAR(12);

UPDATE pagos_recurrentes SET frecuencia_nueva = frecuencia;

ALTER TABLE pagos_recurrentes DROP COLUMN frecuencia;

ALTER TABLE pagos_recurrentes RENAME COLUMN frecuencia_nueva TO frecuencia;

ALTER TABLE pagos_recurrentes ALTER COLUMN frecuencia SET NOT NULL;

ALTER TABLE pagos_recurrentes
    ADD CONSTRAINT ck_pagos_recurrentes_frecuencia
        CHECK (frecuencia IN ('SEMANAL', 'MENSUAL', 'BIMESTRAL', 'TRIMESTRAL', 'SEMESTRAL', 'ANUAL'));
