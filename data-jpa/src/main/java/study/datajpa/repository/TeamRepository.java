package study.datajpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.datajpa.entity.Team;

//굳이 @Repository 안해줘도 알아서 인식함.
public interface TeamRepository extends JpaRepository<Team, Long> {

}
