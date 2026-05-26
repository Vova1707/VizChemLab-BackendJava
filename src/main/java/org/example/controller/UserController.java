package org.example.controller;


import lombok.RequiredArgsConstructor;
import org.example.dto.ErrorResponse;
import org.example.dto.LoginEmailRequest;
import org.example.dto.UserDTO;
import org.example.dto.UserRegisterDTO;
import org.example.exception.UserAlreadyExistsException;
import org.example.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/users")
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> createUser(@RequestBody UserRegisterDTO dto){
        try {
            UserDTO result = userService.createUser(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (UserAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Internal server error"));
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
    
    @PostMapping("/login-email")
    public ResponseEntity<UserDTO> loginByEmail(@RequestBody LoginEmailRequest request){
        try {
            UserDTO userDTO = userService.loginByEmail(request.getEmail(), request.getPassword());
            return ResponseEntity.ok(userDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ResponseEntity.ok(userService.getCurrentUser(authorization));
    }
}
