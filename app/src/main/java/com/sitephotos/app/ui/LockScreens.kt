package com.sitephotos.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sitephotos.app.LockStore
import kotlinx.coroutines.delay

const val PIN_LENGTH = 4

/** Dark-blue full screen with a title, PIN dots and a number pad. */
@Composable
private fun PinPadScreen(
    title: String,
    subtitle: String,
    error: String?,
    pin: String,
    enabled: Boolean,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onFingerKey: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))
        Text("🔒", fontSize = 40.sp)
        Spacer(Modifier.height(12.dp))
        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            repeat(PIN_LENGTH) { i ->
                Box(
                    Modifier
                        .size(18.dp)
                        .border(2.dp, Color.White, CircleShape)
                        .background(if (i < pin.length) Color.White else Color.Transparent, CircleShape)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(error ?: " ", color = Color(0xFFFFC857), fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))

        val rows = listOf("123", "456", "789")
        rows.forEach { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                r.forEach { d -> PadKey(d.toString(), enabled) { onDigit(d) } }
            }
            Spacer(Modifier.height(16.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(76.dp)
                    .clickable(enabled = onFingerKey != null) { onFingerKey?.invoke() },
                contentAlignment = Alignment.Center,
            ) { if (onFingerKey != null) Text("👆", fontSize = 30.sp) }
            PadKey("0", enabled) { onDigit('0') }
            Box(
                Modifier
                    .size(76.dp)
                    .clickable(enabled = enabled && pin.isNotEmpty()) { onDelete() },
                contentAlignment = Alignment.Center,
            ) { Text("⌫", color = Color.White, fontSize = 26.sp) }
        }
        Spacer(Modifier.height(12.dp))
        if (onCancel != null) {
            TextButton(onClick = onCancel) { Text("Cancel", color = Color.White) }
        }
    }
}

@Composable
private fun PadKey(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(76.dp)
            .background(Color.White.copy(alpha = if (enabled) 0.14f else 0.05f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Medium)
    }
}

/** Unlock with PIN, or with fingerprint if enabled. */
@Composable
fun LockScreen(
    lock: LockStore,
    fingerprintAvailable: Boolean,
    onFingerprint: () -> Unit,
    onUnlocked: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var wait by remember { mutableLongStateOf(lock.waitSeconds()) }
    val useFinger = fingerprintAvailable && lock.fingerprintEnabled

    LaunchedEffect(Unit) { if (useFinger) onFingerprint() }
    LaunchedEffect(wait) {
        if (wait > 0) {
            delay(1000)
            wait = lock.waitSeconds()
            if (wait == 0L) error = null
        }
    }

    PinPadScreen(
        title = "Site Photos",
        subtitle = if (useFinger) "Enter your PIN or use your fingerprint" else "Enter your PIN",
        error = if (wait > 0) "Too many wrong PINs. Try again in $wait s" else error,
        pin = pin,
        enabled = wait == 0L,
        onDigit = { d ->
            if (pin.length < PIN_LENGTH) pin += d
            if (pin.length == PIN_LENGTH) {
                if (lock.checkPin(pin)) {
                    onUnlocked()
                } else {
                    error = "Wrong PIN"
                    wait = lock.waitSeconds()
                }
                pin = ""
            }
        },
        onDelete = { pin = pin.dropLast(1) },
        onFingerKey = if (useFinger) onFingerprint else null,
    )
}

/**
 * Create (or change) the PIN: enter twice. Then, if the phone has a fingerprint
 * sensor, offer to enable fingerprint unlock.
 */
@Composable
fun SetPinScreen(
    lock: LockStore,
    isChange: Boolean,
    fingerprintAvailable: Boolean,
    onDone: () -> Unit,
    onCancel: (() -> Unit)?,
) {
    var first by remember { mutableStateOf<String?>(null) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var askFinger by remember { mutableStateOf(false) }

    if (askFinger) {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary)
                .statusBarsPadding()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("👆", fontSize = 56.sp)
            Spacer(Modifier.height(16.dp))
            Text("Use fingerprint to unlock?", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "You can always use your PIN as well. You can change this later in Security.",
                color = Color.White.copy(alpha = 0.75f), textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { lock.fingerprintEnabled = true; onDone() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary),
            ) { Text("Yes, use fingerprint", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = { lock.fingerprintEnabled = false; onDone() }) {
                Text("No, PIN only", color = Color.White)
            }
        }
        return
    }

    PinPadScreen(
        title = when {
            first != null -> "Confirm PIN"
            isChange -> "New PIN"
            else -> "Create a PIN"
        },
        subtitle = if (first == null) "Choose a $PIN_LENGTH-digit PIN to protect the app"
        else "Enter the same PIN again",
        error = error,
        pin = pin,
        enabled = true,
        onDigit = { d ->
            if (pin.length < PIN_LENGTH) pin += d
            if (pin.length == PIN_LENGTH) {
                val f = first
                if (f == null) {
                    first = pin
                    error = null
                } else if (f == pin) {
                    lock.setPin(pin)
                    if (fingerprintAvailable && !isChange) askFinger = true else onDone()
                } else {
                    first = null
                    error = "PINs did not match. Start again."
                }
                pin = ""
            }
        },
        onDelete = { pin = pin.dropLast(1) },
        onCancel = onCancel,
    )
}

/** Security settings: change PIN, turn fingerprint on/off. */
@Composable
fun SecurityScreen(
    lock: LockStore,
    fingerprintAvailable: Boolean,
    onChangePin: () -> Unit,
    onBack: () -> Unit,
) {
    var finger by remember { mutableStateOf(lock.fingerprintEnabled) }
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("‹ Back", color = Color.White, fontSize = 16.sp) }
            Text("Security", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Fingerprint unlock", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(
                        if (fingerprintAvailable) "Unlock with fingerprint or face instead of typing the PIN"
                        else "No fingerprint set up on this phone (add one in phone Settings › Security)",
                        color = Color.Gray, fontSize = 13.sp,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = finger && fingerprintAvailable,
                    enabled = fingerprintAvailable,
                    onCheckedChange = { finger = it; lock.fingerprintEnabled = it },
                )
            }
            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = onChangePin, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text("Change PIN")
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "The app locks when opened and after 1 minute in the background.\n\n" +
                    "Forgot the PIN? Clear the app's storage in phone Settings › Apps › Site Photos. " +
                    "This also deletes photos not yet synced – sync first.",
                color = Color.Gray, fontSize = 13.sp,
            )
        }
    }
}
