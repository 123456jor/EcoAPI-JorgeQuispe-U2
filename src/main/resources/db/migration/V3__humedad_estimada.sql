ALTER TABLE pesaje ADD COLUMN humedad_estimada NUMERIC(5,2) NOT NULL DEFAULT 0 CHECK (humedad_estimada BETWEEN 0 AND 100);
UPDATE pesaje SET humedad_estimada=12.5 WHERE observacion='Dato semilla del examen';
