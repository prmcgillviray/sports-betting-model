package com.airow.launcher;

import java.time.Instant;

public final class ModelFreshnessTest {
  private static void check(boolean pass){if(!pass)throw new AssertionError("Freshness misrepresented");}
  private static NflBoard board(String time)throws Exception{
    return NflBoard.parseFeed("{\"schema_version\":1,\"feed\":\"airow_launcher_model_board\",\"season\":2026,\"week\":3,\"generated_at\":\""+time+"\",\"published_at\":\""+time+"\",\"games\":[{\"away_team\":\"NYG\",\"home_team\":\"LA\",\"projected_away_points\":20,\"projected_home_points\":24,\"model_total\":44,\"model_spread\":\"LA -4\",\"home_win_probability\":0.6}]}");
  }
  public static void main(String[] args)throws Exception{
    long now=Instant.parse("2026-09-22T12:00:00Z").toEpochMilli();
    check(ModelFreshness.describe(board("2026-09-22T11:00:00Z"),now,2026,3).equals("recent publication"));
    check(ModelFreshness.describe(board("2026-09-20T11:00:00Z"),now,2026,3).equals("stale snapshot"));
    check(ModelFreshness.describe(board(""),now,0,0).equals("freshness unknown"));
    check(ModelFreshness.describe(board("bad"),now,0,0).equals("freshness unknown"));
    check(ModelFreshness.describe(board("2026-09-23T11:00:00Z"),now,2026,3).equals("timestamp invalid"));
    check(ModelFreshness.describe(board("2026-09-22T11:00:00Z"),now,2026,4).equals("different week · saved"));
    System.out.println("6 board-specific freshness checks passed.");
  }
}
