package com.airow.launcher;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import java.util.*;

/**
 * AIROW 5 home environment. The visual system is mostly procedural Canvas art.
 * Native controls remain on top for accessibility and predictable launcher behavior.
 */
public final class NeuralHome extends FrameLayout {
  public interface Actions { void go(String action); }

  private static final int WHITE=0xfff4f3ef, MUTED=0xff989ba8, CYAN=0xff4bd9ff,
      VIOLET=0xff7a68ff, MAGENTA=0xffff4fd8, SURFACE=0xcc0e1118;

  private final Actions actions;
  private final NeuralCanvas canvas;
  private final NeuralHaloView halo;
  private final TextView brand,date,mode,ask,roam,make,radar,contextLabel,contextText,modelText,mediaText,mediaControl,search;
  private final TextClock clock;
  private float downX,downY;
  private boolean coreGesture,longPressArmed;
  private long lastTap;
  private final Runnable longPress=new Runnable(){
    @Override public void run(){
      if(!longPressArmed||halo.isOpen())return;
      longPressArmed=false;
      performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
      canvas.pulse();
      halo.bringToFront();
      halo.open(downX,downY);
    }
  };

  public NeuralHome(Context c,Actions actions){
    super(c);this.actions=actions;
    setBackgroundColor(0xff05060a);setClickable(true);

    canvas=new NeuralCanvas(c);addView(canvas,new LayoutParams(-1,-1));

    brand=label("AIROW / NEURAL SPACE",18,WHITE,true);
    brand.setLetterSpacing(.16f);
    date=label("",12,MUTED,false);
    mode=label("CONTEXT  /  LIVE SYSTEM",9,VIOLET,true);mode.setLetterSpacing(.16f);

    clock=new TextClock(c);
    clock.setFormat12Hour("h:mm");clock.setFormat24Hour("HH:mm");
    clock.setTextColor(WHITE);clock.setTextSize(74);clock.setLetterSpacing(-.055f);
    clock.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));
    clock.setIncludeFontPadding(false);addView(clock);

    ask=gate("ASK","assistant",WHITE);
    roam=gate("ROAM","apps",CYAN);
    make=gate("MAKE","create",MAGENTA);
    radar=gate("RADAR","sports",VIOLET);

    contextLabel=label("LIVE INTELLIGENCE",9,CYAN,true);contextLabel.setLetterSpacing(.18f);
    contextText=control("Sports unavailable  ↗",17,WHITE,"sports");
    modelText=control("Model unavailable",12,MUTED,"sports");

    mediaText=control("Listen  ↗",13,MUTED,"music");
    mediaControl=control("▶",18,WHITE,"play");mediaControl.setGravity(Gravity.CENTER);

    search=control("⌘   SEARCH / COMMAND / INTENT",14,WHITE,"search");
    search.setLetterSpacing(.04f);search.setPadding(dp(18),0,dp(18),0);search.setBackground(glass());

    halo=new NeuralHaloView(c,actions::go);addView(halo,new LayoutParams(-1,-1));
  }

  private TextView gate(String text,String action,int color){
    TextView v=control(text,11,color,action);
    v.setGravity(Gravity.CENTER);v.setLetterSpacing(.15f);
    v.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
    GradientDrawable g=new GradientDrawable();g.setColor(0x22161a24);g.setCornerRadius(dp(18));g.setStroke(dp(1),color&0x66ffffff);
    v.setBackground(new RippleDrawable(ColorStateList.valueOf(0x44ffffff),g,null));
    return v;
  }

  private TextView label(String text,int size,int color,boolean bold){
    TextView v=new TextView(getContext());
    v.setText(text);v.setTextSize(size);v.setTextColor(color);
    v.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
    v.setIncludeFontPadding(false);v.setGravity(Gravity.CENTER_VERTICAL);
    v.setSingleLine(true);v.setEllipsize(TextUtils.TruncateAt.END);addView(v);return v;
  }

  private TextView control(String text,int size,int color,String action){
    TextView v=label(text,size,color,false);v.setFocusable(true);
    v.setOnClickListener(view->{view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);actions.go(action);});
    return v;
  }

  private Drawable glass(){
    GradientDrawable shape=new GradientDrawable();
    shape.setColor(SURFACE);shape.setCornerRadius(dp(24));shape.setStroke(dp(1),0x555f68ff);
    return new RippleDrawable(ColorStateList.valueOf(0x336f5cff),shape,null);
  }

  @Override protected void onMeasure(int widthSpec,int heightSpec){
    int w=MeasureSpec.getSize(widthSpec),h=MeasureSpec.getSize(heightSpec);
    setMeasuredDimension(w,h);
    measureChildExact(canvas,w,h);
    measure(brand,w*.78f,dp(42));measure(date,w*.58f,dp(24));measure(mode,w*.70f,dp(20));
    measure(clock,w*.62f,dp(102));
    measure(ask,dp(86),dp(42));measure(roam,dp(86),dp(42));measure(make,dp(86),dp(42));measure(radar,dp(86),dp(42));
    measure(contextLabel,w*.72f,dp(20));measure(contextText,w*.84f,dp(42));measure(modelText,w*.84f,dp(30));
    measure(mediaText,w*.60f,dp(40));measure(mediaControl,dp(44),dp(40));measure(search,w-dp(48),dp(56));
    halo.measure(MeasureSpec.makeMeasureSpec(w,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(h,MeasureSpec.EXACTLY));
  }

  private void measure(View v,float width,int height){
    v.measure(MeasureSpec.makeMeasureSpec((int)width,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(height,MeasureSpec.EXACTLY));
  }
  private void measureChildExact(View v,int w,int h){
    v.measure(MeasureSpec.makeMeasureSpec(w,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(h,MeasureSpec.EXACTLY));
  }

  @Override protected void onLayout(boolean changed,int l,int t,int r,int b){
    int w=r-l,h=b-t;
    canvas.layout(0,0,w,h);
    place(brand,dp(24),dp(10));
    place(mode,dp(24),dp(52));
    place(date,dp(24),dp(78));
    place(clock,dp(20),dp(104));

    int coreX=(int)(w*.68f),coreY=(int)(h*.34f);
    place(ask,coreX-dp(43),coreY-dp(112));
    place(roam,coreX+dp(52),coreY-dp(18));
    place(make,coreX-dp(43),coreY+dp(74));
    place(radar,coreX-dp(138),coreY-dp(18));

    int infoY=(int)(h*.60f);
    place(contextLabel,dp(24),infoY);
    place(contextText,dp(24),infoY+dp(24));
    place(modelText,dp(24),infoY+dp(68));

    place(mediaText,dp(24),h-dp(146));
    place(mediaControl,w-dp(68),h-dp(146));
    place(search,dp(24),h-dp(78));

    halo.layout(0,0,w,h);
  }

  private void place(View v,int x,int y){
    v.layout(x,y,x+v.getMeasuredWidth(),y+v.getMeasuredHeight());
  }

  public void refreshDate(){
    date.setText(new java.text.SimpleDateFormat("EEEE, MMMM d",Locale.getDefault()).format(new Date()));
  }

  public void context(String headline,String model){
    contextText.setText(headline+"  ↗");modelText.setText(model);
  }

  public void media(String title,boolean playing){
    mediaText.setText(title);mediaControl.setText(playing?"Ⅱ":"▶");
    mediaControl.setContentDescription(playing?"Pause Spotify":"Play Spotify");
  }

  private boolean inCore(float x,float y){
    float cx=getWidth()*.68f,cy=getHeight()*.34f;
    return Math.hypot(x-cx,y-cy)<dp(88);
  }

  private boolean interactiveAt(float x,float y){
    return hit(ask,x,y)||hit(roam,x,y)||hit(make,x,y)||hit(radar,x,y)||hit(contextText,x,y)
        ||hit(modelText,x,y)||hit(mediaText,x,y)||hit(mediaControl,x,y)||hit(search,x,y);
  }

  private boolean hit(View v,float x,float y){
    return x>=v.getLeft()&&x<=v.getRight()&&y>=v.getTop()&&y<=v.getBottom();
  }

  private void armLongPress(){
    longPressArmed=true;removeCallbacks(longPress);postDelayed(longPress,430);
  }
  private void disarmLongPress(){longPressArmed=false;removeCallbacks(longPress);}

  @Override public boolean dispatchTouchEvent(MotionEvent e){
    if(halo.isOpen()){
      if(e.getActionMasked()==MotionEvent.ACTION_MOVE)halo.track(e.getX(),e.getY());
      else if(e.getActionMasked()==MotionEvent.ACTION_UP)halo.release(e.getX(),e.getY());
      else if(e.getActionMasked()==MotionEvent.ACTION_CANCEL)halo.close();
      return true;
    }

    if(e.getActionMasked()==MotionEvent.ACTION_DOWN){
      downX=e.getX();downY=e.getY();coreGesture=inCore(downX,downY);
      if(!interactiveAt(downX,downY))armLongPress();
    }else if(e.getActionMasked()==MotionEvent.ACTION_MOVE){
      if(Math.hypot(e.getX()-downX,e.getY()-downY)>dp(16))disarmLongPress();
    }else if(e.getActionMasked()==MotionEvent.ACTION_UP){
      disarmLongPress();
      float dx=e.getX()-downX,dy=e.getY()-downY;
      if(coreGesture&&Math.hypot(dx,dy)>dp(55)){
        canvas.pulse();performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        if(Math.abs(dx)>Math.abs(dy))actions.go(dx>0?"apps":"sports");
        else actions.go(dy>0?"capture":"connect");
        return true;
      }
      if(coreGesture&&Math.hypot(dx,dy)<dp(18)){
        canvas.pulse();actions.go("assistant");return true;
      }
      if(Math.hypot(dx,dy)<dp(18)&&!interactiveAt(e.getX(),e.getY())){
        long now=System.currentTimeMillis();
        if(now-lastTap<320){lastTap=0;canvas.pulse();actions.go("assistant");return true;}
        lastTap=now;
      }
    }else if(e.getActionMasked()==MotionEvent.ACTION_CANCEL){
      disarmLongPress();
    }
    return super.dispatchTouchEvent(e);
  }

  @Override protected void onDetachedFromWindow(){
    disarmLongPress();halo.close();super.onDetachedFromWindow();
  }

  private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
