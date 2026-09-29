package org.rostislav.curiokeep.modules;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rostislav.curiokeep.modules.contract.ModuleSource;
import org.rostislav.curiokeep.modules.importing.ModuleImportStorage;
import org.springframework.core.io.Resource;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModuleServiceTest {

    @Mock
    ModuleLoadTx loader;
    @Mock
    ModuleDefinitionRepository definitions;
    @Mock
    ModuleImportStorage storage;

    @Test
    void anImportedModuleThatNeedsANewerApplicationIsSkippedInsteadOfStoppingStartup() throws Exception {
        when(storage.listXmlFiles()).thenReturn(List.of(Path.of("newer.xml"), Path.of("fine.xml")));
        doNothing().when(loader).loadOneModule(any(Resource.class), anyString(), eq(ModuleSource.BUILTIN));
        doNothing().when(loader).loadOneModule(any(Resource.class), eq("fine.xml"), eq(ModuleSource.IMPORTED));
        doThrow(new ModuleRequiresNewerAppException("needs a newer app"))
                .when(loader).loadOneModule(any(Resource.class), eq("newer.xml"), eq(ModuleSource.IMPORTED));

        new ModuleService(loader, definitions, storage).loadAllModules();

        verify(loader).loadOneModule(any(Resource.class), eq("fine.xml"), eq(ModuleSource.IMPORTED));
    }

    @Test
    void anImportedModuleThatIsInvalidStillStopsStartup() throws Exception {
        when(storage.listXmlFiles()).thenReturn(List.of(Path.of("broken.xml")));
        doNothing().when(loader).loadOneModule(any(Resource.class), anyString(), eq(ModuleSource.BUILTIN));
        doThrow(new IllegalStateException("must define state OWNED"))
                .when(loader).loadOneModule(any(Resource.class), eq("broken.xml"), eq(ModuleSource.IMPORTED));

        assertThatThrownBy(() -> new ModuleService(loader, definitions, storage).loadAllModules())
                .isInstanceOf(IllegalStateException.class)
                .satisfies(ex -> assertThat(ex.getSuppressed()).hasSize(1))
                .hasMessageContaining("Module load failed for 1 module(s)");
        verify(loader, atLeastOnce()).loadOneModule(any(Resource.class), anyString(), eq(ModuleSource.BUILTIN));
    }
}
