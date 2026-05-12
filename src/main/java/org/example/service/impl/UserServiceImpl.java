package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.UserDTO;
import org.example.dto.UserRegisterDTO;
import org.example.entity.Authority;
import org.example.entity.User;
import org.example.exception.UserAlreadyExistsException;
import org.example.exception.UserNotFoundException;
import org.example.repository.AuthorityRepository;
import org.example.repository.UserRepository;
import org.example.service.UserService;
import org.example.util.UserMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthorityRepository authorityRepository;

    @Override
    public List<UserDTO> getUsers() {
        return userRepository.findAll().stream()
                .map(
                        UserMapper::convertToDTO
                )
                .collect(Collectors.toList())
                ;
    }

    @Override
    public UserDTO getUserById(Long id) {
        return userRepository.findById(id).map(
                UserMapper::convertToDTO
        ).orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    @Override
    public UserDTO getUserByUsername(String username) {
        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty()){
            throw new UsernameNotFoundException("user not found exist");
        }
        return UserMapper.convertToDTO(optionalUser.get());
    }

    public UserDTO getUserByEmail(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()){
            throw new UsernameNotFoundException("user not found exist");
        }
        return UserMapper.convertToDTO(optionalUser.get());
    }

    public UserDTO loginByEmail(String email, String password) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()){
            throw new UsernameNotFoundException("user not found exist");
        }
        
        User user = optionalUser.get();
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }
        
        return UserMapper.convertToDTO(user);
    }

    @Override
    public UserDTO createUser(UserRegisterDTO dto) {
        if (userRepository.findByUsername(dto.getUsername()).isPresent()){
            throw new UserAlreadyExistsException("user already exist");
        }
        
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setIsAdmin(false);
        
        Authority userAuthority = authorityRepository.findByAuthority("ROLE_USER")
                .orElseGet(() -> {
                    Authority newAuthority = new Authority();
                    newAuthority.setAuthority("ROLE_USER");
                    return authorityRepository.save(newAuthority);
                });
        
        Set<Authority> authorities = new HashSet<>();
        authorities.add(userAuthority);
        user.setAuthorities(authorities);
        
        return UserMapper.convertToDTO(userRepository.save(user));
    }

    @Override
    public UserDTO updateUser(Long id, UserDTO dto) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User not found"));
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setIsAdmin(dto.getIsAdmin());
        return UserMapper.convertToDTO(userRepository.save(user));
    }

    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
