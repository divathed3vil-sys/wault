class TimeUtils {
  TimeUtils._();

  static String formatRelative(int timestampMs) {
    if (timestampMs <= 0) {
      return 'never';
    }

    final now = DateTime.now().millisecondsSinceEpoch;
    final diffMs = now - timestampMs;

    if (diffMs < 0) {
      return 'just now';
    }

    final seconds = diffMs ~/ 1000;
    final minutes = seconds ~/ 60;
    final hours = minutes ~/ 60;
    final days = hours ~/ 24;

    if (seconds < 60) {
      return 'just now';
    }

    if (minutes < 60) {
      if (minutes == 1) {
        return '1 min ago';
      }
      return '$minutes mins ago';
    }

    if (hours < 24) {
      if (hours == 1) {
        return '1 hr ago';
      }
      return '$hours hrs ago';
    }

    if (days == 1) {
      return '1 day ago';
    }

    if (days < 30) {
      return '$days days ago';
    }

    final months = days ~/ 30;
    if (months == 1) {
      return '1 month ago';
    }

    if (months < 12) {
      return '$months months ago';
    }

    final years = months ~/ 12;
    if (years == 1) {
      return '1 year ago';
    }

    return '$years years ago';
  }

  static String formatRelativeOrDefault(int? timestampMs, String fallback) {
    if (timestampMs == null || timestampMs <= 0) {
      return fallback;
    }
    return formatRelative(timestampMs);
  }

  static int nowMs() {
    return DateTime.now().millisecondsSinceEpoch;
  }
}
