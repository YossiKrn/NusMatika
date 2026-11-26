package id.nusmatika.dakon;

/** Shared mathematical geometry: search objects and the six pieces of one solid L. */
public final class PetakUmpetCatalog {
    public enum Kind { BALOK, KUBUS }
    public static final int ITEM_COUNT = 6;
    public static final int ALL_ITEMS = (1 << ITEM_COUNT) - 1;
    public static final float SCENE_WIDTH = 360f, SCENE_HEIGHT = 310f;

    public static final class Piece {
        public final Kind kind;
        public final int x, y, z, width, depth, height;
        private Piece(Kind kind, int x, int y, int z, int width, int depth, int height) {
            this.kind=kind; this.x=x; this.y=y; this.z=z;
            this.width=width; this.depth=depth; this.height=height;
        }
        public String label() { return kind == Kind.BALOK ? "Balok" : "Kubus"; }
        public int volume() { return width * depth * height; }
    }

    public static final class Bounds {
        public final float left, top, right, bottom;
        private Bounds(float left, float top, float right, float bottom) {
            this.left=left; this.top=top; this.right=right; this.bottom=bottom;
        }
        public boolean contains(float x,float y) {
            return x>=left && x<=right && y>=top && y<=bottom;
        }
        public float centerX() { return (left+right)*.5f; }
        public float centerY() { return (top+bottom)*.5f; }
    }

    // Four 2x1x1 blocks (two are rotated upright), two 1x1x1 cubes.
    // Occupied unit cells form an L, with no overlap or unsupported pieces.
    private static final Piece[] PIECES = {
        new Piece(Kind.BALOK,0,0,0,2,1,1),
        new Piece(Kind.KUBUS,0,0,5,1,1,1),
        new Piece(Kind.BALOK,2,0,0,2,1,1),
        new Piece(Kind.BALOK,0,0,1,1,1,2),
        new Piece(Kind.KUBUS,4,0,0,1,1,1),
        new Piece(Kind.BALOK,0,0,3,1,1,2)
    };
    // Front-face top-left positions; lessons use different hiding places.
    private static final float[][][] POSITIONS = {
        {{41,112},{273,87},{149,167},{40,230},{279,227},{163,272}},
        {{32,223},{157,89},{278,156},{157,267},{277,251},{64,130}}
    };
    private static final String[][] LOCATIONS = {
        {"dekat pohon", "dekat jendela", "di depan bangku", "di dekat pot", "dekat pagar", "di rumput"},
        {"dekat pot", "dekat dinding", "dekat bangku", "di rumput", "dekat pagar", "dekat pohon"}
    };

    private PetakUmpetCatalog() { }
    public static Piece piece(int item) { requireItem(item); return PIECES[item]; }
    static int[] assemblyDrawingOrder() {
        int[] order=new int[ITEM_COUNT];for(int i=0;i<ITEM_COUNT;i++)order[i]=i;
        // Draw supporting solids before those above them so their top faces are occluded.
        for(int i=1;i<ITEM_COUNT;i++){
            int item=order[i],j=i-1;
            while(j>=0 && PIECES[order[j]].z>PIECES[item].z){order[j+1]=order[j];j--;}
            order[j+1]=item;
        }
        return order;
    }
    public static boolean validItem(int item) { return item>=0 && item<ITEM_COUNT; }
    public static int count(Kind kind,int mask) {
        int count=0;
        for(int i=0;i<ITEM_COUNT;i++) if((mask & (1<<i))!=0 && PIECES[i].kind==kind) count++;
        return count;
    }
    public static Bounds sceneBounds(int level,int item) {
        requireScene(level); requireItem(item);
        float x=POSITIONS[level][item][0],y=POSITIONS[level][item][1];
        float width=PIECES[item].kind==Kind.BALOK?44:25;
        return new Bounds(x,y-9,x+width+10,y+25);
    }
    public static String location(int level,int item) { requireScene(level);requireItem(item);return LOCATIONS[level][item]; }
    public static int hitItem(int level,float x,float y) {
        requireScene(level);
        for(int item=ITEM_COUNT-1;item>=0;item--) if(sceneBounds(level,item).contains(x,y))return item;
        return -1;
    }
    public static String levelTitle(int level) {
        return level==0?"Niteni":level==1?"Nirokke":level==2?"Nambahi":"";
    }
    private static void requireItem(int item) { if(!validItem(item))throw new IllegalArgumentException("Unknown hidden piece"); }
    private static void requireScene(int level) { if(level<0||level>1)throw new IllegalArgumentException("Not a search lesson"); }
}
