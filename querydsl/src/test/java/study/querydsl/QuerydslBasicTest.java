package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em); // 안전한 시점에서 초기화

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        em.persist(new Member("member1", 10, teamA));
        em.persist(new Member("member2", 20, teamA));
        em.persist(new Member("member3", 30, teamB));
        em.persist(new Member("member4", 40, teamB));
    }

    // JPQL로 단건 조회 테스트
    @Test
    public void startJPQL() {
        Member findMember = em.createQuery(
                        "select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    // QueryDSL로 단건 조회 - Q타입 직접 생성
    @Test
    public void startQuerydsl() {
        QMember m = new QMember("m");

        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))
                .fetchOne();

        assertThat(findMember).isNotNull();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    // QueryDSL로 단건 조회 - Q타입 static import 사용
    @Test
    public void startQuerydsl2() {
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember).isNotNull();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    // QueryDSL로 조건 검색 - and() 사용
    @Test
    public void search() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1").and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    // QueryDSL로 조건 검색 - where 파라미터로 and 자동 처리
    @Test
    public void searchAndParam() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"),
                        (member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    // 다양한 fetch 방식 테스트
    @Test
    public void resultFetch() {
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch(); // 전체 리스트

        Member fetchOne = queryFactory
                .selectFrom(member)
                .fetchOne(); // 단건

        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst(); // 첫 번째 레코드

        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults(); // 페이징 정보 포함

        results.getTotal();
        List<Member> content = results.getResults();

        long total = queryFactory
                .selectFrom(member)
                .fetchCount(); // 총 개수 조회
    }

    // 정렬 테스트: 나이 내림차순 + 이름 오름차순(null은 마지막)
    @Test
    public void sort() {
        em.persist(new Member(null,100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    // 페이징 테스트 (offset/limit)
    @Test
    public void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.age.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    // 페이징 + 전체 개수 조회 포함
    @Test
    public void paging2() {
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }

    // 집계 함수 테스트
    @Test
    public void aggregation() {
        List<Tuple> result = queryFactory
                .select(member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    // 그룹핑 테스트: 팀별 평균 나이
    @Test
    public void group() throws Exception {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    // 조인 테스트: 특정 팀 소속 회원 조회
    @Test
    public void join() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result).extracting("username").containsExactly("member1", "member2");
    }

    // 세타 조인 (연관관계 없이 조인)
    @Test
    public void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        queryFactory
                .select(member)
                .from(member,team)
                .where(member.username.eq(team.name))
                .fetch();
    }

    //회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
    //연관관계 있음
    @Test
    public void join_on_filtering() throws Exception {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple= " + tuple);
        }
    }


    /**
     * 연관관계 없는 엔티티 외부 조인
     * 회원의 이름이 팀 이름과 같은 대상 외부 조인
     */
    @Test
    public void join_on_no_relation() {
        // 연관관계가 없는 team 이름과 username이 같은 데이터를 테스트하기 위해 더미 데이터 저장
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        // member 테이블과 team 테이블을 외부 조인 (LEFT JOIN) 하는데,
        // 둘 사이에 실제 연관관계가 없으므로 ON 절을 직접 지정해야 함
        List<Tuple> result = queryFactory
                .select(member, team) // member와 team 정보를 모두 조회
                .from(member)         // member 테이블 기준으로 시작
                .leftJoin(team).on(member.username.eq(team.name)) // username과 team.name이 같은 경우만 LEFT JOIN
                .where(member.username.eq(team.name)) // 결과 중 username == team.name인 것만 필터링
                .fetch(); // 결과 조회

        // 결과 출력
        for (Tuple tuple : result) {
            System.out.println("tuple= " + tuple);
        }
        //    .leftJoin(team).on(member.username.eq(team.name))
        //    member와 team 사이에 JPA 연관관계가 없음.
        //    그런데 username == team.name이라는 논리 조건으로 조인을 시도함.
        //    이게 바로 세타 조인이다.
        //    즉, 이 코드는 QueryDSL에서 연관관계 없이 수동으로 조인 조건을 명시한 세타 조인
    }


}