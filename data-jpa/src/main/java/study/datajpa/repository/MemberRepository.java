package study.datajpa.repository;

import org.hibernate.Hibernate;
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

public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findByUsernameAndAgeGreaterThan(String username, int age);

    List<Member> findTop3HelloBy();

    //@Query(name = "Member.findByUsername") 없애도 잘 동작
    List<Member> findByUsername(@Param("username") String username); //jpql을 명확히 작성했을 땐 Param이 필요

    @Query("select m from Member m where m.username = :username and m.age = :age")
    List<Member> findUser(@Param("username") String username, @Param("age") int age);

    //사용자 이름 리스트만 다 가져오려면?
    @Query("select m from Member m")
    List<Member> findUsernameList();

    //리스트에서 DTO 조회
    @Query("select new study.datajpa.dto.MemberDto(m.id, m.username, t.name) from Member m join m.team t")
    List<MemberDto> findMemberDto();

    //파라미터 바인딩 해보기
    @Query("select m from Member m where m.username in :names")
    List<Member> findByNames(@Param("names") List<String> names);

    List<Member> findListByUsername(String username); //컬렉션
    Member findMemberByUsername(String username); //단건
    Optional<Member> findOptionalByUsername(String username); //단건 Optional

    //카운트쿼리 추가 적용.  hibernate 6에선 의미없는 left join 최적화. 따라서 select m from Member m과 같다.
    @Query(value = "select m from Member m left join m.team t")
    Page<Member> findByAge(int age, Pageable pageable); //pageable 인터페이스만 넘기면 됨.
//    이렇게 left join을 걸었지만,
//    정작 select절이나 where절에서 t를 사용하지 않으면 Hibernate 6에서 다음과 같은 일이 발생합니다:
//
//            🔸 의미 없는 JOIN으로 판단
//
//            🔸 SQL에 JOIN 자체가 누락되거나 제거
//
//            🔸 일부 상황에선 "t가 사용되지 않았습니다" 경고 또는 오류


    //카운트 쿼리 분리
    @Query(value = "select m from Member m", countQuery = "select count(m.username) from Member m")
    Page<Member> findMemberAllCountBy(Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("update Member m set m.age = m.age + 1 where m.age >= :age") //JPA가 관리하는 1차 캐시(영속성 컨텍스트)를 건너뛰고 바로 DB에 SQL을 실행
    int bulkAgePlus(@Param("age") int age);

    @Query("select m from Member m left join fetch m.team")
    List<Member> findMemberFetchJoin();


}
