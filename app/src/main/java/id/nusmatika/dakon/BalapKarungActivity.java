package id.nusmatika.dakon;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

/** Separate module and preference file; no Dakon state is read or written here. */
public final class BalapKarungActivity extends Activity implements BalapKarungView.Host {
    private BalapLearningStore store;
    private BalapKarungSession session;
    private BalapKarungView game;
    private EditText answer,working;
    private NaraRenderer nara;
    private ToneGenerator tones;
    private Typeface font;
    private boolean updating,sound;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        store=new BalapLearningStore(this);
        int level=getIntent().getIntExtra("level",-1);
        if(!store.isUnlocked(level)){openMap();return;}
        session=store.loadSession(level);
        sound=store.isSoundEnabled();
        try{font=new Typeface.Builder(getAssets(),"fonts/Nunito.ttf").setFontVariationSettings("'wght' 650").build();}
        catch(RuntimeException error){font=Typeface.create("sans-serif-rounded",Typeface.NORMAL);}
        nara=new NaraRenderer(this);
        game=new BalapKarungView(this,session,nara,font,this);
        game.setId(R.id.balap_canvas);
        answer=new EditText(this);working=new EditText(this);
        answer.setSingleLine(true);answer.setInputType(InputType.TYPE_CLASS_NUMBER);
        answer.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        answer.setImeOptions(EditorInfo.IME_ACTION_DONE|EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        answer.setHint("Jawaban");answer.setGravity(Gravity.CENTER_VERTICAL);
        working.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        working.setFilters(new InputFilter[]{new InputFilter.LengthFilter(300)});
        working.setGravity(Gravity.TOP|Gravity.START);
        working.setContentDescription("Caramu menghitung. Boleh dikosongkan.");
        working.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        for(EditText field:new EditText[]{answer,working}){
            field.setTypeface(font);field.setTextColor(0xFF3C3C3C);field.setHintTextColor(0xFF777777);
            field.setBackgroundColor(Color.TRANSPARENT);field.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        }
        answer.addTextChangedListener(watcher(false));working.addTextChangedListener(watcher(true));
        answer.setOnEditorActionListener((v,action,event)->{if(action==EditorInfo.IME_ACTION_DONE){hideKeyboard();game.focusAction();return true;}return false;});
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);scroll.setBackgroundColor(Color.WHITE);
        scroll.addView(new GameLayout(this),new ScrollView.LayoutParams(-1,-2));
        FrameLayout safe=new FrameLayout(this);safe.setBackgroundColor(Color.WHITE);safe.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        safe.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30){
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
                android.graphics.Insets ime=insets.getInsets(WindowInsets.Type.ime());v.setPadding(bars.left,bars.top,bars.right,Math.max(bars.bottom,ime.bottom));
            }else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(safe);safe.requestApplyInsets();refresh();game.requestFocus();
    }
    private TextWatcher watcher(final boolean isWorking){
        return new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){if(!updating){if(isWorking)session.setWorking(s.toString());else session.setAnswer(s.toString());save();game.onEdited();}}
            @Override public void afterTextChanged(Editable text){}
        };
    }
    private final class GameLayout extends FrameLayout{
        GameLayout(Context context){super(context);addView(game);addView(working);addView(answer);}
        @Override protected void onMeasure(int widthSpec,int heightSpec){
            int w=MeasureSpec.getSize(widthSpec);float s=w/360f;int h=Math.max(Math.round(740*s),MeasureSpec.getSize(heightSpec));
            setMeasuredDimension(w,h);game.measure(MeasureSpec.makeMeasureSpec(w,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(h,MeasureSpec.EXACTLY));
            answer.measure(MeasureSpec.makeMeasureSpec(Math.round(236*s),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.round(54*s),MeasureSpec.EXACTLY));
            working.measure(MeasureSpec.makeMeasureSpec(Math.round(328*s),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.round(182*s),MeasureSpec.EXACTLY));
            answer.setTextSize(TypedValue.COMPLEX_UNIT_PX,20*s);answer.setPadding(Math.round(16*s),0,Math.round(4*s),0);
            working.setTextSize(TypedValue.COMPLEX_UNIT_PX,18*s);int pad=Math.round(14*s);working.setPadding(pad,pad,pad,pad);
        }
        @Override protected void onLayout(boolean changed,int l,int t,int r,int b){
            game.layout(0,0,r-l,b-t);float s=(r-l)/360f;
            answer.layout(Math.round(16*s),Math.round(605*s),Math.round(252*s),Math.round(659*s));
            working.layout(Math.round(16*s),Math.round(352*s),Math.round(344*s),Math.round(534*s));
        }
    }
    private void refresh(){
        boolean exercise=session.getStage()==BalapKarungSession.NIROKKE||session.getStage()==BalapKarungSession.NAMBAHI;
        answer.setVisibility(exercise?View.VISIBLE:View.GONE);working.setVisibility(session.getStage()==BalapKarungSession.NAMBAHI?View.VISIBLE:View.GONE);
        answer.setContentDescription("Jawaban "+(session.getQuestion().unknown==BalapKarungSession.Quantity.DISTANCE?"jarak":session.getQuestion().unknown==BalapKarungSession.Quantity.SPEED?"kecepatan":"waktu")+" dalam "+session.getQuestion().unit());
        updating=true;
        if(!answer.getText().toString().equals(session.getAnswer())){answer.setText(session.getAnswer());answer.setSelection(answer.length());}
        if(!working.getText().toString().equals(session.getWorking())){working.setText(session.getWorking());working.setSelection(working.length());}
        updating=false;
        game.setContentDescription(session.isLevelComplete()?"Level "+(session.getLevel()+1)+" selesai. Kembali ke jalur."
                :session.isTriangleTutorial()?"Niteni. "+session.triangleTitle()+". "+(session.isTriangleLineIntro()?"":session.getQuestion().story+" ")+session.triangleInstruction()+" "+session.triangleCalculation()
                :session.getStage()==BalapKarungSession.NAMBAHI?session.getQuestion().story:session.getStage()==BalapKarungSession.NIROKKE?session.getQuestion().formula()+". "+session.getQuestion().calculation():"Balap Karung");
        game.invalidate();
    }
    private void save(){if(store!=null&&session!=null)store.saveSession(session);}
    private void openMap(){startActivity(new Intent(this,BalapKarungMapActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));finish();}
    private void hideKeyboard(){
        if(answer==null)return;((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(answer.getWindowToken(),0);
        answer.clearFocus();working.clearFocus();game.requestFocus();
    }
    @Override public void onSessionChanged(){hideKeyboard();refresh();save();}
    @Override public void onRequestInput(boolean isWorking){
        EditText target=isWorking?working:answer;
        if(target.getVisibility()!=View.VISIBLE)return;
        target.requestFocus();((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(target,InputMethodManager.SHOW_IMPLICIT);
    }
    @Override public void onExit(){hideKeyboard();save();openMap();}
    @Override public boolean isSoundEnabled(){return sound;}
    @Override public void onSoundToggle(){sound=!sound;store.setSoundEnabled(sound);playTap(false);game.invalidate();}
    @Override public void playTap(boolean correct){if(sound)try{if(tones==null)tones=new ToneGenerator(AudioManager.STREAM_MUSIC,25);tones.startTone(correct?ToneGenerator.TONE_PROP_ACK:ToneGenerator.TONE_PROP_BEEP,correct?100:35);}catch(RuntimeException ignored){}}
    @Override public boolean isReducedMotion(){try{return Settings.Global.getFloat(getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1f)==0;}catch(RuntimeException ignored){return false;}}
    @Override protected void onResume(){super.onResume();if(game!=null)game.setRunning(true);}
    @Override protected void onPause(){save();if(game!=null)game.setRunning(false);super.onPause();}
    @Override protected void onDestroy(){if(nara!=null)nara.release();if(tones!=null)tones.release();super.onDestroy();}
}
