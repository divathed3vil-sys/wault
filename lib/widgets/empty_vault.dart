// lib/widgets/empty_vault.dart

import 'package:flutter/material.dart';
import 'package:wault/theme/wault_colors.dart';
import 'package:wault/widgets/shield_logo.dart';

class EmptyVault extends StatelessWidget {
  final VoidCallback onAddAccount;

  const EmptyVault({super.key, required this.onAddAccount});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 40),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const ShieldLogo(size: 100, opacity: 0.30),
            const SizedBox(height: 32),
            const Text(
              'Your Vault is Empty',
              textAlign: TextAlign.center,
              style: TextStyle(
                color: WaultColors.textPrimary,
                fontSize: 22,
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 8),
            const Text(
              'Add your first WhatsApp account to get started',
              textAlign: TextAlign.center,
              style: TextStyle(
                color: WaultColors.textSecondary,
                fontSize: 14,
                height: 1.4,
              ),
            ),
            const SizedBox(height: 32),
            SizedBox(
              width: 220,
              height: 50,
              child: ElevatedButton.icon(
                onPressed: onAddAccount,
                icon: const Icon(Icons.add),
                label: const Text('Add Account'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: WaultColors.primary,
                  foregroundColor: Colors.white,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(14),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
