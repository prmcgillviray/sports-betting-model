package com.airow.launcher;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/**
 * Static AIROW V6 hero environment: mountains, fog, metallic A monolith and energy accents.
 * Rebuilds geometry only when size changes and never runs an idle animation loop.
 */
public final class CommandBackdropView extends View {
  private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.DITHER_FLAG);
  private final Paint stroke=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.DITHER_FLAG);
  private final Path back=new Path(),mid=new Path(),front=new Path(),leftBlade=new Path(),rightBlade=new Path();
  private LinearGradient sky,metalA,metalB,water;
  private RadialGradient orangeGlow,purpleGlow;
  private float w,h;

  public CommandBackdropView(Context c){
    super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setLayerType(LAYER_TYPE_HARDWARE,null);
  }

  @Override protected void onSizeChanged(int width,int height,int oldW,int oldH){
    w=width;h=height;
    sky=new LinearGradient(0,0,0,h,new int[]{0xff050609,0xff0a0d12,0xff080a0e,0xff030406},new float[]{0,.36f,.72f,1},Shader.TileMode.CLAMP);
    metalA=new LinearGradient(w*.38f,h*.08f,w*.72f,h*.48f,new int[]{0x44ffffff,0x99d9d9d9,0x5594969c,0x88ffffff,0x33454950},new float[]{0,.22f,.48f,.70f,1},Shader.TileMode.CLAMP);
    metalB=new LinearGradient(w*.55f,h*.16f,w*.84f,h*.55f,new int[]{0x225b5e65,0x88f1eee8,0x557c7f86,0x99ffffff,0x22383b42},new float[]{0,.28f,.50f,.72f,1},Shader.TileMode.CLAMP);
    water=new LinearGradient(0,h*.52f,0,h*.68f,new int[]{0x00161920,0x66181c21,0xaa050608},null,Shader.TileMode.CLAMP);
    orangeGlow=new RadialGradient(w*.66f,h*.28f,w*.31f,new int[]{0x66ff6b2c,0x22ff6b2c,0x00000000},new float[]{0,.45f,1},Shader.TileMode.CLAMP);
    purpleGlow=new RadialGradient(w*.23f,h*.68f,w*.34f,new int[]{0x335e2cff,0x145e2cff,0x00000000},new float[]{0,.42f,1},Shader.TileMode.CLAMP);

    back.rewind();
    back.moveTo(0,h*.39f);back.lineTo(w*.08f,h*.31f);back.lineTo(w*.17f,h*.35f);back.lineTo(w*.29f,h*.19f);back.lineTo(w*.38f,h*.30f);back.lineTo(w*.50f,h*.13f);back.lineTo(w*.61f,h*.29f);back.lineTo(w*.72f,h*.17f);back.lineTo(w*.84f,h*.33f);back.lineTo(w,h*.23f);back.lineTo(w,h*.57f);back.lineTo(0,h*.57f);back.close();
    mid.rewind();
    mid.moveTo(0,h*.48f);mid.lineTo(w*.12f,h*.42f);mid.lineTo(w*.22f,h*.49f);mid.lineTo(w*.35f,h*.33f);mid.lineTo(w*.48f,h*.47f);mid.lineTo(w*.61f,h*.34f);mid.lineTo(w*.74f,h*.50f);mid.lineTo(w*.88f,h*.40f);mid.lineTo(w,h*.49f);mid.lineTo(w,h*.62f);mid.lineTo(0,h*.62f);mid.close();
    front.rewind();
    front.moveTo(0,h*.55f);front.lineTo(w*.18f,h*.50f);front.lineTo(w*.31f,h*.59f);front.lineTo(w*.47f,h*.48f);front.lineTo(w*.61f,h*.60f);front.lineTo(w*.79f,h*.49f);front.lineTo(w,h*.58f);front.lineTo(w,h*.69f);front.lineTo(0,h*.69f);front.close();

    leftBlade.rewind();
    leftBlade.moveTo(w*.49f,h*.47f);leftBlade.lineTo(w*.66f,h*.11f);leftBlade.lineTo(w*.73f,h*.18f);leftBlade.lineTo(w*.58f,h*.49f);leftBlade.close();
    rightBlade.rewind();
    rightBlade.moveTo(w*.58f,h*.49f);rightBlade.lineTo(w*.73f,h*.18f);rightBlade.lineTo(w*.87f,h*.48f);rightBlade.lineTo(w*.79f,h*.48f);rightBlade.lineTo(w*.70f,h*.31f);rightBlade.close();
  }

  @Override protected void onDraw(Canvas c){
    p.setStyle(Paint.Style.FILL);p.setShader(sky);c.drawRect(0,0,w,h,p);

    p.setShader(orangeGlow);c.drawCircle(w*.66f,h*.28f,w*.31f,p);
    p.setShader(purpleGlow);c.drawCircle(w*.23f,h*.68f,w*.34f,p);

    p.setShader(null);p.setColor(0xff101319);c.drawPath(back,p);
    p.setColor(0xff0b0e13);c.drawPath(mid,p);
    p.setColor(0xff07090d);c.drawPath(front,p);

    p.setColor(0x18dfe3ea);
    for(int i=0;i<7;i++){
      float x=w*(.08f+i*.15f),y=h*(.42f+(i%3)*.026f);
      c.drawOval(new RectF(x-w*.13f,y-h*.026f,x+w*.17f,y+h*.028f),p);
    }

    p.setShader(metalA);c.drawPath(leftBlade,p);
    p.setShader(metalB);c.drawPath(rightBlade,p);p.setShader(null);

    stroke.setStyle(Paint.Style.STROKE);stroke.setStrokeCap(Paint.Cap.ROUND);
    stroke.setStrokeWidth(dp(7));stroke.setColor(0x22ff6b2c);
    c.drawLine(w*.50f,h*.46f,w*.72f,h*.19f,stroke);
    stroke.setStrokeWidth(dp(2.2f));stroke.setColor(0xffff7437);
    c.drawLine(w*.50f,h*.46f,w*.72f,h*.19f,stroke);

    p.setShader(water);c.drawRect(0,h*.51f,w,h*.71f,p);p.setShader(null);
    stroke.setStrokeWidth(dp(1));stroke.setColor(0x22ff8248);
    for(int i=0;i<9;i++){
      float y=h*(.55f+i*.012f);float half=w*(.17f-i*.012f);
      c.drawLine(w*.62f-half,y,w*.62f+half,y,stroke);
    }

    stroke.setStrokeWidth(dp(2));
    for(int i=0;i<7;i++){
      stroke.setColor(i%2==0?0x445f2cff:0x44ff6b2c);
      float y=h*(.72f+i*.034f);
      c.drawLine(i%2==0?0:w*.82f,y,i%2==0?w*.18f:w,y-h*.045f,stroke);
    }

    p.setColor(0x55ffffff);
    for(int i=0;i<32;i++){
      float x=((i*73)%101)/101f*w;
      float y=((i*47+17)%89)/89f*h*.52f;
      float r=dp((i%4==0)?1.1f:.55f);
      c.drawCircle(x,y,r,p);
    }
  }

  public void pulse(){
    animate().cancel();animate().scaleX(1.012f).scaleY(1.012f).setDuration(90).withEndAction(
      ()->animate().scaleX(1f).scaleY(1f).setDuration(180).start()).start();
  }

  @Override protected void onDetachedFromWindow(){animate().cancel();setScaleX(1);setScaleY(1);super.onDetachedFromWindow();}
  private float dp(float n){return n*getResources().getDisplayMetrics().density;}
}
