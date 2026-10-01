# API-AVI

Backend de **AVI** para interpretar comandos operativos, registrar fugas en Supabase y probar el flujo desde un navegador sin necesitar un teléfono Android.

## Stack

- Java 17
- Spring Boot 3.5.16
- Maven
- Supabase REST / PostgREST
- HTML + CSS + JavaScript puro para el laboratorio de pruebas

## Flujo

```text
Chrome / Android
      ↓
POST /api/v1/interpretar
      ↓
FUGA / vía / placa
      ↓
confirmación del operador
      ↓
POST /api/v1/registros
      ↓
Supabase
```

Ejemplo:

```text
Fuga vía ciento cincuenta y uno placa Bravo Tango Lima dos cuatro cinco
```

Resultado:

```json
{
  "tipo": "FUGA",
  "via": 151,
  "placa": "BTL245",
  "valido": true,
  "errores": []
}
```

## Supabase

Proyecto configurado por defecto:

```text
https://adozechgzkbqopujaapp.supabase.co
```

La tabla esperada es `public.registros` con:

- `id uuid`
- `tipo text`
- `via integer`
- `placa text`
- `texto_reconocido text`
- `creado_en timestamptz`

La URL del proyecto no es secreta. La clave **no se guarda en Git**.

Para esta V1 usa la publishable key que funciona con las políticas RLS ya creadas:

```bash
export SUPABASE_PUBLISHABLE_KEY='sb_publishable_...'
```

Opcionalmente puedes sobrescribir la URL:

```bash
export SUPABASE_URL='https://adozechgzkbqopujaapp.supabase.co'
```

## Ejecutar localmente

Necesitas Java 17 y Maven.

En macOS:

```bash
brew install openjdk@17 maven
```

Luego:

```bash
git clone https://github.com/osx-23/API-AVI.git
cd API-AVI

export SUPABASE_PUBLISHABLE_KEY='TU_CLAVE'

mvn spring-boot:run
```

Abre:

```text
http://localhost:8080
```

Desde Chrome puedes usar el botón **Hablar**. Si el reconocimiento de voz del navegador no está disponible, puedes escribir la frase manualmente.

## API

### Estado

```http
GET /api/v1/status
```

### Interpretar

```http
POST /api/v1/interpretar
Content-Type: application/json
```

```json
{
  "texto": "Fuga vía 151 placa Bravo Tango Lima dos cuatro cinco"
}
```

### Registrar

```http
POST /api/v1/registros
Content-Type: application/json
```

```json
{
  "tipo": "FUGA",
  "via": 151,
  "placa": "BTL245",
  "textoReconocido": "Fuga vía 151 placa Bravo Tango Lima dos cuatro cinco"
}
```

### Historial

```http
GET /api/v1/registros
```

Devuelve los últimos 50 registros, ordenados por `creado_en` descendente.

## Seguridad de esta V1

La API utiliza una publishable key para respetar las políticas RLS de Supabase. No subas `service_role`, `sb_secret_...` ni otras credenciales privadas al repositorio.

Más adelante, cuando la app Android consuma exclusivamente esta API y agreguemos autenticación propia, podremos mover el backend a una secret key y endurecer las políticas de acceso.
