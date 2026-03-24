// lib/widgets/add_account_sheet.dart

import 'package:flutter/material.dart';
import 'package:wault/theme/wault_colors.dart';
import 'package:wault/widgets/liquid_glass_card.dart';

class AddAccountSheet extends StatefulWidget {
  final List<String> accentPalette;
  final String initialSelectedColorHex;
  final ValueChanged<String> onColorSelected;
  final ValueChanged<String> onCreate;

  const AddAccountSheet({
    super.key,
    required this.accentPalette,
    required this.initialSelectedColorHex,
    required this.onColorSelected,
    required this.onCreate,
  });

  @override
  State<AddAccountSheet> createState() => _AddAccountSheetState();
}

class _AddAccountSheetState extends State<AddAccountSheet> {
  late final TextEditingController _nameController;
  String? _errorText;

  @override
  void initState() {
    super.initState();
    _nameController = TextEditingController();
  }

  @override
  void dispose() {
    _nameController.dispose();
    super.dispose();
  }

  void _handleCreate() {
    final trimmed = _nameController.text.trim();
    if (trimmed.isEmpty) {
      setState(() {
        _errorText = 'Please enter an account name';
      });
      return;
    }

    setState(() {
      _errorText = null;
    });

    widget.onCreate(trimmed);
  }

  Color _parseHexColor(String hex) {
    try {
      final cleaned = hex.replaceFirst('#', '');
      if (cleaned.length == 6) {
        return Color(int.parse('FF$cleaned', radix: 16));
      }
      if (cleaned.length == 8) {
        return Color(int.parse(cleaned, radix: 16));
      }
    } catch (_) {}
    return WaultColors.primary;
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(
        left: 20,
        right: 20,
        top: 8,
        bottom: MediaQuery.of(context).viewInsets.bottom + 24,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildDragHandle(),
          const SizedBox(height: 16),
          _buildTitle(),
          const SizedBox(height: 20),
          _buildNameField(),
          const SizedBox(height: 20),
          _buildColorLabel(),
          const SizedBox(height: 10),
          _buildColorPalette(),
          const SizedBox(height: 24),
          _buildCreateButton(),
        ],
      ),
    );
  }

  Widget _buildDragHandle() {
    return Center(
      child: Container(
        width: 40,
        height: 4,
        decoration: BoxDecoration(
          color: WaultColors.glassBorder,
          borderRadius: BorderRadius.circular(2),
        ),
      ),
    );
  }

  Widget _buildTitle() {
    return const Text(
      'New Account',
      style: TextStyle(
        color: WaultColors.textPrimary,
        fontSize: 22,
        fontWeight: FontWeight.w600,
      ),
    );
  }

  Widget _buildNameField() {
    return TextField(
      controller: _nameController,
      style: const TextStyle(color: WaultColors.textPrimary, fontSize: 16),
      decoration: InputDecoration(
        hintText: 'Account name',
        errorText: _errorText,
      ),
      textCapitalization: TextCapitalization.words,
      textInputAction: TextInputAction.done,
      onChanged: (_) {
        if (_errorText != null) {
          setState(() {
            _errorText = null;
          });
        }
      },
      onSubmitted: (_) => _handleCreate(),
    );
  }

  Widget _buildColorLabel() {
    return const Text(
      'Color:',
      style: TextStyle(
        color: WaultColors.textSecondary,
        fontSize: 14,
        fontWeight: FontWeight.w500,
      ),
    );
  }

  Widget _buildColorPalette() {
    return Wrap(
      spacing: 12,
      runSpacing: 12,
      children: widget.accentPalette.map((hex) {
        final color = _parseHexColor(hex);
        final isSelected = hex == widget.initialSelectedColorHex;

        return GestureDetector(
          onTap: () => widget.onColorSelected(hex),
          child: Container(
            width: 36,
            height: 36,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: color,
              border: isSelected
                  ? Border.all(color: WaultColors.textPrimary, width: 2.5)
                  : Border.all(color: color.withOpacity(0.4), width: 1.5),
              boxShadow: isSelected
                  ? [
                      BoxShadow(
                        color: color.withOpacity(0.4),
                        blurRadius: 10,
                        spreadRadius: 1,
                      ),
                    ]
                  : null,
            ),
            child: isSelected
                ? const Icon(
                    Icons.check_rounded,
                    color: WaultColors.background,
                    size: 20,
                  )
                : null,
          ),
        );
      }).toList(),
    );
  }

  Widget _buildCreateButton() {
    return SizedBox(
      width: double.infinity,
      child: LiquidGlassCard(
        onTap: _handleCreate,
        padding: const EdgeInsets.symmetric(vertical: 14),
        borderRadius: 14,
        accentColor: _parseHexColor(widget.initialSelectedColorHex),
        showGlow: true,
        child: const Center(
          child: Text(
            'Create & Open',
            style: TextStyle(
              color: WaultColors.primary,
              fontSize: 16,
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
      ),
    );
  }
}
