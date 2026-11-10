-- Nueva frecuencia: CADA_4_SEMANAS (cada 28 días, 13 veces al año). Por ejemplo, muchos gimnasios.
--
-- 1. Quitamos la regla de las frecuencias (en la V5 le pusimos nombre, así que ahora es fácil).
-- 2. Ensanchamos la columna: "CADA_4_SEMANAS" tiene 14 letras y solo cabían 12.
-- 3. Volvemos a poner la regla, ya con la frecuencia nueva.

ALTER TABLE pagos_recurrentes DROP CONSTRAINT ck_pagos_recurrentes_frecuencia;

ALTER TABLE pagos_recurrentes ALTER COLUMN frecuencia SET DATA TYPE VARCHAR(20);

ALTER TABLE pagos_recurrentes
    ADD CONSTRAINT ck_pagos_recurrentes_frecuencia
        CHECK (frecuencia IN ('SEMANAL', 'CADA_4_SEMANAS', 'MENSUAL', 'BIMESTRAL', 'TRIMESTRAL', 'SEMESTRAL', 'ANUAL'));
