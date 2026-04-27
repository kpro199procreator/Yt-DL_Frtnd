# ytmusicdl — App Android

Frontend Material 3 para [ytmusicdl](https://github.com/tu-usuario/ytmusic-dl).
Se comunica con ytmusicdl corriendo en Termux vía HTTP local.

## Requisitos

- Termux instalado (desde F-Droid)
- ytmusicdl instalado en Termux
- `allow-external-apps = true` en `~/.termux/termux.properties`

## Compilar

```bash
# Debug (sin keystore)
./gradlew assembleDebug

# APK en: app/build/outputs/apk/debug/app-debug.apk
```

## Release automático con GitHub Actions

Crear un tag para disparar el workflow:
```bash
git tag v0.3.0
git push origin v0.3.0
```

El APK aparece en GitHub Releases automáticamente.

## Configurar firma (opcional, para releases firmados)

### 1. Generar keystore
```bash
keytool -genkey -v \
  -keystore ytmusicdl.jks \
  -keyalg RSA -keysize 2048 \
  -validity 10000 \
  -alias ytmusicdl
```

### 2. Convertir a base64
```bash
base64 ytmusicdl.jks | tr -d '\n'
```

### 3. Agregar secrets en GitHub
```
Settings → Secrets → Actions → New secret:

KEYSTORE_BASE64        → base64 del .jks
SIGNING_KEY_ALIAS      → ytmusicdl
SIGNING_KEY_PASSWORD   → tu contraseña de key
SIGNING_STORE_PASSWORD → tu contraseña del keystore
```

### 4. Para desarrollo local
Crear `keystore.properties` en la raíz (está en .gitignore):
```
storePassword=tu_contraseña
keyAlias=ytmusicdl
keyPassword=tu_contraseña
```
