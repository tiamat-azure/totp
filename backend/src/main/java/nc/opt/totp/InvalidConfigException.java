package nc.opt.totp;

/** Configuration TOTP refusee : se traduit par une reponse 400 explicite. */
public class InvalidConfigException extends RuntimeException {

    private final String code;

    public InvalidConfigException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
