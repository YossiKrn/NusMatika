package id.nusmatika.dakon;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ScrollView;

public final class MainActivity extends Activity implements DakonView.Host {
    private DakonSession session;
    private SharedPreferences preferences;
    private DakonView game;
    private final EditText[] answers=new EditText[10];
    private WorksheetState worksheet;
    private int lastStage;
    private NaraRenderer nara;
    private ToneGenerator tones;
    private boolean updatingText;
    private boolean soundEnabled;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        preferences=getSharedPreferences("nusmatika-dakon",MODE_PRIVATE);
        session=DakonSession.restore(preferences.getString("session",""));
        worksheet=WorksheetState.restore(preferences.getString("worksheet-kpk",""));
        lastStage=session.getStage();
        if(lastStage==DakonSession.NAMBAHI)session.setAnswer("");
        soundEnabled=preferences.getBoolean("sound",false);
        Typeface font;
        try { font=Typeface.createFromAsset(getAssets(),"fonts/Nunito.ttf"); }
        catch(RuntimeException error) { font=Typeface.create("sans-serif-rounded",Typeface.NORMAL); }
        nara=new NaraRenderer(this);
        game=new DakonView(this,session,nara,font,this);
        for(int index=0;index<answers.length;index++){
        final int field=index;
        EditText answer=new EditText(this);
        answers[index]=answer;
        answer.setId(1000+index);
        answer.setSingleLine(true);
        answer.setInputType(InputType.TYPE_CLASS_NUMBER);
        answer.setImeOptions((index==9?EditorInfo.IME_ACTION_DONE:EditorInfo.IME_ACTION_NEXT)|EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        answer.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        answer.setTypeface(font);
        answer.setFontVariationSettings("'wght' 650");
        answer.setTextColor(Color.rgb(60,60,60));
        answer.setHintTextColor(Color.rgb(119,119,119));
        answer.setHint("—");
        answer.setContentDescription(index<4?"Kelipatan 3, isian "+(index+1):index<8?"Kelipatan 4, isian "+(index-3):index==8?"Kelipatan yang sama":"KPK 3 dan 4");
        answer.setBackgroundColor(Color.TRANSPARENT);
        answer.setSelectAllOnFocus(false);
        answer.setGravity(Gravity.CENTER);
        if(index<9)answer.setNextFocusForwardId(1001+index);
        answer.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        answer.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){
                if(!updatingText){worksheet.set(field,s.toString());session.setAnswer("");save();game.invalidate();}
            }
            @Override public void afterTextChanged(Editable e){}
        });
        answer.setOnEditorActionListener((v,action,event)->{
            if(action==EditorInfo.IME_ACTION_DONE){hideKeyboard();return true;}
            if(action==EditorInfo.IME_ACTION_NEXT&&field<9){answers[field+1].requestFocus();return true;}
            return false;
        });
        answer.setOnFocusChangeListener((v,focused)->game.invalidate());
        }
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        scroll.setBackgroundColor(Color.WHITE);
        scroll.addView(new GameLayout(this),new ScrollView.LayoutParams(-1,-2));
        FrameLayout safeArea=new FrameLayout(this);
        safeArea.setBackgroundColor(Color.WHITE);
        safeArea.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                |View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                |View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        safeArea.setOnApplyWindowInsetsListener((v,insets)->{
            int left,top,right,bottom;
            if(Build.VERSION.SDK_INT>=30){
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
                android.graphics.Insets keyboard=insets.getInsets(WindowInsets.Type.ime());
                left=bars.left;top=bars.top;right=bars.right;bottom=Math.max(bars.bottom,keyboard.bottom);
            }else{
                left=insets.getSystemWindowInsetLeft();top=insets.getSystemWindowInsetTop();
                right=insets.getSystemWindowInsetRight();bottom=insets.getSystemWindowInsetBottom();
            }
            v.setPadding(left,top,right,bottom);
            return insets;
        });
        setContentView(safeArea);
        safeArea.requestApplyInsets();
        refresh();
    }

    private final class GameLayout extends FrameLayout {
        GameLayout(Context context){super(context);addView(game);for(EditText answer:answers)addView(answer);setClipChildren(false);}
        @Override protected void onMeasure(int widthSpec,int heightSpec){
            int width=MeasureSpec.getSize(widthSpec);
            float scale=width/360f;
            int height=Math.max(Math.round(740*scale),MeasureSpec.getSize(heightSpec));
            setMeasuredDimension(width,height);
            game.measure(MeasureSpec.makeMeasureSpec(width,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(height,MeasureSpec.EXACTLY));
            for(int i=0;i<answers.length;i++){
                EditText answer=answers[i];
                answer.measure(MeasureSpec.makeMeasureSpec(Math.round(WorksheetLayout.width(i)*scale),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.round(WorksheetLayout.height(i)*scale),MeasureSpec.EXACTLY));
                answer.setTextSize(TypedValue.COMPLEX_UNIT_PX,(i==9?22:20)*scale);
                answer.setPadding(Math.round(4*scale),0,Math.round(4*scale),0);
            }
        }
        @Override protected void onLayout(boolean changed,int l,int t,int r,int b){
            game.layout(0,0,r-l,b-t);
            float scale=(r-l)/360f;
            for(int i=0;i<answers.length;i++)answers[i].layout(Math.round(WorksheetLayout.x(i)*scale),Math.round(WorksheetLayout.y(i)*scale),Math.round((WorksheetLayout.x(i)+WorksheetLayout.width(i))*scale),Math.round((WorksheetLayout.y(i)+WorksheetLayout.height(i))*scale));
        }
    }

    private void refresh(){
        for(int i=0;i<answers.length;i++){
            EditText answer=answers[i];
            answer.setVisibility(session.getStage()==DakonSession.NAMBAHI?View.VISIBLE:View.GONE);
            if(!answer.getText().toString().equals(worksheet.get(i))){
                updatingText=true;answer.setText(worksheet.get(i));answer.setSelection(answer.length());updatingText=false;
            }
        }
        game.invalidate();
    }
    private void save(){
        if(preferences!=null&&session!=null)preferences.edit().putString("session",session.encode()).putString("worksheet-kpk",worksheet.encode()).putBoolean("sound",soundEnabled).apply();
    }
    private void hideKeyboard(){
        if(answers[0]==null)return;
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(answers[0].getWindowToken(),0);
        for(EditText answer:answers)answer.clearFocus();
        game.requestFocus();
    }
    @Override public void onSessionChanged(){
        if(session.getStage()!=lastStage){
            if(session.getStage()!=DakonSession.COMPLETE)worksheet.reset();
            lastStage=session.getStage();
        }
        hideKeyboard();refresh();save();
    }
    @Override public void onExit(){hideKeyboard();save();finish();}
    @Override public void onRequestAnswer(){
        if(session.getStage()!=DakonSession.NAMBAHI)return;
        int field=9;for(int i=0;i<answers.length;i++)if(worksheet.get(i).isEmpty()){field=i;break;}
        answers[field].requestFocus();
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(answers[field],InputMethodManager.SHOW_IMPLICIT);
    }
    @Override public WorksheetState worksheet(){return worksheet;}
    @Override public int focusedWorksheetField(){for(int i=0;i<answers.length;i++)if(answers[i].hasFocus())return i;return -1;}
    @Override public void onCheckWorksheet(){
        boolean correct=worksheet.check();
        session.setAnswer(correct?worksheet.get(9):"");
        if(correct)session.submitAnswer();
        hideKeyboard();save();game.invalidate();playTap(correct);
        game.announceForAccessibility(correct?"Benar. KPK-nya 12.":"Cek isian yang ditandai.");
    }
    @Override public boolean isSoundEnabled(){return soundEnabled;}
    @Override public void onSoundToggle(){soundEnabled=!soundEnabled;save();playTap(false);game.invalidate();}
    @Override public void playTap(boolean correct){
        if(!soundEnabled)return;
        try{
            if(tones==null)tones=new ToneGenerator(AudioManager.STREAM_MUSIC,25);
            tones.startTone(correct?ToneGenerator.TONE_PROP_ACK:ToneGenerator.TONE_PROP_BEEP,correct?100:35);
        }catch(RuntimeException ignored){}
    }
    @Override public boolean isReducedMotion(){
        try{return Settings.Global.getFloat(getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1f)==0f;}
        catch(RuntimeException ignored){return false;}
    }
    @Override protected void onResume(){super.onResume();if(game!=null)game.setRunning(true);}
    @Override protected void onPause(){save();if(game!=null)game.setRunning(false);super.onPause();}
    @Override protected void onDestroy(){if(nara!=null)nara.release();if(tones!=null)tones.release();super.onDestroy();}
}
