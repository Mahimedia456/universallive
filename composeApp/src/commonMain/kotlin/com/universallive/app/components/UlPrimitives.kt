package com.universallive.app.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.theme.*

@Composable
fun UlPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(UlRadius.control),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppPrimary,
            contentColor = Color.White,
            disabledContainerColor = AppPrimary.copy(alpha = .34f),
            disabledContentColor = Color.White.copy(alpha = .62f),
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = Color.White,
            )
        } else {
            Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun UlSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(UlRadius.control),
        border = BorderStroke(1.dp, AppPrimary.copy(alpha = .45f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppText),
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun UlTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable (() -> Unit))? = null,
    isError: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { if (placeholder.isNotBlank()) Text(placeholder) },
        visualTransformation = visualTransformation,
        trailingIcon = trailing,
        isError = isError,
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(UlRadius.control),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppPrimary,
            unfocusedBorderColor = AppBorder,
            focusedLabelColor = AppPrimary,
            cursorColor = AppPrimary,
            focusedContainerColor = AppSurface.copy(alpha = .76f),
            unfocusedContainerColor = AppSurface.copy(alpha = .56f),
        ),
    )
}

@Composable
fun UlCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(UlRadius.card))
            .background(AppSurface)
            .border(1.dp, AppBorder, RoundedCornerShape(UlRadius.card))
            .padding(18.dp),
        content = content,
    )
}

@Composable
fun UlStatusBadge(
    text: String,
    color: Color = AppSuccess,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(color.copy(alpha = .12f))
            .border(1.dp, color.copy(alpha = .28f), RoundedCornerShape(50.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun UlSectionHeader(
    eyebrow: String,
    title: String,
    body: String? = null,
) {
    Text(
        eyebrow.uppercase(),
        color = AppPrimary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
    )
    Spacer(Modifier.height(8.dp))
    Text(title, style = MaterialTheme.typography.headlineMedium)
    body?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, style = MaterialTheme.typography.bodyLarge)
    }
}
