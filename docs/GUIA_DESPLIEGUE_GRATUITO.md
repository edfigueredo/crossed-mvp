# Guía de despliegue gratuito de CrossEd

## Arquitectura elegida

| Parte | Servicio | Plan |
| --- | --- | --- |
| Código | GitHub | Repositorio privado gratuito |
| Frontend | Vercel | Hobby |
| Backend | Render | Web Service gratuito, Docker |
| Redis | Upstash | Base Redis gratuita |
| IA | Google AI Studio / Gemini | Cuota gratuita disponible |
| Correo | Brevo | API transaccional gratuita |

No cargues claves en GitHub. Todas deben guardarse como variables secretas del proveedor correspondiente.

## 1. Subir el proyecto a GitHub

1. Descomprimí `CrossEd_MVP.zip`.
2. En GitHub creá un repositorio privado llamado `crossed-mvp`, vacío: sin README, licencia ni `.gitignore`.
3. Abrí una terminal dentro de la carpeta `CrossEd` y ejecutá:

```bash
git init
git add .
git commit -m "CrossEd MVP listo para despliegue"
git branch -M main
git remote add origin https://github.com/TU_USUARIO/crossed-mvp.git
git push -u origin main
```

## 2. Crear Redis en Upstash

1. Creá una base Redis gratuita en la región más cercana a los usuarios.
2. En los datos de conexión copiá host, puerto, usuario y contraseña del endpoint TLS.
3. Guardá estos valores para Render:

```text
REDIS_HOST=<host sin redis://>
REDIS_PORT=<puerto>
REDIS_USERNAME=default
REDIS_PASSWORD=<contraseña>
REDIS_SSL=true
```

## 3. Obtener una clave gratuita de Gemini

1. Abrí Google AI Studio y creá una API key.
2. Guardala como `GEMINI_API_KEY`.
3. No es necesario configurar OpenAI; dejá `OPENAI_API_KEY` vacío.

## 4. Configurar Brevo

1. Creá una cuenta gratuita de Brevo.
2. Verificá una dirección remitente en **Senders & IP**.
3. Creá una API key en **SMTP & API > API Keys**.
4. Guardá:

```text
BREVO_API_KEY=<clave>
MAIL_FROM=<correo remitente verificado>
MAIL_FROM_NAME=CrossEd
```

## 5. Publicar el backend en Render

1. Elegí **New > Web Service**, conectá GitHub y seleccioná `crossed-mvp`.
2. Configurá:

| Campo | Valor |
| --- | --- |
| Runtime | Docker |
| Root Directory | `backend` |
| Dockerfile Path | `./Dockerfile` |
| Instance Type | Free |
| Health Check Path | `/actuator/health` |

3. Agregá las variables:

```text
REDIS_HOST=<Upstash>
REDIS_PORT=<Upstash>
REDIS_USERNAME=default
REDIS_PASSWORD=<Upstash>
REDIS_SSL=true
GEMINI_API_KEY=<Gemini>
OPENAI_API_KEY=
BREVO_API_KEY=<Brevo>
MAIL_FROM=<remitente verificado>
MAIL_FROM_NAME=CrossEd
CORS_ALLOWED_ORIGINS=https://placeholder.vercel.app
```

4. Desplegá y esperá `Live`.
5. Copiá la URL, por ejemplo `https://crossed-api.onrender.com`.
6. Comprobá `https://TU_BACKEND.onrender.com/actuator/health`; debe responder `{"status":"UP"}`.

## 6. Publicar el frontend en Vercel

1. Importá el repositorio de GitHub.
2. Elegí `frontend` como **Root Directory**; Vercel detectará Next.js.
3. Agregá antes del build:

```text
NEXT_PUBLIC_API_URL=https://TU_BACKEND.onrender.com
NEXT_PUBLIC_WS_URL=wss://TU_BACKEND.onrender.com/ws
```

4. Desplegá y copiá la URL, por ejemplo `https://crossed-mvp.vercel.app`.

## 7. Cerrar CORS y redesplegar

1. Volvé al servicio de Render.
2. Cambiá `CORS_ALLOWED_ORIGINS` por la URL exacta de Vercel, sin `/` final.
3. Guardá y desplegá nuevamente.

## 8. Prueba final

1. Abrí la URL de Vercel en una ventana normal y creá una partida.
2. Abrí el QR/enlace en incógnito, ingresá como alumno e iniciá la partida.
3. Respondé al menos una palabra y finalizá desde el panel del profesor.
4. Confirmá el ranking y los dos correos.
5. Revisá en Render que no haya errores y en Brevo que los mensajes figuren como entregados.

## Límites gratuitos esperables

- Render suspende el backend tras un período de inactividad; el primer acceso puede tardar alrededor de un minuto.
- Vercel Hobby está pensado para uso personal/no comercial.
- Upstash, Gemini y Brevo aplican cuotas gratuitas; revisá sus paneles si el uso crece.
- No actives servicios pagos ni ingreses una tarjeta si querés conservar costo cero.

