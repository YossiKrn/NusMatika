package id.nusmatika.dakon;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import java.util.Locale;

/** The race uses simulation seconds, never the learner's time spent answering. */
public final class BalapKarungView extends View {
    public interface Host {
        void onSessionChanged();void onExit();void onSoundToggle();
        void onRequestInput(boolean working);
        boolean isSoundEnabled();boolean isReducedMotion();void playTap(boolean correct);
    }
    private final BalapKarungSession session;
    private final Host host;
    private final NaraRenderer nara;
    private final DakonArtwork art;
    private final JkwTriangleArtwork triangle;
    private static final RectF INTRO_TRIANGLE=new RectF(16,142,344,558),EXAMPLE_TRIANGLE=new RectF(16,200,344,558);
    private static final RectF INLINE_TRIANGLE=new RectF(252,48,344,148);
    private static final int TRIANGLE=207;
    private final long epoch=SystemClock.uptimeMillis();
    private long tick=epoch;
    private double seconds;
    private boolean playing,running=true;
    private long tutorialMillis;
    private boolean tutorialReadyAnnounced;
    private int selected=2,pressed=-1,accessibilityFocus=-1,hovered=-1,keyboardFocus=203;
    private static final int ACTION=200,EXIT=201,SOUND=202,PLAY=203,REPLAY=204;
    private static final float FIELD_TOP=212,FIELD_HEIGHT=320,FIRST_GROUND=270,LANE_HEIGHT=80;

