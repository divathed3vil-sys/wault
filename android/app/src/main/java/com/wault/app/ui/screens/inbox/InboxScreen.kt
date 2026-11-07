package com.wault.app.ui.screens.inbox

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.wault.app.R
import com.wault.app.data.Chat
import com.wault.app.data.WaultRepository
import com.wault.app.ui.components.ChatRow
import com.wault.app.ui.components.EmptyState
import com.wault.app.ui.theme.parseHexAccent
import com.wault.app.util.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** One account-filter chip. [accentHex] is null for the "All" chip. */
data class InboxChipUi(
    val id: String,
    val label: String,
    val accentHex: String?,
    val unread: Int,
)

data class InboxRowUi(val chat: Chat, val accentHex: String)

data class InboxUiState(
    val chips: List<InboxChipUi> = emptyList(),
    val rows: List<InboxRowUi> = emptyList(),
    val query: String = "",
    val filter: String = FILTER_ALL,
) {
    val hasMultipleAccounts: Boolean get() = chips.size > 2
}

@HiltViewModel
class InboxViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: WaultRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(savedStateHandle.get<String>(KEY_FILTER) ?: FILTER_ALL)
    private val query = MutableStateFlow("")

    val uiState: StateFlow<InboxUiState> = combine(
        repository.accounts,
        repository.chats,
        filter,
        query,
    ) { accounts, chats, activeFilter, activeQuery ->
        val chips = buildList {
            add(
                InboxChipUi(
                    id = FILTER_ALL,
                    label = "",
                    accentHex = null,
                    unread = accounts.sumOf { it.unreadTotal },
                ),
            )
            accounts.forEach { account ->
                add(
                    InboxChipUi(
                        id = account.id,
                        label = Formatters.shortAccountName(account.name),
                        accentHex = account.accentHex,
                        unread = account.unreadTotal,
                    ),
                )
            }
        }
        val rows = chats.asSequence()
            .filter { chat -> activeFilter == FILTER_ALL || chat.accountId == activeFilter }
            .filter { chat ->
                val q = activeQuery.trim()
                q.isEmpty() || chat.name.contains(q, ignoreCase = true) ||
                    chat.snippet.contains(q, ignoreCase = true)
            }
            .sortedByDescending { it.lastTs }
            .map { chat ->
                InboxRowUi(
                    chat = chat,
                    accentHex = accounts.firstOrNull { it.id == chat.accountId }?.accentHex
                        ?: FALLBACK_ACCENT,
                )
            }
            .toList()
        InboxUiState(chips = chips, rows = rows, query = activeQuery, filter = activeFilter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InboxUiState())

    fun setFilter(id: String) {
        filter.value = id
        // No savedStateHandle write: the demo keeps filters in memory per entry.
    }

    fun setQuery(value: String) {
        query.value = value
    }

    companion object {
        const val FILTER_ALL = "all"
        private const val KEY_FILTER = "filter"
        private const val FALLBACK_ACCENT = "#10B981"
    }
}

@Composable
fun InboxScreen(
    viewModel: InboxViewModel,
    onOpenChat: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        Text(
            text = stringResource(R.string.inbox_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
        if (state.hasMultipleAccounts) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                modifier = Modifier.padding(bottom = 10.dp),
            ) {
                items(state.chips, key = { it.id }) { chip ->
                    AccountFilterChip(
                        chip = chip,
                        selected = state.filter == chip.id,
                        onClick = { viewModel.setFilter(chip.id) },
                    )
                }
            }
        }
        SearchField(
            query = state.query,
            onQueryChange = viewModel::setQuery,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        if (state.rows.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.SearchOff,
                title = stringResource(R.string.inbox_empty_title),
                subtitle = stringResource(R.string.inbox_empty_subtitle),
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                items(state.rows, key = { it.chat.id }) { row ->
                    ChatRow(
                        chat = row.chat,
                        accentHex = row.accentHex,
                        onOpen = { onOpenChat(row.chat.id) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountFilterChip(
    chip: InboxChipUi,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = chip.accentHex?.let { parseHexAccent(it) }
    val label = if (chip.unread > 0) {
        "${chip.label.ifEmpty { stringResource(R.string.inbox_chip_all) }} · ${chip.unread}"
    } else {
        chip.label.ifEmpty { stringResource(R.string.inbox_chip_all) }
    }
    val backgroundColor = when {
        selected && accent != null -> accent.copy(alpha = 0.14f)
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when {
        selected && accent != null -> accent
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = when {
        selected && accent != null -> accent.copy(alpha = 0.4f)
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.outline
    }
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = {
            Text(
                text = stringResource(R.string.inbox_search_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .heightIn(min = 48.dp),
    )
}
