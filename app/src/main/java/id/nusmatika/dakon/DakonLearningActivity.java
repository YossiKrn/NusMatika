package id.nusmatika.dakon;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
import android.view.WindowManager;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Hosts each independent Dakon level with real Android inputs and accessible tools. */
public final class DakonLearningActivity extends Activity implements DakonLearningView.Host {
    private DakonLearningSession session;
    private DakonLearningStore store;
    private DakonLearningView game;
    private ScrollView scroll;
    private GameLayout content;
    private EditText answer,working;
    private Button back,primary,hint;
    private Dialog hintDialog;
    private final ToolButton[] tools=new ToolButton[6];
    private Typeface font;
    private boolean updating;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        int level=getIntent().getIntExtra("level",-1);
        store=new DakonLearningStore(this);
        if(level<0||level>3||!store.isUnlocked(level)){
            Toast.makeText(this,"Selesaikan level sebelumnya dulu.",Toast.LENGTH_SHORT).show();finish();return;
        }
        session=store.loadSession(level);
        try{font=new Typeface.Builder(getAssets(),"fonts/Nunito.ttf").setFontVariationSettings("'wght' 750").build();}
        catch(RuntimeException error){font=Typeface.create("sans-serif-rounded",Typeface.NORMAL);}
        game=new DakonLearningView(this,session,font,this);
        answer=createInput(false);working=createInput(true);
        back=createButton("×");back.setContentDescription("Simpan dan kembali ke jalur");back.setTextColor(0xFFAFAFAF);
        back.setBackgroundColor(Color.TRANSPARENT);back.setOnClickListener(v->exit());
        hint=createButton("Bantuan");hint.setContentDescription("Buka bantuan untuk soal ini");
        hint.setTextColor(0xFF735099);hint.setOnClickListener(v->showHint());
        primary=createButton("");primary.setOnClickListener(v->performPrimary());
        int[] toolKinds={DakonSession.BLUE,DakonSession.CORAL,DakonLearningSession.PURPLE,DakonSession.REMOVE,DakonLearningSession.UNDO,DakonLearningSession.CLEAR};
        for(int index=0;index<tools.length;index++){
            final int tool=toolKinds[index];
            tools[index]=new ToolButton(this,tool);
            tools[index].setOnClickListener(v->{
                boolean changed=tool==100?session.undo():tool==101?session.clearBoard():session.selectTool(tool);
                if(changed)onBoardChanged();
            });
        }
        content=new GameLayout(this);
        scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);scroll.setBackgroundColor(Color.WHITE);
        scroll.addView(content,new ScrollView.LayoutParams(-1,-2));
        FrameLayout safe=new FrameLayout(this);safe.setBackgroundColor(Color.WHITE);
        safe.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                |View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        safe.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30){
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
                android.graphics.Insets keyboard=insets.getInsets(WindowInsets.Type.ime());
                v.setPadding(bars.left,bars.top,bars.right,Math.max(bars.bottom,keyboard.bottom));
            }else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(safe);safe.requestApplyInsets();refresh();game.requestFocus();
    }

    private Button createButton(String text){
        Button button=new Button(this);button.setText(text);button.setAllCaps(false);button.setTypeface(font);
        button.setTextColor(DakonArtwork.INK);button.setGravity(Gravity.CENTER);button.setMinHeight(0);button.setMinimumHeight(0);
        button.setMinWidth(0);button.setMinimumWidth(0);button.setPadding(0,0,0,0);
        button.setStateListAnimator(null);return button;
    }

    private EditText createInput(boolean isWorking){
        EditText field=new EditText(this);field.setId(isWorking?2101:2102);field.setTypeface(font);
        field.setTextColor(DakonArtwork.INK);field.setHintTextColor(0xFF888888);field.setBackgroundColor(Color.TRANSPARENT);
        field.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);field.setSaveEnabled(false);
        field.setSelectAllOnFocus(false);field.setGravity(Gravity.TOP|Gravity.START);
        if(isWorking){
            field.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            field.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1000)});
            field.setHint("Tulis atau hitung di sini");field.setContentDescription("Caramu. Boleh dikosongkan.");
            field.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        }else{
            field.setSingleLine(true);field.setInputType(InputType.TYPE_CLASS_NUMBER);
            field.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});field.setHint("Jawaban");
            field.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);
            field.setImeOptions(EditorInfo.IME_ACTION_DONE|EditorInfo.IME_FLAG_NO_EXTRACT_UI);
            field.setOnEditorActionListener((v,action,event)->{
                if(action==EditorInfo.IME_ACTION_DONE){hideKeyboard();primary.requestFocus();return true;}return false;
            });
        }
        field.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence text,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence text,int start,int before,int count){
                if(updating||session==null)return;
                if(isWorking)session.setWorking(text.toString());else session.setAnswer(text.toString());
                save();refresh();
            }
            @Override public void afterTextChanged(Editable text){}
        });
        field.setOnFocusChangeListener((v,focused)->{
            if(focused&&scroll!=null)scroll.postDelayed(()->{
                if(v.hasFocus())scroll.smoothScrollTo(0,Math.max(0,v.getTop()-Math.round(72*game.designScale())));
            },250);
        });
        return field;
    }

    private GradientDrawable background(int fill,int border,float radius){
        GradientDrawable drawable=new GradientDrawable();drawable.setColor(fill);drawable.setCornerRadius(radius);
        drawable.setStroke(Math.max(1,Math.round(2*getResources().getDisplayMetrics().density)),border);return drawable;
    }

    private void showHint(){
        if(!session.canShowHint()||hintDialog!=null||isFinishing()||isDestroyed())return;
        float density=getResources().getDisplayMetrics().density;
        Dialog dialog=new Dialog(this);hintDialog=dialog;
        dialog.setOnDismissListener(ignored->{
            if(hintDialog!=dialog)return;
            hintDialog=null;
            if(!isFinishing()&&!isDestroyed())hint.requestFocus();
        });
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);
        int padding=Math.round(24*density);panel.setPadding(padding,padding,padding,padding);
        panel.setBackground(background(Color.WHITE,0xFFE5E5E5,24*density));
        TextView title=new TextView(this);title.setText("Bantuan");title.setTypeface(font);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP,23);title.setTextColor(DakonArtwork.INK);
        panel.addView(title,new LinearLayout.LayoutParams(-1,-2));
        TextView message=new TextView(this);message.setText(session.hintText());message.setTypeface(font);
        message.setTextSize(TypedValue.COMPLEX_UNIT_SP,17);message.setTextColor(DakonArtwork.INK);
        message.setLineSpacing(4*density,1f);
        LinearLayout.LayoutParams messageLayout=new LinearLayout.LayoutParams(-1,-2);
        messageLayout.topMargin=Math.round(14*density);messageLayout.bottomMargin=Math.round(24*density);
        panel.addView(message,messageLayout);
        Button close=createButton("Tutup");close.setTextSize(TypedValue.COMPLEX_UNIT_SP,17);
        close.setBackground(background(DakonArtwork.GREEN,0xFF46A302,15*density));close.setOnClickListener(v->dialog.dismiss());
        panel.addView(close,new LinearLayout.LayoutParams(-1,Math.round(52*density)));
        ScrollView helpScroll=new ScrollView(this);helpScroll.setFillViewport(false);
        helpScroll.setVerticalScrollBarEnabled(false);
        helpScroll.addView(panel,new ScrollView.LayoutParams(-1,-2));
        dialog.setContentView(helpScroll);dialog.setCanceledOnTouchOutside(true);
        Window window=dialog.getWindow();
        if(window!=null){
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params=window.getAttributes();params.dimAmount=.24f;window.setAttributes(params);
        }
        dialog.show();
        if(window!=null)window.setLayout(Math.min(Math.round(328*density),getResources().getDisplayMetrics().widthPixels-Math.round(32*density)),WindowManager.LayoutParams.WRAP_CONTENT);
    }

    private void refresh(){
        if(session==null||game==null)return;
        boolean board=game.boardVisible();
        for(int index=0;index<tools.length;index++){
            ToolButton button=tools[index];int tool=button.tool;
            boolean seed=tool==DakonSession.BLUE||tool==DakonSession.CORAL||tool==DakonLearningSession.PURPLE;
            button.setVisibility(board&&(tool!=DakonLearningSession.PURPLE||game.threeOperandTools())?View.VISIBLE:View.GONE);
            button.setText(seed?session.seedLabel(tool):tool==DakonSession.REMOVE?"Hapus":tool==DakonLearningSession.UNDO?"Urungkan":"Kosongkan");
            boolean seedEnabled=session.getLevel()>=2||session.getTool()==tool||session.highlightTool()==tool;
            boolean enabled=tool==DakonSession.REMOVE?session.canRemove():tool==DakonLearningSession.UNDO?session.canUndo():tool==DakonLearningSession.CLEAR?session.canClear():board&&seedEnabled;
            button.setEnabled(enabled);button.setAlpha(enabled?1f:.38f);
            boolean selected=(seed||tool==DakonSession.REMOVE)&&session.getTool()==tool;
            int color=tool==DakonSession.CORAL?DakonArtwork.CORAL:tool==DakonLearningSession.PURPLE?DakonArtwork.PURPLE:DakonArtwork.BLUE;
            int border=selected?color:session.highlightTool()==button.tool?DakonArtwork.GREEN:0xFFE5E5E5;
            button.setSelected(selected);
            button.setContentDescription(button.getText()+(selected?", dipilih":"")+(session.highlightTool()==button.tool?", gunakan sekarang":""));
            button.setBackground(background(selected?(tool==DakonSession.CORAL?0xFFFFE8E8:tool==DakonLearningSession.PURPLE?0xFFF0E4FD:0xFFDDF4FF):Color.WHITE,border,13*getResources().getDisplayMetrics().density));
        }
        hint.setVisibility(session.canShowHint()?View.VISIBLE:View.GONE);
        hint.setBackground(background(0xFFF7F0FF,0xFFE6D8F5,13*getResources().getDisplayMetrics().density));
        boolean input=session.isAnswerVisible()&&!session.isComplete();
        answer.setVisibility(input?View.VISIBLE:View.GONE);
        working.setVisibility(session.isWorksheet()&&!session.isComplete()?View.VISIBLE:View.GONE);
        answer.setContentDescription("Jawaban"+(session.answerUnit().isEmpty()?"":" dalam "+session.answerUnit()));
        updating=true;
        updateText(answer,session.getAnswer());updateText(working,session.getWorking());updating=false;
        primary.setText(session.isComplete()?"Kembali ke jalur":session.primaryLabel());
        primary.setEnabled(session.isComplete()||session.primaryEnabled());
        primary.setBackground(background(primary.isEnabled()?DakonArtwork.GREEN:0xFFE5E5E5,primary.isEnabled()?0xFF46A302:0xFFD7D7D7,15*getResources().getDisplayMetrics().density));
        primary.setTextColor(primary.isEnabled()?DakonArtwork.INK:0xFFAFAFAF);
        game.setContentDescription(session.isComplete()?session.title()+" selesai. Kembali ke jalur."
                :session.title()+". "+session.progressText()+". "+session.instruction()+". "+session.prompt());
        content.requestLayout();game.invalidate();
        if(session.isComplete())store.markComplete(session.getLevel());
    }

    private void updateText(EditText field,String value){
        if(!field.getText().toString().equals(value)){field.setText(value);field.setSelection(field.length());}
    }
    private void save(){if(store!=null&&session!=null)store.saveSession(session);}
    private void hideKeyboard(){
        if(answer==null)return;
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(answer.getWindowToken(),0);
        answer.clearFocus();working.clearFocus();game.requestFocus();
    }
    private void exit(){hideKeyboard();save();finish();}
    private void performPrimary(){
        if(session.isComplete()){exit();return;}
        if(!session.primaryEnabled())return;
        hideKeyboard();
        String previousPage=session.progressText()+"|"+session.prompt();
        if(session.primaryAction()){
            save();refresh();
            if(!session.feedback().isEmpty())game.announceForAccessibility(session.feedback());
            boolean nextPage=session.isComplete()||!previousPage.equals(session.progressText()+"|"+session.prompt());
            scroll.post(()->scroll.smoothScrollTo(0,nextPage?0:Math.max(0,Math.round((game.feedbackTop()-180)*game.designScale()))));
        }
    }
    @Override public void onBoardChanged(){save();refresh();}
    @Override public boolean isReducedMotion(){
        try{return Settings.Global.getFloat(getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1f)==0f;}
        catch(RuntimeException error){return false;}
    }
    @Override public void onBackPressed(){exit();}
    @Override protected void onResume(){super.onResume();if(game!=null)game.setRunning(true);}
    @Override protected void onPause(){save();if(game!=null)game.setRunning(false);super.onPause();}
    @Override protected void onDestroy(){
        if(hintDialog!=null){Dialog dialog=hintDialog;hintDialog=null;dialog.dismiss();}
        if(game!=null)game.release();super.onDestroy();
    }

    private final class GameLayout extends FrameLayout {
        GameLayout(Context context){
            super(context);addView(game);addView(back);addView(hint);
            for(ToolButton tool:tools)addView(tool);
            addView(working);addView(answer);addView(primary);
            setFocusable(false);
        }
        private void measureAt(View view,float width,float height,float scale){
            view.measure(MeasureSpec.makeMeasureSpec(Math.round(width*scale),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.round(height*scale),MeasureSpec.EXACTLY));
        }
        @Override protected void onMeasure(int widthSpec,int heightSpec){
            int width=MeasureSpec.getSize(widthSpec);float scale=width/360f;
            int height=Math.max(Math.round(game.minimumHeight()*scale),MeasureSpec.getSize(heightSpec));
            setMeasuredDimension(width,height);measureAt(game,360,height/scale,scale);
            measureAt(back,48,48,scale);back.setTextSize(TypedValue.COMPLEX_UNIT_PX,30*scale);
            measureAt(hint,104,44,scale);hint.setTextSize(TypedValue.COMPLEX_UNIT_PX,14*scale);
            boolean three=game.threeOperandTools();
            for(ToolButton tool:tools){
                boolean action=tool.tool==DakonSession.REMOVE||tool.tool==DakonLearningSession.UNDO||tool.tool==DakonLearningSession.CLEAR;
                measureAt(tool,three?104:60.8f,three&&action?54:68,scale);
                tool.setTextSize(TypedValue.COMPLEX_UNIT_PX,(three?11:10)*scale);
                tool.setPadding(0,Math.round((three&&action?26:31)*scale),0,Math.round(5*scale));
            }
            measureAt(primary,328,56,scale);primary.setTextSize(TypedValue.COMPLEX_UNIT_PX,17*scale);
            measureAt(working,328,130,scale);measureAt(answer,session.answerUnit().isEmpty()?328:228,54,scale);
            answer.setTextSize(TypedValue.COMPLEX_UNIT_PX,20*scale);answer.setPadding(Math.round(14*scale),0,Math.round(14*scale),0);
            working.setTextSize(TypedValue.COMPLEX_UNIT_PX,17*scale);int pad=Math.round(13*scale);working.setPadding(pad,pad,pad,pad);
        }
        private void place(View view,float x,float y,float width,float height,float scale){
            view.layout(Math.round(x*scale),Math.round(y*scale),Math.round((x+width)*scale),Math.round((y+height)*scale));
        }
        @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){
            float scale=(right-left)/360f;game.layout(0,0,right-left,bottom-top);
            place(back,4,3,48,48,scale);
            place(hint,240,50,104,44,scale);
            if(game.threeOperandTools()){
                for(int index=0;index<tools.length;index++)place(tools[index],16+(index%3)*112,index<3?534:610,104,index<3?68:54,scale);
            }else{
                int slot=0;for(ToolButton tool:tools)if(tool.tool!=DakonLearningSession.PURPLE){place(tool,16+slot*66.8f,534,60.8f,68,scale);slot++;}
            }
            place(working,16,game.workingTop(),328,130,scale);
            place(answer,16,game.answerTop(),session.answerUnit().isEmpty()?328:228,54,scale);
            place(primary,16,game.actionTop((bottom-top)/scale),328,56,scale);
        }
    }

    /** These remain native Buttons, with the same seed silhouettes as the board. */
    private final class ToolButton extends Button {
        private final int tool;
        private final DakonArtwork art;
        ToolButton(Context context,int tool){
            super(context);this.tool=tool;art=new DakonArtwork(context,font);
            setLayerType(View.LAYER_TYPE_SOFTWARE,null);
            setAllCaps(false);setTypeface(font);setTextColor(DakonArtwork.INK);setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
            setMinWidth(0);setMinimumWidth(0);setMinHeight(0);setMinimumHeight(0);setStateListAnimator(null);setMaxLines(2);
        }
        @Override protected void onDraw(Canvas canvas){
            float scale=game.designScale();if(scale<=0){super.onDraw(canvas);return;}
            canvas.save();canvas.scale(scale,scale);
            boolean seed=tool==DakonSession.BLUE||tool==DakonSession.CORAL||tool==DakonLearningSession.PURPLE;
            float x=getWidth()/scale/2,y=game.threeOperandTools()&&!seed?16:20;
            if(seed)art.seed(canvas,x,y,tool,12);
            else{
                art.stroke(0xFF777777,2.2f);Path path=new Path();
                if(tool==DakonSession.REMOVE){
                    path.moveTo(x-9,y+4);path.lineTo(x-2,y-7);path.lineTo(x+8,y-1);path.lineTo(x+2,y+9);path.lineTo(x-4,y+9);path.close();canvas.drawPath(path,art.paint);
                    canvas.drawLine(x-3,y-1,x+5,y+4,art.paint);
                }else if(tool==100){
                    path.moveTo(x-9,y-3);path.lineTo(x+2,y-3);path.cubicTo(x+15,y-3,x+12,y+12,x-2,y+8);canvas.drawPath(path,art.paint);
                    canvas.drawLine(x-9,y-3,x-4,y-8,art.paint);canvas.drawLine(x-9,y-3,x-4,y+2,art.paint);
                }else{
                    canvas.drawLine(x-8,y-6,x+8,y-6,art.paint);canvas.drawLine(x-3,y-10,x+3,y-10,art.paint);
                    path.moveTo(x-6,y-6);path.lineTo(x-5,y+10);path.lineTo(x+5,y+10);path.lineTo(x+6,y-6);canvas.drawPath(path,art.paint);
                    canvas.drawLine(x-2,y-2,x-2,y+6,art.paint);canvas.drawLine(x+2,y-2,x+2,y+6,art.paint);
                }
            }
            if(session.highlightTool()==tool){
                art.stroke(0xFF3C8A00,1.8f);canvas.drawLine(x,3,x,8,art.paint);canvas.drawLine(x-3,5,x,8,art.paint);canvas.drawLine(x+3,5,x,8,art.paint);
            }
            canvas.restore();super.onDraw(canvas);
        }
    }
}
