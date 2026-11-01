-- Wykonaj raz na swojej istniejącej bazie przed uruchomieniem nowej wersji.
-- Nie usuwa ani nie zmienia dotychczasowych rekordów.
USE hotel_manager;
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS uczestnicy_rezerwacji (
 rezerwacja_id BIGINT NOT NULL,
 pozycja INT NOT NULL,
 imie VARCHAR(60) NOT NULL,
 nazwisko VARCHAR(80) NOT NULL,
 PRIMARY KEY (rezerwacja_id, pozycja),
 CONSTRAINT fk_uczestnik_rezerwacja FOREIGN KEY (rezerwacja_id)
   REFERENCES rezerwacje(id) ON DELETE CASCADE,
 CONSTRAINT chk_uczestnik_pozycja CHECK (pozycja BETWEEN 1 AND 9)
) ENGINE=InnoDB;
