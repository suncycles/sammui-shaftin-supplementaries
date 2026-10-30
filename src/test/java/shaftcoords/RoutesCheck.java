package shaftcoords;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Self-check for Routes, run by build.ps1 with the project folder (where the route .txt files are); exits non-zero on failure. */
public final class RoutesCheck {
    // SkyHanni 9.2.0's MineshaftDetection.MineshaftType, all of them
    static final String[] TYPES = {"TOPA_1", "TOPA_2", "SAPP_1", "SAPP_2", "AMET_1", "AMET_2", "AMBE_1", "AMBE_2", "JADE_1",
            "JADE_2", "TITA_1", "UMBE_1", "TUNG_1", "FAIR_1", "RUBY_1", "RUBY_2", "RUBY_C", "ONYX_1", "ONYX_2", "ONYX_C",
            "AQUA_1", "AQUA_2", "AQUA_C", "CITR_1", "CITR_2", "CITR_C", "PERI_1", "PERI_2", "PERI_C", "JASP_1", "JASP_C",
            "OPAL_1", "OPAL_C", "LITT_L"};

    public static void main(String[] args) throws Exception {
        Path dir = Path.of(args[0]);
        int fails = 0;
        Map<String, String> want = Map.ofEntries(Map.entry("PERI_C", "crystal_shaft.txt"), Map.entry("RUBY_C", "crystal_shaft.txt"),
                Map.entry("PERI_2", "peridot_2.txt"), Map.entry("AMBE_1", "amber_shaft.txt"), Map.entry("UMBE_1", "umber_shaft.txt"),
                Map.entry("TUNG_1", "tungsten_shaft.txt"), Map.entry("JASP_1", "jasper_shaft.txt"), Map.entry("TOPA_2", "topaz_shaft.txt"),
                Map.entry("JADE_1", "jade_shaft.txt"), Map.entry("RUBY_1", "ruby_1.txt"), Map.entry("RUBY_2", "ruby_2.txt"),
                Map.entry("ONYX_1", "onyx_1.txt"), Map.entry("AQUA_2", "aquamarine_2.txt"), Map.entry("CITR_1", "citrine_1.txt"),
                Map.entry("PERI_1", "peridot_1.txt"), Map.entry("ONYX_C", "crystal_shaft.txt"));
        for (var e : want.entrySet()) {
            if (!e.getValue().equals(Routes.file(e.getKey()))) {
                System.out.println("FAIL  " + e.getKey() + " -> " + Routes.file(e.getKey()) + ", wanted " + e.getValue());
                fails++;
            }
        }
        for (String none : new String[]{"TITA_1", "FAIR_1", "LITT_L"}) {
            if (Routes.file(none) != null) {
                System.out.println("FAIL  " + none + " has no route, got " + Routes.file(none));
                fails++;
            }
        }
        // every type's route is there, and every route file is some type's
        Set<String> used = new HashSet<>();
        for (String t : TYPES) {
            String f = Routes.file(t);
            if (f == null) continue;
            used.add(f);
            if (!Files.exists(dir.resolve(f))) {
                System.out.println("FAIL  " + t + " -> " + f + ", which isn't in " + dir);
                fails++;
            }
        }
        try (var files = Files.list(dir)) {
            for (Path p : files.filter(p -> p.getFileName().toString().endsWith(".txt")).toList()) {
                if (!used.contains(p.getFileName().toString())) {
                    System.out.println("FAIL  " + p.getFileName() + " isn't any mineshaft type's route");
                    fails++;
                }
            }
        }
        if (!new HashSet<>(Routes.files()).equals(used)) {  // the settings screen's route buttons
            System.out.println("FAIL  Routes.files() " + Routes.files() + " isn't the types' routes " + used);
            fails++;
        }
        System.out.println(fails == 0 ? "ok    routes: " + used.size() + " files for " + TYPES.length + " mineshaft types" : fails + " failed");
        System.exit(fails == 0 ? 0 : 1);
    }
}
