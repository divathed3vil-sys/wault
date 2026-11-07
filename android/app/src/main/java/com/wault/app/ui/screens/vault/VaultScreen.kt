package com.wault.app.ui.screens.vault

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.wault.app.R
import com.wault.app.data.Account
import com.wault.app.data.AccountStatus
import com.wault.app.data.DemoDataset
import com.wault.app.data.WaultRepository
import com.wault.app.ui.components.AccountCard
import com.wault.app.ui.components.SlotMeter
import com.wault.app.ui.theme.AccentAmber
import com.wault.app.ui.theme.AccentEmerald
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VaultUiState(
    val accounts: List<Account> = emptyList(),
    val slotsTotal: Int = DemoDataset.DEVICE_SLOTS,
) {
    val slotsUsed: Int get() = accounts.size
    val reconnecting: Int
        get() = accounts.count { it.status == AccountStatus.Reconnecting || it.status == AccountStatus.Error }
    val allHealthy: Boolean get() = reconnecting == 0
}

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val repository: WaultRepository,
) : ViewModel() {

    val uiState: StateFlow<VaultUiState> = repository.accounts
        .map { VaultUiState(accounts = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VaultUiState())

    private val _pairingCreated = Channel<String>(Channel.BUFFERED)
    val pairingCreated: Flow<String> = _pairingCreated.receiveAsFlow()

    /** Starts a pairing-mode session and routes its id to the nav layer. */
    fun startPairing() {
        viewModelScope.launch {
            val used = repository.accounts.value.size
            val accent = DemoDataset.ACCENT_HEXES[used % DemoDataset.ACCENT_HEXES.size]
            val id = repository.startPairing(name = DemoDataset.DEFAULT_PAIRING_NAME, accentHex = accent)
            _pairingCreated.send(id)
        }
    }
}

@Composable
fun VaultScreen(
    viewModel: VaultViewModel,
    onOpenAccount: (String) -> Unit,
    onPairAccount: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.pairingCreated.collect { id -> onPairAccount(id) }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 340.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
            VaultHeader(state = state)
        }
        item(key = "health", span = { GridItemSpan(maxLineSpan) }) {
            SessionHealthCard(state = state)
        }
        items(state.accounts, key = { it.id }) { account ->
            AccountCard(account = account, onOpen = { onOpenAccount(account.id) })
        }
        item(key = "add", span = { GridItemSpan(maxLineSpan) }) {
            AddNumberCard(
                slotsFull = state.slotsUsed >= state.slotsTotal,
                slotsTotal = state.slotsTotal,
                onClick = viewModel::startPairing,
            )
        }
    }
}

@Composable
private fun VaultHeader(state: VaultUiState) {
    Column(modifier = Modifier.statusBarsPadding().padding(top = 12.dp)) {
        Text(
            text = stringResource(R.string.vault_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (state.allHealthy) {
                stringResource(R.string.vault_subtitle_healthy, state.accounts.size)
            } else {
                stringResource(R.string.vault_subtitle_attention, state.accounts.size, state.reconnecting)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SessionHealthCard(state: VaultUiState) {
    val healthy = state.allHealthy
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (healthy) AccentEmerald else AccentAmber),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (healthy) {
                        stringResource(R.string.vault_health_all_good)
                    } else {
                        stringResource(R.string.vault_health_reconnecting, state.reconnecting)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = stringResource(R.string.vault_slots_label).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.vault_slots_value, state.slotsUsed, state.slotsTotal),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            SlotMeter(filled = state.slotsUsed, total = state.slotsTotal)
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.vault_keystore_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AddNumberCard(
    slotsFull: Boolean,
    slotsTotal: Int,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.add_number_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (slotsFull) {
                        stringResource(R.string.add_number_full_subtitle, slotsTotal)
                    } else {
                        stringResource(R.string.add_number_subtitle)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
