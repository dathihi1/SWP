package com.badat.study1.controller;

import com.badat.study1.dto.request.UserCreateRequest;
import com.badat.study1.dto.response.UserCreateResponse;
import com.badat.study1.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/users")
    public UserCreateResponse craeteUser(@RequestBody UserCreateRequest request){
        return userService.createUser(request);
    }
}
