package id.nusmatika.dakon;

/** One coordinate source for the worksheet's drawing and native input views. */
public final class WorksheetLayout {
    private WorksheetLayout(){}
    public static float x(int field){return field<8?16+(field%4)*84:16;}
    public static float y(int field){return field<4?273:field<8?375:field==8?483:591;}
    public static float width(int field){return field<8?76:328;}
    public static float height(int field){return field==9?54:50;}
    public static int fieldAt(float x,float y){
        if(!Float.isFinite(x)||!Float.isFinite(y))return -1;
        for(int i=0;i<10;i++)if(x>=x(i)&&x<x(i)+width(i)&&y>=y(i)&&y<y(i)+height(i))return i;
        return -1;
    }
}
