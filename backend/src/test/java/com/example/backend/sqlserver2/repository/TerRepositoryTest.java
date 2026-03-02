package com.example.backend.sqlserver2.repository;

import com.example.backend.sqlserver2.model.Ter;
import com.example.backend.sqlserver2.model.TerId;
import com.example.backend.service.TerSearchOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class TerRepositoryTest {
    @Autowired
    private TerRepository terRepository;

    @BeforeEach
    void setUp() {
        terRepository.deleteAll();
    }

    @Test
    void shouldFindByENT() {
        Ter ter1 = createTer(1, 100, "Provider A", 0);
        Ter ter2 = createTer(1, 101, "Provider B", 1);
        Ter ter3 = createTer(2, 102, "Provider C", 0);
        terRepository.saveAll(List.of(ter1, ter2, ter3));

        List<Ter> results = terRepository.findByENT(1);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(Ter::getTERCOD).containsExactlyInAnyOrder(100, 101);
    }

    @Test
    void shouldFindByENTAndTERCODAndTERBLO() {
        Ter bloqueado = createTer(1, 100, "Provider A", 1);
        Ter noBloqueado = createTer(1, 100, "Provider A Copy", 0);
        terRepository.saveAll(List.of(bloqueado, noBloqueado));

        List<Ter> results = terRepository.findByENTAndTERCODAndTERBLO(1, 100, 1);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTERBLO()).isEqualTo(1);
    }

    @Test
    void shouldFindByENTAndTERNIFContainingAndTERBLO() {
        Ter ter1 = createTer(1, 100, "Provider A", 1);
        ter1.setTERNIF("12345678A");
        Ter ter2 = createTer(1, 101, "Provider B", 0);
        ter2.setTERNIF("87654321B");
        terRepository.saveAll(List.of(ter1, ter2));

        List<Ter> results = terRepository.findByENTAndTERNIFContainingAndTERBLO(1, "12345", 1);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTERNIF()).contains("12345");
    }

    @Test
    void shouldFindAllByENTAndTERCOD() {
        Ter ter1 = createTer(1, 100, "Provider A", 1);
        Ter ter2 = createTer(1, 100, "Provider A Duplicate", 0);
        Ter ter3 = createTer(1, 101, "Provider B", 0);
        terRepository.saveAll(List.of(ter1, ter2, ter3));

        List<Ter> results = terRepository.findAllByENTAndTERCOD(1, 100);

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(ter -> ter.getTERCOD().equals(100));
    }

    @Test
    void shouldFindByENTAndTERNIFContaining() {
        Ter ter1 = createTer(1, 100, "Provider A", 1);
        ter1.setTERNIF("A1234567B");
        Ter ter2 = createTer(1, 101, "Provider B", 0);
        ter2.setTERNIF("X9876543Y");
        terRepository.saveAll(List.of(ter1, ter2));

        List<Ter> results = terRepository.findByENTAndTERNIFContaining(1, "1234");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTERNIF()).contains("1234");
    }

    @Test
    void shouldFindByENTAndTERBLO() {
        Ter bloqueado1 = createTer(1, 100, "Provider A", 1);
        Ter bloqueado2 = createTer(1, 101, "Provider B", 1);
        Ter noBloqueado = createTer(1, 102, "Provider C", 0);
        terRepository.saveAll(List.of(bloqueado1, bloqueado2, noBloqueado));

        List<Ter> bloqueados = terRepository.findByENTAndTERBLO(1, 1);
        List<Ter> noBloqueados = terRepository.findByENTAndTERBLO(1, 0);

        assertThat(bloqueados).hasSize(2);
        assertThat(noBloqueados).hasSize(1);
    }

    @Test
    void shouldFindNextTercodForEnt() {
        Ter ter1 = createTer(1, 5, "Provider A", 0);
        Ter ter2 = createTer(1, 10, "Provider B", 0);
        Ter ter3 = createTer(2, 3, "Provider C", 0);
        terRepository.saveAll(List.of(ter1, ter2, ter3));

        Integer nextTercod = terRepository.findNextTercodForEnt(1);

        assertThat(nextTercod).isEqualTo(11); 
    }

    @Test
    void shouldReturnOneWhenNoRecordsExist() {
        Integer nextTercod = terRepository.findNextTercodForEnt(1);

        assertThat(nextTercod).isEqualTo(1);
    }

    @Test
    void shouldFindByENTAndTERCOD() {
        Ter ter = createTer(1, 100, "Provider A", 0);
        terRepository.save(ter);

        Optional<Ter> result = terRepository.findByENTAndTERCOD(1, 100);

        assertThat(result).isPresent();
        assertThat(result.get().getTERNOM()).isEqualTo("Provider A");
    }

    @Test
    void shouldReturnEmptyWhenNotFound() {
        Optional<Ter> result = terRepository.findByENTAndTERCOD(1, 99999);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldSearchFilteredWithSpecification() {
        Ter bloqueado = createTer(1, 100, "Provider ABC", 1);
        bloqueado.setTERNIF("12345678A");
        bloqueado.setTERALI("Alias ABC");
        
        Ter noBloqueado = createTer(1, 101, "Provider XYZ", 0);
        noBloqueado.setTERNIF("87654321B");
        
        terRepository.saveAll(List.of(bloqueado, noBloqueado));

        Specification<Ter> spec = TerSearchOptions.searchFiltered(1, "ABC");
        List<Ter> results = terRepository.findAll(spec);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTERCOD()).isEqualTo(100);
    }

    @Test
    void shouldSearchByTermWithSpecification() {
        Ter bloqueado = createTer(1, 100, "Provider ABC", 1);
        Ter noBloqueado = createTer(1, 101, "Provider ABC Copy", 0);
        terRepository.saveAll(List.of(bloqueado, noBloqueado));

        Specification<Ter> spec = TerSearchOptions.searchByTerm(1, "ABC");
        List<Ter> results = terRepository.findAll(spec);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTERBLO()).isNotEqualTo(1);
    }

    private Ter createTer(Integer ent, Integer tercod, String nombre, Integer terblo) {
        Ter ter = new Ter();
        ter.setENT(ent);
        ter.setTERCOD(tercod);
        ter.setTERNOM(nombre);
        ter.setTERBLO(terblo);
        ter.setTERNIF("00000000A");
        ter.setTERALI("Alias");
        return ter;
    }
}
