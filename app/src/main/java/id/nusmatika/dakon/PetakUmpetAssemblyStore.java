package id.nusmatika.dakon;

import android.content.Context;
import android.content.SharedPreferences;

/** Menyimpan posisi bangun. Nama dan format penyimpanan tetap kompatibel dengan versi sebelumnya. */
public final class PetakUmpetAssemblyStore {
    private final SharedPreferences preferences;

    PetakUmpetAssemblyStore(Context context) {
        preferences = context.getSharedPreferences(
                "nusmatika-petak-umpet-susun", Context.MODE_PRIVATE);
    }

    PetakUmpetAssemblyModel load() {
        return PetakUmpetAssemblyModel.restore(preferences.getString("board", null));
    }

    void save(PetakUmpetAssemblyModel model) {
        preferences.edit().putString("board", model.encode()).apply();
    }
}
