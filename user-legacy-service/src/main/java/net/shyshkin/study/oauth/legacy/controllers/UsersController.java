package net.shyshkin.study.oauth.legacy.controllers;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.shyshkin.study.oauth.legacy.response.UserRest;
import net.shyshkin.study.oauth.legacy.response.VerifyPasswordResponse;
import net.shyshkin.study.oauth.legacy.service.UsersService;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UsersController {

    private final UsersService usersService;

    @GetMapping("/{userName}")
    public UserRest getUser(@PathVariable("userName") String userName) {
        log.debug("getUser(@PathVariable(\"userName\") {})", userName);
        return usersService.getUserDetails(userName);

    }

    @PostMapping("/{userName}/verify-password")
    public VerifyPasswordResponse verifyUserPassword(@PathVariable("userName") String userName,
                                                     @RequestBody String password) {
        log.debug("verifyUserPassword(@PathVariable(\"userName\") {}, @RequestBody {})", userName, password);

        VerifyPasswordResponse returnValue = new VerifyPasswordResponse(false);

        UserRest user = usersService.getUserDetails(userName, password);

        if (user != null) {
            returnValue.setResult(true);
        }

        return returnValue;
    }

}