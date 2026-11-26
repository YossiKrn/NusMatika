package id.nusmatika.dakon;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;

import java.util.ArrayList;
import java.util.List;

/** Approved Dakon artwork with lesson cues; native controls are hosted by the activity. */
public final class DakonLearningView extends View {
    interface Host {
        void onBoardChanged();
        boolean isReducedMotion();
    }

    private final DakonLearningSession session;
    private final Host host;
    private final DakonArtwork art;
    private final Paint wrappingPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private NaraRenderer nara;
    private final long animationEpoch=SystemClock.uptimeMillis();
    private boolean running=true;
    private int pressed,accessibilityFocus=-1,hovered=-1,keyboardHole=1;

    DakonLearningView(Context context,DakonLearningSession session,Typeface font,Host host){
        super(context);
        this.session=session;this.host=host;
        art=new DakonArtwork(context,font);
        wrappingPaint.setTypeface(font);
        nara=new NaraRenderer(context);
        setBackgroundColor(Color.WHITE);
        // This scrolling surface can exceed the screen height when the keyboard
        // opens. A software bitmap layer would exceed Android 9's drawing cache.
        setLayerType(View.LAYER_TYPE_NONE,null);
        setFocusable(true);setFocusableInTouchMode(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    void setRunning(boolean value){running=value;if(value)invalidate();}
    void release(){running=false;if(nara!=null){nara.release();nara=null;}}
    float designScale(){return getWidth()/360f;}
    boolean boardVisible(){return !session.isWorksheet()&&!session.isComplete();}
    boolean threeOperandTools(){return boardVisible()&&session.operandCount()==3;}
    float toolsBottom(){return threeOperandTools()?664:602;}
    private float boardPromptTop(){return toolsBottom()+12;}

    private List<String> lines(String text,float width,float size){
        List<String> result=new ArrayList<>();
        if(text==null||text.isEmpty())return result;
        wrappingPaint.setTextSize(size);
        for(String paragraph:text.split("\\n",-1)){
            String line="";
            for(String word:paragraph.split(" +")){
                String next=line.isEmpty()?word:line+" "+word;
                if(!line.isEmpty()&&wrappingPaint.measureText(next)>width){result.add(line);line=word;}
                else line=next;
            }
            result.add(line);
        }
        return result;
    }

    private float drawWrapped(Canvas c,String text,float x,float top,float width,float size,float spacing,int color,int weight){
        float y=top+size;
        for(String line:lines(text,width,size)){art.text(c,line,x,y,size,weight,color,false);y+=spacing;}
        return y-spacing;
    }

    private boolean promptInHeading(){return boardVisible()&&session.prompt()!=null&&session.prompt().length()<=56&&!session.prompt().isEmpty();}
    private float promptBottom(){
        if(session.isComplete())return 250;
        if(session.isWorksheet())return 216+Math.max(1,lines(session.prompt(),328,20).size())*27;
        if(promptInHeading())return toolsBottom()+6;
        return boardPromptTop()+lines(session.prompt(),328,15).size()*20;
    }
    float workingTop(){return Math.max(340,promptBottom()+40);}
    float answerTop(){return session.isWorksheet()?workingTop()+174:promptBottom()+36;}
    float feedbackTop(){return session.isWorksheet()||session.isAnswerVisible()?answerTop()+80:promptBottom()+16;}
    float minimumActionTop(){
        if(session.isComplete())return 450;
        if(session.isWorksheet())return feedbackTop()+Math.max(48,lines(session.feedback(),328,14).size()*20+14);
        return Math.max(684+(threeOperandTools()?62:0),feedbackTop()+Math.max(44,lines(session.feedback(),328,14).size()*20+12));
    }
    float minimumHeight(){return minimumActionTop()+78;}
    float actionTop(float designHeight){return Math.max(minimumActionTop(),designHeight-78);}

    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);if(getWidth()==0)return;
        float scale=designScale();canvas.save();canvas.scale(scale,scale);
        long elapsed=SystemClock.uptimeMillis()-animationEpoch;
        boolean reduced=host.isReducedMotion();
        drawHeader(canvas);
        String heading=promptInHeading()?session.prompt():session.isComplete()?"Selesai":session.title();
        boolean hint=session.canShowHint();
        String eyebrow=promptInHeading()||hint?session.title()+" · "+session.progressText():session.progressText();
        if(hint)eyebrow=eyebrow.replace(" / ","/");
        float eyebrowSize=14;
        art.text(canvas,"",16,76,eyebrowSize,800,0xFF777777,false);
        float eyebrowWidth=art.paint.measureText(eyebrow),eyebrowAvailable=hint?216:328;
        if(eyebrowWidth>eyebrowAvailable)eyebrowSize*=eyebrowAvailable/eyebrowWidth;
        art.text(canvas,eyebrow,16,76,eyebrowSize,800,0xFF777777,false);
        // Measure the actual weighted face so every question keeps both margins.
        float headingSize=27;
        art.text(canvas,"",16,112,headingSize,850,DakonArtwork.INK,false);
        float headingWidth=art.paint.measureText(heading);
        if(headingWidth>328)headingSize*=328/headingWidth;
        art.text(canvas,heading,16,hint?122:112,headingSize,850,DakonArtwork.INK,false);
        if(nara!=null)nara.draw(canvas,new RectF(19,hint?126:116,161,198),elapsed,reduced);
        if(!session.isComplete()){
            List<String> instruction=lines(session.instruction(),191,14);
            float top=(hint?161:156)-(Math.min(4,instruction.size())*18)/2f;
            drawWrapped(canvas,session.instruction(),153,top,191,14,18,DakonArtwork.INK,750);
        }
        if(session.isComplete()){
            art.text(canvas,session.title()+" sudah selesai.",16,238,20,750,DakonArtwork.INK,false);
        }else if(boardVisible()){
            art.board(canvas,session,0);
            drawBoardCues(canvas,elapsed,reduced);
            if(!promptInHeading())drawWrapped(canvas,session.prompt(),16,boardPromptTop(),328,15,20,DakonArtwork.INK,750);
        }else if(session.isWorksheet()){
            drawWrapped(canvas,session.prompt(),16,216,328,20,27,DakonArtwork.INK,750);
            art.text(canvas,"Caramu (boleh dikosongkan)",16,workingTop()-14,15,800,DakonArtwork.INK,false);
            field(canvas,16,workingTop(),328,130);
        }
        if(!session.isComplete()&&session.isAnswerVisible()){
            art.text(canvas,"Jawaban",16,answerTop()-13,15,800,DakonArtwork.INK,false);
            field(canvas,16,answerTop(),session.answerUnit().isEmpty()?328:228,54);
            if(!session.answerUnit().isEmpty())drawWrapped(canvas,session.answerUnit(),258,answerTop()+13,86,15,19,0xFF777777,750);
        }
        if(!session.isComplete()&&!session.feedback().isEmpty()){
            drawWrapped(canvas,session.feedback(),16,feedbackTop(),328,14,20,
                    session.isFeedbackCorrect()?0xFF3C8A00:DakonArtwork.INK,800);
        }
        if(boardVisible()&&accessibilityFocus>=1&&accessibilityFocus<=24){
            art.stroke(DakonArtwork.BLUE,2);canvas.drawRoundRect(holeBounds(accessibilityFocus),8,8,art.paint);
        }
        canvas.restore();
        if(running&&isShown()&&!reduced)postInvalidateDelayed(session.highlightHole()>0?33:83);
    }

