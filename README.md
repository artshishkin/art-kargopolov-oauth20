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







        