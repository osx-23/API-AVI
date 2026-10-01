# API-AVI

Backend Spring Boot y laboratorio web de pruebas para **AVI**, orientado al registro operativo por voz.

## Estado de la V1

La V1 permite probar todo el flujo funcional sin un teléfono Android:

```text
texto o SpeechRecognition del navegador
        ↓
POST /api/v1/interpretar
        ↓
parser AVI
        ↓
FUGA / vía / placa
        ↓
corrección manual
        ↓
POST /api/v1/registros
        ↓
Supabase
```

La captura física del micrófono y el reconocimiento de voz del navegador se diagnostican por separado. Esto es importante porque un micrófono puede funcionar correctamente aunque `SpeechRecognition` falle por compatibilidad o red.

## Stack

- Java 17
- Spring Boot 3.5.16
- Maven
- Supabase REST / PostgREST
- HTML + CSS + JavaScript puro
- Web Speech API solo como mecanismo experimental de voz en la V1

## Parser AVI

Ejemplos válidos:

```text
Fuga vía 151
Fuga vía número 151
Fuga vía ciento cincuenta y uno
Fuga vía uno cinco uno
Fuga vía mil doscientos treinta y cuatro
Fuga vía 151 placa Bravo Tango Lima dos cuatro cinco
Fuga vía 151 placa BTL245
Fuga vía 151 placa BTL-245
Fuga vía 151 placa be te ele dos cuatro cinco
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
  "textoOriginal": "Fuga vía ciento cincuenta y uno placa Bravo Tango Lima dos cuatro cinco",
  "valido": true,
  "errores": []
}
```

### Reglas actuales

- Tipo soportado: `FUGA`.
- Vía: entre 1 y 9999.
- La placa es opcional.
- Si se dice la palabra `placa`, debe poder interpretarse.
- Placa: máximo 10 caracteres alfanuméricos.
- Se reconocen números normales, números hablados y secuencias de dígitos.
- Se admite alfabeto fonético y nombres comunes de letras.

## Alfabeto fonético

```text
Alfa A       Bravo B      Charlie C    Delta D
Echo E       Foxtrot F    Golf G       Hotel H
India I      Juliet J     Kilo K       Lima L
Mike M       November N   Oscar O      Papa P
Quebec Q     Romeo R      Sierra S     Tango T
Uniform U    Victor V     Whiskey W    Xray X
Yankee Y     Zulu Z
```

También se toleran algunas variantes habituales del reconocimiento, como `eco`, `charly`, `julieta`, `noviembre`, `uniforme` y `yanki`.

## Supabase

Proyecto actual:

```text
https://adozechgzkbqopujaapp.supabase.co
```

Tabla esperada: `public.registros`

- `id uuid`
- `tipo text`
- `via integer`
- `placa text`
- `texto_reconocido text`
- `creado_en timestamptz`

La publishable key no se guarda en Git.

Configúrala antes de iniciar:

```bash
export SUPABASE_PUBLISHABLE_KEY='sb_publishable_...'
```

Opcionalmente:

```bash
export SUPABASE_URL='https://adozechgzkbqopujaapp.supabase.co'
```

Nunca subas `sb_secret_...` ni `service_role`.

## Ejecutar localmente

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

No abras directamente `index.html` con `file://`.

## Laboratorio web

La pantalla permite:

- escribir un comando;
- usar `Cmd + Enter` o `Ctrl + Enter` para interpretar;
- corregir tipo, vía y placa antes de registrar;
- seleccionar la entrada física de audio;
- medir el nivel real del micrófono;
- probar `SpeechRecognition`;
- revisar el JSON interpretado;
- registrar en Supabase;
- consultar los últimos registros.

La vía se limita automáticamente a cuatro dígitos y la placa se normaliza a mayúsculas alfanuméricas.

## Diagnóstico de voz

### La barra de micrófono no se mueve

El problema está en la captura de audio, el dispositivo seleccionado o los permisos del sistema.

### La barra se mueve pero aparece `no-speech` o `network`

La captura física funciona. El problema está en `SpeechRecognition` del navegador.

La V1 intenta reconocimiento local cuando el navegador lo ofrece y aplica límites de tiempo para evitar quedar bloqueada instalando paquetes de idioma.

Brave puede presentar más limitaciones con Web Speech API. Para comparar comportamiento, prueba también Google Chrome actualizado.

## API

### Estado

```http
GET /api/v1/status
```

Ejemplo:

```json
{
  "api": "ok",
  "version": "0.2.0-v1",
  "parser": "avi-voice-v1.1",
  "supabaseConfigurado": true
}
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

Devuelve hasta 50 registros ordenados por `creado_en` descendente.

## Pruebas

```bash
mvn test
```

La suite cubre vías numéricas, números hablados, miles, dígitos separados, placas fonéticas, placas compactas, variantes de pronunciación y comandos inválidos.

GitHub Actions ejecuta automáticamente:

```text
mvn test
mvn -DskipTests package
```

## Próxima etapa

Una vez cerrada esta V1, el siguiente paso será reemplazar la dependencia principal de Web Speech API por captura de audio real con `MediaRecorder` y transcripción desde el backend. La app Android podrá reutilizar posteriormente la misma API.
