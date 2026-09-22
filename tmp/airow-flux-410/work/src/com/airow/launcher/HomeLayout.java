package com.airow.launcher;

/** Density-independent geometry shared by the native view and layout checks. */
public final class HomeLayout {
  public final float width, height, contextY, actionsY, artY, artHeight, actionHeight;
  public HomeLayout(float width, float height, float fontScale) {
    this.width = width;
    this.height = Math.max(height, 780 + Math.max(0, fontScale - 1) * 240);
    contextY = this.height - 260;
    actionHeight = Math.max(58, 44 * fontScale);
    actionsY = Math.max(270, contextY - 3 * (actionHeight + 4) - 38);
    artY = 126;
    artHeight = contextY - artY + 60;
  }
  public float actionY(int i) { return actionsY + i * (actionHeight + 4); }
  public float searchY() { return height - 72; }
  public float[] bounds(String name) {
    float w=width,c=contextY;
    switch(name){
      case "art":return new float[]{0,artY,w,artHeight};
      case "brand":return new float[]{24,12,w-100,48};
      case "settings":return new float[]{w-72,12,48,48};
      case "gestureHint":return new float[]{24,54,w-96,14};
      case "date":return new float[]{26,72,w-52,24};
      case "clock":return new float[]{21,103,w-42,106};
      case "connect":return new float[]{26,actionY(0),w-52,actionHeight};
      case "create":return new float[]{26,actionY(1),w-52,actionHeight};
      case "explore":return new float[]{26,actionY(2),w-52,actionHeight};
      case "contextLabel":return new float[]{26,c,w-52,20};
      case "contextText":return new float[]{26,c+20,w-52,48};
      case "modelText":return new float[]{26,c+68,w-52,48};
      case "rule":return new float[]{26,c+120,w-52,1};
      case "mediaText":return new float[]{26,c+126,w-110,48};
      case "mediaControl":return new float[]{w-74,c+126,48,48};
      case "search":return new float[]{24,searchY(),w-48,56};
      default:throw new IllegalArgumentException(name);
    }
  }
}
