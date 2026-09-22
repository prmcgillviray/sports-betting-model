package com.airow.launcher;

import android.app.*;
import android.app.role.RoleManager;
import android.content.*;
import android.content.pm.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.MediaMetadata;
import android.media.session.*;
import android.media.session.MediaController;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.provider.Telephony;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** AIROW Command Environment launcher: dense live dashboard over an atmospheric spatial environment. */
public final class MainActivity extends Activity {
  private static final String BOARD="https://nfl-airow-edge.netlify.app/";
  private static final String TRADING_DEFAULT="http://raspberrypi.local:8765/";
  private static final int INK=0xff08090b, SURFACE=0xff151619,
      WHITE=0xfff5f3ef, MUTED=0xffa4a5ab, ACCENT=0xffff644c;
  private SharedPreferences prefs;
  private FrameLayout content;
  private CommandHome home;
  private LinearLayout sportsBody, musicBody;
  private final Handler handler=new Handler(Looper.getMainLooper());
  private final ExecutorService worker=Executors.newSingleThreadExecutor();
  private final ArrayList<AppEntry> apps=new ArrayList<>();
  private ArrayList<AppEntry> filtered=new ArrayList<>();
  private BaseAdapter appAdapter;
  private EditText appSearch;
  private TextView appEmpty;
  private String appQuery="";
  private MediaSessionManager sessions;
  private MediaController spotify;
  private NflSnapshot snapshot;
  private NflBoard board;
  private int page;
  private boolean visible,listening,fetching,sportsFetching,appsLoading,choosingAssistant,powerSaving;
  private long lastAttempt,lastSportsAttempt,appsLoadedAt;
  private String networkNote="",sportsError="";
  private final MediaController.Callback mediaCallback=new MediaController.Callback(){
    @Override public void onMetadataChanged(MediaMetadata m){renderMusic();}
    @Override public void onPlaybackStateChanged(PlaybackState s){renderMusic();}
    @Override public void onSessionDestroyed(){detachSpotify();renderMusic();}
  };
  private final MediaSessionManager.OnActiveSessionsChangedListener sessionListener=list->selectSpotify(list);
  private final BroadcastReceiver clockReceiver=new BroadcastReceiver(){
    @Override public void onReceive(Context c,Intent i){
      if(home!=null)home.refreshDate();
      PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
      powerSaving=pm!=null&&pm.isPowerSaveMode();
    }
  };
  private final BroadcastReceiver packageReceiver=new BroadcastReceiver(){
    @Override public void onReceive(Context c,Intent i){appsLoadedAt=0;if(visible)loadApps(true);}
  };
  private final Runnable freshnessTick=new Runnable(){public void run(){
    if(!visible)return;
    renderNfl();renderSportsWire();refreshNfl(false);refreshSports(false);
    handler.postDelayed(this,300000);
  }};

  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    prefs=getSharedPreferences("airow",MODE_PRIVATE);
    lastAttempt=prefs.getLong("nfl_last_attempt",0);
    lastSportsAttempt=prefs.getLong("sports_last_attempt",0);
    sessions=(MediaSessionManager)getSystemService(MEDIA_SESSION_SERVICE);
    getWindow().setStatusBarColor(INK);getWindow().setNavigationBarColor(INK);
    getWindow().getDecorView().setSystemUiVisibility(0);
    if(Build.VERSION.SDK_INT>=30){
      getWindow().setDecorFitsSystemWindows(false);
      WindowInsetsController controller=getWindow().getInsetsController();
      if(controller!=null)controller.setSystemBarsAppearance(0,
          WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
    }
    content=new FrameLayout(this);content.setBackgroundColor(INK);
    content.setOnApplyWindowInsetsListener((v,in)->{
      if(Build.VERSION.SDK_INT>=30){
        Insets bars=in.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());
        v.setPadding(bars.left,bars.top,bars.right,bars.bottom);
      }else v.setPadding(in.getSystemWindowInsetLeft(),in.getSystemWindowInsetTop(),in.getSystemWindowInsetRight(),in.getSystemWindowInsetBottom());
      return in;
    });
    setContentView(content);
    IntentFilter packages=new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
    packages.addAction(Intent.ACTION_PACKAGE_REMOVED);packages.addAction(Intent.ACTION_PACKAGE_CHANGED);packages.addDataScheme("package");
    registerReceiver(packageReceiver,packages);
    try{snapshot=NflSnapshot.parse(prefs.getString("nfl_cache",""),System.currentTimeMillis());}catch(Exception ignored){}
    try{board=NflBoard.parseCache(prefs.getString("nfl_board_cache",""));}catch(Exception ignored){}
    if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::back);
    showHome();
  }
  @Override protected void onStart(){
    super.onStart();visible=true;loadApps(false);connectMusic();
    IntentFilter times=new IntentFilter(Intent.ACTION_DATE_CHANGED);
    times.addAction(Intent.ACTION_TIME_CHANGED);times.addAction(Intent.ACTION_TIMEZONE_CHANGED);
    times.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
    registerReceiver(clockReceiver,times);
    PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);powerSaving=pm!=null&&pm.isPowerSaveMode();
    if(home!=null)home.refreshDate();
    handler.removeCallbacks(freshnessTick);freshnessTick.run();
  }
  @Override protected void onStop(){
    visible=false;content.animate().cancel();content.setTranslationY(0);content.setAlpha(1);
    handler.removeCallbacks(freshnessTick);disconnectMusic();
    try{unregisterReceiver(clockReceiver);}catch(IllegalArgumentException ignored){}
    super.onStop();
  }
  @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);showHome();}
  @Override protected void onDestroy(){
    handler.removeCallbacksAndMessages(null);worker.shutdownNow();
    try{unregisterReceiver(packageReceiver);}catch(IllegalArgumentException ignored){}
    super.onDestroy();
  }
  @Override public void onBackPressed(){back();}
  private void back(){if(page!=0)showHome();else if(!isDefaultHome())finish();}
  private void clearPage(int nextPage){
    content.animate().cancel();content.setAlpha(1);content.setTranslationY(0);
    ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(content.getWindowToken(),0);
    content.removeAllViews();home=null;sportsBody=null;musicBody=null;appSearch=null;appEmpty=null;appAdapter=null;page=nextPage;
  }
  private void enter(){
    if(!visible||powerSaving||!android.animation.ValueAnimator.areAnimatorsEnabled())return;
    content.setTranslationY(dp(16));content.setAlpha(.7f);
    content.animate().translationY(0).alpha(1).setDuration(180).start();
  }
  private void showHome(){
    clearPage(0);
    home=new CommandHome(this,this::go);
    content.addView(home,new FrameLayout.LayoutParams(-1,-1));
    home.refreshDate();renderNfl();renderSportsWire();renderMusic();
  }
  private void go(String action){
    switch(action){
      case "connect":showConnect();break;
      case "create":showCreate();break;
      case "apps":showApps(false);break;
      case "search":showApps(false);if(appSearch!=null){appSearch.requestFocus();appSearch.postDelayed(()->{
        if(page==1&&appSearch!=null)((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(appSearch,InputMethodManager.SHOW_IMPLICIT);
      },200);}break;
      case "sports":showBoard();break;
      case "music":showMusic();break;
      case "play":transport("play");break;
      case "settings":showSettings();break;
      case "assistant":openAssistant();break;
      case "capture":quickCapture();break;
      case "phone":open(new Intent(Intent.ACTION_DIAL));break;
      case "messages":openMessages();break;
      case "camera":open(new Intent("android.media.action.STILL_IMAGE_CAMERA"));break;
      case "trade":openTrading();break;
    }
  }
  private LinearLayout screen(int id,String title,String subtitle){
    clearPage(id);LinearLayout outer=column();outer.setPadding(dp(24),dp(8),dp(24),dp(16));
    TextView back=action("←  Home",14,MUTED,v->showHome());outer.addView(back,new LinearLayout.LayoutParams(-1,dp(48)));
    TextView heading=text(title,42,WHITE);heading.setTypeface(Typeface.create("sans-serif-light",0));outer.addView(heading);
    if(!subtitle.isEmpty())outer.addView(text(subtitle,13,MUTED));
    space(outer,16);content.addView(outer,new FrameLayout.LayoutParams(-1,-1));enter();return outer;
  }
  private LinearLayout scrollBody(LinearLayout outer){
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
    LinearLayout body=column();scroll.addView(body);outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));return body;
  }
  private void showConnect(){
    LinearLayout body=scrollBody(screen(2,"Connect.","A person. A place. A conversation."));
    rowAction(body,"Call someone","Open your dialer","↗",()->go("phone"));
    rowAction(body,"Messages","Open your messaging app","↗",this::openMessages);
    rowAction(body,"Get directions","Choose a place in Maps","↗",()->open(new Intent(Intent.ACTION_VIEW,Uri.parse("geo:0,0?q="))));
    rowAction(body,"Ask your assistant","Opens your chosen assistant app","↗",this::openAssistant);
  }
  private void showCreate(){
    LinearLayout body=scrollBody(screen(4,"Create.","Start something. Keep the thought."));
    rowAction(body,"Capture a thought","Saved privately on this phone","+",this::quickCapture);
    rowAction(body,"Ask your assistant","Your configured app handles the conversation","↗",this::openAssistant);
    rowAction(body,"Open camera","Photo or video","↗",()->go("camera"));
    space(body,22);body.addView(text("YOUR CAPTURES",11,ACCENT));
    JSONArray notes=notes();
    if(notes.length()==0)body.addView(text("An empty page, ready when you are.",16,MUTED));
    for(int i=notes.length()-1;i>=0;i--){
      final int index=i;JSONObject note=notes.optJSONObject(i);if(note==null)continue;
      String value=note.optString("text","");
      rowAction(body,value,"Tap to edit, copy, or delete","↗",()->editNote(index));
    }
  }
  private JSONArray notes(){try{return new JSONArray(prefs.getString("captures","[]"));}catch(Exception e){return new JSONArray();}}
  private void quickCapture(){editNote(-1);}
  private void editNote(int index){
    JSONArray saved=notes();JSONObject existing=index<0?null:saved.optJSONObject(index);
    EditText input=input("A thought, a task, a link…");input.setSingleLine(false);input.setMinLines(4);
    input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4000)});
    if(existing!=null)input.setText(existing.optString("text",""));
    AlertDialog dialog=new AlertDialog.Builder(this).setTitle(index<0?"Capture":"Your thought").setView(input)
        .setPositiveButton("Save",null).setNegativeButton("Cancel",null)
        .setNeutralButton(index<0?"Copy":"More",null).create();
    dialog.setOnShowListener(d->{
      dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
        String value=input.getText().toString().trim();if(value.isEmpty()){input.setError("Write a thought first");return;}
        JSONArray current=notes();if(index<0&&current.length()>=100){input.setError("100 captures saved. Delete an older capture first.");return;}
        try{JSONObject note=new JSONObject().put("text",value).put("time",System.currentTimeMillis());if(index<0)current.put(note);else current.put(index,note);
          prefs.edit().putString("captures",current.toString()).apply();dialog.dismiss();showCreate();
        }catch(JSONException error){toast("Could not save capture");}
      });
      dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{
        String value=input.getText().toString();
        if(index<0){copy(value);return;}
        new AlertDialog.Builder(this).setItems(new String[]{"Copy text","Delete capture"},(d2,choice)->{
          if(choice==0)copy(value);
          else new AlertDialog.Builder(this).setTitle("Delete this capture?").setPositiveButton("Delete",(d3,b)->{
            JSONArray current=notes();current.remove(index);prefs.edit().putString("captures",current.toString()).apply();dialog.dismiss();showCreate();
          }).setNegativeButton("Keep",null).show();
        }).show();
      });
    });dialog.show();
  }
  private void copy(String text){((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("AIROW capture",text));toast("Copied");}

  private static final class AppEntry{
    final ComponentName component;final String name,key;final Drawable icon;
    AppEntry(ComponentName c,String n,Drawable i){component=c;name=n;key=n.toLowerCase(Locale.ROOT);icon=i;}
  }
  private void loadApps(boolean force){
    long now=System.currentTimeMillis();if(appsLoading||(!force&&!apps.isEmpty()&&now-appsLoadedAt<21600000))return;
    appsLoading=true;
    worker.execute(()->{
      ArrayList<AppEntry> found=new ArrayList<>();
      try{
        Intent intent=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        for(ResolveInfo item:getPackageManager().queryIntentActivities(intent,0)){
          if(!item.activityInfo.exported||getPackageName().equals(item.activityInfo.packageName))continue;
          ComponentName component=new ComponentName(item.activityInfo.packageName,item.activityInfo.name);
          found.add(new AppEntry(component,item.loadLabel(getPackageManager()).toString(),item.loadIcon(getPackageManager())));
        }
        found.sort((a,b)->String.CASE_INSENSITIVE_ORDER.compare(a.name,b.name));
      }catch(Exception ignored){}
      handler.post(()->{if(isDestroyed())return;appsLoading=false;appsLoadedAt=System.currentTimeMillis();apps.clear();apps.addAll(found);filterApps();});
    });
  }
  private void showApps(boolean chooseAssistant){
    LinearLayout outer=screen(1,chooseAssistant?"Your assistant.":"The field.",
        chooseAssistant?"Choose the app opened by Ask.":"Type an intent, app, or command. Hold an app to magnetize it.");
    choosingAssistant=chooseAssistant;appQuery="";
    appSearch=input("Command, app, or intent…");appSearch.setSingleLine(true);
    GradientDrawable searchBg=bg(0xff111317,4);searchBg.setStroke(dp(1),0x55ff5a43);appSearch.setBackground(searchBg);
    outer.addView(appSearch,new LinearLayout.LayoutParams(-1,dp(56)));

    if(!chooseAssistant){
      LinearLayout quick=new LinearLayout(this);quick.setGravity(Gravity.CENTER_VERTICAL);
      quick.addView(action("ASK",12,ACCENT,v->openAssistant()),new LinearLayout.LayoutParams(0,dp(46),1));
      quick.addView(action("CAPTURE",12,ACCENT,v->quickCapture()),new LinearLayout.LayoutParams(0,dp(46),1));
      quick.addView(action("RADAR",12,ACCENT,v->showBoard()),new LinearLayout.LayoutParams(0,dp(46),1));
      quick.addView(action("LISTEN",12,ACCENT,v->showMusic()),new LinearLayout.LayoutParams(0,dp(46),1));
      outer.addView(quick);
      outer.addView(text("MAGNETIZED",10,ACCENT));
      outer.addView(pinnedStrip(),new LinearLayout.LayoutParams(-1,dp(88)));
      space(outer,8);
    }

    GridView grid=new GridView(this);
    grid.setNumColumns(2);grid.setVerticalSpacing(dp(3));grid.setHorizontalSpacing(dp(3));
    grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);grid.setSelector(new ColorDrawable(0x22ff5a43));
    TextView empty=text(appsLoading?"Indexing your field…":"No match. Try a different intent or refresh.",16,MUTED);
    outer.addView(empty);appEmpty=empty;grid.setEmptyView(empty);

    appAdapter=new BaseAdapter(){
      public int getCount(){return filtered.size();}
      public Object getItem(int pos){return filtered.get(pos);}
      public long getItemId(int pos){return pos;}
      public View getView(int pos,View recycled,ViewGroup parent){
        LinearLayout cell;
        if(recycled instanceof LinearLayout)cell=(LinearLayout)recycled;
        else{
          cell=new LinearLayout(MainActivity.this);cell.setOrientation(LinearLayout.HORIZONTAL);
          cell.setGravity(Gravity.CENTER_VERTICAL);cell.setPadding(dp(10),dp(8),dp(8),dp(8));
          cell.setMinimumHeight(dp(78));cell.setBackground(bg(0xff111317,3));
          ImageView icon=new ImageView(MainActivity.this);cell.addView(icon,new LinearLayout.LayoutParams(dp(34),dp(34)));
          TextView name=text("",15,WHITE);name.setSingleLine(true);name.setEllipsize(TextUtils.TruncateAt.END);
          LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.leftMargin=dp(10);cell.addView(name,lp);
          TextView pin=text("",11,ACCENT);pin.setGravity(Gravity.CENTER);cell.addView(pin,new LinearLayout.LayoutParams(dp(24),-1));
        }
        AppEntry app=filtered.get(pos);
        ((ImageView)cell.getChildAt(0)).setImageDrawable(app.icon);
        ((TextView)cell.getChildAt(1)).setText(app.name);
        ((TextView)cell.getChildAt(2)).setText(prefs.getStringSet("pins",Collections.emptySet()).contains(app.component.flattenToString())?"◆":"");
        return cell;
      }
    };
    grid.setAdapter(appAdapter);
    grid.setOnItemClickListener((parent,v,pos,id)->{
      ComponentName component=filtered.get(pos).component;
      if(choosingAssistant){prefs.edit().putString("assistant",component.flattenToString()).apply();showHome();}
      else launch(component);
    });
    grid.setOnItemLongClickListener((parent,v,pos,id)->{
      if(choosingAssistant)return false;
      String key=filtered.get(pos).component.flattenToString();
      Set<String> pins=new HashSet<>(prefs.getStringSet("pins",Collections.emptySet()));
      boolean removed=pins.remove(key);if(!removed)pins.add(key);
      prefs.edit().putStringSet("pins",pins).apply();
      v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
      filterApps();toast(removed?"Released from the field":"Magnetized");return true;
    });

    appSearch.addTextChangedListener(new TextWatcher(){
      public void beforeTextChanged(CharSequence s,int start,int count,int after){}
      public void onTextChanged(CharSequence s,int start,int before,int count){appQuery=s.toString();filterApps();}
      public void afterTextChanged(Editable e){}
    });
    appSearch.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_GO);
    appSearch.setOnEditorActionListener((v,action,event)->{
      if(action!=android.view.inputmethod.EditorInfo.IME_ACTION_GO)return false;
      if(!chooseAssistant&&runCommand(appQuery))return true;
      if(filtered.size()==1){
        ComponentName component=filtered.get(0).component;
        if(chooseAssistant){prefs.edit().putString("assistant",component.flattenToString()).apply();showHome();}
        else launch(component);
        return true;
      }
      toast("Tap a result or type a command");return true;
    });
    outer.addView(grid,new LinearLayout.LayoutParams(-1,0,1));
    outer.addView(action("Refresh field index",12,MUTED,v->loadApps(true)),new LinearLayout.LayoutParams(-1,dp(44)));
    filterApps();loadApps(false);
  }

  private View pinnedStrip(){
    HorizontalScrollView scroll=new HorizontalScrollView(this);scroll.setHorizontalScrollBarEnabled(false);
    LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(4),0,dp(4));
    Set<String> pins=prefs.getStringSet("pins",Collections.emptySet());
    int shown=0;
    for(AppEntry app:apps){
      if(!pins.contains(app.component.flattenToString()))continue;
      final ComponentName component=app.component;
      LinearLayout chip=new LinearLayout(this);chip.setOrientation(LinearLayout.VERTICAL);chip.setGravity(Gravity.CENTER);
      chip.setPadding(dp(8),dp(4),dp(8),dp(4));chip.setBackground(bg(0xff15171b,4));chip.setContentDescription("Pinned "+app.name);
      ImageView icon=new ImageView(this);icon.setImageDrawable(app.icon);chip.addView(icon,new LinearLayout.LayoutParams(dp(34),dp(34)));
      TextView name=text(app.name,10,WHITE);name.setSingleLine(true);name.setGravity(Gravity.CENTER);name.setMaxWidth(dp(86));chip.addView(name);
      chip.setOnClickListener(v->launch(component));
      chip.setOnLongClickListener(v->{Set<String> current=new HashSet<>(prefs.getStringSet("pins",Collections.emptySet()));current.remove(component.flattenToString());prefs.edit().putStringSet("pins",current).apply();showApps(false);return true;});
      LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(94),dp(76));lp.rightMargin=dp(4);row.addView(chip,lp);shown++;
      if(shown>=8)break;
    }
    if(shown==0){
      TextView empty=text("Hold any app below to magnetize it here.",12,MUTED);row.addView(empty,new LinearLayout.LayoutParams(-2,dp(68)));
    }
    scroll.addView(row);return scroll;
  }

  private boolean runCommand(String query){
    String q=query.trim().toLowerCase(Locale.ROOT);
    if(Arrays.asList("ask","assistant","capture","sports","radar","phone","messages","camera","trade","listen","music","link","make","roam","apps").contains(q)){
      if(q.equals("ask"))q="assistant";
      else if(q.equals("radar"))q="sports";
      else if(q.equals("listen"))q="music";
      else if(q.equals("link"))q="connect";
      else if(q.equals("make"))q="create";
      else if(q.equals("roam"))q="apps";
      go(q);return true;
    }return false;
  }
  private void filterApps(){
    if(page!=1||appAdapter==null)return;
    filtered.clear();String query=appQuery.trim().toLowerCase(Locale.ROOT);
    if(appEmpty!=null){
      boolean command=!choosingAssistant&&Arrays.asList("ask","assistant","capture","sports","radar","phone","messages","camera","trade","listen","music","link","make","roam","apps").contains(query);
      appEmpty.setText(command?"Press Go on the keyboard to open "+query+".":appsLoading?"Loading apps…":"No matching apps. Try another name or refresh below.");
    }
    for(AppEntry a:apps)if(a.key.contains(query))filtered.add(a);
    Set<String> pins=prefs.getStringSet("pins",Collections.emptySet());
    filtered.sort((a,b)->{
      int ap=pins.contains(a.component.flattenToString())?0:1,bp=pins.contains(b.component.flattenToString())?0:1;
      return ap==bp?String.CASE_INSENSITIVE_ORDER.compare(a.name,b.name):Integer.compare(ap,bp);
    });appAdapter.notifyDataSetChanged();
  }

  private String modelStatus(){
    if(board==null||board.games.isEmpty())return fetching?"Model · checking":"Model unavailable";
    return "Model · "+ModelFreshness.describe(board,System.currentTimeMillis(),prefs.getInt("sports_nfl_season",0),prefs.getInt("sports_nfl_week",0));
  }
  private String scoreStatus(){
    long age=System.currentTimeMillis()-prefs.getLong("sports_wire_time",0);
    if(prefs.getString("sports_wire_cache","").isEmpty())return sportsFetching?"Checking scores":sportsError.isEmpty()?"Scores unavailable":"Scores offline";
    if(age<0)return "Scores · timestamp unknown";
    return (age>1800000?"Saved scores · ":"Updated ")+Math.max(0,age/60000)+"m ago";
  }
  private void renderNfl(){renderSportsWire();}
  private void renderSportsWire(){
    String scores=prefs.getBoolean("sports_wire",true)?prefs.getString("sports_wire_cache",""):"";
    String first=scores.isEmpty()?(prefs.getBoolean("sports_wire",true)?scoreStatus():"Sports paused"):scores.split("\\s+•\\s+")[0].trim();
    if(home!=null)home.context(first,modelStatus()+" · "+scoreStatus());
    if(sportsBody!=null)renderBoard();
  }
  private void showBoard(){
    LinearLayout outer=screen(3,"The game.","Real scores. Published model projections.");
    LinearLayout row=new LinearLayout(this);
    row.addView(action("Refresh",14,ACCENT,v->{refreshNfl(true);refreshSports(true);}),new LinearLayout.LayoutParams(0,dp(48),1));
    row.addView(action("Full website ↗",14,MUTED,v->open(new Intent(Intent.ACTION_VIEW,Uri.parse(BOARD)))),new LinearLayout.LayoutParams(0,dp(48),1));
    outer.addView(row);sportsBody=scrollBody(outer);renderBoard();
  }
  private void renderBoard(){
    if(sportsBody==null)return;sportsBody.removeAllViews();
    sportsBody.addView(text(scoreStatus(),12,ACCENT));
    String wire=prefs.getString("sports_wire_cache","");
    if(!prefs.getBoolean("sports_wire",true))sportsBody.addView(text("Sports scores are paused in settings.",16,MUTED));
    else if(wire.isEmpty())sportsBody.addView(text("No score data available.",18,WHITE));
    else for(String line:wire.split("\\s+•\\s+")){sportsBody.addView(text(line.trim(),17,WHITE));space(sportsBody,8);}
    space(sportsBody,24);sportsBody.addView(text(modelStatus(),16,ACCENT));
    sportsBody.addView(text(networkNote,12,MUTED));
    if(board==null){sportsBody.addView(text("No model projections available. Refresh to try again.",16,MUTED));return;}
    sportsBody.addView(text(board.season>0?board.season+" · Week "+board.week:"Model period unavailable",13,MUTED));
    sportsBody.addView(text("Published: "+(board.publishedAt.isEmpty()?"Unknown":board.publishedAt),12,MUTED));
    sportsBody.addView(text("Generated: "+(board.generatedAt.isEmpty()?"Unknown":board.generatedAt),12,MUTED));
    if(snapshot!=null)sportsBody.addView(text("Status feed · "+snapshot.summary(System.currentTimeMillis())+" · "+snapshot.date(),12,MUTED));
    if(!board.bettingStatus.isEmpty())sportsBody.addView(text(board.bettingStatus,13,MUTED));
    for(NflBoard.Game game:board.games){
      space(sportsBody,24);divider(sportsBody);
      sportsBody.addView(text(game.away+"  /  "+game.home,24,WHITE));
      sportsBody.addView(text(game.kickoff,13,MUTED));
      sportsBody.addView(text(String.format(Locale.US,"%.1f   —   %.1f",game.projectedAway,game.projectedHome),38,WHITE));
      sportsBody.addView(text("MODEL PROJECTION · NOT A LIVE SCORE",10,ACCENT));
      sportsBody.addView(text(game.spread+"    Total "+String.format(Locale.US,"%.1f",game.total)+"    "+game.home+" "+Math.round(game.homeWin*100)+"%",14,MUTED));
    }
  }
  private void showMusic(){musicBody=scrollBody(screen(5,"Listening.","Spotify, when you want it."));renderMusic();}
  private void renderMusic(){
    MediaMetadata metadata=spotify==null?null:spotify.getMetadata();
    PlaybackState state=spotify==null?null:spotify.getPlaybackState();
    boolean playing=state!=null&&state.getState()==PlaybackState.STATE_PLAYING;
    String title=metadata==null?null:metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
    String artist=metadata==null?null:metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
    if(home!=null)home.media(title==null?"Listen  ↗":(playing?"Playing · ":"Paused · ")+title,playing);
    if(musicBody==null)return;musicBody.removeAllViews();
    Bitmap cover=metadata==null?null:metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
    if(cover==null&&metadata!=null)cover=metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
    if(cover!=null){ImageView image=new ImageView(this);image.setImageBitmap(cover);image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setContentDescription("Album artwork");musicBody.addView(image,new LinearLayout.LayoutParams(-1,dp(240)));space(musicBody,20);}
    musicBody.addView(text(title==null?"A little silence.":title,32,WHITE));
    musicBody.addView(text(artist==null?"Start a track in Spotify.":artist,16,MUTED));
    if(spotify!=null){
      long available=state==null?0:state.getActions();
      LinearLayout controls=new LinearLayout(this);
      TextView previous=action("Previous",15,WHITE,v->transport("prev"));
      TextView toggle=action(playing?"Pause":"Play",20,ACCENT,v->transport("play"));
      TextView next=action("Next",15,WHITE,v->transport("next"));
      previous.setEnabled((available&PlaybackState.ACTION_SKIP_TO_PREVIOUS)!=0);previous.setAlpha(previous.isEnabled()?1:.35f);
      next.setEnabled((available&PlaybackState.ACTION_SKIP_TO_NEXT)!=0);next.setAlpha(next.isEnabled()?1:.35f);
      toggle.setEnabled((available&(PlaybackState.ACTION_PLAY_PAUSE|(playing?PlaybackState.ACTION_PAUSE:PlaybackState.ACTION_PLAY)))!=0);
      toggle.setAlpha(toggle.isEnabled()?1:.35f);
      controls.addView(previous,new LinearLayout.LayoutParams(0,dp(64),1));controls.addView(toggle,new LinearLayout.LayoutParams(0,dp(64),1));controls.addView(next,new LinearLayout.LayoutParams(0,dp(64),1));musicBody.addView(controls);
    }
    rowAction(musicBody,"Open Spotify","Browse your music","↗",this::openSpotify);
    if(!musicAllowed())rowAction(musicBody,"Enable playback controls","Optional notification access","↗",this::explainMusic);
  }
  private void showSettings(){
    LinearLayout body=scrollBody(screen(6,"Make it yours.","AIROW Neural Space · 5.0.0"));
    rowAction(body,"Use AIROW as Home",isDefaultHome()?"Already your default launcher":"Choose the Home role","↗",this::requestHome);
    rowAction(body,"Choose assistant","Open any installed assistant from Ask","↗",()->showApps(true));
    rowAction(body,"Sports leagues","Choose the scores on your radar","↗",this::chooseSports);
    Switch sports=new Switch(this);sports.setText("Show sports on Home");sports.setTextColor(WHITE);sports.setMinHeight(dp(56));
    sports.setChecked(prefs.getBoolean("sports_wire",true));sports.setOnCheckedChangeListener((b,on)->{prefs.edit().putBoolean("sports_wire",on).apply();if(on)refreshSports(false);});body.addView(sports);
    rowAction(body,"Spotify controls","Manage notification access","↗",this::explainMusic);
    rowAction(body,"Trading Center","Open your configured dashboard","↗",this::openTrading);
    rowAction(body,"Trading address","Configure the Raspberry Pi dashboard","↗",this::configureTrading);
    rowAction(body,"Android settings","Device and default apps","↗",()->open(new Intent(Settings.ACTION_SETTINGS)));
    space(body,24);body.addView(text("Aperture is still at rest. Only your touch moves it.",16,WHITE));
    body.addView(text("No motion sensors. No continuous animation. Scores and model data use cached, throttled refreshes. Captures stay on this phone. Ask opens your assistant; AIROW does not run an AI model.",13,MUTED));
  }
  private void refreshNfl(boolean force) {
    long now = System.currentTimeMillis();
    if (fetching || now - lastAttempt < (force ? 30000 : 900000)) return;
    fetching = true; lastAttempt = now; prefs.edit().putLong("nfl_last_attempt", now).apply(); networkNote = "Checking…"; renderNfl();
    worker.execute(() -> {
      String statusData = null, boardData = null;
      NflSnapshot parsedSnapshot = null; NflBoard parsedBoard = null;
      try { statusData = downloadStatus(); parsedSnapshot = NflSnapshot.parse(statusData, System.currentTimeMillis()); } catch (Exception ignored) {}
      try { boardData = downloadLauncherBoard(); parsedBoard = NflBoard.parseFeed(boardData); }
      catch (Exception primary) {
        try { parsedBoard = NflBoard.parseHtml(downloadBoardHtml()); } catch (Exception ignored) {}
      }
      final String rawStatus=statusData; final NflSnapshot resultSnapshot=parsedSnapshot; final NflBoard resultBoard=parsedBoard;
      handler.post(() -> {
        if (isDestroyed()) return; fetching=false; boolean updated=false; SharedPreferences.Editor edit=prefs.edit();
        if (resultSnapshot != null) { snapshot=resultSnapshot; edit.putString("nfl_cache", rawStatus); updated=true; }
        if (resultBoard != null) { board=resultBoard; try { edit.putString("nfl_board_cache", resultBoard.cacheJson()); } catch(Exception ignored){} updated=true; }
        if (updated) { edit.apply(); networkNote="Checked "+java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(new Date()); }
        else networkNote="Could not refresh · saved data only";
        renderNfl();
      });
    });
  }

  private String downloadStatus() throws Exception {
    return downloadText(BOARD + "site-status.json", 65536, "application/json");
  }

  private String downloadLauncherBoard() throws Exception {
    return downloadText(BOARD + "launcher-board.json", 512 * 1024, "application/json");
  }

  private String downloadBoardHtml() throws Exception {
    return downloadText(BOARD, 2 * 1024 * 1024, "text/html,application/xhtml+xml");
  }

  private String downloadText(String url, int maxBytes, String accept) throws Exception {
    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
    c.setConnectTimeout(8000);
    c.setReadTimeout(8000);
    c.setInstanceFollowRedirects(true);
    c.setUseCaches(false);
    c.setRequestProperty("Accept", accept);
    c.setRequestProperty("Cache-Control", "no-cache");
    c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 16) AIROW-Home/4.0.0");
    try {
      if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode());
      try (InputStream in = c.getInputStream();
          ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        byte[] b = new byte[8192];
        int n;
        while ((n = in.read(b)) != -1) {
          if (out.size() + n > maxBytes) throw new IOException("Response too large");
          out.write(b, 0, n);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
      }
    } finally {
      c.disconnect();
    }
  }

  private boolean musicAllowed() {
    NotificationManager n = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    return n.isNotificationListenerAccessGranted(
        new ComponentName(this, SpotifyAccessService.class));
  }

  private void explainMusic() {
    new AlertDialog.Builder(this)
        .setTitle("Enable Spotify controls")
        .setMessage(
            "Android requires notification access to read Spotify's active media session. That"
                + " access is broad; this app uses only Spotify playback metadata and controls and"
                + " does not read, save or send your notification messages. You can leave it off"
                + " and open Spotify normally.")
        .setPositiveButton(
            "OPEN SETTINGS",
            (d, w) -> open(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)))
        .setNegativeButton("LATER", null)
        .show();
  }

  private void connectMusic() {
    disconnectMusic();
    if (!musicAllowed()) {
      renderMusic();
      return;
    }
    try {
      ComponentName cn = new ComponentName(this, SpotifyAccessService.class);
      sessions.addOnActiveSessionsChangedListener(sessionListener, cn, handler);
      listening = true;
      selectSpotify(sessions.getActiveSessions(cn));
    } catch (SecurityException e) {
      disconnectMusic();
    }
  }

  private void selectSpotify(List<MediaController> list) {
    detachSpotify();
    if (list != null)
      for (MediaController c : list)
        if ("com.spotify.music".equals(c.getPackageName())) {
          spotify = c;
          spotify.registerCallback(mediaCallback, handler);
          break;
        }
    renderMusic();
  }

  private void detachSpotify() {
    if (spotify != null) {
      spotify.unregisterCallback(mediaCallback);
      spotify = null;
    }
  }

  private void disconnectMusic() {
    if (listening) {
      sessions.removeOnActiveSessionsChangedListener(sessionListener);
      listening = false;
    }
    detachSpotify();
  }

  private void transport(String action) {
    if (spotify == null) {
      openSpotify();
      return;
    }
    try {
      MediaController.TransportControls t = spotify.getTransportControls();
      if (action.equals("prev")) t.skipToPrevious();
      else if (action.equals("next")) t.skipToNext();
      else {
        PlaybackState s = spotify.getPlaybackState();
        if (s != null && s.getState() == PlaybackState.STATE_PLAYING) t.pause();
        else t.play();
      }
    } catch (Exception e) {
      toast("Reopen Spotify to reconnect controls.");
    }
  }

  private void openSpotify() {
    Intent i = getPackageManager().getLaunchIntentForPackage("com.spotify.music");
    if (i != null) open(i);
    else open(new Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/")));
  }

  private void openAssistant() {
    String saved = prefs.getString("assistant", "");
    if (!saved.isEmpty()) {
      ComponentName c = ComponentName.unflattenFromString(saved);
      if (c != null) {
        launch(c);
        return;
      }
    }
    Intent chat = getPackageManager().getLaunchIntentForPackage("com.openai.chatgpt");
    if (chat != null) {
      open(chat);
      return;
    }
    showApps(true);
  }

  private void openTrading() {
    String value=prefs.getString("trading_url",TRADING_DEFAULT).trim();
    Uri uri=Uri.parse(value);
    if(!("http".equals(uri.getScheme())||"https".equals(uri.getScheme()))||uri.getHost()==null){
      toast("Set a valid Trading Center address in Settings.");configureTrading();return;
    }
    open(new Intent(Intent.ACTION_VIEW,uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  }

  private void configureTrading() {
    final EditText address=new EditText(this);address.setSingleLine(true);address.setText(prefs.getString("trading_url",TRADING_DEFAULT));address.setSelectAllOnFocus(true);address.setHint("http://raspberrypi.local:8765/");
    new AlertDialog.Builder(this).setTitle("AIROW Trading Center")
        .setMessage("Enter the dashboard address reachable from this phone. The default targets airow-trader on your Raspberry Pi.")
        .setView(address).setPositiveButton("SAVE",(dialog,which)->{
          String value=address.getText().toString().trim();Uri uri=Uri.parse(value);
          if(("http".equals(uri.getScheme())||"https".equals(uri.getScheme()))&&uri.getHost()!=null){prefs.edit().putString("trading_url",value).apply();toast("Trading Center saved");}
          else toast("Address must begin with http:// or https://");
        }).setNeutralButton("RESET",(dialog,which)->{prefs.edit().remove("trading_url").apply();toast("Trading Center reset to Raspberry Pi");}).setNegativeButton("CANCEL",null).show();
  }

  private void chooseSports() {
    String[] leagues={"NFL","MLB","NHL","NBA"};
    Set<String> saved=new LinkedHashSet<>(prefs.getStringSet("sports_leagues",new LinkedHashSet<>(Arrays.asList(leagues))));
    boolean[] checked=new boolean[leagues.length];for(int i=0;i<leagues.length;i++)checked[i]=saved.contains(leagues[i]);
    new AlertDialog.Builder(this).setTitle("Sports intelligence")
        .setMultiChoiceItems(leagues,checked,(dialog,which,on)->checked[which]=on)
        .setPositiveButton("SAVE",(dialog,which)->{LinkedHashSet<String> chosen=new LinkedHashSet<>();for(int i=0;i<leagues.length;i++)if(checked[i])chosen.add(leagues[i]);if(chosen.isEmpty()){toast("Choose at least one league");return;}prefs.edit().putStringSet("sports_leagues",chosen).remove("sports_wire_cache").apply();refreshSports(true);})
        .setNegativeButton("CANCEL",null).show();
  }

  private void refreshSports(boolean force) {
    if (!prefs.getBoolean("sports_wire", true)) { renderSportsWire(); return; }
    long now=System.currentTimeMillis(); String cached=prefs.getString("sports_wire_cache",""); boolean hasCache=!cached.isEmpty();
    long interval=!hasCache?90000:(cached.contains("LIVE")?300000:1800000);
    if(sportsFetching||now-lastSportsAttempt<(force?30000:interval))return;
    sportsFetching=true;lastSportsAttempt=now;prefs.edit().putLong("sports_last_attempt",now).apply();sportsError="";renderSportsWire();
    worker.execute(() -> {
      Set<String> selected=new LinkedHashSet<>(prefs.getStringSet("sports_leagues",new LinkedHashSet<>(Arrays.asList("NFL","MLB","NHL","NBA"))));
      SportsWire.Result result=null;String error=null;try{result=SportsWire.fetch(selected);}catch(Exception e){error=e.getMessage();}
      final SportsWire.Result wire=result;final String failure=error;
      handler.post(() -> {
        if(isDestroyed())return;sportsFetching=false;
        if(wire!=null&&wire.text!=null&&!wire.text.trim().isEmpty()){
          SharedPreferences.Editor edit=prefs.edit().putString("sports_wire_cache",wire.text).putLong("sports_wire_time",System.currentTimeMillis());
          if(wire.nflSeason>0)edit.putInt("sports_nfl_season",wire.nflSeason);if(wire.nflWeek>0)edit.putInt("sports_nfl_week",wire.nflWeek);edit.apply();sportsError="";
        }else{sportsError=failure==null?"Score source unavailable":failure;if(force)toast("Sports feed could not refresh. Tap ↻ to retry.");}
        renderSportsWire();renderNfl();
      });
    });
  }


  private void openMessages(){
    String pkg=Telephony.Sms.getDefaultSmsPackage(this);
    Intent intent=pkg==null?null:getPackageManager().getLaunchIntentForPackage(pkg);
    if(intent==null)intent=Intent.makeMainSelectorActivity(Intent.ACTION_MAIN,Intent.CATEGORY_APP_MESSAGING);
    open(intent);
  }
  private boolean isDefaultHome(){
    RoleManager role=(RoleManager)getSystemService(ROLE_SERVICE);
    return role!=null&&role.isRoleAvailable(RoleManager.ROLE_HOME)&&role.isRoleHeld(RoleManager.ROLE_HOME);
  }
  private void requestHome(){
    RoleManager role=(RoleManager)getSystemService(ROLE_SERVICE);
    if(role!=null&&role.isRoleAvailable(RoleManager.ROLE_HOME)&&!role.isRoleHeld(RoleManager.ROLE_HOME))startActivityForResult(role.createRequestRoleIntent(RoleManager.ROLE_HOME),1);
    else open(new Intent(Settings.ACTION_HOME_SETTINGS));
  }
  private void launch(ComponentName component){
    open(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(component)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
  }
  private void open(Intent intent){
    try{startActivity(intent);}catch(ActivityNotFoundException|SecurityException e){toast("This app is unavailable. Choose another in settings.");}
  }
  private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_SHORT).show();}
  private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
  private LinearLayout column(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
  private TextView text(String value,int size,int color){
    TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);
    v.setIncludeFontPadding(false);v.setPadding(0,dp(6),0,dp(6));return v;
  }
  private TextView action(String title,int size,int color,View.OnClickListener click){
    TextView v=text(title,size,color);v.setGravity(Gravity.CENTER_VERTICAL);v.setMinHeight(dp(48));
    v.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33ff644c),null,null));
    v.setFocusable(true);v.setOnClickListener(click);return v;
  }
  private EditText input(String hint){
    EditText v=new EditText(this);v.setHint(hint);v.setTextColor(WHITE);v.setHintTextColor(MUTED);
    v.setTextSize(17);v.setPadding(dp(16),dp(12),dp(16),dp(12));v.setBackgroundColor(SURFACE);return v;
  }
  private void rowAction(LinearLayout parent,String title,String subtitle,String arrow,Runnable action){
    LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(12),0,dp(12));
    LinearLayout labels=column();TextView heading=text(title,21,WHITE);heading.setMaxLines(3);heading.setEllipsize(TextUtils.TruncateAt.END);
    labels.addView(heading);labels.addView(text(subtitle,12,MUTED));row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
    TextView more=text(arrow,22,ACCENT);more.setGravity(Gravity.CENTER);row.addView(more,new LinearLayout.LayoutParams(dp(40),dp(48)));
    row.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33ff644c),null,null));row.setFocusable(true);
    row.setContentDescription(title+". "+subtitle);row.setOnClickListener(v->action.run());
    parent.addView(row,new LinearLayout.LayoutParams(-1,-2));divider(parent);
  }
  private GradientDrawable bg(int color,int radius){
    GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;
  }
  private void divider(LinearLayout parent){View line=new View(this);line.setBackgroundColor(0xff292a2d);parent.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));}
  private void space(LinearLayout parent,int height){parent.addView(new View(this),new LinearLayout.LayoutParams(1,dp(height)));}
}
