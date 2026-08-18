package nc.opt.totp;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

/**
 * Calcul du code TOTP selon la RFC 6238 (qui s'appuie sur le HOTP de la RFC 4226).
 * Aucune librairie tierce : le JDK fournit tout le necessaire via javax.crypto.Mac.
 */
@Service
public class TotpService {

    /** Code valide pour l'instant donne. */
    public String generate(TotpConfig config, Instant now) {
        long counter = Math.floorDiv(now.getEpochSecond(), config.period());
        return generateForCounter(config, counter);
    }

    /** Instant d'expiration du code courant. */
    public Instant validUntil(TotpConfig config, Instant now) {
        long counter = Math.floorDiv(now.getEpochSecond(), config.period());
        return Instant.ofEpochSecond((counter + 1) * config.period());
    }

    String generateForCounter(TotpConfig config, long counter) {
        byte[] message = ByteBuffer.allocate(Long.BYTES).putLong(counter).array();
        byte[] hash = hmac(config.algorithm(), config.key(), message);

        // Dynamic truncation, RFC 4226 section 5.3
        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);

        int modulo = (int) Math.pow(10, config.digits());
        return String.format("%0" + config.digits() + "d", binary % modulo);
    }

    private byte[] hmac(String algorithm, byte[] key, byte[] message) {
        String macAlgorithm = "Hmac" + algorithm;
        try {
            Mac mac = Mac.getInstance(macAlgorithm);
            mac.init(new SecretKeySpec(key, macAlgorithm));
            return mac.doFinal(message);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Calcul HMAC impossible : " + macAlgorithm, e);
        }
    }
}
