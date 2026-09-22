package com.airow.launcher;

import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * AIROW V6 Command Environment. Dense, useful dashboard over a static hero environment.
 * Native controls remain accessible; motion is interaction-only.
 */
public final class CommandEnvironmentHome extends FrameLayout {
  public interface Actions { void go(String action); }

  private static final int WHITE=0xfff5f4ef, MUTED=0xff9b9da6, ORANGE=0xffff6b2c,
      PURPLE=0xffa64dff, CYAN=0xff5bdcff, MAGENTA=0xffff4fd8, PANEL=0xdd0b0e14;

  private final Actions actions;
  private final SharedPreferences prefs;
  private final CommandBackdropView backdrop;
  private final NeuralHaloView halo;
  private final TextView brand,crown,motto,date,clock,sideTag,scoreTitle,scoreHeadline,scoreModel,
      mediaTitle,mediaSub,tasksTitle,task1,task2,task3,systemTitle,battery,storage,systemMode,
      quote,askTitle,askSub,bottomHint;
  private final TextView[] modeButtons=new TextView[5];
  private final LinearLayout modes,sportsCard,musicCard,tasksCard,systemCard,askBar,dock;
  private final TraceView trace;
  private float downX,downY;
  private boolean longPressArmed;
  private long lastTap;
  private final Runnable longPress=new Runnable(){
    @Override public void run(){
      if(!longPressArmed||halo.isOpen())return;
      longPressArmed=false;
      performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
      backdrop.pulse();halo.bringToFront();halo.open(downX,downY);
    }
  };

  public CommandEnvironmentHome(Context c,Actions actions){
    super(c);this.actions=actions;this.prefs=c.getSharedPreferences("airow",Context.MODE_PRIVATE);
    setBackgroundColor(0xff040507);setClickable(true);

    backdrop=new CommandBackdropView(c);addView(backdrop,new LayoutParams(-1,-1));

    brand=label("AIROW",34,WHITE,true);brand.setTypeface(Typeface.create("sans-serif-black",Typeface.ITALIC));brand.setLetterSpacing(.03f);
    crown=label("♛",22,ORANGE,true);crown.setGravity(Gravity.CENTER);
    motto=label("CREATE  //  PLAY  //  PROGRESS",9,0xffc1c2c8,true);motto.setLetterSpacing(.19f);
    date=label("",12,WHITE,true);date.setLetterSpacing(.16f);
    clock=label("",62,WHITE,true);clock.setTypeface(Typeface.create("sans-serif-black",Typeface.ITALIC));clock.setLetterSpacing(-.055f);
    sideTag=label("AI\nALWAYS ON\nYOUR SIDE",10,WHITE,true);sideTag.setSingleLine(false);sideTag.setLineSpacing(0,1.08f);sideTag.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);sideTag.setLetterSpacing(.12f);

    modes=new LinearLayout(c);modes.setOrientation(LinearLayout.HORIZONTAL);modes.setGravity(Gravity.CENTER_VERTICAL);addView(modes);
    modeButtons[0]=mode("◎  FOCUS","focus",ORANGE,true);
    modeButtons[1]=mode("◈  SPORTS","sports",WHITE,false);
    modeButtons[2]=mode("♫  MUSIC","music",WHITE,false);
    modeButtons[3]=mode("◇  CREATE","create",WHITE,false);
    modeButtons[4]=mode("◉  EXPLORE","apps",WHITE,false);
    for(TextView v:modeButtons)modes.addView(v,new LinearLayout.LayoutParams(0,-1,1));

    sportsCard=card();
    scoreTitle=cardLabel(sportsCard,"NFL  //  SIGNAL",11,PURPLE,true);
    scoreHeadline=cardLabel(sportsCard,"Scores unavailable",21,WHITE,true);
    scoreModel=cardLabel(sportsCard,"Model unavailable",11,MUTED,false);
    View sportLine=new View(c);sportLine.setBackground(energyBar(PURPLE,MAGENTA));sportsCard.addView(sportLine,new LinearLayout.LayoutParams(-1,dp(4)));
    TextView sportFooter=cardLabel(sportsCard,"TAP FOR LIVE + MODEL",9,0xffc6c8d0,true);sportFooter.setLetterSpacing(.12f);
    sportsCard.setOnClickListener(v->actions.go("sports"));

