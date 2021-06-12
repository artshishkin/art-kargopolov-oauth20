# art-kargopolov-oauth20
OAuth 2.0 in Spring Boot Applications - Tutorial from Sergey Kargopolov (Udemy) 

####  Section 4: Keycloak. The Standalone Authorization Server.

#####  30. Creating a new Realm

-  Master Realm ->
-  Add realm
-  name: `katarinazart`
-  Create

#####  31. Creating a new user

-  Users section ->
-  Add user
    -  username: `shyshkin.art`
    -  email: `d.art.shishkin@gmail.com`
    -  First Name: `Artem`
    -  Last Name: `Shyshkin`
    -  Save
-  Modify credentials
    -  Credentials:
    -  Password: `password_art`    
    -  Password Confirmation : `password_art`
    -  Set password
-  User can change password
    -  Sign In: `http://localhost:8080/auth/realms/katarinazart/account`
    -  Username or Email: `shyshkin.art`
    -  Password: `password_art`
    -  Update password: `password_art` (just leave the same for study)
    -  Account security
        -  Signing In ->
        -  My Password -> Update
        -  New Password: `password_art_1`