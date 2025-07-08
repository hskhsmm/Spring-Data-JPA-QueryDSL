package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
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



    @PersistenceUnit //엔티티 매니저를 만드는 팩토리 주입
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    @Test
    public void fetchJoinUse() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isTrue();
    }


    @Test
    public void subQuery() {

        // 서브쿼리에 사용할 QMember 별칭 생성 (메인 쿼리와 충돌 방지)
        QMember memberSub = new QMember("memberSub");

        // 메인 쿼리: 나이가 가장 많은 사람을 조회
        List<Member> result = queryFactory
                .selectFrom(member) // member 엔티티를 기준으로 조회
                .where(member.age.eq( // 나이가 서브쿼리의 결과와 같은 조건
                        select(memberSub.age.max()) // memberSub를 기준으로 최대 나이 조회 (서브쿼리)
                                .from(memberSub) // 서브쿼리의 대상: member 테이블
                ))
                .fetch(); // 결과 리스트로 조회

        // 결과 검증: 나이가 40인 멤버만 조회되어야 함
        assertThat(result).extracting("age").containsExactly(40);
    }


    @Test
    public void subQueryGoe() {

        // 서브쿼리용 QMember 객체 생성 (별칭 사용)
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member) // 메인 쿼리: member 테이블에서 조회
                .where(member.age.goe( // 나이가 서브쿼리 평균보다 크거나 같은 조건
                        select(memberSub.age.avg()) // 서브쿼리: 평균 나이
                                .from(memberSub)
                ))
                .fetch(); // 결과 가져오기

        // 평균 이상 나이인 멤버는 30, 40살 -> 검증
        assertThat(result).extracting("age").containsExactly(30, 40);
    }


    @Test
    public void subQueryIn() {

        // 서브쿼리용 QMember 객체 생성
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member) // 메인 쿼리: member 테이블에서 조회
                .where(member.age.in( // age가 서브쿼리 결과에 포함된 경우
                        select(memberSub.age) // 서브쿼리: 나이가 10보다 큰 멤버들의 나이 목록
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch(); // 결과 가져오기

        // 나이가 20, 30, 40인 멤버만 조회됨 -> 검증
        assertThat(result).extracting("age").containsExactly(20, 30, 40);
    }

    //from절의 서브쿼리는 지원하지 않음.
    //서브쿼리를 join으로 변경하고 애플리케이션에서 쿼리를 2번 분리해서 실행하자
    //그래도 안되면 nativeSQL


    /**
     * case문
     * select, 조건절(where)에서 사용 가능
     */
    @Test
    public void basicCase() {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void complexCase() {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
        
    }

    @Test
    public void orderByCase() {
        NumberExpression<Integer> rankPath = new CaseBuilder()
                .when(member.age.between(0, 20)).then(2)
                .when(member.age.between(21, 30)).then(1)
                .otherwise(3);

        List<Tuple> result = queryFactory
                .select(member.username, member.age, rankPath)
                .from(member)
                .orderBy(rankPath.desc())
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            Integer rank = tuple.get(rankPath);
            System.out.println("username = " + username + " age = " + age + " rank = " + rank);
        }
    }

    @Test
    public void constant() {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple= " + tuple);
        }
    }

    @Test
    public void concat() {
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void simpleProjection() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void tupleProjection() {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    @Test
    public void findDtoByJPQL() {
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username,m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoBySetter() {
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);


        }

    }

    @Test
    public void findDtoByField() {
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);


        }

    }

    @Test
    public void findDtoByConstructor() {
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findUserDto() {
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }

    }

    @Test
    public void findDtoByConstructorUser() {
        List<UserDto> result = queryFactory
                .select(Projections.constructor(UserDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }

    }

    @Test
    public void findDtoByQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto );
        }

    }



}