package id.nusmatika.dakon;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.SparseArray;

/** Native vector drawing of the approved board and two-tone seed shapes. */
final class DakonArtwork {
    static final int INK=0xFF3C3C3C,BLUE=0xFF1CB0F6,CORAL=0xFFFF6B6B,PURPLE=0xFFA568E2,GREEN=0xFF58CC02;
    final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Typeface font;
    private final Context context;
    private final SparseArray<Typeface> weightedFonts=new SparseArray<>();
    private final Path blueOutline=new Path(),blueFace=new Path(),coralOutline=new Path(),coralFace=new Path();
    private final Path purpleOutline=new Path(),purpleFace=new Path();
    private final Matrix seedMatrix=new Matrix();
    private final Path seedOutlineDraw=new Path(),seedFaceDraw=new Path();
    DakonArtwork(Context context,Typeface font){
        this.context=context;this.font=font;
        blueOutline.moveTo(-.94f,-.13f);blueOutline.cubicTo(-.83f,-.55f,-.53f,-.84f,-.10f,-.82f);blueOutline.cubicTo(.35f,-.89f,.82f,-.56f,.97f,-.18f);blueOutline.cubicTo(.98f,.15f,.78f,.60f,.43f,.77f);blueOutline.cubicTo(.07f,.89f,-.20f,.65f,-.38f,.45f);blueOutline.cubicTo(-.56f,.28f,-.94f,.26f,-.94f,-.13f);blueOutline.close();
        blueFace.moveTo(-.94f,-.13f);blueFace.cubicTo(-.83f,-.55f,-.53f,-.84f,-.10f,-.82f);blueFace.cubicTo(.35f,-.89f,.82f,-.56f,.97f,-.18f);blueFace.cubicTo(.98f,.03f,.83f,.33f,.50f,.45f);blueFace.cubicTo(.16f,.62f,-.05f,.40f,-.26f,.24f);blueFace.cubicTo(-.46f,.07f,-.66f,.17f,-.83f,.06f);blueFace.cubicTo(-.89f,.02f,-.92f,-.05f,-.94f,-.13f);blueFace.close();
        coralOutline.moveTo(-.73f,-.64f);coralOutline.cubicTo(-.55f,-.80f,-.15f,-.81f,.20f,-.75f);coralOutline.cubicTo(.60f,-.72f,.84f,-.47f,.84f,-.10f);coralOutline.lineTo(.84f,.32f);coralOutline.cubicTo(.77f,.60f,.53f,.78f,.20f,.77f);coralOutline.lineTo(-.40f,.70f);coralOutline.cubicTo(-.73f,.66f,-.90f,.43f,-.87f,.10f);coralOutline.lineTo(-.86f,-.27f);coralOutline.cubicTo(-.86f,-.43f,-.82f,-.53f,-.73f,-.64f);coralOutline.close();
        coralFace.moveTo(-.73f,-.64f);coralFace.cubicTo(-.55f,-.80f,-.15f,-.81f,.20f,-.75f);coralFace.cubicTo(.60f,-.72f,.84f,-.47f,.84f,-.10f);coralFace.lineTo(.84f,.12f);coralFace.cubicTo(.82f,.36f,.55f,.50f,.23f,.49f);coralFace.lineTo(-.38f,.42f);coralFace.cubicTo(-.66f,.38f,-.84f,.22f,-.87f,.10f);coralFace.lineTo(-.86f,-.27f);coralFace.cubicTo(-.86f,-.43f,-.82f,-.53f,-.73f,-.64f);coralFace.close();
        purpleOutline.moveTo(-.17f,-.88f);purpleOutline.cubicTo(.15f,-.96f,.48f,-.66f,.71f,-.36f);purpleOutline.cubicTo(.99f,-.03f,.94f,.36f,.64f,.61f);purpleOutline.cubicTo(.35f,.89f,-.10f,.88f,-.49f,.65f);purpleOutline.cubicTo(-.86f,.44f,-.97f,.12f,-.77f,-.22f);purpleOutline.cubicTo(-.61f,-.50f,-.47f,-.78f,-.17f,-.88f);purpleOutline.close();
        purpleFace.moveTo(-.17f,-.88f);purpleFace.cubicTo(.15f,-.96f,.48f,-.66f,.71f,-.36f);purpleFace.cubicTo(.94f,-.07f,.88f,.18f,.58f,.38f);purpleFace.cubicTo(.28f,.59f,-.09f,.56f,-.46f,.36f);purpleFace.cubicTo(-.67f,.25f,-.79f,.08f,-.77f,-.22f);purpleFace.cubicTo(-.61f,-.50f,-.47f,-.78f,-.17f,-.88f);purpleFace.close();
    }
    void fill(int color){paint.setColor(color);paint.setStyle(Paint.Style.FILL);paint.setAlpha(255);}
    void stroke(int color,float width){paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(width);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);paint.setAlpha(255);}
    void round(Canvas c,float x,float y,float w,float h,float radius,int color){fill(color);c.drawRoundRect(x,y,x+w,y+h,radius,radius,paint);}
    void circle(Canvas c,float x,float y,float r,int color){fill(color);c.drawCircle(x,y,r,paint);}
    void text(Canvas c,String text,float x,float baseline,float size,int weight,int color,boolean centered){
        Typeface face=weightedFonts.get(weight);
        if(face==null){
            try{face=new Typeface.Builder(context.getAssets(),"fonts/Nunito.ttf").setFontVariationSettings("'wght' "+weight).build();}
            catch(RuntimeException error){face=Typeface.create(font,weight>=700?Typeface.BOLD:Typeface.NORMAL);}
            weightedFonts.put(weight,face);
        }
        // Keep variation in the immutable Typeface. Resetting Paint's Typeface
        // then reusing an unchanged variation string can drop the weight on API28.
        fill(color);paint.setTypeface(face);paint.setTextSize(size);
        paint.setTextAlign(centered?Paint.Align.CENTER:Paint.Align.LEFT);c.drawText(text,x,baseline,paint);
    }
    void seed(Canvas c,float x,float y,int kind,float radius){
        // Materialize the geometry before drawing. Android 9 HWUI may cache a
        // two-unit path as a tiny mask and blur it when Canvas.scale enlarges it.
        seedMatrix.setScale(radius,radius);
        (kind==0?blueOutline:kind==3?purpleOutline:coralOutline).transform(seedMatrix,seedOutlineDraw);
        (kind==0?blueFace:kind==3?purpleFace:coralFace).transform(seedMatrix,seedFaceDraw);
        c.save();c.translate(x,y);c.rotate(kind==0?-16:kind==3?-8:12);
        fill(kind==0?0xFF008CCB:kind==3?0xFF7B47B5:0xFFE34955);c.drawPath(seedOutlineDraw,paint);
        fill(kind==0?BLUE:kind==3?PURPLE:CORAL);c.drawPath(seedFaceDraw,paint);c.restore();
    }
    static float[][] slots(int count){
        int visible=Math.min(4,Math.max(0,count));
        float radius=18.4f,padding=radius*.055f,gap=radius*.1f;
        float max=visible==1?radius-padding:visible==2?(radius-padding-gap/2)/2:visible==3?
                (float)((radius-padding-gap/Math.sqrt(3))/(1+2/Math.sqrt(3))):
                (float)((radius-padding-gap/Math.sqrt(2))/(1+Math.sqrt(2)));
        float r=visible==1?9.7f:Math.min(6.9f,max),step=r+gap/2;
        if(visible==0)return new float[0][];
        if(visible==1)return new float[][]{{0,0,r}};
        if(visible==2)return new float[][]{{-step,0,r},{step,0,r}};
        if(visible==3)return new float[][]{{0,(float)(-2*step/Math.sqrt(3)),r},{-step,(float)(step/Math.sqrt(3)),r},{step,(float)(step/Math.sqrt(3)),r}};
        return new float[][]{{-step,-step,r},{step,-step,r},{-step,step,r},{step,step,r}};
    }
    void board(Canvas c,DakonSession session,int animatedHole){
        round(c,16,207,328,310,27,0xFFCD923A);
        round(c,16,202,328,310,27,0xFFE7AE54);
        round(c,16,199,328,310,27,0xFFFBD18A);
        stroke(0xFFFFE6AE,3);c.drawLine(43,201,317,201,paint);
        for(int n=1;n<=24;n++){
            float x=BoardGeometry.centerX(n),y=BoardGeometry.centerY(n);
            circle(c,x,y+.7f,22.5f,0xFFFFE5AC);
            circle(c,x,y,21,0xFFC68A34);
            circle(c,x,y+2.4f,18.4f,0xFFFFF0D3);
            int[] kinds=session.getSeeds(n),visible=BoardGeometry.visibleSeeds(kinds);float[][] slots=slots(kinds.length);
            for(int i=0;i<slots.length;i++)if(!(n==animatedHole&&i==kinds.length-1))seed(c,x+slots[i][0],y+2.4f+slots[i][1],visible[i],slots[i][2]);
            if(kinds.length>4){
                round(c,x+6,y-23,19,17,6,0xFF79512B);
                text(c,Integer.toString(kinds.length),x+15.5f,y-10,10,800,0xFFFFFFFF,true);
            }
            boolean guided=session.getStage()==DakonSession.NIROKKE;
            boolean expected=guided&&session.getExpectedHole()==n;
            boolean common=guided&&session.isGuideComplete()&&(n==12||n==24);
            if(expected||common){stroke(GREEN,2.5f);c.drawCircle(x,y,24.7f,paint);}
            if(session.getMoveSource()==n){stroke(BLUE,2.5f);c.drawCircle(x,y,24.7f,paint);}
            text(c,Integer.toString(n),x,y+38,15,800,0xFF79512B,true);
        }
    }

    /** Learning owns its seed order so all three operands retain their colors. */
    void board(Canvas c,DakonLearningSession session,int animatedHole){
        round(c,16,207,328,310,27,0xFFCD923A);
        round(c,16,202,328,310,27,0xFFE7AE54);
        round(c,16,199,328,310,27,0xFFFBD18A);
        stroke(0xFFFFE6AE,3);c.drawLine(43,201,317,201,paint);
        for(int n=1;n<=24;n++){
            float x=BoardGeometry.centerX(n),y=BoardGeometry.centerY(n);
            circle(c,x,y+.7f,22.5f,0xFFFFE5AC);
            circle(c,x,y,21,0xFFC68A34);
            circle(c,x,y+2.4f,18.4f,0xFFFFF0D3);
            int[] kinds=session.getSeeds(n),visible=visibleLearningSeeds(kinds);float[][] slots=slots(kinds.length);
            for(int i=0;i<slots.length;i++)if(!(n==animatedHole&&i==kinds.length-1))seed(c,x+slots[i][0],y+2.4f+slots[i][1],visible[i],slots[i][2]);
            if(kinds.length>4){
                round(c,x+6,y-23,19,17,6,0xFF79512B);
                text(c,Integer.toString(kinds.length),x+15.5f,y-10,10,800,0xFFFFFFFF,true);
            }
            text(c,Integer.toString(n),x,y+38,15,800,0xFF79512B,true);
        }
    }

    private static int[] visibleLearningSeeds(int[] seeds){
        int[] visible=java.util.Arrays.copyOf(seeds,Math.min(4,seeds.length));
        if(seeds.length<=4)return visible;
        for(int kind:new int[]{0,1,3}){
            boolean present=false,shown=false;
            for(int seed:seeds)if(seed==kind){present=true;break;}
            for(int seed:visible)if(seed==kind){shown=true;break;}
            if(!present||shown)continue;
            for(int i=visible.length-1;i>=0;i--){
                int occurrences=0;for(int seed:visible)if(seed==visible[i])occurrences++;
                if(occurrences>1){visible[i]=kind;break;}
            }
        }
        return visible;
    }
}
