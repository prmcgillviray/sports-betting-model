package com.airow.launcher;

import android.content.Context;
import android.graphics.*;
import android.view.*;

public final class NeuralHaloView extends View {
  public interface Listener { void run(String action); }

  private static final String[] LABELS={"ASK","ROAM","CAPTURE","RADAR","LINK","LISTEN"};
  private static final String[] ACTIONS={"assistant","apps","capture","sports","connect","music"};
  private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG);
  private final Listener listener;
  private float cx,cy,r;
  private int selected=-1;

  public NeuralHaloView(Context c,Listener listener){
    super(c);this.listener=listener;setVisibility(GONE);setClickable(true);
    setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    setLayerType(LAYER_TYPE_HARDWARE,null);
  }

  public boolean isOpen(){return getVisibility()==VISIBLE;}

  public void open(float x,float y){
    cx=Math.max(dp(120),Math.min(getWidth()-dp(120),x));
    cy=Math.max(dp(140),Math.min(getHeight()-dp(140),y));
    r=dp(92);selected=-1;
    setAlpha(0);setScaleX(.94f);setScaleY(.94f);setVisibility(VISIBLE);
    animate().alpha(1).scaleX(1).scaleY(1).setDuration(120).start();
    invalidate();
  }

  public void track(float x,float y){
    if(!isOpen())return;
    double dx=x-cx,dy=y-cy,dist=Math.hypot(dx,dy);
    int next=-1;
    if(dist>dp(38)){
      double angle=Math.toDegrees(Math.atan2(dy,dx));
      if(angle<0)angle+=360;
      next=(int)Math.floor((angle+30)/60)%6;
    }
    if(next!=selected){
      selected=next;
      if(selected>=0)performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
      invalidate();
    }
  }

  public void release(float x,float y){
    if(!isOpen())return;
    track(x,y);int chosen=selected;close();
    if(chosen>=0&&listener!=null)listener.run(ACTIONS[chosen]);
  }

  public void close(){
    animate().cancel();setVisibility(GONE);selected=-1;invalidate();
  }

  @Override protected void onDraw(Canvas c){
    if(!isOpen())return;

    p.setStyle(Paint.Style.FILL);p.setColor(0xd905060a);c.drawRect(0,0,getWidth(),getHeight(),p);

    RadialGradient glow=new RadialGradient(cx,cy,r*1.65f,
        new int[]{0x446f5cff,0x224bd9ff,0x11ff4fd8,0x00000000},
        new float[]{0,.42f,.70f,1},Shader.TileMode.CLAMP);
    p.setShader(glow);c.drawCircle(cx,cy,r*1.65f,p);p.setShader(null);

    SweepGradient sweep=new SweepGradient(cx,cy,new int[]{0xff6f5cff,0xff4bd9ff,0xffff4fd8,0xffffb84d,0xff6f5cff});
    p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));p.setShader(sweep);
    c.drawCircle(cx,cy,r,p);p.setShader(null);

    p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
    for(int i=0;i<6;i++){
      double a=Math.toRadians(i*60);
      float x=cx+(float)Math.cos(a)*r;
      float y=cy+(float)Math.sin(a)*r;
      boolean hot=i==selected;

      p.setStyle(Paint.Style.FILL);p.setColor(hot?0x55ffffff:0x22181b24);
      c.drawCircle(x,y,dp(hot?31:26),p);

      p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(hot?2:1));p.setColor(hot?0xffffffff:0x55ffffff);
      c.drawCircle(x,y,dp(hot?31:26),p);

      p.setStyle(Paint.Style.FILL);p.setColor(hot?0xffffffff:0xffb8bac4);p.setTextSize(dp(hot?12:10));
      c.drawText(LABELS[i],x,y+dp(4),p);
    }

    Path diamond=new Path();
    diamond.moveTo(cx,cy-dp(12));diamond.lineTo(cx+dp(12),cy);diamond.lineTo(cx,cy+dp(12));diamond.lineTo(cx-dp(12),cy);diamond.close();
    p.setStyle(Paint.Style.FILL);p.setColor(0xff0f121a);c.drawPath(diamond,p);
    p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));p.setColor(0xff6f5cff);c.drawPath(diamond,p);

    p.setStyle(Paint.Style.FILL);p.setColor(0xff858895);p.setTextSize(dp(8));p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
    c.drawText(selected<0?"MOVE THROUGH THE HALO":"RELEASE TO EXECUTE",cx,cy+r+dp(52),p);
  }

  @Override public boolean onTouchEvent(MotionEvent e){
    if(!isOpen())return false;
    if(e.getActionMasked()==MotionEvent.ACTION_MOVE){track(e.getX(),e.getY());return true;}
    if(e.getActionMasked()==MotionEvent.ACTION_UP){release(e.getX(),e.getY());return true;}
    if(e.getActionMasked()==MotionEvent.ACTION_CANCEL){close();return true;}
    return true;
  }

  @Override protected void onDetachedFromWindow(){close();super.onDetachedFromWindow();}

  private float dp(float n){return n*getResources().getDisplayMetrics().density;}
}
