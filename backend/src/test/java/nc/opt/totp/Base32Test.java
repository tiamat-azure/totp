package nc.opt.totp;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Vecteurs de test de la RFC 4648, section 10 (sans padding). */
class Base32Test {

    @ParameterizedTest(name = "\"{0}\" <-> {1}")
    @CsvSource({
            "f,      MY",
            "fo,     MZXQ",
            "foo,    MZXW6",
            "foob,   MZXW6YQ",
            "fooba,  MZXW6YTB",
            "foobar, MZXW6YTBOI",
    })
    void encodesAndDecodes(String plain, String base32) {
        byte[] bytes = plain.getBytes(StandardCharsets.US_ASCII);

        assertThat(Base32.encode(bytes)).isEqualTo(base32);
        assertThat(new String(Base32.decode(base32), StandardCharsets.US_ASCII)).isEqualTo(plain);
    }

    @Test
    void toleratesPaddingCaseAndSpaces() {
        assertThat(Base32.decode("mzxw 6ytb oi===")).isEqualTo("foobar".getBytes(StandardCharsets.US_ASCII));
    }

    @Test
    void rejectsInvalidCharacter() {
        assertThatThrownBy(() -> Base32.decode("MZXW1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1");
    }
}
