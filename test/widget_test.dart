// test/widget_test.dart

import 'package:flutter_test/flutter_test.dart';
import 'package:wault/main.dart';

void main() {
  testWidgets('WAult app smoke test', (WidgetTester tester) async {
    await tester.pumpWidget(const WaultApp());
    expect(find.byType(WaultApp), findsOneWidget);
  });
}
