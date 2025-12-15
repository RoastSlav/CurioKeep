package org.rostislav.curiokeep.modules;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
public class ModuleService {

    private final ModuleLoadTx moduleLoadTx;

    public ModuleService(ModuleLoadTx moduleLoadTx) {
        this.moduleLoadTx = moduleLoadTx;
    }

    /**
     * Loads builtin module XMLs from src/main/resources/modules/*.xml
     * Validates XSD, parses, runs semantic checks, and imports into DB.
     */
    public void loadAllModules() throws Exception {
        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:modules/*.xml");

        List<Resource> ordered = Arrays.stream(resources)
                .sorted(Comparator.comparing(r -> {
                    String fn = r.getFilename();
                    return fn != null ? fn : r.getDescription();
                }))
                .toList();

        List<RuntimeException> failures = new ArrayList<>();

        for (Resource r : ordered) {
            String sourceName = (r.getFilename() != null) ? r.getFilename() : r.getDescription();
            try {
                moduleLoadTx.loadOneModule(r, sourceName);
            } catch (RuntimeException ex) {
                failures.add(new IllegalStateException("Failed loading builtin module: " + sourceName, ex));
            }
        }

        if (!failures.isEmpty()) {
            IllegalStateException combined = new IllegalStateException(
                    "Builtin module load failed for " + failures.size() + " module(s). See suppressed exceptions."
            );
            failures.forEach(combined::addSuppressed);
            throw combined;
        }
    }

}
