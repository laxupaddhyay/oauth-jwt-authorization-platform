(() => {
  const $ = (id) => document.getElementById(id);
  const issuer = $('issuer');
  const clientInput = $('client-id');
  const redirectInput = $('redirect-uri');
  const apiInput = $('api-url');
  const statusBox = $('status');
  const sessionContent = $('session-content');
  const pill = $('connection-pill');
  let accessToken = null;

  const redirectUri = `${window.location.origin}${window.location.pathname}`;
  redirectInput.value = redirectUri;

  const randomString = (length = 64) => {
    const bytes = new Uint8Array(length);
    crypto.getRandomValues(bytes);
    return Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('');
  };
  const base64Url = (bytes) => btoa(String.fromCharCode(...new Uint8Array(bytes)))
    .replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
  const challengeFor = async (verifier) => base64Url(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verifier)));
  const cleanIssuer = () => issuer.value.trim().replace(/\/+$/, '');
  const selectedScopes = () => Array.from(document.querySelectorAll('input[name="scope"]:checked'), (input) => input.value);

  function notify(message) {
    const toast = $('toast');
    toast.textContent = message;
    toast.hidden = false;
    window.setTimeout(() => { toast.hidden = true; }, 6500);
  }

  function showStatus(title, message) {
    statusBox.hidden = false;
    statusBox.replaceChildren();
    const icon = document.createElement('span');
    icon.className = 'status-icon';
    icon.setAttribute('aria-hidden', 'true');
    icon.textContent = '↗';
    const content = document.createElement('div');
    const heading = document.createElement('strong');
    heading.textContent = title;
    const description = document.createElement('p');
    description.textContent = message;
    content.append(heading, description);
    statusBox.append(icon, content);
  }

  async function callUserInfo(token, provider) {
    const response = await fetch(`${provider}/userinfo`, { headers: { Authorization: `Bearer ${token}` } });
    if (!response.ok) throw new Error(`User information request failed (${response.status}).`);
    return response.json();
  }

  async function callProtectedApi(token) {
    const endpoint = apiInput.value.trim();
    if (!endpoint) return 'No resource API URL configured.';
    const response = await fetch(endpoint, { headers: { Authorization: `Bearer ${token}` } });
    const body = await response.text();
    if (!response.ok) throw new Error(`Protected API responded ${response.status}: ${body || response.statusText}`);
    try { return JSON.stringify(JSON.parse(body), null, 2); } catch { return body; }
  }

  async function completeLogin() {
    const params = new URLSearchParams(window.location.search);
    const error = params.get('error');
    if (error) {
      const details = params.get('error_description');
      history.replaceState({}, document.title, redirectUri);
      showStatus('Sign-in was not completed', details || 'The authorization server declined the request.');
      notify(details || 'Authorization was declined.');
      return;
    }
    const code = params.get('code');
    if (!code) return;

    const expectedState = sessionStorage.getItem('pkce_state');
    const receivedState = params.get('state');
    const verifier = sessionStorage.getItem('pkce_verifier');
    const provider = sessionStorage.getItem('pkce_issuer');
    const clientId = sessionStorage.getItem('pkce_client_id');
    const savedRedirect = sessionStorage.getItem('pkce_redirect_uri');
    history.replaceState({}, document.title, redirectUri);

    if (!expectedState || !receivedState || receivedState !== expectedState || !verifier || !provider || !clientId || !savedRedirect) {
      sessionStorage.removeItem('pkce_verifier');
      sessionStorage.removeItem('pkce_state');
      showStatus('Could not verify this sign-in', 'The one-time state did not match or this browser session expired. Start again.');
      return;
    }

    showStatus('Finishing secure sign-in', 'Exchanging the one-time authorization code…');
    try {
      const response = await fetch(`${provider}/oauth2/token`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({
          grant_type: 'authorization_code',
          client_id: clientId,
          redirect_uri: savedRedirect,
          code,
          code_verifier: verifier
        })
      });
      const result = await response.json();
      if (!response.ok) throw new Error(result.error_description || result.error || `Token exchange failed (${response.status}).`);
      if (!result.access_token) throw new Error('The server response did not contain an access token.');
      accessToken = result.access_token;
      sessionStorage.removeItem('pkce_verifier');
      sessionStorage.removeItem('pkce_state');
      $('granted-scopes').textContent = result.scope || '—';
      const expiry = result.expires_in ? new Date(Date.now() + Number(result.expires_in) * 1000) : null;
      $('expires-at').textContent = expiry ? expiry.toLocaleTimeString() : 'Not provided';
      $('session-content').hidden = false;
      $('status').hidden = true;
      pill.classList.add('connected');
      pill.querySelector('span').textContent = 'Connected';

      try {
        if ((result.scope || '').split(/\s+/).includes('openid')) {
          const profile = await callUserInfo(accessToken, provider);
          $('user-name').textContent = profile.name || profile.preferred_username || profile.sub || 'Signed-in user';
          $('user-email').textContent = profile.email || 'Signed in with Northstar Identity';
          $('avatar').textContent = (profile.name || profile.preferred_username || profile.email || 'N').trim().charAt(0).toUpperCase();
        }
        $('api-output').textContent = await callProtectedApi(accessToken);
      } catch (error) {
        $('api-output').textContent = error.message;
      }
    } catch (error) {
      showStatus('Could not finish sign-in', error.message);
      notify(error.message);
      sessionStorage.removeItem('pkce_verifier');
      sessionStorage.removeItem('pkce_state');
    }
  }

  $('settings-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    if (!window.isSecureContext && location.hostname !== 'localhost' && location.hostname !== '127.0.0.1') {
      notify('The SPA needs HTTPS to use the browser cryptography API.');
      return;
    }
    const provider = cleanIssuer();
    const clientId = clientInput.value.trim();
    const redirect = redirectUri;
    const scopes = selectedScopes();
    if (!scopes.length) {
      notify('Choose at least one permission to request.');
      return;
    }
    if (!provider || !clientId) {
      notify('Enter the authorization server URL and public client ID.');
      return;
    }
    const verifier = randomString();
    const state = randomString(24);
    const nonce = randomString(24);
    sessionStorage.setItem('pkce_verifier', verifier);
    sessionStorage.setItem('pkce_state', state);
    sessionStorage.setItem('pkce_issuer', provider);
    sessionStorage.setItem('pkce_client_id', clientId);
    sessionStorage.setItem('pkce_redirect_uri', redirect);
    try {
      const challenge = await challengeFor(verifier);
      const authorize = new URL(`${provider}/oauth2/authorize`);
      authorize.search = new URLSearchParams({
        response_type: 'code', client_id: clientId, redirect_uri: redirect,
        scope: scopes.join(' '), state, code_challenge: challenge,
        code_challenge_method: 'S256', nonce
      }).toString();
      window.location.assign(authorize);
    } catch (error) {
      notify(`Could not start secure sign-in: ${error.message}`);
    }
  });

  $('disconnect').addEventListener('click', () => {
    accessToken = null;
    sessionStorage.removeItem('pkce_issuer');
    sessionStorage.removeItem('pkce_client_id');
    sessionStorage.removeItem('pkce_redirect_uri');
    sessionContent.hidden = true;
    pill.classList.remove('connected');
    pill.querySelector('span').textContent = 'Not connected';
    showStatus('Ready when you are', 'Start sign-in to continue at your authorization server.');
  });

  completeLogin();
})();

