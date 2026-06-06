# ASAMI Backend

ASAMI est un assistant commercial WhatsApp multilingue pour les vendeurs.
Le backend recoit les webhooks Meta, gere les vendeurs et leurs produits,
et envoie des messages avec WhatsApp Cloud API.

## Technologies

- Java 21 et Spring Boot 3.5
- PostgreSQL 17 et Flyway
- Docker Compose
- WhatsApp Business Platform
- OpenAI API pour les prochaines etapes IA et audio

## Demarrage local

```powershell
docker compose up -d
.\mvnw.cmd spring-boot:run
```

Etat du backend :

```text
http://localhost:8080/actuator/health
```

La base Docker utilise le port `5433` pour ne pas entrer en conflit avec
PostgreSQL installe sur Windows.

## Configuration Meta

Creer une application sur Meta for Developers, ajouter le produit WhatsApp,
puis recuperer dans la section API Setup :

- le jeton d'acces ;
- le Phone Number ID ;
- le numero WhatsApp de test.

Recuperer aussi l'App Secret dans App settings > Basic.

Completer `.env` :

```properties
WHATSAPP_VERIFY_TOKEN=un-secret-long-choisi-par-vous
WHATSAPP_APP_SECRET=app-secret-meta
WHATSAPP_ACCESS_TOKEN=jeton-meta
WHATSAPP_PHONE_NUMBER_ID=phone-number-id-meta
WHATSAPP_API_VERSION=v23.0
WHATSAPP_TEST_ENDPOINT_ENABLED=true
```

Ne jamais ajouter `.env` dans Git. L'endpoint de test doit revenir a `false`
avant tout deploiement public.

## Envoyer le premier message

Redemarrer Spring Boot apres modification de `.env`, puis executer dans un
deuxieme terminal. Le numero destinataire doit etre autorise dans Meta et
ecrit au format international sans le signe `+`.

Le premier message doit utiliser un modele WhatsApp approuve. Le modele de
demonstration `hello_world` est fourni par Meta.

```powershell
$body = @{ recipientPhone = "221783802344" } | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/test/whatsapp/hello-world" `
  -ContentType "application/json" `
  -Body $body
```

Une reponse avec `messageId` et `status=accepted` confirme que Meta a accepte
le message. Une fois que le destinataire repond, ASAMI peut envoyer des textes
libres pendant la fenetre de conversation autorisee par WhatsApp.

## Webhook WhatsApp

```text
GET  /api/webhooks/whatsapp
POST /api/webhooks/whatsapp
```

L'URL a declarer dans Meta sera :

```text
https://votre-domaine/api/webhooks/whatsapp
```

Meta exige une URL HTTPS publique. En local, il faudra utiliser un tunnel
HTTPS comme Cloudflare Tunnel ou ngrok.

## Tests

```powershell
.\mvnw.cmd test
```

## Espace vendeur

Ouvrir `http://localhost:8080` pour :

- creer un compte vendeur securise ;
- gerer son propre catalogue ;
- enregistrer son Phone Number ID et son WABA ID ;
- connecter plusieurs vendeurs sans melanger leurs produits.

## Numero WhatsApp Business existant

Pour conserver l'application WhatsApp Business sur le telephone du vendeur,
Meta doit activer le parcours **WhatsApp Business App Coexistence** dans une
configuration **Embedded Signup**.

Ajouter ensuite dans `.env` :

```properties
META_APP_ID=identifiant_application_meta
META_CONFIGURATION_ID=identifiant_configuration_embedded_signup
```

Ces identifiants sont crees dans Meta for Developers apres configuration de
Facebook Login for Business et WhatsApp Embedded Signup. Sans cette validation
Meta, le formulaire manuel du tableau de bord permet le developpement, mais ne
remplace pas l'autorisation officielle du numero.

## Deploiement Railway

Vercel n'est pas utilise pour le backend principal : ASAMI a besoin d'un
processus Spring Boot permanent, de PostgreSQL et d'un webhook public stable.

1. Pousser ce projet dans un depot GitHub prive.
2. Dans Railway, creer un projet et ajouter un service PostgreSQL.
3. Ajouter un service depuis le depot GitHub. Railway utilisera `Dockerfile`
   et `railway.json`.
4. Dans les variables du service ASAMI, ajouter :

```properties
DATABASE_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
DATABASE_USERNAME=${{Postgres.PGUSER}}
DATABASE_PASSWORD=${{Postgres.PGPASSWORD}}
WHATSAPP_VERIFY_TOKEN=un_secret_long
WHATSAPP_APP_SECRET=secret_application_meta
WHATSAPP_ACCESS_TOKEN=token_meta_permanent
WHATSAPP_PHONE_NUMBER_ID=1144015498802314
WHATSAPP_GRAPH_API_VERSION=v25.0
WHATSAPP_TEST_ENDPOINT_ENABLED=false
OPENAI_API_KEY=cle_openai
OPENAI_TRANSCRIPTION_MODEL=gpt-4o-transcribe
META_APP_ID=1017257404089186
META_APP_SECRET=secret_application_meta
META_CONFIGURATION_ID=
META_TOKEN_ENCRYPTION_KEY=une_cle_aleatoire_longue_et_stable
```

5. Dans `Settings > Networking`, generer un domaine Railway.
6. Remplacer dans Meta l'URL temporaire Cloudflare par :

```text
https://VOTRE-DOMAINE.up.railway.app/api/webhooks/whatsapp
```

Le token Meta de production doit etre permanent. Une cle temporaire expirera
et ASAMI cessera de repondre.