    private void field(Canvas c,float x,float y,float w,float h){
        art.round(c,x,y,w,h,13,0xFFF7F7F7);
        art.stroke(0xFFE5E5E5,2);c.drawRoundRect(x,y,x+w,y+h,13,13,art.paint);
    }

    private void drawHeader(Canvas c){
        art.round(c,56,20,288,14,7,0xFFE5E5E5);
        float fraction=session.progressFraction();
        fraction=Math.min(1,Math.max(0,fraction));
        if(session.isComplete())fraction=1;
        if(fraction>0){
            float width=Math.max(14,288*fraction);
            art.round(c,56,20,width,14,7,DakonArtwork.GREEN);
            if(width>14)art.round(c,62,23,width-14,4,2,0xFF89E219);
        }
    }

    private void drawBoardCues(Canvas c,long elapsed,boolean reduced){
        for(int hole:session.commonHoles()){
            if(hole<1||hole>24)continue;
            art.stroke(DakonArtwork.GREEN,2.2f);
            c.drawCircle(BoardGeometry.centerX(hole),BoardGeometry.centerY(hole),24.7f,art.paint);
        }
        int hole=session.highlightHole();
        if(hole<1||hole>24)return;
        float x=BoardGeometry.centerX(hole),y=BoardGeometry.centerY(hole);
        float pulse=reduced?0:(float)Math.sin(elapsed/320.0);
        art.stroke(0xFF58CC02,2.4f);art.paint.setAlpha(160+(int)(35*pulse));
        c.drawCircle(x,y,25.5f+pulse*.8f,art.paint);art.paint.setAlpha(255);
        // A short arrow points at the precise place to tap without hiding the seed.
        float top=y-36-(reduced?0:Math.max(0,pulse)*2);
        art.stroke(0xFF3C8A00,2.2f);c.drawLine(x,top-8,x,top+2,art.paint);
        Path arrow=new Path();arrow.moveTo(x-4,top-2);arrow.lineTo(x,top+3);arrow.lineTo(x+4,top-2);c.drawPath(arrow,art.paint);
    }

