import 'package:flutter_test/flutter_test.dart';
import 'package:wault/main.dart';

void main() {
  testWidgets('WAult app smoke test', (WidgetTester tester) async {
    await tester.pumpWidget(const WaultApp());
    await tester.pump();

    expect(find.byType(WaultApp), findsOneWidget);
  });
}
