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
           