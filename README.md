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

#####  32. Creating a new OAuth client application

-  Create Client:
    -  Clients section
    -  Create
    -  Client Id: `photo-app-code-flow-client`
    -  Client Protocol: `openid-connect`
    -  Save
-  Settings
    -  Standard Flow Enabled: `On`
    -  Implicit Flow Enabled: `Off`
    -  **Direct Access Grants Enabled**: `Off`
    -  Valid Redirect URIs: `http://localhost:8083/callback` (fake URL (for testing) - have no running App yet)
    -  Save

##### 33. Configuring Client Application Secrets

-  Enable `Credentials` tab
    -  Settings
    -  Access Type: `confidential`
    -  Save
-  Credentials tab
    -  Client Authenticator: `Client Id and Secret`
    -  Secret: `ee68a49e-5ac6-4673-9465-51e53de3fb0e` (copy or regenerate)    








        