package com.grupo.spent.services;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.grupo.spent.auth.TokenProvider;
import com.grupo.spent.entities.User;
import com.grupo.spent.entities.UserRoleEnum;
import com.grupo.spent.exceptions.NotFoundException;
import com.grupo.spent.repositories.UserRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    @Autowired
    UserRepository userRepository;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private TokenProvider tokenProvider;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JavaMailSender mailSender;

    public User register(String email, String username, String name, String password, String siteURL)
            throws UnsupportedEncodingException, MessagingException {
        String encryptedPassword = passwordEncoder.encode(password);
        String randomCode = UUID.randomUUID().toString();
        User user = User.builder()
                .email(email)
                .username(username)
                .firstName(name)
                .password(encryptedPassword)
                .role(UserRoleEnum.USER)
                .registerDate(LocalDate.now())
                .rating(0.0)
                .enabled(false)
                .verificationCode(randomCode)
                .build();
        sendVerificationEmail(user, siteURL);
        return userRepository.save(user);
    }

    public String login(String email, String password) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(email, password);
        var authUser = authenticationManager.authenticate(usernamePassword);
        var accessToken = tokenProvider.generateAccessToken((User) authUser.getPrincipal());
        return accessToken;
    }

    public User findUserByUsername(String username) throws NotFoundException {
        User user = userRepository.findUserByUsername(username);
        if (user == null) {
            throw new NotFoundException("User not found with username: " + username);
        } else
            return user;
    }

    public User findUserByEmail(String email) throws NotFoundException {
        User user = userRepository.getUserByEmail(email);
        if (user == null) {
            throw new NotFoundException("User not found with email: " + email);
        } else
            return user;
    }

    public boolean existsUserByEmail(String email) {
        return userRepository.existsUserByEmail(email);
    }

    private void sendVerificationEmail(User user, String siteURL)
            throws MessagingException, UnsupportedEncodingException {
        String toAddress = user.getEmail();
        String fromAddress = "Your email address";
        String senderName = "Your company name";
        String subject = "Please verify your registration";
        String content = "Dear [[name]],<br>"
                + "Please click the link below to verify your registration:<br>"
                + "<h3><a href=\"[[URL]]\" target=\"_self\">VERIFY</a></h3>"
                + "Thank you,<br>"
                + "Your company name.";

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setFrom(fromAddress, senderName);
        helper.setTo(toAddress);
        helper.setSubject(subject);

        content = content.replace("[[name]]", user.getFirstName());
        String verifyURL = siteURL + "/verify?code=" + user.getVerificationCode();

        content = content.replace("[[URL]]", verifyURL);

        helper.setText(content, true);

        mailSender.send(message);
    }
}
