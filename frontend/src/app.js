/* Demo TOTP OPT-NC - JavaScript sans dependance. */
(() => {
  'use strict';

  const API = '/api';
  const RING_CIRCUMFERENCE = 2 * Math.PI * 20;
  const TICK_MS = 200;

  const el = (id) => document.getElementById(id);
  const dom = {
    themeToggle: el('theme-toggle'),
    pairing: el('screen-pairing'),
    codeScreen: el('screen-code'),
    qrImage: el('qr-image'),
    secretValue: el('secret-value'),
    seeCode: el('btn-see-code'),
    regeneratePairing: el('btn-regenerate-pairing'),
    regenerateCode: el('btn-regenerate-code'),
    back: el('btn-back'),
    code: el('code'),
    remaining: el('remaining'),
    ring: el('ring-progress'),
    form: el('config-form'),
    secretInput: el('secret-input'),
    algorithm: el('algorithm'),
    digits: el('digits'),
    period: el('period'),
    feedback: el('feedback'),
  };

  /** Ecart entre l'horloge du navigateur et celle du serveur, en millisecondes. */
  let clockSkew = 0;
  let expiresAt = 0;
  let period = 30;
  let ticker = null;

  // --- Theme ---------------------------------------------------------------

  function applyTheme(theme) {
    document.documentElement.dataset.theme = theme;
    // L'icone annonce la destination de la bascule.
    dom.themeToggle.textContent = theme === 'dark' ? '☀️' : '🌙';
    localStorage.setItem('theme', theme);
  }

  function initTheme() {
    const stored = localStorage.getItem('theme');
    const preferred = window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark';
    applyTheme(stored ?? preferred);
  }

  // --- Appels API ----------------------------------------------------------

  async function call(path, options) {
    const response = await fetch(API + path, options);
    const body = response.headers.get('content-type')?.includes('json')
      ? await response.json()
      : null;

    if (!response.ok) {
      throw new Error(body?.message ?? `Erreur ${response.status}`);
    }
    return body;
  }

  const getConfig = () => call('/config');
  const getTotp = () => call('/totp');
  const randomSecret = () => call('/secret/random', { method: 'POST' });

  const putConfig = (payload) => call('/config', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });

  // --- Ecran 1 : appairage -------------------------------------------------

  function showPairing(config) {
    dom.secretValue.textContent = config.secret;
    dom.secretInput.value = config.secret;
    dom.algorithm.value = config.algorithm;
    dom.digits.value = String(config.digits);
    dom.period.value = String(config.period);
    // Le parametre de version force le rechargement de l'image apres changement de secret.
    dom.qrImage.src = `${API}/qrcode?v=${Date.now()}`;
  }

  // --- Ecran 2 : code et compte a rebours ----------------------------------

  const now = () => Date.now() + clockSkew;

  async function refreshCode() {
    const totp = await getTotp();

    clockSkew = new Date(totp.serverTime).getTime() - Date.now();
    expiresAt = new Date(totp.validUntil).getTime();
    period = totp.period;

    dom.code.textContent = totp.code;
    tick();
  }

  function tick() {
    const remainingMs = expiresAt - now();

    if (remainingMs <= 0) {
      refreshCode().catch(reportError);
      return;
    }

    const seconds = Math.ceil(remainingMs / 1000);
    dom.remaining.textContent = String(seconds);
    dom.code.classList.toggle('expiring', seconds <= 5);
    dom.ring.style.strokeDashoffset =
      RING_CIRCUMFERENCE * (1 - remainingMs / (period * 1000));
  }

  function startTicking() {
    stopTicking();
    ticker = setInterval(tick, TICK_MS);
  }

  function stopTicking() {
    if (ticker) {
      clearInterval(ticker);
      ticker = null;
    }
  }

  // --- Navigation ----------------------------------------------------------

  async function goToPairing() {
    stopTicking();
    dom.codeScreen.hidden = true;
    dom.pairing.hidden = false;
    showPairing(await getConfig());
  }

  async function goToCode() {
    dom.pairing.hidden = true;
    dom.codeScreen.hidden = false;
    await refreshCode();
    startTicking();
  }

  // --- Retours utilisateur -------------------------------------------------

  function notify(message, kind = '') {
    dom.feedback.textContent = message;
    dom.feedback.className = `feedback ${kind}`.trim();
  }

  function reportError(error) {
    notify(error.message, 'error');
  }

  /** Un nouveau secret impose de re-scanner : retour force sur l'ecran d'appairage. */
  async function regenerate() {
    try {
      await randomSecret();
      await goToPairing();
    } catch (error) {
      reportError(error);
    }
  }

  async function applyConfig(event) {
    event.preventDefault();
    try {
      await putConfig({
        secret: dom.secretInput.value.trim(),
        algorithm: dom.algorithm.value,
        digits: Number(dom.digits.value),
        period: Number(dom.period.value),
      });
      await refreshCode();
      notify('Configuration appliquée.', 'ok');
    } catch (error) {
      reportError(error);
    }
  }

  // --- Demarrage -----------------------------------------------------------

  dom.themeToggle.addEventListener('click', () => {
    applyTheme(document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark');
  });
  dom.seeCode.addEventListener('click', () => goToCode().catch(reportError));
  dom.back.addEventListener('click', () => goToPairing().catch(reportError));
  dom.regeneratePairing.addEventListener('click', regenerate);
  dom.regenerateCode.addEventListener('click', regenerate);
  dom.form.addEventListener('submit', applyConfig);

  // Les navigateurs ralentissent les minuteurs des onglets en arriere-plan : on resynchronise.
  document.addEventListener('visibilitychange', () => {
    if (!document.hidden && !dom.codeScreen.hidden) {
      refreshCode().catch(reportError);
    }
  });

  initTheme();
  goToPairing().catch(reportError);
})();
