package pl.hotel.bezpieczenstwo;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.security.SecureRandom;

public class Hasla {
    public static boolean sprawdz(String wpisaneHaslo, String zapisanyHash) throws GeneralSecurityException{
        if (wpisaneHaslo == null || wpisaneHaslo.length() > 128 || zapisanyHash == null || zapisanyHash.length() > 255){
            return false;
        }
        String[] czesci = zapisanyHash.split("\\$", -1);

        if (czesci.length != 4 || !czesci[0].equals("pbkdf2_sha256")){
            return  false;
        }

        int iteracje;

        byte[] sol;
        byte[] oczekawianyHash;

        try {
            iteracje = Integer.parseInt(czesci[1]);
            sol = Base64.getDecoder().decode(czesci[2]);
            oczekawianyHash = Base64.getDecoder().decode(czesci[3]);
        } catch (IllegalArgumentException e){
            return false;
        }
        if (iteracje < 600_000 || iteracje > 1_000_000 || sol.length < 16 || sol.length > 32 || oczekawianyHash.length != 32){
            return false;
        }
        char[] znakiHasla = wpisaneHaslo.toCharArray();
        PBEKeySpec ustawienia = new PBEKeySpec(
                znakiHasla,
                sol,
                iteracje,
                256
        );

        try {
            SecretKeyFactory generator = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] obliczonyHash = generator.generateSecret(ustawienia).getEncoded();

            return MessageDigest.isEqual(obliczonyHash, oczekawianyHash);
        } finally {
            ustawienia.clearPassword();
            Arrays.fill(znakiHasla, '\0');
        }

    }
    public static String utworzHash(String haslo)throws GeneralSecurityException{
        if (haslo == null || haslo.length() < 12 || haslo.length() > 128) throw new IllegalArgumentException("Hasło musi mieć od 12 do 128 znaków.");
        byte[] sol = new byte[16];
        new SecureRandom().nextBytes(sol);

        int iteracje = 600_000;
        char[] znakiHasla = haslo.toCharArray();

        PBEKeySpec ustawienia = new PBEKeySpec(znakiHasla, sol,iteracje,256);
        try {
            SecretKeyFactory generator = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

            byte[] hash = generator.generateSecret(ustawienia).getEncoded();

            return "pbkdf2_sha256$" + iteracje + "$" + Base64.getEncoder().encodeToString(sol) + "$" + Base64.getEncoder().encodeToString(hash);
        } finally {
            ustawienia.clearPassword();
            Arrays.fill(znakiHasla, '\0');
        }
    }
}
