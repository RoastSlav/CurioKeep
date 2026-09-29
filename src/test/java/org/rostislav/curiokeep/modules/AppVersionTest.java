package org.rostislav.curiokeep.modules;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppVersionTest {

    @Test
    void aVersionSatisfiesEqualAndOlderRequirementsButNotNewerOnes() {
        AppVersion app = AppVersion.of("1.4.2");

        assertThat(app.satisfies("1.4.2")).isTrue();
        assertThat(app.satisfies("1.4.0")).isTrue();
        assertThat(app.satisfies("0.9.9")).isTrue();
        assertThat(app.satisfies("1.4.3")).isFalse();
        assertThat(app.satisfies("1.10.0")).isFalse();
        assertThat(app.satisfies("2.0.0")).isFalse();
    }

    @Test
    void aSnapshotBuildCountsAsItsReleaseNumber() {
        assertThat(AppVersion.of("1.4.2-SNAPSHOT").satisfies("1.4.2")).isTrue();
        assertThat(AppVersion.of("1.4.2-SNAPSHOT").satisfies("1.4.3")).isFalse();
    }

    @Test
    void nothingIsRefusedWhenNoRequirementIsDeclaredOrTheApplicationVersionIsUnknown() {
        assertThat(AppVersion.of("1.4.2").satisfies(null)).isTrue();
        assertThat(AppVersion.of(null).satisfies("9.9.9")).isTrue();
        assertThat(AppVersion.of("unknown").satisfies("9.9.9")).isTrue();
    }

    @Test
    void theVersionComesFromTheBuildInformationWhenThereIsSome() {
        Properties properties = new Properties();
        properties.setProperty("version", "2.1.0");
        @SuppressWarnings("unchecked")
        ObjectProvider<BuildProperties> present = mock(ObjectProvider.class);
        when(present.getIfAvailable()).thenReturn(new BuildProperties(properties));
        @SuppressWarnings("unchecked")
        ObjectProvider<BuildProperties> absent = mock(ObjectProvider.class);

        assertThat(new AppVersion(present).value()).isEqualTo("2.1.0");
        assertThat(new AppVersion(absent).value()).isNull();
    }
}
