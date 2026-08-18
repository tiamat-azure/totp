package nc.opt.totp;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vecteurs de test officiels de la RFC 6238, annexe B.
 * Les graines sont les chaines ASCII "1234567890..." tronquees a la taille de bloc de chaque algorithme.
 */
class TotpServiceTest {

    private static final String SEED = "12345678901234567890";
    private final TotpService service = new TotpService();

    @ParameterizedTest(name = "T={0} {1} -> {2}")
    @DisplayName("Vecteurs RFC 6238")
    @CsvSource({
            "59,          SHA1,   94287082",
            "59,          SHA256, 46119246",
            "59,          SHA512, 90693936",
            "1111111109,  SHA1,   07081804",
            "1111111109,  SHA256, 68084774",
            "1111111109,  SHA512, 25091201",
            "1111111111,  SHA1,   14050471",
            "1111111111,  SHA256, 67062674",
            "1111111111,  SHA512, 99943326",
            "1234567890,  SHA1,   89005924",
            "1234567890,  SHA256, 91819424",
            "1234567890,  SHA512, 93441116",
            "2000000000,  SHA1,   69279037",
            "2000000000,  SHA256, 90698825",
            "2000000000,  SHA512, 38618901",
            "20000000000, SHA1,   65353130",
            "20000000000, SHA256, 77737706",
            "20000000000, SHA512, 47863826",
    })
    void generatesRfcVectors(long epochSecond, String algorithm, String expected) {
        TotpConfig config = new TotpConfig(seedFor(algorithm), algorithm, 8, 30);

        assertThat(service.generate(config, Instant.ofEpochSecond(epochSecond))).isEqualTo(expected);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("La validite s'aligne sur la fin de la fenetre de temps")
    void computesValidUntil() {
        TotpConfig config = new TotpConfig(seedFor("SHA1"), "SHA1", 6, 30);

        assertThat(service.validUntil(config, Instant.ofEpochSecond(65)))
                .isEqualTo(Instant.ofEpochSecond(90));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Le code reste stable sur toute la fenetre puis change")
    void codeIsStableWithinWindow() {
        TotpConfig config = new TotpConfig(seedFor("SHA1"), "SHA1", 6, 30);

        String atStart = service.generate(config, Instant.ofEpochSecond(60));
        String atEnd = service.generate(config, Instant.ofEpochSecond(89));
        String next = service.generate(config, Instant.ofEpochSecond(90));

        assertThat(atStart).isEqualTo(atEnd).hasSize(6);
        assertThat(next).isNotEqualTo(atStart);
    }

    /** La graine est repetee puis tronquee a la taille de cle attendue par l'algorithme. */
    private static String seedFor(String algorithm) {
        int length = switch (algorithm) {
            case "SHA256" -> 32;
            case "SHA512" -> 64;
            default -> 20;
        };
        String ascii = SEED.repeat(length / SEED.length() + 1).substring(0, length);
        return Base32.encode(ascii.getBytes(StandardCharsets.US_ASCII));
    }
}
