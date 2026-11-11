USE hotel_manager;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS historia_rezerwacji (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 rezerwacja_id BIGINT NOT NULL,
 pracownik_id BIGINT NOT NULL,
 login_pracownika VARCHAR(60) NOT NULL,
 akcja VARCHAR(32) NOT NULL,
 przed TEXT NULL,
 po TEXT NOT NULL,
 utworzono DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CONSTRAINT fk_historia_rezerwacja FOREIGN KEY(rezerwacja_id) REFERENCES rezerwacje(id) ON DELETE RESTRICT,
 CONSTRAINT fk_historia_pracownik FOREIGN KEY(pracownik_id) REFERENCES pracownicy(id) ON DELETE RESTRICT,
 CONSTRAINT chk_historia_akcja CHECK(akcja IN ('UTWORZENIE','ZMIANA_TERMINU','ZMIANA_POKOJU','ZAMELDOWANIE','WYMELDOWANIE','ANULOWANIE')),
 INDEX idx_historia_rezerwacja(rezerwacja_id,id)
) ENGINE=InnoDB;
