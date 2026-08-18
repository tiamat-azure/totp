package nc.opt.totp;

import java.time.Duration;
import java.time.Instant;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API de la demonstration TOTP. */
@RestController
@RequestMapping("/api")
public class TotpController {

    private final TotpConfigStore store;
    private final TotpService totpService;
    private final QrCodeService qrCodeService;

    public TotpController(TotpConfigStore store, TotpService totpService, QrCodeService qrCodeService) {
        this.store = store;
        this.totpService = totpService;
        this.qrCodeService = qrCodeService;
    }

    /** Code courant, accompagne de quoi piloter le compte a rebours cote front. */
    @GetMapping("/totp")
    public TotpResponse totp() {
        TotpConfig config = store.current();
        Instant now = Instant.now();
        Instant validUntil = totpService.validUntil(config, now);

        return new TotpResponse(
                totpService.generate(config, now),
                config.algorithm(),
                config.digits(),
                config.period(),
                Duration.between(now, validUntil).toSeconds(),
                validUntil,
                now);
    }

    /**
     * Configuration courante, secret en clair inclus.
     * Reserve a la demonstration (infobulle de survol) : a retirer pour un usage reel.
     */
    @GetMapping("/config")
    public ConfigResponse config() {
        return toResponse(store.current());
    }

    /** Remplace le secret et, optionnellement, les parametres de calcul. */
    @PutMapping("/config")
    public ConfigResponse updateConfig(@RequestBody ConfigRequest request) {
        return toResponse(
                store.replace(request.secret(), request.algorithm(), request.digits(), request.period()));
    }

    /** Genere un nouveau secret aleatoire et l'applique immediatement. */
    @PostMapping("/secret/random")
    public ConfigResponse randomSecret() {
        return toResponse(store.renewSecret());
    }

    /** QR code otpauth:// de la configuration courante, logo OPT-NC au centre. */
    @GetMapping(value = "/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qrCode() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(qrCodeService.pngFor(store.current()));
    }

    public record TotpResponse(String code, String algorithm, int digits, int period,
                               long remainingSeconds, Instant validUntil, Instant serverTime) {
    }

    public record ConfigRequest(String secret, String algorithm, Integer digits, Integer period) {
    }

    public record ConfigResponse(String secret, String algorithm, int digits, int period, String otpauthUri) {
    }

    private ConfigResponse toResponse(TotpConfig config) {
        return new ConfigResponse(config.secret(), config.algorithm(), config.digits(), config.period(),
                qrCodeService.otpauthUri(config));
    }
}
