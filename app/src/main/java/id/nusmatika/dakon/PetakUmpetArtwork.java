package id.nusmatika.dakon;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Typeface;

/** Original 2D vector schoolyard and projected solids; no external game artwork. */
final class PetakUmpetArtwork {
    private final DakonArtwork art;
    private final int[] assemblyOrder=PetakUmpetCatalog.assemblyDrawingOrder();
    private static final int WOOD=0xFFEFBF68, WOOD_TOP=0xFFFFD98D, WOOD_SIDE=0xFFC68B3F;
    private static final int CUBE=0xFF75CFF0, CUBE_TOP=0xFFACEDFF, CUBE_SIDE=0xFF359CC7;
    PetakUmpetArtwork(Context context,Typeface font){art=new DakonArtwork(context,font);}

    void scene(Canvas c,PetakUmpetSession session){
        c.save();Path frame=new Path();frame.addRoundRect(0,0,360,310,20,20,Path.Direction.CW);c.clipPath(frame);
        art.round(c,0,0,360,310,20,0xFFEAF7FF);
        art.circle(c,303,36,19,0xFFFFDE84);
        art.round(c,20,28,50,12,6,0xFFFFFFFF);art.round(c,41,20,29,22,11,0xFFFFFFFF);
        art.round(c,0,145,360,165,0,0xFFDEECC6);
        path(c,0xFFCEE0AB,0,250,90,207,210,249,360,205,360,310,0,310);
        // A school and familiar playground objects provide visual search context.
        art.round(c,116,64,174,92,3,0xFFFFE7AA);
        path(c,0xFFE98576,100,68,203,21,306,68);
        art.round(c,184,100,32,56,3,0xFFBE8855);
        window(c,130,83);window(c,244,83);
        art.round(c,30,76,14,120,5,0xFFAD8156);
        art.circle(c,38,65,37,0xFF88BB6F);art.circle(c,68,78,29,0xFF9ACA77);
        art.circle(c,16,94,28,0xFF78AA62);
        art.round(c,124,169,106,10,4,0xFFB98C5C);
        art.round(c,134,181,9,24,2,0xFFA27448);art.round(c,211,181,9,24,2,0xFFA27448);
        art.round(c,129,148,98,14,4,0xFFD3A36A);
        for(int i=0;i<8;i++){
            art.round(c,251+i*14,199,8,38,3,0xFFF9F4DD);
        }
        art.round(c,247,209,113,6,2,0xFFE4D7AE);
        pot(c,24,244);pot(c,325,184);
        art.circle(c,109,239,11,0xFFFF9A81);art.stroke(0xFFF7DD89,2);
        c.drawLine(101,232,117,246,art.paint);c.drawLine(105,248,114,230,art.paint);
        for(int i=0;i<9;i++){
            float x=12+i*41,y=287+(i%3)*4;
            art.stroke(0xFF9BB872,2);c.drawLine(x,y,x-3,y-7,art.paint);c.drawLine(x,y,x+4,y-6,art.paint);
        }
        for(int i=0;i<PetakUmpetCatalog.ITEM_COUNT;i++){
            PetakUmpetCatalog.Bounds b=PetakUmpetCatalog.sceneBounds(session.getLevel(),i);
            PetakUmpetCatalog.Piece piece=PetakUmpetCatalog.piece(i);
            if(session.isFound(i)){
                art.circle(c,b.centerX(),b.centerY(),13,0xFF58CC02);
                art.stroke(0xFFFFFFFF,3);c.drawLine(b.centerX()-6,b.centerY(),b.centerX()-1,b.centerY()+5,art.paint);
                c.drawLine(b.centerX()-1,b.centerY()+5,b.centerX()+7,b.centerY()-5,art.paint);
            }else{
                solid(c,b.left,b.top+9,b.right-b.left-10,25,10,piece.kind,false);
                if(session.showsMarkers())marker(c,b.centerX(),b.top-10,piece.label());
            }
        }
        // Light foreground foliage partially overlaps the feet, never the whole object.
        for(float x:new float[]{19,119,245,338}){
            art.stroke(0xFF8DAE65,3);c.drawLine(x,304,x+5,292,art.paint);c.drawLine(x+5,304,x+11,296,art.paint);
        }
        art.stroke(0xFFD4E7ED,1.5f);c.drawRoundRect(1,1,359,309,20,20,art.paint);
        c.restore();
    }

