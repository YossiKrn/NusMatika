package id.nusmatika.dakon;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.FrameLayout;

/** Animated native welcome page; all existing game screens remain unchanged. */
public final class WelcomeActivity extends Activity {
    private WelcomeLayout content;
    private boolean launching;
    private boolean motionReduced;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        Typeface font;
        try{font=Typeface.createFromAsset(getAssets(),"fonts/Nunito.ttf");}
        catch(RuntimeException error){font=Typeface.create("sans-serif-rounded",Typeface.NORMAL);}
        content=new WelcomeLayout(this,font);
        FrameLayout safe=new FrameLayout(this);safe.setBackgroundColor(Color.WHITE);safe.addView(content,new FrameLayout.LayoutParams(-1,-1));
        if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        safe.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);}
            else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(safe);safe.requestApplyInsets();
    }
    private void start(){if(launching)return;launching=true;startActivity(new Intent(this,GameChooserActivity.class));}
    private boolean reducedMotion(){try{return Settings.Global.getFloat(getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1f)==0f;}catch(RuntimeException error){return false;}}
    @Override protected void onResume(){super.onResume();launching=false;motionReduced=reducedMotion();if(content!=null)content.surface.setRunning(true);}
    @Override protected void onPause(){if(content!=null)content.surface.setRunning(false);super.onPause();}
    @Override protected void onDestroy(){if(content!=null)content.surface.setRunning(false);super.onDestroy();}

    private final class WelcomeLayout extends FrameLayout {
        final WelcomeSurface surface;
        final Button begin;
        float scale=1,left;
        WelcomeLayout(Context context,Typeface font){
            super(context);setBackgroundColor(Color.WHITE);surface=new WelcomeSurface(context,font);addView(surface);
            begin=new Button(context){@Override public void refreshDrawableState(){super.refreshDrawableState();if(surface!=null)surface.invalidate();}};
            begin.setText("Mulai");begin.setContentDescription("Mulai, pilih permainan");begin.setTextColor(Color.TRANSPARENT);begin.setBackgroundColor(Color.TRANSPARENT);
            begin.setStateListAnimator(null);begin.setElevation(0);begin.setAllCaps(false);begin.setPadding(0,0,0,0);begin.setMinWidth(0);begin.setMinHeight(0);begin.setMinimumWidth(0);begin.setMinimumHeight(0);
            begin.setOnClickListener(v->start());addView(begin);
        }
        @Override protected void onMeasure(int widthSpec,int heightSpec){
            int w=MeasureSpec.getSize(widthSpec),h=MeasureSpec.getSize(heightSpec);setMeasuredDimension(w,h);
            scale=Math.min(w/360f,h/740f);left=(w-360*scale)/2;
            surface.measure(MeasureSpec.makeMeasureSpec(w,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(h,MeasureSpec.EXACTLY));
            int minTouch=(int)Math.ceil(48*getResources().getDisplayMetrics().density);
            begin.measure(MeasureSpec.makeMeasureSpec(Math.max(minTouch,Math.round(328*scale)),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.max(minTouch,Math.round(60*scale)),MeasureSpec.EXACTLY));
        }
        @Override protected void onLayout(boolean changed,int l,int t,int r,int b){
            surface.layout(0,0,r-l,b-t);
            int x=Math.round(left+180*scale-begin.getMeasuredWidth()/2f),y=Math.round(684*scale-begin.getMeasuredHeight()/2f);
            begin.layout(x,y,x+begin.getMeasuredWidth(),y+begin.getMeasuredHeight());
        }
    }

    private final class WelcomeSurface extends View {
        final DakonArtwork art;
        final WelcomeIcons icons;
        private boolean running,drawn;
        private long accumulated,tick;
        WelcomeSurface(Context context,Typeface font){
            super(context);art=new DakonArtwork(context,font);icons=new WelcomeIcons(art);
            // Rasterize this vector scene at the real viewport resolution.
            // This avoids blurred path masks and costly per-path GPU commands
            // on older HWUI renderers; other game screens keep their renderer.
            setLayerType(View.LAYER_TYPE_SOFTWARE,null);
            setContentDescription("Buku NusMatika, karakter, dan lima ikon permainan tradisional.");
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);setFocusable(false);
        }
        void setRunning(boolean value){long now=SystemClock.uptimeMillis();if(running&&drawn)accumulated+=Math.max(0,now-tick);running=value;tick=now;if(value)invalidate();}
        private long elapsed(){return accumulated+(running&&drawn?Math.max(0,SystemClock.uptimeMillis()-tick):0);}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);if(content==null||content.scale<=0)return;
            // Begin the closed-book hold when it can actually be seen.
            if(!drawn){drawn=true;tick=SystemClock.uptimeMillis();}
            long ms=elapsed();boolean reduced=motionReduced;float seconds=WelcomeMotion.loopSeconds(ms,reduced);
            c.save();c.translate(content.left,0);c.scale(content.scale,content.scale);
            art.text(c,"NusMatika",180,82,33,900,DakonArtwork.INK,true);
            WelcomeBookArtwork.draw(c,180,WelcomeMotion.bookGround(ms,reduced),WelcomeMotion.openProgress(ms,reduced),seconds,reduced);

            int node;
            node=beginNode(c,ms,9,278,174,11,seconds,reduced);if(node>=0){icons.formula(c,"s = v × t",0xFFFFE3AA,0xFF8D571D);c.restoreToCount(node);}
            node=beginNode(c,ms,10,57,288,-13,seconds,reduced);if(node>=0){icons.formula(c,"v = s ÷ t",0xFFDDF3FF,0xFF147AA7);c.restoreToCount(node);}
            node=beginNode(c,ms,11,289,577,10,seconds,reduced);if(node>=0){icons.formula(c,"t = s ÷ v",0xFFEFE3FF,0xFF7950B5);c.restoreToCount(node);}

            // Back-to-front burst: four identities, each appears exactly once.
            human(c,ms,0,0,WelcomeCharacterArtwork.SIT,81,206,.68f,151,seconds,reduced);
            human(c,ms,1,3,WelcomeCharacterArtwork.POINT,166,214,.78f,12,seconds,reduced);
            node=beginNode(c,ms,6,288,244,-15,seconds,reduced);if(node>=0){icons.marbleAngle(c);c.restoreToCount(node);}
            human(c,ms,2,1,WelcomeCharacterArtwork.JUMP,86,372,.88f,0,seconds,reduced);
            node=beginNode(c,ms,7,169,443,-18,seconds,reduced);if(node>=0){icons.net(c,seconds,reduced);c.restoreToCount(node);}
            node=beginNode(c,ms,5,84,487,-22,seconds,reduced);if(node>=0){icons.dakon(c);c.restoreToCount(node);}
            human(c,ms,3,2,WelcomeCharacterArtwork.SACK,248,374,1.32f,-9,seconds,reduced);
            node=beginNode(c,ms,8,282,478,5,seconds,reduced);if(node>=0){icons.blocks(c,seconds,reduced);c.restoreToCount(node);}

            float pressed=content.begin.isPressed()?4:0;
            art.round(c,16,660,328,54,15,0xFF46A302);art.round(c,16,654+pressed,328,54,15,DakonArtwork.GREEN);
            art.text(c,"MULAI",180,688+pressed,17,850,DakonArtwork.INK,true);
            if(content.begin.isFocused()){art.stroke(DakonArtwork.BLUE,2.5f);c.drawRoundRect(14,652,346,716,17,17,art.paint);}
            c.restore();
            if(running&&!reduced)postInvalidateOnAnimation();
        }
        private void human(Canvas c,long ms,int index,int identity,int pose,float x,float y,float size,float rotation,float seconds,boolean reduced){
            int saved=beginNode(c,ms,index,x,y,rotation,seconds,reduced);if(saved<0)return;
            WelcomeCharacterArtwork.draw(c,identity,pose,0,0,size,0,seconds,reduced);c.restoreToCount(saved);
        }
        private int beginNode(Canvas c,long ms,int index,float targetX,float targetY,float angle,float seconds,boolean reduced){
            float p=WelcomeMotion.itemProgress(ms,index,reduced);if(p<.003f)return -1;
            float travel=WelcomeMotion.travel(p),phase=(float)(seconds*Math.PI/6);
            float driftX=reduced?0:4*(float)Math.sin(phase+index*.73f)*p;
            float driftY=reduced?0:(5+index%3)*(float)Math.sin(phase*(1+index%2)+index*.81f)*p;
            float turn=index<4?6.5f:2.4f;
            float rotation=reduced?angle:angle*travel+turn*(float)Math.sin(phase+index*.67f)*p;
            float originY=WelcomeMotion.bookGround(ms,reduced)-60;
            // A curved, staggered emergence has direction and depth; it does not
            // slide a row of identical frontal figures straight up the screen.
            float curve=4*p*(1-p)*(targetX-180)*.2f;
            int saved=c.save();c.translate(180+(targetX-180)*travel+curve+driftX,originY+(targetY-originY)*travel+driftY);c.rotate(rotation);c.scale(p,p);return saved;
        }
    }
}
