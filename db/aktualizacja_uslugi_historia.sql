USE hotel_manager;
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS uslugi (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 kod VARCHAR(36) NOT NULL UNIQUE,
 nazwa VARCHAR(80) NOT NULL UNIQUE,
 jednostka VARCHAR(30) NOT NULL,
 cena DECIMAL(10,2) NOT NULL CHECK(cena>0),
 aktywna BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS pozycje_uslug (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 rezerwacja_id BIGINT NOT NULL,
 usluga_id BIGINT NOT NULL,
 pracownik_id BIGINT NOT NULL,
 nazwa VARCHAR(80) NOT NULL,
 jednostka VARCHAR(30) NOT NULL,
 cena_jednostkowa DECIMAL(10,2) NOT NULL CHECK(cena_jednostkowa>0),
 ilosc INT NOT NULL CHECK(ilosc BETWEEN 1 AND 1000),
 status ENUM('AKTYWNA','ANULOWANA') NOT NULL DEFAULT 'AKTYWNA',
 utworzono DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 anulowano DATETIME(6) NULL,
 anulowal_pracownik_id BIGINT NULL,
 FOREIGN KEY(rezerwacja_id) REFERENCES rezerwacje(id) ON DELETE RESTRICT,
 FOREIGN KEY(usluga_id) REFERENCES uslugi(id) ON DELETE RESTRICT,
 FOREIGN KEY(pracownik_id) REFERENCES pracownicy(id) ON DELETE RESTRICT,
 FOREIGN KEY(anulowal_pracownik_id) REFERENCES pracownicy(id) ON DELETE RESTRICT,
 CHECK((status='AKTYWNA' AND anulowano IS NULL AND anulowal_pracownik_id IS NULL)
 OR(status='ANULOWANA' AND anulowano IS NOT NULL AND anulowal_pracownik_id IS NOT NULL)),
 INDEX idx_poz_rezerwacja(rezerwacja_id,status,id)
) ENGINE=InnoDB;
INSERT IGNORE INTO uslugi(kod,nazwa,jednostka,cena) VALUES
('SNIADANIE','Śniadanie','porcja',45.00),('PARKING','Parking','doba',30.00),('DOSTAWKA','Dostawka','noc',60.00);
SET @sql=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='historia_rezerwacji' AND column_name='platnosc_id')=0,'ALTER TABLE historia_rezerwacji ADD COLUMN platnosc_id BIGINT NULL','SELECT 1');
PREPARE q FROM @sql;
EXECUTE q;
DEALLOCATE PREPARE q;
SET @sql=IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='historia_rezerwacji' AND index_name='uq_historia_platnosc')=0,'ALTER TABLE historia_rezerwacji ADD UNIQUE KEY uq_historia_platnosc(platnosc_id)','SELECT 1');
PREPARE q FROM @sql;
EXECUTE q;
DEALLOCATE PREPARE q;
SET @sql=IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='historia_rezerwacji' AND index_name='idx_historia_czas')=0,'ALTER TABLE historia_rezerwacji ADD INDEX idx_historia_czas(rezerwacja_id,utworzono,id)','SELECT 1');
PREPARE q FROM @sql;
EXECUTE q;
DEALLOCATE PREPARE q;
SET @sql=IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='historia_rezerwacji' AND constraint_name='fk_historia_platnosc')=0,'ALTER TABLE historia_rezerwacji ADD CONSTRAINT fk_historia_platnosc FOREIGN KEY(platnosc_id) REFERENCES platnosci(id) ON DELETE RESTRICT','SELECT 1');
PREPARE q FROM @sql;
EXECUTE q;
DEALLOCATE PREPARE q;
SET @istnieje=(SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='historia_rezerwacji' AND constraint_name='chk_historia_akcja' AND constraint_type='CHECK');
SET @nowy='ADD CONSTRAINT chk_historia_akcja CHECK(akcja IN (''UTWORZENIE'',''ZMIANA_TERMINU'',''ZMIANA_POKOJU'',''ZAMELDOWANIE'',''WYMELDOWANIE'',''ANULOWANIE'',''WPLATA'',''ZWROT'',''DODANIE_USLUGI'',''ANULOWANIE_USLUGI'',''ZMIANA_NOTATKI'',''ZMIANA_UCZESTNIKOW''))';
SET @sql=CONCAT('ALTER TABLE historia_rezerwacji ',IF(@istnieje>0,CONCAT(IF(LOCATE('MariaDB',VERSION())>0,'DROP CONSTRAINT ','DROP CHECK '),'chk_historia_akcja, '),''),@nowy);
PREPARE polecenie FROM @sql;
EXECUTE polecenie;
DEALLOCATE PREPARE polecenie;
INSERT INTO historia_rezerwacji(rezerwacja_id,pracownik_id,login_pracownika,akcja,przed,po,utworzono,platnosc_id)
SELECT p.rezerwacja_id,p.pracownik_id,k.login,p.rodzaj,NULL,
 CONCAT('Płatność #',p.id,' | ',p.rodzaj,' | Kwota: ',p.kwota,' zł | Metoda: ',p.metoda,
 ' | Odtworzono z ewidencji; poprzednie saldo nie jest odtwarzane.'),p.data_platnosci,p.id
FROM platnosci p JOIN pracownicy k ON k.id=p.pracownik_id
LEFT JOIN historia_rezerwacji h ON h.platnosc_id=p.id WHERE h.id IS NULL;
