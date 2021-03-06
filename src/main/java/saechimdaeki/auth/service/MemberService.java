package saechimdaeki.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saechimdaeki.auth.domain.Member;
import saechimdaeki.auth.domain.MemberAccount;
import saechimdaeki.auth.dto.EmailMessage;
import saechimdaeki.auth.dto.JoinMemberDto;
import saechimdaeki.auth.dto.LoginResponseDto;
import saechimdaeki.auth.dto.MemberDto;
import saechimdaeki.auth.dto.MemberInfoDto;
import saechimdaeki.auth.repository.MemberRepository;
import saechimdaeki.auth.smtp.EmailService;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService implements UserDetailsService  {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = memberRepository.findByUserName(username).orElseThrow(() -> new UsernameNotFoundException(username));
        if(!member.isEmailVerified()) throw new RuntimeException("회원 가입 인증 메일을 확인해주세요");
        return new MemberAccount(member);
    }

    @Transactional
    public Long joinNewMember(JoinMemberDto dto) {
        dto.encodePassword(passwordEncoder.encode(dto.getPassword()));
        Member member = Member.joinMember(dto);
        memberRepository.save(member);
        MemberDto memberDto = Member.toDto(member);
        sendConfirmEmail(memberDto);
        return member.getId();
    }

    private void sendConfirmEmail(MemberDto memberDto) {
        emailService.sendEmail(EmailMessage.sendSignUpEmail(memberDto));
    }


    //TODO 반환값 정의 필요
    @Transactional
    public void verifyToken(String token, String email) {
        Member member = memberRepository.findByEmail(email).orElseThrow(()-> new RuntimeException("해당 유저가 존재하지 않습니다"));
        if(!member.getEmailCheckToken().equals(token))
            throw new RuntimeException("올바르지 않은 토큰입니다");

        member.changeEmailVerified();
    }

    public LoginResponseDto getLoginResponseInfo(String username){
        Member findByUsernameMember = memberRepository.findByUserName(username)
                                                      .orElseThrow(() -> new RuntimeException("해당 username을 가진 유저는 존재하지 않습니다"));
        return LoginResponseDto.builder()
                               .email(findByUsernameMember.getEmail())
                               .role(findByUsernameMember.getRole())
                               .userName(findByUsernameMember.getUserName()).build();
    }

    public MemberInfoDto retriveMemberInfo(Long memberId){
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new RuntimeException("존재하지 않는 회원입니다"));
        return MemberInfoDto.builder()
                            .email(member.getEmail())
                            .userName(member.getUserName())
                            .role(member.getRole())
                            .build();
    }
}
