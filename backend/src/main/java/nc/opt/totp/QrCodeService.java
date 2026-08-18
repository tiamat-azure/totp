package nc.opt.totp;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Produit le QR code otpauth:// de la configuration courante, avec le logo OPT-NC incruste au centre.
 *
 * <p>Le niveau de correction d'erreur H tolere la perte de 30 % des modules ; le logo n'en occulte
 * qu'environ 5 %, ce qui laisse une marge confortable.
 */
@Service
public class QrCodeService {

    private static final int SIZE = 512;
    private static final int MARGIN = 2;
    private static final double LOGO_RATIO = 0.27; // part de la largeur occupee par le logo
    private static final int LOGO_PADDING = 8;     // reserve blanche autour du logo, en pixels
    private static final String LOGO_RESOURCE = "/static/opt-cagou.png";

    private final String issuer;
    private final String accountName;

    public QrCodeService(@Value("${totp.issuer:OPT-NC}") String issuer,
                         @Value("${totp.account-name:demo}") String accountName) {
        this.issuer = issuer;
        this.accountName = accountName;
    }

    /** URI otpauth:// standard, scannable par n'importe quelle application d'authentification. */
    public String otpauthUri(TotpConfig config) {
        String label = encode(issuer) + ":" + encode(accountName);
        return "otpauth://totp/" + label
                + "?secret=" + config.secret()
                + "&issuer=" + encode(issuer)
                + "&algorithm=" + config.algorithm()
                + "&digits=" + config.digits()
                + "&period=" + config.period();
    }

    /** PNG du QR code, logo compris. */
    public byte[] pngFor(TotpConfig config) {
        try {
            BufferedImage qr = encodeQr(otpauthUri(config));
            drawLogo(qr);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(qr, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Generation du QR code impossible", e);
        }
    }

    private BufferedImage encodeQr(String uri) throws WriterException {
        Map<EncodeHintType, Object> hints = Map.of(
                EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H,
                EncodeHintType.MARGIN, MARGIN,
                EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());

        return MatrixToImageWriter.toBufferedImage(
                new QRCodeWriter().encode(uri, BarcodeFormat.QR_CODE, SIZE, SIZE, hints));
    }

    private void drawLogo(BufferedImage qr) throws IOException {
        BufferedImage logo = readLogo();
        if (logo == null) {
            return; // sans logo, le QR reste parfaitement valide
        }

        int width = (int) (qr.getWidth() * LOGO_RATIO);
        int height = width * logo.getHeight() / logo.getWidth();
        int x = (qr.getWidth() - width) / 2;
        int y = (qr.getHeight() - height) / 2;

        Graphics2D g = qr.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRoundRect(x - LOGO_PADDING, y - LOGO_PADDING,
                    width + 2 * LOGO_PADDING, height + 2 * LOGO_PADDING, 16, 16);
            g.drawImage(logo, x, y, width, height, null);
        } finally {
            g.dispose();
        }
    }

    private BufferedImage readLogo() throws IOException {
        try (InputStream in = getClass().getResourceAsStream(LOGO_RESOURCE)) {
            return in == null ? null : ImageIO.read(in);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