    void assembly(Canvas c,float progress,boolean assembled){
        art.round(c,0,0,360,270,20,0xFFF6FAFC);
        art.text(c,"Bentuk L",180,28,17,850,DakonArtwork.INK,true);
        for(int i:assemblyOrder){
            PetakUmpetCatalog.Piece p=PetakUmpetCatalog.piece(i);
            float x=104+p.x*25+p.y*12,y=191-(p.z+p.height)*25-p.y*9;
            solid(c,x,y,p.width*25,p.height*25,12,p.kind,true);
        }
        float t=Math.max(0,Math.min(1,progress));t=t*t*(3-2*t);
        if(!assembled||t<.99f)art.text(c,"Bangun temuanmu",180,220,13,750,0xFF777777,true);
        for(int i:assemblyOrder){
            PetakUmpetCatalog.Piece p=PetakUmpetCatalog.piece(i);
            float targetX=104+p.x*25+p.y*12,targetY=191-(p.z+p.height)*25-p.y*9;
            float startX=21+i*53,startY=239;
            float initialWidth=p.kind==PetakUmpetCatalog.Kind.BALOK?34:18;
            solid(c,lerp(startX,targetX,t),lerp(startY,targetY,t),
                    lerp(initialWidth,p.width*25,t),lerp(18,p.height*25,t),lerp(7,12,t),p.kind,false);
        }
        if(assembled&&t>=.99f)art.text(c,"Semua bangun sudah terpakai",180,242,14,800,0xFF448D00,true);
    }

    void icon(Canvas c,float centerX,float baseline,float scale){
        c.save();c.translate(centerX,baseline);c.scale(scale,scale);
        solid(c,-40,-25,47,25,10,PetakUmpetCatalog.Kind.BALOK,false);
        solid(c,-40,-72,25,47,10,PetakUmpetCatalog.Kind.BALOK,false);
        solid(c,10,-25,25,25,10,PetakUmpetCatalog.Kind.KUBUS,false);
        art.stroke(0xFF777777,3);c.drawCircle(31,-48,13,art.paint);c.drawLine(40,-38,50,-28,art.paint);
        c.restore();
    }

    private void window(Canvas c,float x,float y){
        art.round(c,x,y,29,29,3,0xFFA0DDEC);art.stroke(0xFFFFFFFF,3);
        c.drawLine(x+14.5f,y,x+14.5f,y+29,art.paint);c.drawLine(x,y+14.5f,x+29,y+14.5f,art.paint);
    }
    private void pot(Canvas c,float x,float y){
        art.stroke(0xFF81A86C,4);c.drawLine(x,y-9,x,y-34,art.paint);
        art.circle(c,x-7,y-26,6,0xFF85B773);art.circle(c,x+7,y-34,7,0xFF9AC680);
        path(c,0xFFE7A07A,x-12,y-13,x+12,y-13,x+9,y+9,x-9,y+9);
        art.round(c,x-14,y-15,28,6,2,0xFFF2BA97);
    }
    private void marker(Canvas c,float x,float y,String name){
        art.round(c,x-31,y-23,62,21,7,0xFFFFFFFF);
        art.stroke(0xFF58CC02,1.8f);c.drawRoundRect(x-31,y-23,x+31,y-2,7,7,art.paint);
        path(c,0xFF58CC02,x-4,y-1,x+4,y-1,x,y+5);
        art.text(c,name.toUpperCase(java.util.Locale.ROOT),x,y-8,10.5f,900,0xFF428800,true);
    }
    private void solid(Canvas c,float x,float y,float width,float height,float depth,PetakUmpetCatalog.Kind kind,boolean ghost){
        float rise=depth*.75f;
        int front=ghost?0xFFE6EBEE:kind==PetakUmpetCatalog.Kind.BALOK?WOOD:CUBE;
        int top=ghost?0xFFF2F5F7:kind==PetakUmpetCatalog.Kind.BALOK?WOOD_TOP:CUBE_TOP;
        int side=ghost?0xFFD1DBDF:kind==PetakUmpetCatalog.Kind.BALOK?WOOD_SIDE:CUBE_SIDE;
        int edge=ghost?0xFF99ADB5:kind==PetakUmpetCatalog.Kind.BALOK?0xFFAB7836:0xFF258BB4;
        path(c,side,x+width,y,x+width+depth,y-rise,x+width+depth,y+height-rise,x+width,y+height);
        path(c,top,x,y,x+depth,y-rise,x+width+depth,y-rise,x+width,y);
        art.round(c,x,y,width,height,0,front);
        art.stroke(edge,1.3f);
        Path outline=new Path();outline.moveTo(x,y);outline.lineTo(x+depth,y-rise);outline.lineTo(x+width+depth,y-rise);
        outline.lineTo(x+width+depth,y+height-rise);outline.lineTo(x+width,y+height);outline.lineTo(x,y+height);outline.close();
        c.drawPath(outline,art.paint);c.drawLine(x,y,x+width,y,art.paint);
        c.drawLine(x+width,y,x+width+depth,y-rise,art.paint);c.drawLine(x+width,y,x+width,y+height,art.paint);
    }
    private void path(Canvas c,int color,float... points){
        Path p=new Path();p.moveTo(points[0],points[1]);for(int i=2;i<points.length;i+=2)p.lineTo(points[i],points[i+1]);p.close();art.fill(color);c.drawPath(p,art.paint);
    }
    private static float lerp(float from,float to,float progress){return from+(to-from)*progress;}
}
