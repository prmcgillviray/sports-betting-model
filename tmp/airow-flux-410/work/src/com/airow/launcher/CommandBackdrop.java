package com.airow.launcher;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.View;

/** Static-at-idle atmospheric backdrop for AIROW Command Environment. */
public final class CommandBackdrop extends View {
  private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.DITHER_FLAG);
  private final Path far=new Path(),near=new Path(),mark=new Path();
  private LinearGradient sky,mountain,ground,energy;
  private RadialGradient horizon;
  private float w,h;

  public CommandBackdrop(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setLayerType(LAYER_TYPE_HARDWARE,null);}

  @Override protected void onSizeChanged(int width,int height,int ow,int oh){
    w=width;h=height;
    sky=new LinearGradient(0,0,0,h,new int[]{0xff05070b,0xff0a0c13,0xff08070d,0xff030406},null,Shader.TileMode.CLAMP);
    mountain=new LinearGradient(0,h*.08f,0,h*.48f,new int[]{0xff20232b,0xff090b10,0xff030407},null,Shader.TileMode.CLAMP);
    ground=new LinearGradient(0,h*.38f,0,h,new int[]{0x00101820,0xff070810,0xff030407},null,Shader.TileMode.CLAMP);
    energy=new LinearGradient(w*.42f,h*.12f,w*.69f,h*.39f,new int[]{0x00ff6b35,0xffff6b35,0xffffb14d,0x00ff6b35},null,Shader.TileMode.CLAMP);
    horizon=new RadialGradient(w*.57f,h*.33f,w*.42f,new int[]{0x44ff6b35,0x222d174f,0x00000000},null,Shader.TileMode.CLAMP);

    far.rewind();far.moveTo(0,h*.37f);far.lineTo(w*.12f,h*.29f);far.lineTo(w*.22f,h*.33f);far.lineTo(w*.39f,h*.17f);far.lineTo(w*.48f,h*.27f);far.lineTo(w*.61f,h*.10f);far.lineTo(w*.72f,h*.26f);far.lineTo(w*.82f,h*.18f);far.lineTo(w,h*.34f);far.lineTo(w,h*.47f);far.lineTo(0,h*.47f);far.close();
    near.rewind();near.moveTo(0,h*.40f);near.lineTo(w*.18f,h*.32f);near.lineTo(w*.31f,h*.41f);near.lineTo(w*.47f,h*.29f);near.lineTo(w*.58f,h*.40f);near.lineTo(w*.76f,h*.31f);near.lineTo(w,h*.43f);near.lineTo(w,h);near.lineTo(0,h);near.close();

    mark.rewind();mark.moveTo(w*.48f,h*.30f);mark.lineTo(w*.61f,h*.12f);mark.lineTo(w*.75f,h*.32f);
    mark.moveTo(w*.55f,h*.30f);mark.lineTo(w*.66f,h*.20f);mark.lineTo(w*.73f,h*.32f);
  }

  @Override protected void onDraw(Canvas c){
    p.setStyle(Paint.Style.FILL);p.setShader(sky);c.drawRect(0,0,w,h,p);
    p.setShader(horizon);c.drawCircle(w*.57f,h*.30f,w*.42f,p);
    p.setShader(mountain);c.drawPath(far,p);
    p.setShader(null);p.setColor(0xaa05070b);c.drawPath(near,p);
    p.setShader(ground);c.drawRect(0,h*.39f,w,h,p);

    p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
    p.setStrokeWidth(dp(4));p.setShader(energy);c.drawPath(mark,p);p.setShader(null);

    p.setStrokeWidth(dp(1));p.setColor(0x22ffffff);
    for(int i=0;i<7;i++){float y=h*(.09f+i*.052f);c.drawLine(w*.05f,y,w*(.28f+i*.05f),y-dp(12),p);}
    p.setColor(0x332c0e5a);for(int i=0;i<5;i++){float y=h*(.58f+i*.07f);c.drawLine(0,y,w,y-dp(24),p);}
  }

  private float dp(float n){return n*getResources().getDisplayMetrics().density;}
}
