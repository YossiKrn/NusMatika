package id.nusmatika.dakon;

public final class BoardGeometryTest {
    private static int assertions;
    private static void eq(int expected,int actual) {
        assertions++;
        if(expected!=actual)throw new AssertionError("Expected "+expected+", got "+actual);
    }
    public static void main(String[] args) {
        // A shifted/transposed grid would select a different hole from the one touched.
        eq(1,BoardGeometry.hitHole(45,244));
        eq(6,BoardGeometry.hitHole(315,244));
        eq(7,BoardGeometry.hitHole(45,312));
        eq(12,BoardGeometry.hitHole(315,312));
        eq(19,BoardGeometry.hitHole(45,448));
        eq(24,BoardGeometry.hitHole(315,448));
        eq(24,BoardGeometry.hitHole(315,486)); // Number is a touch target too.
        eq(0,BoardGeometry.hitHole(15,244));
        eq(0,BoardGeometry.hitHole(344,244));
        eq(0,BoardGeometry.hitHole(45,198));
        eq(0,BoardGeometry.hitHole(45,510));
        eq(0,BoardGeometry.hitHole(Float.NaN,244));
        eq(0,BoardGeometry.hitHole(45,Float.POSITIVE_INFINITY));
        for(int hole=1;hole<=24;hole++)eq(hole,BoardGeometry.hitHole(BoardGeometry.centerX(hole),BoardGeometry.centerY(hole)));
        eq(0,BoardGeometry.hitTool(40,558));
        eq(1,BoardGeometry.hitTool(96,558));
        // Removed tools leave an inert spacer between the seeds and actions.
        eq(-1,BoardGeometry.hitTool(152,558));
        eq(-1,BoardGeometry.hitTool(208,558));
        eq(-1,BoardGeometry.hitTool(128,534));
        eq(-1,BoardGeometry.hitTool(176,582));
        eq(-1,BoardGeometry.hitTool(184,534));
        eq(-1,BoardGeometry.hitTool(232,582));
        eq(4,BoardGeometry.hitTool(264,558));
        eq(5,BoardGeometry.hitTool(320,558));
        eq(0,BoardGeometry.hitTool(16,534));
        eq(1,BoardGeometry.hitTool(120,582));
        eq(4,BoardGeometry.hitTool(240,534));
        eq(5,BoardGeometry.hitTool(344-.01f,582));
        eq(-1,BoardGeometry.hitTool(68,558)); // Gap must not trigger a destructive tool.
        eq(-1,BoardGeometry.hitTool(320,600));
        // A compact pile must not hide an entire kind that the child placed.
        int[] mixed=BoardGeometry.visibleSeeds(new int[]{0,0,0,0,0,0,1});
        eq(4,mixed.length);eq(0,mixed[0]);eq(0,mixed[1]);eq(0,mixed[2]);eq(1,mixed[3]);
        int[] reversed=BoardGeometry.visibleSeeds(new int[]{1,1,1,1,1,0});
        eq(4,reversed.length);eq(1,reversed[0]);eq(0,reversed[3]);
        int[] pair={1,0};int[] visiblePair=BoardGeometry.visibleSeeds(pair);
        eq(2,visiblePair.length);eq(1,visiblePair[0]);eq(0,visiblePair[1]);
        visiblePair[0]=0;eq(1,pair[0]);
        eq(0,BoardGeometry.visibleSeeds(new int[0]).length);
        System.out.println("BoardGeometry: "+assertions+" assertions passed");
    }
}
