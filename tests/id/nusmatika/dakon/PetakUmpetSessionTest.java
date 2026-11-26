package id.nusmatika.dakon;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Executable checks for mathematical geometry, collection conservation and replay. */
public final class PetakUmpetSessionTest {
    private static int assertions;
    private PetakUmpetSessionTest(){}
    public static void main(String[] args){
        assemblyGeometry();searchLessons();countingAndAssembly();snapshots();progressAndReplay();
        System.out.println("PASS: Petak Umpet, "+assertions+" assertions");
    }
    private static void assemblyGeometry(){
        Set<String> occupied=new HashSet<String>();
        int blocks=0,cubes=0;
        for(int i=0;i<PetakUmpetCatalog.ITEM_COUNT;i++){
            PetakUmpetCatalog.Piece p=PetakUmpetCatalog.piece(i);
            check(p.depth==1,"all sample pieces have one-unit depth");
            if(p.kind==PetakUmpetCatalog.Kind.BALOK){blocks++;check(p.volume()==2,"a rotated block retains its two-unit volume");}
            else{cubes++;check(p.width==p.height&&p.height==p.depth,"a cube has equal edge lengths");}
            for(int x=p.x;x<p.x+p.width;x++)for(int y=p.y;y<p.y+p.depth;y++)for(int z=p.z;z<p.z+p.height;z++){
                check(occupied.add(cell(x,y,z)),"sample pieces never overlap");
            }
        }
        check(blocks==4&&cubes==2,"numeric keys count solids, not visible faces or unit cubes");
        check(occupied.size()==10,"four blocks plus two cubes conserve ten unit volumes");
        for(int x=0;x<5;x++)check(occupied.contains(cell(x,0,0)),"continuous horizontal arm of L");
        for(int z=0;z<6;z++)check(occupied.contains(cell(0,0,z)),"continuous vertical arm of L");
        for(String position:occupied){
            String[] xyz=position.split(",");int x=Integer.parseInt(xyz[0]),y=Integer.parseInt(xyz[1]),z=Integer.parseInt(xyz[2]);
            check(z==0||occupied.contains(cell(x,y,z-1)),"every elevated unit is supported");
        }
        Set<String> visited=new HashSet<String>();ArrayDeque<String> pending=new ArrayDeque<String>();pending.add(cell(0,0,0));
        while(!pending.isEmpty()){
            String current=pending.removeFirst();if(!visited.add(current))continue;
            String[] xyz=current.split(",");int x=Integer.parseInt(xyz[0]),y=Integer.parseInt(xyz[1]),z=Integer.parseInt(xyz[2]);
            for(int[] delta:new int[][]{{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}}){
                String next=cell(x+delta[0],y+delta[1],z+delta[2]);if(occupied.contains(next)&&!visited.contains(next))pending.add(next);
            }
        }
        check(visited.equals(occupied),"L is one connected solid");
        for(int level=0;level<2;level++)for(int i=0;i<PetakUmpetCatalog.ITEM_COUNT;i++){
            PetakUmpetCatalog.Bounds b=PetakUmpetCatalog.sceneBounds(level,i);
            check(b.left>=0&&b.top>=0&&b.right<=360&&b.bottom<=310,"every piece fits the actual scene viewport");
            check(PetakUmpetCatalog.hitItem(level,b.centerX(),b.centerY())==i,"drawn piece and touch geometry agree");
            for(int j=i+1;j<PetakUmpetCatalog.ITEM_COUNT;j++){
                PetakUmpetCatalog.Bounds other=PetakUmpetCatalog.sceneBounds(level,j);
                check(b.right<other.left||other.right<b.left||b.bottom<other.top||other.bottom<b.top,"search pieces have distinct hit regions");
            }
        }
    }
    private static void searchLessons(){
        Random random=new Random(18);
        for(int level=0;level<2;level++)for(int iteration=0;iteration<80;iteration++){
            PetakUmpetSession s=new PetakUmpetSession(level);
            check(s.showsMarkers()==(level==0),"only Niteni reveals markers");
            check(!s.finish(),"an incomplete search cannot earn a completed level");
            check(!s.find(-1)&&!s.find(6),"invalid or decorative objects are not collectible");
            List<Integer> order=new ArrayList<Integer>();for(int i=0;i<6;i++)order.add(i);Collections.shuffle(order,random);
            int found=0;
            for(int id:order){
                check(s.find(id),"any search order is accepted");found++;
                check(!s.find(id)&&s.foundCount()==found,"a repeated click cannot duplicate inventory");
                String before=s.encode();s=PetakUmpetSession.restore(level,before);
                check(before.equals(s.encode())&&s.isFound(id),"leaving and reopening preserves collection");
                check(!s.isComplete(),"finding the last piece never auto-enters the next lesson");
            }
            check(s.foundCount(PetakUmpetCatalog.Kind.BALOK)==4&&s.foundCount(PetakUmpetCatalog.Kind.KUBUS)==2,"all six found shapes form the complete assembly kit");
            check(s.primaryEnabled()&&s.finish()&&s.isComplete(),"explicit completion after collection");
            check(!s.showsMarkers()&&!s.find(0),"completion hides all search interactions");
        }
    }
    private static void countingAndAssembly(){
        PetakUmpetSession s=new PetakUmpetSession(2);
        check(!s.showsMarkers()&&!s.find(0),"assembly cannot collect extra pieces");
        check(!s.finish()&&!s.assemble(),"no shortcut from a blank worksheet to completion");
        for(String bad:new String[]{"","-1","2.0","dua","１２","9999999999999999"}){
            s.setBalokAnswer(bad);s.setKubusAnswer("2");
            check(s.checkAnswers()==PetakUmpetSession.Feedback.INVALID,"invalid counts receive explicit feedback");
        }
        s.setBalokAnswer("3");s.setKubusAnswer("2");String before=s.getBalokAnswer()+s.getKubusAnswer();
        check(s.checkAnswers()==PetakUmpetSession.Feedback.INCORRECT,"incorrect numeric decomposition is rejected");
        check(before.equals(s.getBalokAnswer()+s.getKubusAnswer()),"wrong checks retain all written answers");
        check(!s.assemble(),"incorrect counts cannot assemble the L");
        s.setBalokAnswer("4");
        check(s.getFeedback()==PetakUmpetSession.Feedback.NONE&&!s.assemble(),"typing the right answer never auto-checks it");
        check(s.checkAnswers()==PetakUmpetSession.Feedback.CORRECT,"correct block and cube counts accepted");
        check(s.assemble()&&!s.isComplete(),"assembly is separate from level completion");
        s.setBalokAnswer("0");check(s.getBalokAnswer().equals("4"),"already assembled work cannot be changed underneath the visual");
        check(s.finish()&&s.isComplete(),"explicit finish completes Nambahi");
        check(s.encode().equals(PetakUmpetSession.restore(2,s.encode()).encode()),"completed assembly survives restart");
        PetakUmpetSession faces=new PetakUmpetSession(2);faces.setBalokAnswer("8");faces.setKubusAnswer("2");
        check(faces.checkAnswers()==PetakUmpetSession.Feedback.INCORRECT,"unit volume is not mistaken for number of blocks");
    }
    private static void snapshots(){
        for(int level=0;level<3;level++){
            for(String invalid:new String[]{"", "PU2|0|0|||0|0", "PU1|0|64|||0|0", "PU1|0|0|||1|1", "PU1|0|0|||0|1", "PU1|2|63|0033|0032|1|1", "PU1|2|0|0034|0032|1|1", "PU1|2|63|zzzz|0032|0|0", "PU1|2|63|0034|0032|0|1", "PU1|9|0|||0|0"}){
                PetakUmpetSession restored=PetakUmpetSession.restore(level,invalid);
                check(!restored.isComplete()&&!restored.isAssembled(),"malformed or inconsistent saves fail atomically");
            }
        }
        PetakUmpetSession s=new PetakUmpetSession(2);s.setBalokAnswer("3");s.setKubusAnswer("2");s.checkAnswers();
        PetakUmpetSession restored=PetakUmpetSession.restore(2,s.encode());
        check(restored.getBalokAnswer().equals("3")&&restored.getKubusAnswer().equals("2"),"draft answers survive process death");
        check(restored.getFeedback()==PetakUmpetSession.Feedback.NONE,"transient wrong feedback is not restored as earned progress");
    }
    private static void progressAndReplay(){
        PetakUmpetProgress p=new PetakUmpetProgress(null,0);
        check(p.isUnlocked(0)&&!p.isUnlocked(1)&&!p.isUnlocked(2)&&p.inventoryMask()==0,"only the first node is initially open");
        PetakUmpetSession forged=new PetakUmpetSession(2);forged.setBalokAnswer("4");forged.setKubusAnswer("2");forged.checkAnswers();forged.assemble();forged.finish();p.saveSession(forged);
        check(!p.hasSession(2)&&p.getCompletedMask()==0,"locked work cannot create unlocks");
        for(int level=0;level<2;level++){
            PetakUmpetSession s=p.loadSession(level);for(int id=0;id<6;id++)s.find(id);s.finish();p.saveSession(s);
            check(p.isUnlocked(level+1),"completion unlocks exactly the next node");
        }
        check(p.inventoryMask()==63,"Nambahi inventory is earned by completing Nirokke");
        p.saveSession(forged);check(p.currentLevel()==2&&p.isCompleted(2),"last node completion is retained");
        p.restartSession(1);PetakUmpetSession replay=p.loadSession(1);replay.find(4);p.saveSession(replay);
        check(p.isCompleted(1)&&p.isUnlocked(2)&&p.inventoryMask()==63,"unfinished replay cannot remove a previously earned collection");
        String[] saved=new String[3];for(int i=0;i<3;i++)saved[i]=p.encodedSession(i);
        PetakUmpetProgress reopened=new PetakUmpetProgress(saved,p.getCompletedMask());
        check(reopened.loadSession(1).isFound(4)&&reopened.loadSession(1).foundCount()==1,"replay resumes at the exact found subset");
        check(reopened.isCompleted(2)&&reopened.isUnlocked(2),"later unlock history survives reopening during replay");
        check(new PetakUmpetProgress(null,4).getCompletedMask()==0,"a lone later flag cannot open earlier lessons");
    }
    private static String cell(int x,int y,int z){return x+","+y+","+z;}
    private static void check(boolean condition,String message){assertions++;if(!condition)throw new AssertionError(message);}
}
