package id.nusmatika.dakon;
public final class EngklekTest {
    private static int checks;
    private static void verify(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        long[] hexominoes = {34636833L,1082403L,1082465L,1084449L,33891L,35875L,99363L,33831L,35937L,34017L,2196513L,3171L,1127L,1251L,68643L,3111L,68705L,1253L,1505L,2165793L,72737L,199713L,138273L,2275L,6243L,67809L,71777L,198753L,2277L,2529L,137313L,4577L,8673L,67810L,71778L};
        java.util.Set<String> valid = new java.util.HashSet<>();
        for (long mask : hexominoes) {
            EngklekNet.Result result = EngklekNet.inspect(mask, false);
            if (result.valid) valid.add(EngklekNet.canonical(mask));
        }
        verify(valid.size() == 11, "All 35 free hexominoes yield exactly 11 cube nets");
        verify(EngklekNet.inspect(EngklekCatalog.CUBE, false).valid, "Cube sample valid");
        verify(EngklekNet.inspect(EngklekCatalog.BOX, true).valid, "Measured cuboid sample valid");
        verify(EngklekNet.inspect(EngklekCatalog.SECOND_CUBE, false).valid, "Second cube valid");
        verify(!EngklekNet.canonical(EngklekCatalog.CUBE).equals(
                EngklekNet.canonical(EngklekCatalog.SECOND_CUBE)), "Alternative cube is distinct");
        verify(!EngklekNet.inspect(EngklekCatalog.RECTANGLE, false).valid, "Rectangle cannot fold");
        verify(!EngklekNet.inspect(EngklekCatalog.SECOND_CUBE, true).valid,
                "Valid cube topology with incompatible measured faces rejected as cuboid");
        verify(!EngklekNet.inspect(EngklekNet.cells(0,0,1,1,2,2,3,3,4,4,4,5),false).valid,
                "Corner-only contacts are disconnected");
        for (int stage=0;stage<3;stage++) {
            EngklekSession session = new EngklekSession(stage);
            for (int task=0;task<EngklekCatalog.count(stage);task++) {
                verify(!session.advance(), "No advance before answer");
                EngklekCatalog.Task item=session.task();
                if (item.kind==EngklekCatalog.OBSERVE) {
                    session.observed(false);
                    verify(!session.passed(),"Unfolding alone cannot finish observation");
                    session.observed(true);
                    verify(!session.passed(),"Folding alone cannot finish observation");
                    session=EngklekSession.restore(stage,session.encode());
                    session.observed(false);
                } else if (item.editable()) {
                    long answer=item.kind==EngklekCatalog.OTHER_NET?EngklekCatalog.SECOND_CUBE:item.reference;
                    for(int cell=0;cell<30;cell++) if(EngklekNet.contains(answer & ~item.initial,cell))session.toggle(cell);
                    if(item.initial!=0){
                        int fixed=Long.numberOfTrailingZeros(item.initial);
                        long previous=session.selected();session.toggle(fixed);
                        verify(session.selected()==previous,"Given faces cannot be removed");
                    }
                    session.check();
                } else {
                    session.choose(item.correctChoice);
                    session.giveReason((item.correctReason+1)%4);
                    session.check();
                    verify(!session.passed(),"Correct answer needs correct reasoning");
                    session.giveReason(item.correctReason);
                    session.check();
                }
                verify(session.passed(),"Task solved stage="+stage+" task="+task);
                EngklekSession restored=EngklekSession.restore(stage,session.encode());
                verify(restored.passed()&&restored.selected()==session.selected(),"Passing state restores");
                session=restored;
                verify(session.advance(),"Advance after answer");
                session=EngklekSession.restore(stage,session.encode());
            }
            verify(session.complete(),"All three stages complete");
        }
        EngklekSession novel=new EngklekSession(2);
        for(int cell=0;cell<30;cell++)if(EngklekNet.contains(EngklekCatalog.CUBE,cell))novel.toggle(cell);
        novel.check();verify(!novel.passed(),"Original sample cannot count as new");
        novel.resetTask();
        for(int cell=0;cell<30;cell++)if(EngklekNet.contains(EngklekCatalog.SECOND_CUBE,cell))novel.toggle(cell);
        novel.check();verify(novel.passed()&&novel.discoveredCount()==2,"Discover new cube");
        novel.resetTask();
        for(int cell=0;cell<30;cell++)if(EngklekNet.contains(EngklekCatalog.SECOND_CUBE,cell))novel.toggle(cell);
        novel.check();verify(!novel.passed()&&novel.discoveredCount()==2,"Duplicate cannot count again");
        String[] corrupt={"", "EN1|0|-1|0|0|0|0|-1|-1|", "EN1|2|0|-1|1|1|1|0|0|", "EN1|2|999|0|0|0|0|-1|-1|"};
        for(String value:corrupt)verify(!EngklekSession.restore(2,value).complete(),"Corrupt snapshot resets");
        java.util.Random random=new java.util.Random(17);
        for(int i=0;i<10000;i++){
            long mask=random.nextLong()&EngklekNet.BOARD_MASK;
            EngklekNet.Result result=EngklekNet.inspect(mask,i%2==0);
            if(result.valid)verify(Long.bitCount(mask)==6&&result.faces.size()==6,"Valid always has six connected faces");
            String value="EN1|2|0|"+mask+"|1|1|1|-1|-1|";
            verify(!EngklekSession.restore(2,value).complete(),"Random state cannot skip whole course");
        }
        System.out.println("PASS "+checks+" checks; 35 hexominoes, 11 cube nets, measured cuboids and full 3N sessions");
    }
}
