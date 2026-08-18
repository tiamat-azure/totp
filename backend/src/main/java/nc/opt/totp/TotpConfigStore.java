package nc.opt.totp;

import java.security.SecureRandom;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Component;

/**
 * Detient la configuration TOTP courante, en memoire uniquement.
 * Un seul secret global, perdu au redemarrage : choix assume pour cette demonstration.
 */
@Component
public class TotpConfigStore {

    private static final Set<String> ALGORITHMS = Set.of("SHA1", "SHA256", "SHA512");
    private static final Set<Integer> DIGITS = Set.of(6, 8);
    private static final int MIN_PERIOD = 10;
    private static final int MAX_PERIOD = 120;
    private static final int MIN_SECRET_LENGTH = 16;
    private static final int RANDOM_SECRET_BYTES = 20; // 160 bits, recommandation RFC 4226

    private final SecureRandom random = new SecureRandom();
    private final AtomicReference<TotpConfig> current = new AtomicReference<>();

    /** Un secret est disponible des le demarrage : l'ecran d'appairage a toujours un QR a afficher. */
    @PostConstruct
    void initialise() {
        current.set(new TotpConfig(
                randomSecret(),
                TotpConfig.DEFAULT_ALGORITHM,
                TotpConfig.DEFAULT_DIGITS,
                TotpConfig.DEFAULT_PERIOD));
    }

    public TotpConfig current() {
        return current.get();
    }

    /** Remplace la configuration apres validation. Les parametres nuls prennent leur valeur par defaut. */
    public TotpConfig replace(String secret, String algorithm, Integer digits, Integer period) {
        TotpConfig config = new TotpConfig(
                validateSecret(secret),
                validateAlgorithm(algorithm),
                validateDigits(digits),
                validatePeriod(period));
        current.set(config);
        return config;
    }

    /** Genere un nouveau secret aleatoire et l'applique, en conservant les autres parametres. */
    public TotpConfig renewSecret() {
        TotpConfig previous = current.get();
        TotpConfig config = new TotpConfig(
                randomSecret(), previous.algorithm(), previous.digits(), previous.period());
        current.set(config);
        return config;
    }

    private String randomSecret() {
        byte[] bytes = new byte[RANDOM_SECRET_BYTES];
        random.nextBytes(bytes);
        return Base32.encode(bytes);
    }

    private String validateSecret(String secret) {
        String cleaned = secret == null ? "" : secret.replaceAll("\\s", "").replace("=", "").toUpperCase();
        if (cleaned.length() < MIN_SECRET_LENGTH || !cleaned.matches("[A-Z2-7]+")) {
            throw new InvalidConfigException("INVALID_SECRET",
                    "Le secret doit etre en Base32 (A-Z, 2-7), " + MIN_SECRET_LENGTH + " caracteres minimum.");
        }
        return cleaned;
    }

    private String validateAlgorithm(String algorithm) {
        if (algorithm == null || algorithm.isBlank()) {
            return TotpConfig.DEFAULT_ALGORITHM;
        }
        String upper = algorithm.toUpperCase();
        if (!ALGORITHMS.contains(upper)) {
            throw new InvalidConfigException("INVALID_ALGORITHM",
                    "Algorithme attendu parmi " + ALGORITHMS + ".");
        }
        return upper;
    }

    private int validateDigits(Integer digits) {
        if (digits == null) {
            return TotpConfig.DEFAULT_DIGITS;
        }
        if (!DIGITS.contains(digits)) {
            throw new InvalidConfigException("INVALID_DIGITS", "Le nombre de chiffres doit valoir 6 ou 8.");
        }
        return digits;
    }

    private int validatePeriod(Integer period) {
        if (period == null) {
            return TotpConfig.DEFAULT_PERIOD;
        }
        if (period < MIN_PERIOD || period > MAX_PERIOD) {
            throw new InvalidConfigException("INVALID_PERIOD",
                    "La periode doit etre comprise entre " + MIN_PERIOD + " et " + MAX_PERIOD + " secondes.");
        }
        return period;
    }
}
