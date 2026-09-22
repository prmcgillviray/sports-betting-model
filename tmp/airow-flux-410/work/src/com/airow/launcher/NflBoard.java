package com.airow.launcher;

import java.util.*;
import java.util.regex.*;
import org.json.*;

/** Compact model board consumed directly from launcher-board.json, with HTML fallback. */
public final class NflBoard {
  public static final class Game {
    public final String gameId;
    public final String kickoff;
    public final String away;
    public final String home;
    public final double projectedAway;
    public final double projectedHome;
    public final String spread;
    public final double total;
    public final double homeWin;

    Game(String gameId, String kickoff, String away, String home,
         double projectedAway, double projectedHome, String spread,
         double total, double homeWin) {
      this.gameId = gameId == null ? "" : gameId;
      this.kickoff = kickoff == null ? "" : kickoff;
      this.away = away == null ? "" : away;
      this.home = home == null ? "" : home;
      this.projectedAway = projectedAway;
      this.projectedHome = projectedHome;
      this.spread = spread == null ? "" : spread;
      this.total = total;
      this.homeWin = homeWin;
    }

    public String projectedScore() {
      return away + " " + one(projectedAway) + "  —  " + home + " " + one(projectedHome);
    }

    JSONObject json() throws JSONException {
      JSONObject j = new JSONObject();
      j.put("game_id", gameId);
      j.put("kickoff_display", kickoff);
      j.put("away_team", away);
      j.put("home_team", home);
      j.put("projected_away_points", projectedAway);
      j.put("projected_home_points", projectedHome);
      j.put("model_spread", spread);
      j.put("model_total", total);
      j.put("home_win_probability", homeWin);
      return j;
    }

    static Game json(JSONObject j) {
      return new Game(
          j.optString("game_id", ""),
          j.optString("kickoff_display", j.optString("kickoff", "")),
          j.optString("away_team", j.optString("away", "")),
          j.optString("home_team", j.optString("home", "")),
          j.optDouble("projected_away_points", parseFirst(j.optString("projected_score", ""), 0)),
          j.optDouble("projected_home_points", parseFirst(j.optString("projected_score", ""), 1)),
          j.optString("model_spread", j.optString("spread", "")),
          j.optDouble("model_total", parseNumber(j.optString("total", ""))),
          j.optDouble("home_win_probability", parseProbability(j.optString("home_win", ""))));
    }
  }

  private static final Pattern ARTICLE = Pattern.compile("<article class=\\\"game-card\\\">(.*?)</article>", Pattern.DOTALL);
  private static final Pattern EYEBROW = Pattern.compile("<div class=\\\"eyebrow\\\">(.*?)</div>", Pattern.DOTALL);
  private static final Pattern MATCHUP = Pattern.compile("<div class=\\\"matchup\\\"><span>(.*?)</span><b>@</b><span>(.*?)</span></div>", Pattern.DOTALL);
  private static final Pattern SCORE = Pattern.compile("<div class=\\\"score\\\">(.*?)</div>", Pattern.DOTALL);
  private static final Pattern STATS = Pattern.compile(
      "<small>Model spread</small><strong>(.*?)</strong>.*?"
          + "<small>Model total</small><strong>(.*?)</strong>.*?"
          + "<small>Home win</small><strong>(.*?)</strong>", Pattern.DOTALL);
  private static final Pattern TAG = Pattern.compile("<[^>]+>");
  private static final Pattern NUMBER = Pattern.compile("-?[0-9]+(?:\\.[0-9]+)?");

  public final int season;
  public final int week;
  public final String generatedAt;
  public final String publishedAt;
  public final String bettingStatus;
  public final List<Game> games;

  private NflBoard(int season, int week, String generatedAt, String publishedAt,
                   String bettingStatus, List<Game> games) {
    this.season = season;
    this.week = week;
    this.generatedAt = generatedAt == null ? "" : generatedAt;
    this.publishedAt = publishedAt == null ? "" : publishedAt;
    this.bettingStatus = bettingStatus == null ? "" : bettingStatus;
    this.games = Collections.unmodifiableList(games);
  }

