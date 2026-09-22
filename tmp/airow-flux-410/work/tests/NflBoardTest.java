package com.airow.launcher;

public final class NflBoardTest {
  private static void check(boolean pass, String message) { if (!pass) throw new AssertionError(message); }
  public static void main(String[] args) throws Exception {
    String json = "{\"schema_version\":1,\"feed\":\"airow_launcher_model_board\",\"season\":2026,\"week\":2,"
        + "\"generated_at\":\"2026-09-20T17:00:00Z\",\"published_at\":\"2026-09-20T17:05:00Z\",\"betting_status\":\"NO BET\",\"games\":[{"
        + "\"game_id\":\"2026_02_DET_MIN\",\"kickoff_display\":\"Sun Sep 20 · 1:00 PM ET\",\"away_team\":\"DET\",\"home_team\":\"MIN\","
        + "\"projected_away_points\":27.4,\"projected_home_points\":23.1,\"model_spread\":\"DET -4.3\",\"model_total\":50.5,\"home_win_probability\":0.382}]}";
    NflBoard board=NflBoard.parseFeed(json);
    check(board.season==2026&&board.week==2,"period parse");check(board.games.size()==1,"game count");
    NflBoard.Game g=board.games.get(0);check(g.away.equals("DET")&&g.home.equals("MIN"),"matchup");
    check(Math.abs(g.projectedAway-27.4)<.001&&Math.abs(g.projectedHome-23.1)<.001,"score");
    check(g.spread.equals("DET -4.3")&&Math.abs(g.total-50.5)<.001,"lines");
    NflBoard cached=NflBoard.parseCache(board.cacheJson());check(cached.week==2&&cached.games.get(0).gameId.equals("2026_02_DET_MIN"),"cache round trip");
    rejects(json.replace("\"projected_away_points\":27.4,", ""));
    rejects(json.replace("\"model_total\":50.5", "\"model_total\":0"));
    rejects(json.replace("\"home_win_probability\":0.382", "\"home_win_probability\":1.5"));

    String html="<article class=\"game-card\"><div class=\"eyebrow\">Sun Sep 20 · 1:00 PM ET · MODEL LINE ONLY</div><div class=\"matchup\"><span>DET</span><b>@</b><span>MIN</span></div><div class=\"score\">DET 27.4 <i>—</i> MIN 23.1</div><div class=\"stat-grid\"><div><small>Model spread</small><strong>DET -4.3</strong></div><div><small>Model total</small><strong>50.5</strong></div><div><small>Home win</small><strong>38.2%</strong></div></div></article>";
    NflBoard fallback=NflBoard.parseHtml(html);check(fallback.games.size()==1,"html fallback");check(Math.abs(fallback.games.get(0).homeWin-.382)<.001,"html probability");
    System.out.println("NFL launcher feed parser checks passed.");
  }
  private static void rejects(String value) throws Exception {
    try { NflBoard.parseFeed(value); } catch (IllegalArgumentException expected) { return; }
    throw new AssertionError("Incomplete or invalid projection was displayed as real data");
  }
}
