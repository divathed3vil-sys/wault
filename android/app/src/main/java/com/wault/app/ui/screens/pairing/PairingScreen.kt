package com.wault.app.ui.screens.pairing

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.wault.app.R
import com.wault.app.data.Account
import com.wault.app.data.WaultRepository
import com.wault.app.domain.engine.PairingStage
import com.wault.app.ui.theme.AccentRose
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class PairingUiState(
    val account: Account? = null,
    val stage: PairingStage? = null,
)

@HiltViewModel
class PairingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: WaultRepository,
) : ViewModel() {

    private val accountId: String = checkNotNull(savedStateHandle["accountId"])

    val uiState: StateFlow<PairingUiState> = combine(
        repository.accounts,
        repository.pairingStages,
    ) { accounts, stages ->
        PairingUiState(
            account = accounts.firstOrNull { it.id == accountId },
            stage = stages[accountId],
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PairingUiState())

    /** MockEngine's stand-in for the camera scanning the QR code. */
    fun confirmScan() = repository.confirmPairingScan(accountId)

    fun retry() = repository.retryPairing(accountId)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingScreen(
    viewModel: PairingViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val stage = state.stage ?: PairingStage.Waiting

    // Success holds for a beat so the check is visible, then pops back to Vault.
    LaunchedEffect(state.stage) {
        if (state.stage == PairingStage.Linked) {
            delay(1_000)
            onClose()
        }
    }

    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        TopAppBar(
            title = { Text(text = stringResource(R.string.pairing_title)) },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
        )
        StepIndicator(currentStage = stage, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
        Crossfade(
            targetState = stage,
            animationSpec = tween(220),
            label = "pairing-stage",
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { current ->
            when (current) {
                PairingStage.Waiting -> StageCenter {
                    CircularProgressIndicator(
                        modifier = Modifier.size(30.dp),
                        strokeWidth = 3.dp,
                    )
                    StageLabel(text = stringResource(R.string.pairing_waiting))
                }
                PairingStage.QrReady -> QrStage(
                    accountName = state.account?.name.orEmpty(),
                    onSimulateScan = viewModel::confirmScan,
                )
                PairingStage.Linking -> StageCenter {
                    CircularProgressIndicator(
                        modifier = Modifier.size(30.dp),
                        strokeWidth = 3.dp,
                    )
                    StageLabel(text = stringResource(R.string.pairing_linking))
                }
                PairingStage.Linked -> StageCenter {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(52.dp),
                    )
                    StageLabel(text = stringResource(R.string.pairing_linked_title), title = true)
                    StageLabel(text = stringResource(R.string.pairing_linked_body))
                }
                PairingStage.Failed -> StageCenter {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = AccentRose,
                        modifier = Modifier.size(52.dp),
                    )
                    StageLabel(text = stringResource(R.string.pairing_failed_title), title = true)
                    StageLabel(text = stringResource(R.string.pairing_failed_body))
                    Button(onClick = viewModel::retry) {
                        Text(text = stringResource(R.string.pairing_retry))
                    }
                }
            }
        }
    }
}

/** Three quiet dots, mirroring the web pairing flow's step indicator. */
@Composable
private fun StepIndicator(currentStage: PairingStage, modifier: Modifier = Modifier) {
    val step = when (currentStage) {
        PairingStage.Waiting -> 1
        PairingStage.QrReady, PairingStage.Linking -> 2
        PairingStage.Linked, PairingStage.Failed -> 3
    }
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth(),
    ) {
        repeat(3) { index ->
            val dot = index + 1
            val color = when {
                dot < step -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                dot == step -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outline
            }
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Composable
private fun QrStage(
    accountName: String,
    onSimulateScan: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = accountName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(248.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(28.dp)),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.QrCode2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(104.dp),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.pairing_qr_caption),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 28.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.pairing_qr_instruction),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(22.dp))
        Button(
            onClick = onSimulateScan,
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(text = stringResource(R.string.pairing_simulate_scan))
        }
    }
}

@Composable
private fun StageCenter(content: @Composable () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun StageLabel(text: String, title: Boolean = false) {
    Text(
        text = text,
        style = if (title) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
        color = if (title) {
            MaterialTheme.colorScheme.onBackground
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 10.dp),
    )
}