    musicCard=card();
    LinearLayout musicTop=new LinearLayout(c);musicTop.setGravity(Gravity.CENTER_VERTICAL);musicCard.addView(musicTop,new LinearLayout.LayoutParams(-1,0,1));
    TextView disc=labelDetached("◉",52,PURPLE,true);disc.setGravity(Gravity.CENTER);musicTop.addView(disc,new LinearLayout.LayoutParams(dp(72),-1));
    LinearLayout musicText=new LinearLayout(c);musicText.setOrientation(LinearLayout.VERTICAL);musicText.setGravity(Gravity.CENTER_VERTICAL);musicTop.addView(musicText,new LinearLayout.LayoutParams(0,-1,1));
    mediaTitle=labelDetached("Listen",17,WHITE,true);mediaTitle.setSingleLine(true);mediaTitle.setEllipsize(TextUtils.TruncateAt.END);musicText.addView(mediaTitle);
    mediaSub=labelDetached("Spotify ready",11,MUTED,false);musicText.addView(mediaSub);
    TextView musicPlay=labelDetached("▶",26,WHITE,true);musicPlay.setGravity(Gravity.CENTER);musicPlay.setOnClickListener(v->actions.go("play"));musicTop.addView(musicPlay,new LinearLayout.LayoutParams(dp(52),-1));
    View musicLine=new View(c);musicLine.setBackground(energyBar(PURPLE,CYAN));musicCard.addView(musicLine,new LinearLayout.LayoutParams(-1,dp(4)));
    musicCard.setOnClickListener(v->actions.go("music"));

    tasksCard=card();
    tasksTitle=cardLabel(tasksCard,"UP NEXT",11,PURPLE,true);tasksTitle.setLetterSpacing(.16f);
    task1=taskLine(tasksCard);task2=taskLine(tasksCard);task3=taskLine(tasksCard);
    tasksCard.setOnClickListener(v->actions.go("focus"));

    systemCard=card();
    systemTitle=cardLabel(systemCard,"SYSTEM",11,MUTED,true);systemTitle.setLetterSpacing(.17f);
    battery=cardLabel(systemCard,"POWER  —",13,WHITE,true);
    storage=cardLabel(systemCard,"STORAGE  —",13,WHITE,true);
    systemMode=cardLabel(systemCard,"AIROW  //  NOMINAL",10,CYAN,true);
    trace=new TraceView(c);systemCard.addView(trace,new LinearLayout.LayoutParams(-1,0,1));

    quote=label("Good Things\nTake Time.",25,WHITE,false);quote.setSingleLine(false);quote.setTypeface(Typeface.create("cursive",Typeface.ITALIC));quote.setLineSpacing(dp(-3),.93f);

    askBar=card();askBar.setOrientation(LinearLayout.HORIZONTAL);askBar.setGravity(Gravity.CENTER_VERTICAL);askBar.setPadding(dp(10),dp(7),dp(10),dp(7));
    TextView ai=labelDetached("A",28,WHITE,true);ai.setGravity(Gravity.CENTER);ai.setBackground(ring(PURPLE));askBar.addView(ai,new LinearLayout.LayoutParams(dp(58),dp(58)));
    LinearLayout askText=new LinearLayout(c);askText.setOrientation(LinearLayout.VERTICAL);askText.setGravity(Gravity.CENTER_VERTICAL);askText.setPadding(dp(12),0,0,0);askBar.addView(askText,new LinearLayout.LayoutParams(0,-1,1));
    askTitle=labelDetached("Ask AIROW",16,WHITE,true);askText.addView(askTitle);
    askSub=labelDetached("Ideas. Answers. Action.",10,0xffb9bbc5,false);askText.addView(askSub);
    TextView wave=labelDetached("≋",22,PURPLE,true);wave.setGravity(Gravity.CENTER);askBar.addView(wave,new LinearLayout.LayoutParams(dp(48),-1));
    askBar.setOnClickListener(v->actions.go("assistant"));

