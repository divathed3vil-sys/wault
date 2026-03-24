import 'package:flutter/material.dart';
import 'package:wault/theme/wault_colors.dart';
//import 'package:wault/utils/constants.dart';
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

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(
        left: 20.0,
        right: 20.0,
        top: 8.0,
        bottom: MediaQuery.of(context).viewInsets.bottom + 24.0,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildDragHandle(),
          const SizedBox(height: 16.0),
          _buildTitle(),
          const SizedBox(height: 20.0),
          _buildNameField(),
          const SizedBox(height: 20.0),
          _buildColorLabel(),
          const SizedBox(height: 10.0),
          _buildColorPalette(),
          const SizedBox(height: 24.0),
          _buildCreateButton(),
        ],
      ),
    );
  }

  Widget _buildDragHandle() {
    return Center(
      child: Container(
        width: 40.0,
        height: 4.0,
        decoration: BoxDecoration(
          color: WaultColors.glassBorder,
          borderRadius: BorderRadius.circular(2.0),
        ),
      ),
    );
  }

  Widget _buildTitle() {
    return Text(
      'New Account',
      style: TextStyle(
        color: WaultColors.textPrimary,
        fontSize: 22.0,
        fontWeight: FontWeight.w600,
      ),
    );
  }

  Widget _buildNameField() {
    return TextField(
      controller: _nameController,
      style: TextStyle(color: WaultColors.textPrimary, fontSize: 16.0),
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
    return Text(
      'Color:',
      style: TextStyle(
        color: WaultColors.textSecondary,
        fontSize: 14.0,
        fontWeight: FontWeight.w500,
      ),
    );
  }

  Widget _buildColorPalette() {
    return Wrap(
      spacing: 12.0,
      runSpacing: 12.0,
      children: widget.accentPalette.map((hex) {
        final color = _parseHexColor(hex);
        final isSelected = hex == widget.initialSelectedColorHex;

        return GestureDetector(
          onTap: () => widget.onColorSelected(hex),
          child: Container(
            width: 36.0,
            height: 36.0,
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
                        blurRadius: 10.0,
                        spreadRadius: 1.0,
                      ),
                    ]
                  : null,
            ),
            child: isSelected
                ? Icon(
                    Icons.check_rounded,
                    color: WaultColors.background,
                    size: 20.0,
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
        padding: const EdgeInsets.symmetric(vertical: 14.0),
        borderRadius: 14.0,
        accentColor: _parseHexColor(widget.initialSelectedColorHex),
        showGlow: true,
        child: Center(
          child: Text(
            'Create & Open',
            style: TextStyle(
              color: WaultColors.primary,
              fontSize: 16.0,
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
      ),
    );
  }
}
