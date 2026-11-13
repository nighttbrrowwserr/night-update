package com.night.browser;
import android.provider.Settings;import android.util.Base64;import android.security.keystore.*;import org.json.*;import java.io.*;import java.net.*;import java.security.KeyStore;import javax.crypto.*;import javax.crypto.spec.GCMParameterSpec;import android.app.*;import android.content.*;import android.graphics.*;import android.net.Uri;import android.os.*;import android.view.*;import android.view.inputmethod.EditorInfo;import android.webkit.*;import android.widget.*;import java.util.*;
public class MainActivity extends Activity {
  static final String AS="file:///android_asset/", HOME=AS+"home.html";
  FrameLayout host; EditText bar; Button tabBtn; ProgressBar pb; ValueCallback<Uri[]> fc;
  ArrayList<WebView> tabs=new ArrayList<>(); ArrayList<Boolean> priv=new ArrayList<>(); int cur=-1;
  Button btn(String t,View.OnClickListener c){Button b=new Button(this);b.setText(t);b.setTextColor(Color.WHITE);b.setTextSize(12);b.setAllCaps(false);b.setBackgroundColor(Color.TRANSPARENT);b.setOnClickListener(c);b.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));return b;}
  @Override protected void onCreate(Bundle s){
    super.onCreate(s);getWindow().setStatusBarColor(Color.BLACK);getWindow().setNavigationBarColor(Color.BLACK);
    LinearLayout r=new LinearLayout(this);r.setOrientation(1);r.setBackgroundColor(Color.BLACK);r.setFitsSystemWindows(true);
    bar=new EditText(this);bar.setSingleLine();bar.setHint("Rechercher ou saisir une adresse");bar.setHintTextColor(0xff777777);bar.setTextColor(Color.WHITE);bar.setBackgroundColor(0xff141414);bar.setPadding(40,28,40,28);bar.setImeOptions(EditorInfo.IME_ACTION_GO);bar.setInputType(17);
    bar.setOnEditorActionListener((v,a,e)->{go(bar.getText().toString());return true;});
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(20,10,20,10);r.addView(bar,lp);
    pb=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);pb.setMax(100);r.addView(pb,new LinearLayout.LayoutParams(-1,6));
    host=new FrameLayout(this);r.addView(host,new LinearLayout.LayoutParams(-1,0,1));
    LinearLayout nav=new LinearLayout(this);nav.setBackgroundColor(0xff0c0c0c);
    nav.addView(btn("Retour",v->{if(w()!=null&&w().canGoBack())w().goBack();}));
    nav.addView(btn("Suivant",v->{if(w()!=null&&w().canGoForward())w().goForward();}));
    nav.addView(btn("Accueil",v->go(HOME)));
    tabBtn=btn("Onglets",v->tabsDlg());nav.addView(tabBtn);
    nav.addView(btn("Menu",v->menu()));r.addView(nav,new LinearLayout.LayoutParams(-1,-2));
    sp=getSharedPreferences("night",0);secure(sp.getBoolean("proof",false));checkUpdate();
    setContentView(r);
    Uri d=getIntent().getData();newTab(false,d!=null?d.toString():HOME);
  }
  WebView w(){return cur>=0?tabs.get(cur):null;}
  void go(String q){q=q.trim();if(q.isEmpty())return;String u;
    if(q.startsWith("http://")||q.startsWith("https://")||q.startsWith("file:"))u=q;
    else if(q.contains(".")&&!q.contains(" "))u="https://"+q;
    else u="https://duckduckgo.com/?q="+Uri.encode(q);
    w().loadUrl(u);}
  void show(int i){cur=i;host.removeAllViews();host.addView(tabs.get(i));tabBtn.setText("Onglets ("+tabs.size()+")");upd(tabs.get(i).getUrl());}
  void upd(String u){bar.setText(u==null||u.startsWith(AS)?"":u);}
  void newTab(boolean p,String url){
    WebView v=new WebView(this);v.setBackgroundColor(Color.BLACK);WebSettings st=v.getSettings();
    st.setJavaScriptEnabled(true);st.setDomStorageEnabled(true);st.setMediaPlaybackRequiresUserGesture(false);st.setBuiltInZoomControls(true);st.setDisplayZoomControls(false);st.setLoadWithOverviewMode(true);st.setUseWideViewPort(true);st.setAllowFileAccess(true);
    if(p){st.setCacheMode(WebSettings.LOAD_NO_CACHE);st.setSaveFormData(false);}
    v.setWebViewClient(new WebViewClient(){
      @Override public void onPageStarted(WebView x,String u,Bitmap b){if(x==w())upd(u);}
      @Override public boolean shouldOverrideUrlLoading(WebView x,WebResourceRequest q){String u=q.getUrl().toString();if(u.startsWith("http")||u.startsWith("file:"))return false;try{startActivity(new Intent(Intent.ACTION_VIEW,q.getUrl()));}catch(Exception e){}return true;}});
    v.setWebChromeClient(new WebChromeClient(){
      @Override public void onProgressChanged(WebView x,int n){if(x==w()){pb.setProgress(n);pb.setVisibility(n<100?View.VISIBLE:View.GONE);}}
      @Override public boolean onJsConfirm(WebView x,String u,String m,JsResult r){new AlertDialog.Builder(MainActivity.this,AlertDialog.THEME_DEVICE_DEFAULT_DARK).setMessage(m).setPositiveButton("OK",(d,i)->r.confirm()).setNegativeButton("Annuler",(d,i)->r.cancel()).setOnCancelListener(d->r.cancel()).show();return true;}
      @Override public boolean onJsAlert(WebView x,String u,String m,JsResult r){new AlertDialog.Builder(MainActivity.this,AlertDialog.THEME_DEVICE_DEFAULT_DARK).setMessage(m).setPositiveButton("OK",(d,i)->r.confirm()).setOnCancelListener(d->r.cancel()).show();return true;}
      @Override public boolean onJsPrompt(WebView x,String u,String m,String df,JsPromptResult r){EditText e=new EditText(MainActivity.this);e.setText(df);new AlertDialog.Builder(MainActivity.this,AlertDialog.THEME_DEVICE_DEFAULT_DARK).setMessage(m).setView(e).setPositiveButton("OK",(d,i)->r.confirm(e.getText().toString())).setNegativeButton("Annuler",(d,i)->r.cancel()).setOnCancelListener(d->r.cancel()).show();return true;}
      @Override public boolean onShowFileChooser(WebView x,ValueCallback<Uri[]> cb,FileChooserParams fp){if(fc!=null)fc.onReceiveValue(null);fc=cb;try{startActivityForResult(fp.createIntent(),1);}catch(Exception e){fc=null;return false;}return true;}});
    v.setDownloadListener((u,ua,cd,mt,len)->{DownloadManager.Request q=new DownloadManager.Request(Uri.parse(u));q.setNotificationVisibility(1);q.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,URLUtil.guessFileName(u,cd,mt));((DownloadManager)getSystemService(DOWNLOAD_SERVICE)).enqueue(q);Toast.makeText(this,"Téléchargement lancé",0).show();});
    tabs.add(v);priv.add(p);show(tabs.size()-1);v.loadUrl(url);
  }
  void closeTab(){if(tabs.size()==1){finish();return;}WebView v=tabs.remove(cur);boolean p=priv.remove(cur);v.destroy();if(p){CookieManager.getInstance().removeAllCookies(null);WebStorage.getInstance().deleteAllData();}show(Math.max(0,cur-1));}
  void tabsDlg(){ArrayList<String> l=new ArrayList<>();for(int i=0;i<tabs.size();i++){String t=tabs.get(i).getTitle();l.add((i==cur?"> ":"")+(priv.get(i)?"[Privé] ":"")+(t==null||t.isEmpty()?"Nouvel onglet":t));}
    l.add("+ Nouvel onglet");l.add("+ Onglet privé");
    new AlertDialog.Builder(this,AlertDialog.THEME_DEVICE_DEFAULT_DARK).setTitle("Onglets").setItems(l.toArray(new String[0]),(d,i)->{if(i==tabs.size())newTab(false,HOME);else if(i==tabs.size()+1)newTab(true,HOME);else show(i);}).setNeutralButton("Fermer cet onglet",(d,i)->closeTab()).show();}
  static final int DK=AlertDialog.THEME_DEVICE_DEFAULT_DARK; SharedPreferences sp;
  void toast(String t){Toast.makeText(this,t,0).show();}
  void secure(boolean on){if(on)getWindow().setFlags(8192,8192);else getWindow().clearFlags(8192);}
  void menu(){boolean pf=sp.getBoolean("proof",false);String[] m={"Night Mail","Night Music","Night code","Stream proof : "+(pf?"activé":"désactivé"),"Recharger","Partager la page","Fermer l'onglet"};
    new AlertDialog.Builder(this,DK).setItems(m,(d,i)->{
      if(i==0)go(AS+"mail.html");else if(i==1)go(AS+"music.html");else if(i==2)ncDlg();
      else if(i==3){sp.edit().putBoolean("proof",!pf).apply();secure(!pf);toast(pf?"Stream proof désactivé":"Stream proof activé : invisible en capture et partage d'écran");}
      else if(i==4)w().reload();
      else if(i==5){Intent x=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,String.valueOf(w().getUrl()));startActivity(Intent.createChooser(x,"Partager"));}
      else closeTab();}).show();}
  // ---------- Night code : mots de passe chiffrés (AES-GCM, clé dans l'Android Keystore) ----------
  SecretKey key()throws Exception{KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);if(!ks.containsAlias("nc")){KeyGenerator g=KeyGenerator.getInstance("AES","AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder("nc",3).setBlockModes("GCM").setEncryptionPaddings("NoPadding").build());return g.generateKey();}return(SecretKey)ks.getKey("nc",null);}
  String enc(String t)throws Exception{Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(1,key());return Base64.encodeToString(c.getIV(),2)+":"+Base64.encodeToString(c.doFinal(t.getBytes("UTF-8")),2);}
  String dec(String s)throws Exception{String[] a=s.split(":");Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(2,key(),new GCMParameterSpec(128,Base64.decode(a[0],2)));return new String(c.doFinal(Base64.decode(a[1],2)),"UTF-8");}
  static final String READ="(function(){var p=[].find.call(document.querySelectorAll('input[type=password]'),function(i){return i.value});if(!p)return '';var u=[].filter.call(document.querySelectorAll('input'),function(i){return /^(text|email|tel|)$/.test(i.type)&&i.value&&(p.compareDocumentPosition(i)&2)}).pop();return JSON.stringify({u:u?u.value:'',p:p.value})})()";
  static final String FILL="(function(u,p){var s=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;function f(e,v){s.call(e,v);e.dispatchEvent(new Event('input',{bubbles:true}));e.dispatchEvent(new Event('change',{bubbles:true}))}var w=[].find.call(document.querySelectorAll('input[type=password]'),function(i){return i.offsetParent});if(!w)return;var t=[].filter.call(document.querySelectorAll('input'),function(i){return /^(text|email|tel|)$/.test(i.type)&&i.offsetParent&&(w.compareDocumentPosition(i)&2)}).pop();if(t&&u)f(t,u);f(w,p)})";
  void ncDlg(){try{JSONObject o=new JSONObject(sp.getString("nc","{}"));ArrayList<String> k=new ArrayList<>();for(Iterator<String> it=o.keys();it.hasNext();)k.add(it.next());Collections.sort(k);
    ArrayList<String> l=new ArrayList<>();for(String x:k)l.add(x.replace("|"," — "));l.add("+ Enregistrer depuis cette page");
    new AlertDialog.Builder(this,DK).setTitle("Night code").setItems(l.toArray(new String[0]),(d,i)->{if(i==k.size())ncSave();else ncUse(o,k.get(i));}).show();}catch(Exception e){toast("Erreur Night code");}}
  void ncUse(JSONObject o,String key){new AlertDialog.Builder(this,DK).setTitle(key.replace("|"," — ")).setItems(new String[]{"Remplir la page","Copier le mot de passe","Supprimer"},(d,i)->{try{JSONObject c=new JSONObject(dec(o.getString(key)));
    if(i==0)w().evaluateJavascript(FILL+"("+JSONObject.quote(c.getString("u"))+","+JSONObject.quote(c.getString("p"))+")",null);
    else if(i==1){((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("mdp",c.getString("p")));toast("Mot de passe copié");}
    else{o.remove(key);sp.edit().putString("nc",o.toString()).apply();toast("Supprimé");}}catch(Exception e){toast("Erreur Night code");}}).show();}
  void ncSave(){if(priv.get(cur)){toast("Pas d'enregistrement en navigation privée");return;}
    w().evaluateJavascript(READ,r->{try{String s=String.valueOf(new JSONTokener(r).nextValue());if(s.isEmpty()||s.equals("null")){toast("Saisis d'abord ton mot de passe dans la page");return;}
      JSONObject c=new JSONObject(s);String host=Uri.parse(w().getUrl()).getHost(),u=c.getString("u");
      new AlertDialog.Builder(this,DK).setTitle("Night code").setMessage("Enregistrer le mot de passe de "+host+(u.isEmpty()?"":" ("+u+")")+" ?").setPositiveButton("Enregistrer",(d,i)->{try{JSONObject o=new JSONObject(sp.getString("nc","{}"));o.put(host+"|"+u,enc(c.toString()));sp.edit().putString("nc",o.toString()).apply();toast("Enregistré");}catch(Exception e){toast("Erreur Night code");}}).setNegativeButton("Non",null).show();}catch(Exception e){toast("Erreur Night code");}});}
  // ---------- Mises à jour automatiques (au démarrage puis toutes les 6 h) ----------
  void checkUpdate(){if(BuildConfig.FEED.isEmpty())return;new Handler(Looper.getMainLooper()).postDelayed(this::checkUpdate,21600000L);
    new Thread(()->{try{HttpURLConnection c=(HttpURLConnection)new URL(BuildConfig.FEED).openConnection();c.setConnectTimeout(8000);c.setReadTimeout(8000);BufferedReader br=new BufferedReader(new InputStreamReader(c.getInputStream()));StringBuilder sb=new StringBuilder();String ln;while((ln=br.readLine())!=null)sb.append(ln);
      JSONObject j=new JSONObject(sb.toString());if(j.getInt("version")>BuildConfig.VERSION_CODE){String apk=j.getString("apk"),nt=j.optString("notes");runOnUiThread(()->new AlertDialog.Builder(this,DK).setTitle("Mise à jour de night").setMessage(nt).setPositiveButton("Mettre à jour",(d,i)->dl(apk)).setNegativeButton("Plus tard",null).show());}}catch(Exception e){}}).start();}
  void dl(String u){if(Build.VERSION.SDK_INT>=26&&!getPackageManager().canRequestPackageInstalls()){toast("Autorise night à installer des applications, puis relance la mise à jour");startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName())));return;}
    DownloadManager m=(DownloadManager)getSystemService(DOWNLOAD_SERVICE);DownloadManager.Request q=new DownloadManager.Request(Uri.parse(u));q.setTitle("night");q.setNotificationVisibility(1);q.setDestinationInExternalFilesDir(this,null,"night-update.apk");final long id=m.enqueue(q);toast("Téléchargement de la mise à jour...");
    BroadcastReceiver r=new BroadcastReceiver(){public void onReceive(Context c,Intent i){if(i.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID,-1)!=id)return;unregisterReceiver(this);Uri a=m.getUriForDownloadedFile(id);if(a!=null)startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(a,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK));}};
    IntentFilter f=new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);if(Build.VERSION.SDK_INT>=33)registerReceiver(r,f,Context.RECEIVER_EXPORTED);else registerReceiver(r,f);}
  @Override protected void onActivityResult(int rq,int rc,Intent i){if(rq==1&&fc!=null){fc.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(rc,i));fc=null;}}
  @Override public void onBackPressed(){if(w().canGoBack())w().goBack();else if(tabs.size()>1)closeTab();else super.onBackPressed();}
}
