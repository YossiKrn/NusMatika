package id.nusmatika.dakon;

/** Reference coordinates exclude Android's real status and navigation bars. */
public final class BoardGeometry {
    private BoardGeometry() {}
    public static int[] visibleSeeds(int[] seeds) {
        int[] visible=java.util.Arrays.copyOf(seeds,Math.min(4,seeds.length));
        if(seeds.length<=4)return visible;
        boolean allBlue=false,allCoral=false,visibleBlue=false,visibleCoral=false;
        for(int kind:seeds){allBlue|=kind==0;allCoral|=kind==1;}
        for(int kind:visible){visibleBlue|=kind==0;visibleCoral|=kind==1;}
        if(allBlue&&!visibleBlue)visible[3]=0;
        if(allCoral&&!visibleCoral)visible[3]=1;
        return visible;
    }
    public static float centerX(int hole) { return 45+((hole-1)%6)*54; }
    public static float centerY(int hole) { return 244+((hole-1)/6)*68; }
    public static int hitHole(float x, float y) {
        if(!Float.isFinite(x)||!Float.isFinite(y)||x<18||x>=342||y<220||y>=492)return 0;
        return 1+(int)((x-18)/54)+(int)((y-220)/68)*6;
    }
    public static int hitTool(float x, float y) {
        if(!Float.isFinite(x)||!Float.isFinite(y)||x<16||x>=344||y<534||y>582)return -1;
        int tool=(int)((x-16)/56);
        return (tool==0||tool==1||tool==4||tool==5)&&x<=16+tool*56+48?tool:-1;
    }
}
