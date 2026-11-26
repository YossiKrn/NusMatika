package id.nusmatika.dakon;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
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

/** Portrait game surface. All interaction uses the same coordinates as the art. */
public final class DakonView extends View {
    public interface Host {
        void onSessionChanged(); void onExit(); void onSoundToggle(); void onRequestAnswer();
        WorksheetState worksheet(); int focusedWorksheetField(); void onCheckWorksheet();
        boolean isSoundEnabled(); boolean isReducedMotion(); void playTap(boolean correct);
    }
    private final DakonSession session;
    private final NaraRenderer nara;
    private final Host host;
    private final DakonArtwork art;
    private final long animationEpoch=SystemClock.uptimeMillis();
    private boolean running=true;
    private int pressed=-1,accessibilityFocus=-1,hovered=-1,keyboardFocus=1;
    private String bubble="Biji";
    private int movingHole,movingKind;
    private float movingStartX,movingStartY;
    private long movingAt;
    private static final int[] TOOL_IDS={0,1,4,5};

    public DakonView(Context context,DakonSession session,NaraRenderer nara,Typeface font,Host host){
        super(context);this.session=session;this.nara=nara;this.host=host;art=new DakonArtwork(context,font);
        if(session.getTool()==DakonSession.REMOVE||session.getTool()==DakonSession.MOVE)session.setTool(DakonSession.BLUE);
        setBackgroundColor(Color.WHITE);setFocusable(true);setFocusableInTouchMode(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }
    public void setRunning(boolean value){running=value;if(value)invalidate();}
    private float scale(){return getWidth()/360f;}
    private float actionY(){return Math.max(668,getHeight()/Math.max(.01f,scale())-72);}
    private boolean actionEnabled(){
        int stage=session.getStage();
        return stage!=DakonSession.NIROKKE&&stage!=DakonSession.NAMBAHI
                ||stage==DakonSession.NIROKKE&&session.isGuideComplete()
                ||stage==DakonSession.NAMBAHI&&host.worksheet().isComplete();
    }
    private String actionLabel(){
        if(session.getStage()==DakonSession.COMPLETE)return "MAIN LAGI";
        if(session.getStage()==DakonSession.NAMBAHI)return host.worksheet().isCorrect()?"SELESAI":"PERIKSA";
        return "LANJUT";
    }
    private boolean hasSeeds(){for(int i=1;i<=24;i++)if(session.getSeeds(i).length>0)return true;return false;}
    private boolean isToolId(int id){return id==100||id==101||id==104||id==105;}
    private boolean isVirtualId(int id){return session.getStage()<2&&(id>=1&&id<=24||isToolId(id))||id>=200&&id<=202;}
    private boolean toolEnabled(int tool){
        if(!isToolId(100+tool))return false;
        if(session.getStage()>=DakonSession.NAMBAHI)return false;
        if(tool==4)return session.canUndo();
        if(tool==5)return hasSeeds();
        if(session.getStage()==DakonSession.NIROKKE)return tool==session.getExpectedKind()&&!session.isGuideComplete();
        return true;
    }

    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);if(getWidth()==0)return;
        canvas.save();canvas.scale(scale(),scale());
        long now=SystemClock.uptimeMillis(),elapsed=now-animationEpoch;
        boolean reduced=host.isReducedMotion();
        drawHeader(canvas);
        int stage=session.getStage();
        String stageName=stage==0?"Niteni":stage==1?"Nirokke":stage==2?"Nambahi":"Latihan KPK";
        art.text(canvas,stageName,16,76,14,800,0xFF777777,false);
        art.text(canvas,stage==0?"Kenali dakon":stage==3?"Selesai!":"KPK 3 dan 4",16,112,27,850,DakonArtwork.INK,false);
        nara.draw(canvas,new RectF(19,116,161,198),elapsed,reduced);
        if(stage==0)drawBubble(canvas);
        if(stage==1){
            art.seed(canvas,217,159,0,13);art.text(canvas,"3",240,165,21,800,DakonArtwork.INK,false);
            art.seed(canvas,297,159,1,13);art.text(canvas,"4",320,165,21,800,DakonArtwork.INK,false);
        }
        if(stage==2)drawFeedback(canvas);
        if(stage==3)art.text(canvas,"KPK-nya 12.",180,167,18,800,DakonArtwork.INK,false);
        boolean landing=stage<2&&movingHole>0&&session.getSeeds(movingHole).length<=4&&now-movingAt<300&&!reduced;
        if(stage>=DakonSession.NAMBAHI){
            movingHole=0;drawWorksheet(canvas);
        }else{
            art.board(canvas,session,landing?movingHole:0);
            if(landing)drawLanding(canvas,now);else movingHole=0;
            if(stage==DakonSession.NIROKKE&&!session.isGuideComplete())drawGuidance(canvas,elapsed,reduced);
            drawTools(canvas);
        }
        drawAction(canvas);
        if(accessibilityFocus!=-1&&enabled(accessibilityFocus)){
            RectF bounds=bounds(accessibilityFocus);
            art.stroke(DakonArtwork.BLUE,2);canvas.drawRoundRect(bounds,6,6,art.paint);
        }
        canvas.restore();
        if(running&&isShown()&&!reduced)postInvalidateDelayed(stage==1||landing?33:83);
    }
    private void drawHeader(Canvas c){
        art.stroke(0xFFAFAFAF,2.5f);c.drawLine(21,20,35,34,art.paint);c.drawLine(35,20,21,34,art.paint);
        art.round(c,56,20,240,14,7,0xFFE5E5E5);
        int progress=session.getStage()==0?72:session.getStage()==1?144:session.getStage()==2?192:240;
        art.round(c,56,20,progress,14,7,DakonArtwork.GREEN);
        art.round(c,62,23,progress-14,4,2,0xFF89E219);
        art.stroke(host.isSoundEnabled()?0xFF777777:0xFFAFAFAF,2.5f);
        Path p=new Path();p.moveTo(321,23);p.lineTo(325,23);p.lineTo(331,18);p.lineTo(331,36);p.lineTo(325,31);p.lineTo(321,31);p.close();c.drawPath(p,art.paint);
        if(host.isSoundEnabled()){p.reset();p.moveTo(336,22);p.cubicTo(340,25,340,29,336,32);c.drawPath(p,art.paint);}
        else{c.drawLine(336,23,342,31,art.paint);c.drawLine(342,23,336,31,art.paint);}
    }
    private void drawBubble(Canvas c){
        float width=Math.min(132,Math.max(74,bubble.length()*7.8f+22)),left=261-width/2;
        art.round(c,left,155,width,31,11,Color.WHITE);
        art.stroke(0xFFE5E5E5,2);c.drawRoundRect(left,155,left+width,186,11,11,art.paint);
        art.text(c,bubble,261,176,15,800,DakonArtwork.INK,true);
        art.stroke(0xFFAFAFAF,2);for(int y=190;y<214;y+=7)c.drawLine(261,y,261,y+3,art.paint);
    }
    private void drawFeedback(Canvas c){
        if(!host.worksheet().isChecked())return;
        boolean correct=host.worksheet().isCorrect();
        art.text(c,correct?"Benar! KPK-nya 12.":"Cek isian yang ditandai.",157,165,14,800,correct?0xFF3C8A00:DakonArtwork.INK,false);
    }
    private void drawWorksheet(Canvas c){
        boolean complete=session.getStage()==DakonSession.COMPLETE;
        if(!complete)art.text(c,"Isi empat kelipatan pertama.",16,222,14,700,0xFF777777,false);
        art.text(c,"Kelipatan 3",16,259,17,800,DakonArtwork.INK,false);
        art.text(c,"Kelipatan 4",16,361,17,800,DakonArtwork.INK,false);
        art.text(c,"Kelipatan yang sama",16,469,17,800,DakonArtwork.INK,false);
        art.text(c,"KPK",16,577,17,800,DakonArtwork.INK,false);
        for(int i=0;i<10;i++){
            float x=WorksheetLayout.x(i),y=WorksheetLayout.y(i),w=WorksheetLayout.width(i),h=WorksheetLayout.height(i);
            boolean wrong=host.worksheet().isIncorrect(i),correct=host.worksheet().isCorrect()||complete;
            int border=wrong?DakonArtwork.CORAL:correct?DakonArtwork.GREEN:host.focusedWorksheetField()==i?DakonArtwork.BLUE:0xFFE5E5E5;
            art.round(c,x,y,w,h,13,wrong?0xFFFFF0F0:0xFFF7F7F7);
            art.stroke(border,2);c.drawRoundRect(x,y,x+w,y+h,13,13,art.paint);
            if(complete)art.text(c,host.worksheet().get(i),x+w/2,y+h/2+7,i==9?22:20,800,DakonArtwork.INK,true);
        }
    }
    private void drawGuidance(Canvas c,long elapsed,boolean reduced){
        int hole=session.getExpectedHole();if(hole==0)return;
        float x=BoardGeometry.centerX(hole),y=BoardGeometry.centerY(hole);
        float t=reduced?1:(elapsed%1800)/1800f;
        float offset=reduced?0:(float)(-12*Math.sin(t*Math.PI));
        int layer=c.saveLayerAlpha(x-20,y-28,x+20,y+24,135);
        art.seed(c,x,y+2.4f+offset,session.getExpectedKind(),9.7f);c.restoreToCount(layer);
    }
    private void drawLanding(Canvas c,long now){
        int[] seeds=session.getSeeds(movingHole);if(seeds.length==0)return;
        float[][] slots=DakonArtwork.slots(seeds.length);
        int index=Math.min(seeds.length-1,slots.length-1);
        float endX=BoardGeometry.centerX(movingHole)+slots[index][0],endY=BoardGeometry.centerY(movingHole)+2.4f+slots[index][1];
        float t=Math.min(1,(now-movingAt)/300f),ease=1-(1-t)*(1-t)*(1-t);
        float x=movingStartX+(endX-movingStartX)*ease,y=movingStartY+(endY-movingStartY)*ease-18*(float)Math.sin(t*Math.PI);
        art.seed(c,x,y,movingKind,slots[index][2]*(1+.08f*(float)Math.sin(t*Math.PI)));
    }
    private void drawTools(Canvas c){
        for(int i:TOOL_IDS){
            float x=16+i*56,y=pressed==100+i?536:534;
            boolean enabled=toolEnabled(i),selected=i<2&&session.getTool()==i&&session.getStage()!=DakonSession.COMPLETE;
            int border=selected?(i==1?DakonArtwork.CORAL:DakonArtwork.BLUE):0xFFE5E5E5;
            art.round(c,x,y,48,48,13,selected?(i==1?0xFFFFE8E8:0xFFDDF4FF):Color.WHITE);
            art.stroke(border,2);c.drawRoundRect(x,y,x+48,y+48,13,13,art.paint);
            int layer=c.saveLayerAlpha(x,y,x+48,y+48,enabled?255:95);
            float cx=x+24,cy=y+24;
            if(i<2)art.seed(c,cx,cy,i,15);
            else{
                art.stroke(0xFF777777,2.5f);
                if(i==4){
                    Path p=new Path();p.moveTo(cx-9,cy-3);p.lineTo(cx+2,cy-3);p.cubicTo(cx+15,cy-3,cx+12,cy+14,cx-2,cy+9);c.drawPath(p,art.paint);
                    c.drawLine(cx-9,cy-3,cx-4,cy-8,art.paint);c.drawLine(cx-9,cy-3,cx-4,cy+2,art.paint);
                }
                if(i==5){
                    c.drawLine(cx-8,cy-6,cx+8,cy-6,art.paint);c.drawLine(cx-3,cy-10,cx+3,cy-10,art.paint);
                    Path p=new Path();p.moveTo(cx-6,cy-6);p.lineTo(cx-5,cy+10);p.lineTo(cx+5,cy+10);p.lineTo(cx+6,cy-6);c.drawPath(p,art.paint);
                    c.drawLine(cx-2,cy-2,cx-2,cy+6,art.paint);c.drawLine(cx+2,cy-2,cx+2,cy+6,art.paint);
                }
            }
            c.restoreToCount(layer);
        }
    }
    private void drawKeyboard(Canvas c,float x,float y){
        art.stroke(0xFF777777,2);c.drawRoundRect(x-10,y-7,x+10,y+7,2,2,art.paint);
        for(int row=0;row<2;row++)for(int col=0;col<3;col++)c.drawPoint(x-5+col*5,y-3+row*4,art.paint);
        c.drawLine(x-5,y+4,x+5,y+4,art.paint);
    }
    private void drawAction(Canvas c){
        boolean enabled=actionEnabled();float y=actionY(),offset=pressed==200&&enabled?4:0;
        art.round(c,16,y+6,328,54,15,enabled?0xFF46A302:0xFFD7D7D7);
        art.round(c,16,y+offset,328,54,15,enabled?DakonArtwork.GREEN:0xFFE5E5E5);
        art.text(c,actionLabel(),180,y+34+offset,17,850,enabled?DakonArtwork.INK:0xFFAFAFAF,true);
    }

    private int hit(float x,float y){
        if(x>=4&&x<52&&y>=3&&y<51)return 201;
        if(x>=305&&x<353&&y>=3&&y<51)return 202;
        if(x>=16&&x<=344&&y>=actionY()&&y<=actionY()+60)return 200;
        if(session.getStage()>=DakonSession.NAMBAHI)return 0;
        int tool=BoardGeometry.hitTool(x,y);if(tool>=0)return 100+tool;
        return BoardGeometry.hitHole(x,y);
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        float x=event.getX()/scale(),y=event.getY()/scale();
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN){pressed=hit(x,y);invalidate();return true;}
        if(event.getActionMasked()==MotionEvent.ACTION_CANCEL){pressed=-1;invalidate();return true;}
        if(event.getActionMasked()==MotionEvent.ACTION_MOVE){if(hit(x,y)!=pressed){pressed=-1;invalidate();}return true;}
        if(event.getActionMasked()==MotionEvent.ACTION_UP){int id=hit(x,y),down=pressed;pressed=-1;invalidate();if(id==down&&id>0){performClick();perform(id);}else if(id==0&&session.getStage()==0&&x>=16&&x<=344&&y>=199&&y<=517){bubble="Papan";invalidate();}return true;}
        return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
    private boolean perform(int id){
        if(id==201){host.onExit();return true;}
        if(id==203){host.onRequestAnswer();return session.getStage()==DakonSession.NAMBAHI;}
        if(id==202){host.onSoundToggle();announceForAccessibility(host.isSoundEnabled()?"Efek suara aktif":"Efek suara mati");return true;}
        if(id==200){
            if(!actionEnabled())return false;
            movingHole=0;
            if(session.getStage()==DakonSession.NAMBAHI&&!host.worksheet().isCorrect()){
                host.onCheckWorksheet();return true;
            }else{session.advance();host.playTap(false);bubble="Biji";}
            changed();return true;
        }
        if(isToolId(id)){
            int tool=id-100;if(!toolEnabled(tool))return false;
            movingHole=0;boolean result;
            if(tool==4)result=session.undo();else if(tool==5)result=session.clearBoard();else result=session.setTool(tool);
            if(session.getStage()==0)bubble=tool<2?label(id):"Biji";
            if(result){host.playTap(false);changed();}
            return result;
        }
        if(id>=1&&id<=24&&session.getStage()<DakonSession.NAMBAHI){
            int before=session.getSeeds(id).length,source=session.getMoveSource();
            boolean result=session.tapHole(id);
            if(session.getStage()==0)bubble="Lubang "+id;
            if(result){
                int[] seeds=session.getSeeds(id);
                if(seeds.length>before){
                    movingHole=id;movingKind=seeds[seeds.length-1];movingAt=SystemClock.uptimeMillis();
                    movingStartX=source>0?BoardGeometry.centerX(source):40+movingKind*56;
                    movingStartY=source>0?BoardGeometry.centerY(source):558;
                }
                host.playTap(false);performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);changed();
            }else if(session.getStage()==DakonSession.NIROKKE&&!session.isGuideComplete()){
                announceForAccessibility("Ikuti lubang "+session.getExpectedHole());invalidate();
            }else invalidate();
            return result;
        }
        return false;
    }
    private void changed(){
        host.onSessionChanged();invalidate();sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
    }
    private RectF bounds(int id){
        if(id>=1&&id<=24){float x=BoardGeometry.centerX(id),y=BoardGeometry.centerY(id);return new RectF(x-27,y-24,x+27,y+44);}
        if(isToolId(id)){float x=16+(id-100)*56;return new RectF(x,534,x+48,582);}
        if(id==200)return new RectF(16,actionY(),344,actionY()+60);
        if(id==201)return new RectF(4,3,52,51);
        if(id==203)return new RectF(16,591,344,645);
        return new RectF(305,3,353,51);
    }
    private String label(int id){
        if(id>=1&&id<=24){
            int blue=0,coral=0;for(int kind:session.getSeeds(id))if(kind==0)blue++;else coral++;
            return "Lubang "+id+", "+(blue+coral==0?"kosong":blue+" biji biru, "+coral+" biji koral");
        }
        if(id==100)return "Biji biru";
        if(id==101)return "Biji koral";
        if(id==104)return "Urungkan";
        if(id==105)return "Kosongkan papan";
        if(id==200)return actionLabel();
        if(id==201)return "Simpan dan keluar";
        if(id==203)return "Isi jawaban";
        return host.isSoundEnabled()?"Matikan efek suara":"Aktifkan efek suara";
    }
    private boolean enabled(int id){
        if(session.getStage()>=DakonSession.NAMBAHI&&((id>=1&&id<=24)||isToolId(id)))return false;
        if(id>=100&&id<106)return isToolId(id)&&toolEnabled(id-100);
        if(id==200)return actionEnabled();
        if(id==203)return session.getStage()==DakonSession.NAMBAHI;
        return !(id>=1&&id<=24&&session.getStage()==DakonSession.COMPLETE);
    }
    @Override public AccessibilityNodeProvider getAccessibilityNodeProvider(){return provider;}
    private final AccessibilityNodeProvider provider=new AccessibilityNodeProvider(){
        @Override public AccessibilityNodeInfo createAccessibilityNodeInfo(int id){
            if(id==View.NO_ID){
                AccessibilityNodeInfo node=AccessibilityNodeInfo.obtain(DakonView.this);
                onInitializeAccessibilityNodeInfo(node);
                if(session.getStage()<DakonSession.NAMBAHI){
                    for(int i=1;i<=24;i++)node.addChild(DakonView.this,i);
                    for(int tool:TOOL_IDS)node.addChild(DakonView.this,100+tool);
                }
                for(int i=200;i<=202;i++)node.addChild(DakonView.this,i);
                return node;
            }
            if(!isVirtualId(id))return null;
            AccessibilityNodeInfo node=AccessibilityNodeInfo.obtain();
            node.setSource(DakonView.this,id);node.setParent(DakonView.this);
            node.setPackageName(getContext().getPackageName());node.setClassName("android.widget.Button");
            node.setContentDescription(label(id));node.setFocusable(true);node.setClickable(true);node.setEnabled(enabled(id));
            node.setSelected((id==100||id==101)&&id-100==session.getTool());
            node.setAccessibilityFocused(accessibilityFocus==id);
            RectF b=bounds(id);float factor=scale();
            Rect rect=new Rect(Math.round(b.left*factor),Math.round(b.top*factor),Math.round(b.right*factor),Math.round(b.bottom*factor));
            node.setBoundsInParent(rect);int[] location=new int[2];getLocationOnScreen(location);rect.offset(location[0],location[1]);node.setBoundsInScreen(rect);
            node.setVisibleToUser(isShown());
            node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK);
            node.addAction(accessibilityFocus==id?AccessibilityNodeInfo.AccessibilityAction.ACTION_CLEAR_ACCESSIBILITY_FOCUS:AccessibilityNodeInfo.AccessibilityAction.ACTION_ACCESSIBILITY_FOCUS);
            return node;
        }
        @Override public boolean performAction(int id,int action,Bundle arguments){
            if(!isVirtualId(id))return false;
            if(action==AccessibilityNodeInfo.ACTION_CLICK)return perform(id);
            if(action==AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS){accessibilityFocus=id;accessibilityEvent(id,AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);invalidate();return true;}
            if(action==AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS){accessibilityFocus=-1;invalidate();return true;}
            return false;
        }
    };
    private void accessibilityEvent(int id,int type){
        if(getParent()==null)return;
        AccessibilityEvent event=AccessibilityEvent.obtain(type);event.setPackageName(getContext().getPackageName());event.setSource(this,id);event.setContentDescription(label(id));
        getParent().requestSendAccessibilityEvent(this,event);
    }
    @Override public boolean dispatchHoverEvent(MotionEvent event){
        AccessibilityManager manager=(AccessibilityManager)getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        if(manager.isTouchExplorationEnabled()){
            int id=event.getActionMasked()==MotionEvent.ACTION_HOVER_EXIT?-1:hit(event.getX()/scale(),event.getY()/scale());
            if(id!=hovered){if(hovered>0)accessibilityEvent(hovered,AccessibilityEvent.TYPE_VIEW_HOVER_EXIT);hovered=id;if(id>0)accessibilityEvent(id,AccessibilityEvent.TYPE_VIEW_HOVER_ENTER);}
            return true;
        }
        return super.dispatchHoverEvent(event);
    }
    @Override public boolean onKeyDown(int keyCode,KeyEvent event){
        if(session.getStage()>=DakonSession.NAMBAHI){
            if(keyCode==KeyEvent.KEYCODE_DPAD_CENTER||keyCode==KeyEvent.KEYCODE_ENTER){
                if(keyboardFocus==203)host.onRequestAnswer();else perform(keyboardFocus>=200?keyboardFocus:200);return true;
            }
            if(keyCode==KeyEvent.KEYCODE_TAB){
                if(keyboardFocus==203)keyboardFocus=200;
                else if(keyboardFocus==200)keyboardFocus=201;
                else if(keyboardFocus==201)keyboardFocus=202;
                else keyboardFocus=session.getStage()==DakonSession.NAMBAHI?203:200;
            }else if(keyCode==KeyEvent.KEYCODE_DPAD_DOWN)keyboardFocus=session.getStage()==DakonSession.NAMBAHI&&keyboardFocus!=203?203:200;
            else if(keyCode==KeyEvent.KEYCODE_DPAD_UP)keyboardFocus=201;
            else if(keyCode==KeyEvent.KEYCODE_DPAD_LEFT)keyboardFocus=201;
            else if(keyCode==KeyEvent.KEYCODE_DPAD_RIGHT)keyboardFocus=202;
            else return super.onKeyDown(keyCode,event);
            accessibilityFocus=keyboardFocus;invalidate();return true;
        }
        if(keyCode==KeyEvent.KEYCODE_DPAD_CENTER||keyCode==KeyEvent.KEYCODE_ENTER){perform(keyboardFocus);return true;}
        if(keyCode==KeyEvent.KEYCODE_TAB){
            int[] ids={201,202,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,100,101,104,105,203,200};
            int index=0;for(int i=0;i<ids.length;i++)if(ids[i]==keyboardFocus)index=i;
            int direction=event.isShiftPressed()?-1:1;
            for(int i=0;i<ids.length;i++){index=(index+direction+ids.length)%ids.length;if(enabled(ids[index])){keyboardFocus=ids[index];break;}}
        }
        else if(keyCode==KeyEvent.KEYCODE_DPAD_RIGHT){
            if(keyboardFocus>=1&&keyboardFocus<=24)keyboardFocus=Math.min(24,keyboardFocus+1);
            else if(isToolId(keyboardFocus))keyboardFocus=keyboardFocus==101?104:Math.min(105,keyboardFocus+1);
            else if(keyboardFocus==201)keyboardFocus=202;
        }
        else if(keyCode==KeyEvent.KEYCODE_DPAD_LEFT){
            if(keyboardFocus>=1&&keyboardFocus<=24)keyboardFocus=Math.max(1,keyboardFocus-1);
            else if(isToolId(keyboardFocus))keyboardFocus=keyboardFocus==104?101:Math.max(100,keyboardFocus-1);
            else if(keyboardFocus==202)keyboardFocus=201;
        }
        else if(keyCode==KeyEvent.KEYCODE_DPAD_DOWN){
            if(keyboardFocus>=1&&keyboardFocus<=18)keyboardFocus+=6;
            else if(keyboardFocus>=19&&keyboardFocus<=24){
                int tool=keyboardFocus-19;
                keyboardFocus=100+(tool==2?1:tool==3?4:tool);
            }
            else if(isToolId(keyboardFocus))keyboardFocus=session.getStage()==DakonSession.NAMBAHI?203:200;
            else if(keyboardFocus==203)keyboardFocus=200;
            else if(keyboardFocus==201||keyboardFocus==202)keyboardFocus=keyboardFocus==201?1:6;
        }
        else if(keyCode==KeyEvent.KEYCODE_DPAD_UP){
            if(keyboardFocus>=7&&keyboardFocus<=24)keyboardFocus-=6;
            else if(keyboardFocus>=1&&keyboardFocus<=6)keyboardFocus=keyboardFocus<=3?201:202;
            else if(isToolId(keyboardFocus))keyboardFocus=19+keyboardFocus-100;
            else if(keyboardFocus==200)keyboardFocus=session.getStage()==DakonSession.NAMBAHI?203:100;
            else if(keyboardFocus==203)keyboardFocus=100;
        }
        else return super.onKeyDown(keyCode,event);
        accessibilityFocus=keyboardFocus;invalidate();return true;
    }
}
