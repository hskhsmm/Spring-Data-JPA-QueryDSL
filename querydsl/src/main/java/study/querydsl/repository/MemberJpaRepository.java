package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;

import java.util.List;
import java.util.Optional;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.team;

@Repository
@RequiredArgsConstructor
public class MemberJpaRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

//    public MemberJpaRepository(EntityManager em, JPAQueryFactory queryFactory) {
//        this.em = em;
//        this.queryFactory = queryFactory;
//    }

    public void save(Member member) {
        em.persist(member);
    }

    //식별자 넣으면 조회
    public Optional<Member> findById(Long id) {
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);
    }

    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class).getResultList();
    }

    public List<Member> findAll_Querydsl() {
        return queryFactory
                .selectFrom(member)
                .fetch();
    }

    public List<Member> findByUserName(String username) {
        return em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username",username)
                .getResultList();
    }

    public List<Member> findByUserName_Querydsl(String username) {
        return queryFactory
                .selectFrom(member)
                .where(member.username.eq(username))
                .fetch();
    }

    // 조건에 따라 회원과 팀 정보를 조회하는 메서드
    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {

        // BooleanBuilder: 동적으로 where 조건을 구성하기 위한 객체
        BooleanBuilder builder = new BooleanBuilder();

        // 사용자 이름(username)이 존재하면 해당 조건 추가
        if (hasText(condition.getUsername())) {
            builder.and(member.username.eq(condition.getUsername()));
        }

        // 팀 이름(teamName)이 존재하면 해당 조건 추가
        if (hasText(condition.getTeamName())) {
            builder.and(team.name.eq(condition.getTeamName()));
        }

        // 나이(age)가 최소값 이상인 조건이 존재할 경우 추가 (Greater or Equal)
        if (condition.getAgeGoe() != null) {
            builder.and(member.age.goe(condition.getAgeGoe()));
        }

        // 나이(age)가 최대값 이하인 조건이 존재할 경우 추가 (Less or Equal)
        if (condition.getAgeLoe() != null) {
            builder.and(member.age.loe(condition.getAgeLoe()));
        }

        // 조건(builder)을 적용해서 회원 + 팀 정보 조회 (MemberTeamDto로 매핑)
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
                .where(builder)                      // 위에서 만든 조건(builder)을 where절로 적용
                .fetch();                            // 결과 리스트 반환
    }

}

