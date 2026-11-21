package kr.ac.kopo.wodyd.termproject.repository;

import kr.ac.kopo.wodyd.termproject.domain.Game;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {

    Optional<Game> findByName(String name);
}
