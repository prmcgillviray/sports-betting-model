package com.airow.launcher;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Bounds are the same values consumed by ApertureHome, not a second layout. */
public final class HomeLayoutTest {
  private static final String[] IDS={"brand","settings","gestureHint","date","clock","connect","create","explore","contextLabel","contextText","modelText","rule","mediaText","mediaControl","search"};
  private static final Set<String> BUTTONS=new HashSet<>(Arrays.asList("settings","connect","create","explore","contextText","modelText","mediaText","mediaControl","search"));
  private static void check(boolean pass,String message){if(!pass)throw new AssertionError(message);}
  public static void main(String[] args)throws Exception{
    int cases=0;
    for(float width:new float[]{320,360,384,412,480,600})for(float height:new float[]{480,660,780,860,960})for(float font:new float[]{1,1.3f,1.5f,2}){
      HomeLayout layout=new HomeLayout(width,height,font);
      for(String id:IDS){float[] b=layout.bounds(id);
        check(b[0]>=0&&b[1]>=0&&b[2]>0&&b[3]>0,"Invalid rectangle: "+id);
        check(b[0]+b[2]<=width&&b[1]+b[3]<=layout.height,"Off-screen control: "+id);
        if(BUTTONS.contains(id))check(b[2]>=48&&b[3]>=48,"Small target: "+id);
      }
      for(int i=0;i<IDS.length;i++)for(int j=i+1;j<IDS.length;j++){
        float[] a=layout.bounds(IDS[i]),b=layout.bounds(IDS[j]);
        boolean overlap=a[0]<b[0]+b[2]&&a[0]+a[2]>b[0]&&a[1]<b[1]+b[3]&&a[1]+a[3]>b[1];
        check(!overlap,"Overlapping controls: "+IDS[i]+" / "+IDS[j]);
      }cases++;
    }
    System.out.println(cases+" layout/font-size configurations: positive rectangles, no overlap, 48dp targets.");
    Files.createDirectories(Paths.get("output"));
    preview(412,860,1,"output/aperture-preview.html");
    preview(360,740,1.5f,"output/aperture-large-text.html");
  }
  private static void preview(float width,float height,float font,String path)throws Exception{
    HomeLayout layout=new HomeLayout(width,height,font);
    StringBuilder html=new StringBuilder("<!doctype html><meta charset='utf-8'><style>*{box-sizing:border-box}body{margin:0;background:#08090b;color:#f5f3ef;font-family:Arial,sans-serif}.scene{position:relative;overflow:hidden}.element{position:absolute;display:flex;align-items:center;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.art{position:absolute;overflow:hidden}.blade{position:absolute;transform:rotate(-18deg);background:linear-gradient(135deg,#17191d,#d8d6d0 25%,#55565a 48%,#f2eee5 68%,#25272b);border-radius:36% 12% 42% 18%}.blade.two{transform:rotate(26deg);background:linear-gradient(135deg,#111318,#86888c,#f3efe7 58%,#303237)}.core{position:absolute;background:linear-gradient(135deg,#ff3f28,#9c1409 55%,#240806);transform:rotate(32deg);border-radius:18px}</style>");
    html.append("<div class='scene' style='width:").append(width).append("px;height:").append(layout.height).append("px'>");
    float[] art=layout.bounds("art");
    html.append("<div class='art' style='").append(css(art)).append("'><div class='blade' style='left:18%;top:12%;width:64%;height:58%'></div><div class='blade two' style='left:44%;top:18%;width:44%;height:54%'></div><div class='core' style='left:35%;top:40%;width:30%;height:8%'></div></div>");
    String[] values={"AIROW / APERTURE","⋯","HOLD / BLOOM    DOUBLE TAP / ASK","Tuesday, September 22","7:26","LINK  /  PEOPLE + PLACES","MAKE  /  THOUGHTS + TOOLS","ROAM  /  APPS + SYSTEMS","LIVE / SIGNAL","Sports unavailable  ↗","Model unavailable · Scores unavailable","","Listen  ↗","▶","⌕   COMMAND / APP / ACTION"};
    int[] sizes={19,26,8,12,82,23,23,23,10,17,12,1,14,20,15};
    for(int i=0;i<IDS.length;i++){
      String id=IDS[i],extra="";String color="#f5f3ef";
      if(id.equals("brand"))extra="font-weight:700;letter-spacing:2.8px;";
      if(id.equals("gestureHint")){extra="font-weight:700;letter-spacing:1.2px;";color="#6f7178";}
      if(id.equals("clock"))extra="font-weight:300;letter-spacing:-5px;";
      if(Arrays.asList("date","modelText","mediaText").contains(id))color="#a4a5ab";
      if(id.equals("contextLabel")){color="#ff5a43";extra="font-weight:700;letter-spacing:1.6px;";}
      if(id.equals("rule"))extra="background:#2a2b2e;";
      if(id.equals("settings")||id.equals("mediaControl"))extra="justify-content:center;";
      if(id.equals("search")){extra="background:#111317;border:1px solid rgba(255,90,67,.45);border-radius:4px;padding:0 20px;";}
      html.append("<div class='element' style='").append(css(layout.bounds(id))).append("font-size:").append(sizes[i]*font).append("px;color:").append(color).append(";").append(extra).append("'>").append(values[i]).append("</div>");
    }
    html.append("</div>");Files.write(Paths.get(path),html.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String css(float[] b){return "left:"+b[0]+"px;top:"+b[1]+"px;width:"+b[2]+"px;height:"+b[3]+"px;";}
}
