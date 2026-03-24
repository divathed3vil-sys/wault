import 'package:flutter/material.dart';
import 'package:wault/theme/wault_colors.dart';
import 'package:wault/widgets/shield_logo.dart';
import 'package:wault/widgets/liquid_glass_card.dart';

class EmptyVault extends StatelessWidget {
  final VoidCallback onAddAccount;

  const EmptyVault({super.key, required this.onAddAccount});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 32.0),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ShieldLogo(size: 100.0, opacity: 0.4),
            const SizedBox(height: 32.0),
            Text(
              'Your Vault is Empty',
              style: TextStyle(
                color: WaultColors.textPrimary,
                fontSize: 22.0,
                fontWeight: FontWeight.w600,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 12.0),
            Text(
              'Add your first WhatsApp account to get started',
              style: TextStyle(
                color: WaultColors.textSecondary,
                fontSize: 15.0,
                fontWeight: FontWeight.w400,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 40.0),
            LiquidGlassCard(
              onTap: onAddAccount,
              padding: const EdgeInsets.symmetric(
                horizontal: 28.0,
                vertical: 14.0,
              ),
              borderRadius: 16.0,
              accentColor: WaultColors.primary,
              showGlow: true,
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(
                    Icons.add_rounded,
                    color: WaultColors.primary,
                    size: 22.0,
                  ),
                  const SizedBox(width: 10.0),
                  Text(
                    'Add Account',
                    style: TextStyle(
                      color: WaultColors.primary,
                      fontSize: 16.0,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
