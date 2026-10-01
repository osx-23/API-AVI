# API-AVI

Backend Spring Boot y laboratorio web de pruebas para **AVI**.

## Flujo actual

```text
Micrófono seleccionado
        ↓
MediaRecorder
        ↓
audio webm/mp4
        ↓
POST /api/v1/transcribir
        ↓
Whisper local
        ↓
texto
        ↓
parser AVI
        ↓
FUGA / vía / placa
        ↓
corrección manual
        ↓
Supabase
```

La transcripción principal ya **no usa una API de pago** ni depende de `SpeechRecognition` del navegador. `SpeechRecognition` se mantiene únicamente como modo experimental.

## Requisitos

- Java 17
- Maven
- Python 3.11 recomendado
- FFmpeg
- OpenAI Whisper local
- Supabase para persistencia

## Instalar Whisper local en macOS

Desde Terminal:

```bash
brew install python@3.11 ffmpeg
```

Dentro de la carpeta de API-AVI:

```bash
cd /Users/oscarherrera/Downloads/API-AVI

python3.11 -m venv .venv-whisper
source .venv-whisper/bin/activate

python -m pip install --upgrade pip
python -m pip install -U openai-whisper
```

Verifica:

```bash
whisper --help
ffmpeg -version
```

Si ambos comandos funcionan, Whisper está listo.

> La primera transcripción descargará el modelo configurado. Después el modelo queda almacenado localmente.

## Modelo

Por defecto AVI usa:

```text
small
```

Está pensado como equilibrio entre precisión y consumo para comandos cortos en español.

Puedes cambiarlo antes de iniciar Spring Boot:

```bash
export WHISPER_MODEL='base'
```

o:

```bash
export WHISPER_MODEL='medium'
```

Configuración disponible:

```bash
export WHISPER_COMMAND='whisper'
export WHISPER_MODEL='small'
export WHISPER_LANGUAGE='Spanish'
export WHISPER_TIMEOUT_SECONDS='600'
```

Si Spring Boot no encuentra el ejecutable aunque Whisper esté instalado, usa la ruta absoluta:

```bash
export WHISPER_COMMAND='/Users/oscarherrera/Downloads/API-AVI/.venv-whisper/bin/whisper'
```

## Ejecutar AVI

En una terminal:

```bash
cd /Users/oscarherrera/Downloads/API-AVI
source .venv-whisper/bin/activate

export SUPABASE_PUBLISHABLE_KEY='sb_publishable_TU_CLAVE'

mvn spring-boot:run
```

Abre:

```text
http://localhost:8080
```

Arriba deberías ver:

```text
API ✓
Whisper local ✓
Supabase ✓
```

## Probar voz

Selecciona el micrófono físico del Mac y pulsa:

```text
🎙 Grabar comando
```

Di:

```text
Fuga vía ciento cincuenta y uno placa Bravo Tango Lima dos cuatro cinco
```

Pulsa nuevamente para terminar.

Resultado esperado:

```text
Tipo:  FUGA
Vía:   151
Placa: BTL245
```

## Parser AVI

Ejemplos aceptados:

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

Reglas:

- tipo actual: `FUGA`;
- vía: `1..9999`;
- placa opcional;
- placa máximo 10 caracteres alfanuméricos;
- si se dice `placa`, debe poder interpretarse;
- números normales, números hablados y dígitos separados;
- alfabeto fonético y nombres de letras.

## API

### Estado

```http
GET /api/v1/status
```

Ejemplo:

```json
{
  "api": "ok",
  "version": "0.4.0-v1",
  "parser": "avi-voice-v1.1",
  "supabaseConfigurado": true,
  "transcripcionConfigurada": true,
  "motorTranscripcion": "whisper-local",
  "modeloTranscripcion": "whisper-local:small"
}
```

### Transcribir

```http
POST /api/v1/transcribir
Content-Type: multipart/form-data
```

Campo:

```text
audio
```

Formatos admitidos:

```text
webm wav mp3 mp4 mpeg mpga m4a
```

Whisper genera el texto y el backend devuelve también la interpretación del parser.

### Interpretar texto

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
```

### Historial

```http
GET /api/v1/registros
```

## Supabase

Variables:

```bash
export SUPABASE_URL='https://adozechgzkbqopujaapp.supabase.co'
export SUPABASE_PUBLISHABLE_KEY='sb_publishable_...'
```

Nunca subas claves secretas o `service_role`.

## Diagnóstico

### `Whisper local pendiente`

Prueba:

```bash
source .venv-whisper/bin/activate
which whisper
whisper --help
```

Si `which whisper` devuelve una ruta, inicia Maven desde esa misma terminal.

También puedes fijarla explícitamente:

```bash
export WHISPER_COMMAND="$(which whisper)"
```

### La barra del micrófono se mueve, pero Whisper no genera texto

Comprueba:

```bash
ffmpeg -version
```

y revisa el mensaje mostrado por AVI. El backend incluye la salida final de Whisper cuando el proceso falla.

### Primera transcripción lenta

Es normal si todavía tiene que descargar el modelo `small`.

## Pruebas

```bash
mvn test
```

GitHub Actions ejecuta:

```text
mvn test
mvn -DskipTests package
node --check del JavaScript del laboratorio
```

Las pruebas CI no necesitan Whisper instalado: utilizan un comando inexistente deliberadamente para comprobar el manejo correcto del estado `Whisper local pendiente`.
