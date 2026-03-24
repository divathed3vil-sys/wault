import 'package:flutter/material.dart';
import 'package:wault/models/account.dart';
import 'package:wault/theme/wault_colors.dart';
import 'package:wault/utils/time_utils.dart';
import 'package:wault/widgets/liquid_glass_card.dart';

class AccountCard extends StatelessWidget {
  final Account account;
  final VoidCallback onTap;
  final VoidCallback? onLongPress;

  const AccountCard({
    super.key,
    required this.account,
    required this.onTap,
    this.onLongPress,
  });

  @override
  Widget build(BuildContext context) {
    final accentColor = _parseHexColor(account.accentColorHex);
    final stateInfo = _getStateInfo(account.state);
    final showLastSeen =
        account.state != 'ACTIVE' &&
        account.snapshotTimestamp != null &&
        account.snapshotTimestamp! > 0;

    return LiquidGlassCard(
      onTap: onTap,
      onLongPress: onLongPress,
      padding: const EdgeInsets.all(14.0),
      margin: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 6.0),
      borderRadius: 18.0,
      accentColor: accentColor,
      showGlow: account.state == 'ACTIVE',
      child: Row(
        children: [
          _buildAvatar(accentColor, stateInfo),
          const SizedBox(width: 14.0),
          Expanded(child: _buildContent(stateInfo, showLastSeen)),
          _buildTrailing(),
        ],
      ),
    );
  }

  Widget _buildAvatar(Color accentColor, _StateInfo stateInfo) {
    final initial = account.label.isNotEmpty
        ? account.label[0].toUpperCase()
        : '?';

    return Stack(
      children: [
        Container(
          width: 48.0,
          height: 48.0,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: accentColor.withOpacity(0.15),
            border: Border.all(color: accentColor.withOpacity(0.4), width: 1.5),
          ),
          child: Center(
            child: Text(
              initial,
              style: TextStyle(
                color: accentColor,
                fontSize: 20.0,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ),
        Positioned(
          right: 0,
          bottom: 0,
          child: Container(
            width: 14.0,
            height: 14.0,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: stateInfo.dotColor,
              border: Border.all(color: WaultColors.surface, width: 2.0),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildContent(_StateInfo stateInfo, bool showLastSeen) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          account.label,
          style: TextStyle(
            color: WaultColors.textPrimary,
            fontSize: 16.0,
            fontWeight: FontWeight.w600,
          ),
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
        const SizedBox(height: 4.0),
        Text(
          stateInfo.label,
          style: TextStyle(
            color: stateInfo.labelColor,
            fontSize: 13.0,
            fontWeight: FontWeight.w400,
          ),
        ),
        if (showLastSeen) ...[
          const SizedBox(height: 2.0),
          Text(
            'Last seen ${TimeUtils.formatRelative(account.snapshotTimestamp!)}',
            style: TextStyle(
              color: WaultColors.textTertiary,
              fontSize: 11.0,
              fontWeight: FontWeight.w400,
            ),
          ),
        ],
      ],
    );
  }

  Widget _buildTrailing() {
    if (account.unreadCount > 0) {
      final displayCount = account.unreadCount > 99
          ? '99+'
          : account.unreadCount.toString();

      return Container(
        padding: const EdgeInsets.symmetric(horizontal: 8.0, vertical: 4.0),
        decoration: BoxDecoration(
          color: WaultColors.primary,
          borderRadius: BorderRadius.circular(12.0),
        ),
        child: Text(
          displayCount,
          style: TextStyle(
            color: WaultColors.background,
            fontSize: 12.0,
            fontWeight: FontWeight.w700,
          ),
        ),
      );
    }

    return Icon(
      Icons.chevron_right_rounded,
      color: WaultColors.textTertiary,
      size: 24.0,
    );
  }

  _StateInfo _getStateInfo(String state) {
    switch (state) {
      case 'ACTIVE':
        return _StateInfo(
          label: 'Active',
          labelColor: WaultColors.primary,
          dotColor: WaultColors.primary,
        );
      case 'ERROR':
        return _StateInfo(
          label: 'Needs attention',
          labelColor: WaultColors.error,
          dotColor: WaultColors.error,
        );
      case 'COLD':
      default:
        return _StateInfo(
          label: 'Tap to open',
          labelColor: WaultColors.textSecondary,
          dotColor: WaultColors.textTertiary,
        );
    }
  }

  Color _parseHexColor(String hex) {
    try {
      final cleaned = hex.replaceFirst('#', '');
      if (cleaned.length == 6) {
        final value = int.parse('FF$cleaned', radix: 16);
        return Color(value);
      }
      if (cleaned.length == 8) {
        final value = int.parse(cleaned, radix: 16);
        return Color(value);
      }
    } catch (_) {}
    return WaultColors.primary;
  }
}

class _StateInfo {
  final String label;
  final Color labelColor;
  final Color dotColor;

  const _StateInfo({
    required this.label,
    required this.labelColor,
    required this.dotColor,
  });
}
