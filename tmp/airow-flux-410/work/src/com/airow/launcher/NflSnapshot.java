package com.airow.launcher;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.json.JSONObject;

/** Publication freshness is separate from betting eligibility. */
public final class NflSnapshot {
  public static final long OLD_AFTER_MS = 6L * 60 * 60 * 1000;
  public final Instant published;
  public final Instant generated;
  public final String bettingStatus;
  public final String oddsStatus;

  private NflSnapshot(Instant p, Instant g, String b, String o) {
    published = p;
    generated = g;
    bettingStatus = b;
    oddsStatus = o;
  }

  public static NflSnapshot parse(String json, long now) throws Exception {
    JSONObject j = new JSONObject(json);
    int schema = j.getInt("schema_version");
    if (schema < 3 || schema > 5)
      throw new IllegalArgumentException("Unsupported status format");
    Instant p = Instant.parse(j.getString("published_at"));
    Instant g = Instant.parse(j.getString("manifest_generated_at"));
    if (p.toEpochMilli() > now + 300000 || g.toEpochMilli() > now + 300000)
      throw new IllegalArgumentException("Snapshot timestamp is in the future");
    String b = j.getString("betting_status").trim();
    String o = j.getString("live_odds_status").trim();
    if (b.isEmpty() || b.length() > 80 || o.length() > 100)
      throw new IllegalArgumentException("Invalid status");
    return new NflSnapshot(p, g, b, o);
  }

  public boolean old(long now) {
    return now - published.toEpochMilli() > OLD_AFTER_MS
        || now - generated.toEpochMilli() > OLD_AFTER_MS;
  }

  public String date() {
    return DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a", Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(published);
  }

  public String summary(long now) {
    return (old(now) ? "OLD SNAPSHOT" : "RECENT SNAPSHOT") + "  /  " + bettingStatus;
  }
}
