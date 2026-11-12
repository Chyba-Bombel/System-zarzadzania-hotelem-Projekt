package pl.hotel.baza;

import java.sql.*;

final class ArchiwumGosciDAO {
    private ArchiwumGosciDAO() {}

    static void wymagajAktywnego(Connection c,int id)throws SQLException {
        if(odczytajStan(c,id))throw new SQLException("Gość jest archiwalny. Przywróć kartotekę przed nową rezerwacją.");
    }

    private static boolean odczytajStan(Connection c,int id)throws SQLException {
        if(id<1)throw new SQLException("Podaj poprawne ID gościa.");
        try(PreparedStatement q=c.prepareStatement("SELECT archiwizowany FROM goscie WHERE id=? FOR UPDATE")) {
            q.setInt(1,id);
            try(ResultSet r=q.executeQuery()) {
                if(!r.next())throw new SQLException("Nie znaleziono gościa.");
                return r.getBoolean(1);
            }
        }
    }

    static void zmien(int id,boolean archiwizuj)throws SQLException {
        try(Connection c=PolaczenieBazy.polacz()) {
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            c.setAutoCommit(false);
            try {
                boolean aktualny=odczytajStan(c,id);
                if(aktualny==archiwizuj)throw new SQLException("Stan kartoteki został zmieniony. Odśwież listę gości.");
                if(archiwizuj) {
                    try(PreparedStatement q=c.prepareStatement("SELECT id FROM rezerwacje WHERE gosc_id=? AND status IN ('POTWIERDZONA','ZAMELDOWANA') LIMIT 1")) {
                        q.setInt(1,id);
                        try(ResultSet r=q.executeQuery()) {
                            if(r.next())throw new SQLException("Gość ma aktywne rezerwacje. Najpierw zakończ lub anuluj pobyty.");
                        }
                    }
                }
                String sql=archiwizuj
                    ?"UPDATE goscie SET archiwizowany=TRUE,archiwizowano=CURRENT_TIMESTAMP(6),archiwizowal_pracownik_id=? WHERE id=?"
                    :"UPDATE goscie SET archiwizowany=FALSE,archiwizowano=NULL,archiwizowal_pracownik_id=NULL WHERE id=?";
                try(PreparedStatement q=c.prepareStatement(sql)) {
                    int poz=1;
                    if(archiwizuj)q.setLong(poz++,KontrolaDostepu.identyfikator());
                    q.setInt(poz,id);
                    if(q.executeUpdate()!=1)throw new SQLException("Nie potwierdzono zmiany kartoteki.");
                }
                c.commit();
            }catch(SQLException|RuntimeException e) {
                try{c.rollback();}catch(SQLException b){e.addSuppressed(b);}throw e;
            }
        }
    }
}
