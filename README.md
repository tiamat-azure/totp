# Démo TOTP - OPT-NC

Générateur de codes TOTP (RFC 6238) : back-end Spring Boot et front-end statique, chacun
dans son conteneur, orchestrés par Docker Compose.

## Démarrage

```sh
make build
make start
```

Le front est servi sur <http://localhost:8080>. Le back n'est pas publié sur l'hôte :
nginx relaie `/api` vers `backend:8081` sur le réseau interne, ce qui évite toute
configuration CORS.

| Commande       | Effet                         |
| -------------- | ----------------------------- |
| `make build`   | Construit les images          |
| `make start`   | Démarre la pile               |
| `make stop`    | Arrête la pile                |
| `make restart` | Redémarre la pile             |
| `make status`  | État des conteneurs           |
| `make logs`    | Suit les journaux             |
| `make test`    | Exécute les tests du back-end |

## Parcours

1. **Appairage** - un secret aléatoire de 160 bits est généré au démarrage du back. La
   page d'accueil affiche son QR code `otpauth://`, avec le cagou OPT-NC incrusté au
   centre. Le survol du QR code révèle le secret en Base32.
1. **Code** - le code courant, un compte à rebours circulaire qui déclenche le
   rafraîchissement à zéro, et le formulaire de configuration (secret, algorithme, nombre
   de chiffres, période).

« Générer » produit un nouveau secret et ramène à l'écran d'appairage, puisqu'il faut
re-scanner.

## API

| Méthode | Chemin               | Réponse                                                                       |
| ------- | -------------------- | ----------------------------------------------------------------------------- |
| `GET`   | `/api/totp`          | `{code, algorithm, digits, period, remainingSeconds, validUntil, serverTime}` |
| `GET`   | `/api/config`        | `{secret, algorithm, digits, period, otpauthUri}`                             |
| `PUT`   | `/api/config`        | Corps `{secret, algorithm?, digits?, period?}` - `400` si invalide            |
| `POST`  | `/api/secret/random` | Nouveau secret aléatoire, appliqué immédiatement                              |
| `GET`   | `/api/qrcode`        | `image/png` du QR code otpauth                                                |

Le secret est une chaîne Base32 (RFC 4648, alphabet `A-Z2-7`, sans padding, 16 caractères
minimum), format attendu par toutes les applications d'authentification.

Contraintes : algorithme `SHA1` / `SHA256` / `SHA512`, 6 ou 8 chiffres, période de 10 à
120 secondes.

## Compte à rebours et dérive d'horloge

`GET /api/totp` renvoie `serverTime` et `validUntil`. Le front en déduit une fois l'écart
avec son horloge locale, puis décompte toutes les 200 ms à partir de `Date.now()` corrigé.
Un onglet remis au premier plan resynchronise via `visibilitychange`, les navigateurs
ralentissant les minuteurs en arrière-plan.

## Structure

```
.
├── Makefile
├── docker-compose.yml
├── backend/            Spring Boot 3.3, Java 21, ZXing
│   └── src/main/java/nc/opt/totp/
│       ├── Base32.java            Encodage RFC 4648
│       ├── TotpService.java       Calcul RFC 6238 / 4226
│       ├── TotpConfigStore.java   Configuration courante, en mémoire
│       ├── QrCodeService.java     QR code otpauth + logo
│       └── TotpController.java    API REST
└── frontend/           HTML/CSS/JS sans dépendance, servi par nginx
    └── src/
        ├── index.html
        ├── style.css
        └── app.js
```

Les tests couvrent les vecteurs officiels de la RFC 6238 (SHA1, SHA256, SHA512) et de la
RFC 4648.

## Limites assumées

Il s'agit d'une démonstration :

- secret unique et global, conservé en mémoire, perdu au redémarrage ;
- `GET /api/config` expose le secret en clair, nécessaire à l'infobulle de survol - à
  retirer pour un usage réel ;
- échanges en HTTP, sans authentification ; en production, HTTPS et contrôle d'accès
  obligatoires ;
- Google Authenticator ignore `SHA256` / `SHA512` et retombe sur `SHA1`.
