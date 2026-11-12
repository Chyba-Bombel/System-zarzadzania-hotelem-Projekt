USE hotel_manager;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS historia_rezerwacji (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 rezerwacja_id BIGINT NOT NULL,
 pracownik_id BIGINT NOT NULL,
 login_pracownika VARCHAR(60) NOT NULL,
 platnosc_id BIGINT NULL,
 UNIQUE KEY uq_historia_platnosc(platnosc_id),
 CONSTRAINT fk_historia_platnosc FOREIGN KEY(platnosc_id) REFERENCES platnosci(id) ON DELETE RESTRICT,
 INDEX idx_historia_czas(rezerwacja_id,utworzono,id),
 akcja VARCHAR(32) NOT NULL,
 przed TEXT NULL,
 po TEXT NOT NULL,
 utworzono DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CONSTRAINT fk_historia_rezerwacja FOREIGN KEY(rezerwacja_id) REFERENCES rezerwacje(id) ON DELETE RESTRICT,
 CONSTRAINT fk_historia_pracownik FOREIGN KEY(pracownik_id) REFERENCES pracownicy(id) ON DELETE RESTRICT,
 CONSTRAINT chk_historia_akcja CHECK(akcja IN ('UTWORZENIE','ZMIANA_TERMINU','ZMIANA_POKOJU','ZAMELDOWANIE','WYMELDOWANIE','ANULOWANIE','WPLATA','ZWROT','DODANIE_USLUGI','ANULOWANIE_USLUGI','ZMIANA_NOTATKI','ZMIANA_UCZESTNIKOW')),
 INDEX idx_historia_rezerwacja(rezerwacja_id,id)
) ENGINE=InnoDB;
