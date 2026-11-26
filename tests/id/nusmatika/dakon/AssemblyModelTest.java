package id.nusmatika.dakon;

import java.util.Random;

public final class AssemblyModelTest {
    private static int checks;
    private static void check(boolean ok, String label) {
        checks++;
        if (!ok) throw new AssertionError(label);
    }
    private static PetakUmpetAssemblyModel solved(int ox, int oy) {
        PetakUmpetAssemblyModel m = new PetakUmpetAssemblyModel();
        check(m.rotate(3) && m.rotate(5), "upright balok");
        check(m.place(0, ox, oy + 5), "base 1");
        check(m.place(1, ox, oy), "top cube");
        check(m.place(2, ox + 2, oy + 5), "base 2");
        check(m.place(3, ox, oy + 3), "upright 1");
        check(m.place(4, ox + 4, oy + 5), "end cube");
        check(m.place(5, ox, oy + 1), "upright 2");
        return m;
    }
    public static void main(String[] args) {
        PetakUmpetAssemblyModel m = new PetakUmpetAssemblyModel();
        check(m.placedCount() == 0 && !m.isSolved(), "empty board cannot pass");
        check(!m.rotate(1) && m.w[1] == 1 && m.h[1] == 1, "cube cannot change type");
        check(m.place(0, 1, 5), "first placement");
        check(m.magnet(2, 3.15f, 5.1f), "near full side magnet");
        check(!m.magnet(2, 3.4f, 5), "outside magnetic radius");
        check(!m.magnet(2, 3, 6), "corner contact is not side contact");
        check(!m.magnet(2, 1, 5), "overlap is not magnet");
        check(m.place(2, 3, 5) && m.adjacent(2, 3, 5), "attached piece");
        check(m.place(2, 4, 2), "attached piece can be pulled away");
        check(!m.adjacent(2, 4, 2) && m.x[0] == 1 && m.y[0] == 5,
                "detach moves only selected piece");
        int oldX = m.x[2], oldY = m.y[2];
        check(!m.place(2, 1, 5) && m.x[2] == oldX && m.y[2] == oldY,
                "overlap rejected without losing old position");
        check(!m.place(2, -1, 2) && !m.place(2, 6, 2), "outside board rejected");
        m = new PetakUmpetAssemblyModel();
        check(m.place(0, 0, 0) && m.place(1, 0, 1), "rotation fixture");
        check(!m.rotate(0) && m.w[0] == 2 && m.h[0] == 1, "blocked rotation rejected");
        m.placed[1] = false;
        check(m.rotate(0) && m.w[0] == 1 && m.h[0] == 2, "free rotation accepted");

        for (int x = 0; x <= 2; x++) for (int y = 0; y <= 1; y++) {
            m = solved(x, y);
            check(m.isSolved(), "valid L is translation independent");
            m.selected = 5;
            String snapshot = m.encode();
            PetakUmpetAssemblyModel restored = PetakUmpetAssemblyModel.restore(snapshot);
            check(restored != null && snapshot.equals(restored.encode()) && restored.isSolved(),
                    "layout and selection restored");
            // Interchange the two cubes without changing the occupied solid.
            int cx = m.x[1], cy = m.y[1];
            m.x[1] = m.x[4]; m.y[1] = m.y[4]; m.x[4] = cx; m.y[4] = cy;
            check(m.isSolved(), "identical pieces interchangeable");
            m.placed[4] = false;
            check(!m.isSolved(), "missing collected piece cannot pass");
            check(m.place(4, 6, 0) && !m.isSolved(), "all pieces alone do not prove correct shape");
        }
        m.reset();
        check(m.placedCount() == 0 && m.selected == -1 && m.w[3] == 2 && !m.isSolved(), "reset");
        check(PetakUmpetAssemblyModel.restore(null) == null, "null snapshot");
        check(PetakUmpetAssemblyModel.restore("DD1|7") == null, "invalid selection");
        String invalid = solved(0, 0).encode().replace("1,2,5,2,1", "1,0,5,2,1");
        check(PetakUmpetAssemblyModel.restore(invalid) == null, "overlapping snapshot rejected");
        invalid = new PetakUmpetAssemblyModel().encode().replace("0,0,0,2,1", "0,0,0,3,1");
        check(PetakUmpetAssemblyModel.restore(invalid) == null, "wrong dimensions rejected");

        Random random = new Random(318);
        for (int trial = 0; trial < 250; trial++) {
            m = new PetakUmpetAssemblyModel();
            for (int move = 0; move < 30; move++) {
                int item = random.nextInt(6);
                if (random.nextBoolean()) m.rotate(item);
                m.place(item, random.nextInt(9) - 1, random.nextInt(9) - 1);
                for (int i = 0; i < 6; i++)
                    if (m.placed[i]) check(m.fits(i, m.x[i], m.y[i], m.w[i], m.h[i]), "no overlap after edits");
                PetakUmpetAssemblyModel roundTrip =
                        PetakUmpetAssemblyModel.restore(m.encode());
                check(roundTrip != null && m.encode().equals(roundTrip.encode()), "random layout persistence");
            }
        }
        System.out.println("AssemblyModelTest passed: " + checks + " checks");
    }
}

