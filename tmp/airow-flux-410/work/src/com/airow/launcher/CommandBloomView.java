package com.airow.launcher;

import android.content.Context;
import android.graphics.*;
import android.view.*;

/**
 * Thumb-centered command bloom. It appears only after a deliberate long press,
 * tracks the finger, and disappears immediately after selection.
 */
public final class CommandBloomView extends View {
  public interface Listener { void run(String action); }
  private static final int WHITE=0xfff5f3ef, MUTED=0xff9b9ca2, RED=0xffff5a43, INK=0xff08090b;
  private static final String[] LABELS={"ASK","APPS","CAPTURE","RADAR","CALL"};
  private static final String[] ACTIONS={"assistant","apps","capture","sports","phone"};
  private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG);
  private final float[] nx=new float[5],ny=new float[5];
  private final Listener listener;
  private float cx,cy;
  private int selected=-1;

  public CommandBloomView(Context c,Listener l){
    super(c);listener=l;setVisibility(GONE);setClickable(true);
    setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
  }

  public boolean isOpen(){return getVisibility()==VISIBLE;}

  public void open(float x,float y){
    cx=Math.max(dp(92),Math.min(getWidth()-dp(92),x));
    cy=Math.max(dp(110),Math.min(getHeight()-dp(110),y));
    float r=dp(84);
    for(int i=0;i<5;i++){
      double a=Math.toRadians(-90+i*72);
      nx[i]=cx+(float)Math.cos(a)*r;
      ny[i]=cy+(float)Math.sin(a)*r;
    }
    selected=-1;setAlpha(0);setScaleX(.94f);setScaleY(.94f);setVisibility(VISIBLE);
    animate().alpha(1).scaleX(1).scaleY(1).setDuration(110).start();
    invalidate();
  }

  public void track(float x,float y){
    if(!isOpen())return;
    int next=-1;float best=Float.MAX_VALUE;
    for(int i=0;i<5;i++){
      float dx=x-nx[i],dy=y-ny[i],d=dx*dx+dy*dy;
      if(d<best){best=d;next=i;}
    }
    if(Math.hypot(x-cx,y-cy)<dp(32))next=-1;
    if(next!=selected){
      selected=next;
      performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
      invalidate();
    }
  }

  public void release(float x,float y){
    if(!isOpen())return;
    track(x,y);
    int chosen=selected;
    close();
    if(chosen>=0&&listener!=null)listener.run(ACTIONS[chosen]);
  }

  public void close(){
    animate().cancel();setVisibility(GONE);selected=-1;invalidate();
  }

  @Override protected void onDraw(Canvas c){
    if(!isOpen())return;
    p.setStyle(Paint.Style.FILL);p.setColor(0xcc050608);c.drawRect(0,0,getWidth(),getHeight(),p);

    p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1));p.setColor(0x66ffffff);
    for(int i=0;i<5;i++)c.drawLine(cx,cy,nx[i],ny[i],p);

    p.setStyle(Paint.Style.FILL);p.setColor(RED);
    Path diamond=new Path();
    diamond.moveTo(cx,cy-dp(11));diamond.lineTo(cx+dp(11),cy);diamond.lineTo(cx,cy+dp(11));diamond.lineTo(cx-dp(11),cy);diamond.close();
    c.drawPath(diamond,p);

    p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
    for(int i=0;i<5;i++){
      boolean hot=i==selected;
      p.setColor(hot?RED:WHITE);
      p.setTextSize(dp(hot?13:11));
      c.drawCircle(nx[i],ny[i],dp(hot?29:24),ringPaint(hot));
      p.setStyle(Paint.Style.FILL);
      c.drawText(LABELS[i],nx[i],ny[i]+dp(4),p);
    }
    p.setColor(MUTED);p.setTextSize(dp(8));p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
    c.drawText(selected<0?"FLICK TO COMMAND":"RELEASE",cx,cy+dp(34),p);
  }

  private Paint ringPaint(boolean hot){
    p.setStyle(Paint.Style.FILL);
    p.setColor(hot?0x44ff5a43:0x301d2025);
    return p;
  }

  @Override public boolean onTouchEvent(MotionEvent e){
    if(!isOpen())return false;
    if(e.getActionMasked()==MotionEvent.ACTION_MOVE){track(e.getX(),e.getY());return true;}
    if(e.getActionMasked()==MotionEvent.ACTION_UP){release(e.getX(),e.getY());return true;}
    if(e.getActionMasked()==MotionEvent.ACTION_CANCEL){close();return true;}
    return true;
  }

  private float dp(float n){return n*getResources().getDisplayMetrics().density;}
}
