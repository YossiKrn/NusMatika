package id.nusmatika.dakon;

import android.graphics.Canvas;
import android.graphics.Path;

/** Small original game illustrations, drawn at native display resolution. */
final class WelcomeIcons {
    private final DakonArtwork art;
    private final Path path=new Path();
    private static final int[][] NET={{1,0},{0,1},{1,1},{2,1},{1,2},{1,3}};
    private static final int[] NET_COLORS={0xFFBDEBFF,0xFFFFD58A,0xFF7CD8F1,0xFFA2E7D6,0xFF9ED8FF,0xFFCAB0F7};
    WelcomeIcons(DakonArtwork art){this.art=art;}

    void dakon(Canvas c){
        art.round(c,-44,-24,88,54,11,0xFFCD923A);
        art.round(c,-44,-28,88,54,11,0xFFFBD18A);
        for(int i=0;i<24;i++){
            float x=-33+(i%6)*13.2f,y=-17+(i/6)*11;
            art.circle(c,x,y,4.2f,0xFFC68A34);art.circle(c,x,y+.6f,3.3f,0xFFFFF0D3);
            if(i==2||i==7||i==16||i==22)art.seed(c,x,y+.6f,i%2,2.2f);
        }
    }

    void marbleAngle(Canvas c){
        marble(c,-23,22,9,0xFF1CB0F6,0xFF008CCB,0xFF88DFFF);
        marble(c,22,-18,8,0xFFFF6B6B,0xFFE34955,0xFFFFB0A4);
        art.stroke(0xFF9160D3,3);c.drawLine(-9,12,22,12,art.paint);c.drawLine(-9,12,6.5f,-14.85f,art.paint);
        c.drawArc(-21,0,3,24,-60,60,false,art.paint);
        art.text(c,"60°",10,33,11.5f,850,0xFF7950B5,true);
    }
    private void marble(Canvas c,float x,float y,float r,int face,int depth,int light){
        art.circle(c,x,y,r,depth);art.circle(c,x,y-r*.16f,r*.88f,face);
        art.round(c,x-r*.47f,y-r*.57f,r*.6f,r*.26f,r*.13f,light);
    }

    void net(Canvas c,float seconds,boolean reduced){
        float fold=reduced?1f:.94f+.06f*(float)Math.cos(seconds*Math.PI/3);
        for(int i=0;i<NET.length;i++){
            float x=-21+NET[i][0]*14,y=-28+NET[i][1]*14;
            c.save();if(i==5){c.translate(0,y);c.scale(1,fold);c.translate(0,-y);}
            art.round(c,x,y,14,14,2,NET_COLORS[i]);art.stroke(0xFFFFFFFF,1.2f);c.drawRoundRect(x,y,x+14,y+14,2,2,art.paint);c.restore();
        }
    }

    void blocks(Canvas c,float seconds,boolean reduced){
        float breathe=reduced?0:1.6f*(float)Math.sin(seconds*Math.PI/3);
        box(c,-14,-4,14,7,18,0xFF5CE2CF,0xFF00A991,0xFF25C9B4);
        box(c,14,-4,14,7,18,0xFFFFCD70,0xFFEB8F23,0xFFFFAB3D);
        box(c,0,-22+breathe,24,9,16,0xFFC49AFF,0xFF8250D0,0xFFA56BF0);
    }
    private void box(Canvas c,float x,float y,float w,float d,float h,int top,int left,int right){
        polygon(c,left,x-w,y,x,y+d,x,y+d+h,x-w,y+h);
        polygon(c,right,x,y+d,x+w,y,x+w,y+h,x,y+d+h);
        polygon(c,top,x,y-d,x+w,y,x,y+d,x-w,y);
    }
    private void polygon(Canvas c,int color,float x1,float y1,float x2,float y2,float x3,float y3,float x4,float y4){
        path.reset();path.moveTo(x1,y1);path.lineTo(x2,y2);path.lineTo(x3,y3);path.lineTo(x4,y4);path.close();art.fill(color);c.drawPath(path,art.paint);
    }

    void formula(Canvas c,String value,int fill,int ink){
        art.round(c,-45,-13,90,28,10,fill);
        art.text(c,value,0,5,13.5f,850,ink,true);
    }
}
