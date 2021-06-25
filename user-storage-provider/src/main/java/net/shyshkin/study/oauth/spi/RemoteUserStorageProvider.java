package net.shyshkin.study.oauth.spi;

import lombok.RequiredArgsConstructor;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.UserCredentialStore;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.adapter.AbstractUserAdapter;
import org.keycloak.storage.user.UserLookupProvider;

@RequiredArgsConstructor
public class RemoteUserStorageProvider implements UserStorageProvider,
        UserLookupProvider, CredentialInputValidator {

    private final KeycloakSession session;
    private final ComponentModel model;
    private final UsersApiService usersService;

    @Override
    public void close() {

    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        return null;
    }

    // We need to implement AT LEAST ONE of methods: getUserByUsername, getUserByEmail
    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {

        User user = usersService.getUserDetails(username);
        if (user != null) {
            return createUserModel(username, realm);
        }
        return null;
    }

    private UserModel createUserModel(String username, RealmModel realm) {
        return new AbstractUserAdapter(session, realm, model) {
            @Override
            public String getUsername() {
                return username;
            }
        };
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        return null;
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {

        if (!supportsCredentialType(credentialType))
            return false;

        return getCredentialStore()
                .getStoredCredentialsByTypeStream(realm, user, credentialType)
                .findAny()
                .isPresent();
    }

    private UserCredentialStore getCredentialStore() {
        return session.userCredentialManager();
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {

        String userPassword = credentialInput.getChallengeResponse();
        var verifyPasswordResponse = usersService.verifyUserPassword(user.getUsername(), userPassword);

        return verifyPasswordResponse != null && verifyPasswordResponse.isResult();
    }
}
