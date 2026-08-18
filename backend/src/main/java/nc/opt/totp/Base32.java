package nc.opt.totp;

/**
 * Encodage / decodage Base32 selon la RFC 4648, sans padding.
 * C'est le format attendu par les applications d'authentification et les URI otpauth://.
 */
public final class Base32 {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private Base32() {
    }

    /** Decode une chaine Base32 (insensible a la casse, padding et espaces toleres). */
    public static byte[] decode(String base32) {
        String cleaned = base32.replace("=", "").replaceAll("\\s", "").toUpperCase();
        if (cleaned.isEmpty()) {
            return new byte[0];
        }

        byte[] out = new byte[cleaned.length() * 5 / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;

        for (char c : cleaned.toCharArray()) {
            int value = ALPHABET.indexOf(c);
            if (value < 0) {
                throw new IllegalArgumentException("Caractere Base32 invalide : " + c);
            }
            buffer = (buffer << 5) | value;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bitsLeft -= 8;
                out[index++] = (byte) (buffer >> bitsLeft);
            }
        }
        return out;
    }

    /** Encode des octets en Base32 sans padding. */
    public static String encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;

        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                bitsLeft -= 5;
                sb.append(ALPHABET.charAt((buffer >> bitsLeft) & 0x1F));
            }
        }
        if (bitsLeft > 0) {
            sb.append(ALPHABET.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        }
        return sb.toString();
    }
}
