package com.airow.launcher;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.*;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import java.util.*;

/**
 * AIROW Aperture Flux: native controls over a procedural sculpture, plus an additive
 * thumb-centered command bloom. Static at idle; gesture effects exist only while touched.
 */
public final class ApertureHome extends FrameLayout {
  public interface Actions { void go(String action); }
  private static final int WHITE=0xfff5f3ef, MUTED=0xff9fa0a6, RED=0xffff5a43, PANEL=0xff111317;
  private final Actions actions;
  private final ApertureArtwork art;
  private final CommandBloomView bloom;
  private final TextView brand, settings, gestureHint, date, connect, create, explore, contextLabel,
      contextText, modelText, mediaText, mediaControl, search;
  private final TextClock clock;
  private final View rule;
  private float downX,downY;
  private boolean swiping,longPressArmed;
  private long lastTap;
  private HomeLayout geometry;
  private final Runnable longPress=new Runnable(){
    @Override public void run(){
      if(!longPressArmed||swiping||bloom.isOpen())return;
      longPressArmed=false;
      performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
      art.pulse();
      bloom.bringToFront();
      bloom.open(downX,downY);
    }
  };

  public ApertureHome(Context c,Actions actions){
    super(c);this.actions=actions;setBackgroundColor(0xff08090b);setClickable(true);
    art=new ApertureArtwork(c);addView(art);

    brand=label("AIROW / APERTURE",19,WHITE,true);brand.setLetterSpacing(.14f);
    settings=control("⋯",26,WHITE,"settings");settings.setContentDescription("Launcher settings");settings.setGravity(Gravity.CENTER);
    gestureHint=label("HOLD / BLOOM    DOUBLE TAP / ASK",8,0xff6f7178,true);gestureHint.setLetterSpacing(.11f);
    date=label("",12,MUTED,false);

    clock=new TextClock(c);clock.setFormat12Hour("h:mm");clock.setFormat24Hour("HH:mm");
    clock.setTextColor(WHITE);clock.setTextSize(82);clock.setLetterSpacing(-.06f);
    clock.setAutoSizeTextTypeUniformWithConfiguration(32,82,1,android.util.TypedValue.COMPLEX_UNIT_SP);
    clock.setTypeface(android.graphics.Typeface.create("sans-serif-light",android.graphics.Typeface.NORMAL));
    clock.setIncludeFontPadding(false);addView(clock);

    connect=port("LINK  /  PEOPLE + PLACES","connect");
    create=port("MAKE  /  THOUGHTS + TOOLS","create");
    explore=port("ROAM  /  APPS + SYSTEMS","apps");

    contextLabel=label("LIVE / SIGNAL",10,RED,true);contextLabel.setLetterSpacing(.16f);
    contextText=control("Sports unavailable  ↗",17,WHITE,"sports");
    modelText=control("Model · freshness unknown",12,MUTED,"sports");
    mediaText=control("Listen  ↗",14,MUTED,"music");
    mediaControl=control("▶",20,WHITE,"play");mediaControl.setGravity(Gravity.CENTER);
    mediaControl.setContentDescription("Play or pause Spotify");

    rule=new View(c);rule.setBackgroundColor(0xff2a2b2e);addView(rule);

    search=control("⌕   COMMAND / APP / ACTION",15,WHITE,"search");
    search.setLetterSpacing(.04f);search.setPadding(dp(20),0,dp(18),0);search.setBackground(commandPlate());

    bloom=new CommandBloomView(c,actions::go);addView(bloom,new FrameLayout.LayoutParams(-1,-1));
  }