  public static NflBoard parseFeed(String json) throws Exception {
    JSONObject root = new JSONObject(json);
    if (root.optInt("schema_version", -1) != 1
        || !"airow_launcher_model_board".equals(root.optString("feed", "")))
      throw new IllegalArgumentException("Unsupported launcher feed");
    int season = root.getInt("season");
    int week = root.getInt("week");
    if (season < 2020 || week < 1 || week > 18) throw new IllegalArgumentException("Invalid feed period");
    JSONArray rows = root.getJSONArray("games");
    ArrayList<Game> games = new ArrayList<>();
    for (int i = 0; i < rows.length(); i++) {
      JSONObject row = rows.optJSONObject(i);
      if (row == null) continue;
      Game game = Game.json(row);
      if (valid(game)) games.add(game);
    }
    if (games.isEmpty()) throw new IllegalArgumentException("No launcher model games");
    return new NflBoard(season, week, root.optString("generated_at", ""),
        root.optString("published_at", ""), root.optString("betting_status", ""), games);
  }

  public static NflBoard parseHtml(String html) throws Exception {
    if (html == null || html.length() < 100) throw new IllegalArgumentException("Dashboard HTML missing");
    ArrayList<Game> games = new ArrayList<>();
    Matcher article = ARTICLE.matcher(html);
    while (article.find()) {
      String block = article.group(1);
      Matcher matchup = MATCHUP.matcher(block);
      Matcher stats = STATS.matcher(block);
      if (!matchup.find() || !stats.find()) continue;
      String away = clean(matchup.group(1));
      String home = clean(matchup.group(2));
      if (away.isEmpty() || home.isEmpty()) continue;
      String kickoff = match(EYEBROW, block).replace(" · MODEL LINE ONLY", "").trim();
      String score = match(SCORE, block);
      ArrayList<Double> scoreNumbers = numbers(score);
      double a = scoreNumbers.size() > 0 ? scoreNumbers.get(0) : Double.NaN;
      double h = scoreNumbers.size() > 1 ? scoreNumbers.get(1) : Double.NaN;
      Game game = new Game("", kickoff, away, home, a, h, clean(stats.group(1)),
          parseNumber(clean(stats.group(2))), parseProbability(clean(stats.group(3))));
      if (valid(game)) games.add(game);
    }
    if (games.isEmpty()) throw new IllegalArgumentException("No public model games found");
    return new NflBoard(0, 0, "", "", "", games);
  }

  public static NflBoard parseCache(String json) throws Exception { return parseFeed(json); }

  public String cacheJson() throws Exception {
    JSONObject root = new JSONObject();
    root.put("schema_version", 1);
    root.put("feed", "airow_launcher_model_board");
    root.put("season", season);
    root.put("week", week);
    root.put("generated_at", generatedAt);
    root.put("published_at", publishedAt);
    root.put("betting_status", bettingStatus);
    JSONArray rows = new JSONArray();
    for (Game game : games) rows.put(game.json());
    root.put("games", rows);
    return root.toString();
  }

  private static String match(Pattern p, String value) {
    Matcher m = p.matcher(value);
    return m.find() ? clean(m.group(1)) : "";
  }

  private static String clean(String value) {
    if (value == null) return "";
    String out = TAG.matcher(value).replaceAll("");
    out = out.replace("&amp;", "&").replace("&#39;", "'").replace("&quot;", "\"")
        .replace("&lt;", "<").replace("&gt;", ">").replace("&nbsp;", " ");
    return out.replaceAll("\\s+", " ").trim();
  }

  private static ArrayList<Double> numbers(String value) {
    ArrayList<Double> out = new ArrayList<>();
    Matcher m = NUMBER.matcher(value == null ? "" : value);
    while (m.find()) try { out.add(Double.parseDouble(m.group())); } catch (Exception ignored) {}
    return out;
  }

  private static double parseNumber(String value) {
    ArrayList<Double> n = numbers(value);
    return n.isEmpty() ? Double.NaN : n.get(0);
  }

  private static double parseProbability(String value) {
    double n = parseNumber(value);
    return n > 1 ? n / 100.0 : n;
  }

  private static double parseFirst(String value, int index) {
    ArrayList<Double> n = numbers(value);
    return n.size() > index ? n.get(index) : Double.NaN;
  }

  private static boolean valid(Game g) {
    return !g.away.isEmpty() && !g.home.isEmpty() && !g.spread.isEmpty()
        && Double.isFinite(g.projectedAway) && g.projectedAway >= 0
        && Double.isFinite(g.projectedHome) && g.projectedHome >= 0
        && Double.isFinite(g.total) && g.total > 0
        && Double.isFinite(g.homeWin) && g.homeWin >= 0 && g.homeWin <= 1;
  }

  private static String one(double n) { return String.format(Locale.US, "%.1f", n); }
}
