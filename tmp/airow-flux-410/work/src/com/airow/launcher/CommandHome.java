package com.airow.launcher;

import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.os.*;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import java.util.*;

/** AIROW V6: dense premium command environment based on the original dashboard concept. */
public final class CommandHome extends FrameLayout {
  public interface Actions{void go(String action);}
  private static final int WHITE=0xfff5f3ef,MUTED=0xffa7a5ad,ORANGE=0xffff6b35,PURPLE=0xffb43cff,CYAN=0xff55d9ff,GLASS=0xd90a0d14;
  private final Actions actions; private final CommandBackdrop art; private final NeuralHaloView halo;
  private final TextClock clock; private final TextView date,nfl,model,media,mediaCtl,upNext,system;
  private float downX,downY; private boolean armed; private long lastTap;
  private final Runnable bloom=new Runnable(){public void run(){if(!armed)return;armed=false;performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);halo.bringToFront();halo.open(downX,downY);}};

  public CommandHome(Context c,Actions a){
    super(c);actions=a;setClickable(true);setBackgroundColor(0xff030406);
    art=new CommandBackdrop(c);addView(art,new LayoutParams(-1,-1));

    TextView brand=txt("AIROW",31,WHITE,true);brand.setLetterSpacing(.07f);add(brand,20,12,205,50);
    TextView motto=txt("CREATE  //  PLAY  //  PROGRESS",8,MUTED,true);motto.setLetterSpacing(.18f);add(motto,22,59,220,18);
    TextView side=txt("MORE THAN A LAUNCHER",9,ORANGE,true);side.setGravity(Gravity.RIGHT);add(side,245,18,105,34);

    clock=new TextClock(c);clock.setFormat12Hour("h:mm");clock.setFormat24Hour("HH:mm");clock.setTextColor(WHITE);clock.setTextSize(58);clock.setTypeface(Typeface.create("sans-serif-light",0));clock.setIncludeFontPadding(false);add(clock,18,82,210,78);
    date=txt("",11,WHITE,true);date.setLetterSpacing(.20f);add(date,22,155,190,22);

    String[] labels={"FOCUS","SPORTS","MUSIC","CREATE","EXPLORE"};
    String[] acts={"assistant","sports","music","create","apps"};
    for(int i=0;i<5;i++){TextView v=button(labels[i],acts[i],i==0?ORANGE:0xff70758a);add(v,12+i*69,202,64,42);}

    TextView nflTitle=txt("NFL  //  MODEL",9,PURPLE,true);nflTitle.setLetterSpacing(.14f);add(nflTitle,20,258,145,18);
    nfl=click("Loading live intelligence…",16,WHITE,"sports");add(nfl,20,280,160,55);
    model=click("Model syncing…",10,MUTED,"sports");add(model,20,337,160,42);
    panel(12,250,178,140,PURPLE);

    TextView musicTitle=txt("NOW PLAYING",9,ORANGE,true);musicTitle.setLetterSpacing(.14f);add(musicTitle,205,258,135,18);
    media=click("Listen  ↗",15,WHITE,"music");add(media,205,282,125,48);
    mediaCtl=click("▶",20,WHITE,"play");mediaCtl.setGravity(Gravity.CENTER);add(mediaCtl,286,337,44,42);
    panel(197,250,151,140,ORANGE);

    TextView nextTitle=txt("UP NEXT",9,PURPLE,true);nextTitle.setLetterSpacing(.15f);add(nextTitle,20,408,120,18);
    upNext=click("Capture a thought\nPlan the next move\nKeep momentum",11,WHITE,"create");upNext.setGravity(Gravity.TOP);upNext.setLineSpacing(dp(5),1);add(upNext,20,432,155,80);
    panel(12,400,178,122,PURPLE);

    TextView sysTitle=txt("SYSTEM",9,CYAN,true);sysTitle.setLetterSpacing(.15f);add(sysTitle,205,408,100,18);
    system=txt("BATTERY  --\nAIROW  READY\nMODEL  LIVE",10,WHITE,false);system.setGravity(Gravity.TOP);system.setLineSpacing(dp(5),1);add(system,205,432,125,80);
    panel(197,400,151,122,CYAN);

    TextView quote=txt("Good Things Take Time.",18,WHITE,false);quote.setTypeface(Typeface.create("cursive",Typeface.ITALIC));add(quote,18,540,170,46);
    TextView ask=click("A   Ask AIROW\n     Ideas. Answers. Action.","assistant".equals("")?12:12,WHITE,"assistant");ask.setPadding(dp(15),0,0,0);ask.setBackground(glass(PURPLE,24));add(ask,174,538,174,52);

    TextView phone=dock("☎","PHONE","phone");add(phone,18,610,58,70);
    TextView messages=dock("●●●","MESSAGES","messages");add(messages,87,610,58,70);
    TextView ai=dock("A","AIROW","assistant");ai.setTextColor(WHITE);ai.setBackground(glass(PURPLE,32));add(ai,156,600,68,80);
    TextView apps=dock("•••\n•••","APPS","apps");add(apps,235,610,58,70);
    TextView capture=dock("＋","CAPTURE","capture");add(capture,304,610,58,70);

    halo=new NeuralHaloView(c,actions::go);addView(halo,new LayoutParams(-1,-1));
  }

  private TextView txt(String s,int size,int color,boolean bold){TextView v=new TextView(getContext());v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setIncludeFontPadding(false);v.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));v.setSingleLine(false);return v;}
  private TextView click(String s,int size,int color,String action){TextView v=txt(s,size,color,false);v.setClickable(true);v.setFocusable(true);v.setOnClickListener(x->{x.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);actions.go(action);});return v;}
  private TextView button(String s,String action,int accent){TextView v=click(s,9,WHITE,action);v.setGravity(Gravity.CENTER);v.setTypeface(Typeface.create("sans-serif-medium",0));v.setBackground(glass(accent,10));return v;}
  private TextView dock(String glyph,String label,String action){TextView v=click(glyph+"\n"+label,10,WHITE,action);v.setGravity(Gravity.CENTER);v.setTypeface(Typeface.create("sans-serif-medium",0));v.setBackground(glass(0xff596070,12));return v;}
  private Drawable glass(int accent,int radius){GradientDrawable g=new GradientDrawable();g.setColor(GLASS);g.setCornerRadius(dp(radius));g.setStroke(dp(1),accent);return new RippleDrawable(ColorStateList.valueOf(0x44ffffff),g,null);}
  private void panel(int x,int y,int w,int h,int accent){View v=new View(getContext());GradientDrawable g=new GradientDrawable();g.setColor(0x08000000);g.setCornerRadius(dp(12));g.setStroke(dp(1),accent);v.setBackground(g);add(v,x,y,w,h);v.setClickable(false);v.setFocusable(false);}
  private void add(View v,int x,int y,int w,int h){LayoutParams lp=new LayoutParams(dp(w),dp(h));lp.leftMargin=dp(x);lp.topMargin=dp(y);addView(v,lp);}

  public void refreshDate(){date.setText(new java.text.SimpleDateFormat("EEE   MMM d",Locale.getDefault()).format(new Date()).toUpperCase(Locale.ROOT));updateSystem();}
  public void context(String headline,String detail){nfl.setText(headline);model.setText(detail);}
  public void media(String title,boolean playing){media.setText(title);mediaCtl.setText(playing?"Ⅱ":"▶");}
  private void updateSystem(){BatteryManager b=(BatteryManager)getContext().getSystemService(Context.BATTERY_SERVICE);int pct=b==null?-1:b.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);system.setText("BATTERY  "+(pct<0?"--":pct+"%")+"\nAIROW  READY\nMODEL  LIVE");}

  @Override protected void onMeasure(int ws,int hs){
    int w=MeasureSpec.getSize(ws),h=MeasureSpec.getSize(hs);setMeasuredDimension(w,h);
    for(int i=0;i<getChildCount();i++){
      View v=getChildAt(i);LayoutParams lp=(LayoutParams)v.getLayoutParams();
      int vw=lp.width<0?w:lp.width,vh=lp.height<0?h:lp.height;
      v.measure(MeasureSpec.makeMeasureSpec(vw,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(vh,MeasureSpec.EXACTLY));
    }
  }
  @Override protected void onLayout(boolean changed,int l,int t,int r,int b){
    for(int i=0;i<getChildCount();i++){View v=getChildAt(i);LayoutParams lp=(LayoutParams)v.getLayoutParams();v.layout(lp.leftMargin,lp.topMargin,lp.leftMargin+v.getMeasuredWidth(),lp.topMargin+v.getMeasuredHeight());}
  }

  @Override public boolean dispatchTouchEvent(MotionEvent e){
    if(halo.isOpen()){if(e.getActionMasked()==MotionEvent.ACTION_MOVE)halo.track(e.getX(),e.getY());else if(e.getActionMasked()==MotionEvent.ACTION_UP)halo.release(e.getX(),e.getY());else if(e.getActionMasked()==MotionEvent.ACTION_CANCEL)halo.close();return true;}
    if(e.getActionMasked()==MotionEvent.ACTION_DOWN){downX=e.getX();downY=e.getY();armed=true;postDelayed(bloom,430);}
    else if(e.getActionMasked()==MotionEvent.ACTION_MOVE&&Math.hypot(e.getX()-downX,e.getY()-downY)>dp(14)){armed=false;removeCallbacks(bloom);}
    else if(e.getActionMasked()==MotionEvent.ACTION_UP){armed=false;removeCallbacks(bloom);if(Math.hypot(e.getX()-downX,e.getY()-downY)<dp(14)){long now=System.currentTimeMillis();if(now-lastTap<320){lastTap=0;actions.go("assistant");return true;}lastTap=now;}}
    else if(e.getActionMasked()==MotionEvent.ACTION_CANCEL){armed=false;removeCallbacks(bloom);}
    return super.dispatchTouchEvent(e);
  }
  @Override protected void onDetachedFromWindow(){armed=false;removeCallbacks(bloom);halo.close();super.onDetachedFromWindow();}
  private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
