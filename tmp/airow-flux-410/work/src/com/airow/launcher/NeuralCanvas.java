package com.airow.launcher;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;

/**
 * AIROW 5 procedural environment. Static at idle; touch adds a few pixels of parallax.
 * No timers, no perpetual invalidation.
 */
public final class NeuralCanvas extends View {
  private final Paint fill=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.DITHER_FLAG);
  private final Paint stroke=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.DITHER_FLAG);
  private final Path shardA=new Path(),shardB=new Path(),spine=new Path(),core=new Path();
  private LinearGradient background, shardAGradient, shardBGradient, spineGradient;
  private RadialGradient aura,hotspot;
  private SweepGradient spectrum;
  private float w,h,cx,cy,px,py;
  private boolean touching;

  public NeuralCanvas(Context context){
    super(context);
    setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    setLayerType(LAYER_TYPE_HARDWARE,null);
  }

  @Override protected void onSizeChanged(int width,int height,int oldW,int oldH){
    w=width;h=height;cx=w*.68f;cy=h*.34f;

    background=new LinearGradient(0,0,w,h,
        new int[]{0xff05060a,0xff090b13,0xff07080d,0xff030407},
        new float[]{0,.35f,.72f,1},Shader.TileMode.CLAMP);
    aura=new RadialGradient(cx,cy,w*.46f,
        new int[]{0x445f52ff,0x1f4bd9ff,0x0cff4fd8,0x00000000},
        new float[]{0,.38f,.67f,1},Shader.TileMode.CLAMP);
    hotspot=new RadialGradient(cx,cy,w*.18f,
        new int[]{0x99ffffff,0x664bd9ff,0x226f5cff,0x00000000},
        new float[]{0,.18f,.55f,1},Shader.TileMode.CLAMP);
    spectrum=new SweepGradient(cx,cy,new int[]{
        0xff6f5cff,0xff4bd9ff,0xffff4fd8,0xffffb84d,0xff6f5cff});

    shardAGradient=new LinearGradient(w*.18f,h*.16f,w*.90f,h*.58f,
        new int[]{0x00ffffff,0x55cfd4ff,0x225e6cff,0x00736cff},null,Shader.TileMode.CLAMP);
    shardBGradient=new LinearGradient(w*.46f,h*.10f,w*.88f,h*.64f,
        new int[]{0x00ffffff,0x44ffffff,0x224bd9ff,0x00000000},null,Shader.TileMode.CLAMP);
    spineGradient=new LinearGradient(w*.12f,h*.50f,w*.82f,h*.78f,
        new int[]{0x006f5cff,0x776f5cff,0x884bd9ff,0x00ff4fd8},null,Shader.TileMode.CLAMP);

    shardA.rewind();
    shardA.moveTo(w*.34f,h*.17f);
    shardA.cubicTo(w*.58f,h*.08f,w*.88f,h*.13f,w*.96f,h*.27f);
    shardA.cubicTo(w*.83f,h*.33f,w*.72f,h*.48f,w*.68f,h*.64f);
    shardA.cubicTo(w*.53f,h*.52f,w*.38f,h*.39f,w*.24f,h*.29f);
    shardA.close();

    shardB.rewind();
    shardB.moveTo(w*.55f,h*.10f);
    shardB.cubicTo(w*.77f,h*.18f,w*.92f,h*.32f,w*.94f,h*.50f);
    shardB.cubicTo(w*.80f,h*.42f,w*.63f,h*.43f,w*.45f,h*.52f);
    shardB.cubicTo(w*.48f,h*.36f,w*.50f,h*.20f,w*.55f,h*.10f);
    shardB.close();

    spine.rewind();
    spine.moveTo(w*.06f,h*.62f);
    spine.cubicTo(w*.28f,h*.53f,w*.47f,h*.56f,w*.65f,h*.66f);
    spine.cubicTo(w*.76f,h*.72f,w*.86f,h*.74f,w*.98f,h*.70f);

    core.rewind();
    core.moveTo(cx,cy-w*.095f);
    core.lineTo(cx+w*.085f,cy-w*.012f);
    core.lineTo(cx+w*.050f,cy+w*.095f);
    core.lineTo(cx-w*.058f,cy+w*.085f);
    core.lineTo(cx-w*.090f,cy-w*.018f);
    core.close();
  }

  @Override protected void onDraw(Canvas c){
    fill.setShader(background);c.drawRect(0,0,w,h,fill);
    fill.setShader(aura);c.drawCircle(cx+px*.25f,cy+py*.25f,w*.46f,fill);

    c.save();c.translate(px*.45f,py*.45f);
    fill.setShader(shardAGradient);c.drawPath(shardA,fill);
    fill.setShader(shardBGradient);c.drawPath(shardB,fill);
    c.restore();

    stroke.setStyle(Paint.Style.STROKE);
    stroke.setStrokeCap(Paint.Cap.ROUND);
    stroke.setStrokeWidth(dp(1));
    stroke.setColor(0x1affffff);
    for(int i=0;i<7;i++){
      float y=h*(.17f+i*.062f);
      c.drawLine(w*.11f,y,w*(.92f-i*.035f),y-h*.055f,stroke);
    }

    c.save();c.translate(px*.70f,py*.70f);
    stroke.setShader(spineGradient);stroke.setStrokeWidth(dp(2.2f));c.drawPath(spine,stroke);stroke.setShader(null);
    c.restore();

    c.save();c.translate(px,py);
    fill.setShader(hotspot);c.drawCircle(cx,cy,w*.18f,fill);

    stroke.setStyle(Paint.Style.STROKE);
    stroke.setStrokeWidth(dp(2.2f));stroke.setShader(spectrum);
    RectF ring=new RectF(cx-w*.132f,cy-w*.132f,cx+w*.132f,cy+w*.132f);
    c.drawArc(ring,-34,242,false,stroke);
    stroke.setShader(null);

    fill.setShader(null);fill.setColor(0xdd0b0d14);c.drawPath(core,fill);
    stroke.setColor(0x88ffffff);stroke.setStrokeWidth(dp(1));c.drawPath(core,stroke);

    Path mark=new Path();
    mark.moveTo(cx-w*.030f,cy+w*.035f);
    mark.lineTo(cx,cy-w*.040f);
    mark.lineTo(cx+w*.032f,cy+w*.035f);
    mark.moveTo(cx-w*.016f,cy+w*.006f);
    mark.lineTo(cx+w*.015f,cy+w*.006f);
    stroke.setStrokeWidth(dp(3.0f));stroke.setColor(0xfff4f3ef);c.drawPath(mark,stroke);
    c.restore();

    stroke.setStrokeWidth(dp(1));stroke.setColor(0x335e6cff);
    c.drawLine(w*.08f,h*.82f,w*.78f,h*.82f,stroke);
    stroke.setColor(0x33ff4fd8);
    c.drawLine(w*.65f,h*.09f,w*.92f,h*.09f,stroke);
  }

  @Override public boolean onTouchEvent(MotionEvent e){
    switch(e.getActionMasked()){
      case MotionEvent.ACTION_DOWN: touching=true;updateParallax(e);return true;
      case MotionEvent.ACTION_MOVE: if(touching){updateParallax(e);return true;}break;
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        touching=false;px=py=0;invalidate();return true;
    }
    return super.onTouchEvent(e);
  }

  private void updateParallax(MotionEvent e){
    px=(e.getX()-w*.5f)*.018f;
    py=(e.getY()-h*.5f)*.012f;
    invalidate();
  }

  public void pulse(){
    animate().cancel();
    animate().scaleX(1.015f).scaleY(1.015f).setDuration(80).withEndAction(
        ()->animate().scaleX(1f).scaleY(1f).setDuration(180).start()).start();
  }

  @Override protected void onDetachedFromWindow(){
    animate().cancel();setScaleX(1);setScaleY(1);px=py=0;super.onDetachedFromWindow();
  }

  private float dp(float n){return n*getResources().getDisplayMetrics().density;}
}
