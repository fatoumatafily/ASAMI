const $ = id => document.getElementById(id);
const API_BASE = (window.ASAMI_API_URL || '').replace(/\/$/, '');
let loginMode = false;
let dashboard;
let signupData = {};

async function api(url, options = {}) {
  const response = await fetch(API_BASE + url, {
    headers: {'Content-Type': 'application/json'},
    credentials: 'include',
    ...options
  });
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.detail || 'Une erreur est survenue');
  }
  return response.status === 204 ? null : response.json();
}

function showApp(authenticated) {
  $('auth').classList.toggle('hidden', authenticated);
  $('dashboard').classList.toggle('hidden', !authenticated);
  $('logout').classList.toggle('hidden', !authenticated);
}

$('toggle').onclick = event => {
  event.preventDefault();
  loginMode = !loginMode;
  $('business').classList.toggle('hidden', loginMode);
  $('business').required = !loginMode;
  $('authTitle').textContent = loginMode ? 'Connexion' : 'Créer mon espace';
  $('toggle').textContent = loginMode
    ? 'Créer un compte'
    : 'J’ai déjà un compte';
};

$('authForm').onsubmit = async event => {
  event.preventDefault();
  $('authError').textContent = '';
  try {
    await api('/api/auth/' + (loginMode ? 'login' : 'register'), {
      method: 'POST',
      body: JSON.stringify({
        businessName: $('business').value,
        email: $('email').value,
        password: $('password').value
      })
    });
    await load();
  } catch (error) {
    $('authError').textContent = error.message;
  }
};

async function load() {
  dashboard = await api('/api/dashboard');
  showApp(true);
  $('welcome').textContent = 'Bonjour, ' + dashboard.businessName;
  const connected = dashboard.connectionStatus === 'CONNECTED';
  $('status').textContent = connected
    ? 'WhatsApp connecté' + (dashboard.displayPhone ? ' · ' + dashboard.displayPhone : '')
    : 'WhatsApp à connecter';
  $('status').classList.toggle('connected', connected);
  $('metaConnect').textContent = connected
    ? 'Reconnecter avec Facebook / Meta'
    : 'Continuer avec Facebook / Meta';
  initMeta();
  await loadProducts();
}

function initMeta() {
  if (!dashboard.metaAppId || !dashboard.metaConfigurationId) {
    $('metaResult').textContent =
      'Embedded Signup sera disponible après validation Meta. Utilisez le numéro de test maintenant.';
    $('metaConnect').classList.add('hidden');
    $('testConnect').classList.remove('hidden');
    return;
  }
  $('metaConnect').classList.remove('hidden');
  $('testConnect').classList.add('hidden');
  window.fbAsyncInit = () => FB.init({
    appId: dashboard.metaAppId,
    cookie: true,
    xfbml: false,
    version: dashboard.metaApiVersion || 'v25.0'
  });
  if (!document.getElementById('facebook-jssdk')) {
    const script = document.createElement('script');
    script.id = 'facebook-jssdk';
    script.src = 'https://connect.facebook.net/fr_FR/sdk.js';
    script.async = true;
    script.defer = true;
    document.body.appendChild(script);
  }
}

window.addEventListener('message', event => {
  if (!['https://www.facebook.com', 'https://web.facebook.com'].includes(event.origin)) {
    return;
  }
  let data = event.data;
  try {
    if (typeof data === 'string') data = JSON.parse(data);
  } catch (_) {
    return;
  }
  if (data?.type !== 'WA_EMBEDDED_SIGNUP') return;
  if (data.event === 'FINISH') {
    signupData = {
      phoneNumberId: data.data?.phone_number_id,
      businessAccountId: data.data?.waba_id
    };
  }
});

$('metaConnect').onclick = () => {
  if (!dashboard.metaAppId || !dashboard.metaConfigurationId) {
    $('metaResult').textContent =
      'Ajoutez META_APP_ID, META_APP_SECRET et META_CONFIGURATION_ID dans .env.';
    return;
  }
  if (!window.FB) {
    $('metaResult').textContent = 'Meta est en cours de chargement. Réessayez dans un instant.';
    return;
  }
  signupData = {};
  $('metaResult').textContent = 'Connexion à Meta en cours...';
  FB.login(async response => {
    const code = response.authResponse?.code;
    if (!code || !signupData.phoneNumberId || !signupData.businessAccountId) {
      $('metaResult').textContent =
        'Connexion annulée ou informations WhatsApp incomplètes.';
      return;
    }
    try {
      await api('/api/meta/connect', {
        method: 'POST',
        body: JSON.stringify({code, ...signupData})
      });
      $('metaResult').textContent = 'WhatsApp Business connecté avec succès.';
      await load();
    } catch (error) {
      $('metaResult').textContent = error.message;
    }
  }, {
    config_id: dashboard.metaConfigurationId,
    response_type: 'code',
    override_default_response_type: true,
    extras: {
      setup: {},
      featureType: 'whatsapp_business_app_onboarding',
      sessionInfoVersion: '3'
    }
  });
};

$('testConnect').onclick = async () => {
  $('metaResult').textContent = 'Connexion au numéro Meta de test...';
  try {
    const result = await api('/api/meta/connect-test-number', {method: 'POST'});
    $('metaResult').textContent =
      'Connecté au numéro Meta de test ' + result.displayPhone + '.';
    await load();
  } catch (error) {
    $('metaResult').textContent = error.message;
  }
};

async function loadProducts() {
  const list = await api('/api/dashboard/products');
  $('count').textContent = list.length + ' produit(s)';
  $('products').innerHTML = list.length
    ? list.map(product => `<div class="product"><div><b>${product.name}</b><br>
      <small>${product.price} ${product.currency} · stock ${product.stockQuantity ?? 'à confirmer'}</small>
      </div><button onclick="removeProduct('${product.id}')">Supprimer</button></div>`).join('')
    : '<p>Votre catalogue est vide.</p>';
}

$('productForm').onsubmit = async event => {
  event.preventDefault();
  await api('/api/dashboard/products', {
    method: 'POST',
    body: JSON.stringify({
      name: $('productName').value,
      description: $('description').value,
      price: +$('price').value,
      currency: 'XOF',
      stockQuantity: $('stock').value ? +$('stock').value : null
    })
  });
  event.target.reset();
  loadProducts();
};

async function removeProduct(id) {
  await api('/api/dashboard/products/' + id, {method: 'DELETE'});
  loadProducts();
}

$('logout').onclick = async () => {
  await fetch(API_BASE + '/api/auth/logout', {
    method: 'POST',
    credentials: 'include'
  });
  location.reload();
};

load().catch(() => showApp(false));
