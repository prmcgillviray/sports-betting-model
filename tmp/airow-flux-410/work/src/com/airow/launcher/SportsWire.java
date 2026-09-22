package com.airow.launcher;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Fast no-key sports wire plus the live NFL season/week identity. */
public final class SportsWire {
  private static final String API = "https://site.api.espn.com/apis/site/v2/sports/";

  public static final class Result {
    public final String text;
    public final int nflSeason;
    public final int nflWeek;
    Result(String text, int nflSeason, int nflWeek) {
      this.text = text; this.nflSeason = nflSeason; this.nflWeek = nflWeek;
    }
  }

  private static final class LeagueResult {
    final List<String> lines; final int season; final int week;
    LeagueResult(List<String> lines, int season, int week) { this.lines=lines; this.season=season; this.week=week; }
  }

  private SportsWire() {}

  public static Result fetch() throws Exception {
    return fetch(new LinkedHashSet<>(Arrays.asList("NFL","MLB","NHL","NBA")));
  }

  public static Result fetch(Set<String> selected) throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(5);
    try {
      ArrayList<Callable<LeagueResult>> tasks=new ArrayList<>();
      boolean nfl=selected.contains("NFL");
      tasks.add(() -> scores("NFL", API + "football/nfl/scoreboard?limit=50", nfl?7:0, true));
      if(selected.contains("MLB"))tasks.add(() -> scores("MLB", API + "baseball/mlb/scoreboard?limit=50", 2, false));
      if(selected.contains("NHL"))tasks.add(() -> scores("NHL", API + "hockey/nhl/scoreboard?limit=50", 2, false));
      if(selected.contains("NBA"))tasks.add(() -> scores("NBA", API + "basketball/nba/scoreboard?limit=50", 2, false));
      if(nfl)tasks.add(() -> new LeagueResult(news(API + "football/nfl/news?limit=4", 2), 0, 0));
      List<Future<LeagueResult>> futures = pool.invokeAll(tasks, 5, TimeUnit.SECONDS);
      ArrayList<String> items = new ArrayList<>();
      int season = 0, week = 0;
      for (Future<LeagueResult> future : futures) {
        if (future.isCancelled()) continue;
        try {
          LeagueResult r = future.get();
          items.addAll(r.lines);
          if (r.week > 0) { season = r.season; week = r.week; }
        } catch (Exception ignored) {}
      }
      if (items.isEmpty()) throw new IOException("Score source unavailable");
      return new Result(join(items, "   •   "), season, week);
    } finally { pool.shutdownNow(); }
  }

  private static LeagueResult scores(String league, String url, int limit, boolean nfl) {
    ArrayList<String> out = new ArrayList<>(); int season = 0, week = 0;
    try {
      JSONObject root = new JSONObject(download(url));
      if (nfl) {
        JSONObject s = root.optJSONObject("season");
        JSONObject w = root.optJSONObject("week");
        season = s == null ? 0 : s.optInt("year", 0);
        week = w == null ? 0 : w.optInt("number", 0);
      }
      JSONArray events = root.optJSONArray("events");
      if (events == null) return new LeagueResult(out, season, week);
      for (int i = 0; i < events.length() && out.size() < limit; i++) {
        JSONObject event = events.optJSONObject(i);
        JSONArray competitions = event == null ? null : event.optJSONArray("competitions");
        JSONObject competition = competitions == null ? null : competitions.optJSONObject(0);
        JSONArray competitors = competition == null ? null : competition.optJSONArray("competitors");
        if (competitors == null || competitors.length() < 2) continue;
        JSONObject home = null, away = null;
        for (int j = 0; j < competitors.length(); j++) {
          JSONObject c = competitors.optJSONObject(j);
          if (c == null) continue;
          if ("home".equals(c.optString("homeAway"))) home = c;
          else if ("away".equals(c.optString("homeAway"))) away = c;
        }
        if (home == null || away == null) continue;
        String homeName = abbr(home), awayName = abbr(away);
        String homeScore = home.optString("score", ""), awayScore = away.optString("score", "");
        JSONObject status = competition.optJSONObject("status");
        JSONObject type = status == null ? null : status.optJSONObject("type");
        String state = type == null ? "" : type.optString("state", "");
        String detail = type == null ? "" : type.optString("shortDetail", type.optString("detail", ""));
        String game;
        if ("pre".equals(state) || (homeScore.isEmpty() && awayScore.isEmpty()))
          game = league + "  " + awayName + " @ " + homeName + (detail.isEmpty() ? "" : "  " + detail);
        else
          game = league + "  " + awayName + " " + awayScore + "  " + homeName + " " + homeScore
              + (detail.isEmpty() ? "" : "  " + detail);
        out.add(game.trim());
      }
    } catch (Exception ignored) {}
    return new LeagueResult(out, season, week);
  }

  private static List<String> news(String url, int limit) {
    ArrayList<String> out = new ArrayList<>();
    try {
      JSONObject root = new JSONObject(download(url));
      JSONArray articles = root.optJSONArray("articles");
      if (articles == null) return out;
      for (int i = 0; i < Math.min(limit, articles.length()); i++) {
        JSONObject article = articles.optJSONObject(i);
        String headline = article == null ? "" : article.optString("headline", "").trim();
        if (!headline.isEmpty()) out.add("NFL NEWS  " + headline);
      }
    } catch (Exception ignored) {}
    return out;
  }

  private static String abbr(JSONObject competitor) {
    JSONObject team = competitor.optJSONObject("team");
    String value = team == null ? "TEAM" : team.optString("abbreviation", team.optString("shortDisplayName", "TEAM"));
    return value.toUpperCase(Locale.US);
  }

  private static String download(String url) throws Exception {
    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
    c.setConnectTimeout(3500); c.setReadTimeout(3500); c.setInstanceFollowRedirects(true);
    c.setUseCaches(false); c.setRequestProperty("Accept", "application/json");
    c.setRequestProperty("Accept-Language", "en-US,en;q=0.9"); c.setRequestProperty("Connection", "close");
    c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 16) AIROW-Home/4.0.0");
    try {
      int code = c.getResponseCode(); if (code != 200) throw new IOException("HTTP " + code);
      try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        byte[] buffer = new byte[8192]; int n;
        while ((n = in.read(buffer)) != -1) {
          if (out.size() + n > 2 * 1024 * 1024) throw new IOException("Response too large");
          out.write(buffer, 0, n);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
      }
    } finally { c.disconnect(); }
  }

  private static String join(List<String> values, String separator) {
    StringBuilder out = new StringBuilder();
    for (String value : values) { if (out.length() > 0) out.append(separator); out.append(value); }
    return out.toString();
  }
}
