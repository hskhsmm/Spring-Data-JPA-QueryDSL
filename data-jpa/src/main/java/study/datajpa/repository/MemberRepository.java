package study.datajpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import study.datajpa.dto.MemberDto;
import study.datajpa.entity.Member;

import java.util.List;
import java.util.Optional;

// Member 엔티티에 대한 CRUD 및 쿼리를 정의하는 Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    // username이 일치하고 age가 주어진 값보다 큰 Member 목록 조회
    List<Member> findByUsernameAndAgeGreaterThan(String username, int age);

    // username이 "Hello"로 시작하는 상위 3개 Member 조회 (관례 기반 쿼리)
    List<Member> findTop3HelloBy();

    // username으로 Member 목록 조회 (JPQL 명확할 때 @Param 필수)
    List<Member> findByUsername(@Param("username") String username);

    // username과 age가 일치하는 Member 조회 (명시적 JPQL 사용)
    @Query("select m from Member m where m.username = :username and m.age = :age")
    List<Member> findUser(@Param("username") String username, @Param("age") int age);

    // 모든 Member 목록 조회 (전체 username 리스트 조회)
    @Query("select m from Member m")
    List<Member> findUsernameList();

    // Member와 연관된 Team 정보를 포함해 DTO로 조회
    @Query("select new study.datajpa.dto.MemberDto(m.id, m.username, t.name) from Member m join m.team t")
    List<MemberDto> findMemberDto();

    // 이름 리스트에 포함되는 Member 조회 (IN절 사용)
    @Query("select m from Member m where m.username in :names")
    List<Member> findByNames(@Param("names") List<String> names);

    // username으로 Member 컬렉션 조회
    List<Member> findListByUsername(String username);

    // username으로 단일 Member 조회
    Member findMemberByUsername(String username);

    // username으로 단일 Member 조회, 결과가 없을 수도 있는 경우 Optional로 감싸서 반환
    Optional<Member> findOptionalByUsername(String username);

    // 특정 나이의 Member를 Page로 조회 (join은 있지만 사용되지 않으면 Hibernate 6에서 제거됨)
    @Query(value = "select m from Member m left join m.team t")
    Page<Member> findByAge(int age, Pageable pageable);

    // count 쿼리를 별도로 분리해 성능 최적화
    @Query(value = "select m from Member m", countQuery = "select count(m.username) from Member m")
    Page<Member> findMemberAllCountBy(Pageable pageable);

    // 조건에 따라 age를 +1 하는 벌크 수정 쿼리 (flush, clear 주의)
    @Modifying(clearAutomatically = true)
    @Query("update Member m set m.age = m.age + 1 where m.age >= :age")
    int bulkAgePlus(@Param("age") int age);

    // Member를 조회하면서 Team을 함께 가져오는 fetch join 쿼리
    @Query("select m from Member m left join fetch m.team")
    List<Member> findMemberFetchJoin();

    // 기본 findAll() 메서드에서 team을 함께 조회하도록 EntityGraph 사용
    @Override
    @EntityGraph(attributePaths = {"team"})
    List<Member> findAll();

    // 명시적 JPQL + EntityGraph를 함께 사용 (team을 즉시 로딩)
    @EntityGraph(attributePaths = {"team"})
    @Query("select m from Member m")
    List<Member> findMemberEntityGraph();

    // 조건 기반 조회에도 EntityGraph를 적용하여 team 즉시 로딩
    @EntityGraph(attributePaths = ("team"))
    List<Member> findEntityGraphByUsername(@Param("username") String username);
}
