// test/widget_test.dart

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:wault/screens/vault_screen.dart';

void main() {
  testWidgets('WAult vault screen smoke test', (WidgetTester tester) async {
    await tester.pumpWidget(const MaterialApp(home: VaultScreen()));

    expect(find.byType(VaultScreen), findsOneWidget);
  });
}
