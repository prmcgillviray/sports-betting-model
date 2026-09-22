package com.airow.launcher;

import java.time.Instant;

/** Freshness belongs to the displayed board, never to a separate status file. */
public final class ModelFreshness {
  private ModelFreshness() {}
  public static String describe(NflBoard board,long now,int season,int week) {
    if(board.week>0&&week>0&&(board.week!=week||(season>0&&board.season!=season)))return "different week · saved";
    try {
      long published=Instant.parse(board.publishedAt).toEpochMilli();
      long generated=Instant.parse(board.generatedAt).toEpochMilli();
      if(published>now+300000||generated>now+300000)return "timestamp invalid";
      if(now-published>NflSnapshot.OLD_AFTER_MS||now-generated>NflSnapshot.OLD_AFTER_MS)return "stale snapshot";
      return "recent publication";
    }catch(Exception e){return "freshness unknown";}
  }
}
