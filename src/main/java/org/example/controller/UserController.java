package org.example.controller;


import lombok.RequiredArgsConstructor;
import org.example.dto.LoginEmailRequest;
import org.example.dto.LoginResponseDto;
import org.example.dto.UserDTO;
import org.example.dto.UserRegisterDTO;
import org.example.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/users")
public class UserController {
    private final UserService userService;

    @GetMapping("/test")
    public ResponseEntity<String> test(){
        return ResponseEntity.ok("Backend is working!");
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getPersonById(@PathVariable Long id){
        return ResponseEntity.ok(userService.getUserById(id));
    }
    
    @PostMapping("/register")
    public ResponseEntity<UserDTO> createUser(@RequestBody UserRegisterDTO dto){
        try {
            UserDTO result = userService.createUser(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @RequestBody UserDTO dto){
        return ResponseEntity.ok(userService.updateUser(id, dto));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id){
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/login")
    public ResponseEntity<UserDTO> login(Authentication authentication){
        return ResponseEntity.ok(userService.getUserByUsername(authentication.getName()));
    }

    @PostMapping("/login-email")
    public ResponseEntity<UserDTO> loginByEmail(@RequestBody LoginEmailRequest request){
        try {
            UserDTO userDTO = userService.loginByEmail(request.getEmail(), request.getPassword());
            return ResponseEntity.ok(userDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<String> getByName(@PathVariable String username){
        UserDTO userDTO = userService.getUserByUsername(username);
        return ResponseEntity.ok("User" + userDTO.getUsername() + "is register");
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(){
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("testuser");
        userDTO.setEmail("test@test.com");
        userDTO.setIsAdmin(false);
        return ResponseEntity.ok(userDTO);
    }
}
