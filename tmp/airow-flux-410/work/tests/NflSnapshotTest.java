package com.airow.launcher;

import java.time.Instant;

/** Run with the JVM org.json implementation; no Android device required. */
public final class NflSnapshotTest {
  private static final long NOW = Instant.parse("2026-09-20T14:00:00Z").toEpochMilli();
  private static int checks = 0;

  private static String json(String pub, String generated, String status) {
    return "{\"schema_version\":3,\"published_at\":\""
        + pub
        + "\",\"manifest_generated_at\":\""
        + generated
        + "\",\"betting_status\":\""
        + status
        + "\",\"live_odds_status\":\"DEFERRED_NOT_CONFIGURED\"}";
  }

  private static void check(boolean pass) {
    if (!pass) throw new AssertionError("Check " + (checks + 1) + " failed");
    checks++;
  }

  private static void rejects(String value) {
    try {
      NflSnapshot.parse(value, NOW);
    } catch (Exception expected) {
      checks++;
      return;
    }
    throw new AssertionError("Invalid input was accepted");
  }

  public static void main(String[] args) throws Exception {
    String fresh = json("2026-09-20T13:00:00Z", "2026-09-20T12:59:00Z", "NO BET");
    NflSnapshot recent = NflSnapshot.parse(fresh, NOW);
    check(!recent.old(NOW));
    check(recent.bettingStatus.equals("NO BET"));
    check(!NflSnapshot.parse(fresh.replace("\"schema_version\":3", "\"schema_version\":4"), NOW).old(NOW));
    check(!NflSnapshot.parse(fresh.replace("\"schema_version\":3", "\"schema_version\":5"), NOW).old(NOW));
    check(recent.summary(NOW).equals("RECENT SNAPSHOT  /  NO BET"));
    check(
        NflSnapshot.parse(
                json("2026-09-10T02:22:57.500216+00:00", "2026-09-10T02:23:35.715569Z", "NO BET"),
                NOW)
            .old(NOW));
    check(
        NflSnapshot.parse(json("2026-09-20T13:00:00Z", "2026-09-10T12:00:00Z", "NO BET"), NOW)
            .old(NOW));
    rejects(fresh.replace("\"schema_version\":3", "\"schema_version\":99"));
    rejects(json("2026-09-21T13:00:00Z", "2026-09-20T12:00:00Z", "NO BET"));
    rejects(json("bad date", "2026-09-20T12:00:00Z", "NO BET"));
    rejects(json("2026-09-20T13:00:00Z", "2026-09-20T12:00:00Z", ""));
    rejects("{}");
    rejects("not json");
    System.out.println(checks + " freshness and invalid-data checks passed.");
  }
}
