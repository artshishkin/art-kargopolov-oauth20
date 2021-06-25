package net.shyshkin.study.oauth.legacy.controllers;


import lombok.RequiredArgsConstructor;
import net.shyshkin.study.oauth.legacy.response.UserRest;
import net.shyshkin.study.oauth.legacy.response.VerifyPasswordResponse;
import net.shyshkin.study.oauth.legacy.service.UsersService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UsersController {

    private final UsersService usersService;

    @GetMapping("/{userName}")
    public UserRest getUser(@PathVariable("userName") String userName) {

        return usersService.getUserDetails(userName);

    }

    @PostMapping("/{userName}/verify-password")
    public VerifyPasswordResponse verifyUserPassword(@PathVariable("userName") String userName,
                                                     @RequestBody String password) {

        VerifyPasswordResponse returnValue = new VerifyPasswordResponse(false);

        UserRest user = usersService.getUserDetails(userName, password);

        if (user != null) {
            returnValue.setResult(true);
        }

        return returnValue;
    }

}