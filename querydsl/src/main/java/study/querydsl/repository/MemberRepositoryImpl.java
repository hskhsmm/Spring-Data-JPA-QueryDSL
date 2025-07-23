package study.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;

import java.util.List;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

public class MemberRepositoryImpl implements MemberRepositoryCustom {



    private final JPAQueryFactory queryFactory;

    public MemberRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),     // 회원 ID
                        member.username,              // 회원 이름
                        member.age,                   // 회원 나이
                        team.id.as("teamId"),         // 팀 ID
                        team.name.as("teamName")      // 팀 이름
                ))
                .from(member)                        // member 테이블 기준으로 조회 시작
                .leftJoin(member.team, team)         // member와 team 테이블을 left join
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )                      // 위에서 만든 조건(builder)을 where절로 적용
                .fetch();
    }

    /**
     * 단순한 페이징, fetchResults() 사용
     */
    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition,
                                                Pageable pageable) {
        // QueryDSL의 fetchResults()를 사용하여 페이징 처리된 결과와 총 개수를 함께 조회
        QueryResults<MemberTeamDto> results = queryFactory
                .select(new QMemberTeamDto(
                        member.id,       // 회원 ID
                        member.username, // 회원 이름
                        member.age,      // 회원 나이
                        team.id,         // 소속 팀 ID
                        team.name        // 소속 팀 이름
                ))
                .from(member)                // member 테이블을 기준으로 조회 시작
                .leftJoin(member.team, team) // member.team과 team 테이블을 LEFT JOIN
                .where(                      // 검색 조건들 (null이면 무시됨)
                        usernameEq(condition.getUsername()),   // 사용자 이름 조건
                        teamNameEq(condition.getTeamName()),   // 팀 이름 조건
                        ageGoe(condition.getAgeGoe()),         // 나이 >= 조건
                        ageLoe(condition.getAgeLoe())          // 나이 <= 조건
                )
                .offset(pageable.getOffset())     // 몇 번째부터 가져올지 설정 (페이지 시작 위치)
                .limit(pageable.getPageSize())    // 한 페이지당 몇 개 가져올지 설정
                .fetchResults();                  // 결과와 총 개수를 함께 조회 (쿼리 2번 실행됨)

        // 조회된 실제 데이터 리스트
        List<MemberTeamDto> content = results.getResults();

        // 전체 개수 (count 쿼리 결과)
        long total = results.getTotal();

        // PageImpl을 이용해 Page 객체로 반환 (스프링 데이터가 요구하는 형식)
        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 복잡한 페이징
     * - 데이터 조회 쿼리와 전체 카운트 쿼리를 분리하여 실행
     * - 성능 최적화가 필요할 경우 fetchCount 쿼리를 따로 튜닝하거나 생략 가능
     */
    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition,
                                                 Pageable pageable) {

        // 1. 실제 페이지에 보여줄 데이터 목록 조회 (limit, offset 적용)
        List<MemberTeamDto> content = queryFactory
                .select(new QMemberTeamDto(
                        member.id,       // 회원 ID
                        member.username, // 회원 이름
                        member.age,      // 회원 나이
                        team.id,         // 팀 ID
                        team.name        // 팀 이름
                ))
                .from(member)                 // member 테이블 기준
                .leftJoin(member.team, team)  // team 테이블과 LEFT JOIN
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 3. 결과를 Page 객체로 감싸서 반환
        //   PageableExecutionUtils.getPage(...) 사용 이유:
        // - content의 개수가 페이지 사이즈보다 작으면 자동으로 count 쿼리를 생략함 → 성능 최적화
        // - 직접 PageImpl로 감싸는 방식보다 불필요한 쿼리 최소화 가능
        // - 내부적으로 countSupplier는 필요할 때만 실행됨 (lazy evaluation)

        // fetchCount() 제거 이유:
        // - QueryDSL 5.0 이상에서 fetchCount()는 deprecated 되었고, 정확하지 않은 count를 반환할 수 있어 추천되지 않음
        // - 대신 select(member.count()).fetchOne()을 명시적으로 사용하여 명확한 count 쿼리 작성

        // null-safe 처리:
        // - fetchOne()은 결과가 없을 경우 null을 반환하므로, count가 null이면 0L을 반환하도록 처리
        return PageableExecutionUtils.getPage(content, pageable, () -> {
            Long count = queryFactory
                    .select(member.count())           // 정확한 count 쿼리
                    .from(member)
                    .leftJoin(member.team, team)
                    .where(
                            usernameEq(condition.getUsername()),
                            teamNameEq(condition.getTeamName()),
                            ageGoe(condition.getAgeGoe()),
                            ageLoe(condition.getAgeLoe())
                    )
                    .fetchOne();                      // fetchCount() 대신 fetchOne() 사용
            return count != null ? count : 0L;        // null-safe 처리
        });
    }






    private BooleanExpression usernameEq(String username) {
        return hasText(username) ? member.username.eq(username) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return hasText(teamName) ? member.team.name.eq(teamName) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }
}
