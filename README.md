[![CircleCI](https://circleci.com/gh/artshishkin/art-kargopolov-oauth20.svg?style=svg)](https://circleci.com/gh/artshishkin/art-kargopolov-oauth20)
[![codecov](https://codecov.io/gh/artshishkin/art-kargopolov-oauth20/branch/master/graph/badge.svg)](https://codecov.io/gh/artshishkin/art-kargopolov-oauth20)
![Java CI with Maven](https://github.com/artshishkin/art-kargopolov-oauth20/workflows/Java%20CI%20with%20Maven/badge.svg)
[![GitHub issues](https://img.shields.io/github/issues/artshishkin/art-kargopolov-oauth20)](https://github.com/artshishkin/art-kargopolov-oauth20/issues)
![Spring Boot version][springver]
![Project licence][licence]
![Docker][docker]
![Testcontainers version][testcontainersver]
![Keycloak version][keycloakver]
![Keycloak Container][keycloak-container-ver]
![Selenium version][seleniumver]
![PostgreSQL Container][postgres-container-ver]

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

#####  34. Requesting Access Token and Refresh Token

-  URL: `http://localhost:8080/auth/realms/katarinazart/protocol/openid-connect/auth`
-  Params:
    -  response_type: `code`
    -  client_id: `photo-app-code-flow-client`
    -  scope: `openid profile`
    -  redirect_uri: `http://localhost:8083/callback` (must match `Valid Redirect URIs` from keycloak)
    -  state: `jskd879sdkj`
-  In Browser:
    -  go to `http://localhost:8080/auth/realms/katarinazart/protocol/openid-connect/auth?response_type=code&client_id=photo-app-code-flow-client&scope=openid profile&state=jskd879sdkj&redirect_uri=http://localhost:8083/callback`
    -  sign in:
        -  Username or Email: `shyshkin.art`
        -  Password: `password_art_1` (was changed)
    -  redirection happened:
        -  `http://localhost:8083/callback?state=jskd879sdkj&session_state=d0108229-39dc-44ca-baa3-eae0133f532a&code=37d5dc26-6af4-44b3-9677-21f54616087c.d0108229-39dc-44ca-baa3-eae0133f532a.5ca38048-af08-42e3-8bef-fa42c2956a9c`
        -  code: `37d5dc26-6af4-44b3-9677-21f54616087c.d0108229-39dc-44ca-baa3-eae0133f532a.5ca38048-af08-42e3-8bef-fa42c2956a9c`
-  Receive Token
    -  HttpMethod: `POST`
    -  URL: `http://localhost:8080/auth/realms/katarinazart/protocol/openid-connect/token`
    -  Body: `x-www-form-urlencoded`
    -  grant_type: `authorization_code`
    -  client_id: `photo-app-code-flow-client`
    -  client_secret: `ee68a49e-5ac6-4673-9465-51e53de3fb0e`
    -  code: `37d5dc26-6af4-44b3-9677-21f54616087c.d0108229-39dc-44ca-baa3-eae0133f532a.5ca38048-af08-42e3-8bef-fa42c2956a9c`
    -  redirect_uri: `http://localhost:8083/callback`
    -  scope: `openid profile`        
-  Got an Error - code expired
```json
{
    "error": "invalid_grant",
    "error_description": "Code not valid"
}
```    
-  Repeat faster - using Postman
-  Got an Access Token

```json
{
    "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI2YmJadUpFNVFwc0xCTlc5STNEbzFQRVVjci1VQmFicjdiNkR6NmVvNnhRIn0.eyJleHAiOjE2MjM1NTM1ODIsImlhdCI6MTYyMzU1MzI4MiwiYXV0aF90aW1lIjoxNjIzNTUyMTMzLCJqdGkiOiI4YmU4YWM3Ny04OWNjLTQ3NWQtYmIyMC1hMzgwZWQ2NjAzOGIiLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODAvYXV0aC9yZWFsbXMva2F0YXJpbmF6YXJ0IiwiYXVkIjoiYWNjb3VudCIsInN1YiI6IjYyNGJhOGNkLWIwMmYtNDQwNS1iNmU3LTY4NTVhNGJiMzQ1MiIsInR5cCI6IkJlYXJlciIsImF6cCI6InBob3RvLWFwcC1jb2RlLWZsb3ctY2xpZW50Iiwic2Vzc2lvbl9zdGF0ZSI6ImQwMTA4MjI5LTM5ZGMtNDRjYS1iYWEzLWVhZTAxMzNmNTMyYSIsImFjciI6IjAiLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiZGVmYXVsdC1yb2xlcy1rYXRhcmluYXphcnQiLCJvZmZsaW5lX2FjY2VzcyIsInVtYV9hdXRob3JpemF0aW9uIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJvcGVuaWQgcHJvZmlsZSBlbWFpbCIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwibmFtZSI6IkFydGVtIFNoeXNoa2luIiwicHJlZmVycmVkX3VzZXJuYW1lIjoic2h5c2hraW4uYXJ0IiwiZ2l2ZW5fbmFtZSI6IkFydGVtIiwiZmFtaWx5X25hbWUiOiJTaHlzaGtpbiIsImVtYWlsIjoiZC5hcnQuc2hpc2hraW5AZ21haWwuY29tIn0.aHycqZptDylgEWc7UvgOcuKVeCn1o3apu0E4ouMJcz2UIWWK9__JiRrOoWQhWR2Z7PauSLpSYWbBBiTTzVk33lh1fxx8tu5dhdce2hxDpUxxcwd7U4ChHgRLJQhdPRvie7Uf8xsBWjlDrnQHkh8RcUmVwjSsRm-yMYPrMoVuWAjlB0h_ZcdOLDamT1vq_prcvXvTcbmxzUuj6LgcTyv0YlM3DyrgGrD6iP4iexQeZj5fx2wIlmwLTh77uYXKZ6hVSpySWUWX7PL8IXh-TcVv2KmJPlIlOUNQ2xpvt9I3MRXszxR84saO_6ef5bVwpei6b6H8-RtEn1NFZQNrqEtpRw",
    "expires_in": 300,
    "refresh_expires_in": 1800,
    "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJmMDQ2YmI0MS0zMmE2LTQ0YjItYjNlYS00MWZjYTlhNjRhMzIifQ.eyJleHAiOjE2MjM1NTUwODIsImlhdCI6MTYyMzU1MzI4MiwianRpIjoiNmUzMWJiM2MtNzc4Yi00MDIzLThhNWMtNTJmMjk2OWYxMTIzIiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL2F1dGgvcmVhbG1zL2thdGFyaW5hemFydCIsImF1ZCI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MC9hdXRoL3JlYWxtcy9rYXRhcmluYXphcnQiLCJzdWIiOiI2MjRiYThjZC1iMDJmLTQ0MDUtYjZlNy02ODU1YTRiYjM0NTIiLCJ0eXAiOiJSZWZyZXNoIiwiYXpwIjoicGhvdG8tYXBwLWNvZGUtZmxvdy1jbGllbnQiLCJzZXNzaW9uX3N0YXRlIjoiZDAxMDgyMjktMzlkYy00NGNhLWJhYTMtZWFlMDEzM2Y1MzJhIiwic2NvcGUiOiJvcGVuaWQgcHJvZmlsZSBlbWFpbCJ9.oGjRSycr4jScFpsih2UtbKuBQr8N7qxgt62eqHmHtZ8",
    "token_type": "Bearer",
    "id_token": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI2YmJadUpFNVFwc0xCTlc5STNEbzFQRVVjci1VQmFicjdiNkR6NmVvNnhRIn0.eyJleHAiOjE2MjM1NTM1ODIsImlhdCI6MTYyMzU1MzI4MiwiYXV0aF90aW1lIjoxNjIzNTUyMTMzLCJqdGkiOiIzNWJhZTJiZS0yMGY3LTRlN2EtYjA4ZS02MWY4ZGUwM2ZlODYiLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODAvYXV0aC9yZWFsbXMva2F0YXJpbmF6YXJ0IiwiYXVkIjoicGhvdG8tYXBwLWNvZGUtZmxvdy1jbGllbnQiLCJzdWIiOiI2MjRiYThjZC1iMDJmLTQ0MDUtYjZlNy02ODU1YTRiYjM0NTIiLCJ0eXAiOiJJRCIsImF6cCI6InBob3RvLWFwcC1jb2RlLWZsb3ctY2xpZW50Iiwic2Vzc2lvbl9zdGF0ZSI6ImQwMTA4MjI5LTM5ZGMtNDRjYS1iYWEzLWVhZTAxMzNmNTMyYSIsImF0X2hhc2giOiJFeUh0RmVhSDlmOFM4Z1dueEVxVENRIiwiYWNyIjoiMCIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwibmFtZSI6IkFydGVtIFNoeXNoa2luIiwicHJlZmVycmVkX3VzZXJuYW1lIjoic2h5c2hraW4uYXJ0IiwiZ2l2ZW5fbmFtZSI6IkFydGVtIiwiZmFtaWx5X25hbWUiOiJTaHlzaGtpbiIsImVtYWlsIjoiZC5hcnQuc2hpc2hraW5AZ21haWwuY29tIn0.dRbxx0IqO46nTWxLIJqIClqqit1jUfNKICAQQ0PhNvyMBNVW6luDjPqDSWmdCKtrSVr0B58Pswa8CUw6NwzzUVhXMjctJMcYUNu2Eikmi0YgRQ1evWUy-ko_x6kEVmIqbEE3u-HGU89XPkqBt1ycmKR1kUKysjptA5ViFDSyjiRlcLsDcGa8bNfXAJFcd9Pp0DM9BdWyCcvrCe4322t2YKaS0m7KWgz5S2oILAjkRSFLBtw_biLdXVEnCUOsirnLyqlz_RUfufI2gZztfN4n3npzLcVTATJQO_vbApcG1B3YRlcV3g0ZxbYJWYLityFHAbTvpf5tBEpPXQdDOrfGBA",
    "not-before-policy": 0,
    "session_state": "d0108229-39dc-44ca-baa3-eae0133f532a",
    "scope": "openid profile email"
}
```
-  Using cURL
```shell script
curl --location --request POST 'http://localhost:8080/auth/realms/katarinazart/protocol/openid-connect/token' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'grant_type=authorization_code' \
--data-urlencode 'client_id=photo-app-code-flow-client' \
--data-urlencode 'client_secret=ee68a49e-5ac6-4673-9465-51e53de3fb0e' \
--data-urlencode 'code=c375552f-f420-43fe-8f58-1bbd4d73ff62.d0108229-39dc-44ca-baa3-eae0133f532a.5ca38048-af08-42e3-8bef-fa42c2956a9c' \
--data-urlencode 'redirect_uri=http://localhost:8083/callback' \
--data-urlencode 'scope=openid profile'
```

####  Section 6: Resource Server - Scope Based Access Control

#####  50. Demo - without using proper Scope

1. Remove Default Client Scopes
    -  log in into Keycloak management console
    -  Client Scopes
    -  Assigned Default Client Scopes -> move email and profile into Optional
2.  Remove Client Scopes from Client
    -  Clients -> `Photo-app-code-flow-client`
    -  Client Scopes
    -  Assigned Default Client Scopes -> move email and profile into Optional
3.  Stop Docker compose `keycloak-postgres`
4.  Export new settings          
    -  Start Docker compose `keycloak-postgres-export`
    -  Wait for `export/realm-export.json` to be changed
    -  Stop docker compose          

####  Section 7: Role Based Access Control with Keycloak

#####  54. Creating User Role

-  Log in into Keycloak management console as admin
-  Roles -> 
    -  Add role -> `developer`
    -  Save
-  Assign Role to the user
    -  Users -> shyshkin.art
    -  Role Mappings
    -  Move `developer` to Assigned Role

####  Section 8: Resource Server: Method Level Security

#####  66. Reading UserId from JWT Access Token

Jwt.sub - stands for subject - most of the time it equals User ID

####  Section 9: Resource Server Behind API Gateway

#####  78. Trying how it works

#####  Enabling password grant_type

-  Sign in into Keycloak Administration Console as `admin`.
-  Clients -> `photo-app-code-flow-client`
-  Settings:
    -  Direct Access Grants Enabled: `true`
    -  Save
-  Test using Postman or curl
```shell script
curl --location --request POST 'http://localhost:8080/auth/realms/katarinazart/protocol/openid-connect/token' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'grant_type=password' \
--data-urlencode 'username=shyshkin.art' \
--data-urlencode 'password=password_art_1' \
--data-urlencode 'client_id=photo-app-code-flow-client' \
--data-urlencode 'client_secret=ee68a49e-5ac6-4673-9465-51e53de3fb0e' \
--data-urlencode 'scope=openid profile'
```    
-  Must return correct access token 

####  Section 13: OAuth 2.0 in MVC Web App

#####  102. Configuring OAuth2 Client properties

1.  Register new client in Keycloak
    -  Clients
    -  Create -> `photo-app-webclient`
    -  Save
2.  Configure
    -  Valid Redirect URIs: 
        -  `http://localhost:8050/login/oauth2/code/photo-app-webclient`
        -  `http://localhost:8080/login/oauth2/code/photo-app-webclient` - for testing purposes
        -  `http://host.testcontainers.internal:8050/login/oauth2/code/photo-app-webclient` - for testing purposes
        -  `http://photo-app-webclient:8080/login/oauth2/code/photo-app-webclient` - for running in docker compose (swarm)
    -  Access Type: `confidential`
    -  Save
3.  Get clientSecret
    -  Credentials -> Secret        

####  Section 14: OAuth 2 - Social Login

#####  121. Facebook: Client Id and Client Secret

1.  Create Facebook App
    -  Visit [developers.facebook.com](https://developers.facebook.com)
    -  Begin (register as developer with your Facebook account)
    -  (Facebook asked me to add new email - `d.art.shishkin@ukr.net`)
    -  Create App -> Other (For Everything Else)
    -  App Display Name: `SocialLoginExample`
    -  Create App ID
2.  Get Client ID
    -  Settings -> Basic
    -  App ID -> copy it `200469625293049`
    -  App Secret -> copy it 
        -  Create Environment Variable `FACEBOOK_CLIENT_SECRET` and paste it

#####  123. Google Client Id and Client Secret

1.  Create Google App
    -  Visit [console.developers.google.com](https://console.developers.google.com)
    -  Create Project
    -  Project Name: `SocialLoginExample`
    -  Create
2.  Configure Consent Screen
    -  Credentials
    -  Configure Consent Screen
    -  External User Type -> Create    
    -  App information:
        -  App name: `SocialLoginExample`
        -  User support email: my-email
        -  Developer contact information: my-email
        -  Save and Continue
    -  Scopes:
        -  Add or Remove Scopes:
        -  `openid` -> Update
        -  Save and Continue
    -  Test users
        -  `While publishing status is set to "Testing", only test users are able to access the app. Allowed user cap prior to app verification is 100, and is counted over the entire lifetime of the app.`
        -  Add users -> add myself
        -  Save and Continue
    -  Summary
        -  Back to Dashboard
3.  Get Client ID
    -  Credentials
    -  Create credentials
    -  OAuth Client ID
        -  Application type: `Web application`
        -  Name: `SocialLoginExample`
        -  Authorized redirect URIs: `http://localhost:8051/login/oauth2/code/google`
        -  Create
        -  Copy Google Client Id: `57502764787-df6bk8i3vlsdmikgvq84tf981refqtif.apps.googleusercontent.com`
        -  Copy Google Client Secret and create Environment variable: `GOOGLE_CLIENT_SECRET` 
        
#####  125. Register a new Okta app

1.  Create Okta App
    -  Visit [developer.okta.com](https://developer.okta.com)
    -  Create Account (I have no Okta account)
    -  Applications -> Create App Integration
        -  Sign-on method: `OpenID Connect`
        -  Application type: `Web Application`
        -  Next
    -  New Web App Integration
        -  App integration name : `SocialLoginExample`
        -  Sign-in redirect URIs: `http://localhost:8051/login/oauth2/code/okta`
        -  Save
2.  Get Client ID
    -  Client ID : `0oa11x7wuyEyJtNjY5d7`
    -  Client secret : copy and create Environment variable `OKTA_CLIENT_SECRET`

#####  131. OpenID Connect End Session Endpoint

To view if provider supports OpenID Connect End Session Endpoint visit
-  `https://{base-server-url}/.well-known/openid-configuration`
-  [Okta my dev account example](https://dev-83879807.okta.com/.well-known/openid-configuration)
    -  receive JSON with available endpoints
    -  "end_session_endpoint": "https://dev-83879807.okta.com/oauth2/v1/logout"
-  [Keycloak on localhost example](http://localhost:8080/auth/realms/katarinazart/.well-known/openid-configuration)    
    -  "end_session_endpoint": "http://localhost:8080/auth/realms/katarinazart/protocol/openid-connect/logout"
-  [Google](https://accounts.google.com/.well-known/openid-configuration)
    -  has no "end_session_endpoint"

#####  133.1 Trying how Logout from Okta works

-  Configure **Sign-out redirect URIs** in Okta console
    -  http://localhost:8080 // standard (I do not need it but just left)
    -  http://localhost:8051 // application port
    -  http://social-login-example:8080 // for future use in docker compose

#####  133.2 Trying how Logout from Keycloak works

-  Valid Redirect URIs: 
    -  add `http://localhost:8051/login/oauth2/code/photo-app-webclient`
    -  add `http://localhost:8051` - for post_logout_redirect_uri

####  Section 15: OAuth2 + PKCE in JavaScript Application

#####  135. Creating a new Public client in Keycloak

1.  Start Keycloak server
2.  Log in into management console as admin
3.  Create client
    -  Clients -> Create
    -  Client ID: `photo-app-pkce-client`
    -  Save
4.  Configure client
    -  Access Type: `public`
    -  Standard Flow Enabled: true
    -  Direct Access Grant: FALSE
    -  Valid Redirect URIs: `http://localhost:8181/authcodeReader.html`
    -  Web Origins: `+` (permit all origins of Valid Redirect URIs)
    -  Advanced Settings: 
        -  Proof Key for Code Exchange Code Challenge Method: `S256`
    -  Save      

##### 138. Import jQuery  

Google for `Google Hosted Libraries` and find jQuery 

##### 147. Finding Refresh Token and ID Token

1.  In browser open Developers' Tools  (`Ctrl+Shift+I` in Chrome)
2.  Network
3.  Click `Get Auth Code` button
4.  View Response Body JSON -> Preview

####  Section 16: Keycloak Remote User Authentication. User Storage SPI.

#####  166. Deploying User Storage SPI

- Copy `my-remote-user-storage-provider` JAR to Keycloak
    -  `keycloak/standalone/deployments/ `(Kargopolov solution)
    -  `/opt/jboss/keycloak/standalone/deployments` (in case of docker)
    -  `docker container cp .\my-remote-user-storage-provider.jar keycloak-postgres_keycloak_1:/opt/jboss/keycloak/standalone/deployments`
    -  in docker container files will see files added and deployed
    -  in container logs will see success deployment:
```
13:07:00,005 INFO  [org.jboss.as.repository] (DeploymentScanner-threads - 1) WFLYDR0001: Content added at location /opt/jboss/keycloak/standalone/data/content/d1/bb6440489e99524675b04f4cd3ddbff8f3f6f3/content
13:07:00,024 INFO  [org.jboss.as.server.deployment] (MSC service thread 1-2) WFLYSRV0027: Starting deployment of "my-remote-user-storage-provider.jar" (runtime-name: "my-remote-user-storage-provider.jar")
13:07:00,271 INFO  [org.keycloak.subsystem.server.extension.KeycloakProviderDeploymentProcessor] (MSC service thread 1-5) Deploying Keycloak provider: my-remote-user-storage-provider.jar
13:07:00,438 INFO  [org.jboss.as.server] (DeploymentScanner-threads - 1) WFLYSRV0010: Deployed "my-remote-user-storage-provider.jar" (runtime-name : "my-remote-user-storage-provider.jar")
```
-  Enable `my-remote-user-storage-provider`
    -  Log in into Keycloak management console
    -  User Federation
    -  Add provider: `my-remote-user-storage-provider`
    -  Priority: 0 (Lowest first)
    -  Success message appears

#####  167. Trying how it works - localhost

1.  Start `user-legacy-service` locally
2.  Redeploy `my-remote-user-storage-provider`
    -  keycloak runs in docker container so
    -  modify LEGACY_SYSTEM_URI to "http://host.docker.internal:8099"
    -  `mvn clean package`
    -  `docker container cp .\my-remote-user-storage-provider.jar keycloak-postgres_keycloak_1:/opt/jboss/keycloak/standalone/deployments`
    -   put cache policy to NO_CACHE on User Federation in Keycloak management console
3.  Ask for authorization code
    -  `http://localhost:8080/auth/realms/katarinazart/protocol/openid-connect/auth?response_type=code&client_id=photo-app-code-flow-client&scope=openid%20profile&state=jskd879sdkj&redirect_uri=http://localhost:8083/callback`
    -  Username or email: `test2@test.com`
    -  Password: `art`    

#####  167. Trying how it works - docker

1.  In service `user-legacy-service` enable endpoint `/users/**` for everyone 
2.  Create `user-legacy-service` docker image
3.  Modify RemoteUserStorageProvider for new URI - from docker-compose
4.  Create `keycloak-postgres-remote-user-storage-provider` docker-compose file



[springver]: https://img.shields.io/badge/dynamic/xml?label=Spring%20Boot&query=%2F%2A%5Blocal-name%28%29%3D%27project%27%5D%2F%2A%5Blocal-name%28%29%3D%27parent%27%5D%2F%2A%5Blocal-name%28%29%3D%27version%27%5D&url=https%3A%2F%2Fraw.githubusercontent.com%2Fartshishkin%2Fart-kargopolov-oauth20%2Fmaster%2Fpom.xml&logo=Spring&labelColor=white&color=grey
[licence]: https://img.shields.io/github/license/artshishkin/art-kargopolov-oauth20.svg
[testcontainersver]: https://img.shields.io/badge/dynamic/xml?label=Testcontainers&query=%2F%2A%5Blocal-name%28%29%3D%27project%27%5D%2F%2A%5Blocal-name%28%29%3D%27properties%27%5D%2F%2A%5Blocal-name%28%29%3D%27testcontainers.version%27%5D&url=https%3A%2F%2Fraw.githubusercontent.com%2Fartshishkin%2Fart-kargopolov-oauth20%2Fmaster%2Fpom.xml&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAB4AAAAjCAIAAAAMti2GAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAAEnQAABJ0Ad5mH3gAAAPCSURBVEhL7ZZNbBRlGMffj9nZabddtqW1yldDoSUSP1ICaoKlUJtw0BDTNCQcOBDjxSOnxpvGaLhw9KYSLxxAOGDCQa1iNBFogYCBdLds60KXfmw3W9idz/fDZ3bGsl1md6cYb/4Om5ln3uf/Pvt/n3nfwVJKFI6fHmXh952XNnm3DQklnbeso1fGby3n4Pq19o7zB4fao1HvUR0aS5+8fvWr5NQLmhYlBG4tIRZN84O+Xaf3vekNqEU96a9TybHJ682UxhQFY+xHEYKUEmM656f27juxs8+PPkOw9GQud/y3KwXLTKhRUiFaiZCyYFvtUe3bgcE9Gzv8aAXV0kXHOfbrL78vzIMDStmB+rCyP/u7Xjx74GBLJOJHy6yR/vjGxJf37nZomkapHwqHyXnOND96effne/b6oVXpszPpk9f+UAluUSKVtoYHdIrMsYU8/cZbx7b3QATPrKyMjP+YNQ3op1q2hgcWADp1U6z5wtAwzXx49Gbx8RYbI4yh/ucr2QPSCUbxaCSzbKfmS6QV00Jn83Rvm90UiTAJf8wfuG6kQhFz8ExG5PMypkbKPSAkRyi9pSXTHUeEECbWOYGEVsISZ+flbJZzKQmFf4/89gIXFC71KJ3q2bDUFaMCYR5mAgkuKgRDmdMZrpsCCl+19GnnQoBId4J8XE32thUTGly76xI0ARhXdgDrJZz6i+efCGhXAm1QsVTVLwU8oZAl5Fxnc7onwTTFnaBa3a1UMDz7UGRzHNToWlGP4PcNRilC2gTf39Y6tzUOacT3p2wrwguLMj3HGXcLf1bUI1jaA54pTBY1OrUzke+MwWQgVCi4tj4x1tgaSD1pAFJhASiTSwk1tXtjOsVyK4KSalsDaSDtARqUI0GQ4DLQ1kBCSftIt1vDsx7pdfK/dBXQWv8JsD0QXXDEGWwVfuxfA1LCcnTGyfkd/Z9s3mXZpsFZ4E4UHvcMc5he1D870H/uvYGnx+6R6clLy1kSgXMsaAFgj2oiyveLqCn4RLY4d4rG+6/0XDwy6EXWnOizlj6YvJYxS6qiwrbjRz1qS3MhDcPsbt/w8+jQ9kSrH62S9vgu/2g0fQsuNFrx0RQkDbkly4ED8dy7+0f7uv3oPwRIe4w9nDqVTSJF1bC7a1RJQxYslDSssbdf/2Kg30upoqY0AF9Gh6cnxgsLVImqmKxK21zYJWO4d+vlkUN1vrDqSXvc0R8PpyYWbUNt1ZRLSzpyuuKxH0YOvdrZ5o+oBUiH4ZulB+j2ZfTpmTN/3vdDjWhc9XOC0N95QCMLG07m0AAAAABJRU5ErkJggg==&labelColor=white&color=grey
[docker]: https://img.shields.io/static/v1?label=&message=Docker&labelColor=white&color=white&logo=docker
[keycloakver]: https://img.shields.io/badge/dynamic/xml?label=Keycloak&query=%2F%2A%5Blocal-name%28%29%3D%27project%27%5D%2F%2A%5Blocal-name%28%29%3D%27properties%27%5D%2F%2A%5Blocal-name%28%29%3D%27keycloak.version%27%5D&url=https%3A%2F%2Fraw.githubusercontent.com%2Fartshishkin%2Fart-kargopolov-oauth20%2Fmaster%2Fpom.xml&logo=data:image/png;base64,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&labelColor=white&color=grey
[seleniumver]: https://img.shields.io/badge/dynamic/xml?label=Selenium&query=%2F%2A%5Blocal-name%28%29%3D%27project%27%5D%2F%2A%5Blocal-name%28%29%3D%27properties%27%5D%2F%2A%5Blocal-name%28%29%3D%27selenium.version%27%5D&url=https%3A%2F%2Fraw.githubusercontent.com%2Fartshishkin%2Fart-kargopolov-oauth20%2Fmaster%2Fpom.xml&logo=selenium&labelColor=white&color=grey
[postgres-container-ver]: https://img.shields.io/badge/dynamic/yaml?label=PosgtreSQL&query=$.POSTGRES_VERSION&url=https%3A%2F%2Fraw.githubusercontent.com%2Fartshishkin%2Fart-kargopolov-oauth20%2Fmaster%2Fdocker-compose%2Fenv-versions.yaml&logoWidth=40&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAFQAAAAjCAYAAAAKTC24AAAABGdBTUEAALGPC/xhBQAAAAlwSFlzAAASdAAAEnQB3mYfeAAABA5JREFUaEPVmU123CAQhOcoPkqO4qP4KFnlHF7nNNlnMelqU7xSq0Ew1l/qvYpGCBD90SDJeTyDfv3+83z/+fn8Ycbx4/PTy2aFNuhD+3n+ne/nu8I4HnZ/9Zv5KFWgfuN3uyH8IWaZWcES2FuphyPAxTra3m31tM7R8jEUkKcBrYEXOJl/mCmH2KqfQMvqaH9H6hKgWzBxnUqvZ7Y2HzLw1T3sHBNztBwo7ncW0M3stGvMNizpbt1oq4s2UHofO/e99UjZvu17ud2Hfj/wng+HpEFGW9DUFEza2nBCEFi8jrJZob/orXJ/IKoP0iMLUs0Mm85OdWdSppe9wfC2GE8xl/Bs+RF68CmdWrLLQaiz+i1bfS7tOIHTGQqgBUwENFs+KmwTo9oEShGsy4JyMFtg7bpnuCwx9KPtuAKGdTJQj7N4RJtLfiWB091/DVqcBGgB1I7TDyUARVsJtIKbLB9RbLulzYcSxXdPHDWrNNvUujeiPs8BUIEuoA8KbaJfKd8S6ilMeOsNweL6CkxhqCHvGHUSAK0tIy5ltltMoPXXE1ZP9rrD3+qZco6F/dKsT6EsAoV76r+HloAXdeyIQAmstWXwOiZMs1rr43dPXg/1i19d2rPlFM71Og0ealX/S6kA9X1L6xSoUCvDucQ5GdwrK1Ari4OJioFUEN8s59hb5ZSPdcSI147QF9ACYWULGiIUvcZv8eaWUdpC+M0M9SWP+nK9pTjws4FiC4h1esb9HKjPRARSDKVArQ3UA8qsBMT4UCLgnrwfjK24AtqjvPO2UIU6cn3LzgntMmBwXLZ6bZFxUr6wtaH4m339D/KxCrCea9b7v6YMKAytgNpvlEGtp7zb6jFLWR8T0PrTHZ6qyJKrnQHrWbeKCjQFg0wqL+QRKLUozyx1oXiueiWYS4y47IgEYKJQFWhrL6z0+aQ3c7k3989gnUH9HbUa+A29pQoUSuFYWZ0Fg+r7apa1PWsfHd09Q3vJQC2ANvfDBhCUdffQYp+ElsrkQBhwFsgt3ItBtAAKQKuss/M4Mw5XQCzqR8tkODA7r5OAvu1Izb73nWnslyNaAIUQYIVagocABf9H5DBw3Tz86qRbBG3XCJryCS0B3M2jWgGFCAJHyANVGMUEMvouivreRrI7KgvmajPOEaVAobQTA1GhiEaBjigL6EqPLnWqCXRGewLFZGWBXWG8dczqdkChLLiz/QpM6JZAr87SV2FCtwQKtf5afqhtvPGv9rPaBWj3E/RFoNDZX07pg3hSuwD1JdoAOvK51tPhX082xu9mpWoXoFAE6UZ2dt45R3XUnronSGo3oKsstd97LCHVHmCzP7ntqd2AQhgoligeUntkZlPlAwMZhvvFvRbnPo4C70iASz2f/wC11/BeQd5XfwAAAABJRU5ErkJggg==&labelColor=white&color=grey
[keycloak-container-ver]: https://img.shields.io/badge/dynamic/yaml?label=Keycloak&query=$.KEYCLOAK_VERSION&url=https%3A%2F%2Fraw.githubusercontent.com%2Fartshishkin%2Fart-kargopolov-oauth20%2Fmaster%2Fdocker-compose%2Fenv-versions.yaml&logoWidth=40&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAFoAAAAjCAYAAAAUhR0LAAAABGdBTUEAALGPC/xhBQAAAAlwSFlzAAASdAAAEnQB3mYfeAAAA99JREFUaEPtmkGSmzAQRecoPoqPwlF8FC9SOYfXOUMOMfssSP8ufaXVtEAyMMBUftWrGKkR6EWWcXk+xoa8Xq/xdruP9/t6MM7z+UM5Mj9/fY4fz1fBTdgrTaIjYVtwZP6L/qKcSvTj8VAiSVuAsY+Kin6cQfSfz1BOZgjaEoNhrg7gOocE8xOxd/n8IYOwV76VaKxSz1K7Xt+yU6qih2G6ZVCevtXwtpMVAFSoAwNbfD/BdTaJSKptBb3te2QiGo9yYCJEuMmeCvQGjWjcYFln+jAWQI0Q/afwmqsC0TJ+JK63vTWYV2smoou3kSMLNiKB7m2m7mH6AG4IwoGtI6/fIhpIv6c5Mk5NXG97S7BoeF5Lvpdo3JcRkIV2trfEn7uUUrQTYMFN6OC8gEggUb3tz9tHgjVZ8Aw98R94+mH3RvtSUGclg6UnlkK0TszIIk8ZJEsGOBbyzUmNF8Q+1lIypLPPnxMRpfZYxteWnnadS2q3sJ5BmxcN5nJJ0XoPZoLvbhG97QyObT/hvIjNP9FOrkUvbC+ehNmaiaDUjjrWAwoH/pyIKHZyIAta2Y53ylw7Yz8IZxFn+VpWFF8TvqWyaLxOeCFVzDmAqxpg7PAcIv1R/IS+WrQ6cTVz4HpZNLETzYLx7IxiXCAJ8rVz6I0lKBngWJ8ugnMU6Yui9+Qmslm7zKtWn4Ma078EtpFLij46Ks7JrJHfJeEEE1m0FEMM91Y8J6to+TcTnA9QN+CiZixKV9Gos2PwdQLR7Qb3cDBWYAt2yyn2aAsFqSSZKKBoSI+kKG4ctOV3BcBxAiu9Ok4CeWeSh5AWEhxNnzouIHoyoROylPx4NxEk8D+BYri3Uniu9a8FnqurkVsGkD5i62sgZ1/R/qkkyuQLS4Ru/jJpwlWNdhCdgy85xRcdvE5gjweRWA+CiUQTPAUyt5YUorECI2m6MmXSFMVVzRWudfiXyHEWDHBD0s5zi9oFEH1q8RM8CfDQkkuI7nmc+mpaU4qWWLkWlScTt8KIfsKaWl2BVjSOExy/AO0VmGiSR6PzbsxUdJqg/xVEfzmRwfWDSfoBVrWubGlDP6n9CsM6P7aXGiWa6JG0bhnMZUSfafvAPHozEc1EP84SnGTx8nCcb8zURJJ7fpy1kz2KdyQjcBBH9lovpWDhzwi0n0T9Cd3XG3P0qn5XMlIXLTnjXyphq4ok7IpsgfiAX5PLiUawskIhO9HzdFHLrGgmkrQFa7L7t8UNVrFNk2g8EZzx76P32rO3FMxcWjSzhXDs/VtsEbU0idaYb36r2SsyNmRhRWJr8Xs5jtGOftTtKbbMOP4F/8/qedWTQ2AAAAAASUVORK5CYII=&labelColor=white&color=grey 

           