    private int hit(float x,float y){return boardVisible()?BoardGeometry.hitHole(x,y):0;}
    @Override public boolean onTouchEvent(MotionEvent event){
        float scale=designScale();if(scale<=0)return false;
        int hole=hit(event.getX()/scale,event.getY()/scale);
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:pressed=hole;return hole>0;
            case MotionEvent.ACTION_MOVE:if(hole!=pressed)pressed=0;return pressed>0;
            case MotionEvent.ACTION_CANCEL:pressed=0;return true;
            case MotionEvent.ACTION_UP:
                int down=pressed;pressed=0;
                if(hole>0&&hole==down){performClick();tap(hole);return true;}
                return down>0;
            default:return false;
        }
    }
    @Override public boolean performClick(){super.performClick();return true;}

    private boolean tap(int hole){
        if(!boardVisible())return false;
        if(session.tapHole(hole)){
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            host.onBoardChanged();invalidate();sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
            return true;
        }
        if(session.highlightHole()>0)announceForAccessibility("Ketuk lubang "+session.highlightHole());
        invalidate();return false;
    }

    private RectF holeBounds(int hole){
        float x=BoardGeometry.centerX(hole),y=BoardGeometry.centerY(hole);
        return new RectF(x-26,y-24,x+26,y+43);
    }
    private String holeLabel(int hole){
        int blue=0,coral=0,purple=0;
        for(int seed:session.getSeeds(hole)){
            if(seed==DakonSession.BLUE)blue++;
            else if(seed==DakonLearningSession.PURPLE)purple++;
            else coral++;
        }
        if(blue+coral+purple==0)return "Lubang "+hole+", kosong";
        String label="Lubang "+hole+", "+blue+" biji biru, "+coral+" biji merah";
        return session.operandCount()==3?label+", "+purple+" biji ungu":label;
    }

    @Override public AccessibilityNodeProvider getAccessibilityNodeProvider(){return provider;}
    private final AccessibilityNodeProvider provider=new AccessibilityNodeProvider(){
        @Override public AccessibilityNodeInfo createAccessibilityNodeInfo(int id){
            if(id==View.NO_ID){
                AccessibilityNodeInfo node=AccessibilityNodeInfo.obtain(DakonLearningView.this);
                onInitializeAccessibilityNodeInfo(node);
                if(boardVisible())for(int hole=1;hole<=24;hole++)node.addChild(DakonLearningView.this,hole);
                return node;
            }
            if(!boardVisible()||id<1||id>24)return null;
            AccessibilityNodeInfo node=AccessibilityNodeInfo.obtain();
            node.setSource(DakonLearningView.this,id);node.setParent(DakonLearningView.this);
            node.setPackageName(getContext().getPackageName());node.setClassName("android.widget.Button");
            node.setContentDescription(holeLabel(id));node.setFocusable(true);node.setClickable(true);node.setEnabled(true);
            node.setAccessibilityFocused(accessibilityFocus==id);node.setVisibleToUser(isShown());
            RectF b=holeBounds(id);float scale=designScale();
            Rect bounds=new Rect(Math.round(b.left*scale),Math.round(b.top*scale),Math.round(b.right*scale),Math.round(b.bottom*scale));
            node.setBoundsInParent(bounds);int[] location=new int[2];getLocationOnScreen(location);bounds.offset(location[0],location[1]);node.setBoundsInScreen(bounds);
            node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK);
            node.addAction(accessibilityFocus==id?AccessibilityNodeInfo.AccessibilityAction.ACTION_CLEAR_ACCESSIBILITY_FOCUS:AccessibilityNodeInfo.AccessibilityAction.ACTION_ACCESSIBILITY_FOCUS);
            return node;
        }
        @Override public boolean performAction(int id,int action,Bundle arguments){
            if(!boardVisible()||id<1||id>24)return false;
            if(action==AccessibilityNodeInfo.ACTION_CLICK)return tap(id);
            if(action==AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS){accessibilityFocus=id;accessibilityEvent(id,AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);invalidate();return true;}
            if(action==AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS){accessibilityFocus=-1;invalidate();return true;}
            return false;
        }
    };

    private void accessibilityEvent(int hole,int type){
        if(getParent()==null)return;
        AccessibilityEvent event=AccessibilityEvent.obtain(type);event.setPackageName(getContext().getPackageName());
        event.setSource(this,hole);event.setContentDescription(holeLabel(hole));getParent().requestSendAccessibilityEvent(this,event);
    }
    @Override public boolean dispatchHoverEvent(MotionEvent event){
        AccessibilityManager manager=(AccessibilityManager)getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        if(manager!=null&&manager.isTouchExplorationEnabled()&&boardVisible()){
            int hole=event.getActionMasked()==MotionEvent.ACTION_HOVER_EXIT?-1:hit(event.getX()/designScale(),event.getY()/designScale());
            if(hole!=hovered){
                if(hovered>0)accessibilityEvent(hovered,AccessibilityEvent.TYPE_VIEW_HOVER_EXIT);
                hovered=hole;if(hole>0)accessibilityEvent(hole,AccessibilityEvent.TYPE_VIEW_HOVER_ENTER);
            }
            return hole>0;
        }
        return super.dispatchHoverEvent(event);
    }

    @Override public boolean onKeyDown(int keyCode,KeyEvent event){
        if(!boardVisible())return super.onKeyDown(keyCode,event);
        if(keyCode==KeyEvent.KEYCODE_DPAD_CENTER||keyCode==KeyEvent.KEYCODE_ENTER){tap(keyboardHole);return true;}
        if(keyCode==KeyEvent.KEYCODE_DPAD_RIGHT)keyboardHole=Math.min(24,keyboardHole+1);
        else if(keyCode==KeyEvent.KEYCODE_DPAD_LEFT)keyboardHole=Math.max(1,keyboardHole-1);
        else if(keyCode==KeyEvent.KEYCODE_DPAD_DOWN){if(keyboardHole>18)return super.onKeyDown(keyCode,event);keyboardHole+=6;}
        else if(keyCode==KeyEvent.KEYCODE_DPAD_UP){if(keyboardHole<=6)return super.onKeyDown(keyCode,event);keyboardHole-=6;}
        else return super.onKeyDown(keyCode,event);
        accessibilityFocus=keyboardHole;invalidate();return true;
    }
}