  private TextView port(String text,String action){
    TextView v=control(text,23,WHITE,action);
    v.setLetterSpacing(.04f);
    v.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL));
    return v;
  }

  private TextView label(String text,int size,int color,boolean bold){
    TextView v=new TextView(getContext());v.setText(text);v.setTextSize(size);v.setTextColor(color);
    v.setTypeface(android.graphics.Typeface.create("sans-serif",bold?android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL));
    v.setIncludeFontPadding(false);v.setGravity(Gravity.CENTER_VERTICAL);
    v.setSingleLine(true);v.setEllipsize(TextUtils.TruncateAt.END);addView(v);return v;
  }

  private TextView control(String text,int size,int color,String action){
    TextView v=label(text,size,color,false);v.setFocusable(true);v.setBackground(background(0x00000000,8));
    v.setOnClickListener(view->{view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);actions.go(action);});
    return v;
  }

  private Drawable background(int color,int radius){
    GradientDrawable shape=new GradientDrawable();shape.setColor(color);shape.setCornerRadius(dp(radius));
    return new RippleDrawable(ColorStateList.valueOf(0x33ff5a43),shape,null);
  }

  private Drawable commandPlate(){
    GradientDrawable shape=new GradientDrawable();
    shape.setColor(PANEL);shape.setCornerRadius(dp(4));shape.setStroke(dp(1),0x66ff5a43);
    return new RippleDrawable(ColorStateList.valueOf(0x33ff5a43),shape,null);
  }

  @Override protected void onMeasure(int widthSpec,int heightSpec){
    int width=MeasureSpec.getSize(widthSpec),available=MeasureSpec.getSize(heightSpec);
    float density=getResources().getDisplayMetrics().density;
    geometry=new HomeLayout(width/density,available/density,getResources().getConfiguration().fontScale);
    setMeasuredDimension(width,dp(geometry.height));
    place(art,"art");place(brand,"brand");place(settings,"settings");place(gestureHint,"gestureHint");
    place(date,"date");place(clock,"clock");place(connect,"connect");place(create,"create");place(explore,"explore");
    place(contextLabel,"contextLabel");place(contextText,"contextText");place(modelText,"modelText");place(rule,"rule");
    place(mediaText,"mediaText");place(mediaControl,"mediaControl");place(search,"search");
    bloom.measure(MeasureSpec.makeMeasureSpec(width,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(dp(geometry.height),MeasureSpec.EXACTLY));
  }

  private void place(View v,String name){
    float[] b=geometry.bounds(name);
    FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)v.getLayoutParams();
    lp.width=dp(b[2]);lp.height=dp(b[3]);lp.leftMargin=dp(b[0]);lp.topMargin=dp(b[1]);
    v.measure(MeasureSpec.makeMeasureSpec(lp.width,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(lp.height,MeasureSpec.EXACTLY));
  }

  @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){
    for(int i=0;i<getChildCount();i++){
      View child=getChildAt(i);
      if(child==bloom){child.layout(0,0,getMeasuredWidth(),getMeasuredHeight());continue;}
      FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)child.getLayoutParams();
      child.layout(lp.leftMargin,lp.topMargin,lp.leftMargin+child.getMeasuredWidth(),lp.topMargin+child.getMeasuredHeight());
    }
  }

  public void refreshDate(){date.setText(new java.text.SimpleDateFormat("EEEE, MMMM d",Locale.getDefault()).format(new Date()));}
  public void context(String headline,String model){contextText.setText(headline+"  ↗");modelText.setText(model);}
  public void media(String title,boolean playing){mediaText.setText(title);mediaControl.setText(playing?"Ⅱ":"▶");mediaControl.setContentDescription(playing?"Pause Spotify":"Play Spotify");}

  private boolean inside(View v,float x,float y){
    return v.getVisibility()==VISIBLE&&x>=v.getLeft()&&x<=v.getRight()&&y>=v.getTop()&&y<=v.getBottom();
  }

  private boolean interactiveAt(float x,float y){
    return inside(settings,x,y)||inside(connect,x,y)||inside(create,x,y)||inside(explore,x,y)
        ||inside(contextText,x,y)||inside(modelText,x,y)||inside(mediaText,x,y)||inside(mediaControl,x,y)||inside(search,x,y);
  }

  private void disarmBloom(){longPressArmed=false;removeCallbacks(longPress);}

  @Override public boolean dispatchTouchEvent(MotionEvent e){
    if(bloom.isOpen()){
      if(e.getActionMasked()==MotionEvent.ACTION_MOVE)bloom.track(e.getX(),e.getY());
      else if(e.getActionMasked()==MotionEvent.ACTION_UP)bloom.release(e.getX(),e.getY());
      else if(e.getActionMasked()==MotionEvent.ACTION_CANCEL)bloom.close();
      return true;
    }

    if(e.getActionMasked()==MotionEvent.ACTION_DOWN){
      downX=e.getX();downY=e.getY();swiping=false;
      longPressArmed=!interactiveAt(downX,downY);
      if(longPressArmed)postDelayed(longPress,420);
    }else if(e.getActionMasked()==MotionEvent.ACTION_MOVE){
      if(Math.hypot(e.getX()-downX,e.getY()-downY)>dp(14))disarmBloom();
    }else if(e.getActionMasked()==MotionEvent.ACTION_CANCEL){
      disarmBloom();
    }else if(e.getActionMasked()==MotionEvent.ACTION_UP){
      disarmBloom();
      float dx=e.getX()-downX,dy=e.getY()-downY;
      if(Math.hypot(dx,dy)<dp(18)&&!interactiveAt(e.getX(),e.getY())){
        long now=System.currentTimeMillis();
        art.pulse();
        if(now-lastTap<320){lastTap=0;performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);actions.go("assistant");return true;}
        lastTap=now;
      }
      if(downY-e.getY()>dp(90)&&Math.abs(dx)<dp(60)&&getParent() instanceof View
          &&getHeight()<=((View)getParent()).getHeight()+dp(2)){
        MotionEvent cancel=MotionEvent.obtain(e);cancel.setAction(MotionEvent.ACTION_CANCEL);
        super.dispatchTouchEvent(cancel);cancel.recycle();actions.go("apps");return true;
      }
    }
    return super.dispatchTouchEvent(e);
  }

  @Override public boolean onInterceptTouchEvent(MotionEvent event){
    if(bloom.isOpen())return true;
    if(event.getActionMasked()==MotionEvent.ACTION_DOWN){downX=event.getX();downY=event.getY();swiping=false;}
    if(event.getActionMasked()==MotionEvent.ACTION_MOVE
        &&Math.abs(event.getX()-downX)>dp(60)
        &&Math.abs(event.getX()-downX)>Math.abs(event.getY()-downY)*1.4f){
      disarmBloom();swiping=true;return true;
    }
    return false;
  }

  @Override public boolean onTouchEvent(MotionEvent event){
    if(bloom.isOpen()){
      if(event.getActionMasked()==MotionEvent.ACTION_MOVE)bloom.track(event.getX(),event.getY());
      else if(event.getActionMasked()==MotionEvent.ACTION_UP)bloom.release(event.getX(),event.getY());
      else if(event.getActionMasked()==MotionEvent.ACTION_CANCEL)bloom.close();
      return true;
    }
    if(event.getActionMasked()==MotionEvent.ACTION_MOVE&&swiping){
      art.setTranslationX(Math.max(-dp(26),Math.min(dp(26),(event.getX()-downX)*.13f)));return true;
    }
    if(event.getActionMasked()==MotionEvent.ACTION_CANCEL){
      art.setTranslationX(0);swiping=false;disarmBloom();return true;
    }
    if(event.getActionMasked()==MotionEvent.ACTION_UP&&swiping){
      art.setTranslationX(0);actions.go(event.getX()<downX?"sports":"create");swiping=false;return true;
    }
    return true;
  }

  @Override protected void onDetachedFromWindow(){
    disarmBloom();animate().cancel();bloom.close();art.setTranslationX(0);super.onDetachedFromWindow();
  }

  private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
