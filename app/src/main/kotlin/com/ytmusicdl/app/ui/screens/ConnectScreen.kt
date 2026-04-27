package com.ytmusicdl.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Pantalla de conexión — se muestra mientras el servidor no está listo.
 * Muestra spinner + estado actual + botón para reintentar.
 */
@Composable
fun ConnectScreen(
    state: ConnectState,
    onRetry: () -> Unit,
) {
    val rotation by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Column(
        modifier            = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Logo / ícono girando mientras conecta
        if (state == ConnectState.CONNECTING) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                strokeWidth = 3.dp,
            )
        } else {
            Icon(
                painter            = painterResource(id = android.R.drawable.ic_media_play),
                contentDescription = null,
                modifier           = Modifier.size(64.dp),
                tint               = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text  = "ytmusicdl",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text      = when (state) {
                ConnectState.CONNECTING   -> "Conectando con Termux…"
                ConnectState.STARTING     -> "Iniciando servidor…"
                ConnectState.NO_TERMUX    -> "Termux no encontrado"
                ConnectState.TIMEOUT      -> "No se pudo conectar"
                ConnectState.CONNECTED    -> "¡Listo!"
            },
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )

        if (state == ConnectState.NO_TERMUX || state == ConnectState.TIMEOUT) {
            Spacer(Modifier.height(24.dp))

            if (state == ConnectState.NO_TERMUX) {
                Text(
                    text      = "Instala Termux desde F-Droid y ejecuta:\nbash install.sh",
                    style     = MaterialTheme.typography.bodySmall,
                    color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
            }

            OutlinedButton(onClick = onRetry) {
                Text("Reintentar")
            }
        }
    }
}

enum class ConnectState { CONNECTING, STARTING, NO_TERMUX, TIMEOUT, CONNECTED }
