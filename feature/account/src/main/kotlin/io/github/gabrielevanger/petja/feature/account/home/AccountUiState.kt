package io.github.gabrielevanger.petja.feature.account.home

import androidx.compose.runtime.Immutable
import io.github.gabrielevanger.petja.core.domain.account.AccountState
import io.github.gabrielevanger.petja.feature.account.AccountNotice

/**
 * @property notice aviso no topo ao chegar por um link de e-mail ou depois de trocar a senha.
 * @property canOperateStore dono ou atendente de uma loja: aparece o atalho do modo lojista (RF-80).
 */
@Immutable
data class AccountUiState(
    val account: AccountState = AccountState.Loading,
    val notice: AccountNotice? = null,
    val canOperateStore: Boolean = false,
)
