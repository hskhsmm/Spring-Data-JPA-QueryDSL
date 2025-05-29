package study.datajpa.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Member {

    @Id @GeneratedValue
    private Long id;
    private String username;

    protected Member() {
    } //기본 생성자를 private으로 막으면 객체를 못만들어냄.

    public Member(String username) {
        this.username = username;
    }
}
