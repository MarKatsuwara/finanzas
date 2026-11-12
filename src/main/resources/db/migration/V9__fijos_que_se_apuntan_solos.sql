-- Los gastos fijos que se apuntan SOLOS como movimientos cuando llega su día.

-- 1. En cada fijo, el interruptor "Apuntar solo" (encendido de serie).
--    Se apaga en los que cambian de precio cada vez (la luz, el agua...), que se apuntan a mano.
ALTER TABLE pagos_recurrentes ADD COLUMN apuntar_solo BOOLEAN NOT NULL DEFAULT TRUE;

-- 2. En cada fijo, la "marca": hasta qué día está ya apuntado.
--    Empieza en HOY para todos: así no se rellenan los meses pasados (esos ya los apuntaste a mano).
ALTER TABLE pagos_recurrentes ADD COLUMN apuntado_hasta DATE;

UPDATE pagos_recurrentes SET apuntado_hasta = CURRENT_DATE;

ALTER TABLE pagos_recurrentes ALTER COLUMN apuntado_hasta SET NOT NULL;

-- 3. En cada movimiento: ¿lo apuntó la app sola? ¿Y de qué fijo salió?
ALTER TABLE movimientos ADD COLUMN automatico BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE movimientos ADD COLUMN pago_recurrente_id BIGINT;

-- Si borras el fijo, sus movimientos se quedan (es dinero que de verdad salió): solo se pierde el enlace.
ALTER TABLE movimientos
    ADD CONSTRAINT fk_movimientos_pago_recurrente FOREIGN KEY (pago_recurrente_id)
        REFERENCES pagos_recurrentes (id) ON DELETE SET NULL;

CREATE INDEX idx_movimientos_pago_recurrente ON movimientos (pago_recurrente_id);
