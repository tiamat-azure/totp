package nc.opt.totp;

/**
 * Configuration TOTP courante : la cle secrete et les parametres de l'algorithme.
 * Immuable : toute modification produit une nouvelle instance.
 *
 * @param secret    secret en Base32, tel que saisi ou genere
 * @param algorithm SHA1, SHA256 ou SHA512
 * @param digits    nombre de chiffres du code (6 ou 8)
 * @param period    duree de validite d'un code, en secondes
 */
public record TotpConfig(String secret, String algorithm, int digits, int period) {

    public static final String DEFAULT_ALGORITHM = "SHA1";
    public static final int DEFAULT_DIGITS = 6;
    public static final int DEFAULT_PERIOD = 30;

    /** Cle binaire derivee du secret Base32. */
    public byte[] key() {
        return Base32.decode(secret);
    }
}
