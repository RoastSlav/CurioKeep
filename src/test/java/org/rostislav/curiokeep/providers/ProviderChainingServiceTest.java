package org.rostislav.curiokeep.providers;

import org.junit.jupiter.api.Test;
import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity.IdType;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderChainingServiceTest {

    /** Answers every lookup with the result its function builds, and remembers what it was asked. */
    private static final class FakeProvider implements MetadataProvider {
        final String key;
        final Set<IdType> supported;
        final Function<String, Optional<ProviderResult>> answer;
        final List<String> asked = new ArrayList<>();

        FakeProvider(String key, Set<IdType> supported, Function<String, Optional<ProviderResult>> answer) {
            this.key = key;
            this.supported = supported;
            this.answer = answer;
        }

        @Override
        public String key() {
            return key;
        }

        @Override
        public boolean supports(IdType idType) {
            return supported.contains(idType);
        }

        @Override
        public Optional<ProviderResult> fetch(IdType idType, String idValue) {
            asked.add(idType + ":" + idValue);
            return answer.apply(idValue);
        }
    }

    private static ProviderResult result(String provider, Map<String, Object> normalized) {
        return new ProviderResult(provider, Map.of(), normalized, List.of(), null);
    }

    private static FakeProvider providerReturning(String key, Map<String, Object> normalized) {
        return new FakeProvider(key, Set.of(IdType.CUSTOM), id -> Optional.of(result(key, normalized)));
    }

    private static ModuleProviderSpec spec(String key, ModuleProviderSpec.Chain... chains) {
        return new ModuleProviderSpec(key, 1, true, List.of(chains));
    }

    private static ModuleProviderSpec.Chain chain(String from, String to) {
        return new ModuleProviderSpec.Chain(from, to, IdType.CUSTOM);
    }

    private List<ProviderResult> follow(ModuleProviderSpec start, ProviderResult found, List<FakeProvider> providers, ModuleProviderSpec... module) {
        Map<String, ModuleProviderSpec> active = new LinkedHashMap<>();
        for (ModuleProviderSpec s : module) active.put(s.key(), s);
        List<ProviderResult> results = new ArrayList<>();
        new ProviderChainingService(new ObjectMapper(), new ProviderRegistry(List.copyOf(providers)))
                .follow(start, found, active, new HashSet<>(), results);
        return results;
    }

    @Test
    void aChainLooksUpTheNamedFieldOfTheResultInTheTargetProvider() {
        FakeProvider target = providerReturning("comicvine", Map.of("title", "Hulk"));
        ModuleProviderSpec metron = spec("metron", chain("comicvine_id", "comicvine"));

        List<ProviderResult> derived = follow(metron, result("metron", Map.of("comicvine_id", "4000-77")), List.of(target), metron, spec("comicvine"));

        assertThat(derived).extracting(ProviderResult::providerKey).containsExactly("comicvine");
        assertThat(target.asked).containsExactly("CUSTOM:4000-77");
    }

    @Test
    void aNumericFieldIsLookedUpAsItsText() {
        FakeProvider target = providerReturning("comicvine", Map.of("title", "Hulk"));
        ModuleProviderSpec metron = spec("metron", chain("comicvine_id", "comicvine"));

        follow(metron, result("metron", Map.of("comicvine_id", 77)), List.of(target), metron, spec("comicvine"));

        assertThat(target.asked).containsExactly("CUSTOM:77");
    }

    @Test
    void nothingIsLookedUpWhenTheResultHasNoValueForTheField() {
        FakeProvider target = providerReturning("comicvine", Map.of());
        ModuleProviderSpec metron = spec("metron", chain("comicvine_id", "comicvine"));

        assertThat(follow(metron, result("metron", Map.of("title", "Hulk")), List.of(target), metron, spec("comicvine"))).isEmpty();
        assertThat(follow(metron, result("metron", Map.of("comicvine_id", "  ")), List.of(target), metron, spec("comicvine"))).isEmpty();
        assertThat(target.asked).isEmpty();
    }

    @Test
    void aTargetThatIsNotAnActiveProviderOfTheModuleIsNotAsked() {
        FakeProvider target = providerReturning("comicvine", Map.of());
        ModuleProviderSpec metron = spec("metron", chain("comicvine_id", "comicvine"));

        assertThat(follow(metron, result("metron", Map.of("comicvine_id", "1")), List.of(target), metron)).isEmpty();
        assertThat(target.asked).isEmpty();
    }

    @Test
    void aTargetThatDoesNotSupportTheIdentifierTypeIsNotAsked() {
        FakeProvider target = new FakeProvider("comicvine", Set.of(IdType.ISBN13), id -> Optional.of(result("comicvine", Map.of())));
        ModuleProviderSpec metron = spec("metron", chain("comicvine_id", "comicvine"));

        assertThat(follow(metron, result("metron", Map.of("comicvine_id", "1")), List.of(target), metron, spec("comicvine"))).isEmpty();
        assertThat(target.asked).isEmpty();
    }

    @Test
    void aChainCanLookUpAnotherIdentifierType() {
        FakeProvider target = new FakeProvider("googlebooks", Set.of(IdType.ISBN13), id -> Optional.of(result("googlebooks", Map.of())));
        ModuleProviderSpec search = spec("openlibrary", new ModuleProviderSpec.Chain("isbn13", "googlebooks", IdType.ISBN13));

        follow(search, result("openlibrary", Map.of("isbn13", "9780261103573")), List.of(target), search, spec("googlebooks"));

        assertThat(target.asked).containsExactly("ISBN13:9780261103573");
    }

    @Test
    void aTargetThatFindsNothingAddsNothing() {
        FakeProvider target = new FakeProvider("comicvine", Set.of(IdType.CUSTOM), id -> Optional.empty());
        ModuleProviderSpec metron = spec("metron", chain("comicvine_id", "comicvine"));

        assertThat(follow(metron, result("metron", Map.of("comicvine_id", "1")), List.of(target), metron, spec("comicvine"))).isEmpty();
    }

    @Test
    void aFailingHopIsSkippedAndTheLaterChainsStillRun() {
        FakeProvider broken = new FakeProvider("broken", Set.of(IdType.CUSTOM), id -> {
            throw new IllegalStateException("upstream down");
        });
        FakeProvider fine = providerReturning("fine", Map.of());
        ModuleProviderSpec first = spec("first", chain("a_id", "broken"), chain("b_id", "fine"));

        List<ProviderResult> derived = follow(first, result("first", Map.of("a_id", "1", "b_id", "2")), List.of(broken, fine),
                first, spec("broken"), spec("fine"));

        assertThat(derived).extracting(ProviderResult::providerKey).containsExactly("fine");
    }

    @Test
    void chainsAreFollowedFromTheResultsTheyProduce() {
        FakeProvider second = providerReturning("second", Map.of("third_id", "t-1"));
        FakeProvider third = providerReturning("third", Map.of("title", "end"));
        ModuleProviderSpec first = spec("first", chain("second_id", "second"));

        List<ProviderResult> derived = follow(first, result("first", Map.of("second_id", "s-1")), List.of(second, third),
                first, spec("second", chain("third_id", "third")), spec("third"));

        assertThat(derived).extracting(ProviderResult::providerKey).containsExactly("second", "third");
    }

    @Test
    void aValueAlreadyAskedForIsNotAskedAgain() {
        FakeProvider target = providerReturning("comicvine", Map.of());
        ModuleProviderSpec metron = spec("metron", chain("comicvine_id", "comicvine"));
        Map<String, ModuleProviderSpec> active = Map.of("metron", metron, "comicvine", spec("comicvine"));
        Set<String> asked = new HashSet<>(Set.of(ProviderChainingService.askedKey("comicvine", IdType.CUSTOM, "9")));
        List<ProviderResult> results = new ArrayList<>();

        new ProviderChainingService(new ObjectMapper(), new ProviderRegistry(List.of(target)))
                .follow(metron, result("metron", Map.of("comicvine_id", "9")), active, asked, results);

        assertThat(results).isEmpty();
        assertThat(target.asked).isEmpty();
    }

    @Test
    void aLoopBetweenProvidersStopsAfterTheDepthLimit() {
        FakeProvider a = new FakeProvider("a", Set.of(IdType.CUSTOM), id -> Optional.of(result("a", Map.of("b_id", "b-" + id))));
        FakeProvider b = new FakeProvider("b", Set.of(IdType.CUSTOM), id -> Optional.of(result("b", Map.of("a_id", "a-" + id))));
        ModuleProviderSpec specA = spec("a", chain("b_id", "b"));
        ModuleProviderSpec specB = spec("b", chain("a_id", "a"));

        List<ProviderResult> derived = follow(specA, result("a", Map.of("b_id", "b-0")), List.of(a, b), specA, specB);

        assertThat(derived).hasSize(ProviderChainingService.MAX_DEPTH);
    }
}