    dock=new LinearLayout(c);dock.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);addView(dock);
    dock.addView(dockButton("☎","PHONE","phone",false),new LinearLayout.LayoutParams(0,-1,1));
    dock.addView(dockButton("●","MESSAGES","messages",false),new LinearLayout.LayoutParams(0,-1,1));
    dock.addView(dockButton("A","AI","assistant",true),new LinearLayout.LayoutParams(0,-1,1));
    dock.addView(dockButton("⠿","APPS","apps",false),new LinearLayout.LayoutParams(0,-1,1));
    dock.addView(dockButton("⊕","CAPTURE","capture",false),new LinearLayout.LayoutParams(0,-1,1));

    bottomHint=label("SWIPE UP FOR APPS  •  HOLD ANYWHERE FOR COMMAND HALO",8,0xff737681,true);bottomHint.setGravity(Gravity.CENTER);bottomHint.setLetterSpacing(.12f);

    halo=new NeuralHaloView(c,actions::go);addView(halo,new LayoutParams(-1,-1));

    refreshDate();refreshTasks();refreshSystem();
  }

  private TextView mode(String text,String action,int color,boolean active){
    TextView v=labelDetached(text,9,color,true);v.setGravity(Gravity.CENTER);v.setSingleLine(true);v.setLetterSpacing(.08f);
    GradientDrawable g=new GradientDrawable();g.setColor(0xcc0c1017);g.setCornerRadius(dp(10));g.setStroke(dp(active?2:1),active?ORANGE:0xff252b36);
    v.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33ffffff),g,null));v.setOnClickListener(x->actions.go(action));return v;
  }

  private LinearLayout card(){
    LinearLayout box=new LinearLayout(getContext());box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(14),dp(12),dp(14),dp(12));box.setBackground(glassCard());addView(box);return box;
  }

  private TextView cardLabel(LinearLayout parent,String text,int size,int color,boolean bold){
    TextView v=labelDetached(text,size,color,bold);parent.addView(v,new LinearLayout.LayoutParams(-1,-2));return v;
  }

  private TextView taskLine(LinearLayout parent){
    TextView v=labelDetached("•  Empty slot",12,WHITE,false);v.setSingleLine(true);v.setEllipsize(TextUtils.TruncateAt.END);v.setPadding(0,dp(3),0,dp(3));parent.addView(v,new LinearLayout.LayoutParams(-1,0,1));return v;
  }

  private TextView label(String text,int size,int color,boolean bold){TextView v=labelDetached(text,size,color,bold);addView(v);return v;}
  private TextView labelDetached(String text,int size,int color,boolean bold){
    TextView v=new TextView(getContext());v.setText(text);v.setTextSize(size);v.setTextColor(color);v.setIncludeFontPadding(false);v.setGravity(Gravity.CENTER_VERTICAL);v.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));return v;
  }

  private Drawable glassCard(){
    GradientDrawable g=new GradientDrawable();g.setColor(PANEL);g.setCornerRadius(dp(13));g.setStroke(dp(1),0xff252b36);return g;
  }
  private Drawable energyBar(int a,int b){return new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{a,b,0xff2a2d34});}
  private Drawable ring(int color){
    GradientDrawable g=new GradientDrawable();g.setShape(GradientDrawable.OVAL);g.setColor(0xff17131f);g.setStroke(dp(2),color);return new RippleDrawable(ColorStateList.valueOf(0x44ffffff),g,null);
  }

  private View dockButton(String glyph,String name,String action,boolean center){
    LinearLayout box=new LinearLayout(getContext());box.setOrientation(LinearLayout.VERTICAL);box.setGravity(Gravity.CENTER);box.setPadding(dp(3),0,dp(3),0);
    TextView icon=labelDetached(glyph,center?27:23,WHITE,true);icon.setGravity(Gravity.CENTER);icon.setBackground(center?ring(PURPLE):dockTile());
    box.addView(icon,new LinearLayout.LayoutParams(center?dp(60):dp(48),center?dp(60):dp(48)));
    TextView title=labelDetached(name,8,WHITE,true);title.setGravity(Gravity.CENTER);title.setLetterSpacing(.08f);box.addView(title,new LinearLayout.LayoutParams(-1,dp(20)));
    View mark=new View(getContext());mark.setBackgroundColor(center?PURPLE:ORANGE);LinearLayout.LayoutParams mlp=new LinearLayout.LayoutParams(dp(center?30:20),dp(3));box.addView(mark,mlp);
    box.setOnClickListener(v->{v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);actions.go(action);});return box;
  }
  private Drawable dockTile(){GradientDrawable g=new GradientDrawable();g.setColor(0xdd0c1016);g.setCornerRadius(dp(9));g.setStroke(dp(1),0xff272d38);return new RippleDrawable(ColorStateList.valueOf(0x33ffffff),g,null);}

  public void refreshDate(){
    Date now=new Date();date.setText(new SimpleDateFormat("EEE   MMM d",Locale.getDefault()).format(now).toUpperCase(Locale.getDefault()));
    clock.setText(new SimpleDateFormat("h:mm",Locale.getDefault()).format(now));
  }

  public void context(String headline,String model){scoreHeadline.setText(headline);scoreModel.setText(model);}

  public void media(String title,boolean playing){
    if(title==null||title.trim().isEmpty()||title.startsWith("Listen")){mediaTitle.setText("Listen");mediaSub.setText("Spotify ready");}
    else{String clean=title.replace("Playing · ","").replace("Paused · ","");mediaTitle.setText(clean);mediaSub.setText(playing?"NOW PLAYING":"PAUSED");}
  }

  public void refreshTasks(){
    TextView[] slots={task1,task2,task3};
    try{
      org.json.JSONArray arr=new org.json.JSONArray(prefs.getString("captures","[]"));
      for(int i=0;i<slots.length;i++){
        int index=arr.length()-1-i;
        if(index>=0){org.json.JSONObject note=arr.optJSONObject(index);String value=note==null?"":note.optString("text","");slots[i].setText((i==0?"◉  ":"•  ")+value);slots[i].setTextColor(i==0?WHITE:0xffc0c1c6);}
        else{slots[i].setText(i==0?"◉  Capture your next move":"•  Open slot");slots[i].setTextColor(MUTED);}
      }
    }catch(Exception e){for(TextView slot:slots){slot.setText("•  Open slot");slot.setTextColor(MUTED);}}
  }

  public void refreshSystem(){
    BatteryManager bm=(BatteryManager)getContext().getSystemService(Context.BATTERY_SERVICE);int pct=bm==null?-1:bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    battery.setText("POWER      "+(pct>=0?pct+"%":"—"));
    try{
      StatFs fs=new StatFs(new File(getContext().getFilesDir().getAbsolutePath()).getAbsolutePath());long gb=fs.getAvailableBytes()/(1024L*1024L*1024L);storage.setText("STORAGE   "+gb+" GB FREE");
    }catch(Exception e){storage.setText("STORAGE   —");}
  }

  @Override protected void onMeasure(int widthSpec,int heightSpec){
    int w=MeasureSpec.getSize(widthSpec),h=MeasureSpec.getSize(heightSpec);setMeasuredDimension(w,h);
    exact(backdrop,w,h);exact(halo,w,h);
    exact(brand,dp(230),dp(44));exact(crown,dp(44),dp(38));exact(motto,dp(250),dp(18));exact(date,dp(180),dp(26));exact(clock,dp(240),dp(76));exact(sideTag,dp(110),dp(76));
    exact(modes,w-dp(24),dp(50));
    int cardW=(w-dp(36))/2;exact(sportsCard,cardW,dp(146));exact(musicCard,cardW,dp(146));exact(tasksCard,cardW,dp(126));exact(systemCard,cardW,dp(126));
    exact(quote,dp(165),dp(68));exact(askBar,w-dp(214),dp(72));exact(dock,w-dp(18),dp(91));exact(bottomHint,w-dp(20),dp(18));
  }
  private void exact(View v,int w,int h){v.measure(MeasureSpec.makeMeasureSpec(Math.max(1,w),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.max(1,h),MeasureSpec.EXACTLY));}

  @Override protected void onLayout(boolean changed,int l,int t,int r,int b){
    int w=r-l,h=b-t;backdrop.layout(0,0,w,h);
    place(brand,dp(20),dp(12));place(crown,dp(196),dp(5));place(motto,dp(21),dp(55));place(date,dp(22),dp(145));place(clock,dp(18),dp(78));place(sideTag,w-dp(124),dp(90));

    int modeY=Math.max(dp(238),(int)(h*.31f));place(modes,dp(12),modeY);
    int cardsY=modeY+dp(58);int cardW=(w-dp(36))/2;
    place(sportsCard,dp(12),cardsY);place(musicCard,dp(24)+cardW,cardsY);
    int lowerY=cardsY+dp(156);place(tasksCard,dp(12),lowerY);place(systemCard,dp(24)+cardW,lowerY);

    int askY=Math.min(h-dp(190),lowerY+dp(137));place(quote,dp(18),askY);place(askBar,dp(190),askY-2);
    place(dock,dp(9),h-dp(112));place(bottomHint,dp(10),h-dp(20));halo.layout(0,0,w,h);
  }
  private void place(View v,int x,int y){v.layout(x,y,x+v.getMeasuredWidth(),y+v.getMeasuredHeight());}

  private boolean hit(View v,float x,float y){return v.getVisibility()==VISIBLE&&x>=v.getLeft()&&x<=v.getRight()&&y>=v.getTop()&&y<=v.getBottom();}
  private boolean interactive(float x,float y){return hit(modes,x,y)||hit(sportsCard,x,y)||hit(musicCard,x,y)||hit(tasksCard,x,y)||hit(askBar,x,y)||hit(dock,x,y);}
  private void arm(){longPressArmed=true;removeCallbacks(longPress);postDelayed(longPress,430);}
  private void disarm(){longPressArmed=false;removeCallbacks(longPress);}

  @Override public boolean dispatchTouchEvent(MotionEvent e){
    if(halo.isOpen()){
      if(e.getActionMasked()==MotionEvent.ACTION_MOVE)halo.track(e.getX(),e.getY());
      else if(e.getActionMasked()==MotionEvent.ACTION_UP)halo.release(e.getX(),e.getY());
      else if(e.getActionMasked()==MotionEvent.ACTION_CANCEL)halo.close();return true;
    }
    if(e.getActionMasked()==MotionEvent.ACTION_DOWN){downX=e.getX();downY=e.getY();if(!interactive(downX,downY))arm();}
    else if(e.getActionMasked()==MotionEvent.ACTION_MOVE){if(Math.hypot(e.getX()-downX,e.getY()-downY)>dp(16))disarm();}
    else if(e.getActionMasked()==MotionEvent.ACTION_UP){
      disarm();float dx=e.getX()-downX,dy=e.getY()-downY;
      if(Math.abs(dy)>dp(82)&&Math.abs(dy)>Math.abs(dx)*1.3f){backdrop.pulse();actions.go(dy<0?"apps":"capture");return true;}
      if(Math.abs(dx)>dp(92)&&Math.abs(dx)>Math.abs(dy)*1.4f){backdrop.pulse();actions.go(dx<0?"sports":"create");return true;}
      if(Math.hypot(dx,dy)<dp(18)&&!interactive(e.getX(),e.getY())){long now=System.currentTimeMillis();if(now-lastTap<320){lastTap=0;backdrop.pulse();actions.go("assistant");return true;}lastTap=now;}
    }else if(e.getActionMasked()==MotionEvent.ACTION_CANCEL)disarm();
    return super.dispatchTouchEvent(e);
  }

  @Override protected void onDetachedFromWindow(){disarm();halo.close();super.onDetachedFromWindow();}

  private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}

  private static final class TraceView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final Path path=new Path();
    TraceView(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    @Override protected void onSizeChanged(int w,int h,int ow,int oh){path.rewind();path.moveTo(0,h*.72f);for(int i=1;i<=12;i++){float x=w*i/12f;float y=h*(.55f+(float)Math.sin(i*1.7)*.18f);path.lineTo(x,y);}}
    @Override protected void onDraw(Canvas c){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(getResources().getDisplayMetrics().density*1.5f);p.setColor(0xff9f45ff);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);p.setColor(0xff5bdcff);c.drawCircle(getWidth()*.72f,getHeight()*.46f,getResources().getDisplayMetrics().density*3.2f,p);}
  }
}
