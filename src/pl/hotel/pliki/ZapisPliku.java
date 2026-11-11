package pl.hotel.pliki;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public final class ZapisPliku {
    private ZapisPliku() {}

    public static void tekst(Path plik, String tekst) throws IOException {
        Path cel = plik.toAbsolutePath();
        Path tymczasowy = Files.createTempFile(cel.getParent(), ".hotel-eksport-", ".tmp");
        try {
            Files.writeString(tymczasowy, tekst, StandardCharsets.UTF_8);
            try { Files.move(tymczasowy, cel, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) {
                Files.move(tymczasowy, cel, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(tymczasowy); }
    }
}
