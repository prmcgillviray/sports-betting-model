package com.airow.launcher;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/**
 * AIROW Aperture Flux sculpture.
 * Fully procedural and static at idle: geometry and shaders rebuild only when size changes.
 */
public final class ApertureArtwork extends View {
  private final Paint fill=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.DITHER_FLAG);
  private final Paint line=new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Path bladeA=new Path(),bladeB=new Path(),core=new Path(),cut=new Path();
  private LinearGradient steelA,steelB,heat,glass;
  private float w,h;

  public ApertureArtwork(Context context){
    super(context);
    setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    setLayerType(View.LAYER_TYPE_HARDWARE,null);
  }

  @Override protected void onSizeChanged(int width,int height,int oldW,int oldH){
    w=width;h=height;
    bladeA.rewind();
    bladeA.moveTo(w*.15f,h*.26f);
    bladeA.cubicTo(w*.36f,h*.09f,w*.69f,h*.08f,w*.90f,h*.19f);
    bladeA.cubicTo(w*.70f,h*.30f,w*.63f,h*.51f,w*.70f,h*.77f);
    bladeA.cubicTo(w*.48f,h*.66f,w*.30f,h*.55f,w*.15f,h*.26f);
    bladeA.close();

    bladeB.rewind();
    bladeB.moveTo(w*.67f,h*.17f);
    bladeB.cubicTo(w*.82f,h*.28f,w*.91f,h*.43f,w*.96f,h*.66f);
    bladeB.cubicTo(w*.78f,h*.56f,w*.59f,h*.55f,w*.38f,h*.63f);
    bladeB.cubicTo(w*.46f,h*.44f,w*.54f,h*.29f,w*.67f,h*.17f);
    bladeB.close();

    core.rewind();
    core.moveTo(w*.32f,h*.42f);
    core.cubicTo(w*.44f,h*.30f,w*.60f,h*.31f,w*.71f,h*.41f);
    core.cubicTo(w*.62f,h*.46f,w*.55f,h*.55f,w*.50f,h*.67f);
    core.cubicTo(w*.43f,h*.56f,w*.37f,h*.49f,w*.32f,h*.42f);
    core.close();

    cut.rewind();
    cut.moveTo(w*.42f,h*.37f);
    cut.cubicTo(w*.51f,h*.31f,w*.60f,h*.34f,w*.64f,h*.42f);
    cut.cubicTo(w*.57f,h*.46f,w*.51f,h*.53f,w*.47f,h*.60f);
    cut.cubicTo(w*.43f,h*.52f,w*.40f,h*.45f,w*.42f,h*.37f);
    cut.close();

    steelA=new LinearGradient(w*.12f,h*.18f,w*.83f,h*.70f,
        new int[]{0xff17191d,0xffd8d6d0,0xff55565a,0xfff2eee5,0xff25272b},
        new float[]{0,.24f,.46f,.67f,1},Shader.TileMode.CLAMP);
    steelB=new LinearGradient(w*.80f,h*.14f,w*.43f,h*.72f,
        new int[]{0xff0f1115,0xff8c8d91,0xfff3efe7,0xff303237},
        new float[]{0,.34f,.58f,1},Shader.TileMode.CLAMP);
    heat=new LinearGradient(w*.28f,h*.31f,w*.66f,h*.66f,
        new int[]{0xffff3f28,0xff9c1409,0xff240806},null,Shader.TileMode.CLAMP);
    glass=new LinearGradient(0,h*.15f,0,h*.72f,
        new int[]{0x55ffffff,0x08ffffff,0x00000000},null,Shader.TileMode.CLAMP);
  }

  @Override protected void onDraw(Canvas c){
    fill.setStyle(Paint.Style.FILL);
    fill.setShader(steelA);c.drawPath(bladeA,fill);
    fill.setShader(steelB);c.drawPath(bladeB,fill);
    fill.setShader(heat);c.drawPath(core,fill);
    fill.setShader(null);fill.setColor(0xff08090b);c.drawPath(cut,fill);

    fill.setShader(glass);c.drawPath(bladeA,fill);fill.setShader(null);

    line.setStyle(Paint.Style.STROKE);
    line.setStrokeWidth(dp(1));
    line.setColor(0x66ffffff);
    c.drawLine(w*.19f,h*.28f,w*.76f,h*.15f,line);
    c.drawLine(w*.29f,h*.39f,w*.79f,h*.31f,line);
    c.drawLine(w*.39f,h*.57f,w*.77f,h*.52f,line);

    line.setStrokeWidth(dp(2));
    line.setColor(0xffff5a43);
    c.drawLine(w*.30f,h*.43f,w*.53f,h*.64f,line);
    line.setStrokeWidth(dp(1));
    line.setColor(0x55ff5a43);
    c.drawLine(w*.27f,h*.46f,w*.49f,h*.68f,line);
  }

  public void pulse(){
    animate().cancel();
    animate().scaleX(1.018f).scaleY(1.018f).setDuration(90).withEndAction(
        ()->animate().scaleX(1f).scaleY(1f).setDuration(170).start()).start();
  }

  @Override protected void onDetachedFromWindow(){
    animate().cancel();setScaleX(1);setScaleY(1);setTranslationX(0);
    super.onDetachedFromWindow();
  }

  private float dp(float n){return n*getResources().getDisplayMetrics().density;}
}
