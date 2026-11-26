package id.nusmatika.dakon;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

/** Canvas artwork with real Android buttons for touch, keyboard and accessibility. */
final class PetakUmpetSceneView extends FrameLayout {
    interface Host { void onFind(int item);void onMiss();void onAssemblyReady();boolean reducedMotion(); }
    private static final long ASSEMBLY_DURATION=1100;
    private final PetakUmpetSession session;
    private final Host host;
    private final Surface surface;
    private final Button[] targets=new Button[PetakUmpetCatalog.ITEM_COUNT];
    private long animationMillis=ASSEMBLY_DURATION,tick=SystemClock.uptimeMillis(),naraMillis;
    private boolean running,readyReported=true;
    PetakUmpetSceneView(Context context,PetakUmpetSession session,Typeface font,NaraRenderer nara,Host host){
        super(context);this.session=session;this.host=host;
        surface=new Surface(context,font,nara);addView(surface);
        surface.setOnClickListener(view->{if(session.isSearch())host.onMiss();});
        for(int i=0;i<targets.length;i++){
            final int item=i;Button button=new Button(context);targets[i]=button;
            button.setBackgroundColor(Color.TRANSPARENT);button.setTextColor(Color.TRANSPARENT);
            button.setStateListAnimator(null);button.setElevation(0);button.setPadding(0,0,0,0);
            button.setMinWidth(0);button.setMinHeight(0);button.setMinimumWidth(0);button.setMinimumHeight(0);
            button.setId(7300+i);button.setOnClickListener(view->host.onFind(item));addView(button);
        }
        refresh();
    }
    void refresh(){
        for(int i=0;i<targets.length;i++){
            targets[i].setVisibility(session.isSearch()&&!session.isFound(i)?View.VISIBLE:View.GONE);
            if(session.getLevel()<2)targets[i].setContentDescription(
                    (session.showsMarkers()?PetakUmpetCatalog.piece(i).label():"Benda")+" "+PetakUmpetCatalog.location(session.getLevel(),i));
        }
        surface.setContentDescription(session.isComplete()?"Nara merayakan level selesai."
                :session.getLevel()==2?"Susunan berbentuk L. Hitung setiap bangun dari garis sambungannya."
                :"Halaman sekolah. Cari bangun berbentuk kubus dan balok.");
        requestLayout();surface.invalidate();
    }
    void startAssembly(){animationMillis=host.reducedMotion()?ASSEMBLY_DURATION:0;readyReported=false;tick=SystemClock.uptimeMillis();surface.invalidate();}
    boolean isAssemblyReady(){return host.reducedMotion()||animationMillis>=ASSEMBLY_DURATION;}
    void setRunning(boolean active){updateClock();running=active;tick=SystemClock.uptimeMillis();if(active)surface.invalidate();}
    private void updateClock(){long now=SystemClock.uptimeMillis();if(running){long elapsed=Math.max(0,now-tick);naraMillis+=elapsed;if(session.isAssembled())animationMillis=Math.min(ASSEMBLY_DURATION,animationMillis+elapsed);}tick=now;}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int w=MeasureSpec.getSize(widthSpec);float scale=w/360f;
        int h=Math.round((session.isComplete()?240:session.getLevel()==2?270:310)*scale);
        setMeasuredDimension(w,h);surface.measure(MeasureSpec.makeMeasureSpec(w,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(h,MeasureSpec.EXACTLY));
        int minTouch=Math.round(48*getResources().getDisplayMetrics().density);
        for(int i=0;i<targets.length;i++){
            if(session.getLevel()==2)continue;
            PetakUmpetCatalog.Bounds b=PetakUmpetCatalog.sceneBounds(session.getLevel(),i);
            int tw=Math.max(minTouch,Math.round((b.right-b.left+8)*scale));
            int th=Math.max(minTouch,Math.round((b.bottom-b.top+(session.showsMarkers()?34:8))*scale));
            targets[i].measure(MeasureSpec.makeMeasureSpec(tw,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(th,MeasureSpec.EXACTLY));
        }
    }
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){
        surface.layout(0,0,r-l,b-t);if(session.getLevel()==2)return;float scale=(r-l)/360f;
        for(int i=0;i<targets.length;i++){
            PetakUmpetCatalog.Bounds box=PetakUmpetCatalog.sceneBounds(session.getLevel(),i);
            int tw=targets[i].getMeasuredWidth(),th=targets[i].getMeasuredHeight();
            float centerY=box.centerY()-(session.showsMarkers()?11:0);
            int x=Math.max(0,Math.min(r-l-tw,Math.round(box.centerX()*scale-tw*.5f)));
            int y=Math.max(0,Math.min(b-t-th,Math.round(centerY*scale-th*.5f)));
            targets[i].layout(x,y,x+tw,y+th);
        }
    }
    private final class Surface extends View {
        private final PetakUmpetArtwork artwork;
        private final NaraRenderer nara;
        Surface(Context context,Typeface font,NaraRenderer nara){super(context);artwork=new PetakUmpetArtwork(context,font);this.nara=nara;setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);updateClock();c.save();float scale=getWidth()/360f;c.scale(scale,scale);
            if(session.isComplete())nara.draw(c,new RectF(87,10,273,231),naraMillis,host.reducedMotion());
            else if(session.getLevel()<2)artwork.scene(c,session);
            else artwork.assembly(c,session.isAssembled()?(host.reducedMotion()?1:animationMillis/(float)ASSEMBLY_DURATION):0,session.isAssembled());
            c.restore();
            if(session.isAssembled()&&isAssemblyReady()&&!readyReported){readyReported=true;post(()->host.onAssemblyReady());}
            if(running&&session.isComplete()&&!host.reducedMotion())postInvalidateDelayed(83);
            else if(running&&session.isAssembled()&&!isAssemblyReady())postInvalidateOnAnimation();
        }
    }
}
