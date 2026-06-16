package app.thdev.glassnavlab.feature.notmid

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun NotmidLoginScreen(
    errorMessage: String?,
    isAuthenticating: Boolean = false,
    onContinueLocal: () -> Unit,
    onContinueGoogle: () -> Unit = onContinueLocal,
    onBrowseSignedOut: () -> Unit,
) {
    var identity by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LoginBackground),
    ) {
        KineticLoginBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            LoginHeader(onTitleClick = onBrowseSignedOut)
            Spacer(modifier = Modifier.height(48.dp))
            LoginCard(
                identity = identity,
                onIdentityChange = { identity = it },
                password = password,
                onPasswordChange = { password = it },
                showPassword = showPassword,
                onTogglePassword = { showPassword = !showPassword },
                isAuthenticating = isAuthenticating,
                onContinueLocal = onContinueLocal,
                onContinueGoogle = onContinueGoogle,
            )
            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                LoginErrorMessage(errorMessage)
            }
            Spacer(modifier = Modifier.height(48.dp))
            LoginFooter(enabled = !isAuthenticating, onJoin = onContinueLocal)
            Spacer(modifier = Modifier.height(48.dp))
        }

        LoginHelpButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),
        )
    }
}

@Composable
private fun LoginErrorMessage(message: String) {
    Text(
        text = message,
        modifier = Modifier
            .widthIn(max = 448.dp)
            .fillMaxWidth()
            .clip(LoginControlShape)
            .background(Color.White.copy(alpha = 0.6f))
            .border(BorderStroke(1.dp, LoginOnSurface.copy(alpha = 0.12f)), LoginControlShape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        color = LoginOnSurface,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
        lineHeight = 18.sp,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun LoginHeader(onTitleClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "notmid",
            modifier = Modifier.clickable(onClick = onTitleClick),
            color = LoginOnSurface,
            fontSize = 48.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp,
            lineHeight = 53.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "show receipts.",
            modifier = Modifier
                .padding(top = 8.dp)
                .background(LoginOnSurface, LoginTagShape)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            color = LoginPrimaryContainer,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp,
            lineHeight = 31.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LoginCard(
    identity: String,
    onIdentityChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    showPassword: Boolean,
    onTogglePassword: () -> Unit,
    isAuthenticating: Boolean,
    onContinueLocal: () -> Unit,
    onContinueGoogle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = 448.dp)
            .fillMaxWidth()
            .shadow(24.dp, LoginCardShape)
            .clip(LoginCardShape)
            .background(Color.White.copy(alpha = 0.4f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)), LoginCardShape)
            .padding(32.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            StitchLoginTextField(
                value = identity,
                onValueChange = onIdentityChange,
                label = "Username or Email",
                placeholder = "yourname@receipts.xyz",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
            )
            StitchLoginTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = "Password",
                placeholder = "********",
                visualTransformation = if (showPassword) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                trailingContent = {
                    Text(
                        text = if (showPassword) "Hide" else "Show",
                        modifier = Modifier
                            .clip(LoginControlShape)
                            .clickable(onClick = onTogglePassword)
                            .padding(start = 10.dp, top = 8.dp, bottom = 8.dp),
                        color = LoginOnSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.sp,
                    )
                },
            )
            StitchPrimaryButton(
                text = if (isAuthenticating) "Checking..." else "Login",
                enabled = !isAuthenticating,
                onClick = onContinueLocal,
            )
        }

        LoginDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StitchSocialButton(
                label = "Google",
                monogram = "G",
                enabled = !isAuthenticating,
                onClick = onContinueGoogle,
                modifier = Modifier.weight(1f),
            )
            StitchSocialButton(
                label = "Apple",
                monogram = "A",
                enabled = !isAuthenticating,
                onClick = onContinueLocal,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StitchLoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = LoginOnSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
            lineHeight = 13.sp,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { isFocused = it.isFocused },
                singleLine = true,
                textStyle = TextStyle(
                    color = LoginOnSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 24.sp,
                    letterSpacing = 0.sp,
                ),
                cursorBrush = SolidColor(LoginPrimaryContainer),
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = LoginOnSurfaceVariant.copy(alpha = 0.5f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 24.sp,
                                letterSpacing = 0.sp,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            trailingContent?.invoke()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    color = if (isFocused) {
                        LoginPrimaryContainer
                    } else {
                        LoginOnSurface.copy(alpha = 0.2f)
                    },
                ),
        )
    }
}

@Composable
private fun StitchPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = LoginControlShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = LoginOnSurface,
            contentColor = LoginPrimaryContainer,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp,
            lineHeight = 17.sp,
        )
    }
}

@Composable
private fun LoginDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(LoginOnSurface.copy(alpha = 0.1f)),
        )
        Text(
            text = "Verify with",
            color = LoginOnSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
            lineHeight = 13.sp,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(LoginOnSurface.copy(alpha = 0.1f)),
        )
    }
}

@Composable
private fun StitchSocialButton(
    label: String,
    monogram: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = LoginControlShape,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.6f),
            contentColor = LoginOnSurface,
        ),
        contentPadding = PaddingValues(horizontal = 8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(LoginOnSurface),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = monogram,
                    color = LoginPrimaryContainer,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 12.sp,
                )
            }
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.sp,
                lineHeight = 17.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LoginFooter(enabled: Boolean, onJoin: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Not a member yet?",
            color = LoginOnSurfaceVariant,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp,
            lineHeight = 24.sp,
        )
        Column(
            modifier = Modifier
                .padding(top = 8.dp)
                .clip(LoginControlShape)
                .clickable(enabled = enabled, onClick = onJoin)
                .padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Join the movement",
                color = LoginOnSurface,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.sp,
                lineHeight = 31.sp,
            )
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .width(220.dp)
                    .height(4.dp)
                    .background(LoginPrimaryContainer),
            )
        }
    }
}
