package id.nusmatika.dakon;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Original toon figures: shared rig, four deliberately different silhouettes/faces.
 * The center is the torso pivot, not the feet. Geometry is authored in three-quarter
 * view with graphic side planes; parent joints keep every limb connected.
 * These native rigs are not Rive files or Duolingo-owned character assets.
 */
public final class WelcomeCharacterArtwork {
    public static final int WAVE=0,JUMP=1,POINT=2,SIT=3,SACK=4;
    private static final float TAU=(float)(Math.PI*2);
    private static final int[] SKIN={0xFFFFC888,0xFFC68455,0xFFFFA257,0xFFEBA57D};
    private static final int[] SHADE={0xFFF0A55C,0xFFAC653D,0xFFE78038,0xFFD88A67};
    private static final int[] SHIRT={0xFF16B5E5,0xFFFFAA00,0xFF008E88,0xFFFFA0DC};
    private static final int[] DEPTH={0xFF008CC5,0xFFEC8700,0xFF00726F,0xFFEA72BC};
    private static final int[] PANTS={0xFF0085B9,0xFF238CCB,0xFF1852C8,0xFF8460D7};
    private static final int[] SHOES={0xFFFF7B55,0xFFFA5968,0xFF00716C,0xFFC43EA1};
    private static final int INK=0xFF4A2B26;
    private static final ThreadLocal<Paint> BRUSH=new ThreadLocal<Paint>() {
        @Override protected Paint initialValue(){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);return p;}
    };
    private WelcomeCharacterArtwork(){}
    private static final Path BROAD=path("M -23 -17 Q -30 -13 -28 1 L -25 24 Q -23 33 -10 34 L 15 32 Q 27 31 27 19 L 24 -8 Q 23 -19 9 -20 Z");
    private static final Path BROAD_SIDE=path("M 15 -18 Q 25 -14 25 -5 L 27 20 Q 27 31 15 32 L 11 0 Z");
    private static final Path HOODIE=path("M -19 -17 Q -27 -15 -26 -3 L -23 27 Q -22 35 -10 35 L 15 32 Q 25 30 24 20 L 23 -7 Q 21 -18 10 -19 Z");
    // Flat, continuous trouser contours and small capsule feet. Cuffs make the
    // ankle direction readable, and feet follow their corresponding shin.
    private static final Path TARI_TUNIC=path("M -14 -18 Q -23 -15 -23 -4 L -19 15 L -25 37 Q -18 43 -3 43 L 21 40 Q 28 39 25 31 L 18 11 L 20 -5 Q 19 -17 8 -20 Z");
    private static final Path TARI_HEM=path("M -25 37 Q -4 42 25 35 L 25 39 Q 2 46 -25 41 Z");
    private static final Path TARI_FAR_LEG=path("M -18 31 L -2 34 L -9 51 L -3 65 Q 0 72 -7 74 Q -14 76 -17 70 L -24 54 Q -27 46 -18 31 Z");
    private static final Path TARI_NEAR_LEG=path("M 3 32 Q 10 29 18 33 Q 20 43 28 50 L 35 63 Q 39 71 31 74 Q 23 75 20 67 L 13 55 Q 6 48 3 32 Z");
    private static final Path RAKA_TEE=path("M -21 -16 Q -12 -23 8 -21 L 18 -17 Q 27 -11 26 1 L 23 28 Q 12 35 -7 35 Q -23 35 -25 24 L -27 -1 Q -27 -11 -21 -16 Z");
    private static final Path RAKA_HEM=path("M -24 28 Q -4 35 24 26 L 23 31 Q 1 39 -20 34 Z");
    private static final Path RAKA_COLLAR=path("M -10 -20 Q 0 -13 12 -20 L 10 -16 Q 0 -10 -9 -17 Z");
    // Sideways leap in its on-screen orientation. The folded knee is the low
    // point of the pose. Its heel returns behind the hip, left of the knee.
    private static final Path RAKA_EXTENDED_LEG = path(
        "M 78 89 Q 81 78 93 79 Q 105 80 112 90 L 158 81 "
        + "Q 166 79 168 91 L 170 100 Q 172 112 161 115 "
        + "Q 152 116 110 109 L 86 109 Q 72 106 78 89 Z");
    private static final Path RAKA_TUCKED_LEG = path(
        "M 71 100 Q 78 90 91 94 Q 104 102 119 119 "
        + "C 129 130 133 140 123 143 Q 111 147 83 135 "
        + "Q 73 131 74 121 Q 68 113 69 106 Q 69 102 71 100 Z");
    private static final Path RAKA_JUMP_SHIRT = path(
        "M 44 79 Q 50 64 65 64 Q 78 66 89 77 "
        + "Q 96 84 88 95 Q 81 106 70 107 "
        + "Q 58 102 49 95 Q 41 89 44 79 Z");
    private static final Path BOB=path("M -28 10 Q -33 11 -31 -2 Q -36 -13 -28 -26 Q -27 -42 -7 -43 Q 12 -50 24 -34 Q 34 -28 30 -10 Q 39 10 25 21 L -20 24 Q -31 25 -28 10 Z");
    private static final Path NARA_FACE=path("M -23 -22 Q -26 -12 -24 10 Q -23 22 -10 25 L 13 24 Q 25 22 25 8 L 23 -14 Q 22 -27 10 -28 L -10 -28 Z");
    private static final Path NARA_SIDE=path("M 15 -26 Q 25 -23 25 -10 L 25 10 Q 25 22 13 24 L 10 22 L 13 -14 Z");
    private static final Path NARA_FRINGE=path("M -28 -16 Q -30 -35 -12 -40 Q 7 -48 24 -33 Q 19 -17 3 -18 Q -3 -19 -9 -14 Q -21 -10 -28 -16 Z");
    private static final Path BIMA_HEAD=path("M -22 -19 Q -25 -8 -20 12 Q -17 25 1 25 Q 18 25 23 11 L 23 -14 Q 20 -27 7 -29 L -10 -28 Z");
    private static final Path BIMA_SIDE=path("M 15 -24 Q 24 -18 23 -4 L 23 10 Q 20 23 7 25 L 9 17 L 12 -11 Z");
    private static final Path BIMA_HAIR=path("M -24 -8 Q -32 -25 -17 -34 Q -4 -41 13 -36 Q 26 -33 24 -16 L 14 -19 L 9 -27 L -1 -22 L -8 -24 L -17 -13 Z");
    private static final Path RAKA_HEAD=path("M -25 -20 Q -29 -6 -23 12 Q -19 26 -3 28 L 17 24 Q 28 21 27 6 L 24 -19 Q 22 -30 6 -30 L -12 -29 Z");
    private static final Path RAKA_SIDE=path("M 16 -27 Q 27 -21 27 -5 L 27 7 Q 27 20 17 24 L 9 26 L 14 13 Z");
    private static final Path CURLS=path("M -28 -10 Q -36 -15 -28 -26 Q -30 -37 -17 -38 Q -12 -48 0 -42 Q 12 -51 19 -41 Q 33 -41 31 -27 Q 35 -17 25 -14 L 18 -20 Q 10 -15 3 -22 Q -4 -16 -12 -23 Q -20 -12 -28 -10 Z");
    private static final Path HIJAB=path("M -29 15 Q -28 -1 -27 -22 Q -25 -43 -4 -46 Q 20 -46 29 -24 L 34 18 Q 13 31 -13 25 Z");
    private static final Path HIJAB_SIDE=path("M 12 -43 Q 27 -37 30 -20 L 34 18 L 18 25 L 15 4 Z");
    private static final Path TARI_FACE=path("M -18 -19 Q -11 -30 3 -29 Q 19 -26 21 -10 L 21 5 Q 16 21 1 22 Q -18 20 -20 2 Z");
    private static final Path HIJAB_FOLD=path("M -26 11 Q -9 26 17 15 L 30 21 Q 10 31 -12 25 L -28 19 Z");
    private static final Path NARA_MOUTH=path("M -7 10 Q 2 14 12 7 Q 14 20 5 22 Q -3 22 -7 10 Z");
    private static final Path NARA_TONGUE=path("M -1 19 Q 5 13 11 16 Q 8 22 3 22 Z");
    private static final Path RAKA_MOUTH=path("M -12 9 Q 1 15 15 6 Q 17 25 4 26 Q -9 26 -12 9 Z");
    private static final Path RAKA_TEETH=path("M -10 10 Q 2 16 14 8 L 12 13 Q 1 20 -7 15 Z");
    private static final Path RAKA_TONGUE=path("M -3 24 Q 4 16 12 21 Q 10 26 3 26 Z");
    private static final Path TARI_SMILE=path("M -5 9 Q 4 13 13 7 Q 14 18 5 20 Q -4 19 -5 9 Z");
    private static final Path TARI_TONGUE=path("M 0 18 Q 6 13 10 16 Q 8 20 4 20 Z");
    private static final Path SACK_BACK=path("M -29 0 Q -34 17 -27 47 Q -25 57 -9 59 L 17 56 Q 31 53 32 39 L 31 0 Z");
    private static final Path SACK_FACE=path("M -29 3 L 20 4 L 21 40 Q 22 50 12 52 L -12 54 Q -23 53 -24 43 Z");
    private static final Path SACK_SIDE=path("M 20 2 L 31 0 L 32 39 Q 31 53 17 56 L 12 53 Q 23 48 21 37 Z");
    private static final Path SACK_FACET=path("M -25 7 L -13 7 L -10 51 L -18 50 Q -25 47 -25 35 Z");
    private static final Path SACK_RIM=path("M -30 -2 Q -5 6 31 -2 L 31 8 Q 0 16 -30 7 Z");
    private static final Path SHOE=path("M -7 -3 Q 0 -8 7 -3 L 17 2 Q 23 5 18 11 L -8 10 Q -13 8 -10 2 Z");
    private static final Path SOLE=path("M -11 6 L 20 6 L 18 11 L -8 10 Q -12 9 -11 6 Z");

    public static void draw(Canvas c,int character,int pose,float centerX,float centerY,
                            float scale,float rotation,float seconds,boolean reduced){
        if(c==null||!finite(scale)||scale<=0||!finite(centerX)||!finite(centerY))return;
        int id=Math.floorMod(character,4),action=pose>=0&&pose<=4?pose:WAVE;
        float t=finite(seconds)?seconds%12f:0;if(t<0)t+=12;
        float phase=t*TAU/12f,offset=id*.69f;
        float breath=reduced?0:.7f*sine(phase*2+offset);
        // Authored pose holds, with the head/torso following the hand a moment
        // later. These tracks have zero velocity at every hold and loop seam.
        float gesture=reduced?0:acting(t+id*.71f);
        float follow=reduced?0:acting(t+id*.71f-.28f);
        float gaze=reduced?0:1.6f*acting(t+id*.71f+.25f);
        float expression=reduced?1:1-.55f*Math.max(0,acting(t+id*.71f-1.1f));
        float blink=reduced?0:Math.max(blink(t,2.2f+id*.37f),blink(t,8.1f-id*.31f));
        Paint p=BRUSH.get();int saved=c.save();c.translate(centerX,centerY);c.scale(scale,scale);
        c.rotate(finite(rotation)?rotation:0);c.scale(1f+breath*.009f,1f-breath*.009f);
        if(action==SACK){
            sack(c,p,id,breath,follow,gaze,blink,expression);c.restoreToCount(saved);return;
        }
        if (id == 1 && action == JUMP) {
            drawRakaLeap(c, p, gesture, follow, gaze, blink, expression);
            c.restoreToCount(saved);
            return;
        }
        boolean designedLower=id==3&&action==POINT;
        if(designedLower){
            drawTariLegs(c,p,follow);
        }else if(action==JUMP){
            leg(c,p,id,-12,26,37+follow*3,20,19,-78,15,false);
            leg(c,p,id,11,26,-61+follow*3,22,18,74,16,true);
        }else if(action==SIT){
            leg(c,p,id,-10,25,51+follow*4,18,20,-82,15,false);
            leg(c,p,id,10,25,-30-follow*3,22,18,76,16,true);
        }else{
            leg(c,p,id,-11,27,12+follow*2,19,18,12,14,false);
            leg(c,p,id,10,27,-22-follow*2,21,18,-13,15,true);
        }
        if(!designedLower)rr(c,p,-23,15,47,23,12,PANTS[id]);
        int torso=c.save();c.translate(0,breath);c.rotate(follow*3.5f,0,4);
        float farShoulder=action==JUMP?122:action==POINT?28:action==SIT?105:145;
        float farBend=action==JUMP?-30:action==SIT?70:-20;
        float nearShoulder=action==JUMP?-110:action==POINT?-100:action==SIT?-118:-10;
        float nearBend=action==JUMP?30:action==POINT?8:action==SIT?-35:-22;
        arm(c,p,id,-1,farShoulder+gesture*9,farBend,0.9f,false);
        shape(c,p,id==3?TARI_TUNIC:id==1?RAKA_TEE:id==0?HOODIE:BROAD,SHIRT[id]);
        if(id==3)shape(c,p,TARI_HEM,0xFFF291D0);
        else if(id==1){shape(c,p,RAKA_HEM,0xFFF19A00);shape(c,p,RAKA_COLLAR,0xFFEC8700);}
        else shape(c,p,BROAD_SIDE,DEPTH[id]);
        // A single large collar shape, not many shirt/seam decorations.
        if(id==0){rr(c,p,-9,-15,23,9,4,DEPTH[id]);line(c,p,-4,-8,-4,3,2,0xFFE1F9FF);line(c,p,5,-8,5,2,2,0xFFE1F9FF);}
        arm(c,p,id,1,nearShoulder-gesture*(action==POINT?5:10),nearBend+follow*5,1f,true);
        int head=c.save();c.translate(id==3?-1:2,-39);c.rotate(-3+follow*5.5f,0,5);
        head(c,p,id,gaze,blink,expression);c.restoreToCount(head);
        c.restoreToCount(torso);c.restoreToCount(saved);
    }
    private static void drawTariLegs(Canvas c,Paint p,float follow){
        int s=c.save();c.rotate(follow*1.2f,0,30);
        shape(c,p,TARI_FAR_LEG,0xFF6F4DBC);
        flatShoe(c,p,-10,70,-16,0xFFAC368F,0xFF9174D1,.94f);
        shape(c,p,TARI_NEAR_LEG,PANTS[3]);
        flatShoe(c,p,29,69,-20,SHOES[3],0xFFA88BEA,1f);
        c.restoreToCount(s);
    }
    private static void drawRakaLeap(Canvas c, Paint p, float gesture, float follow,
                                     float gaze, float blink, float expression) {
        c.save();
        c.translate(-92, -84);

        // Plant the little heel behind the folded trouser leg.
        c.save();
        c.rotate(30, 73, 126);
        rr(c, p, 68, 117, 11, 20, 5.5f, 0xFFE94F61);
        c.restore();

        // Rotate cloth, cuff and shoe together around the hip.
        c.save();
        c.rotate(-2 * follow, 94, 102);
        shape(c, p, RAKA_EXTENDED_LEG, PANTS[1]);
        c.rotate(-13, 161, 98);
        oval(c, p, 150, 83, 170, 113, 0xFF4FAADB);
        rr(c, p, 164, 69, 15, 34, 7.5f, SHOES[1]);
        c.restore();
        shape(c, p, RAKA_TUCKED_LEG, PANTS[1]);

        // One arm rises behind the head; the other reaches past the chin.
        c.save();
        c.rotate(2 * gesture, 70, 70);
        limb(c, p, 72, 70, 74, 46, 14, SHIRT[1]);
        limb(c, p, 74, 46, 71, 28, 12, SHADE[1]);
        circle(c, p, 71, 27, 9, SHADE[1]);
        c.restore();
        shape(c, p, RAKA_JUMP_SHIRT, SHIRT[1]);
        c.save();
        c.rotate(2.5f * gesture, 68, 88);
        limb(c, p, 69, 87, 43, 102, 18, SHIRT[1]);
        limb(c, p, 43, 102, 19, 110, 12, SKIN[1]);
        circle(c, p, 18, 110, 9.5f, SKIN[1]);
        c.restore();
        c.save();
        c.translate(49, 68);
        c.rotate(-12 + follow * 2);
        c.scale(1.08f, 1.08f);
        head(c, p, 1, gaze, blink, expression);
        c.restore();
        c.restore();
    }
    private static void flatShoe(Canvas c,Paint p,float x,float y,float ankleAngle,int face,int cuff,float scale){
        int s=c.save();c.translate(x,y);c.rotate(ankleAngle);c.scale(scale,scale);
        rr(c,p,-7,-2,14,5,1.5f,cuff);
        rr(c,p,-8,4,22,8,4,face);
        c.restoreToCount(s);
    }
    private static void sack(Canvas c,Paint p,int id,float breath,float follow,float gaze,float blink,float expression){
        int saved=c.save();c.translate(0,-2.5f*follow);
        // Back rim, shirt, face and foreground gripping hands are ordered by depth.
        shape(c,p,SACK_BACK,0xFFAE7136);
        rr(c,p,-21,-13,42,25,9,SHIRT[id]);
        limb(c,p,-17,-9,-29,1,11,SHADE[id]);limb(c,p,-29,1,-25,13,10,SKIN[id]);
        limb(c,p,17,-10,28,3,12,SKIN[id]);limb(c,p,28,3,26,14,11,SKIN[id]);
        int h=c.save();c.translate(2,-39+breath);c.rotate(-6+follow*5,0,7);head(c,p,id,gaze,blink,expression);c.restoreToCount(h);
        shape(c,p,SACK_FACE,0xFFE6B25B);shape(c,p,SACK_SIDE,0xFFC68A3D);shape(c,p,SACK_FACET,0xFFF5CB78);
        shape(c,p,SACK_RIM,0xFFF1C26A);
        rr(c,p,-31,3,13,16,6,SKIN[id]);rr(c,p,20,5,13,16,6,SKIN[id]);
        c.restoreToCount(saved);
    }
    private static void leg(Canvas c,Paint p,int id,float x,float y,float angle,float thigh,float shin,float knee,float width,boolean near){
        int s=c.save();c.translate(x,y);c.rotate(angle);
        rr(c,p,-width/2,-5,width,thigh+10,width/2,PANTS[id]);
        c.translate(0,thigh);c.rotate(knee);rr(c,p,-width/2,-width/2,width,shin+width,width/2,PANTS[id]);
        c.translate(0,shin);c.rotate(-angle*.35f-knee*.45f);if(!near)c.scale(.92f,.92f);
        shape(c,p,SHOE,SHOES[id]);shape(c,p,SOLE,id==3?0xFF972982:id==1?0xFFE83E50:0xFF006A8B);
        c.restoreToCount(s);
    }
    private static void arm(Canvas c,Paint p,int id,int side,float shoulder,float elbow,float size,boolean near){
        int s=c.save();c.translate(side*(id==3?18:23),-9);c.rotate(shoulder);c.scale(size,size);
        int skin=near?SKIN[id]:SHADE[id];
        if(id==3){
            int sleeve=near?SHIRT[id]:0xFFF28ED1;
            rr(c,p,-8,-5,16,30,7,sleeve);
            c.translate(0,20);c.rotate(elbow);
            rr(c,p,-7.5f,-7,15,28,7,sleeve);
            rr(c,p,-7.5f,14.5f,15,6,1.5f,near?0xFFFFB6E6:0xFFFFABDF);
            rr(c,p,-6,19,12,13,5.5f,skin);circle(c,p,side*5.5f,24.5f,3.5f,skin);
            c.restoreToCount(s);return;
        }
        rr(c,p,-7,-5,14,29,7,skin);rr(c,p,-8,-5,16,17,7,SHIRT[id]);
        c.translate(0,20);c.rotate(elbow);rr(c,p,-6.5f,-7,13,29,6.5f,skin);
        // One chunky mitten and a thumb: readable at phone size.
        rr(c,p,-7,12,14,17,6.5f,skin);circle(c,p,side*6,19,4,skin);
        c.restoreToCount(s);
    }
    private static void head(Canvas c,Paint p,int id,float gaze,float blink,float expression){
        if(id==2){
            shape(c,p,BOB,0xFF4B241B);shape(c,p,NARA_FACE,SKIN[id]);shape(c,p,NARA_SIDE,SHADE[id]);
            circle(c,p,-24,5,6,SKIN[id]);circle(c,p,24,7,5,SHADE[id]);shape(c,p,NARA_FRINGE,0xFF4B241B);
            eye(c,p,-11,-2,15,17,gaze-1,blink);eye(c,p,7,-3,12,15,gaze-1,blink);
            line(c,p,-17,-12,-8,-14,3,INK);line(c,p,3,-15,13,-13,3,INK);
            int mouth=c.save();c.translate(0,10);c.scale(1,.3f+.7f*expression);c.translate(0,-10);
            shape(c,p,NARA_MOUTH,0xFF963D28);shape(c,p,NARA_TONGUE,0xFFF66B59);
            line(c,p,-5,11,8,10,2.6f,0xFFFFF9EE);
            c.restoreToCount(mouth);
        }else if(id==0){
            shape(c,p,BIMA_HEAD,SKIN[id]);shape(c,p,BIMA_SIDE,SHADE[id]);
            circle(c,p,-22,5,5,SKIN[id]);circle(c,p,22,4,4,SHADE[id]);shape(c,p,BIMA_HAIR,0xFF253F50);
            eye(c,p,-10,-2,14,14,gaze+1.4f,blink);eye(c,p,7,-3,10,11,gaze+1.5f,blink);
            line(c,p,-17,-13,-10,-15,2.7f,0xFF253F50);line(c,p,0,-17,11,-14,2.7f,0xFF253F50);
            int mouth=c.save();c.translate(0,14);c.scale(1,.4f+.6f*expression);c.translate(0,-14);
            oval(c,p,1,10,11,21,0xFF9F4630);oval(c,p,4,15,10,21,0xFFF26B52);c.restoreToCount(mouth);
        }else if(id==1){
            shape(c,p,RAKA_HEAD,SKIN[id]);shape(c,p,RAKA_SIDE,SHADE[id]);
            circle(c,p,-25,5,6,SKIN[id]);circle(c,p,26,4,5,SHADE[id]);shape(c,p,CURLS,0xFF49291F);
            // Raka's crescent smile-eyes are intentionally unlike the other three.
            arc(c,p,-20,-6,-4,5,205,130,3.2f,INK);arc(c,p,4,-9,21,3,205,130,3.2f,INK);
            int mouth=c.save();c.translate(0,10);c.scale(1,.7f+.3f*expression);c.translate(0,-10);
            shape(c,p,RAKA_MOUTH,0xFF7E3025);shape(c,p,RAKA_TONGUE,0xFFEC6252);shape(c,p,RAKA_TEETH,0xFFFFF6DE);c.restoreToCount(mouth);
        }else{
            shape(c,p,HIJAB,0xFFF49AD9);shape(c,p,HIJAB_SIDE,0xFFE174C2);shape(c,p,TARI_FACE,SKIN[id]);
            float look=1.2f+gaze*.35f;
            eye(c,p,-10,-2,13,11,look,blink);eye(c,p,7,-2,10,12,look,blink);
            arc(c,p,-18,-16,-6,-9,205,130,2.6f,0xFF66312B);
            arc(c,p,0,-16,12,-9,205,130,2.6f,0xFF66312B);
            int mouth=c.save();c.translate(0,10);c.scale(1,.8f+.2f*expression);c.translate(0,-10);
            shape(c,p,TARI_SMILE,0xFF9F443D);shape(c,p,TARI_TONGUE,0xFFF47E72);
            line(c,p,-2,10,9,9,2.2f,0xFFFFF9EE);c.restoreToCount(mouth);
            shape(c,p,HIJAB_FOLD,0xFFFFB6E6);
        }
    }
    private static void eye(Canvas c,Paint p,float x,float y,float w,float h,float gaze,float blink){
        int s=c.save();c.translate(x,y);
        int inner=c.save();c.scale(1,1-.91f*blink);
        int alpha=Math.round(255*Math.min(1,(1-blink)/.3f));
        fill(p,0xFFFFFCEF);p.setAlpha(alpha);c.drawRoundRect(-w/2,-h/2,w/2,h/2,w*.43f,w*.43f,p);
        fill(p,INK);p.setAlpha(alpha);c.drawOval(gaze-2.8f,-h*.24f,gaze+2.8f,h*.34f,p);c.restoreToCount(inner);
        if(blink>.6f){fill(p,INK);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.3f);p.setAlpha(Math.round(255*(blink-.6f)/.4f));c.drawArc(-w*.43f,-1.8f,w*.43f,3.8f,205,130,false,p);}
        c.restoreToCount(s);
    }
    private static float blink(float time,float center){float d=Math.abs(time-center)/.15f;if(d>=1)return 0;float v=(float)Math.cos(d*Math.PI/2);return v*v;}
    private static float acting(float time){
        float t=time%12;if(t<0)t+=12;
        if(t<1.2f)return 0;
        if(t<2.3f)return smooth((t-1.2f)/1.1f);
        if(t<4.2f)return 1;
        if(t<5.6f)return 1-1.65f*smooth((t-4.2f)/1.4f);
        if(t<8.1f)return -.65f;
        if(t<9.5f)return -.65f+.65f*smooth((t-8.1f)/1.4f);
        return 0;
    }
    private static float smooth(float t){return t*t*t*(t*(t*6-15)+10);}
    private static float sine(float phase){return (float)Math.sin(phase);}
    private static boolean finite(float v){return !Float.isNaN(v)&&!Float.isInfinite(v);}
    private static void fill(Paint p,int color){p.setColor(color);p.setStyle(Paint.Style.FILL);p.setAlpha(255);}
    private static void shape(Canvas c,Paint p,Path path,int color){fill(p,color);c.drawPath(path,p);}
    private static void rr(Canvas c,Paint p,float x,float y,float w,float h,float r,int color){fill(p,color);c.drawRoundRect(x,y,x+w,y+h,r,r,p);}
    private static void oval(Canvas c,Paint p,float l,float t,float r,float b,int color){fill(p,color);c.drawOval(l,t,r,b,p);}
    private static void circle(Canvas c,Paint p,float x,float y,float r,int color){fill(p,color);c.drawCircle(x,y,r,p);}
    private static void limb(Canvas c,Paint p,float x1,float y1,float x2,float y2,float width,int color){line(c,p,x1,y1,x2,y2,width,color);}
    private static void line(Canvas c,Paint p,float x1,float y1,float x2,float y2,float width,int color){fill(p,color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width);c.drawLine(x1,y1,x2,y2,p);}
    private static void arc(Canvas c,Paint p,float l,float t,float r,float b,float start,float sweep,float width,int color){fill(p,color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width);c.drawArc(l,t,r,b,start,sweep,false,p);}
    /** Small authoring parser used only once while immutable cached paths initialize. */
    private static Path path(String source){
        ArrayList<String> tokens=new ArrayList<>();Matcher m=Pattern.compile("[MLQCZ]|[-+]?(?:[0-9]*\\.)?[0-9]+").matcher(source);
        while(m.find())tokens.add(m.group());Path p=new Path();int i=0;
        while(i<tokens.size()){
            String op=tokens.get(i++);
            if(op.equals("Z")){p.close();continue;}
            int n=op.equals("C")?6:op.equals("Q")?4:2;float[] v=new float[n];for(int j=0;j<n;j++)v[j]=Float.parseFloat(tokens.get(i++));
            if(op.equals("M"))p.moveTo(v[0],v[1]);else if(op.equals("L"))p.lineTo(v[0],v[1]);else if(op.equals("Q"))p.quadTo(v[0],v[1],v[2],v[3]);else if(op.equals("C"))p.cubicTo(v[0],v[1],v[2],v[3],v[4],v[5]);
        }return p;
    }
}
