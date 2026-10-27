CREATE DATABASE IF NOT EXISTS hotel_manager CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hotel_manager;
SET NAMES utf8mb4;

CREATE TABLE pracownicy (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 imie VARCHAR(60) NOT NULL, nazwisko VARCHAR(80) NOT NULL,
 login VARCHAR(60) NOT NULL UNIQUE, hash_hasla VARCHAR(255) NOT NULL,
 rola ENUM('ADMIN','RECEPCJA') NOT NULL, aktywny BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;
CREATE TABLE goscie (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 imie VARCHAR(60) NOT NULL, nazwisko VARCHAR(80) NOT NULL,
 telefon VARCHAR(30) NOT NULL, email VARCHAR(120) NOT NULL DEFAULT '',
 INDEX idx_gosc_nazwisko(nazwisko,imie)
) ENGINE=InnoDB;
CREATE TABLE pokoje (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, numer VARCHAR(10) NOT NULL UNIQUE,
 typ VARCHAR(40) NOT NULL, liczba_miejsc INT NOT NULL,
 cena_za_noc DECIMAL(10,2) NOT NULL,
 stan ENUM('GOTOWY','DO_SPRZATANIA','WYLACZONY') NOT NULL DEFAULT 'GOTOWY',
 CONSTRAINT chk_miejsca CHECK(liczba_miejsc BETWEEN 1 AND 10),
 CONSTRAINT chk_cena CHECK(cena_za_noc>0)
) ENGINE=InnoDB;
CREATE TABLE rezerwacje (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 gosc_id BIGINT NOT NULL, pokoj_id BIGINT NOT NULL, pracownik_id BIGINT NOT NULL,
 data_od DATE NOT NULL, data_do DATE NOT NULL, liczba_osob INT NOT NULL,
 cena_za_noc DECIMAL(10,2) NOT NULL,
 status ENUM('POTWIERDZONA','ZAMELDOWANA','ZAKONCZONA','ANULOWANA') NOT NULL,
 uwagi VARCHAR(500) NOT NULL DEFAULT '', utworzono DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_rez_gosc FOREIGN KEY(gosc_id) REFERENCES goscie(id) ON DELETE RESTRICT,
 CONSTRAINT fk_rez_pokoj FOREIGN KEY(pokoj_id) REFERENCES pokoje(id) ON DELETE RESTRICT,
 CONSTRAINT fk_rez_prac FOREIGN KEY(pracownik_id) REFERENCES pracownicy(id) ON DELETE RESTRICT,
 CONSTRAINT chk_termin CHECK(data_do>data_od AND DATEDIFF(data_do,data_od)<=365),
 CONSTRAINT chk_osoby CHECK(liczba_osob BETWEEN 1 AND 10),
 CONSTRAINT chk_stawka CHECK(cena_za_noc>0),
 INDEX idx_dostepnosc(pokoj_id,status,data_od,data_do),
 INDEX idx_termin(data_od,data_do)
) ENGINE=InnoDB;
CREATE TABLE platnosci (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, rezerwacja_id BIGINT NOT NULL, pracownik_id BIGINT NOT NULL,
 kwota DECIMAL(10,2) NOT NULL, rodzaj ENUM('WPLATA','ZWROT') NOT NULL,
 metoda ENUM('GOTOWKA','KARTA','PRZELEW') NOT NULL,
 data_platnosci DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_plat_rez FOREIGN KEY(rezerwacja_id) REFERENCES rezerwacje(id) ON DELETE RESTRICT,
 CONSTRAINT fk_plat_prac FOREIGN KEY(pracownik_id) REFERENCES pracownicy(id) ON DELETE RESTRICT,
 CONSTRAINT chk_kwota CHECK(kwota>0), INDEX idx_plat_data(data_platnosci)
) ENGINE=InnoDB;

START TRANSACTION;
SET @dzis=CURDATE();
INSERT INTO pracownicy(id,imie,nazwisko,login,hash_hasla,rola) VALUES
(1,'Anna','Nowak','admin','pbkdf2_sha256$600000$nRCrfCjzpxXsWYGfmp3rig==$VK5AdfVz4k8seDb2Q/sqbbsnvNdG0a/TahDWSGCIrko=','ADMIN'),
(2,'Piotr','Zieliński','recepcja','pbkdf2_sha256$600000$l55XBlKOKwZpnRqYdDaQxQ==$gbUEYr7rFoCBVsypXAsaf+WG0lWxu8WKhVWIRqGFizI=','RECEPCJA');
INSERT INTO goscie(id,imie,nazwisko,telefon,email) VALUES
(1,'Jan','Kowalski','+48 500 100 001','gosc1@example.com'),
(2,'Maria','Wiśniewska','+48 500 100 002','gosc2@example.com'),
(3,'Michał','Wójcik','+48 500 100 003','gosc3@example.com'),
(4,'Julia','Kamińska','+48 500 100 004','gosc4@example.com'),
(5,'Adam','Lewandowski','+48 500 100 005','gosc5@example.com'),
(6,'Zofia','Dąbrowska','+48 500 100 006','gosc6@example.com'),
(7,'Kacper','Woźniak','+48 500 100 007','gosc7@example.com'),
(8,'Alicja','Mazur','+48 500 100 008','gosc8@example.com'),
(9,'Filip','Krawczyk','+48 500 100 009','gosc9@example.com'),
(10,'Oliwia','Piotrowska','+48 500 100 010','gosc10@example.com'),
(11,'Tomasz','Grabowski','+48 500 100 011','gosc11@example.com'),
(12,'Natalia','Pawlak','+48 500 100 012','gosc12@example.com');
INSERT INTO pokoje(id,numer,typ,liczba_miejsc,cena_za_noc,stan) VALUES
(1,'101','Standard',1,180,'GOTOWY'),
(2,'102','Standard',1,190,'GOTOWY'),
(3,'103','Standard',2,210,'GOTOWY'),
(4,'104','Standard',2,240,'GOTOWY'),
(5,'105','Standard',2,250,'GOTOWY'),
(6,'106','Standard',2,260,'GOTOWY'),
(7,'107','Standard',2,280,'GOTOWY'),
(8,'108','Deluxe',2,300,'GOTOWY'),
(9,'109','Deluxe',2,320,'GOTOWY'),
(10,'110','Deluxe',2,330,'GOTOWY'),
(11,'201','Deluxe',2,340,'GOTOWY'),
(12,'202','Deluxe',2,360,'GOTOWY'),
(13,'203','Deluxe',2,380,'GOTOWY'),
(14,'204','Deluxe',2,400,'GOTOWY'),
(15,'205','Deluxe',2,420,'GOTOWY'),
(16,'206','Apartament',4,450,'DO_SPRZATANIA'),
(17,'207','Apartament',4,480,'DO_SPRZATANIA'),
(18,'208','Apartament',4,500,'GOTOWY'),
(19,'209','Apartament',4,550,'GOTOWY'),
(20,'210','Apartament',4,650,'WYLACZONY');
INSERT INTO rezerwacje(id,gosc_id,pokoj_id,pracownik_id,data_od,data_do,liczba_osob,cena_za_noc,status,uwagi) VALUES
(1,1,1,2,DATE_ADD(@dzis, INTERVAL 0 DAY),DATE_ADD(@dzis, INTERVAL 3 DAY),1,180,'POTWIERDZONA','Dzisiejszy przyjazd — przykład do zameldowania'),
(2,2,2,2,DATE_ADD(@dzis, INTERVAL -1 DAY),DATE_ADD(@dzis, INTERVAL 0 DAY),1,190,'ZAMELDOWANA','Dzisiejszy wyjazd — opłacono w całości'),
(3,3,3,2,DATE_ADD(@dzis, INTERVAL -2 DAY),DATE_ADD(@dzis, INTERVAL 2 DAY),2,210,'ZAMELDOWANA','Pozostało do zapłaty 540 zł'),
(4,4,4,2,DATE_ADD(@dzis, INTERVAL 2 DAY),DATE_ADD(@dzis, INTERVAL 5 DAY),2,240,'POTWIERDZONA','Wpłacona zaliczka'),
(5,5,5,2,DATE_ADD(@dzis, INTERVAL 1 DAY),DATE_ADD(@dzis, INTERVAL 4 DAY),2,250,'POTWIERDZONA','Prośba o cichy pokój'),
(6,6,6,2,DATE_ADD(@dzis, INTERVAL -8 DAY),DATE_ADD(@dzis, INTERVAL -5 DAY),2,260,'ZAKONCZONA','Pobyt zakończony'),
(7,7,7,2,DATE_ADD(@dzis, INTERVAL -6 DAY),DATE_ADD(@dzis, INTERVAL -3 DAY),2,280,'ZAKONCZONA','Pobyt zakończony'),
(8,8,8,2,DATE_ADD(@dzis, INTERVAL 7 DAY),DATE_ADD(@dzis, INTERVAL 9 DAY),2,300,'ANULOWANA','Zaliczka zwrócona'),
(9,9,9,2,DATE_ADD(@dzis, INTERVAL 0 DAY),DATE_ADD(@dzis, INTERVAL 2 DAY),2,320,'POTWIERDZONA','Dzisiejszy przyjazd'),
(10,10,10,2,DATE_ADD(@dzis, INTERVAL 4 DAY),DATE_ADD(@dzis, INTERVAL 7 DAY),2,330,'POTWIERDZONA',''),
(11,11,11,2,DATE_ADD(@dzis, INTERVAL -1 DAY),DATE_ADD(@dzis, INTERVAL 1 DAY),2,340,'ZAMELDOWANA','Pobyt opłacony'),
(12,12,12,2,DATE_ADD(@dzis, INTERVAL 3 DAY),DATE_ADD(@dzis, INTERVAL 6 DAY),2,360,'POTWIERDZONA',''),
(13,1,13,2,DATE_ADD(@dzis, INTERVAL -4 DAY),DATE_ADD(@dzis, INTERVAL -1 DAY),2,380,'ZAKONCZONA',''),
(14,2,14,2,DATE_ADD(@dzis, INTERVAL 1 DAY),DATE_ADD(@dzis, INTERVAL 3 DAY),2,400,'POTWIERDZONA',''),
(15,3,15,2,DATE_ADD(@dzis, INTERVAL -3 DAY),DATE_ADD(@dzis, INTERVAL 0 DAY),2,420,'ZAMELDOWANA','Dzisiejszy wyjazd');
INSERT INTO platnosci(id,rezerwacja_id,pracownik_id,kwota,rodzaj,metoda,data_platnosci) VALUES
(1,2,2,190,'WPLATA','KARTA',TIMESTAMP(DATE_ADD(@dzis, INTERVAL -1 DAY),'09:00:00')),
(2,3,2,300,'WPLATA','PRZELEW',TIMESTAMP(DATE_ADD(@dzis, INTERVAL -3 DAY),'09:00:00')),
(3,4,2,200,'WPLATA','PRZELEW',TIMESTAMP(DATE_ADD(@dzis, INTERVAL -1 DAY),'09:00:00')),
(4,6,2,780,'WPLATA','KARTA',TIMESTAMP(DATE_ADD(@dzis, INTERVAL -5 DAY),'09:00:00')),
(5,7,2,840,'WPLATA','GOTOWKA',TIMESTAMP(DATE_ADD(@dzis, INTERVAL -3 DAY),'09:00:00')),
(6,8,2,200,'WPLATA','PRZELEW',TIMESTAMP(DATE_ADD(@dzis, INTERVAL -2 DAY),'09:00:00')),
(7,8,2,200,'ZWROT','PRZELEW',TIMESTAMP(DATE_ADD(@dzis, INTERVAL -1 DAY),'09:00:00')),
(8,9,2,100,'WPLATA','GOTOWKA',TIMESTAMP(DATE_ADD(@dzis, INTERVAL 0 DAY),'09:00:00')),
(9,11,2,680,'WPLATA','KARTA',TIMESTAMP(DATE_ADD(@dzis, INTERVAL -1 DAY),'09:00:00')),
(10,13,2,1140,'WPLATA','KARTA',TIMESTAMP(DATE_ADD(@dzis, INTERVAL -1 DAY),'09:00:00')),
(11,15,2,1260,'WPLATA','PRZELEW',TIMESTAMP(DATE_ADD(@dzis, INTERVAL -3 DAY),'09:00:00'));
COMMIT;

SELECT 'goscie' tabela,COUNT(*) rekordy FROM goscie UNION ALL SELECT 'pokoje',COUNT(*) FROM pokoje UNION ALL SELECT 'pracownicy',COUNT(*) FROM pracownicy UNION ALL SELECT 'rezerwacje',COUNT(*) FROM rezerwacje UNION ALL SELECT 'platnosci',COUNT(*) FROM platnosci;


-- Uruchom jako administrator MySQL po 01_baza_i_dane.sql.
-- Jeżeli te konta już istnieją, wybierz inną nazwę i zmień config/db.properties.
CREATE USER 'hotel_app'@'localhost' IDENTIFIED BY 'HotelDemo2026!';
CREATE USER 'hotel_app'@'127.0.0.1' IDENTIFIED BY 'HotelDemo2026!';
GRANT SELECT,INSERT,UPDATE,DELETE ON hotel_manager.* TO 'hotel_app'@'localhost';
GRANT SELECT,INSERT,UPDATE,DELETE ON hotel_manager.* TO 'hotel_app'@'127.0.0.1';
