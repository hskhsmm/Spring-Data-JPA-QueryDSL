package study.datajpa.controller;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import study.datajpa.entity.Member;
import study.datajpa.repository.MemberRepository;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberRepository memberRepository;


    //    @PathVariable("id")로 id만 받아옴.
    //    memberRepository.findById(id)로 직접 조회 (수동 조회).
    //    예외처리 필요 (.orElseThrow() 등).
    @GetMapping("/members/{id}")
    public String findMember(@PathVariable("id") Long id) {
        Member member = memberRepository.findById(id).get();
        return member.getUsername();
    }


    //    @PathVariable("id")를 Member 객체로 바로 변환
    //    이 기능은 Spring이 MemberRepository.findById(id)를 자동으로 호출해주는 기능
    //    이걸 "도메인 클래스 컨버터 (Domain Class Converter)" 라고 함.
    //    도메인 클래스 컨버터로 엔티티를 파라미터로 받으면 단순 조회용으로만 사용해야 한다.
    @GetMapping("/members2/{id}")
    public String findMember2(@PathVariable("id") Member member) {
        return member.getUsername();
    }

    @GetMapping("/members")
    public Page<Member> list(@PageableDefault(size=5) Pageable pageable) {
        Page<Member> page = memberRepository.findAll(pageable);
        return page;
    }

    @PostConstruct
    public void init() {

        for(int i=0; i<100; i++) {
            memberRepository.save(new Member("user" + i, i));
        }
    }
}
