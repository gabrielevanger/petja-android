package io.github.gabrielevanger.petja.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** Junta informações curtas com o separador do app, ignorando as vazias: "Centro · 440 m". */
@Composable
fun dotSeparated(vararg parts: String?): String = parts
    .filterNot { it.isNullOrBlank() }
    .joinToString(stringResource(R.string.separator_dot))
