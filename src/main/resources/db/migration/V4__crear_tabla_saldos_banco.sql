-- Lo que tenías en el banco en una fecha ("el 10/11 tenía 400 €").
-- Se guardan todos (historial); la app usa el más reciente como punto de partida.
CREATE TABLE saldos_banco (
    id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fecha   DATE           NOT NULL,
    importe NUMERIC(12, 2) NOT NULL -- puede ser negativo si estás en descubierto
);

-- Índice: acelera buscar el saldo más reciente
CREATE INDEX idx_saldos_banco_fecha ON saldos_banco (fecha);
