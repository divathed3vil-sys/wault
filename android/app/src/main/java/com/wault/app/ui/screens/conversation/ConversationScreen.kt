package com.wault.app.ui.screens.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.wault.app.R
import com.wault.app.data.Account
import com.wault.app.data.Chat
import com.wault.app.data.Message
import com.wault.app.data.WaultRepository
import com.wault.app.ui.components.AccountAvatar
import com.wault.app.ui.components.EmptyState
import com.wault.app.ui.components.MessageBubble
import com.wault.app.ui.theme.parseHexAccent
import com.wault.app.data.DemoDataset
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ConversationUiState(
    val chat: Chat? = null,
    val account: Account? = null,
    /** Newest first — pairs with LazyColumn(reverseLayout = true). */
    val messages: List<Message> = emptyList(),
    val online: Boolean = false,
    val composer: String = "",
)

@HiltViewModel
class ConversationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: WaultRepository,
) : ViewModel() {

    private val chatId: String = checkNotNull(savedStateHandle["chatId"])
    private val composer = MutableStateFlow("")

    val uiState: StateFlow<ConversationUiState> = combine(
        repository.chats,
        repository.accounts,
        repository.messagesByChat,
        repository.presence,
        composer,
    ) { chats, accounts, messages, presence, draft ->
        val chat = chats.firstOrNull { it.id == chatId }
        ConversationUiState(
            chat = chat,
            account = accounts.firstOrNull { it.id == chat?.accountId },
            messages = messages[chatId].orEmpty().sortedByDescending { it.timestampEpochMs },
            online = presence[chatId] ?: false,
            composer = draft,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConversationUiState())

    init {
        // Opening a conversation clears its unread badge.
        repository.markRead(chatId)
    }

    fun setComposer(value: String) {
        composer.value = value
    }

    fun send() {
        val text = composer.value.trim()
        if (text.isEmpty()) return
        repository.send(chatId, text)
        composer.value = ""
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    viewModel: ConversationViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var lastFirstId by remember { mutableStateOf<String?>(null) }

    // Auto-scroll to the newest message when one actually arrives (not on
    // mere receipt upgrades).
    LaunchedEffect(state.messages) {
        val first = state.messages.firstOrNull()?.id
        if (first != null && first != lastFirstId) {
            listState.scrollToItem(0)
            lastFirstId = first
        }
    }

    val accent = parseHexAccent(state.account?.accentHex ?: DemoDataset.FALLBACK_ACCENT)
    val chat = state.chat

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AccountAvatar(
                        name = chat?.name.orEmpty(),
                        accentHex = state.account?.accentHex ?: DemoDataset.FALLBACK_ACCENT,
                        size = 36.dp,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = chat?.name.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = subtitleFor(state),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
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
        AccountChipStrip(accountName = state.account?.name.orEmpty(), accent = accent)
        if (state.messages.isEmpty()) {
            EmptyState(
                icon = Icons.AutoMirrored.Filled.Send,
                title = chat?.name.orEmpty(),
                subtitle = null,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        } else {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                items(state.messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        isGroupChat = chat?.isGroup == true,
                        accountAccent = accent,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
        ComposerBar(
            draft = state.composer,
            onDraftChange = viewModel::setComposer,
            onSend = viewModel::send,
        )
    }
}

/** Account-tinted strip under the header: whose number is sending this. */
@Composable
private fun AccountChipStrip(accountName: String, accent: Color) {
    if (accountName.isBlank()) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(accent.copy(alpha = 0.08f))
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = accountName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.conversation_via_wault),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun subtitleFor(state: ConversationUiState): String {
    val chat = state.chat ?: return ""
    return when {
        chat.isGroup -> stringResource(R.string.conversation_group_subtitle)
        state.online -> stringResource(R.string.conversation_online)
        else -> stringResource(R.string.conversation_offline)
    }
}

@Composable
private fun ComposerBar(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        TextField(
            value = draft,
            onValueChange = onDraftChange,
            placeholder = {
                Text(
                    text = stringResource(R.string.composer_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            maxLines = 3,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        FilledIconButton(
            onClick = onSend,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.send),
            )
        }
    }
}