    public BalapKarungView(Context context,BalapKarungSession session,NaraRenderer nara,Typeface font,Host host){
        super(context);this.session=session;this.nara=nara;this.host=host;art=new DakonArtwork(context,font);triangle=new JkwTriangleArtwork(context,font);
        keyboardFocus=session.isTriangleTutorial()?ACTION:session.getStage()==0?PLAY:ACTION;
        setBackgroundColor(Color.WHITE);setFocusable(true);setFocusableInTouchMode(true);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }
    private float scale(){return getWidth()/360f;}
    private float actionY(){return Math.max(668,getHeight()/Math.max(.01f,scale())-72);}
    private boolean guided(){return session.getStage()==BalapKarungSession.NIROKKE;}
    private boolean correct(){return session.getFeedback()==BalapKarungSession.Feedback.CORRECT;}
    private double heroDuration(){return guided()?session.getQuestion().seconds:12;}
    private double duration(){return guided()?session.getQuestion().seconds*2.0:12;}
    public void setRunning(boolean active){
        updateClock();running=active;tick=SystemClock.uptimeMillis();
        if(active&&guided()&&correct()&&seconds<heroDuration())playing=true;
        if(active)invalidate();
    }
    public void onEdited(){if(guided()&&!correct()){playing=false;seconds=0;}invalidate();}
    public void focusAction(){keyboardFocus=ACTION;invalidate();}
    private void resetRace(){playing=false;seconds=0;selected=2;tick=SystemClock.uptimeMillis();accessibilityFocus=-1;
        tutorialMillis=0;tutorialReadyAnnounced=false;
        keyboardFocus=session.isTriangleTutorial()?ACTION:session.getStage()==0?PLAY:ACTION;}
    private JkwTutorialMotion.Frame tutorialFrame(){return JkwTutorialMotion.at(session.getTriangleStep(),tutorialMillis,host.isReducedMotion(),session.triangleInstruction());}
    private void updateClock(){
        long now=SystemClock.uptimeMillis();
        if(running){
            long delta=Math.max(0,now-tick);
            if(session.isTriangleTutorial())tutorialMillis+=delta;
            if(playing){seconds=Math.min(duration(),seconds+delta/1000.0);if(seconds>=duration())playing=false;}
        }
        tick=now;
    }
    private void playSolvedRace(){
        seconds=0;tick=SystemClock.uptimeMillis();selected=2;
        if(host.isReducedMotion()){seconds=heroDuration();playing=false;}else playing=true;
    }
    private boolean actionEnabled(){
        int stage=session.getStage();
        if(session.isTriangleTutorial())return tutorialFrame().ready;
        if(stage==BalapKarungSession.NITENI||stage==BalapKarungSession.COMPLETE)return true;
        if(correct())return !guided()||seconds>=heroDuration();
        return session.canSubmit();
    }
    private String actionLabel(){
        int stage=session.getStage();
        if(stage==BalapKarungSession.COMPLETE)return session.getLevel()>=0?"KEMBALI KE JALUR":"MAIN LAGI";
        if(session.isTriangleTutorial())return session.getTriangleExampleIndex()==2?"LIHAT BALAPAN":"LANJUT";
        if(stage==BalapKarungSession.NITENI)return "SELESAI";
        if(stage==BalapKarungSession.NITENI||correct())return "LANJUT";
        return "PERIKSA";
    }
    private String number(double value){
        if(Math.abs(value-Math.rint(value))<.00001)return Integer.toString((int)Math.rint(value));
        return String.format(Locale.ROOT,"%.1f",value).replace('.',',');
    }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);if(getWidth()==0)return;
        updateClock();c.save();c.scale(scale(),scale());
        int stage=session.getStage();long elapsed=SystemClock.uptimeMillis()-epoch;
        boolean reduced=host.isReducedMotion();
        drawHeader(c);
        int namedStage=session.isLevelComplete()?session.getLevel():stage;
        String stageName=namedStage==0?"Niteni":namedStage==1?"Nirokke":namedStage==2?"Nambahi":"Balap Karung";
        if(stage==1||stage==2)stageName+=" · "+(session.getIndex()+1)+"/3";
        if(session.isTriangleTutorial())stageName+=session.isTriangleLineIntro()?" · Garis "+(session.getTriangleStep()+1)+"/2":" · Contoh "+(session.getTriangleExampleIndex()+1)+"/3";
        art.text(c,stageName,16,76,14,800,0xFF777777,false);
        String title=session.isTriangleTutorial()?session.triangleTitle():stage==0?"Kenali balap karung":stage==2?"Caramu sendiri":stage==3?"Selesai!":
                session.getQuestion().unknown==BalapKarungSession.Quantity.DISTANCE?"Cari jarak":session.getQuestion().unknown==BalapKarungSession.Quantity.SPEED?"Cari kecepatan":"Cari waktu";
        art.text(c,title,16,112,stage==3?27:24,850,DakonArtwork.INK,false);
        if(stage==1||stage==2)triangle.drawCompact(c,INLINE_TRIANGLE,session.triangleQuestionQuantity());
        if(stage==3){
            nara.draw(c,new RectF(74,190,286,450),elapsed,reduced);
            if(session.isLevelComplete()){
                art.text(c,"Level "+(session.getLevel()+1)+" selesai",180,487,22,850,DakonArtwork.INK,true);
                art.text(c,stageName+" tuntas!",180,521,17,800,0xFF3C8A00,true);
                art.text(c,session.getLevel()<2?"Tahap berikutnya terbuka di jalur.":"Semua tahap Balap Karung tuntas.",180,557,14,700,0xFF777777,true);
            }
        }else if(session.isTriangleTutorial()){
            drawTutorial(c);
        }else{
            nara.draw(c,new RectF(19,126,161,198),elapsed,reduced);
            if(stage==0){
                art.text(c,BalapKarungSession.racerName(selected),174,146,17,800,DakonArtwork.INK,false);
                art.text(c,number(Math.min(seconds,BalapKarungSession.raceFinishSeconds(selected)))+" detik",174,176,21,850,DakonArtwork.INK,false);
            }else if(stage==1){
                String formula=session.getQuestion().formula();
                art.text(c,formula,157,161,12.5f,800,0xFF777777,false);
                art.text(c,session.getQuestion().calculation()+" = ?",157,188,21,850,DakonArtwork.INK,false);
            }
            if(stage<2){drawTrack(c,elapsed,reduced);drawData(c);}
            if(stage==0){
                smallButton(c,PLAY,16,610,156,48,playing?"JEDA":seconds>=duration()?"MULAI LAGI":"MULAI");
                smallButton(c,REPLAY,188,610,156,48,"ULANGI");
            }else if(stage==1){
                drawAnswerBox(c);
                if(session.getFeedback()==BalapKarungSession.Feedback.INCORRECT)art.text(c,"Coba hitung lagi.",157,205,12,800,0xFF777777,false);
                if(correct())art.text(c,"Benar!",157,205,12,800,0xFF3C8A00,false);
            }else{
                drawStory(c,session.getQuestion().story,16,235,328);
                art.text(c,"Caramu menghitung",16,337,16,800,DakonArtwork.INK,false);
                art.round(c,16,352,328,182,15,0xFFF7F7F7);art.stroke(0xFFE5E5E5,2);c.drawRoundRect(16,352,344,534,15,15,art.paint);
                art.text(c,"Jawaban",16,583,16,800,DakonArtwork.INK,false);drawAnswerBox(c);
                if(correct())art.text(c,"Benar!",157,166,15,800,0xFF3C8A00,false);
                else if(session.getFeedback()==BalapKarungSession.Feedback.INCORRECT)art.text(c,"Coba periksa hitunganmu.",151,166,13,800,DakonArtwork.INK,false);
            }
        }
        boolean enabled=actionEnabled();float y=actionY(),press=pressed==ACTION&&enabled?4:0;
        art.round(c,16,y+6,328,54,15,enabled?0xFF46A302:0xFFD7D7D7);
        art.round(c,16,y+press,328,54,15,enabled?DakonArtwork.GREEN:0xFFE5E5E5);
        art.text(c,actionLabel(),180,y+34+press,17,850,enabled?DakonArtwork.INK:0xFFAFAFAF,true);
        if(accessibilityFocus>0){art.stroke(DakonArtwork.BLUE,2);c.drawRoundRect(bounds(accessibilityFocus),6,6,art.paint);}
        c.restore();
        if(running&&isShown()){
            if(session.isTriangleTutorial()&&!reduced)postInvalidateOnAnimation();
            else if(!reduced||playing)postInvalidateDelayed(playing?33:83);
        }
    }
    private void drawTutorial(Canvas c){
        JkwTutorialMotion.Frame frame=tutorialFrame();
        if(!session.isTriangleLineIntro())wrapped(c,session.getTriangleExample().story,16,151,328,16,22,750);
        triangle.drawAnimated(c,session.isTriangleLineIntro()?INTRO_TRIANGLE:EXAMPLE_TRIANGLE,session.triangleQuestionQuantity(),frame);
        typed(c,session.triangleInstruction(),frame.visibleChars,16,580,16,22,800);
        if(frame.calculationAlpha>0){int layer=frame.calculationAlpha<1?c.saveLayerAlpha(new RectF(14,608,346,662),Math.round(255*frame.calculationAlpha)):c.save();String calculation=session.triangleCalculation();typed(c,calculation,calculation.length(),16,628,15,22,850);c.restoreToCount(layer);}
        if(frame.ready&&!tutorialReadyAnnounced){tutorialReadyAnnounced=true;sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);}
    }
    private void typed(Canvas c,String text,int visible,float x,float baseline,float size,float spacing,int weight){
        int remaining=visible;
        // Explicit full-message line breaks keep the layout fixed while letters appear.
        for(String line:text.split("\\n",-1)){
            int end=Math.max(0,Math.min(line.length(),remaining));
            if(end>0)art.text(c,line.substring(0,end),x,baseline,size,weight,DakonArtwork.INK,false);
            remaining-=line.length()+1;baseline+=spacing;
        }
    }
    private void wrapped(Canvas c,String text,float x,float baseline,float width,float size,float spacing,int weight){
        art.text(c,"",x,baseline,size,weight,DakonArtwork.INK,false);String line="";float y=baseline;
        for(String word:text.split(" +")){
            String next=line.isEmpty()?word:line+" "+word;
            if(!line.isEmpty()&&art.paint.measureText(next)>width){art.text(c,line,x,y,size,weight,DakonArtwork.INK,false);y+=spacing;line=word;}
            else line=next;
        }
        if(!line.isEmpty())art.text(c,line,x,y,size,weight,DakonArtwork.INK,false);
    }
    private void drawHeader(Canvas c){
        art.stroke(0xFFAFAFAF,2.5f);c.drawLine(21,20,35,34,art.paint);c.drawLine(35,20,21,34,art.paint);
        art.round(c,56,20,240,14,7,0xFFE5E5E5);
        float progress=session.getStage()==0?60:session.getStage()==1?90+session.getIndex()*20:session.getStage()==2?164+session.getIndex()*24:240;
        if(session.getLevel()==0&&session.getStage()==0)progress=240*session.getTriangleStep()/6f;
        if(progress>0){art.round(c,56,20,progress,14,7,DakonArtwork.GREEN);if(progress>14)art.round(c,62,23,progress-14,4,2,0xFF89E219);}
        art.stroke(0xFFAFAFAF,2.5f);Path p=new Path();p.moveTo(321,23);p.lineTo(325,23);p.lineTo(331,18);p.lineTo(331,36);p.lineTo(325,31);p.lineTo(321,31);p.close();c.drawPath(p,art.paint);
        if(host.isSoundEnabled()){p.reset();p.moveTo(336,22);p.cubicTo(341,25,341,29,336,32);c.drawPath(p,art.paint);}
        else{c.drawLine(336,23,342,31,art.paint);c.drawLine(342,23,336,31,art.paint);}
    }
    private void drawTrack(Canvas c,long elapsed,boolean reduced){
        art.round(c,16,FIELD_TOP+6,328,FIELD_HEIGHT,24,0xFFD4AA73);
        art.round(c,16,FIELD_TOP,328,FIELD_HEIGHT,24,0xFFF5D7A8);
        // Sparse, fixed pebbles keep the ground flat and illustrated rather than noisy.
        for(int lane=0;lane<4;lane++){
            float ground=FIRST_GROUND+lane*LANE_HEIGHT;
            for(int stone=0;stone<4;stone++){
                float x=112+stone*52+((lane+stone)%2)*7;
                float y=ground-34+(stone%2)*16;
                art.round(c,x,y,stone%2==0?4:3,2,1,0xFFE6BD85);
            }
            if(lane<3){art.stroke(0xFFFFF4DE,2);c.drawLine(30,ground+21,330,ground+21,art.paint);}
        }
        art.stroke(0xFFC29761,2);art.paint.setPathEffect(new DashPathEffect(new float[]{4,5},0));c.drawLine(52,226,52,514,art.paint);art.paint.setPathEffect(null);
        for(int row=0;row<24;row++)for(int col=0;col<2;col++)art.round(c,309+col*5,226+row*12,5,12,0,(row+col)%2==0?0xFF9A714C:0xFFFFF8EB);
        for(int i=0;i<4;i++){
            float ground=FIRST_GROUND+i*LANE_HEIGHT;
            double distance=guided()?session.getQuestion().distance:BalapKarungSession.RACE_DISTANCE;
            double speed=guided()?session.getQuestion().speed*BalapKarungSession.raceSpeed(i)/2.0:BalapKarungSession.raceSpeed(i);
            double position=Math.min(distance,seconds*speed);
            float x=52+260*(float)(position/distance);
            float phase=playing?(float)((seconds*2.4+i*.17)%1):0;
            if(position>=distance)phase=0;
            SackRacerArtwork.draw(c,i,x,ground,.64f,phase,reduced);
            art.text(c,BalapKarungSession.racerName(i),33,ground+15,11.5f,800,i==selected?DakonArtwork.INK:0xFF795B3D,false);
            if(position>=distance)art.text(c,number(distance/speed)+" detik",274,ground+15,11.5f,800,0xFF795B3D,true);
        }
    }
    private void drawData(Canvas c){
        boolean reveal=!guided()||correct();BalapKarungSession.Question q=session.getQuestion();
        String[] labels={"Jarak","Waktu","Kecepatan"};
        String[] values;
        if(!guided())values=new String[]{"12 m",number(BalapKarungSession.raceFinishSeconds(selected))+" detik",number(BalapKarungSession.raceSpeed(selected))+" m/detik"};
        else values=new String[]{!reveal&&q.unknown==BalapKarungSession.Quantity.DISTANCE?"?":q.distance+" m",!reveal&&q.unknown==BalapKarungSession.Quantity.TIME?"?":q.seconds+" detik",!reveal&&q.unknown==BalapKarungSession.Quantity.SPEED?"?":q.speed+" m/detik"};
        for(int i=0;i<3;i++){
            float x=16+i*112;art.round(c,x,554,104,43,12,0xFFF7F7F7);
            art.text(c,labels[i],x+52,568,10.5f,800,0xFF777777,true);art.text(c,values[i],x+52,590,16,800,DakonArtwork.INK,true);
        }
    }
    private void drawAnswerBox(Canvas c){
        int border=session.getFeedback()==BalapKarungSession.Feedback.INCORRECT?DakonArtwork.CORAL:correct()?DakonArtwork.GREEN:0xFFE5E5E5;
        art.round(c,16,605,328,54,15,0xFFF7F7F7);art.stroke(border,2);c.drawRoundRect(16,605,344,659,15,15,art.paint);
        art.text(c,session.getQuestion().unit(),294,639,15,800,0xFF777777,true);
    }
    private void drawStory(Canvas c,String story,float x,float baseline,float width){
        String line="";float y=baseline;art.paint.setTextSize(18);
        for(String word:story.split(" ")){
            String next=line.isEmpty()?word:line+" "+word;
            if(art.paint.measureText(next)>width&&!line.isEmpty()){art.text(c,line,x,y,18,700,DakonArtwork.INK,false);y+=25;line=word;}
            else line=next;
        }
        if(!line.isEmpty())art.text(c,line,x,y,18,700,DakonArtwork.INK,false);
    }
    private void smallButton(Canvas c,int id,float x,float y,float w,float h,String label){
        float down=pressed==id?2:0;art.round(c,x,y+down,w,h,13,Color.WHITE);
        art.stroke(0xFFE5E5E5,2);c.drawRoundRect(x,y+down,x+w,y+h+down,13,13,art.paint);art.text(c,label,x+w/2,y+30+down,14,850,DakonArtwork.INK,true);
    }
    private int hit(float x,float y){
        if(x>=4&&x<=52&&y>=3&&y<=51)return EXIT;
        if(x>=305&&x<=353&&y>=3&&y<=51)return SOUND;
        if(x>=16&&x<=344&&y>=actionY()&&y<=actionY()+60)return ACTION;
        if((guided()||session.getStage()==2)&&INLINE_TRIANGLE.contains(x,y))return TRIANGLE;
        if(session.isTriangleTutorial())return -1;
        if(session.getStage()==0){
            if(y>=610&&y<=658){if(x>=16&&x<=172)return PLAY;if(x>=188&&x<=344)return REPLAY;}
            if(x>=16&&x<=344&&y>=FIELD_TOP&&y<FIELD_TOP+FIELD_HEIGHT)return 10+Math.max(0,Math.min(3,(int)((y-FIELD_TOP)/LANE_HEIGHT)));
        }
        return -1;
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        float x=event.getX()/scale(),y=event.getY()/scale();int id=hit(x,y);
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN){pressed=id;invalidate();return true;}
        if(event.getActionMasked()==MotionEvent.ACTION_MOVE){if(pressed!=id)pressed=-1;invalidate();return true;}
        if(event.getActionMasked()==MotionEvent.ACTION_CANCEL){pressed=-1;invalidate();return true;}
        if(event.getActionMasked()==MotionEvent.ACTION_UP){int old=pressed;pressed=-1;if(id==old&&id>0&&id!=TRIANGLE){performClick();activate(id);}invalidate();return true;}
        return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
    private boolean activate(int id){
        if(id==205&&(guided()||session.getStage()==2)){host.onRequestInput(false);return true;}
        if(id==206&&session.getStage()==2){host.onRequestInput(true);return true;}
        if(id==EXIT){host.onExit();return true;}if(id==SOUND){host.onSoundToggle();return true;}
        if(session.getStage()==0){
            if(id>=10&&id<14){selected=id-10;invalidate();sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);return true;}
            if(id==REPLAY){resetRace();invalidate();return true;}
            if(id==PLAY){
                updateClock();if(playing)playing=false;else{if(seconds>=duration())seconds=0;playing=true;tick=SystemClock.uptimeMillis();if(host.isReducedMotion()){seconds=duration();playing=false;}}
                invalidate();return true;
            }
        }
        if(id==ACTION&&actionEnabled()){
            int stage=session.getStage();
            if(session.isLevelComplete()){host.onExit();return true;}
            if(stage==0||stage==3||correct()){session.advance();resetRace();host.onSessionChanged();}
            else{
                BalapKarungSession.Feedback result=session.submitAnswer();host.onSessionChanged();host.playTap(result==BalapKarungSession.Feedback.CORRECT);
                if(result==BalapKarungSession.Feedback.CORRECT&&guided())playSolvedRace();
                announceForAccessibility(result==BalapKarungSession.Feedback.CORRECT?"Benar.":"Coba periksa hitunganmu.");
            }
            sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);invalidate();return true;
        }
        return false;
    }
    private RectF bounds(int id){
        if(id==TRIANGLE)return new RectF(INLINE_TRIANGLE);
        if(id==ACTION)return new RectF(16,actionY(),344,actionY()+60);
        if(id==EXIT)return new RectF(4,3,52,51);if(id==SOUND)return new RectF(305,3,353,51);
        if(id==PLAY)return new RectF(16,610,172,658);if(id==REPLAY)return new RectF(188,610,344,658);
        if(id==205)return new RectF(16,605,252,659);if(id==206)return new RectF(16,352,344,534);
        return new RectF(16,FIELD_TOP+(id-10)*LANE_HEIGHT,344,FIELD_TOP+(id-9)*LANE_HEIGHT);
    }
    private String label(int id){
        if(id==TRIANGLE)return JkwTriangleArtwork.description(session.triangleQuestionQuantity());
        if(id==ACTION)return actionLabel();if(id==EXIT)return "Simpan dan kembali";if(id==SOUND)return host.isSoundEnabled()?"Matikan efek suara":"Aktifkan efek suara";
        if(id==PLAY)return playing?"Jeda balapan":"Mulai balapan";if(id==REPLAY)return "Ulangi balapan";
        int racer=id-10;return BalapKarungSession.racerName(racer)+", jarak 12 meter, waktu "+number(BalapKarungSession.raceFinishSeconds(racer))+" detik, kecepatan "+number(BalapKarungSession.raceSpeed(racer))+" meter per detik";
    }
    private boolean validId(int id){
        if(id==ACTION||id==EXIT||id==SOUND)return true;
        if(id==TRIANGLE)return guided()||session.getStage()==2;
        if(session.isTriangleTutorial())return false;
        return session.getStage()==0&&(id==PLAY||id==REPLAY||id>=10&&id<14);
    }
    @Override public AccessibilityNodeProvider getAccessibilityNodeProvider(){return provider;}
    private final AccessibilityNodeProvider provider=new AccessibilityNodeProvider(){
        @Override public AccessibilityNodeInfo createAccessibilityNodeInfo(int id){
            if(id==View.NO_ID){
                AccessibilityNodeInfo root=AccessibilityNodeInfo.obtain(BalapKarungView.this);onInitializeAccessibilityNodeInfo(root);
                root.addChild(BalapKarungView.this,EXIT);root.addChild(BalapKarungView.this,SOUND);
                if(guided()||session.getStage()==2)root.addChild(BalapKarungView.this,TRIANGLE);
                if(!session.isTriangleTutorial()&&session.getStage()==0){
                    for(int i=10;i<14;i++)root.addChild(BalapKarungView.this,i);
                    root.addChild(BalapKarungView.this,PLAY);root.addChild(BalapKarungView.this,REPLAY);
                }
                root.addChild(BalapKarungView.this,ACTION);return root;
            }
            if(!validId(id))return null;
            AccessibilityNodeInfo node=AccessibilityNodeInfo.obtain();node.setSource(BalapKarungView.this,id);node.setParent(BalapKarungView.this);node.setPackageName(getContext().getPackageName());node.setClassName(id==TRIANGLE?"android.widget.ImageView":"android.widget.Button");node.setContentDescription(label(id));node.setClickable(id!=TRIANGLE);node.setFocusable(true);node.setAccessibilityFocused(accessibilityFocus==id);node.setEnabled(id!=ACTION||actionEnabled());node.setVisibleToUser(isShown());
            RectF b=bounds(id);float s=scale();Rect r=new Rect(Math.round(b.left*s),Math.round(b.top*s),Math.round(b.right*s),Math.round(b.bottom*s));node.setBoundsInParent(r);int[] loc=new int[2];getLocationOnScreen(loc);r.offset(loc[0],loc[1]);node.setBoundsInScreen(r);if(id!=TRIANGLE)node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK);node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_ACCESSIBILITY_FOCUS);node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLEAR_ACCESSIBILITY_FOCUS);return node;
        }
        @Override public boolean performAction(int id,int action,Bundle arguments){
            if(!validId(id))return false;
            if(action==AccessibilityNodeInfo.ACTION_CLICK)return activate(id);
            if(action==AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS){
                accessibilityFocus=id;RectF b=bounds(id);float s=scale();requestRectangleOnScreen(new Rect(Math.round(b.left*s),Math.round(b.top*s),Math.round(b.right*s),Math.round(b.bottom*s)),false);
                accessibilityEvent(id,AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);invalidate();return true;
            }
            if(action==AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS){accessibilityFocus=-1;accessibilityEvent(id,AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);invalidate();return true;}
            return false;
        }
    };
    private void accessibilityEvent(int id,int type){
        if(getParent()==null)return;
        AccessibilityEvent event=AccessibilityEvent.obtain(type);event.setSource(this,id);event.setPackageName(getContext().getPackageName());event.setClassName(id==TRIANGLE?"android.widget.ImageView":"android.widget.Button");event.setContentDescription(label(id));
        getParent().requestSendAccessibilityEvent(this,event);
    }
    @Override public boolean dispatchHoverEvent(MotionEvent event){
        AccessibilityManager manager=(AccessibilityManager)getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        if(manager.isTouchExplorationEnabled()){
            int id=event.getActionMasked()==MotionEvent.ACTION_HOVER_EXIT?-1:hit(event.getX()/scale(),event.getY()/scale());
            if(id!=hovered){if(validId(hovered))accessibilityEvent(hovered,AccessibilityEvent.TYPE_VIEW_HOVER_EXIT);hovered=id;if(validId(id))accessibilityEvent(id,AccessibilityEvent.TYPE_VIEW_HOVER_ENTER);}
            return true;
        }
        return super.dispatchHoverEvent(event);
    }
    @Override public boolean onKeyDown(int code,KeyEvent event){
        if(code==KeyEvent.KEYCODE_ENTER||code==KeyEvent.KEYCODE_DPAD_CENTER){activate(keyboardFocus);return true;}
        int[] controls=session.isTriangleTutorial()?new int[]{EXIT,SOUND,ACTION}:session.getStage()==0?new int[]{EXIT,SOUND,10,11,12,13,PLAY,REPLAY,ACTION}:
                session.getStage()==2?new int[]{EXIT,SOUND,TRIANGLE,206,205,ACTION}:guided()?new int[]{EXIT,SOUND,TRIANGLE,205,ACTION}:new int[]{EXIT,SOUND,ACTION};
        int direction=code==KeyEvent.KEYCODE_TAB?(event.isShiftPressed()?-1:1):code==KeyEvent.KEYCODE_DPAD_LEFT||code==KeyEvent.KEYCODE_DPAD_UP?-1:code==KeyEvent.KEYCODE_DPAD_RIGHT||code==KeyEvent.KEYCODE_DPAD_DOWN?1:0;
        if(direction==0)return super.onKeyDown(code,event);
        int index=0;for(int i=0;i<controls.length;i++)if(controls[i]==keyboardFocus)index=i;
        int next=index+direction;
        keyboardFocus=controls[(next+controls.length)%controls.length];
        accessibilityFocus=keyboardFocus;invalidate();return true;
    }
}
