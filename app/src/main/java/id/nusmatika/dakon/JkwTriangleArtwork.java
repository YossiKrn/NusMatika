package id.nusmatika.dakon;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

/** Approved flat JKW triangle, including colored dividers and separated arrows. */
public final class JkwTriangleArtwork {
    public static final int DIVIDE = 0xFFD93649, MULTIPLY = 0xFF1854C4;
    private final DakonArtwork art;
    private final Path outline = new Path(), lines = new Path();

    public JkwTriangleArtwork(Context context, Typeface font) {
        art = new DakonArtwork(context, font);
        outline.moveTo(166,38); outline.quadTo(180,12,194,38); outline.lineTo(332,314);
        outline.quadTo(346,342,314,342); outline.lineTo(46,342); outline.quadTo(14,342,28,314); outline.close();
    }
    private float scale(RectF bounds) { return Math.min(bounds.width()/360f,bounds.height()/452f); }
    private float left(RectF b,float s) { return b.centerX()-180*s; }
    private float top(RectF b,float s) { return b.centerY()-226*s; }

    public void draw(Canvas c, RectF bounds, BalapKarungSession.Quantity hidden) {
        drawAnimated(c,bounds,hidden,null);
    }
    public void drawAnimated(Canvas c,RectF bounds,BalapKarungSession.Quantity hidden,JkwTutorialMotion.Frame frame){
        float s=scale(bounds);if(s<=0)return;
        int saved=c.save();c.translate(left(bounds,s),top(bounds,s));c.scale(s,s);
        int depth=c.save();c.translate(0,6);c.clipPath(outline);
        face(c,0xFF46A302,0xFFE34955,0xFF008CCB);c.restoreToCount(depth);
        int face=c.save();c.clipPath(outline);face(c,DakonArtwork.GREEN,0xFFFF8585,DakonArtwork.BLUE);
        float division=frame==null?1:frame.divideLine,multiplication=frame==null?1:frame.multiplyLine;
        if(division<1){art.stroke(0xFFFFFFFF,8);c.drawLine(62,184,298,184,art.paint);}
        if(division>0){art.stroke(DIVIDE,8);c.drawLine(62,184,62+236*division,184,art.paint);}c.restoreToCount(face);
        if(multiplication<1){art.stroke(0xFFFFFFFF,8);art.paint.setStrokeCap(android.graphics.Paint.Cap.BUTT);c.drawLine(180,188,180,348,art.paint);}
        if(multiplication>0){art.stroke(MULTIPLY,8);art.paint.setStrokeCap(android.graphics.Paint.Cap.BUTT);c.drawLine(180,188,180,188+160*multiplication,art.paint);}
        if(hidden!=BalapKarungSession.Quantity.DISTANCE){label(c,"J",180,120,66,900);label(c,"Jarak",180,150,19,850);}
        if(hidden!=BalapKarungSession.Quantity.SPEED){label(c,"K",109,275,66,900);label(c,"Kecepatan",109,310,19,850);}
        if(hidden!=BalapKarungSession.Quantity.TIME){label(c,"W",251,275,66,900);label(c,"Waktu",251,310,19,850);}
        float pulse=frame==null?0:frame.pulse;
        operation(c,true,frame==null?1:frame.divideArrow,frame!=null&&frame.activeOperation==JkwTutorialMotion.DIVIDE?pulse:0);
        operation(c,false,frame==null?1:frame.multiplyArrow,frame!=null&&frame.activeOperation==JkwTutorialMotion.MULTIPLY?pulse:0);
        c.restoreToCount(saved);
    }
    private void operation(Canvas c,boolean division,float alpha,float pulse){
        if(alpha<=0)return;
        RectF area=division?new RectF(8,140,100,198):new RectF(132,348,228,454);
        int layer=alpha<1?c.saveLayerAlpha(area,Math.round(255*alpha)):c.save();
        // Move away from the divider, retaining the approved gap at the arrow tip.
        c.translate(division?-4*pulse:0,division?0:4*pulse);
        int color=division?DIVIDE:MULTIPLY;
        art.text(c,division?"Dibagi":"Dikali",division?48:180,division?166:434,19,850,color,true);
        art.stroke(color,3.5f+.5f*pulse);lines.reset();
        if(division){lines.moveTo(18,184);lines.lineTo(88,184);lines.moveTo(80,178);lines.lineTo(88,184);lines.lineTo(80,190);}
        else{lines.moveTo(180,405);lines.lineTo(180,362);lines.moveTo(174,370);lines.lineTo(180,362);lines.lineTo(186,370);}
        c.drawPath(lines,art.paint);c.restoreToCount(layer);
    }
    /** Small permanent aid: letters and dividers stay legible without tiny captions. */
    public void drawCompact(Canvas c,RectF bounds,BalapKarungSession.Quantity hidden){
        float s=Math.min(bounds.width()/336f,bounds.height()/336f);if(s<=0)return;
        int saved=c.save();c.translate(bounds.centerX()-180*s,bounds.centerY()-188*s);c.scale(s,s);
        int depth=c.save();c.translate(0,6);c.clipPath(outline);face(c,0xFF46A302,0xFFE34955,0xFF008CCB);c.restoreToCount(depth);
        int surface=c.save();c.clipPath(outline);face(c,DakonArtwork.GREEN,0xFFFF8585,DakonArtwork.BLUE);
        art.stroke(DIVIDE,8);c.drawLine(62,184,298,184,art.paint);c.restoreToCount(surface);
        art.stroke(MULTIPLY,8);art.paint.setStrokeCap(android.graphics.Paint.Cap.BUTT);c.drawLine(180,188,180,348,art.paint);
        if(hidden!=BalapKarungSession.Quantity.DISTANCE)label(c,"J",180,128,72,900);
        if(hidden!=BalapKarungSession.Quantity.SPEED)label(c,"K",109,282,72,900);
        if(hidden!=BalapKarungSession.Quantity.TIME)label(c,"W",251,282,72,900);
        c.restoreToCount(saved);
    }
    private void face(Canvas c,int green,int coral,int blue) {
        art.fill(green);c.drawRect(0,0,360,184,art.paint);
        art.fill(coral);c.drawRect(0,184,180,360,art.paint);
        art.fill(blue);c.drawRect(180,184,360,360,art.paint);
    }
    private void label(Canvas c,String text,float x,float y,float size,int weight){art.text(c,text,x,y,size,weight,DakonArtwork.INK,true);}

    public static String description(BalapKarungSession.Quantity hidden) {
        return "Segitiga JKW. "+(hidden==BalapKarungSession.Quantity.DISTANCE?"Jarak dicari. ":"J, jarak di atas. ")
                +(hidden==BalapKarungSession.Quantity.SPEED?"Kecepatan dicari. ":"K, kecepatan di kiri bawah. ")
                +(hidden==BalapKarungSession.Quantity.TIME?"Waktu dicari. ":"W, waktu di kanan bawah. ")
                +"Garis mendatar merah: dibagi. Garis tegak biru: dikali.";
    }
}
