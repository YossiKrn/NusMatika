package id.nusmatika.dakon;

/** Deterministic vector/typing timeline. The view supplies foreground-only time. */
public final class JkwTutorialMotion {
    public static final int DIVIDE=1,MULTIPLY=2;
    private JkwTutorialMotion(){}
    public static final class Frame {
        public final float divideLine,multiplyLine,divideArrow,multiplyArrow,calculationAlpha,pulse;
        public final int visibleChars,activeOperation;
        public final boolean ready;
        private Frame(float dl,float ml,float da,float ma,float ca,float p,int chars,int operation,boolean done){
            divideLine=dl;multiplyLine=ml;divideArrow=da;multiplyArrow=ma;calculationAlpha=ca;pulse=p;
            visibleChars=chars;activeOperation=operation;ready=done;
        }
    }
    public static Frame at(int step,long elapsedMillis,boolean reduced,String text){
        String copy=text==null?"":text;
        int page=Math.max(0,Math.min(4,step)),operation=page==1||page==2?MULTIPLY:DIVIDE;
        boolean intro=page<2;
        long time=reduced?1000000L:Math.max(0,elapsedMillis),delay=intro?1050:260;
        int count=copy.codePointCount(0,copy.length());
        int visible=reduced?count:(int)Math.min(count,Math.max(0,time-delay)/34);
        int chars=copy.offsetByCodePoints(0,visible);
        long textEnd=delay+count*34L;
        float reveal=ease(time,200,700),arrow=ease(time,intro?800:650,intro?400:600);
        float dl=page==0?reveal:1,ml=page==0?0:page==1?reveal:1;
        float da=operation==DIVIDE?arrow:.38f,ma=operation==MULTIPLY?arrow:page==0?0:.38f;
        float calculation=intro?0:ease(time,textEnd+180,280);
        float pulse=reduced?0:(float)((1-Math.cos(Math.max(0,time-1400)%1800/1800.0*Math.PI*2))*.5);
        return new Frame(dl,ml,da,ma,calculation,pulse,chars,operation,reduced||time>=textEnd+(intro?300:600));
    }
    private static float ease(long time,long start,long duration){
        if(time<=start)return 0;if(time>=start+duration)return 1;
        float t=(time-start)/(float)duration;return t*t*(3-2*t);
    }
}
